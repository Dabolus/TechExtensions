package dev.gga.techextensions.utils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Helpers to read single slots of an item's container component without materialising every slot. */
public final class TEContainerUtils {
    private TEContainerUtils() {}

    /** Copy of the given slot of the stack's container component, or an empty stack. */
    public static ItemStack copySlot(ItemStack stack, int slot) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        return contents == null ? ItemStack.EMPTY : copySlot(contents, slot);
    }

    /** Copy of the given slot, or an empty stack. */
    public static ItemStack copySlot(ItemContainerContents contents, int slot) {
        if (slot == 0) {
            return contents.copyOne();
        }
        return contents.itemCopies().skip(slot).findFirst().orElse(ItemStack.EMPTY);
    }
}
