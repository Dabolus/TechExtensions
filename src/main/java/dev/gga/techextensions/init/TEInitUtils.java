package dev.gga.techextensions.init;

import dev.gga.techextensions.TechExtensions;
import dev.gga.techextensions.events.ModRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class TEInitUtils {
    // No translation key check here: before 1.21.2 the key comes from the registry, so it can't mismatch, and
    // reading it before registration would cache a wrong one.
    public static <I extends Item> I setup(I item, String name) {
        ModRegistry.registerIdent(item, ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, name));
        return item;
    }

    public static <B extends Block> B setup(B block, String name) {
        ModRegistry.registerIdent(block, ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, name));
        return block;
    }

    private TEInitUtils() {
        /* No instantiation. */
    }
}
