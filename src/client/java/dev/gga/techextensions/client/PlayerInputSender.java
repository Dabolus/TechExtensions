package dev.gga.techextensions.client;

import dev.gga.techextensions.items.armor.ElectricJetpackItem;
import dev.gga.techextensions.network.PlayerInputPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

/** Sends the movement keys to the server while the player wears the Electric Jetpack, whenever they change. */
public final class PlayerInputSender {
    private static @Nullable PlayerInputPayload lastSent;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            if (player == null
                    || !(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ElectricJetpackItem)
                    || !ClientPlayNetworking.canSend(PlayerInputPayload.TYPE)) {
                // Resend on the next equip, the server may have kept a stale state
                lastSent = null;
                return;
            }
            Input input = player.input;
            PlayerInputPayload current = new PlayerInputPayload(
                    input.up, input.down, input.left, input.right, input.jumping, input.shiftKeyDown);
            if (!current.equals(lastSent)) {
                ClientPlayNetworking.send(current);
                lastSent = current;
            }
        });
    }

    private PlayerInputSender() {}
}
