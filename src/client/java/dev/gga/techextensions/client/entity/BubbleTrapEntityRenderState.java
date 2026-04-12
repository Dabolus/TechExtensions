package dev.gga.techextensions.client.entity;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class BubbleTrapEntityRenderState extends EntityRenderState {
    /** Width of the trapped entity's bounding box, used to scale the bubble model. */
    public float trappedWidth = 1.0F;
    /** Height of the trapped entity's bounding box, used to scale the bubble model. */
    public float trappedHeight = 1.0F;
    /** Whether a trapped entity is present. */
    public boolean hasTrappedEntity = false;
}
