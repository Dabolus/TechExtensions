package dev.gga.techextensions.client.gui;

import dev.gga.techextensions.TechExtensions;
import dev.gga.techextensions.items.tool.advanced.ResonanceScannerItem;
import dev.gga.techextensions.menu.ResonanceScannerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import reborncore.client.gui.GuiBase;
import reborncore.client.gui.GuiBuilder;
import reborncore.client.gui.GuiSprites;

public class GuiResonanceScanner extends AbstractContainerScreen<ResonanceScannerMenu> {
    private static final SpriteId UPGRADES_TOP_SPRITE = new SpriteId(
            Identifier.parse("gui"), Identifier.fromNamespaceAndPath(TechExtensions.MOD_ID, "upgrades_top"));
    private static final SpriteId UPGRADES_SLOT_SPRITE = new SpriteId(
            Identifier.parse("gui"), Identifier.fromNamespaceAndPath(TechExtensions.MOD_ID, "upgrades_slot"));
    private static final SpriteId UPGRADES_BOTTOM_SPRITE = new SpriteId(
            Identifier.parse("gui"), Identifier.fromNamespaceAndPath(TechExtensions.MOD_ID, "upgrades_bottom"));

    private final Container inventory;
    private final GuiBuilder builder = GuiBuilder.INSTANCE;

    public GuiResonanceScanner(ResonanceScannerMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.inventory = inventory;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        // Upgrades are normally outside the bounds, so let's pretend we are within the bounds if there is a slot here.
        return getHoveredSlot(mouseX, mouseY) == null && super.hasClickedOutside(mouseX, mouseY, left, top);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(drawContext, mouseX, mouseY, partialTicks);
        this.extractTooltip(drawContext, mouseX, mouseY);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float partialTicks) {
        GuiBase.Layer layer = GuiBase.Layer.BACKGROUND;
        builder.drawDefaultBackground(drawContext, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

        GuiSprites.drawSpriteStretched(drawContext, UPGRADES_TOP_SPRITE, leftPos - 24, topPos + 6, 24, 5);
        for (int i = 0; i < ResonanceScannerItem.ALLOWED_UPGRADES; i++) {
            GuiSprites.drawSpriteStretched(
                    drawContext, UPGRADES_SLOT_SPRITE, leftPos - 24, topPos + 11 + (i * 18), 24, 18);
        }
        GuiSprites.drawSpriteStretched(
                drawContext,
                UPGRADES_BOTTOM_SPRITE,
                leftPos - 24,
                topPos + 11 + (ResonanceScannerItem.ALLOWED_UPGRADES * 18),
                24,
                4);

        // Scanner Slot
        this.drawSlot(drawContext, 80, 35, layer);

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
    protected void extractLabels(GuiGraphicsExtractor drawContext, int mouseX, int mouseY) {
        super.extractLabels(drawContext, mouseX, mouseY);
        GuiBase.Layer layer = GuiBase.Layer.FOREGROUND;
        ItemStack itemStack = inventory.getItem(0);
        ItemStack targetStack = ResonanceScannerItem.getTarget(itemStack);
        // No target set
        if (targetStack.isEmpty()) {
            drawCenteredText(
                    drawContext,
                    Component.translatable("techextensions.message.resonance_scanner.no_target"),
                    56,
                    0xFFAA0000,
                    layer);
            return;
        }
        Item item = targetStack.getItem();
        // Target is not a block
        if (!ResonanceScannerItem.isValidTarget(item)) {
            drawCenteredText(
                    drawContext,
                    Component.translatable("techextensions.message.resonance_scanner.invalid_target"),
                    56,
                    0xFFFF5555,
                    layer);
            return;
        }
        // Compute the range and display it
        long effectiveRange = ResonanceScannerItem.computeEffectiveRange(targetStack);
        drawCenteredText(
                drawContext,
                Component.translatable(
                        "techextensions.message.resonance_scanner.range",
                        Component.literal(Long.toString(effectiveRange)).withStyle(ChatFormatting.DARK_GREEN)),
                56,
                0xFF555555,
                layer);
    }

    public void drawSlot(GuiGraphicsExtractor drawContext, int x, int y, GuiBase.Layer layer) {
        if (layer == GuiBase.Layer.BACKGROUND) {
            x += this.leftPos;
            y += this.topPos;
        }
        builder.drawSlot(drawContext, x - 1, y - 1);
    }

    public void drawCenteredText(
            GuiGraphicsExtractor drawContext, Component text, int y, int colour, GuiBase.Layer layer) {
        drawText(drawContext, text, (imageWidth / 2 - getFont().width(text) / 2), y, colour, layer);
    }

    public void drawText(
            GuiGraphicsExtractor drawContext, Component text, int x, int y, int colour, GuiBase.Layer layer) {
        int factorX = 0;
        int factorY = 0;
        if (layer == GuiBase.Layer.BACKGROUND) {
            factorX = this.leftPos;
            factorY = this.topPos;
        }
        drawContext.text(Minecraft.getInstance().font, text, x + factorX, y + factorY, colour, false);
    }
}
