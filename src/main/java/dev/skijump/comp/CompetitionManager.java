package dev.skijump.comp;

import dev.skijump.ModConfig;
import dev.skijump.career.CareerState;
import dev.skijump.hill.HillRecord;
import dev.skijump.hill.HillState;
import dev.skijump.physics.JumpResult;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Zarządza zawodami (po jednych na skocznię) i udostępnia akcje blokom/komendom. */
public final class CompetitionManager {
    private static final Map<String, Competition> ACTIVE = new HashMap<>();

    private CompetitionManager() { }

    public static void clearAll() {
        ACTIVE.clear();
    }

    public static Competition of(HillRecord h) {
        return ACTIVE.get(h.id);
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        for (String id : new ArrayList<>(ACTIVE.keySet())) {
            Competition c = ACTIVE.get(id);
            if (c.tick()) ACTIVE.remove(id);
        }
    }

    // ------------------------------------------------------------------ sesje skoków

    /** Zwraca klucz tłumaczenia błędu albo null, jeśli gracz może teraz skoczyć. */
    public static String checkStart(ServerPlayerEntity p, HillRecord h) {
        Competition c = ACTIVE.get(h.id);
        if (c == null) return null;                                   // wolny trening
        if (c.state == Competition.State.REGISTRATION) return "skijump.msg.comp_registration";
        if (c.state == Competition.State.FINISHED) return null;
        if (c.isPlayersTurn(p.getUuid())) return null;
        return "skijump.msg.comp_wait";
    }

    public static boolean isCompetitionJump(HillRecord h, ServerPlayerEntity p) {
        Competition c = ACTIVE.get(h.id);
        return c != null && c.isPlayersTurn(p.getUuid());
    }

    public static int refGate(HillRecord h) {
        Competition c = ACTIVE.get(h.id);
        return c != null ? c.refGate : h.gate;
    }

    public static void onPlayerResult(ServerWorld world, HillRecord h, ServerPlayerEntity p, JumpResult r) {
        Competition c = ACTIVE.get(h.id);
        if (c != null) c.playerResult(p, r);
    }

    public static void onPlayerAborted(ServerWorld world, HillRecord h, ServerPlayerEntity p) {
        Competition c = ACTIVE.get(h.id);
        if (c != null) c.playerAborted(p);
    }

    // ------------------------------------------------------------------ akcje

    public static void start(ServerPlayerEntity p, HillRecord h) {
        ServerWorld w = p.getServerWorld();
        if (ACTIVE.containsKey(h.id)) {
            p.sendMessage(Text.translatable("skijump.comp.already"), false);
            return;
        }
        if (!h.built) {
            p.sendMessage(Text.translatable("skijump.msg.hill_building"), true);
            return;
        }
        Competition c = new Competition(w, h);
        ACTIVE.put(h.id, c);
        c.tell(Text.translatable("skijump.comp.opened", hillName(h), String.valueOf(ModConfig.get().registrationSeconds)));
        c.join(p);
    }

    public static void join(ServerPlayerEntity p, HillRecord h) {
        Competition c = ACTIVE.get(h.id);
        if (c == null) { p.sendMessage(Text.translatable("skijump.comp.none"), false); return; }
        if (!c.join(p)) p.sendMessage(Text.translatable("skijump.comp.cannot_join"), false);
    }

    public static void leave(ServerPlayerEntity p, HillRecord h) {
        Competition c = ACTIVE.get(h.id);
        if (c == null || !c.leave(p)) p.sendMessage(Text.translatable("skijump.comp.cannot_leave"), false);
    }

    public static void cancel(HillRecord h) {
        Competition c = ACTIVE.remove(h.id);
        if (c != null) c.cancel();
    }

    public static void status(ServerPlayerEntity p, HillRecord h) {
        Competition c = ACTIVE.get(h.id);
        if (c == null) {
            p.sendMessage(Text.translatable("skijump.comp.none"), false);
            return;
        }
        p.sendMessage(Text.translatable("skijump.comp.status", hillName(h), c.state.name(), String.valueOf(c.entrants.size())), false);
        if (c.state != Competition.State.REGISTRATION) c.sendStandings(Text.translatable("skijump.comp.standings"), 8);
    }

    /** Prawy klik na stacji zawodów: zapisy / dołączanie / status. */
    public static void stationUse(ServerPlayerEntity p, HillRecord h) {
        Competition c = ACTIVE.get(h.id);
        if (c == null) {
            if (p.isSneaking()) start(p, h);
            else p.sendMessage(Text.translatable("skijump.comp.station_idle", hillName(h)), false);
            return;
        }
        if (c.state == Competition.State.REGISTRATION) {
            if (c.hasPlayer(p.getUuid())) c.leave(p);
            else c.join(p);
        } else {
            status(p, h);
        }
    }

    // ------------------------------------------------------------------ tablice

    public static Text hillName(HillRecord h) {
        return h.isCustom() ? Text.literal(h.name) : Text.translatable("skijump.hill." + h.presetId);
    }

    /** Tablica wyników: bieżące zawody lub rekordy skoczni. */
    public static void sendBoard(ServerPlayerEntity p, HillRecord h) {
        Competition c = ACTIVE.get(h.id);
        p.sendMessage(Text.translatable("skijump.board.title", hillName(h), String.valueOf((int) h.k), String.valueOf((int) h.hs)), false);
        if (c != null && c.state != Competition.State.REGISTRATION) {
            c.sendStandings(Text.translatable("skijump.comp.standings"), 8);
        }
        if (h.recordHolder.isEmpty()) p.sendMessage(Text.translatable("skijump.board.no_record"), false);
        else p.sendMessage(Text.translatable("skijump.board.record", String.format(Locale.US, "%.1f", h.recordDist), h.recordHolder), false);
        int i = 1;
        for (HillRecord.Entry e : h.top) {
            if (i > 5) break;
            p.sendMessage(Text.translatable("skijump.board.row", String.valueOf(i++), e.name,
                    String.format(Locale.US, "%.1f", e.distance), String.format(Locale.US, "%.1f", e.points)), false);
        }
    }

    public static void sendJudgeInfo(ServerPlayerEntity p, HillRecord h) {
        p.sendMessage(Text.translatable("skijump.judges.info", String.format(Locale.US, "%.1f", dev.skijump.physics.Scoring.metersValue(h.k)),
                String.valueOf((int) h.k)), false);
        CareerState.Data d = CareerState.get(p.getServer()).of(p.getUuid());
        p.sendMessage(Text.translatable("skijump.judges.you", String.format(Locale.US, "%.1f", d.bestDistance)), false);
    }
}
