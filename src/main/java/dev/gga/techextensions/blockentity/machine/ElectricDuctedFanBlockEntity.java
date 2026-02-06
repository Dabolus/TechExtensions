package dev.gga.techextensions.blockentity.machine;

import dev.gga.techextensions.blocks.machine.ElectricDuctedFanBlock;
import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.init.TEBlockEntities;
import dev.gga.techextensions.init.TEContent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import reborncore.api.IToolDrop;
import reborncore.common.blockentity.MachineBaseBlockEntity;
import reborncore.common.powerSystem.PowerAcceptorBlockEntity;
import reborncore.common.screen.BuiltScreenHandler;
import reborncore.common.screen.BuiltScreenHandlerProvider;
import reborncore.common.screen.builder.ScreenHandlerBuilder;

public class ElectricDuctedFanBlockEntity extends PowerAcceptorBlockEntity
        implements IToolDrop, BuiltScreenHandlerProvider {

    // Configuration constants for push mechanics
    private static final double BASE_PUSH_STRENGTH = 0.05;
    private static final double MAX_PUSH_STRENGTH = 1.0;
    private static final int BASE_REACH = 1;
    private static final int MAX_REACH = 21;

    // Keep track of the last tick so that we can display particles every x ticks
    private long lastParticlesTick = 0;

    public ElectricDuctedFanBlockEntity(BlockPos pos, BlockState state) {
        super(TEBlockEntities.ELECTRIC_DUCTED_FAN, pos, state);
    }

    // PowerAcceptorBlockEntity
    @Override
    public void tick(Level world, BlockPos pos, BlockState state, MachineBaseBlockEntity blockEntity) {
        super.tick(world, pos, state, blockEntity);
        if (world == null) {
            return;
        }
        if ((state.getBlock() instanceof ElectricDuctedFanBlock)) {
            long minCost = TechExtensionsConfig.electricDuctedFanMinEnergyCost;
            long maxCost = TechExtensionsConfig.electricDuctedFanMaxEnergyCost;
            long stored = getEnergy();
            boolean shouldProcess = world.isClientSide() ? ElectricDuctedFanBlock.isActive(state) : (stored > minCost);
            if (shouldProcess) {
                long usedEnergy = Math.min(stored, maxCost);
                Direction facing = ElectricDuctedFanBlock.getFacing(state);
                int fanCount = ElectricDuctedFanBlock.getFanCount(state);
                // Calculate power ratio (0.0 to 1.0) based on energy used
                double powerRatio = (double) (usedEnergy - minCost) / (maxCost - minCost);
                powerRatio = Math.max(0.0, Math.min(1.0, powerRatio));
                // Stacked fans increase both reach and strength
                double fanMultiplier = 1.0 + (fanCount - 1) * 0.52; // 1x, 1.52x, 2.04x, 2.56x

                if (world.isClientSide()) {
                    long currentTick = world.getGameTime();
                    long particlesCooldown = 10L - Math.round(computeRangedValue(0.0, 9.0, powerRatio, fanMultiplier));
                    if (currentTick - lastParticlesTick < particlesCooldown) {
                        return;
                    }
                    lastParticlesTick = currentTick;

                    spawnParticles(world, pos, facing, powerRatio, fanCount);
                } else {
                    useEnergy(usedEnergy);
                    // Push entities in front of the fan
                    pushEntities(world, pos, facing, powerRatio, fanMultiplier);

                    if (!ElectricDuctedFanBlock.isActive(state)) {
                        ElectricDuctedFanBlock.setActive(true, world, pos);
                    }
                }
            } else if (!world.isClientSide() && ElectricDuctedFanBlock.isActive(state)) {
                ElectricDuctedFanBlock.setActive(false, world, pos);
            }
        }
    }

    public double computeReach(double powerRatio, double fanMultiplier) {
        return computeRangedValue(BASE_REACH, MAX_REACH, powerRatio, fanMultiplier);
    }

    public double computePushStrength(double powerRatio, double fanMultiplier) {
        return computeRangedValue(BASE_PUSH_STRENGTH, MAX_PUSH_STRENGTH, powerRatio, fanMultiplier);
    }

    private static double computeRangedValue(double base, double max, double powerRatio, double fanMultiplier) {
        return (base + powerRatio * (max - base)) * fanMultiplier;
    }

    /**
     * Push entities in front of the fan based on energy usage. The more energy used, the stronger
     * the push and the further the reach. Multiple stacked fans multiply the effect.
     */
    private void pushEntities(Level world, BlockPos pos, Direction facing, double powerRatio, double fanMultiplier) {
        // Calculate reach and push strength based on power ratio
        double reach = computeReach(powerRatio, fanMultiplier);
        double pushStrength = computePushStrength(powerRatio, fanMultiplier);

        // Create AABB in front of the fan
        AABB pushArea = createPushArea(pos, facing, (int) Math.round(reach));

        // Get all entities in the push area
        List<Entity> entities = world.getEntities((Entity) null, pushArea, entity -> !entity.isSpectator());

        // Push direction vector
        Vec3 pushDir = Vec3.atLowerCornerOf(facing.getUnitVec3i());

        for (Entity entity : entities) {
            // Calculate distance-based falloff (entities closer to fan get pushed more)
            double distance = getDistanceFromFan(entity.position(), pos, facing);
            double distanceFalloff = 1.0 - (distance / (reach + 1));
            distanceFalloff = Math.max(0.1, distanceFalloff); // Minimum 10% push at max range

            // Calculate final push velocity
            double finalStrength = pushStrength * distanceFalloff;
            Vec3 pushVelocity = pushDir.scale(finalStrength);

            // Add to entity's current motion
            Vec3 currentMotion = entity.getDeltaMovement();
            entity.setDeltaMovement(currentMotion.add(pushVelocity));
            entity.hurtMarked = true; // Force motion sync to client

            // Reset fall distance for upward fans to prevent fall damage
            if (facing == Direction.UP) {
                entity.resetFallDistance();
            }
        }
    }

    /** Create an AABB extending from the fan face in the facing direction */
    private AABB createPushArea(BlockPos pos, Direction facing, int reach) {
        // Start from the block center, expand to cover the area in front
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        // The push area starts slightly inside the block to catch entities standing right at the edge
        // and extends outward in the facing direction
        return switch (facing) {
            case UP -> new AABB(x, y + 0.25, z, x + 1, y + 1 + reach, z + 1);
            case DOWN -> new AABB(x, y - reach, z, x + 1, y + 0.75, z + 1);
            case NORTH -> new AABB(x, y, z - reach, x + 1, y + 1, z + 0.75);
            case SOUTH -> new AABB(x, y, z + 0.25, x + 1, y + 1, z + 1 + reach);
            case WEST -> new AABB(x - reach, y, z, x + 0.75, y + 1, z + 1);
            case EAST -> new AABB(x + 0.25, y, z, x + 1 + reach, y + 1, z + 1);
        };
    }

    /** Calculate the distance from the fan face along the facing axis */
    private double getDistanceFromFan(Vec3 entityPos, BlockPos fanPos, Direction facing) {
        return switch (facing) {
            case UP -> entityPos.y - (fanPos.getY() + 1);
            case DOWN -> fanPos.getY() - entityPos.y;
            case NORTH -> fanPos.getZ() - entityPos.z;
            case SOUTH -> entityPos.z - (fanPos.getZ() + 1);
            case WEST -> fanPos.getX() - entityPos.x;
            case EAST -> entityPos.x - (fanPos.getX() + 1);
        };
    }

    /** Spawn particles in front of the fan. Uses bubbles underwater, or clouds otherwise. */
    private void spawnParticles(Level world, BlockPos pos, Direction facing, double powerRatio, int fanCount) {
        RandomSource random = world.getRandom();

        // Each fan is 4 pixels (0.25 blocks) thick
        double stackHeight = fanCount * 0.25;
        double particlesOffset = stackHeight + 0.3;

        // Check if the block in front of the fan is water
        BlockPos frontPos = pos.relative(facing);
        boolean isUnderwater = world.getFluidState(frontPos).is(Fluids.WATER)
                || world.getFluidState(frontPos).is(Fluids.FLOWING_WATER);

        // More particles with more stacked fans and higher power
        int baseParticles = 1 + (int) Math.round(powerRatio * 8);
        int particleCount = baseParticles + random.nextInt(fanCount);

        for (int i = 0; i < particleCount; i++) {
            // Random position on the fan face (centered on 12x12 face)
            double offsetX = 0.3 + random.nextDouble() * 0.4;
            double offsetY = 0.3 + random.nextDouble() * 0.4;
            double offsetZ = 0.3 + random.nextDouble() * 0.4;

            // Calculate spawn position based on facing and stack height
            double x, y, z;
            double baseSpeed = 0.05 + powerRatio * 0.2;
            double speed = baseSpeed + random.nextDouble() * 0.1 * fanCount;
            double vx = 0, vy = 0, vz = 0;

            switch (facing) {
                case UP -> {
                    x = pos.getX() + offsetX;
                    y = pos.getY() + particlesOffset;
                    z = pos.getZ() + offsetZ;
                    vy = speed;
                }
                case DOWN -> {
                    x = pos.getX() + offsetX;
                    y = pos.getY() + (1.0 - particlesOffset);
                    z = pos.getZ() + offsetZ;
                    vy = -speed;
                }
                case NORTH -> {
                    x = pos.getX() + offsetX;
                    y = pos.getY() + offsetY;
                    z = pos.getZ() + (1.0 - particlesOffset);
                    vz = -speed;
                }
                case SOUTH -> {
                    x = pos.getX() + offsetX;
                    y = pos.getY() + offsetY;
                    z = pos.getZ() + particlesOffset;
                    vz = speed;
                }
                case WEST -> {
                    x = pos.getX() + (1.0 - particlesOffset);
                    y = pos.getY() + offsetY;
                    z = pos.getZ() + offsetZ;
                    vx = -speed;
                }
                case EAST -> {
                    x = pos.getX() + particlesOffset;
                    y = pos.getY() + offsetY;
                    z = pos.getZ() + offsetZ;
                    vx = speed;
                }
                default -> {
                    x = pos.getX() + 0.5;
                    y = pos.getY() + 0.5;
                    z = pos.getZ() + 0.5;
                }
            }

            world.addParticle(
                    isUnderwater ? ParticleTypes.BUBBLE_COLUMN_UP : ParticleTypes.CLOUD,
                    x,
                    y,
                    z,
                    vx * powerRatio * 30,
                    vy * powerRatio * 30,
                    vz * powerRatio * 30);
        }
    }

    @Override
    public long getBaseMaxPower() {
        return TechExtensionsConfig.electricDuctedFanMaxEnergy;
    }

    @Override
    protected boolean canAcceptEnergy(@Nullable Direction side) {
        // Accept energy from any side except the front
        return side == null || getFacing() != Direction.values()[side.ordinal()];
    }

    @Override
    public boolean canProvideEnergy(@Nullable Direction side) {
        return false;
    }

    @Override
    public long getBaseMaxOutput() {
        return 0;
    }

    @Override
    public long getBaseMaxInput() {
        return TechExtensionsConfig.electricDuctedFanMaxInput;
    }

    // MachineBaseBlockEntity
    @Override
    public Direction getFacing() {
        if (level == null) {
            return Direction.NORTH;
        }
        return ElectricDuctedFanBlock.getFacing(level.getBlockState(worldPosition));
    }

    @Override
    public boolean hasSlotConfig() {
        return false;
    }

    @Override
    public boolean canBeUpgraded() {
        return false;
    }

    // IToolDrop
    @Override
    public ItemStack getToolDrop(Player player) {
        return new ItemStack(TEContent.ELECTRIC_DUCTED_FAN);
    }

    // BuiltScreenHandlerProvider
    @Override
    public BuiltScreenHandler createScreenHandler(int syncID, Player player) {
        return new ScreenHandlerBuilder("electric_ducted_fan")
                .player(player.getInventory())
                .inventory()
                .hotbar()
                .addInventory()
                .blockEntity(this)
                .syncEnergyValue()
                .addInventory()
                .create(this, syncID);
    }
}
