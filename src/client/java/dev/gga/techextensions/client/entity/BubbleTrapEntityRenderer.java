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
public class BubbleTrapEntityRenderer extends EntityRenderer<BubbleTrapEntity, BubbleTrapEntityRenderState> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "textures/entity/bubble_trap.png");

    private final BubbleTrapModel model;

    public BubbleTrapEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new BubbleTrapModel(ctx.bakeLayer(BubbleTrapModel.LAYER_LOCATION), RenderType::entityTranslucent);
    }

    @Override
    public BubbleTrapEntityRenderState createRenderState() {
        return new BubbleTrapEntityRenderState();
    }

    @Override
    public void extractRenderState(BubbleTrapEntity entity, BubbleTrapEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        Entity trapped = entity.getTrappedEntity();
        if (trapped != null) {
            state.hasTrappedEntity = true;
            // Add padding so bubble is slightly larger than the entity
            state.trappedWidth = trapped.getBbWidth() + 0.4F;
            state.trappedHeight = trapped.getBbHeight() + 0.4F;
        } else {
            state.hasTrappedEntity = false;
            state.trappedWidth = 1.0F;
            state.trappedHeight = 1.0F;
        }
    }

    @Override
    public void render(
            BubbleTrapEntityRenderState state, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        if (!state.hasTrappedEntity) {
            return;
        }

        poseStack.pushPose();

        // Shift down by half the padding so the bubble is centered around the entity
        poseStack.translate(0.0, -0.2, 0.0);

        // Scale the 16x16x16 cube model to match the trapped entity's dimensions
        float scaleX = state.trappedWidth;
        float scaleY = state.trappedHeight;
        float scaleZ = state.trappedWidth;
        poseStack.scale(scaleX, scaleY, scaleZ);

        RenderType renderType = this.model.renderType(TEXTURE);
        // Render with the bubble texture. Translucency comes from the texture's own alpha channel.
        this.model.setupAnim(state);
        this.model.renderToBuffer(poseStack, buffers.getBuffer(renderType), packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();

        super.render(state, poseStack, buffers, packedLight);
    }
}
