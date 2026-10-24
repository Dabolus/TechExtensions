package dev.gga.techextensions.client.gui;

import dev.gga.techextensions.TechExtensions;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import reborncore.client.gui.GuiBuilder;
import reborncore.client.gui.GuiSprites;

/**
 * Base screen for handheld items that expose an upgrade panel on the left (Resonance Scanner, Vacuum Gun,
 * Bubble Gun). Draws the default background, the upgrade panel and the player inventory slots; subclasses draw
 * their own slots and decorations in {@link #extractItemContents}.
 */
public abstract class AbstractUpgradeableItemScreen<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {
    private static final SpriteId UPGRADES_TOP_SPRITE = sprite("upgrades_top");
    private static final SpriteId UPGRADES_SLOT_SPRITE = sprite("upgrades_slot");
    private static final SpriteId UPGRADES_BOTTOM_SPRITE = sprite("upgrades_bottom");

    protected static final int LABEL_COLOR = 0xFF555555;

    protected final GuiBuilder builder = GuiBuilder.INSTANCE;
    private final int upgradeSlots;

    protected AbstractUpgradeableItemScreen(T menu, Inventory inventory, Component title, int upgradeSlots) {
        super(menu, inventory, title);
        this.upgradeSlots = upgradeSlots;
    }

    private static SpriteId sprite(String path) {
        return new SpriteId(Identifier.parse("gui"), Identifier.fromNamespaceAndPath(TechExtensions.MOD_ID, path));
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        // Upgrade slots sit outside the screen bounds: treat a click on them as inside the screen
        return getHoveredSlot(mouseX, mouseY) == null && super.hasClickedOutside(mouseX, mouseY, left, top);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(drawContext, mouseX, mouseY, partialTicks);
        this.extractTooltip(drawContext, mouseX, mouseY);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float partialTicks) {
        builder.drawDefaultBackground(drawContext, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        drawUpgradePanel(drawContext);
        extractItemContents(drawContext);
        drawPlayerInventorySlots(drawContext);
        super.extractContents(drawContext, mouseX, mouseY, partialTicks);
    }

    /** Draws the item-specific slots and decorations. Coordinates passed to {@link #drawSlot} are screen-relative. */
    protected abstract void extractItemContents(GuiGraphicsExtractor drawContext);

    private void drawUpgradePanel(GuiGraphicsExtractor drawContext) {
        GuiSprites.drawSpriteStretched(drawContext, UPGRADES_TOP_SPRITE, leftPos - 24, topPos + 6, 24, 5);
        for (int i = 0; i < upgradeSlots; i++) {
            GuiSprites.drawSpriteStretched(
                    drawContext, UPGRADES_SLOT_SPRITE, leftPos - 24, topPos + 11 + (i * 18), 24, 18);
        }
        GuiSprites.drawSpriteStretched(
                drawContext, UPGRADES_BOTTOM_SPRITE, leftPos - 24, topPos + 11 + (upgradeSlots * 18), 24, 4);
    }

    private void drawPlayerInventorySlots(GuiGraphicsExtractor drawContext) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(drawContext, 8 + col * 18, 84 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(drawContext, 8 + col * 18, 142);
        }
    }

    /** Draws a slot background at the given screen-relative position. */
    protected void drawSlot(GuiGraphicsExtractor drawContext, int x, int y) {
        builder.drawSlot(drawContext, this.leftPos + x - 1, this.topPos + y - 1);
    }

    /** Draws text in label space, i.e. already translated to the screen origin as in {@code extractLabels}. */
    protected void drawText(GuiGraphicsExtractor drawContext, Component text, int x, int y, int colour) {
        drawContext.text(this.font, text, x, y, colour, false);
    }

    protected void drawCenteredText(GuiGraphicsExtractor drawContext, Component text, int y, int colour) {
        drawText(drawContext, text, (this.imageWidth - this.font.width(text)) / 2, y, colour);
    }
}
