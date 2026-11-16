package dev.gga.techextensions.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.gga.techextensions.items.tool.industrial.MetaToolItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Lets anvils and commands put mining and weapon enchantments on the Meta Tool, see `MetaToolItem#canEnchant`. */
@Mixin(Enchantment.class)
public abstract class MetaToolEnchantmentMixin {
    @WrapOperation(
            method = "canEnchant",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/enchantment/EnchantmentCategory;canEnchant(Lnet/minecraft/world/item/Item;)Z"))
    private boolean techextensions$canEnchantMetaTool(
            EnchantmentCategory category, Item item, Operation<Boolean> original) {
        return original.call(category, item) || MetaToolItem.canEnchant(item, category);
    }
}
