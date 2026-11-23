package dev.skijump.session;

import dev.skijump.ModConfig;
import dev.skijump.career.CareerState;
import dev.skijump.comp.CompetitionManager;
import dev.skijump.hill.HillRecord;
import dev.skijump.hill.HillState;
import dev.skijump.net.InputData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Aktywne sesje skoków (po jednej na gracza). */
public final class SessionManager {
    private static final Map<UUID, JumpSession> SESSIONS = new HashMap<>();

    private SessionManager() { }

    public static boolean busy(UUID id) {
        return SESSIONS.containsKey(id);
    }

    public static void clearAll() {
        SESSIONS.clear();
    }

    static void remove(UUID id) {
        SESSIONS.remove(id);
    }

    public static void onInput(ServerPlayerEntity player, InputData d) {
        JumpSession s = SESSIONS.get(player.getUuid());
        if (s != null) s.accept(d);
    }

    public static void abort(ServerPlayerEntity player) {
        JumpSession s = SESSIONS.get(player.getUuid());
        if (s != null) s.end(false);
    }

    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        for (JumpSession s : new ArrayList<>(SESSIONS.values())) s.tick();
    }

    public static void tryStart(ServerPlayerEntity p, BlockPos gatePos) {
        ServerWorld w = p.getServerWorld();
        MinecraftServer server = w.getServer();
        if (SESSIONS.containsKey(p.getUuid())) {
            p.sendMessage(Text.translatable("skijump.msg.already"), true);
            return;
        }
        HillRecord h = HillState.get(server).byGate(w, gatePos);
        if (h == null) {
            p.sendMessage(Text.translatable("skijump.msg.gate_unregistered"), true);
            return;
        }
        if (!h.built) {
            p.sendMessage(Text.translatable("skijump.msg.hill_building"), true);
            return;
        }
        CareerState.Data cd = CareerState.get(server).of(p.getUuid());
        boolean bypass = p.isCreative() || p.getCommandSource().hasPermissionLevel(2) || !ModConfig.get().enforceRanks;
        int rank = CareerState.rankOf(cd.xp);
        if (!bypass && rank < h.rank) {
            p.sendMessage(Text.translatable("skijump.msg.locked", Text.translatable("skijump.rank." + CareerState.RANK_KEYS[Math.min(5, h.rank)])), true);
            return;
        }
        if (ModConfig.get().survivalHunger && !p.isCreative() && p.getHungerManager().getFoodLevel() < 4) {
            p.sendMessage(Text.translatable("skijump.msg.too_hungry"), true);
            return;
        }
        String err = CompetitionManager.checkStart(p, h);
        if (err != null) {
            p.sendMessage(Text.translatable(err), true);
            return;
        }
        boolean comp = CompetitionManager.isCompetitionJump(h, p);
        int refGate = comp ? CompetitionManager.refGate(h) : h.gate;
        boolean guided = !comp && (cd.guideMode == 1 || (cd.guideMode == 0 && cd.jumps < ModConfig.get().guidedFirstJumps));
        SESSIONS.put(p.getUuid(), new JumpSession(p, w, h, comp, refGate, guided));
        if (guided) p.sendMessage(Text.translatable("skijump.guide.started"), false);
        p.sendMessage(Text.translatable("skijump.msg.seated", String.valueOf(h.gate)), true);
    }
}
