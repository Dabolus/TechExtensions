package dev.gga.techextensions.init;

import dev.gga.techextensions.TechExtensions;
import dev.gga.techextensions.events.ModRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class TEInitUtils {
    public static <I extends Item> I setup(I item, String name) {
        ModRegistry.registerIdent(item, ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, name));

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            String expect = Util.makeDescriptionId("item", ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, name));
            String actual = item.getDescriptionId();

            if (!expect.equals(actual)) {
                // This happens when the item settings registry key does not match key used to register the item
                throw new IllegalStateException("Item translation key mismatch: expected " + expect + ", got " + actual);
            }
        }

        return item;
    }

    public static <B extends Block> B setup(B block, String name) {
        ModRegistry.registerIdent(block, ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, name));

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            String expect = Util.makeDescriptionId("block", ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, name));
            String actual = block.getDescriptionId();

            if (!expect.equals(actual)) {
                // This happens when the block settings registry key does not match key used to register the block
                throw new IllegalStateException("Block translation key mismatch: expected " + expect + ", got " + actual);
            }
        }

        return block;
    }

    public static SoundEvent setup(String name) {
        ResourceLocation identifier = ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, identifier, SoundEvent.createVariableRangeEvent(identifier));
    }

    public static boolean isDatagenRunning() {
        return System.getProperty("fabric-api.datagen") != null;
    }

    private TEInitUtils() {/* No instantiation. */}
}
