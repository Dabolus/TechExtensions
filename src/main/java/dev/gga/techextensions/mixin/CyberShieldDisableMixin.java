package dev.gga.techextensions.mixin;

import dev.gga.techextensions.items.tool.advanced.CyberShieldItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Puts the Cyber Shield on cooldown when an axe disables it, like later versions do for any blocking item. In 1.21.1
 * `Player.disableShield` always targets the vanilla shield.
 */
@Mixin(Player.class)
public abstract class CyberShieldDisableMixin {
    @ModifyArg(
            method = "disableShield",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/world/item/Item;I)V"))
    private Item techextensions$cooldownCyberShield(Item item) {
        Item used = ((Player) (Object) this).getUseItem().getItem();
        return used instanceof CyberShieldItem ? used : item;
    }
}
