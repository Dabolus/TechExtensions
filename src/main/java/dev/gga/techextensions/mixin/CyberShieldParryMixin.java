package dev.gga.techextensions.mixin;

import dev.gga.techextensions.items.tool.advanced.CyberShieldItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into `LivingEntity.applyItemBlocking` to trigger a perfect parry
 * when the player blocks with a `CyberShieldItem` within the parry window.
 */
@Mixin(LivingEntity.class)
public abstract class CyberShieldParryMixin {
    @Inject(method = "applyItemBlocking", at = @At("RETURN"))
    private void techextensions$onItemBlocking(
            ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        float blocked = cir.getReturnValue();
        if (blocked <= 0) return;

        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack blockingItem = self.getItemBlockingWith();
        if (blockingItem == null || !(blockingItem.getItem() instanceof CyberShieldItem shield)) return;

        CyberShieldItem.handleParry(level, self, blockingItem, shield, source);
    }
}
