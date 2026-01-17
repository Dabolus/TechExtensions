package dev.gga.techextensions.items.armor;

import dev.gga.techextensions.init.TEItemSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import org.jetbrains.annotations.Nullable;
import reborncore.common.powerSystem.RcEnergyItem;
import reborncore.common.powerSystem.RcEnergyTier;
import reborncore.common.util.ItemUtils;

public abstract class TEEnergyArmourItem extends Item implements RcEnergyItem {
    public final long maxCharge;
    private final RcEnergyTier energyTier;

    public TEEnergyArmourItem(
            ArmorMaterial material, ArmorType slot, long maxCharge, RcEnergyTier energyTier, String name) {
        super(TEItemSettings.unbreakable(name).stacksTo(1).humanoidArmor(material, slot));
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

    @Nullable
    public EquipmentSlot getSlotType() {
        Equippable equippableComponent = this.components().get(DataComponents.EQUIPPABLE);
        return equippableComponent != null ? equippableComponent.slot() : null;
    }
}
