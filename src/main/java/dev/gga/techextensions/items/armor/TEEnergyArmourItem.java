package dev.gga.techextensions.items.armor;

import dev.gga.techextensions.init.TEArmorMaterials;
import dev.gga.techextensions.init.TEItemSettings;
import dev.gga.techextensions.items.TEEnergyItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import reborncore.common.powerSystem.RcEnergyTier;
import reborncore.common.util.ItemUtils;

public abstract class TEEnergyArmourItem extends ArmorItem implements TEEnergyItem {
    public final long maxCharge;
    private final RcEnergyTier energyTier;

    public TEEnergyArmourItem(
            ArmorMaterial material,
            ArmorItem.Type slot,
            int durability,
            long maxCharge,
            RcEnergyTier energyTier,
            String name) {
        super(
                material,
                slot,
                TEItemSettings.unbreakable(name).durability(TEArmorMaterials.getDurability(slot, durability)));
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
