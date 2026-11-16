package dev.gga.techextensions.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.gga.techextensions.items.tool.advanced.CyberShieldItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Since 1.21.4, vanilla applies an extra first-person transform to every blocking item that isn't a
 * {@code ShieldItem}, and the Cyber Shield blocking models are authored around it. 1.20.1 lacks that
 * transform, so it is reproduced here for the Cyber Shield only.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class CyberShieldFirstPersonMixin {
    @WrapOperation(
            method = "renderArmWithItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V"))
    private void techextensions$applyCyberShieldBlockTransform(
            ItemInHandRenderer renderer,
            PoseStack poseStack,
            HumanoidArm arm,
            float equipProgress,
            Operation<Void> original,
            @Local(argsOnly = true) AbstractClientPlayer player,
            @Local(argsOnly = true) InteractionHand hand,
            @Local(argsOnly = true) ItemStack stack) {
        original.call(renderer, poseStack, arm, equipProgress);
        // Same condition vanilla uses to pick the use animation branch (BLOCK for the Cyber Shield)
        if (stack.getItem() instanceof CyberShieldItem
                && player.isUsingItem()
                && player.getUseItemRemainingTicks() > 0
                && player.getUsedItemHand() == hand) {
            float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            poseStack.translate(side * -0.14142136F, 0.08F, 0.14142136F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-102.25F));
            poseStack.mulPose(Axis.YP.rotationDegrees(side * 13.365F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * 78.05F));
        }
    }
}
