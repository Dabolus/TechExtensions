package dev.gga.techextensions.utils;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Shared cleaning logic used by both {@code SoapItem} and {@code BubbleGunItem}.
 *
 * Handles four categories:
 * - Oxidized copper: reverts one oxidation level via `WeatheringCopper.getPrevious(Block)`
 * - Waxed blocks: removes wax using vanilla's `HoneycombItem.WAX_OFF_BY_BLOCK`
 * - Waxed signs: removes wax from sign block entities
 * - Mossy blocks: removes moss (mossy cobblestone → cobblestone, etc.)
 */
public final class TECleaningUtils {

    // Mossy → Clean mapping
    private static final Map<Block, Block> MOSSY_MAP;

    static {
        MOSSY_MAP = ImmutableMap.<Block, Block>builder()
                .put(Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE)
                .put(Blocks.MOSSY_COBBLESTONE_STAIRS, Blocks.COBBLESTONE_STAIRS)
                .put(Blocks.MOSSY_COBBLESTONE_SLAB, Blocks.COBBLESTONE_SLAB)
                .put(Blocks.MOSSY_COBBLESTONE_WALL, Blocks.COBBLESTONE_WALL)
                .put(Blocks.MOSSY_STONE_BRICKS, Blocks.STONE_BRICKS)
                .put(Blocks.MOSSY_STONE_BRICK_STAIRS, Blocks.STONE_BRICK_STAIRS)
                .put(Blocks.MOSSY_STONE_BRICK_SLAB, Blocks.STONE_BRICK_SLAB)
                .put(Blocks.MOSSY_STONE_BRICK_WALL, Blocks.STONE_BRICK_WALL)
                .build();
    }

    private TECleaningUtils() {}

    /**
     * Returns {@code true} if the given block state can be cleaned by soap / bubble gun.
     * For signs, use {@link #isWaxedSign(Level, BlockPos)} instead.
     */
    public static boolean isCleanable(BlockState state) {
        Block block = state.getBlock();
        // Oxidized copper (un-waxed): has a previous oxidation stage
        if (WeatheringCopper.getPrevious(block).isPresent()) {
            return true;
        }
        // Waxed blocks (vanilla map covers all waxed copper variants)
        if (HoneycombItem.WAX_OFF_BY_BLOCK.get().containsKey(block)) {
            return true;
        }
        // Mossy blocks
        return MOSSY_MAP.containsKey(block);
    }

    /**
     * Returns {@code true} if the block at the given position is a waxed sign.
     */
    public static boolean isWaxedSign(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            return sign.isWaxed();
        }
        return false;
    }

    /**
     * Returns {@code true} if the given block state can be cleaned, or
     * is a waxed sign at the given position.
     */
    public static boolean isCleanable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return isCleanable(state) || isWaxedSign(level, pos);
    }

    /**
     * Cleans the block at the given position. For waxed signs, removes the wax
     * from the block entity. For other blocks, replaces the block state.
     *
     * @return {@code true} if the block was cleaned
     */
    public static boolean cleanBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // Try cleaning the block state (waxed copper, oxidized copper, mossy)
        BlockState cleaned = getCleanedState(state);
        if (cleaned != null) {
            level.setBlockAndUpdate(pos, cleaned);
            return true;
        }

        // Try removing wax from signs
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign && sign.isWaxed()) {
            sign.setWaxed(false);
            return true;
        }

        return false;
    }

    /**
     * Returns the cleaned variant of the given block state, preserving all
     * block-state properties (facing, half, waterlogged, etc.).
     *
     * <p>Priority: waxed blocks → oxidized copper → mossy.
     *
     * @return the cleaned state, or {@code null} if the block is not cleanable
     */
    public static BlockState getCleanedState(BlockState state) {
        Block block = state.getBlock();

        // 1. Waxed blocks → un-waxed equivalent (uses vanilla's complete map)
        Block unwaxed = HoneycombItem.WAX_OFF_BY_BLOCK.get().get(block);
        if (unwaxed != null) {
            return copyProperties(state, unwaxed);
        }

        // 2. Oxidized copper → previous oxidation level
        Optional<Block> prevCopper = WeatheringCopper.getPrevious(block);
        if (prevCopper.isPresent()) {
            return copyProperties(state, prevCopper.get());
        }

        // 3. Mossy → clean
        Block clean = MOSSY_MAP.get(block);
        if (clean != null) {
            return copyProperties(state, clean);
        }

        return null;
    }

    /**
     * Copies all matching block-state properties from `source` to the
     * default state of `targetBlock`. Properties that do not exist on
     * the target are silently dropped.
     */
    private static BlockState copyProperties(BlockState source, Block targetBlock) {
        BlockState target = targetBlock.defaultBlockState();
        for (var property : source.getProperties()) {
            if (target.hasProperty(property)) {
                target = copyProperty(source, target, property);
            }
        }
        return target;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState copyProperty(
            BlockState source, BlockState target, Property<T> property) {
        return target.setValue(property, source.getValue(property));
    }
}
