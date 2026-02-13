package dev.gga.techextensions.client.gui;

import dev.gga.techextensions.TechExtensions;
import dev.gga.techextensions.items.tool.advanced.VacuumGunItem;
import dev.gga.techextensions.menu.VacuumGunMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.model.Material;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import reborncore.client.gui.GuiBase;
import reborncore.client.gui.GuiBuilder;
import reborncore.client.gui.GuiSprites;

public class GuiVacuumGun extends AbstractContainerScreen<VacuumGunMenu> {
    private static final Material UPGRADES_TOP_SPRITE = new Material(
            ResourceLocation.parse("gui"),
            ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "upgrades_top"));
    private static final Material UPGRADES_SLOT_SPRITE = new Material(
            ResourceLocation.parse("gui"),
            ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "upgrades_slot"));
    private static final Material UPGRADES_BOTTOM_SPRITE = new Material(
            ResourceLocation.parse("gui"),
            ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "upgrades_bottom"));

    private final GuiBuilder builder = new GuiBuilder();

    public GuiVacuumGun(VacuumGunMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        // Upgrades are normally outside the bounds, so let's pretend we are within the bounds if there is a slot here.
        return getHoveredSlot(mouseX, mouseY) == null && super.hasClickedOutside(mouseX, mouseY, left, top);
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float partialTicks) {
        super.render(drawContext, mouseX, mouseY, partialTicks);
        this.renderTooltip(drawContext, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics drawContext, float partialTicks, int mouseX, int mouseY) {
        GuiBase.Layer layer = GuiBase.Layer.BACKGROUND;
        builder.drawDefaultBackground(drawContext, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

        // Upgrade slots on the left
        GuiSprites.drawSpriteStretched(drawContext, UPGRADES_TOP_SPRITE, leftPos - 24, topPos + 6, 24, 5);
        for (int i = 0; i < VacuumGunItem.ALLOWED_UPGRADES; i++) {
            GuiSprites.drawSpriteStretched(
                    drawContext, UPGRADES_SLOT_SPRITE, leftPos - 24, topPos + 11 + (i * 18), 24, 18);
        }
        GuiSprites.drawSpriteStretched(
                drawContext,
                UPGRADES_BOTTOM_SPRITE,
                leftPos - 24,
                topPos + 11 + (VacuumGunItem.ALLOWED_UPGRADES * 18),
                24,
                4);

        // 5 Hopper-like inventory slots (row of 5, centered)
        for (int j = 0; j < VacuumGunItem.INVENTORY_SIZE; j++) {
            this.drawSlot(drawContext, 44 + j * 18, 35, layer);
        }

        // Player Inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.drawSlot(drawContext, 8 + col * 18, 84 + row * 18, layer);
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.drawSlot(drawContext, 8 + col * 18, 142, layer);
        }
    }

    public void drawSlot(GuiGraphics drawContext, int x, int y, GuiBase.Layer layer) {
        if (layer == GuiBase.Layer.BACKGROUND) {
            x += this.leftPos;
            y += this.topPos;
        }
        builder.drawSlot(drawContext, x - 1, y - 1);
    }
}
