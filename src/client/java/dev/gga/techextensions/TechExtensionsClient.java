package dev.gga.techextensions;

import dev.gga.techextensions.client.TETooltipHandler;
import net.fabricmc.api.ClientModInitializer;

public class TechExtensionsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TETooltipHandler.setup();
	}
}
