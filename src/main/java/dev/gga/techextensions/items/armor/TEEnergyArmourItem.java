package dev.gga.techextensions.items.armor;

import dev.gga.techextensions.init.TEItemSettings;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import reborncore.common.powerSystem.RcEnergyItem;
import reborncore.common.powerSystem.RcEnergyTier;
import reborncore.common.util.ItemUtils;

public abstract class TEEnergyArmourItem extends ArmorItem implements RcEnergyItem {
    public final long maxCharge;
    private final RcEnergyTier energyTier;

    public TEEnergyArmourItem(
            Holder<ArmorMaterial> material,
            ArmorItem.Type slot,
            int durability,
            long maxCharge,
            RcEnergyTier energyTier,
            String name) {
        super(material, slot, TEItemSettings.unbreakable(name).durability(slot.getDurability(durability)));
        this.maxCharge = maxCharge;
        this.energyTier = energyTier;
    }

    // Item
    @Override
    public int getBarWidth(ItemStack stack) {
        return ItemUtils.getPowerForDurabilityBar(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return ItemUtils.getColorForDurabilityBar(stack);
    }

    // RcEnergyItem
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return maxCharge;
    }

    @Override
    public RcEnergyTier getTier() {
        return energyTier;
    }
}
