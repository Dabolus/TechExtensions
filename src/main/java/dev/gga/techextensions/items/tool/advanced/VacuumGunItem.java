package dev.gga.techextensions.items.tool.advanced;

import dev.gga.techextensions.component.TEDataComponentTypes;
import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.init.TEItemSettings;
import dev.gga.techextensions.menu.VacuumGunMenu;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import reborncore.api.blockentity.IUpgrade;
import reborncore.api.blockentity.IUpgradeable;
import reborncore.common.powerSystem.RcEnergyItem;
import reborncore.common.powerSystem.RcEnergyTier;
import reborncore.common.util.ItemUtils;
import techreborn.config.TechRebornConfig;
import techreborn.entities.EntityNukePrimed;
import techreborn.init.TRContent;
import techreborn.items.UpgradeItem;

public class VacuumGunItem extends Item implements RcEnergyItem, IUpgradeable {
    public enum VacuumGunMode {
        VACUUM,
        BLOW,
        INSPECT
    }

    public static final int INVENTORY_SIZE = 5;
    public static final int ALLOWED_UPGRADES = 2;
    public static final int TOTAL_SLOTS = INVENTORY_SIZE + ALLOWED_UPGRADES;

    // Trajectory physics constants
    private static final double TRAJECTORY_GRAVITY = 0.05;
    private static final double TRAJECTORY_DRAG = 0.99;
    private static final double LAUNCH_POWER = 1.5;
    private static final int MAX_TRAJECTORY_TICKS = 200;

    // Travel speed for vacuum/blow animations in blocks per tick
    private static final double ANIMATION_SPEED = 1.5;

    // Currently active travel animations
    private static final List<AnimatedTask> ACTIVE_TASKS = new ArrayList<>();

    /**
     * Represents an in-flight item traveling along a path. A
     * `ItemDisplay` entity follows the waypoints and the
     * `onComplete` callback fires when the animation finishes.
     */
    private static class AnimatedTask {
        final ServerLevel world;
        final List<Vec3> path;
        final int durationTicks;
        final Runnable onComplete;
        final Display.ItemDisplay displayEntity;
        int currentTick = 0;

        AnimatedTask(ServerLevel world, List<Vec3> path, ItemStack displayItem, Runnable onComplete) {
            this.world = world;
            this.path = path;
            // Compute duration from total path length and travel speed
            double totalLength = 0;
            for (int i = 0; i < path.size() - 1; i++) {
                totalLength += path.get(i).distanceTo(path.get(i + 1));
            }
            this.durationTicks = Math.max(1, (int) Math.ceil(totalLength / ANIMATION_SPEED));
            this.onComplete = onComplete;

            Vec3 start = path.getFirst();
            this.displayEntity = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, world);
            this.displayEntity.setItemStack(displayItem.copy());
            this.displayEntity.setNoGravity(true);
            this.displayEntity.setInvulnerable(true);
            this.displayEntity.setPos(start.x, start.y, start.z);
            world.addFreshEntity(this.displayEntity);
        }
    }

    /**
     * Interpolates a position along a list of waypoints. `progress`
     * ranges from 0.0 (first point) to 1.0 (last point).
     */
    private static Vec3 interpolatePath(List<Vec3> path, double progress) {
        if (path.size() < 2) return path.getFirst();

        // Compute total path length
        double totalLength = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            totalLength += path.get(i).distanceTo(path.get(i + 1));
        }

        double targetDist = totalLength * Math.min(1.0, Math.max(0.0, progress));
        double accumulated = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            double segLen = path.get(i).distanceTo(path.get(i + 1));
            if (accumulated + segLen >= targetDist) {
                double segProgress = segLen > 0 ? (targetDist - accumulated) / segLen : 0;
                return path.get(i).lerp(path.get(i + 1), segProgress);
            }
            accumulated += segLen;
        }
        return path.getLast();
    }

    public final RcEnergyTier tier = RcEnergyTier.HIGH;

    public VacuumGunItem(String name) {
        super(TEItemSettings.item(name).durability(0));
    }

    // Mode handling

    public static VacuumGunMode getCurrentMode(ItemStack stack) {
        Integer mode = stack.get(TEDataComponentTypes.TOOL_MODE);
        if (mode == null) {
            return VacuumGunMode.VACUUM;
        }
        VacuumGunMode[] modes = VacuumGunMode.values();
        return mode >= 0 && mode < modes.length ? modes[mode] : VacuumGunMode.VACUUM;
    }

    private void switchMode(ItemStack stack, Player entity) {
        VacuumGunMode[] modes = VacuumGunMode.values();
        int nextMode = (getCurrentMode(stack).ordinal() + 1) % modes.length;
        stack.set(TEDataComponentTypes.TOOL_MODE, nextMode);
        if (entity instanceof ServerPlayer serverPlayer) {
            String modeText =
                    switch (modes[nextMode]) {
                        case VACUUM -> "Vacuum";
                        case BLOW -> "Blow";
                        case INSPECT -> "Inspect";
                    };
            serverPlayer.displayClientMessage(
                    Component.translatable("techextensions.message.setTo")
                            .withStyle(ChatFormatting.GRAY)
                            .append(" ")
                            .append(Component.literal(modeText).withStyle(ChatFormatting.GOLD)),
                    true);
        }
    }

    // Trajectory computation

    /**
     * Result of a trajectory simulation. Represents where a projectile
     * following arrow-like physics would impact.
     */
    private record TrajectoryResult(
            BlockPos hitBlock,
            BlockPos adjacentPos,
            Direction hitFace,
            Vec3 hitLocation,
            Vec3 velocity,
            List<Vec3> path) {}

    /**
     * Simulates an arrow-like trajectory from the player's eye position along
     * the look direction, returning where the projectile would hit a block.
     */
    private static TrajectoryResult computeTrajectory(ServerLevel world, Player player) {
        Vec3 pos = player.getEyePosition(1.0F);
        Vec3 vel = player.getViewVector(1.0F).normalize().scale(LAUNCH_POWER);
        List<Vec3> path = new ArrayList<>();
        path.add(pos);

        for (int tick = 0; tick < MAX_TRAJECTORY_TICKS; tick++) {
            Vec3 nextPos = pos.add(vel);

            // Raytrace between current and next position
            BlockHitResult hit = world.clip(new ClipContext(
                    pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));

            if (hit.getType() != HitResult.Type.MISS) {
                path.add(hit.getLocation());
                return new TrajectoryResult(
                        hit.getBlockPos(),
                        hit.getBlockPos().relative(hit.getDirection()),
                        hit.getDirection(),
                        hit.getLocation(),
                        vel,
                        path);
            }

            pos = nextPos;
            path.add(pos);
            vel = new Vec3(vel.x, vel.y - TRAJECTORY_GRAVITY, vel.z).scale(TRAJECTORY_DRAG);

            // Below world boundary
            if (pos.y < world.getMinY()) {
                BlockPos lastPos = BlockPos.containing(pos);
                return new TrajectoryResult(lastPos, lastPos.above(), Direction.UP, pos, vel, path);
            }
        }

        // Max range reached
        BlockPos lastPos = BlockPos.containing(pos);
        return new TrajectoryResult(lastPos, lastPos.above(), Direction.UP, pos, vel, path);
    }

    // ── Animation tick processing ────────────────────────────────────────

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        if (world.isClientSide() || ACTIVE_TASKS.isEmpty()) return;

        Iterator<AnimatedTask> it = ACTIVE_TASKS.iterator();
        while (it.hasNext()) {
            AnimatedTask task = it.next();
            task.currentTick++;
            double progress = (double) task.currentTick / task.durationTicks;
            progress = Math.min(1.0, progress);

            // Move display entity along the path
            Vec3 pos = interpolatePath(task.path, progress);
            task.displayEntity.setPos(pos);

            // Spawn firework trail particles between previous and current position
            double prevProgress = (double) (task.currentTick - 1) / task.durationTicks;
            Vec3 prevPos = interpolatePath(task.path, Math.max(0, prevProgress));
            Vec3 seg = pos.subtract(prevPos);
            double segLen = seg.length();
            int particleSteps = Math.max(1, (int) Math.ceil(segLen / 0.5));
            for (int p = 0; p < particleSteps; p++) {
                double t = (double) p / particleSteps;
                task.world.sendParticles(
                        ParticleTypes.FIREWORK,
                        prevPos.x + seg.x * t,
                        prevPos.y + seg.y * t,
                        prevPos.z + seg.z * t,
                        1,
                        0,
                        0,
                        0,
                        0);
            }

            if (task.currentTick >= task.durationTicks) {
                task.displayEntity.discard();
                task.onComplete.run();
                it.remove();
            }
        }
    }

    // ── Item overrides ──────────────────────────────────────────────────────

    @Override
    public boolean allowComponentsUpdateAnimation(
            Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
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

    // ── RcEnergyItem ────────────────────────────────────────────────────────

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return getEnergyCapacityFromCache(stack);
    }

    @Override
    public RcEnergyTier getTier() {
        return tier;
    }

    // ── Interaction ─────────────────────────────────────────────────────────

    @Override
    public InteractionResult use(final Level world, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            switchMode(stack, player);
            return InteractionResult.CONSUME;
        }

        if (world.isClientSide()) {
            return InteractionResult.CONSUME;
        }

        ServerLevel serverLevel = (ServerLevel) world;

        // Cooldown check
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }

        // Energy check
        long cost = TechExtensionsConfig.vacuumGunCostPerAction;
        if (getStoredEnergy(stack) < cost) {
            world.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.2F);
            return InteractionResult.FAIL;
        }

        VacuumGunMode mode = getCurrentMode(stack);
        boolean didAction =
                switch (mode) {
                    case VACUUM -> performVacuum(serverLevel, player, stack);
                    case BLOW -> performBlow(serverLevel, player, stack);
                    case INSPECT -> {
                        player.openMenu(new SimpleMenuProvider(
                                (syncId, inventory, _p) -> new VacuumGunMenu(syncId, inventory), stack.getHoverName()));
                        yield true;
                    }
                };

        if (didAction) {
            tryUseEnergy(stack, cost);
            player.getCooldowns().addCooldown(stack, getCooldown(stack));
        }

        return InteractionResult.CONSUME;
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

    @Override
    public int getUpgradeSlotCount() {
        return ALLOWED_UPGRADES;
    }

    @Override
    public boolean isUpgradeValid(IUpgrade upgrade, ItemStack stack) {
        return stack.is(TRContent.Upgrades.OVERCLOCKER.item) || stack.is(TRContent.Upgrades.ENERGY_STORAGE.item);
    }

    // Inventory management

    /**
     * Returns a 7-slot container (5 inventory + 2 upgrades) backed by the
     * stack's `DataComponents.CONTAINER` data.
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

    /**
     * Tries to insert a stack into the first `INVENTORY_SIZE` slots.
     *
     * @return the leftover items that did not fit
     */
    public static ItemStack insertIntoInventory(Container inventory, ItemStack stack) {
        ItemStack remaining = stack.copy();
        // First pass: merge into existing matching stacks
        for (int i = 0; i < INVENTORY_SIZE && !remaining.isEmpty(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, remaining)) {
                int canFit = slot.getMaxStackSize() - slot.getCount();
                int toAdd = Math.min(remaining.getCount(), canFit);
                if (toAdd > 0) {
                    slot.grow(toAdd);
                    remaining.shrink(toAdd);
                    inventory.setChanged();
                }
            }
        }
        // Second pass: place into empty slots
        for (int i = 0; i < INVENTORY_SIZE && !remaining.isEmpty(); i++) {
            if (inventory.getItem(i).isEmpty()) {
                inventory.setItem(i, remaining.copy());
                remaining.setCount(0);
            }
        }
        return remaining;
    }

    // Vacuum mode

    /**
     * Finds the first entity along a straight ray from `start` in
     * direction `dir` up to `maxDist`. Prioritises item entities
     * over living entities (mobs).
     */
    @Nullable
    private static Entity findEntityAlongRay(ServerLevel world, Player player, Vec3 start, Vec3 dir, double maxDist) {
        int steps = (int) Math.ceil(maxDist / 0.5);
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(dir.scale(i * 0.5));
            AABB searchBox =
                    new AABB(point.x - 1.0, point.y - 1.0, point.z - 1.0, point.x + 1.0, point.y + 1.0, point.z + 1.0);

            List<ItemEntity> items =
                    world.getEntitiesOfClass(ItemEntity.class, searchBox, e -> e.isAlive() && !e.hasPickUpDelay());
            if (!items.isEmpty()) {
                return items.getFirst();
            }

            List<LivingEntity> mobs =
                    world.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e.isAlive() && !(e instanceof Player));
            if (!mobs.isEmpty()) {
                return mobs.getFirst();
            }
        }
        return null;
    }

    private boolean performVacuum(ServerLevel world, Player player, ItemStack gunStack) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookDir = player.getViewVector(1.0F).normalize();
        double range = TechExtensionsConfig.vacuumGunRange;

        // Straight-line raycast for block hits (ignoring fluids)
        Vec3 endPos = eyePos.add(lookDir.scale(range));
        BlockHitResult blockHit = world.clip(new ClipContext(
                eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));

        // Separate raycast that detects fluid source blocks
        BlockHitResult fluidHit = world.clip(new ClipContext(
                eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, CollisionContext.empty()));

        // Effective range limited by block hit if any
        double effectiveRange =
                blockHit.getType() != HitResult.Type.MISS ? eyePos.distanceTo(blockHit.getLocation()) : range;

        // Check for entities along the straight ray
        Entity hitEntity = findEntityAlongRay(world, player, eyePos, lookDir, effectiveRange);

        // Living entity (mob) - convert to spawn egg or attract
        if (hitEntity instanceof LivingEntity living) {
            Vec3 mobPos = living.position().add(0, living.getBbHeight() / 2, 0);
            List<Vec3> trail = List.of(mobPos, eyePos);
            SpawnEggItem egg = SpawnEggItem.byId(living.getType());
            if (egg != null) {
                ItemStack eggStack = new ItemStack(egg);
                // Serialize all entity data (HP, name, color, etc.) into the egg
                TagValueOutput output =
                        TagValueOutput.createWithContext(ProblemReporter.DISCARDING, world.registryAccess());
                living.saveWithoutId(output);
                CompoundTag entityTag = output.buildResult();
                eggStack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(living.getType(), entityTag));
                if (living.hasCustomName()) {
                    eggStack.set(DataComponents.CUSTOM_NAME, living.getCustomName());
                }
                living.discard();
                // Animate the egg traveling from mob to player, insert on arrival
                ACTIVE_TASKS.add(new AnimatedTask(world, trail, eggStack, () -> {
                    Container inv = getInventory(gunStack);
                    ItemStack leftover = insertIntoInventory(inv, eggStack);
                    if (!leftover.isEmpty()) {
                        player.drop(leftover, false);
                    }
                    world.playSound(
                            null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 0.8F);
                }));
            } else {
                // No spawn egg — attract mob toward player (no animation needed)
                Vec3 pullDir = eyePos.subtract(living.position()).normalize().scale(1.5);
                living.push(pullDir.x, pullDir.y, pullDir.z);
                world.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 0.5F);
            }
            return true;
        }

        // Item entity - animate it traveling to the player, then insert
        if (hitEntity instanceof ItemEntity itemEntity) {
            Vec3 itemPos = itemEntity.position();
            List<Vec3> trail = List.of(itemPos, eyePos);
            ItemStack itemStack = itemEntity.getItem().copy();
            itemEntity.discard();
            ACTIVE_TASKS.add(new AnimatedTask(world, trail, itemStack, () -> {
                Container inv = getInventory(gunStack);
                ItemStack leftover = insertIntoInventory(inv, itemStack);
                if (!leftover.isEmpty()) {
                    player.drop(leftover, false);
                }
                world.playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.ITEM_PICKUP,
                        SoundSource.PLAYERS,
                        0.2F,
                        ((world.random.nextFloat() - world.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            }));
            return true;
        }

        // Vacuum fluid source if the gun contains an empty bucket
        if (fluidHit.getType() != HitResult.Type.MISS) {
            BlockPos fluidPos = fluidHit.getBlockPos();
            BlockState fluidState = world.getBlockState(fluidPos);
            // Determine if this is a collectible fluid source
            ItemStack filledBucket = ItemStack.EMPTY;
            if (fluidState.is(Blocks.WATER) && fluidState.getFluidState().isSource()) {
                filledBucket = new ItemStack(Items.WATER_BUCKET);
            } else if (fluidState.is(Blocks.LAVA) && fluidState.getFluidState().isSource()) {
                filledBucket = new ItemStack(Items.LAVA_BUCKET);
            } else if (fluidState.is(Blocks.POWDER_SNOW)) {
                filledBucket = new ItemStack(Items.POWDER_SNOW_BUCKET);
            }
            if (!filledBucket.isEmpty()) {
                // Check if the gun has an empty bucket to consume
                Container inventory = getInventory(gunStack);
                int bucketSlot = -1;
                for (int i = 0; i < INVENTORY_SIZE; i++) {
                    if (inventory.getItem(i).is(Items.BUCKET)) {
                        bucketSlot = i;
                        break;
                    }
                }
                if (bucketSlot >= 0) {
                    // Consume one empty bucket
                    inventory.getItem(bucketSlot).shrink(1);
                    inventory.setChanged();
                    // Remove the fluid source
                    world.setBlock(fluidPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    world.playSound(null, fluidPos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    // Animate the filled bucket traveling to the player
                    Vec3 fluidCenter = Vec3.atCenterOf(fluidPos);
                    List<Vec3> trail = List.of(fluidCenter, eyePos);
                    ItemStack finalBucket = filledBucket;
                    ACTIVE_TASKS.add(new AnimatedTask(world, trail, filledBucket, () -> {
                        Container inv = getInventory(gunStack);
                        ItemStack leftover = insertIntoInventory(inv, finalBucket);
                        if (!leftover.isEmpty()) {
                            player.drop(leftover, false);
                        }
                    }));
                    return true;
                }
            }
        }

        // Break the block, animate drops traveling to the player
        if (blockHit.getType() != HitResult.Type.MISS) {
            BlockPos target = blockHit.getBlockPos();
            BlockState state = world.getBlockState(target);
            if (!state.isAir() && !state.is(Blocks.BEDROCK) && state.getDestroySpeed(world, target) >= 0) {
                Vec3 blockCenter = Vec3.atCenterOf(target);
                List<Vec3> trail = List.of(blockCenter, eyePos);
                List<ItemStack> drops =
                        Block.getDrops(state, world, target, world.getBlockEntity(target), player, gunStack);
                world.destroyBlock(target, false);
                // Use the first drop as the display item; insert all drops on arrival
                ItemStack displayItem = drops.isEmpty() ? ItemStack.EMPTY : drops.getFirst();
                if (!displayItem.isEmpty()) {
                    ACTIVE_TASKS.add(new AnimatedTask(world, trail, displayItem, () -> {
                        Container inv = getInventory(gunStack);
                        for (ItemStack drop : drops) {
                            ItemStack leftover = insertIntoInventory(inv, drop);
                            if (!leftover.isEmpty()) {
                                player.drop(leftover, false);
                            }
                        }
                    }));
                }
                return true;
            }
        }

        // Nothing to vacuum
        world.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.2F);
        return false;
    }

    // Blow mode

    private boolean performBlow(ServerLevel world, Player player, ItemStack gunStack) {
        Container inventory = getInventory(gunStack);

        // Find first non-empty inventory slot
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack slotStack = inventory.getItem(i);
            if (slotStack.isEmpty()) continue;

            Item item = slotStack.getItem();

            // Instant: Projectiles (they create their own visible entity)
            if (item instanceof ProjectileItem projectileItem) {
                return blowProjectile(world, player, inventory, slotStack, projectileItem);
            }

            // Animated: everything else
            TrajectoryResult trajectory = computeTrajectory(world, player);
            List<Vec3> trailPath = trajectory.path();

            // Offset the start of the animation to the gun barrel (in front
            // of the player, slightly below eye level) so the display entity
            // doesn't spawn inside the player's head.
            Vec3 lookVec = player.getViewVector(1.0F).normalize();
            Vec3 gunPos = player.getEyePosition(1.0F).add(lookVec.scale(1.0)).add(0, -0.3, 0);
            trailPath.set(0, gunPos);

            // Take one item from inventory immediately
            ItemStack takenItem = slotStack.copyWithCount(1);
            slotStack.shrink(1);
            inventory.setChanged();

            Runnable onArrival;

            Item nukeItem = TRContent.NUKE.asItem();
            if (item == Items.TNT || (TechRebornConfig.nukeEnabled && item == nukeItem)) {
                onArrival = () -> {
                    BlockPos target = trajectory.adjacentPos();
                    PrimedTnt tnt = item == nukeItem
                            ? new EntityNukePrimed(
                                    world, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, player)
                            : new PrimedTnt(
                                    world, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, player);
                    tnt.setDeltaMovement(trajectory.velocity().normalize().scale(0.1));
                    world.addFreshEntity(tnt);
                    world.playSound(null, target, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
                };
            } else if (item instanceof SpawnEggItem spawnEgg) {
                onArrival = () -> {
                    EntityType<?> entityType = spawnEgg.getType(takenItem);
                    if (entityType == null) return;
                    BlockPos target = trajectory.adjacentPos();
                    TypedEntityData<?> savedData = takenItem.get(DataComponents.ENTITY_DATA);
                    if (savedData != null) {
                        Entity entity = entityType.create(world, EntitySpawnReason.LOAD);
                        if (entity != null) {
                            savedData.loadInto(entity);
                            entity.snapTo(
                                    target.getX() + 0.5,
                                    target.getY(),
                                    target.getZ() + 0.5,
                                    entity.getYRot(),
                                    entity.getXRot());
                            world.addFreshEntity(entity);
                        }
                    } else {
                        entityType.spawn(
                                world, takenItem, player, target, EntitySpawnReason.SPAWN_ITEM_USE, true, false);
                    }
                    world.playSound(null, target, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.0F);
                };
            } else if (item instanceof BoneMealItem) {
                onArrival = () -> {
                    BlockPos target = trajectory.hitBlock();
                    boolean grew = BoneMealItem.growCrop(takenItem.copy(), world, target);
                    if (!grew) {
                        grew = BoneMealItem.growWaterPlant(takenItem.copy(), world, target, trajectory.hitFace());
                    }
                    if (grew) {
                        world.levelEvent(1505, target, 15);
                    } else {
                        player.drop(takenItem.copy(), false);
                    }
                };
            } else if (item instanceof BucketItem) {
                onArrival = () -> deferredBlowBucket(world, player, gunStack, takenItem, trajectory);
            } else if (item instanceof FlintAndSteelItem) {
                onArrival = () -> deferredBlowFlintAndSteel(world, player, takenItem, trajectory);
            } else if (item instanceof ShearsItem) {
                onArrival = () -> deferredBlowShears(world, player, takenItem, trajectory);
            } else if (item instanceof BlockItem blockItem) {
                onArrival = () -> {
                    BlockPos target = trajectory.adjacentPos();
                    Direction hitFace = trajectory.hitFace();
                    ItemStack placeStack = takenItem.copy();
                    placeStack.setCount(1);
                    DirectionalPlaceContext ctx =
                            new DirectionalPlaceContext(world, target, hitFace.getOpposite(), placeStack, hitFace);
                    InteractionResult result = blockItem.place(ctx);
                    if (!result.consumesAction()) {
                        player.drop(takenItem.copy(), false);
                    }
                };
            } else if (takenItem.has(DataComponents.EQUIPPABLE)) {
                onArrival = () -> {
                    BlockPos target = trajectory.adjacentPos();
                    AABB searchBox = new AABB(target).inflate(1.0);
                    List<LivingEntity> entities = world.getEntitiesOfClass(
                            LivingEntity.class, searchBox, e -> e.isAlive() && !e.isSpectator());
                    boolean equipped = false;
                    for (LivingEntity entity : entities) {
                        for (EquipmentSlot equipSlot : EquipmentSlot.values()) {
                            if (entity.getItemBySlot(equipSlot).isEmpty() && entity.canEquipWithDispenser(takenItem)) {
                                entity.setItemSlot(equipSlot, takenItem.copy());
                                world.playSound(
                                        null,
                                        entity.blockPosition(),
                                        SoundEvents.ARMOR_EQUIP_GENERIC.value(),
                                        SoundSource.BLOCKS,
                                        1.0F,
                                        1.0F);
                                equipped = true;
                                break;
                            }
                        }
                        if (equipped) break;
                    }
                    if (!equipped) {
                        player.drop(takenItem.copy(), false);
                    }
                };
            } else {
                // Default: drop item at trajectory endpoint
                onArrival = () -> {
                    Vec3 hitLoc = trajectory.hitLocation();
                    ItemEntity ie = new ItemEntity(world, hitLoc.x, hitLoc.y, hitLoc.z, takenItem.copy());
                    ie.setPickUpDelay(20);
                    world.addFreshEntity(ie);
                    world.playSound(
                            null,
                            BlockPos.containing(hitLoc),
                            SoundEvents.DISPENSER_DISPENSE,
                            SoundSource.BLOCKS,
                            1.0F,
                            1.0F);
                };
            }

            ACTIVE_TASKS.add(new AnimatedTask(world, trailPath, takenItem, onArrival));
            return true;
        }

        // Inventory is empty
        world.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 1.2F);
        return false;
    }

    // Blow: Projectile items

    private boolean blowProjectile(
            ServerLevel world, Player player, Container inventory, ItemStack slotStack, ProjectileItem projectileItem) {
        ItemStack toShoot = slotStack.split(1);
        inventory.setChanged();

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F).normalize();
        Vec3 spawnPos = eyePos.add(lookVec.scale(0.7));

        ProjectileItem.DispenseConfig config = projectileItem.createDispenseConfig();

        Projectile projectile = projectileItem.asProjectile(world, spawnPos, toShoot, Direction.UP);
        projectile.setOwner(player);

        Projectile.spawnProjectileUsingShoot(
                projectile, world, toShoot, lookVec.x, lookVec.y, lookVec.z, config.power(), config.uncertainty());

        world.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_LAUNCH, SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    // ── Deferred blow: Flint and steel ────────────────────────────────────

    /**
     * Called when the animated flint and steel arrives at the trajectory
     * endpoint. Performs the fire action and drops the (damaged) flint and
     * steel at the target location.
     */
    private void deferredBlowFlintAndSteel(
            ServerLevel world, Player player, ItemStack flintItem, TrajectoryResult trajectory) {
        BlockPos hitBlock = trajectory.hitBlock();
        BlockPos adjacent = trajectory.adjacentPos();
        BlockState hitState = world.getBlockState(hitBlock);

        boolean used = false;

        if (hitState.getBlock() instanceof TntBlock) {
            TntBlock.prime(world, hitBlock);
            used = true;
        } else if (hitState.is(Blocks.CAMPFIRE) || hitState.is(Blocks.SOUL_CAMPFIRE)) {
            if (CampfireBlock.canLight(hitState)) {
                world.setBlock(hitBlock, hitState.setValue(CampfireBlock.LIT, true), Block.UPDATE_ALL);
                used = true;
            }
        } else if (BaseFireBlock.canBePlacedAt(world, adjacent, trajectory.hitFace())) {
            world.setBlock(adjacent, BaseFireBlock.getState(world, adjacent), Block.UPDATE_ALL_IMMEDIATE);
            used = true;
        }

        if (used) {
            flintItem.hurtAndBreak(1, world, player instanceof ServerPlayer sp ? sp : null, item -> {});
            world.playSound(null, adjacent, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        // Drop the flint and steel at the target (damaged or intact)
        if (!flintItem.isEmpty()) {
            Vec3 dropPos = Vec3.atCenterOf(adjacent);
            ItemEntity ie = new ItemEntity(world, dropPos.x, dropPos.y, dropPos.z, flintItem.copy());
            ie.setPickUpDelay(20);
            world.addFreshEntity(ie);
        }
    }

    // ── Deferred blow: Shears ───────────────────────────────────────────────

    /**
     * Called when the animated shears arrive at the trajectory endpoint.
     * Tries to shear blocks (beehive, pumpkin, growing plant) or entities
     * (sheep, mooshroom, etc.), then drops the (damaged) shears at the
     * target location.
     */
    private void deferredBlowShears(
            ServerLevel world, Player player, ItemStack shearsItem, TrajectoryResult trajectory) {
        BlockPos hitBlock = trajectory.hitBlock();
        BlockPos adjacent = trajectory.adjacentPos();
        BlockState hitState = world.getBlockState(hitBlock);

        boolean used = false;

        // Beehive/bee nest with full honey
        if (hitState.getBlock() instanceof BeehiveBlock beehiveBlock
                && hitState.hasProperty(BlockStateProperties.LEVEL_HONEY)
                && hitState.getValue(BlockStateProperties.LEVEL_HONEY) >= 5) {
            BeehiveBlock.dropHoneycomb(world, shearsItem, hitState, world.getBlockEntity(hitBlock), player, hitBlock);
            beehiveBlock.releaseBeesAndResetHoneyLevel(
                    world, hitState, hitBlock, player, BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED);
            world.gameEvent(player, GameEvent.SHEAR, hitBlock);
            shearsItem.hurtAndBreak(1, world, player instanceof ServerPlayer sp ? sp : null, item -> {});
            used = true;
        }

        // Pumpkin → carved pumpkin
        if (!used && hitState.getBlock() instanceof PumpkinBlock) {
            Direction facing = trajectory.hitFace().getAxis() == Direction.Axis.Y
                    ? player.getDirection().getOpposite()
                    : trajectory.hitFace();
            world.setBlock(
                    hitBlock,
                    Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, facing),
                    Block.UPDATE_ALL);
            // Drop pumpkin seeds (same as vanilla carving — 4 seeds)
            ItemEntity seeds = new ItemEntity(
                    world,
                    hitBlock.getX() + 0.5,
                    hitBlock.getY() + 0.5,
                    hitBlock.getZ() + 0.5,
                    new ItemStack(Items.PUMPKIN_SEEDS, 4));
            world.addFreshEntity(seeds);
            world.playSound(null, hitBlock, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0F, 1.0F);
            shearsItem.hurtAndBreak(1, world, player instanceof ServerPlayer sp ? sp : null, item -> {});
            used = true;
        }

        // Growing plant head (kelp, cave vines, etc.) → stop growth
        if (!used && hitState.getBlock() instanceof GrowingPlantHeadBlock growingPlant) {
            if (!growingPlant.isMaxAge(hitState)) {
                world.setBlock(hitBlock, growingPlant.getMaxAgeState(hitState), Block.UPDATE_ALL);
                world.gameEvent(player, GameEvent.BLOCK_CHANGE, hitBlock);
                world.playSound(null, hitBlock, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
                shearsItem.hurtAndBreak(1, world, player instanceof ServerPlayer sp ? sp : null, item -> {});
                used = true;
            }
        }

        // Try shearing entities near the target (sheep, mooshroom, etc.)
        if (!used) {
            AABB searchBox = new AABB(adjacent).inflate(1.0);
            List<LivingEntity> entities =
                    world.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e.isAlive() && !e.isSpectator());
            for (LivingEntity entity : entities) {
                if (entity instanceof Shearable shearable && shearable.readyForShearing()) {
                    shearable.shear(world, SoundSource.BLOCKS, shearsItem);
                    shearsItem.hurtAndBreak(1, world, player instanceof ServerPlayer sp ? sp : null, item -> {});
                    world.gameEvent(player, GameEvent.SHEAR, entity.blockPosition());
                    used = true;
                    break;
                }
            }
        }

        // Drop the shears at the target (damaged or intact)
        if (!shearsItem.isEmpty()) {
            Vec3 dropPos = Vec3.atCenterOf(used ? hitBlock : adjacent);
            ItemEntity ie = new ItemEntity(world, dropPos.x, dropPos.y, dropPos.z, shearsItem.copy());
            ie.setPickUpDelay(20);
            world.addFreshEntity(ie);
        }
    }

    // ── Deferred blow: Bucket handling ──────────────────────────────────────

    /**
     * Handles bucket placement/collection when the animated bucket arrives
     * at the trajectory endpoint. The bucket has already been removed from
     * the gun's inventory. Results are dropped at the target location.
     */
    private void deferredBlowBucket(
            ServerLevel world, Player player, ItemStack gunStack, ItemStack bucketItem, TrajectoryResult trajectory) {
        Item bucketType = bucketItem.getItem();

        if (bucketType == Items.BUCKET) {
            // Empty bucket: try to collect fluid at the hit block
            BlockPos target = trajectory.hitBlock();
            BlockState state = world.getBlockState(target);
            ItemStack filledBucket = ItemStack.EMPTY;
            if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                filledBucket = new ItemStack(Items.WATER_BUCKET);
            } else if (state.is(Blocks.LAVA) && state.getFluidState().isSource()) {
                filledBucket = new ItemStack(Items.LAVA_BUCKET);
            } else if (state.is(Blocks.POWDER_SNOW)) {
                filledBucket = new ItemStack(Items.POWDER_SNOW_BUCKET);
            }
            if (!filledBucket.isEmpty()) {
                world.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                // Drop the filled bucket at the target position
                Vec3 dropPos = Vec3.atCenterOf(target);
                ItemEntity ie = new ItemEntity(world, dropPos.x, dropPos.y, dropPos.z, filledBucket);
                ie.setPickUpDelay(20);
                world.addFreshEntity(ie);
                world.playSound(null, target, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                // Couldn't collect — drop the empty bucket at the target
                Vec3 hitLoc = trajectory.hitLocation();
                ItemEntity ie = new ItemEntity(world, hitLoc.x, hitLoc.y, hitLoc.z, bucketItem.copy());
                ie.setPickUpDelay(20);
                world.addFreshEntity(ie);
            }
        } else if (bucketType instanceof BucketItem bucketItemObj) {
            // Filled bucket: try to place fluid at the adjacent block
            BlockPos target = trajectory.adjacentPos();
            if (bucketItemObj.emptyContents(player, world, target, null)) {
                // Drop empty bucket at the target
                Vec3 dropPos = Vec3.atCenterOf(target);
                ItemEntity ie = new ItemEntity(world, dropPos.x, dropPos.y, dropPos.z, new ItemStack(Items.BUCKET));
                ie.setPickUpDelay(20);
                world.addFreshEntity(ie);
            } else {
                // Couldn't place — drop the filled bucket at the target
                Vec3 hitLoc = trajectory.hitLocation();
                ItemEntity ie = new ItemEntity(world, hitLoc.x, hitLoc.y, hitLoc.z, bucketItem.copy());
                ie.setPickUpDelay(20);
                world.addFreshEntity(ie);
            }
        }
    }

    // ── Cached upgrade values ───────────────────────────────────────────────

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

        final int cooldown = computeCooldown(overclockers);
        final long energyCapacity =
                TechExtensionsConfig.vacuumGunCharge + (long) (energyStorage * TechRebornConfig.energyStoragePower);

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt("cache:cooldown", cooldown);
            tag.putLong("cache:energy_capacity", energyCapacity);
        });
    }

    public static int getCooldown(ItemStack stack) {
        return getCachedValue(
                stack, "cache:cooldown", TechExtensionsConfig.vacuumGunCooldown, tag -> tag.getInt("cache:cooldown"));
    }

    private static int getEnergyCapacityFromCache(ItemStack stack) {
        return getCachedValue(
                stack,
                "cache:energy_capacity",
                TechExtensionsConfig.vacuumGunCharge,
                tag -> tag.getInt("cache:energy_capacity"));
    }

    private static <T> T getCachedValue(
            ItemStack stack, String key, T defaultValue, Function<CompoundTag, Optional<T>> extractor) {
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
        return extractor
                .apply(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag())
                .orElse(defaultValue);
    }

    public static int computeCooldown(int overclockerUpgrades) {
        double speedMultiplier = TechRebornConfig.overclockerSpeed * overclockerUpgrades;
        double cooldown = TechExtensionsConfig.vacuumGunCooldown * (1.0 - speedMultiplier);
        return (int) Math.round(cooldown);
    }
}
