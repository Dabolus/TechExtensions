package dev.gga.techextensions.items.tool.advanced;

import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.utils.ItemAnimationManager;
import dev.gga.techextensions.utils.TEAimingUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ClipContext;
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
import techreborn.config.TechRebornConfig;
import techreborn.entities.EntityNukePrimed;
import techreborn.init.TRContent;

/**
 * Contains all vacuum and blow action logic for the `VacuumGunItem`.
 * Separated from the Item class to keep it focused on Item lifecycle.
 *
 * Vacuumed items go into the gun right away: the flying item display is purely visual, so nothing is lost
 * if the gun is moved, the player logs out or the server stops while an animation is in flight.
 */
final class VacuumGunActions {

    // Trajectory physics constants
    private static final double TRAJECTORY_GRAVITY = 0.05;
    private static final double TRAJECTORY_DRAG = 0.99;
    private static final double LAUNCH_POWER = 1.5;
    private static final int MAX_TRAJECTORY_TICKS = 600;

    private VacuumGunActions() {}

    record TrajectoryResult(
            BlockPos hitBlock,
            BlockPos adjacentPos,
            Direction hitFace,
            Vec3 hitLocation,
            Vec3 velocity,
            List<Vec3> path) {}

    // Vacuum mode

    static boolean performVacuum(ServerLevel world, Player player, ItemStack gunStack) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookDir = player.getViewVector(1.0F).normalize();
        double range = TechExtensionsConfig.vacuumGunRange.get();
        Vec3 endPos = eyePos.add(lookDir.scale(range));

        BlockHitResult blockHit = world.clip(new ClipContext(
                eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));

        Vec3 entityEnd = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : endPos;
        TEAimingUtils.EntityHit entityHit = TEAimingUtils.traceEntity(
                world,
                player,
                eyePos,
                entityEnd,
                e -> e.isAlive()
                        && !e.isSpectator()
                        && ((e instanceof ItemEntity ie && !ie.hasPickUpDelay()) || (e instanceof LivingEntity)),
                1.0D,
                null);
        Entity hitEntity = entityHit != null ? entityHit.entity() : null;

        if (hitEntity instanceof LivingEntity living) {
            return vacuumMob(world, player, gunStack, living, eyePos);
        }
        if (hitEntity instanceof ItemEntity itemEntity) {
            return vacuumItem(world, player, gunStack, itemEntity, eyePos);
        }

        // Entities take priority, so fluids are only traced when none was hit
        BlockHitResult fluidHit = world.clip(new ClipContext(
                eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, CollisionContext.empty()));
        if (fluidHit.getType() != HitResult.Type.MISS && vacuumFluid(world, player, gunStack, fluidHit, eyePos)) {
            return true;
        }
        if (blockHit.getType() != HitResult.Type.MISS && vacuumBlock(world, player, gunStack, blockHit, eyePos)) {
            return true;
        }

        world.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.2F);
        return false;
    }

    private static boolean vacuumMob(
            ServerLevel world, Player player, ItemStack gunStack, LivingEntity living, Vec3 eyePos) {
        Vec3 mobPos = living.position().add(0, living.getBbHeight() / 2, 0);

        // First, try to strip equipped items one at a time
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipped = living.getItemBySlot(slot);
            if (equipped.isEmpty()) continue;

            ItemStack stripped = equipped.copy();
            living.setItemSlot(slot, ItemStack.EMPTY);
            insertOrDrop(player, gunStack, stripped);

            world.playSound(
                    null,
                    living.blockPosition(),
                    SoundEvents.ARMOR_EQUIP_GENERIC.value(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            animateToPlayer(world, player, mobPos, eyePos, stripped, 1.0F);
            return true;
        }

        // Second, if no equipped items, vacuum the mob itself as a spawn egg (if possible)
        Optional<Holder<Item>> eggHolder = SpawnEggItem.byId(living.getType());
        if (eggHolder.isPresent()) {
            ItemStack eggStack = new ItemStack(eggHolder.get());
            TagValueOutput output =
                    TagValueOutput.createWithContext(ProblemReporter.DISCARDING, world.registryAccess());
            living.saveWithoutId(output);
            CompoundTag entityTag = output.buildResult();
            eggStack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(living.getType(), entityTag));
            if (living.hasCustomName()) {
                eggStack.set(DataComponents.CUSTOM_NAME, living.getCustomName());
            }
            living.discard();
            insertOrDrop(player, gunStack, eggStack);
            animateToPlayer(world, player, mobPos, eyePos, eggStack, 0.8F);
        } else {
            Vec3 pullDir = eyePos.subtract(living.position()).normalize().scale(1.5);
            living.push(pullDir.x, pullDir.y, pullDir.z);
            world.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 0.5F);
        }
        return true;
    }

    private static boolean vacuumItem(
            ServerLevel world, Player player, ItemStack gunStack, ItemEntity itemEntity, Vec3 eyePos) {
        Vec3 itemPos = itemEntity.position();
        ItemStack itemStack = itemEntity.getItem().copy();
        itemEntity.discard();
        insertOrDrop(player, gunStack, itemStack);

        float pitch = ((world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F;
        ItemAnimationManager.schedule(
                world,
                List.of(itemPos, eyePos),
                itemStack,
                () -> world.playSound(
                        null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, pitch));
        return true;
    }

    private static boolean vacuumFluid(
            ServerLevel world, Player player, ItemStack gunStack, BlockHitResult fluidHit, Vec3 eyePos) {
        BlockPos fluidPos = fluidHit.getBlockPos();
        ItemStack filledBucket = getFilledBucketForBlock(world.getBlockState(fluidPos));
        if (filledBucket.isEmpty()) return false;

        Container inventory = VacuumGunItem.getInventory(gunStack);
        int bucketSlot = -1;
        for (int i = 0; i < VacuumGunItem.INVENTORY_SIZE; i++) {
            if (inventory.getItem(i).is(Items.BUCKET)) {
                bucketSlot = i;
                break;
            }
        }
        if (bucketSlot < 0) return false;

        inventory.getItem(bucketSlot).shrink(1);
        ItemStack leftover = VacuumGunItem.insertIntoInventory(inventory, filledBucket);
        inventory.setChanged();
        if (!leftover.isEmpty()) {
            player.drop(leftover, false);
        }
        world.setBlock(fluidPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        world.playSound(null, fluidPos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        ItemAnimationManager.schedule(world, List.of(Vec3.atCenterOf(fluidPos), eyePos), filledBucket, () -> {});
        return true;
    }

    private static boolean vacuumBlock(
            ServerLevel world, Player player, ItemStack gunStack, BlockHitResult blockHit, Vec3 eyePos) {
        BlockPos target = blockHit.getBlockPos();
        BlockState state = world.getBlockState(target);
        if (state.isAir() || state.is(Blocks.BEDROCK) || state.getDestroySpeed(world, target) < 0) return false;

        List<ItemStack> drops = Block.getDrops(state, world, target, world.getBlockEntity(target), player, gunStack);
        world.destroyBlock(target, false);
        insertAllOrDrop(player, gunStack, drops);
        if (!drops.isEmpty()) {
            ItemAnimationManager.schedule(world, List.of(Vec3.atCenterOf(target), eyePos), drops.getFirst(), () -> {});
        }
        return true;
    }

    /** Plays the "flying to the player" visual and the pickup sound on arrival. */
    private static void animateToPlayer(
            ServerLevel world, Player player, Vec3 from, Vec3 to, ItemStack displayItem, float pitch) {
        ItemAnimationManager.schedule(
                world,
                List.of(from, to),
                displayItem,
                () -> world.playSound(
                        null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, pitch));
    }

    // Blow mode

    static boolean performBlow(ServerLevel world, Player player, ItemStack gunStack) {
        Container inventory = VacuumGunItem.getInventory(gunStack);

        for (int i = 0; i < VacuumGunItem.INVENTORY_SIZE; i++) {
            ItemStack slotStack = inventory.getItem(i);
            if (slotStack.isEmpty()) continue;

            // Instant: Projectiles (they create their own visible entity)
            if (slotStack.getItem() instanceof ProjectileItem pi) {
                return blowProjectile(world, player, inventory, slotStack, pi);
            }

            // Animated: everything else
            TrajectoryResult trajectory = computeTrajectory(world, player);
            List<Vec3> trailPath = trajectory.path();
            trailPath.set(0, getGunBarrelPos(player));

            ItemStack takenItem = slotStack.copyWithCount(1);
            slotStack.shrink(1);
            inventory.setChanged();

            Runnable onArrival = buildArrivalAction(world, player, gunStack, takenItem, trajectory);
            ItemAnimationManager.schedule(world, trailPath, takenItem, onArrival);
            return true;
        }

        world.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 1.2F);
        return false;
    }

    private static Vec3 getGunBarrelPos(Player player) {
        Vec3 lookVec = player.getViewVector(1.0F).normalize();
        return player.getEyePosition(1.0F).add(lookVec.scale(1.0)).add(0, -0.3, 0);
    }

    private static Runnable buildArrivalAction(
            ServerLevel world, Player player, ItemStack gunStack, ItemStack takenItem, TrajectoryResult tr) {
        Item item = takenItem.getItem();
        Item nukeItem = TRContent.NUKE.asItem();

        if (item == Items.TNT || (TechRebornConfig.nukeEnabled.get() && item == nukeItem)) {
            return () -> blowExplosive(world, player, takenItem, tr);
        }
        if (item instanceof SpawnEggItem) {
            return () -> blowSpawnEgg(world, player, takenItem, tr);
        }
        if (item instanceof BoneMealItem) {
            return () -> blowBoneMeal(world, player, takenItem, tr);
        }
        if (item instanceof BucketItem) {
            return () -> blowBucket(world, player, takenItem, tr);
        }
        if (item instanceof FlintAndSteelItem) {
            return () -> blowFlintAndSteel(world, player, takenItem, tr);
        }
        if (item instanceof BlockItem blockItem) {
            // Blocks are placed, never handed to an entity, so no entity lookup is needed
            return () -> blowBlock(world, player, takenItem, tr, blockItem);
        }
        // Pre-resolve the entity along the parabolic trajectory before the animation starts
        // (it is captured now so it won't be missed if it moves during the flight)
        LivingEntity trajectoryTarget = findEntityAlongPath(world, player, tr.path());
        if (item instanceof ShearsItem) {
            return () -> blowShears(world, player, takenItem, tr, trajectoryTarget);
        }
        if (takenItem.has(DataComponents.EQUIPPABLE)) {
            return () -> blowEquippable(world, player, takenItem, tr, trajectoryTarget);
        }
        return () -> blowDefault(world, player, takenItem, tr, trajectoryTarget);
    }

    // Individual blow handlers

    private static boolean blowProjectile(
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

    private static void blowExplosive(ServerLevel world, Player player, ItemStack item, TrajectoryResult tr) {
        BlockPos target = tr.adjacentPos();
        Item nukeItem = TRContent.NUKE.asItem();
        PrimedTnt tnt = item.getItem() == nukeItem
                ? new EntityNukePrimed(world, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, player)
                : new PrimedTnt(world, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, player);
        tnt.setDeltaMovement(tr.velocity().normalize().scale(0.1));
        world.addFreshEntity(tnt);
        world.playSound(null, target, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static void blowSpawnEgg(ServerLevel world, Player player, ItemStack takenItem, TrajectoryResult tr) {
        SpawnEggItem spawnEgg = (SpawnEggItem) takenItem.getItem();
        EntityType<?> entityType = spawnEgg.getType(takenItem);
        if (entityType == null) return;
        BlockPos target = tr.adjacentPos();
        TypedEntityData<?> savedData = takenItem.get(DataComponents.ENTITY_DATA);
        if (savedData != null) {
            Entity entity = entityType.create(world, EntitySpawnReason.LOAD);
            if (entity != null) {
                savedData.loadInto(entity);
                entity.snapTo(
                        target.getX() + 0.5, target.getY(), target.getZ() + 0.5, entity.getYRot(), entity.getXRot());
                world.addFreshEntity(entity);
            }
        } else {
            entityType.spawn(world, takenItem, player, target, EntitySpawnReason.SPAWN_ITEM_USE, true, false);
        }
        world.playSound(null, target, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static void blowBoneMeal(ServerLevel world, Player player, ItemStack takenItem, TrajectoryResult tr) {
        BlockPos target = tr.hitBlock();
        boolean grew = BoneMealItem.growCrop(takenItem.copy(), world, target);
        if (!grew) {
            grew = BoneMealItem.growWaterPlant(takenItem.copy(), world, target, tr.hitFace());
        }
        if (grew) {
            world.levelEvent(1505, target, 15);
        } else {
            dropItemAt(world, Vec3.atCenterOf(target), takenItem);
        }
    }

    private static void blowFlintAndSteel(ServerLevel world, Player player, ItemStack flintItem, TrajectoryResult tr) {
        BlockPos hitBlock = tr.hitBlock();
        BlockPos adjacent = tr.adjacentPos();
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
        } else if (BaseFireBlock.canBePlacedAt(world, adjacent, tr.hitFace())) {
            world.setBlock(adjacent, BaseFireBlock.getState(world, adjacent), Block.UPDATE_ALL_IMMEDIATE);
            used = true;
        }

        if (used) {
            flintItem.hurtAndBreak(1, world, player instanceof ServerPlayer sp ? sp : null, item -> {});
            world.playSound(null, adjacent, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        dropIfNotEmpty(world, Vec3.atCenterOf(adjacent), flintItem);
    }

    private static void blowShears(
            ServerLevel world,
            Player player,
            ItemStack shearsItem,
            TrajectoryResult tr,
            @Nullable LivingEntity preResolvedTarget) {
        BlockPos hitBlock = tr.hitBlock();
        BlockPos adjacent = tr.adjacentPos();
        BlockState hitState = world.getBlockState(hitBlock);
        boolean used = false;

        // Beehive / bee nest with full honey
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

        // Pumpkin -> carved pumpkin
        if (!used && hitState.getBlock() instanceof PumpkinBlock) {
            Direction facing = tr.hitFace().getAxis() == Direction.Axis.Y
                    ? player.getDirection().getOpposite()
                    : tr.hitFace();
            world.setBlock(
                    hitBlock,
                    Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, facing),
                    Block.UPDATE_ALL);
            dropItemAt(world, Vec3.atCenterOf(hitBlock), new ItemStack(Items.PUMPKIN_SEEDS, 4));
            world.playSound(null, hitBlock, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0F, 1.0F);
            shearsItem.hurtAndBreak(1, world, player instanceof ServerPlayer sp ? sp : null, item -> {});
            used = true;
        }

        // Growing plant head -> stop growth
        if (!used && hitState.getBlock() instanceof GrowingPlantHeadBlock growingPlant) {
            if (!growingPlant.isMaxAge(hitState)) {
                world.setBlock(hitBlock, growingPlant.getMaxAgeState(hitState), Block.UPDATE_ALL);
                world.gameEvent(player, GameEvent.BLOCK_CHANGE, hitBlock);
                world.playSound(null, hitBlock, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
                shearsItem.hurtAndBreak(1, world, player instanceof ServerPlayer sp ? sp : null, item -> {});
                used = true;
            }
        }

        // Try shearing the pre-resolved entity target
        if (!used && preResolvedTarget != null && preResolvedTarget.isAlive()) {
            if (preResolvedTarget instanceof Shearable shearable && shearable.readyForShearing()) {
                shearable.shear(world, SoundSource.BLOCKS, shearsItem);
                shearsItem.hurtAndBreak(1, world, player instanceof ServerPlayer sp ? sp : null, item -> {});
                world.gameEvent(player, GameEvent.SHEAR, preResolvedTarget.blockPosition());
                used = true;
            }
        }

        dropIfNotEmpty(world, Vec3.atCenterOf(used ? hitBlock : adjacent), shearsItem);
    }

    private static void blowBucket(ServerLevel world, Player player, ItemStack bucketItem, TrajectoryResult tr) {
        Item bucketType = bucketItem.getItem();

        if (bucketType == Items.BUCKET) {
            BlockPos target = tr.hitBlock();
            ItemStack filledBucket = getFilledBucketForBlock(world.getBlockState(target));
            if (!filledBucket.isEmpty()) {
                world.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                dropItemAt(world, Vec3.atCenterOf(target), filledBucket);
                world.playSound(null, target, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                dropItemAt(world, tr.hitLocation(), bucketItem);
            }
        } else if (bucketType instanceof BucketItem bucketItemObj) {
            BlockPos target = tr.adjacentPos();
            if (bucketItemObj.emptyContents(player, world, target, null)) {
                dropItemAt(world, Vec3.atCenterOf(target), new ItemStack(Items.BUCKET));
            } else {
                dropItemAt(world, tr.hitLocation(), bucketItem);
            }
        }
    }

    private static void blowBlock(
            ServerLevel world, Player player, ItemStack takenItem, TrajectoryResult tr, BlockItem blockItem) {
        BlockPos target = tr.adjacentPos();
        Direction hitFace = tr.hitFace();
        ItemStack placeStack = takenItem.copy();
        placeStack.setCount(1);
        DirectionalPlaceContext ctx =
                new DirectionalPlaceContext(world, target, hitFace.getOpposite(), placeStack, hitFace);
        InteractionResult result = blockItem.place(ctx);
        if (!result.consumesAction()) {
            dropItemAt(world, Vec3.atCenterOf(target), takenItem);
        }
    }

    private static void blowEquippable(
            ServerLevel world, Player player, ItemStack takenItem, TrajectoryResult tr, @Nullable LivingEntity target) {
        if (target != null && target.isAlive()) {
            Equippable equippable = takenItem.get(DataComponents.EQUIPPABLE);
            if (equippable != null) {
                EquipmentSlot slot = equippable.slot();
                if (target.getItemBySlot(slot).isEmpty()) {
                    target.setItemSlot(slot, takenItem.copy());
                    world.playSound(
                            null,
                            target.blockPosition(),
                            SoundEvents.ARMOR_EQUIP_GENERIC.value(),
                            SoundSource.BLOCKS,
                            1.0F,
                            1.0F);
                    return;
                }
            }
        }
        dropItemAt(world, tr.hitLocation(), takenItem);
    }

    private static void blowDefault(
            ServerLevel world, Player player, ItemStack takenItem, TrajectoryResult tr, @Nullable LivingEntity target) {
        // Try to hand the item to the targeted entity: mainhand → offhand → drop
        if (target != null && target.isAlive()) {
            if (target.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
                target.setItemSlot(EquipmentSlot.MAINHAND, takenItem.copy());
                world.playSound(null, target.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
                return;
            }
            if (target.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) {
                target.setItemSlot(EquipmentSlot.OFFHAND, takenItem.copy());
                world.playSound(null, target.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
                return;
            }
        }
        Vec3 hitLoc = tr.hitLocation();
        dropItemAt(world, hitLoc, takenItem);
        world.playSound(
                null, BlockPos.containing(hitLoc), SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    // Shared helpers

    static TrajectoryResult computeTrajectory(ServerLevel world, Player player) {
        Vec3 pos = player.getEyePosition(1.0F);
        Vec3 vel = player.getViewVector(1.0F).normalize().scale(LAUNCH_POWER);
        List<Vec3> path = new ArrayList<>();
        path.add(pos);

        for (int tick = 0; tick < MAX_TRAJECTORY_TICKS; tick++) {
            Vec3 nextPos = pos.add(vel);
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

            if (pos.y < world.getMinY()) {
                BlockPos lastPos = BlockPos.containing(pos);
                return new TrajectoryResult(lastPos, lastPos.above(), Direction.UP, pos, vel, path);
            }
        }

        BlockPos lastPos = BlockPos.containing(pos);
        return new TrajectoryResult(lastPos, lastPos.above(), Direction.UP, pos, vel, path);
    }

    /**
     * Finds the nearest `LivingEntity` along a parabolic trajectory path (the first one, in flight order,
     * whose bounding box comes within a block of a path point). Used by blow actions that target entities
     * (equippable, shears, etc.). One entity query covers the whole path.
     */
    @Nullable
    private static LivingEntity findEntityAlongPath(ServerLevel world, Player player, List<Vec3> path) {
        if (path.isEmpty()) return null;

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (Vec3 point : path) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }
        AABB pathBounds = new AABB(minX - 1.0, minY - 1.0, minZ - 1.0, maxX + 1.0, maxY + 1.0, maxZ + 1.0);
        List<LivingEntity> mobs =
                world.getEntitiesOfClass(LivingEntity.class, pathBounds, e -> e.isAlive() && e != player);
        if (mobs.isEmpty()) return null;

        for (Vec3 point : path) {
            AABB searchBox =
                    new AABB(point.x - 1.0, point.y - 1.0, point.z - 1.0, point.x + 1.0, point.y + 1.0, point.z + 1.0);
            for (LivingEntity mob : mobs) {
                if (mob.getBoundingBox().intersects(searchBox)) return mob;
            }
        }
        return null;
    }

    private static ItemStack getFilledBucketForBlock(BlockState state) {
        if (state.is(Blocks.WATER) && state.getFluidState().isSource()) return new ItemStack(Items.WATER_BUCKET);
        if (state.is(Blocks.LAVA) && state.getFluidState().isSource()) return new ItemStack(Items.LAVA_BUCKET);
        if (state.is(Blocks.POWDER_SNOW)) return new ItemStack(Items.POWDER_SNOW_BUCKET);
        return ItemStack.EMPTY;
    }

    private static void insertOrDrop(Player player, ItemStack gunStack, ItemStack stack) {
        insertAllOrDrop(player, gunStack, List.of(stack));
    }

    /** Inserts the stacks into the gun, building its container once; whatever does not fit is dropped. */
    private static void insertAllOrDrop(Player player, ItemStack gunStack, List<ItemStack> stacks) {
        if (stacks.isEmpty()) return;
        Container inv = VacuumGunItem.getInventory(gunStack);
        for (ItemStack stack : stacks) {
            ItemStack leftover = VacuumGunItem.insertIntoInventory(inv, stack);
            if (!leftover.isEmpty()) {
                player.drop(leftover, false);
            }
        }
    }

    private static void dropItemAt(ServerLevel world, Vec3 pos, ItemStack stack) {
        ItemEntity ie = new ItemEntity(world, pos.x, pos.y, pos.z, stack.copy());
        ie.setPickUpDelay(20);
        world.addFreshEntity(ie);
    }

    private static void dropIfNotEmpty(ServerLevel world, Vec3 pos, ItemStack stack) {
        if (!stack.isEmpty()) {
            dropItemAt(world, pos, stack);
        }
    }
}
