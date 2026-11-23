package dev.skijump.net;

import dev.skijump.SkiJumpMod;
import dev.skijump.session.SessionManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Kanały sieciowe moda. */
public final class Net {
    public static final Identifier INPUT = SkiJumpMod.id("input");   // C2S
    public static final Identifier STATE = SkiJumpMod.id("state");   // S2C

    private Net() { }

    /** Rejestracja odbiorników po stronie serwera. */
    public static void initServer() {
        ServerPlayNetworking.registerGlobalReceiver(INPUT, (server, player, handler, buf, responseSender) -> {
            InputData d = InputData.read(buf);
            server.execute(() -> SessionManager.onInput(player, d));
        });
    }

    public static void sendState(ServerPlayerEntity player, StateData d) {
        PacketByteBuf buf = PacketByteBufs.create();
        d.write(buf);
        ServerPlayNetworking.send(player, STATE, buf);
    }
}
