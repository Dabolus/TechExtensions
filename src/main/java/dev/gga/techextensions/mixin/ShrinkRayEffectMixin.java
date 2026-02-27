package dev.gga.techextensions.mixin;

import dev.gga.techextensions.items.tool.advanced.ShrinkRayItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies saturation or hunger effects to living entities that have been
 * resized by the Shrink Ray. The effect persists as long as the entity's
 * scale differs from its base scale, with a level proportional to the
 * magnitude of the size change.
 */
@Mixin(LivingEntity.class)
public abstract class ShrinkRayEffectMixin {

    private static final int CHECK_INTERVAL_TICKS = 20;
    // Duration slightly longer than the check interval to avoid flickering
    private static final int EFFECT_DURATION_TICKS = 45;

    @Inject(method = "tick", at = @At("TAIL"))
    private void techextensions$applyShrinkRayEffects(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Only process on the server side, once per second
        if (self.level().isClientSide() || self.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        AttributeInstance scaleAttr = self.getAttribute(Attributes.SCALE);
        if (scaleAttr == null) {
            return;
        }

        // Quick check: does this entity have the shrink ray's scale modifier?
        AttributeModifier modifier = scaleAttr.getModifier(ShrinkRayItem.SHRINK_RAY_SCALE_MODIFIER_ID);
        if (modifier == null) {
            return;
        }

        double baseScale = scaleAttr.getBaseValue();
        double currentScale = scaleAttr.getValue();

        if (Math.abs(currentScale - baseScale) < 0.001D) {
            // Entity is at base scale (restored), remove both effects
            self.removeEffect(MobEffects.SATURATION);
            self.removeEffect(MobEffects.HUNGER);
            return;
        }

        // Effect level proportional to size change using log2 of the ratio.
        // level 0 (I) at ~2x change, level 1 (II) at ~4x, level 2 (III) at ~8x, etc.
        double ratio = currentScale / baseScale;
        int level = Math.max(0, (int) Math.floor(Math.abs(Math.log(ratio) / Math.log(2.0D))));

        if (currentScale < baseScale) {
            // Shrunk — apply saturation, clear hunger
            applyOrRefreshEffect(self, true, level);
        } else {
            // Enlarged — apply hunger, clear saturation
            applyOrRefreshEffect(self, false, level);
        }
    }

    /**
     * Applies or refreshes the appropriate effect, removing the opposite one.
     *
     * @param entity the target entity
     * @param shrunk true for saturation (shrunk), false for hunger (enlarged)
     * @param level the effect amplifier (0-based)
     */
    private static void applyOrRefreshEffect(LivingEntity entity, boolean shrunk, int level) {
        var desiredEffect = shrunk ? MobEffects.SATURATION : MobEffects.HUNGER;
        var oppositeEffect = shrunk ? MobEffects.HUNGER : MobEffects.SATURATION;

        entity.removeEffect(oppositeEffect);

        // Check if the current effect already matches the desired level to avoid
        // unnecessary removal/reapplication which could cause icon flickering
        MobEffectInstance existing = entity.getEffect(desiredEffect);
        if (existing != null && existing.getAmplifier() == level && existing.getDuration() > CHECK_INTERVAL_TICKS) {
            // Effect is already correct and has enough remaining duration
            return;
        }

        // Remove and reapply if amplifier changed, or apply fresh
        if (existing != null && existing.getAmplifier() != level) {
            entity.removeEffect(desiredEffect);
        }
        entity.addEffect(new MobEffectInstance(desiredEffect, EFFECT_DURATION_TICKS, level, false, false, true));
    }
}
