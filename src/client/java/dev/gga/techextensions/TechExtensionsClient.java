package dev.gga.techextensions;

import dev.gga.techextensions.client.TETooltipHandler;
import dev.gga.techextensions.client.gui.GuiResonanceScanner;
import dev.gga.techextensions.client.tint.ResonanceScannerScreenTintSource;
import dev.gga.techextensions.init.TEContent;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;

public class TechExtensionsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TETooltipHandler.setup();
		MenuScreens.register(TEContent.RESONANCE_SCANNER_MENU, GuiResonanceScanner::new);
		ItemTintSources.ID_MAPPER.put(ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "resonance_scanner_screen_color"), ResonanceScannerScreenTintSource.MAP_CODEC);
	}
}
