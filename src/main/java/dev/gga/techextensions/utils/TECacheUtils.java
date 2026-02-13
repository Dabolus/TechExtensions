package dev.gga.techextensions.utils;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Generic cache utilities for items that store computed values in
 * their `CustomData` NBT.
 */
public final class TECacheUtils {

    private TECacheUtils() {}

    /**
     * Retrieves a cached value from the stack's CustomData, recomputing
     * the cache if the key is missing.
     *
     * @param stack           the item stack
     * @param key             the NBT key for the cached value
     * @param defaultValue    fallback if both cache and recomputation fail
     * @param extractor       reads the value from a `CompoundTag`
     * @param inventoryGetter returns the item's inventory container
     * @param cacheUpdater    recomputes and writes all cache entries
     */
    public static <T> T getCachedValue(
            ItemStack stack,
            String key,
            T defaultValue,
            Function<CompoundTag, Optional<T>> extractor,
            Function<ItemStack, Container> inventoryGetter,
            BiConsumer<ItemStack, Container> cacheUpdater) {
        if (stack == null) {
            return defaultValue;
        }
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains(key)) {
            return extractor.apply(tag).orElse(defaultValue);
        }
        Container inv = inventoryGetter.apply(stack);
        cacheUpdater.accept(stack, inv);
        return extractor
                .apply(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag())
                .orElse(defaultValue);
    }
}
