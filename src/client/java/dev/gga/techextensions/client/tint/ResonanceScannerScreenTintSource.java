package dev.gga.techextensions.client.tint;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.gga.techextensions.items.tool.advanced.ResonanceScannerItem;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import techreborn.utils.TRItemUtils;

public record ResonanceScannerScreenTintSource(int color) implements ItemTintSource {
    private static final int OFF_SCREEN_COLOR = 0xFF000000;

    public static final MapCodec<ResonanceScannerScreenTintSource> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(ExtraCodecs.RGB_COLOR_CODEC
                            .fieldOf("value")
                            .forGetter(ResonanceScannerScreenTintSource::color))
                    .apply(instance, ResonanceScannerScreenTintSource::new));

    public ResonanceScannerScreenTintSource() {
        this(-1); // Unused default value
    }

    @Override
    public int calculate(ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity) {
        if (!itemStack.has(DataComponents.CUSTOM_DATA)) {
            return OFF_SCREEN_COLOR;
        }
        long scanCost = ResonanceScannerItem.getScanCost(itemStack);
        TRItemUtils.checkActive(itemStack, (int) scanCost, livingEntity);
        if (!TRItemUtils.isActive(itemStack)) {
            return OFF_SCREEN_COLOR;
        }
        CustomData customData = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (!tag.contains("estimated_block_distance_percent")) {
            return OFF_SCREEN_COLOR;
        }
        double blockDistancePercent =
                tag.getDouble("estimated_block_distance_percent").orElse(1.0);
        // HSB Logic: 0.05 (Far/Red) -> 0.3 (Close/Green)
        float hue = 0.05f + (float) (0.25 * (1.0 - blockDistancePercent));
        return java.awt.Color.HSBtoRGB(hue, 1.0f, 0.8f);
    }

    @Override
    public @NotNull MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
