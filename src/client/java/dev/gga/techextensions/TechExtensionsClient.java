package dev.gga.techextensions;

import dev.gga.techextensions.client.PlayerInputSender;
import dev.gga.techextensions.client.TEClientGuiType;
import dev.gga.techextensions.client.TETooltipHandler;
import dev.gga.techextensions.client.entity.BubbleTrapEntityRenderer;
import dev.gga.techextensions.client.entity.BubbleTrapModel;
import dev.gga.techextensions.client.gui.GuiBubbleGun;
import dev.gga.techextensions.client.gui.GuiResonanceScanner;
import dev.gga.techextensions.client.gui.GuiVacuumGun;
import dev.gga.techextensions.client.particle.ShrinkRayParticle;
import dev.gga.techextensions.client.property.ToolModeProperty;
import dev.gga.techextensions.client.tint.ResonanceScannerScreenTintSource;
import dev.gga.techextensions.init.TEContent;
import dev.gga.techextensions.particle.TEParticleTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

public class TechExtensionsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TETooltipHandler.setup();
        MenuScreens.register(TEContent.BUBBLE_GUN_MENU, GuiBubbleGun::new);
        MenuScreens.register(TEContent.RESONANCE_SCANNER_MENU, GuiResonanceScanner::new);
        MenuScreens.register(TEContent.VACUUM_GUN_MENU, GuiVacuumGun::new);
        ColorProviderRegistry.ITEM.register(new ResonanceScannerScreenTintSource(), TEContent.RESONANCE_SCANNER);
        registerItemProperties();
        PlayerInputSender.init();
        BlockRenderLayerMap.INSTANCE.putBlock(TEContent.ELECTRIC_DUCTED_FAN, RenderType.cutout());
        registerMachineGuis();
        registerParticles();
        registerEntityRenderers();
    }

    /** Predicates used by the item model overrides (cyber_shield.json). */
    private void registerItemProperties() {
        ItemProperties.register(
                TEContent.CYBER_SHIELD,
                new ResourceLocation(TechExtensions.MOD_ID, "blocking"),
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        ItemProperties.register(
                TEContent.CYBER_SHIELD,
                new ResourceLocation(TechExtensions.MOD_ID, "tool_mode"),
                new ToolModeProperty());
    }

    @SuppressWarnings("unused")
    private void registerMachineGuis() {
        // Forces TEClientGuiType static initialization which registers the screens
        var _unused = TEClientGuiType.ELECTRIC_DUCTED_FAN;
    }

    private void registerParticles() {
        ParticleFactoryRegistry.getInstance().register(TEParticleTypes.SHRINK_RAY, ShrinkRayParticle.Provider::new);
    }

    private void registerEntityRenderers() {
        EntityModelLayerRegistry.registerModelLayer(BubbleTrapModel.LAYER_LOCATION, BubbleTrapModel::createBodyLayer);
        EntityRendererRegistry.register(TEContent.BUBBLE_TRAP_ENTITY, BubbleTrapEntityRenderer::new);
    }
}
