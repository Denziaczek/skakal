package dev.skijump.session;

import dev.skijump.ModConfig;
import dev.skijump.hill.HillRecord;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Wiatr w świecie: losowy, wolno zmienny (błądzenie losowe), zależny od pogody Minecrafta
 * (deszcz = silniejszy, burza = silne porywy). Wartości lokalne dla skoczni liczone względem jej kierunku.
 */
public final class WindManager {
    public record WindInfo(double speed, double dirRad, double gust) { }

    private static final class State {
        double speed = 1.5;
        double dir;
        double gust = 0.8;
        final Random rng;

        State(long seed) {
            rng = new Random(seed);
            dir = rng.nextDouble() * Math.PI * 2;
        }
    }

    private static final Map<RegistryKey<World>, State> STATES = new HashMap<>();

    private WindManager() { }

    private static State state(ServerWorld w) {
        return STATES.computeIfAbsent(w.getRegistryKey(), k -> new State(w.getSeed() ^ 0x77f1dL));
    }

    public static WindInfo get(ServerWorld w) {
        State s = state(w);
        return new WindInfo(s.speed, s.dir, s.gust);
    }

    public static void tick(MinecraftServer server) {
        if (server.getTicks() % 20 != 0) return;
        double mult = ModConfig.get().windStrength;
        for (ServerWorld w : server.getWorlds()) {
            State s = state(w);
            boolean thunder = w.isThundering();
            boolean rain = w.isRaining();
            double target = (thunder ? 6.0 : rain ? 3.2 : 1.6) * mult;
            double tg = (thunder ? 3.5 : rain ? 1.8 : 0.9) * mult;
            s.speed += (target - s.speed) * 0.04 + s.rng.nextGaussian() * 0.25 * Math.max(0.2, mult);
            s.speed = Math.max(0, Math.min(12, s.speed));
            s.dir += s.rng.nextGaussian() * 0.12;
            s.gust += (tg - s.gust) * 0.1;
        }
    }

    /** Składowe wiatru w układzie skoczni: {head (w twarz > 0), side (w prawo > 0), gust}. */
    public static double[] local(ServerWorld w, HillRecord hill) {
        return local(get(w), hill.facing);
    }

    public static double[] local(WindInfo info, Direction facing) {
        double wx = Math.cos(info.dirRad()) * info.speed();   // prędkość powietrza w osiach świata
        double wz = Math.sin(info.dirRad()) * info.speed();
        Direction r = facing.rotateYClockwise();
        double along = wx * facing.getOffsetX() + wz * facing.getOffsetZ();
        double side = wx * r.getOffsetX() + wz * r.getOffsetZ();
        return new double[]{-along, side, info.gust()};
    }

    /** Klucz tłumaczenia strony świata, Z KTÓREJ wieje wiatr. */
    public static String compassKey(double dirRad) {
        double ox = -Math.cos(dirRad), oz = -Math.sin(dirRad);
        double heading = Math.atan2(ox, -oz);               // od północy zgodnie ze wskazówkami zegara
        int idx = (int) Math.round(heading / (Math.PI / 4.0));
        idx = Math.floorMod(idx, 8);
        String[] keys = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        return "skijump.compass." + keys[idx];
    }
}
