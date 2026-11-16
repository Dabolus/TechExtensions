package dev.gga.techextensions.client.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.gga.techextensions.component.TEDataComponentTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Selects an item model by the stack's tool mode. Later versions use the vanilla {@code minecraft:component} select
 * property instead, which 1.21.4 does not have.
 */
public record ToolModeProperty() implements SelectItemModelProperty<Integer> {
    public static final SelectItemModelProperty.Type<ToolModeProperty, Integer> TYPE =
            SelectItemModelProperty.Type.create(MapCodec.unit(new ToolModeProperty()), Codec.INT);

    @Override
    public @Nullable Integer get(
            ItemStack itemStack,
            @Nullable ClientLevel clientLevel,
            @Nullable LivingEntity livingEntity,
            int seed,
            ItemDisplayContext displayContext) {
        return itemStack.get(TEDataComponentTypes.TOOL_MODE);
    }

    @Override
    public SelectItemModelProperty.Type<ToolModeProperty, Integer> type() {
        return TYPE;
    }
}
