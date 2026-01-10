package dev.gga.techextensions.init;

import dev.gga.techextensions.TechExtensions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.LinkedHashSet;
import java.util.Set;

public class TEItemSettings {
    public static TooltipDisplay UNBREAKABLE_HIDE = new TooltipDisplay(
            false, new LinkedHashSet<>(Set.of(DataComponents.UNBREAKABLE))
    );

    public static Item.Properties item(String name) {
        return new Item.Properties().setId(key(name));
    }

    public static Item.Properties unbreakable(String name) {
        return item(name).component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                .component(DataComponents.TOOLTIP_DISPLAY, UNBREAKABLE_HIDE);
    }

    public static ResourceKey<Item> key(String name) {
        return ResourceKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, name));
    }
}
