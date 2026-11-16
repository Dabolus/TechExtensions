package dev.gga.techextensions.client.property;

import dev.gga.techextensions.component.TEDataComponentTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Item model predicate exposing the stack's tool mode ordinal (0 when unset), for model {@code overrides}. Later
 * versions use the vanilla {@code minecraft:component} select property instead, which 1.21.1 does not have.
 */
public record ToolModeProperty() implements ClampedItemPropertyFunction {
    @Override
    public float unclampedCall(
            ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity, int seed) {
        return itemStack.getOrDefault(TEDataComponentTypes.TOOL_MODE, 0);
    }

    /** Not clamped to [0, 1]: items can have more than two modes. */
    @Override
    public float call(
            ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity, int seed) {
        return unclampedCall(itemStack, clientLevel, livingEntity, seed);
    }
}
