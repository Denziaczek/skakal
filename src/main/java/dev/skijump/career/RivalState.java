package dev.skijump.career;

import dev.skijump.comp.Competition;
import dev.skijump.physics.JumperStats;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Stali rywale z imieniem, charakterem (typem) i rozwojem - startują w zawodach i uczą się razem z graczem. */
public final class RivalState extends PersistentState {
    public static final int TYPES = 6;   // 0 technik, 1 siłacz, 2 telemarkowiec, 3 ryzykant, 4 weteran, 5 debiutant

    public static final class Rival {
        public String name;
        public int type;
        public double skill;
        public int wins, podiums, meetings;

        public JumperStats stats() {
            double b = 0.30 + 0.60 * skill;
            double str = b, bal = b, lan = b;
            switch (type) {
                case 0 -> bal += 0.15;
                case 1 -> { str += 0.15; lan -= 0.08; }
                case 2 -> lan += 0.18;
                case 3 -> { str += 0.05; bal -= 0.08; }
                case 4 -> { str += 0.05; bal += 0.05; lan += 0.05; }
                default -> { str -= 0.08; bal -= 0.08; lan -= 0.08; }
            }
            return new JumperStats(str, bal, lan);
        }

        public double riskBias() {
            return type == 3 ? 3.0 : 0.0;
        }
    }

    public final List<Rival> rivals = new ArrayList<>();

    public static RivalState get(MinecraftServer server) {
        RivalState s = server.getOverworld().getPersistentStateManager().getOrCreate(RivalState::fromNbt, RivalState::new, "skijump_rivals");
        if (s.rivals.isEmpty()) {
            Random r = new Random(server.getOverworld().getSeed() ^ 0xA11CEL);
            int[] types = {0, 1, 2, 3, 4, 5, 0, 2};
            for (int i = 0; i < types.length; i++) {
                Rival v = new Rival();
                v.name = Competition.randomName(r);
                v.type = types[i];
                v.skill = types[i] == 5 ? 0.30 + r.nextDouble() * 0.1 : types[i] == 4 ? 0.70 + r.nextDouble() * 0.1 : 0.45 + r.nextDouble() * 0.3;
                s.rivals.add(v);
            }
            s.markDirty();
        }
        return s;
    }

    public static RivalState fromNbt(NbtCompound nbt) {
        RivalState s = new RivalState();
        NbtList list = nbt.getList("rivals", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound c = list.getCompound(i);
            Rival v = new Rival();
            v.name = c.getString("n");
            v.type = c.getInt("t");
            v.skill = c.getDouble("s");
            v.wins = c.getInt("w");
            v.podiums = c.getInt("p");
            v.meetings = c.getInt("m");
            s.rivals.add(v);
        }
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (Rival v : rivals) {
            NbtCompound c = new NbtCompound();
            c.putString("n", v.name);
            c.putInt("t", v.type);
            c.putDouble("s", v.skill);
            c.putInt("w", v.wins);
            c.putInt("p", v.podiums);
            c.putInt("m", v.meetings);
            list.add(c);
        }
        nbt.put("rivals", list);
        return nbt;
    }
}
