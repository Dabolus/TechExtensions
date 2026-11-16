package dev.gga.techextensions.datagen;

import dev.gga.techextensions.init.TEContent;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public class TEItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public TEItemTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        getOrCreateTagBuilder(ItemTags.CLUSTER_MAX_HARVESTABLES).add(key(TEContent.META_TOOL));

        // No enchantable item tags before 1.20.5: the durability enchantments apply to any damageable item, and
        // MetaToolItem#canEnchant adds the mining and weapon ones to the Meta Tool
    }

    /** Tag appenders only accept registry keys since 26.2; resolve them from the registered item. */
    private static ResourceKey<Item> key(ItemLike item) {
        return BuiltInRegistries.ITEM.getResourceKey(item.asItem()).orElseThrow();
    }
}
