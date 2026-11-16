package dev.gga.techextensions.utils;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/**
 * Helpers for the item inventories, stored in the stack's NBT (`Items` list, like vanilla containers) as
 * Minecraft 1.20.1 has no container component.
 */
public final class TEContainerUtils {
    private static final String ITEMS_KEY = "Items";

    private TEContainerUtils() {}

    /** Copy of the given slot of the stack's inventory, or an empty stack. Reads only that slot. */
    public static ItemStack copySlot(ItemStack stack, int slot) {
        ListTag items = TENbtUtils.view(stack).getList(ITEMS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag item = items.getCompound(i);
            if ((item.getByte("Slot") & 255) == slot) {
                return ItemStack.of(item);
            }
        }
        return ItemStack.EMPTY;
    }

    /** Whether the stack has a saved inventory. */
    public static boolean hasItems(ItemStack stack) {
        return TENbtUtils.view(stack).contains(ITEMS_KEY, Tag.TAG_LIST);
    }

    /** Loads the stack's inventory into `items`, by slot index. */
    public static void load(ItemStack stack, NonNullList<ItemStack> items) {
        ContainerHelper.loadAllItems(TENbtUtils.view(stack), items);
    }

    /** Saves `items` as the stack's inventory, by slot index. */
    public static void save(ItemStack stack, NonNullList<ItemStack> items) {
        ContainerHelper.saveAllItems(stack.getOrCreateTag(), items);
    }
}
