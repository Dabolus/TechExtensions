package dev.gga.techextensions.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class TechExtensionsDataGen implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        pack.addProvider(TEItemTagProvider::new);
        pack.addProvider(TEBlockTagProvider::new);
        pack.addProvider(TEBlockLootTableProvider::new);
        pack.addProvider(TEModelProvider::new);
    }
}
