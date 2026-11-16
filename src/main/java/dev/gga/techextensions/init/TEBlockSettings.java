package dev.gga.techextensions.init;

import dev.gga.techextensions.TechExtensions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class TEBlockSettings {
    private static BlockBehaviour.Properties metal(String name) {
        return BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .mapColor(MapColor.METAL)
                .strength(2f, 2f)
                .setId(key(name));
    }

    public static BlockBehaviour.Properties fan(String name) {
        return metal(name).strength(1f, 1f);
    }

    private static ResourceKey<Block> key(String name) {
        return ResourceKey.create(
                BuiltInRegistries.BLOCK.key(), ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, name));
    }

    private TEBlockSettings() {}
}
