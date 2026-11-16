package dev.gga.techextensions.mixin;

import dev.gga.techextensions.items.tool.advanced.CyberShieldItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the `CyberShieldItem` block attacks like the `BlocksAttacks` component of later versions does: within the
 * mode's blocking angle and absorbing only part of the damage. When it blocks, triggers a perfect parry if the player
 * raised the shield within the parry window.
 */
@Mixin(LivingEntity.class)
public abstract class CyberShieldParryMixin {
    /** Damage the Cyber Shield let through on the hit being processed, added back after vanilla zeroes it. */
    @Unique
    private float techextensions$unblockedDamage;

    @Inject(method = "isDamageSourceBlocked", at = @At("HEAD"), cancellable = true)
    private void techextensions$isBlockedByCyberShield(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack blockingItem = self.getItemBlockingWith();
        if (blockingItem != null && blockingItem.getItem() instanceof CyberShieldItem) {
            cir.setReturnValue(CyberShieldItem.blocks(self, blockingItem, source));
        }
    }

    @Inject(
            method = "hurtServer",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/LivingEntity;hurtCurrentlyUsedShield(F)V",
                            shift = At.Shift.AFTER))
    private void techextensions$onItemBlocking(
            ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack blockingItem = self.getItemBlockingWith();
        if (blockingItem == null || !(blockingItem.getItem() instanceof CyberShieldItem shield)) return;

        // Vanilla has not zeroed the damage yet, so `amount` is the blocked hit
        techextensions$unblockedDamage = amount * (1.0F - shield.damageReductionFactor(blockingItem));
        CyberShieldItem.handleParry(level, self, blockingItem, shield, source);
    }

    /** Runs right after the blocking code, which sets the damage to 0 when an item blocks the hit. */
    @ModifyVariable(
            method = "hurtServer",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/tags/DamageTypeTags;IS_FREEZING:Lnet/minecraft/tags/TagKey;"),
            argsOnly = true)
    private float techextensions$addUnblockedDamage(float amount) {
        float unblocked = techextensions$unblockedDamage;
        techextensions$unblockedDamage = 0.0F;
        return amount + unblocked;
    }
}
