package dev.gga.techextensions.client;

import dev.gga.techextensions.blockentity.TEGuiType;
import dev.gga.techextensions.blockentity.machine.ElectricDuctedFanBlockEntity;
import dev.gga.techextensions.client.gui.GuiElectricDuctedFan;
import java.util.Objects;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.level.block.entity.BlockEntity;

public record TEClientGuiType<T extends BlockEntity>(TEGuiType<T> guiType, TEGuiFactory<T> guiFactory) {
    public static final TEClientGuiType<ElectricDuctedFanBlockEntity> ELECTRIC_DUCTED_FAN =
            register(TEGuiType.ELECTRIC_DUCTED_FAN, GuiElectricDuctedFan::new);

    public static <T extends BlockEntity> TEClientGuiType<T> register(TEGuiType<T> type, TEGuiFactory<T> factory) {
        return new TEClientGuiType<>(type, factory);
    }

    public TEClientGuiType(TEGuiType<T> guiType, TEGuiFactory<T> guiFactory) {
        this.guiType = Objects.requireNonNull(guiType);
        this.guiFactory = Objects.requireNonNull(guiFactory);

        MenuScreens.register(guiType.getScreenHandlerType(), guiFactory());
    }
}
