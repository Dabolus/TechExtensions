package dev.gga.techextensions.items.armor;

import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.init.TEArmorMaterials;
import reborncore.api.items.ArmorBlockEntityTicker;
import reborncore.api.items.ArmorRemoveHandler;
import reborncore.common.powerSystem.RcEnergyTier;
import techreborn.items.armor.QuantumSuitFlightHandler;
import techreborn.items.armor.VanillaQuantumSuitFlightHandler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;

public class ElectricJetpackItem extends TEEnergyArmourItem implements ArmorBlockEntityTicker, ArmorRemoveHandler {
    public static QuantumSuitFlightHandler HANDLER = new VanillaQuantumSuitFlightHandler();

    public ElectricJetpackItem(String name) {
        super(TEArmorMaterials.ELECTRIC_JETPACK, ArmorType.CHESTPLATE, TechExtensionsConfig.electricJetpackCharge, RcEnergyTier.HIGH, name);
    }

    // TREnergyArmourItem
    @Override
    public long getEnergyMaxOutput(ItemStack stack) { return 0; }

    // ArmorBlockEntityTicker
    @Override
    public void tickArmor(ItemStack stack, boolean hasFullSuit, Player playerEntity) {
        if (playerEntity instanceof ServerPlayer && !playerEntity.isCreative()) {
            if (getStoredEnergy(stack) > TechExtensionsConfig.electricJetpackFlyingCost) {
                HANDLER.setAllowFlight(playerEntity, true);

                if (HANDLER.isFlying(playerEntity)) {
                    tryUseEnergy(stack, TechExtensionsConfig.electricJetpackFlyingCost);
                }
                playerEntity.setOnGround(true);
            } else {
                HANDLER.setAllowFlight(playerEntity, false);
            }
        }
    }

    // ArmorRemoveHandler
    @Override
    public void onRemoved(Player playerEntity) {
        if (!playerEntity.isCreative() && !playerEntity.isSpectator()) {
            HANDLER.setAllowFlight(playerEntity, false);
        }
    }
}
