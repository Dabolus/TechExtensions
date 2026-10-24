package dev.gga.techextensions.utils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Read-only access to item custom data without copying the tag on every read. The backing field is opened
 * by the class tweaker; see {@code techextensions.classtweaker}.
 */
public final class TENbtUtils {
    private static final CompoundTag EMPTY = new CompoundTag();

    private TENbtUtils() {}

    /**
     * Returns the stack's custom data tag for reading. This is the live component value: never mutate it, use
     * {@link CustomData#update} or {@link CustomData#set} to write.
     */
    public static CompoundTag view(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? EMPTY : data.tag;
    }
}
