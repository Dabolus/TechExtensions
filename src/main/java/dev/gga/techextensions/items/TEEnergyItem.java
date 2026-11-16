package dev.gga.techextensions.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import reborncore.common.powerSystem.RcEnergyItem;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyItem;

/**
 * An {@link RcEnergyItem} with a per-stack capacity, like on later RebornCore versions: upgrades can raise it. On
 * 1.20.1 RebornCore only has a per-item capacity, so the energy storage is registered here and the tooltip and
 * durability bar use the stack's capacity through `RcEnergyCapacityMixin` and `RcEnergyTooltipMixin`.
 */
public interface TEEnergyItem extends RcEnergyItem {
    /** The capacity of the stack, or the base capacity when `stack` is `null`. */
    long getEnergyCapacity(@Nullable ItemStack stack);

    default long getEnergyMaxInput(@Nullable ItemStack stack) {
        return getTier().getMaxInput();
    }

    default long getEnergyMaxOutput(@Nullable ItemStack stack) {
        return getTier().getMaxOutput();
    }

    @Override
    default long getEnergyCapacity() {
        return getEnergyCapacity(null);
    }

    @Override
    default long getEnergyMaxInput() {
        return getEnergyMaxInput(null);
    }

    @Override
    default long getEnergyMaxOutput() {
        return getEnergyMaxOutput(null);
    }

    /** Exposes the items' energy with their per-stack capacity, instead of the energy API's per-item fallback. */
    static void registerEnergyStorage(Item... items) {
        EnergyStorage.ITEM.registerForItems(
                (stack, context) -> {
                    TEEnergyItem item = (TEEnergyItem) stack.getItem();
                    return SimpleEnergyItem.createStorage(
                            context,
                            item.getEnergyCapacity(stack),
                            item.getEnergyMaxInput(stack),
                            item.getEnergyMaxOutput(stack));
                },
                items);
    }
}
