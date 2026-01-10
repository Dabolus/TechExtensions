package dev.gga.techextensions.datagen;

import dev.gga.techextensions.init.TEContent;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class TEItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public TEItemTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        valueLookupBuilder(ItemTags.CLUSTER_MAX_HARVESTABLES)
            .add(TEContent.META_TOOL);

        valueLookupBuilder(ItemTags.DURABILITY_ENCHANTABLE)
            .add(TEContent.META_TOOL);

        valueLookupBuilder(ItemTags.MINING_ENCHANTABLE)
            .add(TEContent.META_TOOL);

        valueLookupBuilder(ItemTags.MINING_LOOT_ENCHANTABLE)
            .add(TEContent.META_TOOL);

        valueLookupBuilder(ItemTags.SWORD_ENCHANTABLE)
            .add(TEContent.META_TOOL);
    }
}
