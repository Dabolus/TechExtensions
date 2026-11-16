package dev.gga.techextensions.client.entity;

import dev.gga.techextensions.TechExtensions;
import dev.gga.techextensions.entity.BubbleTrapEntity;
import java.util.function.Function;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * A simple cube model for the bubble trap. The cube is 16x16x16 centered,
 * and scaled at render time to match the trapped entity's bounding box.
 */
public class BubbleTrapModel extends HierarchicalModel<BubbleTrapEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(TechExtensions.MOD_ID, "bubble_trap"), "main");

    private final ModelPart root;

    public BubbleTrapModel(ModelPart root, Function<ResourceLocation, RenderType> renderTypeFactory) {
        super(renderTypeFactory);
        this.root = root;
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
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(
            BubbleTrapEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        // Make the bubble bob up and down slightly while floating
        float bobOffset = (float) Math.sin(ageInTicks * 0.1F) * 0.05F;
        this.root().getChild("bubble").setPos(0.0F, bobOffset, 0.0F);
    }
}
