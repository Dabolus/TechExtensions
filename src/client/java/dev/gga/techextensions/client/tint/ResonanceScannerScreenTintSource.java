package dev.gga.techextensions.client.tint;

import dev.gga.techextensions.items.tool.advanced.ResonanceScannerItem;
import dev.gga.techextensions.utils.TENbtUtils;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import reborncore.common.util.ItemUtils;

/** Tints the scanner screen from red (far) to green (close) based on the last estimated distance. */
public record ResonanceScannerScreenTintSource() implements ItemColor {
    private static final int OFF_SCREEN_COLOR = 0xFF000000;
    /** Tint index of the screen layer (layer1) in the item model; layer0 is left untinted. */
    private static final int SCREEN_TINT_INDEX = 1;

    @Override
    public int getColor(ItemStack itemStack, int tintIndex) {
        if (tintIndex != SCREEN_TINT_INDEX) {
            return -1;
        }
        // Pure read: the server deactivates the scanner when it runs out of energy
        if (!ItemUtils.isActive(itemStack)) {
            return OFF_SCREEN_COLOR;
        }
        CompoundTag tag = TENbtUtils.view(itemStack);
        if (!tag.contains(ResonanceScannerItem.DISTANCE_PERCENT_KEY)) {
            return OFF_SCREEN_COLOR;
        }
        double blockDistancePercent = tag.getDouble(ResonanceScannerItem.DISTANCE_PERCENT_KEY);
        // HSB Logic: 0.05 (Far/Red) -> 0.3 (Close/Green)
        float hue = 0.05f + (float) (0.25 * (1.0 - blockDistancePercent));
        return 0xFF000000 | Mth.hsvToRgb(hue, 1.0f, 0.8f);
    }
}
