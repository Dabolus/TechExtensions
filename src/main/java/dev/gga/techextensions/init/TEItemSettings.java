package dev.gga.techextensions.init;

import net.fabricmc.fabric.api.item.v1.CustomDamageHandler;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.world.item.Item;

public class TEItemSettings {
    /** 1.20.1 has no unbreakable component: the damage is cancelled instead, which also hides the tooltip line. */
    private static final CustomDamageHandler NO_DAMAGE = (stack, amount, entity, breakCallback) -> 0;

    public static Item.Properties item(String name) {
        return new FabricItemSettings();
    }

    public static Item.Properties unbreakable(String name) {
        return new FabricItemSettings().customDamage(NO_DAMAGE);
    }
}
