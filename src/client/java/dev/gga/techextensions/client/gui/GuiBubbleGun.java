package dev.gga.techextensions.client.gui;

import dev.gga.techextensions.TechExtensions;
import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.items.tool.advanced.BubbleGunItem;
import dev.gga.techextensions.menu.BubbleGunMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.model.Material;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import reborncore.client.gui.GuiBase;
import reborncore.client.gui.GuiBuilder;
import reborncore.client.gui.GuiSprites;
import reborncore.common.powerSystem.RcEnergyItem;

public class GuiBubbleGun extends AbstractContainerScreen<BubbleGunMenu> {
    private static final Material UPGRADES_TOP_SPRITE = new Material(
            ResourceLocation.parse("gui"),
            ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "upgrades_top"));
    private static final Material UPGRADES_SLOT_SPRITE = new Material(
            ResourceLocation.parse("gui"),
            ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "upgrades_slot"));
    private static final Material UPGRADES_BOTTOM_SPRITE = new Material(
            ResourceLocation.parse("gui"),
            ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, "upgrades_bottom"));

    /** Color of the water tank fill bar (light blue). */
    private static final int WATER_COLOR = 0xFF3F76E4;
    /** Color of the tank background (dark). */
    private static final int TANK_BG_COLOR = 0xFF373737;

    private final Inventory playerInventory;
    private final GuiBuilder builder = new GuiBuilder();

    public GuiBubbleGun(BubbleGunMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.playerInventory = inventory;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
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
        for (int i = 0; i < BubbleGunItem.ALLOWED_UPGRADES; i++) {
            GuiSprites.drawSpriteStretched(
                    drawContext, UPGRADES_SLOT_SPRITE, leftPos - 24, topPos + 11 + (i * 18), 24, 18);
        }
        GuiSprites.drawSpriteStretched(
                drawContext,
                UPGRADES_BOTTOM_SPRITE,
                leftPos - 24,
                topPos + 11 + (BubbleGunItem.ALLOWED_UPGRADES * 18),
                24,
                4);

        // Soap slot (top center)
        this.drawSlot(drawContext, 80, 17, layer);

        // Cell input slot (left bottom)
        this.drawSlot(drawContext, 62, 53, layer);

        // Cell output slot (right bottom)
        this.drawSlot(drawContext, 98, 53, layer);

        // Water tank indicator (right side of GUI)
        drawWaterTank(drawContext, leftPos + 140, topPos + 14, 12, 56);

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

    @Override
    protected void renderLabels(GuiGraphics drawContext, int mouseX, int mouseY) {
        super.renderLabels(drawContext, mouseX, mouseY);

        // Get the gun stack to display stats
        ItemStack gunStack = getGunStack();
        if (gunStack.isEmpty()) {
            return;
        }

        // Energy display
        long storedEnergy = ((RcEnergyItem) gunStack.getItem()).getStoredEnergy(gunStack);
        long maxEnergy = ((RcEnergyItem) gunStack.getItem()).getEnergyCapacity(gunStack);
        String energyText = formatNumber(storedEnergy) + " / " + formatNumber(maxEnergy) + " EU";
        drawContext.drawString(Minecraft.getInstance().font, Component.literal(energyText), 8, 6, 0xFF404040, false);

        // Water amount label near the tank
        long waterAmount = BubbleGunItem.getWaterAmount(gunStack);
        long maxWater = TechExtensionsConfig.bubbleGunWaterCapacity;
        String waterText = waterAmount + "/" + maxWater + " mB";
        drawContext.drawString(Minecraft.getInstance().font, Component.literal(waterText), 25, 38, 0xFF404040, false);
    }

    private void drawWaterTank(GuiGraphics drawContext, int x, int y, int width, int height) {
        // Draw tank background
        drawContext.fill(x, y, x + width, y + height, TANK_BG_COLOR);

        // Draw water fill level
        ItemStack gunStack = getGunStack();
        if (!gunStack.isEmpty()) {
            long waterAmount = BubbleGunItem.getWaterAmount(gunStack);
            long maxWater = TechExtensionsConfig.bubbleGunWaterCapacity;
            if (maxWater > 0 && waterAmount > 0) {
                float fillPercent = Math.min(1.0F, (float) waterAmount / maxWater);
                int fillHeight = Math.round(fillPercent * (height - 2));
                drawContext.fill(x + 1, y + height - 1 - fillHeight, x + width - 1, y + height - 1, WATER_COLOR);
            }
        }

        // Draw tank border using fill lines
        int borderColor = 0xFFA0A0A0;
        drawContext.fill(x, y, x + width, y + 1, borderColor); // top
        drawContext.fill(x, y + height - 1, x + width, y + height, borderColor); // bottom
        drawContext.fill(x, y, x + 1, y + height, borderColor); // left
        drawContext.fill(x + width - 1, y, x + width, y + height, borderColor); // right
    }

    private ItemStack getGunStack() {
        ItemStack mainHand = playerInventory.player.getMainHandItem();
        if (mainHand.getItem() instanceof BubbleGunItem) {
            return mainHand;
        }
        ItemStack offHand = playerInventory.player.getOffhandItem();
        if (offHand.getItem() instanceof BubbleGunItem) {
            return offHand;
        }
        return ItemStack.EMPTY;
    }

    public void drawSlot(GuiGraphics drawContext, int x, int y, GuiBase.Layer layer) {
        if (layer == GuiBase.Layer.BACKGROUND) {
            x += this.leftPos;
            y += this.topPos;
        }
        builder.drawSlot(drawContext, x - 1, y - 1);
    }

    private static String formatNumber(long value) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000.0);
        } else if (value >= 1_000) {
            return String.format("%.1fK", value / 1_000.0);
        }
        return Long.toString(value);
    }
}
