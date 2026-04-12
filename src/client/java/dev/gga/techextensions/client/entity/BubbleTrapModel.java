package dev.gga.techextensions.client.entity;

import dev.gga.techextensions.TechExtensions;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * A simple cube model for the bubble trap. The cube is 16x16x16 centered,
 * and scaled at render time to match the trapped entity's bounding box.
 */
public class BubbleTrapModel extends EntityModel<BubbleTrapEntityRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(TechExtensions.MOD_ID, "bubble_trap"), "main");

    public BubbleTrapModel(ModelPart root, Function<Identifier, RenderType> renderTypeFactory) {
        super(root, renderTypeFactory);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                "bubble",
                CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 16.0F, 16.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(BubbleTrapEntityRenderState state) {
        // Make the bubble bob up and down slightly while floating
        float bobOffset = (float) Math.sin(state.ageInTicks * 0.1F) * 0.05F;
        this.root().getChild("bubble").setPos(0.0F, bobOffset, 0.0F);
    }
}
