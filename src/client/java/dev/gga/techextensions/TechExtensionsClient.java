package dev.gga.techextensions;

import dev.gga.techextensions.client.TETooltipHandler;
import dev.gga.techextensions.client.gui.GuiResonanceScanner;
import dev.gga.techextensions.init.TEContent;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class TechExtensionsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TETooltipHandler.setup();
		MenuScreens.register(TEContent.RESONANCE_SCANNER_MENU, GuiResonanceScanner::new);
	}
}
