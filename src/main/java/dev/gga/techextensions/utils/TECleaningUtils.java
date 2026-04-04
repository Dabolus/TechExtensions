package dev.gga.techextensions.utils;

import com.google.common.collect.ImmutableMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Shared cleaning logic used by both {@code SoapItem} and {@code BubbleGunItem}.
 *
 * Handles three categories:
 * - Oxidized copper: reverts one oxidation level via `WeatheringCopper.getPrevious(Block)`
 * - Waxed copper: removes wax, yielding the un-waxed equivalent
 * - Mossy blocks: removes moss (mossy cobblestone → cobblestone, etc.)
 */
public final class TECleaningUtils {

    // Waxed → Un-waxed mapping (reverse of HoneycombItem.WAXABLES)
    private static final Map<Block, Block> WAX_OFF_MAP;

    // Mossy → Clean mapping
    private static final Map<Block, Block> MOSSY_MAP;

    static {
        // Build waxed → un-waxed map
        // These are the same pairings defined in HoneycombItem.WAXABLES but reversed
        Map<Block, Block> wax = new IdentityHashMap<>();
        wax.put(Blocks.WAXED_COPPER_BLOCK, Blocks.COPPER_BLOCK);
        wax.put(Blocks.WAXED_EXPOSED_COPPER, Blocks.EXPOSED_COPPER);
        wax.put(Blocks.WAXED_WEATHERED_COPPER, Blocks.WEATHERED_COPPER);
        wax.put(Blocks.WAXED_OXIDIZED_COPPER, Blocks.OXIDIZED_COPPER);
        wax.put(Blocks.WAXED_CUT_COPPER, Blocks.CUT_COPPER);
        wax.put(Blocks.WAXED_EXPOSED_CUT_COPPER, Blocks.EXPOSED_CUT_COPPER);
        wax.put(Blocks.WAXED_WEATHERED_CUT_COPPER, Blocks.WEATHERED_CUT_COPPER);
        wax.put(Blocks.WAXED_OXIDIZED_CUT_COPPER, Blocks.OXIDIZED_CUT_COPPER);
        wax.put(Blocks.WAXED_CUT_COPPER_STAIRS, Blocks.CUT_COPPER_STAIRS);
        wax.put(Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS, Blocks.EXPOSED_CUT_COPPER_STAIRS);
        wax.put(Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS, Blocks.WEATHERED_CUT_COPPER_STAIRS);
        wax.put(Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS, Blocks.OXIDIZED_CUT_COPPER_STAIRS);
        wax.put(Blocks.WAXED_CUT_COPPER_SLAB, Blocks.CUT_COPPER_SLAB);
        wax.put(Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB, Blocks.EXPOSED_CUT_COPPER_SLAB);
        wax.put(Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB, Blocks.WEATHERED_CUT_COPPER_SLAB);
        wax.put(Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB, Blocks.OXIDIZED_CUT_COPPER_SLAB);
        wax.put(Blocks.WAXED_COPPER_DOOR, Blocks.COPPER_DOOR);
        wax.put(Blocks.WAXED_EXPOSED_COPPER_DOOR, Blocks.EXPOSED_COPPER_DOOR);
        wax.put(Blocks.WAXED_WEATHERED_COPPER_DOOR, Blocks.WEATHERED_COPPER_DOOR);
        wax.put(Blocks.WAXED_OXIDIZED_COPPER_DOOR, Blocks.OXIDIZED_COPPER_DOOR);
        wax.put(Blocks.WAXED_COPPER_TRAPDOOR, Blocks.COPPER_TRAPDOOR);
        wax.put(Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR, Blocks.EXPOSED_COPPER_TRAPDOOR);
        wax.put(Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR, Blocks.WEATHERED_COPPER_TRAPDOOR);
        wax.put(Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR, Blocks.OXIDIZED_COPPER_TRAPDOOR);
        wax.put(Blocks.WAXED_COPPER_GRATE, Blocks.COPPER_GRATE);
        wax.put(Blocks.WAXED_EXPOSED_COPPER_GRATE, Blocks.EXPOSED_COPPER_GRATE);
        wax.put(Blocks.WAXED_WEATHERED_COPPER_GRATE, Blocks.WEATHERED_COPPER_GRATE);
        wax.put(Blocks.WAXED_OXIDIZED_COPPER_GRATE, Blocks.OXIDIZED_COPPER_GRATE);
        wax.put(Blocks.WAXED_COPPER_BULB, Blocks.COPPER_BULB);
        wax.put(Blocks.WAXED_EXPOSED_COPPER_BULB, Blocks.EXPOSED_COPPER_BULB);
        wax.put(Blocks.WAXED_WEATHERED_COPPER_BULB, Blocks.WEATHERED_COPPER_BULB);
        wax.put(Blocks.WAXED_OXIDIZED_COPPER_BULB, Blocks.OXIDIZED_COPPER_BULB);
        wax.put(Blocks.WAXED_CHISELED_COPPER, Blocks.CHISELED_COPPER);
        wax.put(Blocks.WAXED_EXPOSED_CHISELED_COPPER, Blocks.EXPOSED_CHISELED_COPPER);
        wax.put(Blocks.WAXED_WEATHERED_CHISELED_COPPER, Blocks.WEATHERED_CHISELED_COPPER);
        wax.put(Blocks.WAXED_OXIDIZED_CHISELED_COPPER, Blocks.OXIDIZED_CHISELED_COPPER);
        WAX_OFF_MAP = ImmutableMap.copyOf(wax);

        // Build mossy → clean map
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
     */
    public static boolean isCleanable(BlockState state) {
        Block block = state.getBlock();
        // Oxidized copper (un-waxed): has a previous oxidation stage
        if (WeatheringCopper.getPrevious(block).isPresent()) {
            return true;
        }
        // Waxed copper
        if (WAX_OFF_MAP.containsKey(block)) {
            return true;
        }
        // Mossy blocks
        return MOSSY_MAP.containsKey(block);
    }

    /**
     * Returns the cleaned variant of the given block state, preserving all
     * block-state properties (facing, half, waterlogged, etc.).
     *
     * <p>Priority: waxed copper → oxidized copper → mossy.
     *
     * @return the cleaned state, or {@code null} if the block is not cleanable
     */
    public static BlockState getCleanedState(BlockState state) {
        Block block = state.getBlock();

        // 1. Waxed copper → un-waxed equivalent
        Block unwaxed = WAX_OFF_MAP.get(block);
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
