package dev.gga.techextensions.init;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Unbreakable;

public class TEItemSettings {
    public static final Unbreakable UNBREAKABLE_HIDE = new Unbreakable(false);

    public static Item.Properties item(String name) {
        return new Item.Properties();
    }

    public static Item.Properties unbreakable(String name) {
        return item(name).component(DataComponents.UNBREAKABLE, UNBREAKABLE_HIDE);
    }
}
