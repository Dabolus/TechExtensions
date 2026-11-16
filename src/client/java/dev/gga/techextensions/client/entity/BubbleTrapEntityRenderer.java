package dev.gga.techextensions.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.gga.techextensions.TechExtensions;
import dev.gga.techextensions.entity.BubbleTrapEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Renders a semi-transparent bubble cube around the trapped entity.
 */
public class BubbleTrapEntityRenderer extends EntityRenderer<BubbleTrapEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "textures/entity/bubble_trap.png");

    private final BubbleTrapModel model;

    public BubbleTrapEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new BubbleTrapModel(ctx.bakeLayer(BubbleTrapModel.LAYER_LOCATION), RenderType::entityTranslucent);
    }

    @Override
    public ResourceLocation getTextureLocation(BubbleTrapEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(
            BubbleTrapEntity entity,
            float yaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        Entity trapped = entity.getTrappedEntity();
        if (trapped == null) {
            return;
        }

        poseStack.pushPose();

        // Shift down by half the padding so the bubble is centered around the entity
        poseStack.translate(0.0, -0.2, 0.0);

        // Scale the 16x16x16 cube model to match the trapped entity's dimensions, with padding so the bubble is
        // slightly larger than the entity
        float scaleX = trapped.getBbWidth() + 0.4F;
        float scaleY = trapped.getBbHeight() + 0.4F;
        float scaleZ = trapped.getBbWidth() + 0.4F;
        poseStack.scale(scaleX, scaleY, scaleZ);

        RenderType renderType = this.model.renderType(TEXTURE);
        // Render with the bubble texture. Translucency comes from the texture's own alpha channel.
        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        this.model.renderToBuffer(poseStack, buffers.getBuffer(renderType), packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();

        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }
}
