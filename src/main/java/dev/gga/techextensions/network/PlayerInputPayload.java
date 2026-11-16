package dev.gga.techextensions.network;

import dev.gga.techextensions.TechExtensions;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Movement keys held by a client. Before 1.21.2 the server only receives them while riding, so the client sends them
 * itself while it wears an item that needs them (the Electric Jetpack). Mirrors the later vanilla {@code Input} record.
 */
public record PlayerInputPayload(
        boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean shift)
        implements FabricPacket {
    public static final PlayerInputPayload EMPTY = new PlayerInputPayload(false, false, false, false, false, false);
    public static final PacketType<PlayerInputPayload> TYPE = PacketType.create(
            new ResourceLocation(TechExtensions.MOD_ID, "player_input"), buf -> unpack(buf.readByte()));

    private static final Map<UUID, PlayerInputPayload> LAST_INPUTS = new ConcurrentHashMap<>();

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(
                TYPE, (payload, player, responseSender) -> LAST_INPUTS.put(player.getUUID(), payload));
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> LAST_INPUTS.remove(handler.getPlayer().getUUID()));
    }

    /** The keys the player held when its client last reported them. */
    public static PlayerInputPayload lastInput(ServerPlayer player) {
        return LAST_INPUTS.getOrDefault(player.getUUID(), EMPTY);
    }

    private int pack() {
        return (forward ? 1 : 0)
                | (backward ? 2 : 0)
                | (left ? 4 : 0)
                | (right ? 8 : 0)
                | (jump ? 16 : 0)
                | (shift ? 32 : 0);
    }

    private static PlayerInputPayload unpack(int flags) {
        return new PlayerInputPayload(
                (flags & 1) != 0,
                (flags & 2) != 0,
                (flags & 4) != 0,
                (flags & 8) != 0,
                (flags & 16) != 0,
                (flags & 32) != 0);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(pack());
    }

    @Override
    public PacketType<PlayerInputPayload> getType() {
        return TYPE;
    }
}
