package dev.gga.techextensions.component;

import com.mojang.serialization.codecs.PrimitiveCodec;
import dev.gga.techextensions.TechExtensions;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

public class TEDataComponentTypes {
    public static final DataComponentType<Integer> TOOL_MODE = DataComponentType.<Integer>builder()
            .persistent(PrimitiveCodec.INT)
            .networkSynchronized(ByteBufCodecs.INT)
            .build();

    public static void init() {
        Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "tool_mode"),
                TOOL_MODE);
    }
}
