package dev.gga.techextensions.items.tool.advanced;

import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.utils.ItemAnimationManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
        double range = TechExtensionsConfig.vacuumGunRange;
        Vec3 endPos = eyePos.add(lookDir.scale(range));

        BlockHitResult blockHit = world.clip(new ClipContext(
                eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        BlockHitResult fluidHit = world.clip(new ClipContext(
                eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, CollisionContext.empty()));

        double effectiveRange =
                blockHit.getType() != HitResult.Type.MISS ? eyePos.distanceTo(blockHit.getLocation()) : range;
        Entity hitEntity = findEntityAlongRay(world, player, eyePos, lookDir, effectiveRange);

        if (hitEntity instanceof LivingEntity living) {
            return vacuumMob(world, player, gunStack, living, eyePos);
        }
        if (hitEntity instanceof ItemEntity itemEntity) {
            return vacuumItem(world, player, gunStack, itemEntity, eyePos);
        }
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
        List<Vec3> trail = List.of(mobPos, eyePos);
        SpawnEggItem egg = SpawnEggItem.byId(living.getType());
        if (egg != null) {
            ItemStack eggStack = new ItemStack(egg);
            TagValueOutput output =
                    TagValueOutput.createWithContext(ProblemReporter.DISCARDING, world.registryAccess());
            living.saveWithoutId(output);
            CompoundTag entityTag = output.buildResult();
            eggStack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(living.getType(), entityTag));
            if (living.hasCustomName()) {
                eggStack.set(DataComponents.CUSTOM_NAME, living.getCustomName());
            }
            living.discard();
            ItemAnimationManager.schedule(world, trail, eggStack, () -> {
                insertOrDrop(player, gunStack, eggStack);
                world.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 0.8F);
            });
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
        List<Vec3> trail = List.of(itemPos, eyePos);
        ItemStack itemStack = itemEntity.getItem().copy();
        itemEntity.discard();
        ItemAnimationManager.schedule(world, trail, itemStack, () -> {
            insertOrDrop(player, gunStack, itemStack);
            world.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS,
                    0.2F,
                    ((world.random.nextFloat() - world.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
        });
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
        inventory.setChanged();
        world.setBlock(fluidPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        world.playSound(null, fluidPos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        Vec3 fluidCenter = Vec3.atCenterOf(fluidPos);
        List<Vec3> trail = List.of(fluidCenter, eyePos);
        ItemStack finalBucket = filledBucket;
        ItemAnimationManager.schedule(world, trail, filledBucket, () -> insertOrDrop(player, gunStack, finalBucket));
        return true;
    }

    private static boolean vacuumBlock(
            ServerLevel world, Player player, ItemStack gunStack, BlockHitResult blockHit, Vec3 eyePos) {
        BlockPos target = blockHit.getBlockPos();
        BlockState state = world.getBlockState(target);
        if (state.isAir() || state.is(Blocks.BEDROCK) || state.getDestroySpeed(world, target) < 0) return false;

        Vec3 blockCenter = Vec3.atCenterOf(target);
        List<Vec3> trail = List.of(blockCenter, eyePos);
        List<ItemStack> drops = Block.getDrops(state, world, target, world.getBlockEntity(target), player, gunStack);
        world.destroyBlock(target, false);
        ItemStack displayItem = drops.isEmpty() ? ItemStack.EMPTY : drops.getFirst();
        if (!displayItem.isEmpty()) {
            ItemAnimationManager.schedule(world, trail, displayItem, () -> {
                Container inv = VacuumGunItem.getInventory(gunStack);
                for (ItemStack drop : drops) {
                    ItemStack leftover = VacuumGunItem.insertIntoInventory(inv, drop);
                    if (!leftover.isEmpty()) {
                        player.drop(leftover, false);
                    }
                }
            });
        }
        return true;
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

        if (item == Items.TNT || (TechRebornConfig.nukeEnabled && item == nukeItem)) {
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
        if (item instanceof ShearsItem) {
            return () -> blowShears(world, player, takenItem, tr);
        }
        if (item instanceof BlockItem blockItem) {
            return () -> blowBlock(world, player, takenItem, tr, blockItem);
        }
        if (takenItem.has(DataComponents.EQUIPPABLE)) {
            return () -> blowEquippable(world, player, takenItem, tr);
        }
        return () -> blowDefault(world, takenItem, tr);
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
            player.drop(takenItem.copy(), false);
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

    private static void blowShears(ServerLevel world, Player player, ItemStack shearsItem, TrajectoryResult tr) {
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

        // Try shearing entities near the target
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
            player.drop(takenItem.copy(), false);
        }
    }

    private static void blowEquippable(ServerLevel world, Player player, ItemStack takenItem, TrajectoryResult tr) {
        BlockPos target = tr.adjacentPos();
        AABB searchBox = new AABB(target).inflate(1.0);
        List<LivingEntity> entities =
                world.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e.isAlive() && !e.isSpectator());
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
    }

    private static void blowDefault(ServerLevel world, ItemStack takenItem, TrajectoryResult tr) {
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

    @Nullable
    static Entity findEntityAlongRay(ServerLevel world, Player player, Vec3 start, Vec3 dir, double maxDist) {
        int steps = (int) Math.ceil(maxDist / 0.5);
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(dir.scale(i * 0.5));
            AABB searchBox =
                    new AABB(point.x - 1.0, point.y - 1.0, point.z - 1.0, point.x + 1.0, point.y + 1.0, point.z + 1.0);

            List<ItemEntity> items =
                    world.getEntitiesOfClass(ItemEntity.class, searchBox, e -> e.isAlive() && !e.hasPickUpDelay());
            if (!items.isEmpty()) return items.getFirst();

            List<LivingEntity> mobs =
                    world.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e.isAlive() && !(e instanceof Player));
            if (!mobs.isEmpty()) return mobs.getFirst();
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
        Container inv = VacuumGunItem.getInventory(gunStack);
        ItemStack leftover = VacuumGunItem.insertIntoInventory(inv, stack);
        if (!leftover.isEmpty()) {
            player.drop(leftover, false);
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
