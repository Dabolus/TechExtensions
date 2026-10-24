package dev.gga.techextensions.items.tool.advanced;

import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.init.TEItemSettings;
import dev.gga.techextensions.menu.ResonanceScannerMenu;
import dev.gga.techextensions.utils.TECacheUtils;
import dev.gga.techextensions.utils.TEContainerUtils;
import dev.gga.techextensions.utils.TENbtUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Arrays;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
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
    /** Index 0: the block to look for */
    public static final int TARGET_SLOT = 0;
    /** Functional item slots (just the target) */
    public static final int INVENTORY_SIZE = 1;
    /** Number of upgrade slots */
    public static final int ALLOWED_UPGRADES = 2;
    /** Total slots in the container */
    public static final int TOTAL_SLOTS = INVENTORY_SIZE + ALLOWED_UPGRADES;

    /** CustomData key of the last estimated distance, read by the screen tint on the client. */
    public static final String DISTANCE_PERCENT_KEY = "estimated_block_distance_percent";

    private static final String SCAN_COOLDOWN_CACHE_KEY = "cache:scan_cooldown";
    private static final String SCAN_COST_CACHE_KEY = "cache:scan_cost";
    private static final String ENERGY_CAPACITY_CACHE_KEY = "cache:energy_capacity";

    // Precomputed Fibonacci percentages, capped at 100% of max range
    private static final double[] FIBONACCI_SEQUENCE = {0.01, 0.02, 0.03, 0.05, 0.08, 0.13, 0.21, 0.34, 0.55, 1};

    /** "Thickness" of the beam (radius around the central line): 3.5 checks a 7x7ish area around the crosshair. */
    private static final double SCAN_RADIUS = 3.5;
    /** Step size along the view line: 1.0 is standard block steps. */
    private static final double SCAN_STEP = 1.0;
    /** Block offsets (x, y, z triples) inside the scan sphere, computed once instead of at every step. */
    private static final int[] SCAN_OFFSETS = buildScanOffsets();

    public final RcEnergyTier tier = RcEnergyTier.HIGH;

    public ResonanceScannerItem(String name) {
        super(TEItemSettings.item(name).durability(0));
    }

    private static int[] buildScanOffsets() {
        int r = (int) Math.ceil(SCAN_RADIUS);
        double radiusSq = SCAN_RADIUS * SCAN_RADIUS;
        IntArrayList offsets = new IntArrayList();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    // Keep it spherical (a cylinder along the beam) rather than cubic
                    if (x * x + y * y + z * z <= radiusSq) {
                        offsets.add(x);
                        offsets.add(y);
                        offsets.add(z);
                    }
                }
            }
        }
        return offsets.toIntArray();
    }

    // Item
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel worldIn, Entity entityIn, @Nullable EquipmentSlot slot) {
        if (!(entityIn instanceof ServerPlayer playerIn)) {
            return;
        }
        // Scan on a per-stack cadence derived from the game time: no state shared between stacks or players
        int scanCooldown = Math.max(1, getScanCooldown(stack));
        if (worldIn.getGameTime() % scanCooldown != 0) {
            return;
        }
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
        // Only a held scanner shows the result, so only a held scanner pays for the actual scan
        if (stack != playerIn.getMainHandItem() && stack != playerIn.getOffhandItem()) {
            resetDistancePercent(stack);
            return;
        }
        ItemStack targetStack = getTarget(stack);
        Item item = targetStack.getItem();
        // Ignore non-block items
        if (!isValidTarget(item)) {
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
        // Compute the distance to the found block
        double distance = playerIn.position().distanceTo(Vec3.atCenterOf(foundPos));
        double estimatedDistancePercent = estimateDistancePercent(distance / (double) effectiveRange);
        // Write the current estimated distance to NBT for screen tinting
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.putDouble(DISTANCE_PERCENT_KEY, estimatedDistancePercent));
        // Display message to player
        playerIn.sendSystemMessage(
                Component.translatable(
                                "techextensions.message.resonance_scanner.block_in_range",
                                Component.literal(item.getName(targetStack).getString())
                                        .withStyle(ChatFormatting.GOLD),
                                Component.literal(Long.toString(Math.round(estimatedDistancePercent * effectiveRange)))
                                        .withStyle(ChatFormatting.GOLD))
                        .withStyle(ChatFormatting.GRAY),
                true);
        // Play sound effect
        float pitch = 2.0F - (float) (estimatedDistancePercent * 1.5);
        worldIn.playSound(
                null,
                playerIn.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                playerIn.getSoundSource(),
                0.5F,
                pitch);
    }

    @Override
    public boolean allowComponentsUpdateAnimation(
            Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
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
        }
        if (!world.isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (syncId, inventory, _p) -> new ResonanceScannerMenu(syncId, inventory), stack.getHoverName()));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level world, LivingEntity entity, ItemStack stack, int i) {
        TRItemUtils.checkActive(stack, (int) getScanCost(stack), entity);
    }

    // IUpgradeable
    @Override
    public boolean canBeUpgraded() {
        return true;
    }

    @Override
    public Container getUpgradeInventory() {
        return null; // Cannot be implemented for Item
    }

    /** Returns a container (target + upgrades) backed by the stack's `DataComponents.CONTAINER` data. */
    public static Container getInventory(ItemStack stack) {
        SimpleContainer container = new SimpleContainer(TOTAL_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                // Save every slot by index, so upgrades never slide into the target slot
                stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
                updateCache(stack, this);
            }
        };

        if (stack == null) {
            return container;
        }

        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(container.getItems());
            migrateCompactedUpgrade(container);
        }

        return container;
    }

    /** Older versions saved the container compacted, which could leave an upgrade in the target slot. */
    private static void migrateCompactedUpgrade(SimpleContainer container) {
        ItemStack target = container.getItem(TARGET_SLOT);
        if (!(target.getItem() instanceof IUpgrade)) {
            return;
        }
        for (int i = INVENTORY_SIZE; i < TOTAL_SLOTS; i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, target.copy());
                container.setItem(TARGET_SLOT, ItemStack.EMPTY);
                return;
            }
        }
    }

    private static void resetDistancePercent(ItemStack stack) {
        if (TENbtUtils.view(stack).contains(DISTANCE_PERCENT_KEY)) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(DISTANCE_PERCENT_KEY));
        }
    }

    private static void updateCache(ItemStack stack, Container inventory) {
        int overclockers = 0;
        int energyStorage = 0;

        for (int i = INVENTORY_SIZE; i < TOTAL_SLOTS; i++) {
            ItemStack s = inventory.getItem(i);
            if (s.isEmpty() || !(s.getItem() instanceof UpgradeItem)) continue;
            if (s.is(TRContent.Upgrades.OVERCLOCKER.item)) overclockers += s.getCount();
            if (s.is(TRContent.Upgrades.ENERGY_STORAGE.item)) energyStorage += s.getCount();
        }

        final ItemStack targetStack = inventory.getItem(TARGET_SLOT);
        final int scanCooldown = computeScanCooldown(overclockers);
        final long scanCost = computeScanCost(targetStack, overclockers);
        final long energyCapacity = TechExtensionsConfig.resonanceScannerCharge.get()
                + (long) (energyStorage * TechRebornConfig.energyStoragePower.get());

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(SCAN_COOLDOWN_CACHE_KEY, scanCooldown);
            tag.putLong(SCAN_COST_CACHE_KEY, scanCost);
            tag.putLong(ENERGY_CAPACITY_CACHE_KEY, energyCapacity);
        });
    }

    public static int getScanCooldown(ItemStack stack) {
        return TECacheUtils.getCachedValue(
                stack,
                SCAN_COOLDOWN_CACHE_KEY,
                TechExtensionsConfig.resonanceScannerScanCooldown.get(),
                tag -> tag.getInt(SCAN_COOLDOWN_CACHE_KEY),
                ResonanceScannerItem::getInventory,
                ResonanceScannerItem::updateCache);
    }

    public static long getScanCost(ItemStack stack) {
        return TECacheUtils.getCachedValue(
                stack,
                SCAN_COST_CACHE_KEY,
                TechExtensionsConfig.resonanceScannerBaseCost.get(),
                tag -> tag.getLong(SCAN_COST_CACHE_KEY),
                ResonanceScannerItem::getInventory,
                ResonanceScannerItem::updateCache);
    }

    private static long getEnergyCapacityFromCache(ItemStack stack) {
        return TECacheUtils.getCachedValue(
                stack,
                ENERGY_CAPACITY_CACHE_KEY,
                (long) TechExtensionsConfig.resonanceScannerCharge.get(),
                tag -> tag.getLong(ENERGY_CAPACITY_CACHE_KEY),
                ResonanceScannerItem::getInventory,
                ResonanceScannerItem::updateCache);
    }

    /** The block the scanner looks for (a copy of the target slot). */
    public static ItemStack getTarget(ItemStack stack) {
        return TEContainerUtils.copySlot(stack, TARGET_SLOT);
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
        double speedMultiplier = TechRebornConfig.overclockerSpeed.get() * overclockerUpgrades;
        double cooldown = TechExtensionsConfig.resonanceScannerScanCooldown.get() * (1.0 - speedMultiplier);
        return Math.max(1, (int) Math.round(cooldown));
    }

    public static long computeScanCost(ItemStack targetStack, int overclockerUpgrades) {
        int itemsInStack = targetStack.isEmpty() ? 0 : targetStack.getCount();
        long stackScanCost = TechExtensionsConfig.resonanceScannerBaseCost.get()
                + (itemsInStack * TechExtensionsConfig.resonanceScannerPerItemCost.get());
        double powerMultiplier = Math.pow(1f + TechRebornConfig.overclockerPower.get(), overclockerUpgrades);
        return Math.round(stackScanCost * powerMultiplier);
    }

    public static long computeEffectiveRange(ItemStack targetStack) {
        return targetStack.isEmpty()
                ? 0
                : Math.round(TechExtensionsConfig.resonanceScannerBaseRange.get()
                        + (TechExtensionsConfig.resonanceScannerRangeMultiplier.get()
                                * Math.log(targetStack.getCount())));
    }

    /** Walks a cylinder along the player's view line and returns the first matching block. */
    @Nullable
    private static BlockPos findTargetBlock(ServerLevel worldIn, Player playerIn, Block targetBlock, long maxRange) {
        Vec3 start = playerIn.getEyePosition(1.0F);
        Vec3 lookVec = playerIn.getViewVector(1.0F).normalize();

        // Consecutive steps overlap almost entirely, so remember which positions were already checked
        LongOpenHashSet visited = new LongOpenHashSet();
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        for (double d = 0; d < maxRange; d += SCAN_STEP) {
            Vec3 centerPoint = start.add(lookVec.scale(d));
            int centerX = Mth.floor(centerPoint.x);
            int centerY = Mth.floor(centerPoint.y);
            int centerZ = Mth.floor(centerPoint.z);

            for (int i = 0; i < SCAN_OFFSETS.length; i += 3) {
                checkPos.set(centerX + SCAN_OFFSETS[i], centerY + SCAN_OFFSETS[i + 1], centerZ + SCAN_OFFSETS[i + 2]);
                if (!visited.add(checkPos.asLong())) {
                    continue;
                }
                // Never force-load chunks for a scan
                if (!worldIn.isLoaded(checkPos)) {
                    continue;
                }
                if (worldIn.getBlockState(checkPos).is(targetBlock)) {
                    return checkPos.immutable();
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
