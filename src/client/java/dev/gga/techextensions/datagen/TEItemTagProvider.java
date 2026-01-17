package dev.gga.techextensions.datagen;

import dev.gga.techextensions.init.TEContent;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;

public class TEItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public TEItemTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        valueLookupBuilder(ItemTags.CLUSTER_MAX_HARVESTABLES).add(TEContent.META_TOOL);

        valueLookupBuilder(ItemTags.DURABILITY_ENCHANTABLE)
                .add(TEContent.META_TOOL)
                .add(TEContent.ELECTRIC_JETPACK);

        valueLookupBuilder(ItemTags.MINING_ENCHANTABLE).add(TEContent.META_TOOL);

        valueLookupBuilder(ItemTags.MINING_LOOT_ENCHANTABLE).add(TEContent.META_TOOL);

        valueLookupBuilder(ItemTags.SWORD_ENCHANTABLE).add(TEContent.META_TOOL);
    }
}
