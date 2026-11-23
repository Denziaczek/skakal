package dev.skijump.career;

import dev.skijump.physics.JumpResult;
import dev.skijump.physics.JumperStats;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Kariera graczy: doświadczenie, ranga, statystyki zawodnika i medale (zapis w danych świata). */
public final class CareerState extends PersistentState {
    public static final int[] RANK_XP = {0, 120, 400, 1000, 2200, 4500};
    public static final String[] RANK_KEYS = {"amateur", "junior", "contender", "national", "master", "legend"};

    public static final class Data {
        public int xp;
        public double strength = 30, balance = 30, landing = 30;
        public int jumps, gold, silver, bronze, wins;
        public double bestDistance;
        public boolean tutorialSeen;
        /** 0 = automatycznie (pierwsze skoki), 1 = zawsze, 2 = wyłączony */
        public int guideMode;

        public JumperStats stats() {
            return new JumperStats(strength / 100.0, balance / 100.0, landing / 100.0);
        }
    }

    private final Map<UUID, Data> data = new HashMap<>();

    public static CareerState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(CareerState::fromNbt, CareerState::new, "skijump_career");
    }

    public Data of(UUID id) {
        return data.computeIfAbsent(id, k -> new Data());
    }

    public static int rankOf(int xp) {
        int r = 0;
        for (int i = 0; i < RANK_XP.length; i++) if (xp >= RANK_XP[i]) r = i;
        return r;
    }

    /** Doświadczenie i rozwój statystyk po skoku. Zwraca zdobyte XP. */
    public int applyJump(UUID id, JumpResult r, double k, boolean competition) {
        Data d = of(id);
        double rel = Math.max(0, Math.min(1.4, r.distance / k));
        int xp = (int) Math.round(4 + rel * 7 + (r.perfectTakeoff ? 3 : 0) + (r.fell ? -3 : 0) + (competition ? 4 : 0));
        xp = Math.max(1, xp);
        d.xp += xp;
        d.jumps++;
        d.bestDistance = Math.max(d.bestDistance, r.distance);
        double q = Math.exp(-Math.pow(r.takeoffErrMs / 110.0, 2));
        d.strength = Math.min(100, d.strength + 0.45 * q * (1 - d.strength / 100.0));
        d.balance = Math.min(100, d.balance + 0.40 * (r.fell ? 0.2 : 0.8) * (1 - d.balance / 100.0));
        d.landing = Math.min(100, d.landing + 0.45 * r.telemarkQuality * (1 - d.landing / 100.0));
        markDirty();
        return xp;
    }

    public static CareerState fromNbt(NbtCompound nbt) {
        CareerState s = new CareerState();
        NbtList list = nbt.getList("players", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound c = list.getCompound(i);
            Data d = new Data();
            d.xp = c.getInt("xp");
            d.strength = c.getDouble("str");
            d.balance = c.getDouble("bal");
            d.landing = c.getDouble("lan");
            d.jumps = c.getInt("jumps");
            d.gold = c.getInt("g");
            d.silver = c.getInt("s");
            d.bronze = c.getInt("b");
            d.wins = c.getInt("w");
            d.bestDistance = c.getDouble("best");
            d.tutorialSeen = c.getBoolean("tut");
            d.guideMode = c.getInt("gm");
            s.data.put(c.getUuid("id"), d);
        }
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, Data> e : data.entrySet()) {
            NbtCompound c = new NbtCompound();
            Data d = e.getValue();
            c.putUuid("id", e.getKey());
            c.putInt("xp", d.xp);
            c.putDouble("str", d.strength);
            c.putDouble("bal", d.balance);
            c.putDouble("lan", d.landing);
            c.putInt("jumps", d.jumps);
            c.putInt("g", d.gold);
            c.putInt("s", d.silver);
            c.putInt("b", d.bronze);
            c.putInt("w", d.wins);
            c.putDouble("best", d.bestDistance);
            c.putBoolean("tut", d.tutorialSeen);
            c.putInt("gm", d.guideMode);
            list.add(c);
        }
        nbt.put("players", list);
        return nbt;
    }
}
