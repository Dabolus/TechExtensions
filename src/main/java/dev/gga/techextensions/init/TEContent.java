package dev.gga.techextensions.init;

import dev.gga.techextensions.TechExtensions;
import dev.gga.techextensions.events.ModRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class TEContent {
    public static final Marker DATAGEN = MarkerFactory.getMarker("datagen");

    public static Item JETPACK;
    public static Item META_TOOL;

    public final static class BlockTags {
        public static final TagKey<Block> META_TOOL_MINEABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "mineable/meta_tool"));

        private BlockTags() {
        }
    }


    public static void register() {
        ModRegistry.register();
        TEItemGroup.register();
    }
}
