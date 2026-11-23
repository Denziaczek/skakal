package dev.skijump.career;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Krótki samouczek wyświetlany raz po dołączeniu do świata (powtórka: /skijump tutorial). */
public final class Tutorial {
    private Tutorial() { }

    public static void onJoin(ServerPlayerEntity p, MinecraftServer server) {
        CareerState cs = CareerState.get(server);
        CareerState.Data d = cs.of(p.getUuid());
        if (d.tutorialSeen) return;
        d.tutorialSeen = true;
        cs.markDirty();
        send(p);
    }

    public static void send(ServerPlayerEntity p) {
        p.sendMessage(Text.translatable("skijump.tutorial.title"), false);
        for (int i = 1; i <= 9; i++) p.sendMessage(Text.translatable("skijump.tutorial." + i), false);
    }
}
