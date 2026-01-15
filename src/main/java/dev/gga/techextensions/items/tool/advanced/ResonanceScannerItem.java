package dev.gga.techextensions.items.tool.advanced;

import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.menu.ResonanceScannerMenu;
import dev.gga.techextensions.init.TEItemSettings;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.item.component.CustomData;
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
            long currentTick = worldIn.getGameTime();
            long scanCooldown = getScanCooldown(stack);
            if (currentTick - lastDisplayTick < scanCooldown) {
                return;
            }
            lastDisplayTick = currentTick;
            // Compute cost based on items in target stack and overclocker upgrades
            long scanCost = getScanCost(stack);
            TRItemUtils.checkActive(stack, (int) scanCost, entityIn);
            if (!TRItemUtils.isActive(stack)) {
                // Scanner not active, remove any existing NBT data (display off)
                resetDistancePercent(stack);
                return;
            }
            // Consume energy, regardless of whether player is holding it or not
            tryUseEnergy(stack, scanCost);
            ItemStack targetStack = getTarget(stack);
            Item item = targetStack.getItem();
            // Ignore non-block items
            if (!isValidTarget(item)) {
                // Invalid target, remove any existing NBT data (display off)
                resetDistancePercent(stack);
                return;
            }
            long effectiveRange = computeEffectiveRange(targetStack);
            BlockPos foundPos = findTargetBlock(worldIn, playerIn, ((BlockItem) item).getBlock(), effectiveRange);
            if (foundPos == null) {
                // Block not found, remove any existing NBT data (display off)
                resetDistancePercent(stack);
                return;
            }
            // Only display info text and play sound if scanner is held in main/off hand
            if (stack.equals(playerIn.getMainHandItem()) || stack.equals(playerIn.getOffhandItem())) {
                // Compute the distance to the found block
                double distance = playerIn.position().distanceTo(Vec3.atCenterOf(foundPos));
                double estimatedDistancePercent = estimateDistancePercent(distance / (double) effectiveRange);
                // Write the current estimated distance to NBT for screen tinting
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putDouble("estimated_block_distance_percent", estimatedDistancePercent);
                });
                // Display message to player
                playerIn.displayClientMessage(
                    Component.translatable(
                        "techextensions.message.resonance_scanner.block_in_range",
                        Component.literal(item.getName().getString()).withStyle(ChatFormatting.GOLD),
                        Component.literal(Long.toString(Math.round(estimatedDistancePercent * effectiveRange))).withStyle(ChatFormatting.GOLD)
                    ).withStyle(ChatFormatting.GRAY),
                    true
                );
                // Play sound effect
                float pitch = 2.0F - (float)(estimatedDistancePercent * 1.5);
                worldIn.playSound(
                    null,
                    playerIn.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    playerIn.getSoundSource(),
                    0.5F,
                    pitch
                );
            }
        }
    }

    @Override
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        // Avoid animation when updating NBT data
        return false;
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
        return getEnergyCapacityFromCache(stack);
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
                updateCache(stack, this);
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

    private static void resetDistancePercent(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove("estimated_block_distance_percent");
        });
    }

    private static void updateCache(ItemStack stack, Container inventory) {
        int overclockers = 0;
        int energyStorage = 0;
        ItemStack targetStack = ItemStack.EMPTY;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (s.isEmpty()) continue;

            if (targetStack.isEmpty()) {
                targetStack = s;
            }

            if (s.getItem() instanceof UpgradeItem) {
                if (s.is(TRContent.Upgrades.OVERCLOCKER.item)) overclockers += s.getCount();
                if (s.is(TRContent.Upgrades.ENERGY_STORAGE.item)) energyStorage += s.getCount();
            }
        }

        final long scanCooldown = computeScanCooldown(overclockers);
        final long scanCost = computeScanCost(targetStack, overclockers);
        final long energyCapacity = TechExtensionsConfig.resonanceScannerCharge + (long)(energyStorage * TechRebornConfig.energyStoragePower);

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong("cache:scan_cooldown", scanCooldown);
            tag.putLong("cache:scan_cost", scanCost);
            tag.putLong("cache:energy_capacity", energyCapacity);
        });
    }

    public static int getScanCooldown(ItemStack stack) {
        return getCachedValue(stack, "cache:scan_cooldown", TechExtensionsConfig.resonanceScannerScanCooldown,
            tag -> tag.getInt("cache:scan_cooldown"));
    }

    public static long getScanCost(ItemStack stack) {
        return getCachedValue(stack, "cache:scan_cost", TechExtensionsConfig.resonanceScannerBaseCost,
            tag -> tag.getLong("cache:scan_cost"));
    }

    private static int getEnergyCapacityFromCache(ItemStack stack) {
        return getCachedValue(stack, "cache:energy_capacity", TechExtensionsConfig.resonanceScannerCharge,
            tag -> tag.getInt("cache:energy_capacity"));
    }

    private static <T> T getCachedValue(ItemStack stack, String key, T defaultValue, Function<CompoundTag, Optional<T>> extractor) {
        if (stack == null) {
            return defaultValue;
        }
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains(key)) {
            return extractor.apply(tag).orElse(defaultValue);
        }
        Container inv = getInventory(stack);
        updateCache(stack, inv);
        return extractor.apply(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()).orElse(defaultValue);
    }

    public static ItemStack getTarget(ItemStack stack) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        return contents != null
            ? contents.stream().findFirst().orElse(ItemStack.EMPTY)
            : ItemStack.EMPTY;
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
        double powerMultiplier = Math.pow(1f + TechRebornConfig.overclockerPower, overclockerUpgrades);
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

    public static double estimateDistancePercent(double exactDistancePercent) {
        return Arrays.stream(FIBONACCI_SEQUENCE)
            .filter(d -> d >= exactDistancePercent)
            .findFirst()
            .orElse(1.0);
    }
}

