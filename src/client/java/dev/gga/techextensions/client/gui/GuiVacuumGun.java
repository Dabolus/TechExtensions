package dev.gga.techextensions.client.gui;

import dev.gga.techextensions.items.tool.advanced.VacuumGunItem;
import dev.gga.techextensions.menu.VacuumGunMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GuiVacuumGun extends AbstractUpgradeableItemScreen<VacuumGunMenu> {
    public GuiVacuumGun(VacuumGunMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, VacuumGunItem.ALLOWED_UPGRADES);
    }

    @Override
    protected void extractItemContents(GuiGraphicsExtractor drawContext) {
        // 5 Hopper-like inventory slots (row of 5, centered)
        for (int j = 0; j < VacuumGunItem.INVENTORY_SIZE; j++) {
            drawSlot(drawContext, 44 + j * 18, 35);
        }
    }
}
