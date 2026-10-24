package dev.gga.techextensions.client.gui;

import dev.gga.techextensions.items.tool.advanced.ResonanceScannerItem;
import dev.gga.techextensions.menu.ResonanceScannerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GuiResonanceScanner extends AbstractUpgradeableItemScreen<ResonanceScannerMenu> {
    public GuiResonanceScanner(ResonanceScannerMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, ResonanceScannerItem.ALLOWED_UPGRADES);
    }

    @Override
    protected void extractItemContents(GuiGraphicsExtractor drawContext) {
        // Target slot
        drawSlot(drawContext, 80, 35);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor drawContext, int mouseX, int mouseY) {
        super.extractLabels(drawContext, mouseX, mouseY);
        // Read the live target slot of the open menu, which is what the player is looking at
        ItemStack targetStack =
                this.menu.getSlot(ResonanceScannerItem.TARGET_SLOT).getItem();
        if (targetStack.isEmpty()) {
            drawCenteredText(
                    drawContext,
                    Component.translatable("techextensions.message.resonance_scanner.no_target"),
                    56,
                    0xFFAA0000);
            return;
        }
        if (!ResonanceScannerItem.isValidTarget(targetStack.getItem())) {
            drawCenteredText(
                    drawContext,
                    Component.translatable("techextensions.message.resonance_scanner.invalid_target"),
                    56,
                    0xFFFF5555);
            return;
        }
        long effectiveRange = ResonanceScannerItem.computeEffectiveRange(targetStack);
        drawCenteredText(
                drawContext,
                Component.translatable(
                        "techextensions.message.resonance_scanner.range",
                        Component.literal(Long.toString(effectiveRange)).withStyle(ChatFormatting.DARK_GREEN)),
                56,
                LABEL_COLOR);
    }
}
