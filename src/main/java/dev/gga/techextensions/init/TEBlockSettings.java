package dev.gga.techextensions.init;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class TEBlockSettings {
    private static BlockBehaviour.Properties metal(String name) {
        return BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .mapColor(MapColor.METAL)
                .strength(2f, 2f);
    }

    public static BlockBehaviour.Properties fan(String name) {
        return metal(name).strength(1f, 1f);
    }

    private TEBlockSettings() {}
}
