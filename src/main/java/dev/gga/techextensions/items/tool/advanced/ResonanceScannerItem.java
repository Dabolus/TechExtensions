package dev.gga.techextensions.items.tool.advanced;

import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.menu.ResonanceScannerMenu;
import dev.gga.techextensions.init.TEItemSettings;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import reborncore.api.blockentity.IUpgrade;
import reborncore.api.blockentity.IUpgradeable;
import reborncore.common.powerSystem.RcEnergyItem;
import reborncore.common.powerSystem.RcEnergyTier;
import reborncore.common.util.ItemUtils;
import techreborn.config.TechRebornConfig;
import techreborn.init.TRContent;
import techreborn.items.UpgradeItem;
import techreborn.utils.TRItemUtils;

public class ResonanceScannerItem extends Item implements RcEnergyItem, IUpgradeable {
    public static final int ALLOWED_UPGRADES = 2;
    // Precomputed Fibonacci percentages, capped at 100% of max range
    private static final double[] FIBONACCI_SEQUENCE = {0.01, 0.02, 0.03, 0.05, 0.08, 0.13, 0.21, 0.34, 0.55, 1};

    public final RcEnergyTier tier = RcEnergyTier.HIGH;

    // Keep track of the last tick so that we can display coordinates every x ticks
    private long lastDisplayTick = 0;

    public ResonanceScannerItem(String name) {
        super(TEItemSettings.item(name).durability(0));
    }

    // Item
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel worldIn, Entity entityIn, @Nullable EquipmentSlot slot) {
        if (worldIn.isClientSide()) {
            return;
        }
        if (entityIn instanceof Player playerIn) {
            TRItemUtils.checkActive(stack, 1, entityIn);
            if (!TRItemUtils.isActive(stack)) {
                return;
            }
            long currentTick = worldIn.getGameTime();
            int overclockerUpgrades = getUpgradesCount(stack, TRContent.Upgrades.OVERCLOCKER);
            if (currentTick - lastDisplayTick < computeScanCooldown(overclockerUpgrades)) {
                return;
            }
            lastDisplayTick = currentTick;
            // Compute cost based on items in target stack and overclocker upgrades
            ItemStack targetStack = getTarget(stack);
            long scanCost = computeScanCost(targetStack, overclockerUpgrades);
            // Consume energy, regardless of whether player is holding it or not
            tryUseEnergy(stack, scanCost);
            Item item = targetStack.getItem();
            // Ignore non-block items
            if (!isValidTarget(item)) {
                return;
            }
            long effectiveRange = computeEffectiveRange(targetStack);
            BlockPos foundPos = findTargetBlock(worldIn, playerIn, ((BlockItem) item).getBlock(), effectiveRange);
            if (foundPos == null) {
                return;
            }
            // Only display info text if scanner is held in main/off hand
            if (stack.equals(playerIn.getMainHandItem()) || stack.equals(playerIn.getOffhandItem())) {
                // Compute the distance to the found block
                double distance = playerIn.position().distanceTo(Vec3.atCenterOf(foundPos));
                long estimatedDistance = estimateDistance(distance, effectiveRange);
                // Display message to player
                playerIn.displayClientMessage(
                    Component.translatable(
                        "techextensions.message.resonance_scanner.block_in_range",
                        Component.literal(item.getName().getString()).withStyle(ChatFormatting.GOLD),
                        Component.literal(Long.toString(estimatedDistance)).withStyle(ChatFormatting.GOLD)
                    ).withStyle(ChatFormatting.GRAY),
                    true
                );
            }
        }
    }

    @Override
    public long getStoredEnergy(ItemStack stack) {
        return Math.min(RcEnergyItem.super.getStoredEnergy(stack), getEnergyCapacity(stack));
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return ItemUtils.getPowerForDurabilityBar(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return ItemUtils.getColorForDurabilityBar(stack);
    }

    // RcEnergyItem
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        int energyStorageUpgrades = getUpgradesCount(stack, TRContent.Upgrades.ENERGY_STORAGE);
        return TechExtensionsConfig.resonanceScannerCharge + (long)(energyStorageUpgrades * TechRebornConfig.energyStoragePower);
    }

    @Override
    public RcEnergyTier getTier() {
        return tier;
    }

    @Override
    public InteractionResult use(final Level world, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            TRItemUtils.switchActive(stack, 1, player);
            return InteractionResult.SUCCESS;
        } else if (!world.isClientSide()) {
            player.openMenu(
                new SimpleMenuProvider((syncId, inventory, _p) -> new ResonanceScannerMenu(syncId, inventory), stack.getHoverName())
            );
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onUseTick(Level world, LivingEntity entity, ItemStack stack, int i) {
        TRItemUtils.checkActive(stack, 1, entity);
    }

    // IUpgradeable
    @Override
    public boolean canBeUpgraded() { return true; }

    @Override
    public Container getUpgradeInventory() {
        return null; // Cannot be implemented for Item
    }

    public static Container getInventory(ItemStack stack) {
        SimpleContainer container = new SimpleContainer(3) {
            @Override
            public void setChanged() {
                super.setChanged();
                List<ItemStack> items = this.getItems().stream().filter(s -> !s.isEmpty()).toList();
                stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
            }
        };

        if (stack == null) {
            return container;
        }

        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(container.getItems());
        }

        return container;
    }

    public static ItemStack getTarget(ItemStack stack) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        return contents != null
            ? contents.stream().findFirst().orElse(ItemStack.EMPTY)
            : ItemStack.EMPTY;
    }

    private static int getUpgradesCount(ItemStack stack, TRContent.Upgrades upgradeType) {
        Container inventory = getInventory(stack);
        AtomicInteger upgradesCount = new AtomicInteger();
        inventory.forEach(itemStack -> {
            if (itemStack.getItem() instanceof UpgradeItem && itemStack.is(upgradeType.item)) {
                upgradesCount.getAndIncrement();
            }
        });
        return upgradesCount.get();
    }

    @Override
    public int getUpgradeSlotCount() {
        return ALLOWED_UPGRADES;
    }

    @Override
    public boolean isUpgradeValid(IUpgrade upgrade, ItemStack stack) {
        return stack.is(TRContent.Upgrades.OVERCLOCKER.item) || stack.is(TRContent.Upgrades.ENERGY_STORAGE.item);
    }

    public static boolean isValidTarget(Item item) {
        return item instanceof BlockItem;
    }

    public static int computeScanCooldown(int overclockerUpgrades) {
        double speedMultiplier = TechRebornConfig.overclockerSpeed * overclockerUpgrades;
        double cooldown = TechExtensionsConfig.resonanceScannerScanCooldown * (1.0 - speedMultiplier);
        return (int)Math.round(cooldown);
    }

    public static long computeScanCost(ItemStack targetStack, int overclockerUpgrades) {
        int itemsInStack = targetStack.isEmpty() ? 0 : targetStack.getCount();
        long stackScanCost = TechExtensionsConfig.resonanceScannerBaseCost +
            (itemsInStack * TechExtensionsConfig.resonanceScannerPerItemCost);
        double powerMultiplier = 1;
        for (int i = 0; i < overclockerUpgrades; i++) {
            powerMultiplier = powerMultiplier * (1f + TechRebornConfig.overclockerPower);
        }
        return Math.round(stackScanCost * powerMultiplier);
    }

    public static long computeEffectiveRange(ItemStack targetStack) {
        return targetStack.isEmpty()
            ? 0
            : Math.round(
                TechExtensionsConfig.resonanceScannerBaseRange +
                    (TechExtensionsConfig.resonanceScannerRangeMultiplier * Math.log(targetStack.getCount()))
            );
    }

    private BlockPos findTargetBlock(ServerLevel worldIn, Player playerIn, Block targetBlock, long maxRange) {
        Vec3 start = playerIn.getEyePosition(1.0F);
        Vec3 lookVec = playerIn.getViewVector(1.0F).normalize();

        // The "Thickness" of the beam (Radius around the central line)
        // 3.5 = checks a 7x7ish area around the crosshair line
        double scanRadius = 3.5;
        // Step size: 1.0 is standard block steps
        double stepSize = 1.0;

        // Use MutableBlockPos to save memory allocation
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        // Walk forward along the view line
        for (double d = 0; d < maxRange; d += stepSize) {
            // Calculate the center point of the beam at distance 'd'
            Vec3 centerPoint = start.add(lookVec.scale(d));

            // Iterate a small box around this center point
            int r = (int) Math.ceil(scanRadius);

            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        checkPos.set(centerPoint.x + x, centerPoint.y + y, centerPoint.z + z);

                        // Simple distance check to keep it circular (cylinder) rather than square
                        if (checkPos.distToCenterSqr(centerPoint) > (scanRadius * scanRadius)) {
                            continue;
                        }

                        // For debugging purposes: draw the scan area
                         // worldIn.sendParticles(ParticleTypes.END_ROD,
                         //     checkPos.getX() + 0.5,
                         //     checkPos.getY() + 0.5,
                         //     checkPos.getZ() + 0.5,
                         //     1,
                         //     0.0, 0.0, 0.0,
                         //     0.0);

                        BlockState state = worldIn.getBlockState(checkPos);
                        if (state.is(targetBlock)) {
                            // FOUND IT!
                            return checkPos.immutable();
                        }
                    }
                }
            }
        }

        return null;
    }

    private static long estimateDistance(double exactDistance, long maxRange) {
        double exactDistancePercent = exactDistance / maxRange;
        double estimatedDistance = Arrays.stream(FIBONACCI_SEQUENCE)
            .filter(d -> d >= exactDistancePercent)
            .findFirst()
            .orElse(1.0) * maxRange;
        return Math.round(estimatedDistance);
    }
}

