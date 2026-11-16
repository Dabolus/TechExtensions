package dev.gga.techextensions.init;

import dev.gga.techextensions.TechExtensions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Unbreakable;

public class TEItemSettings {
    public static final Unbreakable UNBREAKABLE_HIDE = new Unbreakable(false);

    public static Item.Properties item(String name) {
        return new Item.Properties().setId(key(name));
    }

    public static Item.Properties unbreakable(String name) {
        return item(name).component(DataComponents.UNBREAKABLE, UNBREAKABLE_HIDE);
    }

    public static ResourceKey<Item> key(String name) {
        return ResourceKey.create(
                BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, name));
    }
}
