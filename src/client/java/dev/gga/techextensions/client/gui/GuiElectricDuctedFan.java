package dev.gga.techextensions.client.gui;

import dev.gga.techextensions.blockentity.machine.ElectricDuctedFanBlockEntity;
import dev.gga.techextensions.blocks.machine.ElectricDuctedFanBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import reborncore.client.gui.GuiBase;
import reborncore.common.screen.BuiltScreenHandler;

public class GuiElectricDuctedFan extends GuiBase<BuiltScreenHandler> {

    private final ElectricDuctedFanBlockEntity blockEntity;

    public GuiElectricDuctedFan(int syncID, Player player, ElectricDuctedFanBlockEntity blockEntity) {
        super(player, blockEntity, blockEntity.createScreenHandler(syncID, player));
        this.blockEntity = blockEntity;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor drawContext, int mouseX, int mouseY) {
        super.extractLabels(drawContext, mouseX, mouseY);
        final Layer layer = Layer.FOREGROUND;

        long stored = blockEntity.getEnergy();

        // Draw the energy bar
        builder.drawMultiEnergyBar(
                drawContext,
                this,
                9,
                19,
                (int) stored,
                (int) blockEntity.getMaxStoredPower(),
                mouseX,
                mouseY,
                0,
                layer);

        if (!ElectricDuctedFanBlock.isActive(blockEntity.getBlockState())) {
            drawText(
                    drawContext,
                    Component.translatable("techextensions.message.electric_ducted_fan.inactive"),
                    30,
                    20,
                    0xFFFF5555,
                    layer);
            return;
        }

        // Same formulas as the block entity, so the GUI always matches the actual push behaviour
        double powerRatio = ElectricDuctedFanBlockEntity.computePowerRatio(stored);
        double fanMultiplier = ElectricDuctedFanBlockEntity.computeFanMultiplier(
                ElectricDuctedFanBlock.getFanCount(blockEntity.getBlockState()));

        double reach = blockEntity.computeReach(powerRatio, fanMultiplier);
        double pushStrength = blockEntity.computePushStrength(powerRatio, fanMultiplier);
        double gForce = pushStrength / 0.08; // 0.08 is the default Minecraft gravity strength

        drawText(
                drawContext,
                Component.translatable(
                        "techextensions.message.electric_ducted_fan.reach",
                        Component.literal(Long.toString(Math.round(reach))).withStyle(ChatFormatting.DARK_GREEN)),
                30,
                36,
                0xFF555555,
                layer);
        drawText(
                drawContext,
                Component.translatable(
                                "techextensions.message.electric_ducted_fan.push_strength",
                                Component.literal(String.format("%.1f", gForce)).withStyle(ChatFormatting.DARK_GREEN))
                        .append(Component.literal(" g").withStyle(ChatFormatting.ITALIC)),
                30,
                48,
                0xFF555555,
                layer);
    }
}
