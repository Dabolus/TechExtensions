package dev.gga.techextensions.component;

import dev.gga.techextensions.utils.TENbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Minecraft 1.20.1 has no data components: the tool mode is kept in the stack's NBT instead. */
public class TEDataComponentTypes {
    public static final String TOOL_MODE = "tool_mode";

    /** The stack's tool mode ordinal, or `null` when none was set yet. */
    @Nullable
    public static Integer getToolMode(ItemStack stack) {
        return TENbtUtils.view(stack).contains(TOOL_MODE, Tag.TAG_INT)
                ? TENbtUtils.view(stack).getInt(TOOL_MODE)
                : null;
    }

    public static int getToolMode(ItemStack stack, int defaultValue) {
        Integer mode = getToolMode(stack);
        return mode == null ? defaultValue : mode;
    }

    public static void setToolMode(ItemStack stack, int mode) {
        stack.getOrCreateTag().putInt(TOOL_MODE, mode);
    }
}
