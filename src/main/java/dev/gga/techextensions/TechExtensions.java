package dev.gga.techextensions;

import dev.gga.techextensions.component.TEDataComponentTypes;
import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.entity.BubbleTrapEntity;
import dev.gga.techextensions.init.TEContent;
import dev.gga.techextensions.items.tool.advanced.ShrinkRayItem;
import dev.gga.techextensions.particle.TEParticleTypes;
import dev.gga.techextensions.utils.ItemAnimationManager;
import dev.gga.techextensions.utils.TEMiningUtils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TechExtensions implements ModInitializer {
    public static final String MOD_ID = "techextensions";

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Config must be loaded first: items capture their energy capacity from it on construction
        TechExtensionsConfig.init();
        TEContent.register();
        TEDataComponentTypes.init();
        TEParticleTypes.init();
        // Server tickers: in-flight animations must keep running even when nobody holds the item anymore
        ItemAnimationManager.init();
        ShrinkRayItem.initTicking();
        TEMiningUtils.init();
        BubbleTrapEntity.init();
    }
}
