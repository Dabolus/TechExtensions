package dev.gga.techextensions.blockentity;

import dev.gga.techextensions.TechExtensions;
import dev.gga.techextensions.blockentity.machine.ElectricDuctedFanBlockEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import reborncore.api.blockentity.IMachineGuiHandler;
import reborncore.common.network.BlockPosPayload;
import reborncore.common.screen.BuiltScreenHandler;
import reborncore.common.screen.BuiltScreenHandlerProvider;

public record TEGuiType<T extends BlockEntity>(
        ResourceLocation identifier, MenuType<BuiltScreenHandler> screenHandlerType) implements IMachineGuiHandler {

    public static final TEGuiType<ElectricDuctedFanBlockEntity> ELECTRIC_DUCTED_FAN = register("electric_ducted_fan");

    private static <T extends BlockEntity> TEGuiType<T> register(String path) {
        var id = ResourceLocation.fromNamespaceAndPath(TechExtensions.MOD_ID, path);
        var screenHandlerType = Registry.register(
                BuiltInRegistries.MENU,
                id,
                new ExtendedScreenHandlerType<>(getScreenHandlerFactory(id), ScreenHandlerData.PACKET_CODEC));
        return new TEGuiType<>(id, screenHandlerType);
    }

    private static ExtendedScreenHandlerType.ExtendedFactory<BuiltScreenHandler, ScreenHandlerData>
            getScreenHandlerFactory(ResourceLocation identifier) {
        return (syncId, playerInventory, payload) -> {
            if (!payload.isWithinDistance(playerInventory.player, 16)) {
                throw new IllegalStateException("Player cannot use this block entity as its too far away");
            }

            final BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(payload.pos());
            BuiltScreenHandler screenHandler =
                    ((BuiltScreenHandlerProvider) blockEntity).createScreenHandler(syncId, playerInventory.player);

            //noinspection unchecked
            screenHandler.setType((MenuType<BuiltScreenHandler>) BuiltInRegistries.MENU.getValue(identifier));
            return screenHandler;
        };
    }

    @Override
    public void open(Player player, BlockPos pos, Level world) {
        if (!world.isClientSide()) {
            player.openMenu(new ExtendedScreenHandlerFactory<ScreenHandlerData>() {
                @Override
                public ScreenHandlerData getScreenOpeningData(ServerPlayer player) {
                    return new ScreenHandlerData(pos);
                }

                @Override
                public Component getDisplayName() {
                    return Component.literal("N/A");
                }

                @Override
                public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
                    final BlockEntity blockEntity = player.level().getBlockEntity(pos);
                    BuiltScreenHandler screenHandler =
                            ((BuiltScreenHandlerProvider) blockEntity).createScreenHandler(syncId, player);
                    screenHandler.setType(screenHandlerType);
                    return screenHandler;
                }
            });
        }
    }

    record ScreenHandlerData(BlockPos pos) implements BlockPosPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, ScreenHandlerData> PACKET_CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, ScreenHandlerData::pos, ScreenHandlerData::new);
    }

    public ResourceLocation getIdentifier() {
        return identifier;
    }

    public MenuType<BuiltScreenHandler> getScreenHandlerType() {
        return screenHandlerType;
    }
}
