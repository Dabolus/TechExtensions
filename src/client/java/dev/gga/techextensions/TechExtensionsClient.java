package dev.gga.techextensions;

import dev.gga.techextensions.client.TEClientGuiType;
import dev.gga.techextensions.client.TETooltipHandler;
import dev.gga.techextensions.client.gui.GuiBubbleGun;
import dev.gga.techextensions.client.gui.GuiResonanceScanner;
import dev.gga.techextensions.client.gui.GuiVacuumGun;
import dev.gga.techextensions.client.particle.ShrinkRayParticle;
import dev.gga.techextensions.client.tint.ResonanceScannerScreenTintSource;
import dev.gga.techextensions.init.TEContent;
import dev.gga.techextensions.particle.TEParticleTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.resources.ResourceLocation;

public class TechExtensionsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TETooltipHandler.setup();
        MenuScreens.register(TEContent.BUBBLE_GUN_MENU, GuiBubbleGun::new);
        MenuScreens.register(TEContent.RESONANCE_SCANNER_MENU, GuiResonanceScanner::new);
        MenuScreens.register(TEContent.VACUUM_GUN_MENU, GuiVacuumGun::new);
        ItemTintSources.ID_MAPPER.put(
                ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "resonance_scanner_screen_color"),
                ResonanceScannerScreenTintSource.MAP_CODEC);
        BlockRenderLayerMap.putBlock(TEContent.ELECTRIC_DUCTED_FAN, ChunkSectionLayer.CUTOUT);
        registerMachineGuis();
        registerParticles();
    }

    @SuppressWarnings("unused")
    private void registerMachineGuis() {
        // Forces TEClientGuiType static initialization which registers the screens
        var _unused = TEClientGuiType.ELECTRIC_DUCTED_FAN;
    }

    private void registerParticles() {
        ParticleFactoryRegistry.getInstance().register(TEParticleTypes.SHRINK_RAY, ShrinkRayParticle.Provider::new);
    }
}
