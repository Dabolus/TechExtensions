package dev.gga.techextensions.blocks.machine;

import dev.gga.techextensions.blockentity.TEGuiType;
import dev.gga.techextensions.blockentity.machine.ElectricDuctedFanBlockEntity;
import dev.gga.techextensions.init.TEBlockSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import reborncore.api.ToolManager;
import reborncore.api.blockentity.IMachineGuiHandler;
import reborncore.common.BaseBlockEntityProvider;
import reborncore.common.blocks.BlockMachineBase;
import reborncore.common.blocks.BlockWrenchEventHandler;
import reborncore.common.util.WrenchUtils;

/**
 * Electric Ducted Fan block that can be stacked up to 4 times in a single block space.
 * Each fan is 4 pixels (1/4 block) thick. When stacked, their pushing power combines.
 */
public class ElectricDuctedFanBlock extends BaseBlockEntityProvider implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ACTIVE = BlockMachineBase.ACTIVE;
    public static final IntegerProperty FANS = IntegerProperty.create("fans", 1, 4);

    public static final int MAX_FANS = 4;

    private final IMachineGuiHandler gui = TEGuiType.ELECTRIC_DUCTED_FAN;

    /** Shapes indexed by [facing.ordinal()][fanCount-1] */
    protected final VoxelShape[][] shapes;

    private final double depth;
    private final double width;
    private final int cost;

    public ElectricDuctedFanBlock(int cost, double depth, double width, String name) {
        super(TEBlockSettings.fan(name));
        this.depth = depth;
        this.width = width;
        this.shapes = genAllShapes();
        this.cost = cost;
        this.registerDefaultState(this.getStateDefinition()
                .any()
                .setValue(FACING, Direction.UP)
                .setValue(WATERLOGGED, false)
                .setValue(ACTIVE, false)
                .setValue(FANS, 1));
        BlockWrenchEventHandler.wrenchableBlocks.add(this);
    }

    /** Generate shapes for all facing directions and fan counts (1-4) */
    private VoxelShape[][] genAllShapes() {
        VoxelShape[][] allShapes = new VoxelShape[6][MAX_FANS];
        for (Direction facing : Direction.values()) {
            for (int count = 1; count <= MAX_FANS; count++) {
                allShapes[facing.ordinal()][count - 1] = genStackedShape(facing, count);
            }
        }
        return allShapes;
    }

    /** Generate the combined VoxelShape for a given facing and fan count */
    private VoxelShape genStackedShape(Direction facing, int count) {
        double culling = (16.0 - width) / 2.0;
        double totalDepth = depth * count;

        return switch (facing) {
            case DOWN -> box(culling, 16.0 - totalDepth, culling, 16.0 - culling, 16.0, 16.0 - culling);
            case UP -> box(culling, 0.0, culling, 16.0 - culling, totalDepth, 16.0 - culling);
            case NORTH -> box(culling, culling, 16.0 - totalDepth, 16.0 - culling, 16.0 - culling, 16.0);
            case SOUTH -> box(culling, culling, 0.0, 16.0 - culling, 16.0 - culling, totalDepth);
            case WEST -> box(16.0 - totalDepth, culling, culling, 16.0, 16.0 - culling, 16.0 - culling);
            case EAST -> box(0.0, culling, culling, totalDepth, 16.0 - culling, 16.0 - culling);
        };
    }

    public static boolean isActive(BlockState state) {
        return state.hasProperty(ACTIVE) && state.getValue(ACTIVE);
    }

    public static int getFanCount(BlockState state) {
        return state.hasProperty(FANS) ? state.getValue(FANS) : 1;
    }

    public IMachineGuiHandler getGui() {
        return gui;
    }

    public static Direction getFacing(BlockState state) {
        return state.getValue(FACING);
    }

    public static void setActive(Boolean active, Level world, BlockPos pos) {
        BlockState currentState = world.getBlockState(pos);
        BlockState newState = currentState.setValue(ACTIVE, active);
        world.setBlock(pos, newState, 3);
    }

    public int getCost() {
        return cost;
    }

    // BaseBlockEntityProvider
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricDuctedFanBlockEntity(pos, state);
    }

    // Block
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, ACTIVE, FANS);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        // Allow replacing if the player is holding another fan and we haven't reached max
        if (!context.isSecondaryUseActive()
                && context.getItemInHand().getItem() == this.asItem()
                && state.getValue(FANS) < MAX_FANS) {
            return true;
        }
        return super.canBeReplaced(state, context);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos clickedPos = context.getClickedPos();
        BlockState existingState = context.getLevel().getBlockState(clickedPos);

        // If placing on an existing fan stack of the same type
        if (existingState.is(this)) {
            int currentFans = existingState.getValue(FANS);
            if (currentFans < MAX_FANS) {
                // Add another fan to the stack
                return existingState.setValue(FANS, currentFans + 1);
            }
            return null; // Can't place more
        }

        // New placement, determine facing based on look direction
        FluidState fluidState = context.getLevel().getFluidState(clickedPos);
        boolean inWaterSource = fluidState.is(Fluids.WATER) && fluidState.isSource();

        for (Direction facing : context.getNearestLookingDirections()) {
            BlockState state = this.defaultBlockState()
                    .setValue(FACING, facing.getOpposite())
                    // Waterlog if placing in water
                    .setValue(WATERLOGGED, inWaterSource);
            if (state.canSurvive(context.getLevel(), clickedPos)) {
                return state;
            }
        }
        return null;
    }

    @Override
    public VoxelShape getShape(
            BlockState blockState, BlockGetter blockView, BlockPos blockPos, CollisionContext shapeContext) {
        int fanCount = blockState.getValue(FANS);
        Direction facing = blockState.getValue(FACING);
        return shapes[facing.ordinal()][fanCount - 1];
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess scheduledTickAccess,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public InteractionResult useWithoutItem(
            BlockState state, Level worldIn, BlockPos pos, Player playerIn, BlockHitResult hitResult) {
        ItemStack stack = playerIn.getItemInHand(InteractionHand.MAIN_HAND);
        BlockEntity blockEntity = worldIn.getBlockEntity(pos);

        // Block entity should never be null
        if (blockEntity == null) {
            return InteractionResult.FAIL;
        }

        if (!stack.isEmpty() && ToolManager.INSTANCE.canHandleTool(stack)) {
            if (WrenchUtils.handleWrench(stack, worldIn, pos, playerIn, hitResult.getDirection())) {
                return InteractionResult.SUCCESS;
            }
        }

        // Open GUI, but only if not crouching and not holding another fan to stack
        if (gui != null && !playerIn.isCrouching() && stack.getItem() != this.asItem()) {
            gui.open(playerIn, pos, worldIn);
            return InteractionResult.SUCCESS;
        }

        return super.useWithoutItem(state, worldIn, pos, playerIn, hitResult);
    }
}
