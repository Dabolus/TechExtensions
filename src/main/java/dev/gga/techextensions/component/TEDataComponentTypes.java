package dev.gga.techextensions.component;

import com.mojang.serialization.codecs.PrimitiveCodec;
import dev.gga.techextensions.TechExtensions;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

public class TEDataComponentTypes {
    public static final DataComponentType<Integer> META_TOOL_MODE = DataComponentType.<Integer>builder()
            .persistent(PrimitiveCodec.INT)
            .networkSynchronized(ByteBufCodecs.INT)
            .build();

    public static final DataComponentType<Integer> SHRINK_RAY_MODE = DataComponentType.<Integer>builder()
            .persistent(PrimitiveCodec.INT)
            .networkSynchronized(ByteBufCodecs.INT)
            .build();

    public static void init() {
        Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "meta_tool_mode"),
                META_TOOL_MODE);
        Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "shrink_ray_mode"),
                SHRINK_RAY_MODE);
    }
}
