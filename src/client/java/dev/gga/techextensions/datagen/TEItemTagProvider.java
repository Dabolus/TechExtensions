package dev.gga.techextensions.datagen;

import dev.gga.techextensions.init.TEContent;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;

public class TEItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
    public TEItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
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

        valueLookupBuilder(ItemTags.SHARP_WEAPON_ENCHANTABLE).add(TEContent.META_TOOL);
    }
}
