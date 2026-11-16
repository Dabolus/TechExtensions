package dev.gga.techextensions.utils;

import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Access to the stack's NBT tag, which holds every custom value before 1.20.5. */
public final class TENbtUtils {
    private static final CompoundTag EMPTY = new CompoundTag();

    private TENbtUtils() {}

    /**
     * Returns the stack's tag for reading, without creating one. This is the live tag (or a shared empty one):
     * never mutate it, use {@link #update} to write.
     */
    public static CompoundTag view(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? EMPTY : tag;
    }

    /** Mutates the stack's tag, creating it when missing. */
    public static void update(ItemStack stack, Consumer<CompoundTag> updater) {
        updater.accept(stack.getOrCreateTag());
    }
}
