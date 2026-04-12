package dev.gga.techextensions.items.tool.advanced;

import dev.gga.techextensions.component.TEDataComponentTypes;
import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.init.TEContent;
import dev.gga.techextensions.init.TEItemSettings;
import dev.gga.techextensions.menu.BubbleGunMenu;
import dev.gga.techextensions.utils.TECacheUtils;
import dev.gga.techextensions.utils.TECleaningUtils;
import java.util.Optional;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import reborncore.api.blockentity.IUpgrade;
import reborncore.api.blockentity.IUpgradeable;
import reborncore.common.powerSystem.RcEnergyItem;
import reborncore.common.powerSystem.RcEnergyTier;
import reborncore.common.util.ItemUtils;
import techreborn.config.TechRebornConfig;
import techreborn.init.TRContent;
import techreborn.items.UpgradeItem;

/**
 * A powered gun that shoots a stream of bubbles to clean blocks at a distance.
 *
 * <p>Features:
 * <ul>
 *   <li>Internal water tank (filled via cell slots in GUI or right-clicking water sources)</li>
 *   <li>Soap slot (consumes durability during cleaning)</li>
 *   <li>Two modes: INSPECT (opens GUI) and SHOOT (fires bubble stream)</li>
 *   <li>Upgradable with Overclocker and Energy Storage upgrades</li>
 * </ul>
 */
public class BubbleGunItem extends Item implements RcEnergyItem, IUpgradeable {
    public enum BubbleGunMode {
        INSPECT,
        SHOOT
    }

    /** Index 0: Soap slot */
    public static final int SOAP_SLOT = 0;
    /** Index 1: Cell input slot (water cells go in) */
    public static final int CELL_INPUT_SLOT = 1;
    /** Index 2: Cell output slot (empty cells come out) */
    public static final int CELL_OUTPUT_SLOT = 2;
    /** Total functional item slots (soap + cell input + cell output) */
    public static final int INVENTORY_SIZE = 3;
    /** Number of upgrade slots */
    public static final int ALLOWED_UPGRADES = 2;
    /** Total slots in the container */
    public static final int TOTAL_SLOTS = INVENTORY_SIZE + ALLOWED_UPGRADES;

    /** Millibuckets per bucket (used for world water source fill). */
    private static final long MB_PER_BUCKET = 1000;

    public final RcEnergyTier tier = RcEnergyTier.HIGH;

    public BubbleGunItem(String name) {
        super(TEItemSettings.item(name).durability(0));
    }

    // --- Mode handling ---

    public static BubbleGunMode getCurrentMode(ItemStack stack) {
        Integer mode = stack.get(TEDataComponentTypes.TOOL_MODE);
        if (mode == null) {
            return BubbleGunMode.INSPECT;
        }
        BubbleGunMode[] modes = BubbleGunMode.values();
        return mode >= 0 && mode < modes.length ? modes[mode] : BubbleGunMode.INSPECT;
    }

    private void switchMode(ItemStack stack, Player entity) {
        BubbleGunMode[] modes = BubbleGunMode.values();
        int nextMode = (getCurrentMode(stack).ordinal() + 1) % modes.length;
        stack.set(TEDataComponentTypes.TOOL_MODE, nextMode);
        if (entity instanceof ServerPlayer serverPlayer) {
            String modeTranslationKey =
                    switch (modes[nextMode]) {
                        case INSPECT -> "techextensions.message.bubble_gun.mode_inspect";
                        case SHOOT -> "techextensions.message.bubble_gun.mode_shoot";
                    };
            serverPlayer.sendSystemMessage(
                    Component.translatable("techextensions.message.set_to")
                            .withStyle(ChatFormatting.GRAY)
                            .append(" ")
                            .append(Component.translatable(modeTranslationKey).withStyle(ChatFormatting.GOLD)),
                    true);
        }
    }

    // --- RcEnergyItem ---

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

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return getEnergyCapacityFromCache(stack);
    }

    @Override
    public RcEnergyTier getTier() {
        return tier;
    }

    // --- Item overrides ---

    @Override
    public boolean allowComponentsUpdateAnimation(
            Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000; // Effectively infinite hold for shooting
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    // --- Interaction ---

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        // Check if player right-clicked on a water source block to fill the tank
        if (level.getFluidState(pos).getType() == Fluids.WATER
                && level.getFluidState(pos).isSource()) {
            if (!level.isClientSide()) {
                ItemStack stack = context.getItemInHand();
                long currentWater = getWaterAmount(stack);
                long maxWater = TechExtensionsConfig.bubbleGunWaterCapacity;
                if (currentWater < maxWater) {
                    long toFill = Math.min(MB_PER_BUCKET, maxWater - currentWater);
                    setWaterAmount(stack, currentWater + toFill);
                    level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
            return InteractionResult.CONSUME;
        }

        // Fall through to use() for mode-based logic
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(final Level world, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            switchMode(stack, player);
            return InteractionResult.CONSUME;
        }

        BubbleGunMode mode = getCurrentMode(stack);

        if (mode == BubbleGunMode.INSPECT) {
            if (!world.isClientSide()) {
                player.openMenu(new SimpleMenuProvider(
                        (syncId, inventory, _p) -> new BubbleGunMenu(syncId, inventory), stack.getHoverName()));
            }
            return InteractionResult.CONSUME;
        }

        // SHOOT mode — start using (hold to shoot)
        if (!world.isClientSide()) {
            // Validate conditions before starting
            if (getStoredEnergy(stack) < TechExtensionsConfig.bubbleGunEnergyCostPerTick) {
                world.playSound(
                        null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.2F);
                return InteractionResult.FAIL;
            }
            if (getWaterAmount(stack) < TechExtensionsConfig.bubbleGunWaterCostPerTick) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(
                            Component.translatable("techextensions.message.bubble_gun.no_water")
                                    .withStyle(ChatFormatting.RED),
                            true);
                }
                return InteractionResult.FAIL;
            }
            Container inv = getInventory(stack);
            ItemStack soapStack = inv.getItem(SOAP_SLOT);
            if (soapStack.isEmpty() || !(soapStack.getItem() instanceof SoapItem)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(
                            Component.translatable("techextensions.message.bubble_gun.no_soap")
                                    .withStyle(ChatFormatting.RED),
                            true);
                }
                return InteractionResult.FAIL;
            }
        }

        player.startUsingItem(hand);

        // Play a start-shooting sound
        if (!world.isClientSide()) {
            world.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT,
                    SoundSource.PLAYERS,
                    0.8F,
                    1.2F);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        // Check energy
        long energyCostPerTick = TechExtensionsConfig.bubbleGunEnergyCostPerTick;
        if (getStoredEnergy(stack) < energyCostPerTick) {
            entity.stopUsingItem();
            return;
        }

        // Check water
        long waterCostPerTick = TechExtensionsConfig.bubbleGunWaterCostPerTick;
        long currentWater = getWaterAmount(stack);
        if (currentWater < waterCostPerTick) {
            entity.stopUsingItem();
            return;
        }

        // Check soap
        Container inv = getInventory(stack);
        ItemStack soapStack = inv.getItem(SOAP_SLOT);
        if (soapStack.isEmpty() || !(soapStack.getItem() instanceof SoapItem)) {
            entity.stopUsingItem();
            return;
        }

        int soapDurabilityLeft = soapStack.getMaxDamage() - soapStack.getDamageValue();
        if (soapDurabilityLeft <= 0) {
            entity.stopUsingItem();
            return;
        }

        // Consume resources
        tryUseEnergy(stack, energyCostPerTick);
        setWaterAmount(stack, currentWater - waterCostPerTick);

        // Spawn bubble particles along the look direction
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F).normalize();
        double range = TechExtensionsConfig.bubbleGunRange;

        for (double d = 1.0; d < range; d += 0.8) {
            Vec3 particlePos = eyePos.add(lookVec.scale(d));
            serverLevel.sendParticles(
                    ParticleTypes.BUBBLE, particlePos.x, particlePos.y, particlePos.z, 1, 0.05, 0.05, 0.05, 0.01);
        }

        // Play bubble sound every 10 ticks
        int usedTicks = getUseDuration(stack, entity) - remainingUseTicks;
        if (usedTicks % 10 == 0) {
            level.playSound(
                    null,
                    entity.blockPosition(),
                    SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                    SoundSource.PLAYERS,
                    0.5F,
                    1.0F + level.getRandom().nextFloat() * 0.3F);
        }

        // Raytrace to find targeted block or entity
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        // Entity raytrace — find closest living entity along line of sight
        AABB searchBox =
                player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0D);
        LivingEntity closestEntity = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Entity candidate : level.getEntities(player, searchBox, e -> e.isAlive() && !e.isSpectator())) {
            if (!(candidate instanceof LivingEntity living)) continue;
            // Skip entities already trapped in a bubble
            if (candidate.entityTags().contains("techextensions:bubble_trapped")) continue;
            AABB entityBB = candidate.getBoundingBox().inflate(candidate.getPickRadius() + 0.3D);
            Optional<Vec3> intersection = entityBB.clip(eyePos, endPos);
            if (intersection.isPresent()) {
                double distSq = eyePos.distanceToSqr(intersection.get());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closestEntity = living;
                }
            }
        }

        // Block raytrace
        BlockHitResult hitResult =
                level.clip(new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        // Determine if entity or block is closer
        boolean entityCloser = false;
        if (closestEntity != null) {
            if (hitResult.getType() == HitResult.Type.MISS) {
                entityCloser = true;
            } else {
                double blockDistSq = eyePos.distanceToSqr(hitResult.getLocation());
                entityCloser = closestDistSq < blockDistSq;
            }
        }

        if (entityCloser) {
            // Reset block cleaning state since we're targeting an entity
            resetCleaningState(stack);

            // Track entity trapping progress
            int entityId = closestEntity.getId();
            int prevTargetId = getTrappingTargetId(stack);
            int trapProgress = getTrappingProgress(stack);

            if (prevTargetId == entityId) {
                trapProgress++;
            } else {
                trapProgress = 1;
            }

            setTrappingTargetId(stack, entityId);
            setTrappingProgress(stack, trapProgress);

            // Spawn bubble particles around the targeted entity to show it's being hit
            serverLevel.sendParticles(
                    ParticleTypes.BUBBLE,
                    closestEntity.getX(),
                    closestEntity.getY() + closestEntity.getBbHeight() * 0.5,
                    closestEntity.getZ(),
                    5,
                    closestEntity.getBbWidth() * 0.4,
                    closestEntity.getBbHeight() * 0.4,
                    closestEntity.getBbWidth() * 0.4,
                    0.02);

            // Trap the entity once the threshold is met
            if (trapProgress >= TechExtensionsConfig.bubbleGunTrapDuration) {
                dev.gga.techextensions.entity.BubbleTrapEntity bubble =
                        new dev.gga.techextensions.entity.BubbleTrapEntity(TEContent.BUBBLE_TRAP_ENTITY, serverLevel);
                bubble.setPos(closestEntity.getX(), closestEntity.getY(), closestEntity.getZ());
                bubble.setTrappedEntity(closestEntity);
                serverLevel.addFreshEntity(bubble);

                // Damage the soap
                soapStack.hurtAndBreak(2, entity, EquipmentSlot.MAINHAND);
                if (soapStack.isEmpty()) {
                    inv.setItem(SOAP_SLOT, ItemStack.EMPTY);
                }
                inv.setChanged();

                // Reset progress
                resetTrappingState(stack);
            }
        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            // Reset entity trapping state since we're targeting a block
            resetTrappingState(stack);

            BlockPos hitPos = hitResult.getBlockPos();

            if (TECleaningUtils.isCleanable(level, hitPos)) {
                // Track cleaning progress on the targeted block
                BlockPos prevTarget = getCleaningTarget(stack);
                int progress = getCleaningProgress(stack);

                if (prevTarget != null && prevTarget.equals(hitPos)) {
                    progress++;
                } else {
                    progress = 1;
                }

                setCleaningTarget(stack, hitPos);
                setCleaningProgress(stack, progress);

                // Clean the block once the threshold is met
                if (progress >= TechExtensionsConfig.bubbleGunCleanDuration) {
                    if (TECleaningUtils.cleanBlock(level, hitPos)) {
                        level.playSound(null, hitPos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);

                        // Damage the soap inside the gun's inventory
                        soapStack.hurtAndBreak(1, entity, EquipmentSlot.MAINHAND);
                        if (soapStack.isEmpty()) {
                            inv.setItem(SOAP_SLOT, ItemStack.EMPTY);
                        }
                        inv.setChanged();
                    }
                    // Reset progress after cleaning
                    setCleaningProgress(stack, 0);
                    setCleaningTarget(stack, null);
                }
            } else {
                // Not a cleanable block — reset progress
                resetCleaningState(stack);
            }
        } else {
            // Missed — reset both progress states
            resetCleaningState(stack);
            resetTrappingState(stack);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        resetCleaningState(stack);
        resetTrappingState(stack);
        return super.releaseUsing(stack, level, entity, timeLeft);
    }

    // --- Inventory tick: process cell input ---

    @Override
    public void inventoryTick(
            ItemStack stack,
            ServerLevel world,
            net.minecraft.world.entity.Entity entity,
            net.minecraft.world.entity.EquipmentSlot slot) {
        if (world.isClientSide()) {
            return;
        }

        // When the bubble gun menu is open, use its live container so that
        // changes here are visible in the GUI and won't be overwritten on close.
        Container inv;
        if (entity instanceof Player player
                && player.containerMenu instanceof dev.gga.techextensions.menu.BubbleGunMenu bubbleGunMenu) {
            inv = bubbleGunMenu.getGunInventory();
        } else {
            inv = getInventory(stack);
        }

        ItemStack inputItem = inv.getItem(CELL_INPUT_SLOT);

        if (!inputItem.isEmpty()) {
            long currentWater = getWaterAmount(stack);
            long maxWater = TechExtensionsConfig.bubbleGunWaterCapacity;

            if (currentWater < maxWater) {
                // Use Fabric Transfer API to properly drain fluid from the input container,
                // moving the emptied container to the output slot (like TR fluid tanks).
                long extracted = drainFluidContainer(inv, maxWater - currentWater);
                if (extracted > 0) {
                    setWaterAmount(stack, currentWater + extracted);
                    inv.setChanged();
                }
            }
        }
    }

    /**
     * Drains water from the item in the input slot using the Fabric Transfer API.
     * The emptied container (empty cell, empty bucket, etc.) is moved to the output slot.
     *
     * @param inv        the gun's internal inventory
     * @param maxDrainMb maximum amount of water (in mB) to drain
     * @return the amount of water drained in mB, or 0 if nothing was drained
     */
    private static long drainFluidContainer(Container inv, long maxDrainMb) {
        ItemStack inputStack = inv.getItem(CELL_INPUT_SLOT);
        if (inputStack.isEmpty()) return 0;

        // Work on a single-item copy to simulate the extraction and discover
        // what the empty container looks like (empty cell, empty bucket, etc.)
        ItemStack singleInput = inputStack.copyWithCount(1);
        ItemStack[] held = {singleInput};
        SingleStackStorage slotStorage = new SingleStackStorage() {
            @Override
            protected ItemStack getStack() {
                return held[0];
            }

            @Override
            protected void setStack(ItemStack stack) {
                held[0] = stack;
            }
        };

        ContainerItemContext ctx = ContainerItemContext.ofSingleSlot(slotStorage);
        Storage<FluidVariant> fluidStorage = ctx.find(FluidStorage.ITEM);
        if (fluidStorage == null) return 0;

        long maxDrainDroplets = maxDrainMb * FluidConstants.BUCKET / 1000;

        try (Transaction tx = Transaction.openOuter()) {
            long totalExtracted = 0;

            for (StorageView<FluidVariant> view : fluidStorage) {
                if (view.isResourceBlank()) continue;
                if (view.getResource().getFluid() != Fluids.WATER) continue;

                long extracted = view.extract(view.getResource(), maxDrainDroplets - totalExtracted, tx);
                totalExtracted += extracted;
                if (totalExtracted >= maxDrainDroplets) break;
            }

            if (totalExtracted > 0) {
                tx.commit();

                // After extraction, held[0] contains the empty container variant
                ItemStack emptyContainer = held[0];

                // Move empty container to output slot (if any)
                if (!emptyContainer.isEmpty()) {
                    ItemStack outputSlot = inv.getItem(CELL_OUTPUT_SLOT);
                    if (outputSlot.isEmpty()) {
                        inv.setItem(CELL_OUTPUT_SLOT, emptyContainer);
                    } else if (ItemStack.isSameItemSameComponents(outputSlot, emptyContainer)
                            && outputSlot.getCount() < outputSlot.getMaxStackSize()) {
                        outputSlot.grow(1);
                    } else {
                        return 0; // Output full, cannot process
                    }
                }

                // Consume one item from the input slot
                inputStack.shrink(1);
                if (inputStack.isEmpty()) {
                    inv.setItem(CELL_INPUT_SLOT, ItemStack.EMPTY);
                }

                return dropletsToMb(totalExtracted);
            }
        }

        return 0;
    }

    // --- Fluid API helpers ---

    /**
     * Converts Fabric API droplets to millibuckets.
     * 1 bucket = {@link FluidConstants#BUCKET} droplets = 1000 mB.
     */
    private static long dropletsToMb(long droplets) {
        return droplets * 1000 / FluidConstants.BUCKET;
    }

    // --- IUpgradeable ---

    @Override
    public boolean canBeUpgraded() {
        return true;
    }

    @Override
    public Container getUpgradeInventory() {
        return null; // Cannot be implemented for Item
    }

    @Override
    public int getUpgradeSlotCount() {
        return ALLOWED_UPGRADES;
    }

    @Override
    public boolean isUpgradeValid(IUpgrade upgrade, ItemStack stack) {
        return stack.is(TRContent.Upgrades.OVERCLOCKER.item) || stack.is(TRContent.Upgrades.ENERGY_STORAGE.item);
    }

    // --- Inventory management ---

    /**
     * Returns a container backed by the stack's {@code DataComponents.CONTAINER} data.
     */
    public static Container getInventory(ItemStack stack) {
        SimpleContainer container = new SimpleContainer(TOTAL_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
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
        }

        return container;
    }

    // --- Water storage ---

    public static long getWaterAmount(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains("water_amount")) {
            return tag.getLong("water_amount").orElse(0L);
        }
        return 0;
    }

    public static void setWaterAmount(ItemStack stack, long amount) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong("water_amount", Math.max(0, amount));
        });
    }

    // --- Cleaning state tracking ---

    private static BlockPos getCleaningTarget(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains("cleaning_target")) {
            return BlockPos.of(tag.getLong("cleaning_target").orElse(0L));
        }
        return null;
    }

    private static void setCleaningTarget(ItemStack stack, BlockPos pos) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (pos == null) {
                tag.remove("cleaning_target");
            } else {
                tag.putLong("cleaning_target", pos.asLong());
            }
        });
    }

    private static int getCleaningProgress(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains("cleaning_progress")) {
            return tag.getInt("cleaning_progress").orElse(0);
        }
        return 0;
    }

    private static void setCleaningProgress(ItemStack stack, int progress) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt("cleaning_progress", progress);
        });
    }

    private static void resetCleaningState(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove("cleaning_target");
            tag.remove("cleaning_progress");
        });
    }

    // --- Entity trapping state tracking ---

    private static int getTrappingTargetId(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains("trapping_target_id")) {
            return tag.getInt("trapping_target_id").orElse(-1);
        }
        return -1;
    }

    private static void setTrappingTargetId(ItemStack stack, int entityId) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt("trapping_target_id", entityId);
        });
    }

    private static int getTrappingProgress(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains("trapping_progress")) {
            return tag.getInt("trapping_progress").orElse(0);
        }
        return 0;
    }

    private static void setTrappingProgress(ItemStack stack, int progress) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt("trapping_progress", progress);
        });
    }

    private static void resetTrappingState(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove("trapping_target_id");
            tag.remove("trapping_progress");
        });
    }

    // --- Cached upgrade values ---

    private static void updateCache(ItemStack stack, Container inventory) {
        int overclockers = 0;
        int energyStorage = 0;

        for (int i = INVENTORY_SIZE; i < TOTAL_SLOTS; i++) {
            ItemStack s = inventory.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof UpgradeItem) {
                if (s.is(TRContent.Upgrades.OVERCLOCKER.item)) overclockers += s.getCount();
                if (s.is(TRContent.Upgrades.ENERGY_STORAGE.item)) energyStorage += s.getCount();
            }
        }

        final long energyCapacity =
                TechExtensionsConfig.bubbleGunCharge + (long) (energyStorage * TechRebornConfig.energyStoragePower);

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong("cache:energy_capacity", energyCapacity);
        });
    }

    private static int getEnergyCapacityFromCache(ItemStack stack) {
        return TECacheUtils.getCachedValue(
                stack,
                "cache:energy_capacity",
                TechExtensionsConfig.bubbleGunCharge,
                tag -> tag.getInt("cache:energy_capacity"),
                BubbleGunItem::getInventory,
                BubbleGunItem::updateCache);
    }
}
