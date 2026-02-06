package dev.gga.techextensions.datagen;

import dev.gga.techextensions.init.TEContent;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

public class TEBlockTagProvider extends FabricTagProvider.BlockTagProvider {
    public TEBlockTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        valueLookupBuilder(TEContent.BlockTags.META_TOOL_MINEABLE).addOptionalTag(BlockTags.MINEABLE_WITH_AXE);

        // Make Electric Ducted Fan mineable with pickaxe
        valueLookupBuilder(BlockTags.MINEABLE_WITH_PICKAXE).add((Block) TEContent.ELECTRIC_DUCTED_FAN);
    }
}
