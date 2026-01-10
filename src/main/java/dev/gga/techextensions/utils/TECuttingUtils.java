package dev.gga.techextensions.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class TECuttingUtils {
    public record FindWoodResult(List<BlockPos> wood, List<BlockPos> leaves) {}

    private static final Direction[] SEARCH_ORDER = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};

    public static boolean isValidLog(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    public static boolean isValidLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.WART_BLOCKS) || state.is(Blocks.SHROOMLIGHT);
    }

    public static boolean isValidTreeCapitateStartBlock(BlockState state) {
        return isValidLog(state) || isValidLeaves(state);
    }

    public static FindWoodResult findWood(Level world, BlockPos pos) {
        List<BlockPos> wood = new ArrayList<>();
        List<BlockPos> leaves = new ArrayList<>();
        recursivelyFindWood(world, pos, wood, leaves);
        return new FindWoodResult(wood, leaves);
    }

    private static void recursivelyFindWood(Level world, BlockPos pos, List<BlockPos> wood, List<BlockPos> leaves) {
        // Limit the amount of wood to be broken to 64 blocks.
        if (wood.size() >= 64) { return; }
        // Search 256 leaves for wood.
        if (leaves.size() >= 256) { return; }
        for (Direction facing : SEARCH_ORDER) {
            BlockPos checkPos = pos.relative(facing);
            if (!wood.contains(checkPos) && !leaves.contains(checkPos)) {
                BlockState state = world.getBlockState(checkPos);

                if (isValidLog(state)) {
                    wood.add(checkPos);
                    recursivelyFindWood(world, checkPos, wood, leaves);
                } else if (isValidLeaves(state)) {
                    leaves.add(checkPos);
                    recursivelyFindWood(world, checkPos, wood, leaves);
                }
            }
        }
    }

}
