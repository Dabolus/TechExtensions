package dev.gga.techextensions.utils;

import dev.gga.techextensions.config.TechExtensionsConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import techreborn.utils.ToolsUtil;

public final class TEMiningUtils {
    /** Limit the amount of ores to be broken. */
    private static final int MAX_ORES = 64;
    /** Per-block memo of the ore tag check. Tags only change on (re)load, which clears the cache. */
    private static final Map<Block, Boolean> ORE_CACHE = new ConcurrentHashMap<>();

    private TEMiningUtils() {}

    public static void init() {
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> ORE_CACHE.clear());
    }

    public static boolean isValidOre(BlockState state) {
        // Consider it an ore if it has a tag that starts with ores/
        return ORE_CACHE.computeIfAbsent(
                state.getBlock(),
                block -> state.typeHolder()
                        .tags()
                        .anyMatch(tag -> tag.location().getPath().startsWith("ores/")));
    }

    public static boolean isValidVeinMineStartBlock(BlockState state) {
        return isValidOre(state);
    }

    /**
     * Breaks the 3x3 around {@code pos}, like Tech Reborn's industrial drill does: every neighbour except air,
     * liquids and unbreakable blocks ({@code ToolsUtil.breakBlock} skips the latter and charges the energy).
     */
    public static void mineAoe3x3(ItemStack stack, Level worldIn, BlockPos pos, LivingEntity entityLiving) {
        for (BlockPos additionalPos : ToolsUtil.getAOEMiningBlocks(worldIn, pos, entityLiving, 1)) {
            if (shouldBreak(worldIn, pos, additionalPos)) {
                ToolsUtil.breakBlock(
                        stack, worldIn, additionalPos, entityLiving, TechExtensionsConfig.metaToolCost.get());
            }
        }
    }

    private static boolean shouldBreak(Level worldIn, BlockPos originalPos, BlockPos pos) {
        if (originalPos.equals(pos)) {
            return false;
        }
        BlockState blockState = worldIn.getBlockState(pos);
        return !blockState.isAir() && !(blockState.getBlock() instanceof LiquidBlock);
    }

    /** Finds the ores connected to {@code pos}. The start position itself is not included. */
    public static List<BlockPos> findVein(Level world, BlockPos pos) {
        // Sets keep the visited checks O(1); insertion order is preserved so blocks break in the same order
        Set<BlockPos> foundOres = new LinkedHashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        visited.add(pos);
        recursivelyFindVein(world, pos, foundOres, visited);
        return new ArrayList<>(foundOres);
    }

    private static void recursivelyFindVein(Level world, BlockPos pos, Set<BlockPos> foundOres, Set<BlockPos> visited) {
        if (foundOres.size() >= MAX_ORES) {
            return;
        }
        for (Direction facing : Direction.values()) {
            BlockPos checkPos = pos.relative(facing);
            if (!visited.add(checkPos)) {
                continue;
            }
            if (isValidOre(world.getBlockState(checkPos))) {
                foundOres.add(checkPos);
                recursivelyFindVein(world, checkPos, foundOres, visited);
            }
        }
    }
}
