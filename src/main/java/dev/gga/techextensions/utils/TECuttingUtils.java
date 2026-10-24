package dev.gga.techextensions.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class TECuttingUtils {
    public record FindWoodResult(List<BlockPos> wood, List<BlockPos> leaves) {}

    /** Limit the amount of wood to be broken. */
    private static final int MAX_WOOD = 64;
    /** Limit the amount of leaves searched for wood. */
    private static final int MAX_LEAVES = 256;

    private static final Direction[] SEARCH_ORDER =
            new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};

    private TECuttingUtils() {}

    public static boolean isValidLog(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    public static boolean isValidLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.WART_BLOCKS) || state.is(Blocks.SHROOMLIGHT);
    }

    public static boolean isValidTreeCapitateStartBlock(BlockState state) {
        return isValidLog(state) || isValidLeaves(state);
    }

    /** Finds the logs and leaves connected to {@code pos}. The start position itself is not included. */
    public static FindWoodResult findWood(Level world, BlockPos pos) {
        // Sets keep the visited checks O(1); insertion order is preserved so blocks break in the same order
        Set<BlockPos> wood = new LinkedHashSet<>();
        Set<BlockPos> leaves = new LinkedHashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        visited.add(pos);
        recursivelyFindWood(world, pos, wood, leaves, visited);
        return new FindWoodResult(new ArrayList<>(wood), new ArrayList<>(leaves));
    }

    private static void recursivelyFindWood(
            Level world, BlockPos pos, Set<BlockPos> wood, Set<BlockPos> leaves, Set<BlockPos> visited) {
        if (wood.size() >= MAX_WOOD || leaves.size() >= MAX_LEAVES) {
            return;
        }
        for (Direction facing : SEARCH_ORDER) {
            BlockPos checkPos = pos.relative(facing);
            if (!visited.add(checkPos)) {
                continue;
            }
            BlockState state = world.getBlockState(checkPos);
            if (isValidLog(state)) {
                wood.add(checkPos);
                recursivelyFindWood(world, checkPos, wood, leaves, visited);
            } else if (isValidLeaves(state)) {
                leaves.add(checkPos);
                recursivelyFindWood(world, checkPos, wood, leaves, visited);
            }
        }
    }
}
