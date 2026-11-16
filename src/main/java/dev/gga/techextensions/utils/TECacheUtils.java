package dev.gga.techextensions.utils;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Generic cache utilities for items that store computed values in
 * their `CustomData` NBT.
 */
public final class TECacheUtils {

    private TECacheUtils() {}

    /**
     * Retrieves a cached value from the stack's CustomData, recomputing
     * the cache if the key is missing. Reads never copy the tag.
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
            Function<CompoundTag, T> extractor,
            Function<ItemStack, Container> inventoryGetter,
            BiConsumer<ItemStack, Container> cacheUpdater) {
        if (stack == null) {
            return defaultValue;
        }
        CompoundTag tag = TENbtUtils.view(stack);
        if (tag.contains(key)) {
            return extractor.apply(tag);
        }
        cacheUpdater.accept(stack, inventoryGetter.apply(stack));
        tag = TENbtUtils.view(stack);
        return tag.contains(key) ? extractor.apply(tag) : defaultValue;
    }
}
