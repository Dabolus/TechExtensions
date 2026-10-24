package dev.gga.techextensions.client.gui;

import dev.gga.techextensions.config.TechExtensionsConfig;
import dev.gga.techextensions.items.tool.advanced.BubbleGunItem;
import dev.gga.techextensions.menu.BubbleGunMenu;
import dev.gga.techextensions.utils.TEFormatUtils;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public class GuiBubbleGun extends AbstractUpgradeableItemScreen<BubbleGunMenu> {
    /** Color of the tank background (dark). */
    private static final int TANK_BG_COLOR = 0xFF373737;

    private static final int TANK_BORDER_COLOR = 0xFFA0A0A0;
    /** Fallback water colour if the fluid sprite is unavailable. */
    private static final int WATER_FALLBACK_COLOR = 0xFF3F76E4;

    /** Tank position constants (relative to leftPos/topPos). */
    private static final int TANK_X = 140;

    private static final int TANK_Y = 14;
    private static final int TANK_WIDTH = 12;
    private static final int TANK_HEIGHT = 56;

    // Refreshed once per tick instead of on every frame
    private long waterAmount;
    private long maxWater;

    @Nullable
    private TextureAtlasSprite waterSprite;

    private int waterColor = 0xFFFFFF;
    private boolean hasGun;

    public GuiBubbleGun(BubbleGunMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component, BubbleGunItem.ALLOWED_UPGRADES);
    }

    @Override
    protected void init() {
        super.init();
        refreshState();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshState();
    }

    private void refreshState() {
        // Look the gun up again every tick: the stack captured by the menu goes stale on the client once the
        // first container sync replaces the hand stack object
        ItemStack gunStack = findGunStack();
        hasGun = !gunStack.isEmpty();
        waterAmount = hasGun ? BubbleGunItem.getWaterAmount(gunStack) : 0;
        maxWater = TechExtensionsConfig.bubbleGunWaterCapacity.get();

        Minecraft mc = Minecraft.getInstance();
        FluidModel fluidModel = mc.getModelManager().getFluidStateModelSet().get(Fluids.WATER.defaultFluidState());
        waterSprite = fluidModel != null && fluidModel.stillMaterial() != null
                ? fluidModel.stillMaterial().sprite()
                : null;
        waterColor = fluidModel != null && fluidModel.tintSource() != null && mc.level != null && mc.player != null
                ? fluidModel
                        .tintSource()
                        .colorInWorld(
                                Fluids.WATER.defaultFluidState().createLegacyBlock(),
                                mc.level,
                                mc.player.blockPosition())
                : 0xFFFFFF;
    }

    private ItemStack findGunStack() {
        Player player = this.minecraft != null ? this.minecraft.player : null;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof BubbleGunItem) {
            return mainHand;
        }
        ItemStack offHand = player.getOffhandItem();
        return offHand.getItem() instanceof BubbleGunItem ? offHand : ItemStack.EMPTY;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(drawContext, mouseX, mouseY, partialTicks);
        drawTankTooltip(drawContext, mouseX, mouseY);
    }

    @Override
    protected void extractItemContents(GuiGraphicsExtractor drawContext) {
        // Soap slot (top center)
        drawSlot(drawContext, 80, 17);
        // Water container input slot (left bottom)
        drawSlot(drawContext, 62, 53);
        // Container output slot (right bottom)
        drawSlot(drawContext, 98, 53);
        // Water tank indicator (right side of GUI)
        drawWaterTank(drawContext, leftPos + TANK_X, topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor drawContext, int mouseX, int mouseY) {
        super.extractLabels(drawContext, mouseX, mouseY);
        if (!hasGun) {
            return;
        }
        // Water amount label below the tank
        Component waterText = Component.literal(TEFormatUtils.formatNumber(waterAmount) + " mB");
        int tankCenterX = TANK_X + TANK_WIDTH / 2;
        drawText(
                drawContext,
                waterText,
                tankCenterX - this.font.width(waterText) / 2,
                TANK_Y + TANK_HEIGHT + 2,
                0xFF404040);
    }

    private void drawWaterTank(GuiGraphicsExtractor drawContext, int x, int y, int width, int height) {
        // Draw tank background
        drawContext.fill(x, y, x + width, y + height, TANK_BG_COLOR);

        // Draw water fill level
        if (hasGun && maxWater > 0 && waterAmount > 0) {
            float fillPercent = Math.min(1.0F, (float) waterAmount / maxWater);
            int fillHeight = Math.max(1, Math.round(fillPercent * (height - 2)));
            int innerWidth = width - 2;
            int fillX = x + 1;
            int fillBottom = y + height - 1;

            if (waterSprite != null) {
                int color = waterColor | 0xFF000000;

                // Tile the sprite from bottom up
                drawContext.enableScissor(fillX, fillBottom - fillHeight, fillX + innerWidth, fillBottom);
                int spriteSize = waterSprite.contents().width();
                for (int oy = 0; oy < fillHeight; oy += spriteSize) {
                    for (int ox = 0; ox < innerWidth; ox += spriteSize) {
                        int rw = Math.min(spriteSize, innerWidth - ox);
                        int rh = Math.min(spriteSize, fillHeight - oy);
                        drawContext.blitSprite(
                                RenderPipelines.GUI_TEXTURED,
                                waterSprite,
                                fillX + ox,
                                fillBottom - oy - rh,
                                rw,
                                rh,
                                color);
                    }
                }
                drawContext.disableScissor();
            } else {
                // Fallback to solid color if sprite unavailable
                drawContext.fill(x + 1, fillBottom - fillHeight, x + width - 1, fillBottom, WATER_FALLBACK_COLOR);
            }
        }

        // Draw tank border
        drawContext.fill(x, y, x + width, y + 1, TANK_BORDER_COLOR); // top
        drawContext.fill(x, y + height - 1, x + width, y + height, TANK_BORDER_COLOR); // bottom
        drawContext.fill(x, y, x + 1, y + height, TANK_BORDER_COLOR); // left
        drawContext.fill(x + width - 1, y, x + width, y + height, TANK_BORDER_COLOR); // right
    }

    private void drawTankTooltip(GuiGraphicsExtractor drawContext, int mouseX, int mouseY) {
        int tankScreenX = leftPos + TANK_X;
        int tankScreenY = topPos + TANK_Y;
        if (mouseX >= tankScreenX
                && mouseX < tankScreenX + TANK_WIDTH
                && mouseY >= tankScreenY
                && mouseY < tankScreenY + TANK_HEIGHT
                && hasGun) {
            List<Component> tooltip = List.of(
                    Component.translatable("techextensions.tooltip.bubble_gun.water"),
                    Component.literal(TEFormatUtils.formatNumber(waterAmount) + " / "
                                    + TEFormatUtils.formatNumber(maxWater) + " mB")
                            .withStyle(ChatFormatting.GRAY));
            drawContext.setTooltipForNextFrame(this.font, tooltip, Optional.empty(), mouseX, mouseY);
        }
    }
}
