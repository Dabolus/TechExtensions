package dev.gga.techextensions.mixin;

import dev.gga.techextensions.items.tool.advanced.ShrinkRayItem;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Scales the gravity of entities resized by the Shrink Ray, like its gravity attribute modifier does in later
 * versions. 1.20.1 has no gravity attribute and Pehkui no gravity scale.
 */
@Mixin(LivingEntity.class)
public abstract class ShrinkRayGravityMixin {
    @ModifyConstant(method = "travel", constant = @Constant(doubleValue = 0.08D))
    private double techextensions$scaleGravity(double gravity) {
        double ratio = ShrinkRayItem.getSizeRatio((LivingEntity) (Object) this);
        return ratio == 1.0D
                ? gravity
                : gravity * Math.max(0.0D, 1.0D + (ratio - 1.0D) * ShrinkRayItem.GRAVITY_MULTIPLY_DELTA);
    }
}
