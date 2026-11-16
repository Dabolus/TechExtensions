package dev.gga.techextensions.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.gga.techextensions.items.TEEnergyItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import reborncore.common.powerSystem.RcEnergyItem;
import reborncore.common.util.ItemUtils;

/** Uses the stack's energy capacity for the durability bar of a {@link TEEnergyItem}, like later RebornCore does. */
@Mixin(value = ItemUtils.class, remap = false)
public abstract class RcEnergyCapacityMixin {
    @WrapOperation(
            method = "getPowerForDurabilityBar",
            at = @At(value = "INVOKE", target = "Lreborncore/common/powerSystem/RcEnergyItem;getEnergyCapacity()J"))
    private static long techextensions$getStackEnergyCapacity(
            RcEnergyItem item, Operation<Long> original, @Local(argsOnly = true) ItemStack stack) {
        return item instanceof TEEnergyItem teItem ? teItem.getEnergyCapacity(stack) : original.call(item);
    }
}
