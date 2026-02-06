package dev.gga.techextensions.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import reborncore.common.screen.BuiltScreenHandler;

public interface TEGuiFactory<T extends BlockEntity>
        extends MenuScreens.ScreenConstructor<BuiltScreenHandler, AbstractContainerScreen<BuiltScreenHandler>> {
    AbstractContainerScreen<BuiltScreenHandler> create(int syncId, Player playerEntity, T blockEntity);

    @Override
    default AbstractContainerScreen<BuiltScreenHandler> create(
            BuiltScreenHandler builtScreenHandler, Inventory playerInventory, Component text) {
        Player playerEntity = playerInventory.player;
        //noinspection unchecked
        T blockEntity = (T) builtScreenHandler.getBlockEntity();
        return create(builtScreenHandler.containerId, playerEntity, blockEntity);
    }
}
