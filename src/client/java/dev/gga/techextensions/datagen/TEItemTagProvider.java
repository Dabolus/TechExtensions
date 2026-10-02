package dev.gga.techextensions.datagen;

import dev.gga.techextensions.init.TEContent;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public class TEItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
    public TEItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        builder(ItemTags.CLUSTER_MAX_HARVESTABLES).add(key(TEContent.META_TOOL));

        builder(ItemTags.DURABILITY_ENCHANTABLE).add(key(TEContent.META_TOOL)).add(key(TEContent.ELECTRIC_JETPACK));

        builder(ItemTags.MINING_ENCHANTABLE).add(key(TEContent.META_TOOL));

        builder(ItemTags.MINING_LOOT_ENCHANTABLE).add(key(TEContent.META_TOOL));

        builder(ItemTags.SHARP_WEAPON_ENCHANTABLE).add(key(TEContent.META_TOOL));
    }

    /** Tag appenders only accept registry keys since 26.2; resolve them from the registered item. */
    private static ResourceKey<Item> key(ItemLike item) {
        return BuiltInRegistries.ITEM.getResourceKey(item.asItem()).orElseThrow();
    }
}
