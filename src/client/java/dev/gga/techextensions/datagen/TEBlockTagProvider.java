package dev.gga.techextensions.datagen;

import dev.gga.techextensions.init.TEContent;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

public class TEBlockTagProvider extends FabricTagsProvider.BlockTagsProvider {
    public TEBlockTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        builder(TEContent.BlockTags.META_TOOL_MINEABLE)
                .addOptionalTag(BlockTags.MINEABLE_WITH_PICKAXE)
                .addOptionalTag(BlockTags.MINEABLE_WITH_AXE)
                .addOptionalTag(BlockTags.MINEABLE_WITH_SHOVEL)
                .addOptionalTag(BlockTags.SWORD_EFFICIENT)
                .addOptionalTag(BlockTags.LEAVES);

        // Make Electric Ducted Fan mineable with pickaxe
        builder(BlockTags.MINEABLE_WITH_PICKAXE).add(key(TEContent.ELECTRIC_DUCTED_FAN));
    }

    /** Tag appenders only accept registry keys since 26.2; resolve them from the registered block. */
    private static ResourceKey<Block> key(Block block) {
        return BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow();
    }
}
