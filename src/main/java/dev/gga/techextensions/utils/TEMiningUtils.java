package dev.gga.techextensions.utils;

import dev.gga.techextensions.config.TechExtensionsConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import techreborn.utils.ToolsUtil;

public final class TEMiningUtils {
    public static boolean isValidOre(BlockState state) {
        // Consider it an ore if it has a tag that starts with ores/
        return state.getTags().anyMatch(tag -> tag.location().getPath().startsWith("ores/"));
    }

    public static boolean isValidVeinMineStartBlock(BlockState state) {
        return isValidOre(state);
    }

    private static boolean shouldBreak(Player playerIn, Level worldIn, BlockPos originalPos, BlockPos pos) {
        if (originalPos.equals(pos)) {
            return false;
        }
        BlockState blockState = worldIn.getBlockState(pos);
        if (blockState.isAir()) {
            return false;
        }
        if (blockState.liquid()) {
            return false;
        }
        float blockHardness = blockState.getDestroyProgress(playerIn, worldIn, pos);
        if (blockHardness == -1.0F) {
            return false;
        }
        float originalHardness = worldIn.getBlockState(originalPos).getDestroySpeed(worldIn, originalPos);
        return !((originalHardness / blockHardness) > 10.0F);
    }

    public static void mineAoe3x3(
            ItemStack stack, Level worldIn, Player playerIn, BlockPos pos, LivingEntity entityLiving) {
        for (BlockPos additionalPos : ToolsUtil.getAOEMiningBlocks(worldIn, pos, entityLiving, 1)) {
            if (shouldBreak(playerIn, worldIn, pos, additionalPos)) {
                ToolsUtil.breakBlock(stack, worldIn, additionalPos, entityLiving, TechExtensionsConfig.metaToolCost);
            }
        }
    }

    public static List<BlockPos> findVein(Level world, BlockPos pos) {
        List<BlockPos> foundOres = new ArrayList<>();
        recursivelyFindVein(world, pos, foundOres);
        return foundOres;
    }

    private static void recursivelyFindVein(Level world, BlockPos pos, List<BlockPos> foundOres) {
        // Limit the amount of ores to be broken to 64 blocks.
        if (foundOres.size() >= 64) {
            return;
        }
        for (Direction facing : Direction.values()) {
            BlockPos checkPos = pos.relative(facing);
            if (!foundOres.contains(checkPos)) {
                BlockState state = world.getBlockState(checkPos);

                if (isValidOre(state)) {
                    foundOres.add(checkPos);
                    recursivelyFindVein(world, checkPos, foundOres);
                }
            }
        }
    }
}
