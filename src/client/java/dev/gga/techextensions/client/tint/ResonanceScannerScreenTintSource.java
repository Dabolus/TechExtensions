package dev.gga.techextensions.client.tint;

import com.mojang.serialization.MapCodec;
import dev.gga.techextensions.items.tool.advanced.ResonanceScannerItem;
import dev.gga.techextensions.utils.TENbtUtils;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import techreborn.utils.TRItemUtils;

/** Tints the scanner screen from red (far) to green (close) based on the last estimated distance. */
public record ResonanceScannerScreenTintSource() implements ItemTintSource {
    private static final int OFF_SCREEN_COLOR = 0xFF000000;

    public static final MapCodec<ResonanceScannerScreenTintSource> MAP_CODEC =
            MapCodec.unit(ResonanceScannerScreenTintSource::new);

    @Override
    public int calculate(ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity) {
        // Pure read: the server deactivates the scanner when it runs out of energy
        if (!TRItemUtils.isActive(itemStack)) {
            return OFF_SCREEN_COLOR;
        }
        CompoundTag tag = TENbtUtils.view(itemStack);
        if (!tag.contains(ResonanceScannerItem.DISTANCE_PERCENT_KEY)) {
            return OFF_SCREEN_COLOR;
        }
        double blockDistancePercent = tag.getDoubleOr(ResonanceScannerItem.DISTANCE_PERCENT_KEY, 1.0);
        // HSB Logic: 0.05 (Far/Red) -> 0.3 (Close/Green)
        float hue = 0.05f + (float) (0.25 * (1.0 - blockDistancePercent));
        return Mth.hsvToArgb(hue, 1.0f, 0.8f, 255);
    }

    @Override
    public @NotNull MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
