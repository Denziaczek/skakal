package dev.skijump.comp;

import dev.skijump.ModConfig;
import dev.skijump.career.CareerState;
import dev.skijump.entity.SkoczekEntity;
import dev.skijump.hill.HillRecord;
import dev.skijump.hill.HillState;
import dev.skijump.physics.AutoPilot;
import dev.skijump.physics.ControlInput;
import dev.skijump.physics.GearStats;
import dev.skijump.physics.HillGeometry;
import dev.skijump.physics.JumpResult;
import dev.skijump.physics.JumpSimulation;
import dev.skijump.physics.JumperStats;
import dev.skijump.physics.Scoring;
import dev.skijump.physics.WindField;
import dev.skijump.registry.ModEntities;
import dev.skijump.registry.ModItems;
import dev.skijump.registry.ModBlocks;
import dev.skijump.registry.ModSounds;
import dev.skijump.session.WindManager;
import dev.skijump.session.WorldTerrain;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * Zawody na jednej skoczni: zapisy -> seria 1 (wszyscy) -> seria 2 (top 30) -> klasyfikacja i nagrody.
 * Zawodnicy skaczą po kolei (kolejka startowa); gracze skaczą sami, "Skoczkowie" liczeni tą samą fizyką.
 */
public final class Competition {
    public enum State { REGISTRATION, ROUND1, ROUND2, FINISHED }

    public static final class Entrant {
        public UUID player;                 // null = zawodnik komputerowy
        public String name;
        public double skill;
        public JumperStats stats;
        public dev.skijump.career.RivalState.Rival rival;
        public final JumpResult[] res = new JumpResult[2];
        public final boolean[] dns = new boolean[2];

        public double total() {
            double t = 0;
            for (JumpResult r : res) if (r != null) t += r.total;
            return t;
        }
    }

    private static final String[] FIRST = {"Aldo", "Bruno", "Cezary", "Dario", "Emil", "Fenn", "Gustaw", "Henrik", "Ivo", "Jarek", "Kasper",
            "Leon", "Marek", "Nils", "Oskar", "Pavel", "Rafael", "Sven", "Tomi", "Urban", "Viktor", "Wolf"};
    private static final String[] LAST = {"Vallen", "Brandelson", "Kestrin", "Vollmar", "Orlovsky", "Pryszka", "Daalberg", "Zielenko",
            "Mortensky", "Falkren", "Lindqvarn", "Hartwiger", "Sokolwen", "Tarnowicz", "Eskilsen", "Dravnik", "Moorhaven", "Kowarsky",
            "Ulbrecht", "Ravenholt", "Szarkow", "Nordmann", "Bielawski", "Torvaldsen"};

    public final ServerWorld world;
    public final HillRecord hill;
    public State state = State.REGISTRATION;
    public final List<Entrant> entrants = new ArrayList<>();
    public int round = 0;
    public final int refGate;

    private int regTicks;
    private List<Entrant> order = new ArrayList<>();
    private int idx;
    private Entrant current;
    private boolean npcPlaying;
    private int turnTicks;
    private int pauseTicks;
    private SkoczekEntity npcEntity;
    private final Random rng = new Random();

    Competition(ServerWorld world, HillRecord hill) {
        this.world = world;
        this.hill = hill;
        this.refGate = hill.gate;
        this.regTicks = ModConfig.get().registrationSeconds * 20;
    }

    // ------------------------------------------------------------------ zapisy

    public boolean join(ServerPlayerEntity p) {
        if (state != State.REGISTRATION) return false;
        for (Entrant e : entrants) if (p.getUuid().equals(e.player)) return false;
        Entrant e = new Entrant();
        e.player = p.getUuid();
        e.name = p.getName().getString();
        e.stats = CareerState.get(world.getServer()).of(p.getUuid()).stats();
        e.skill = 0.5;
        entrants.add(e);
        tell(Text.translatable("skijump.comp.joined", e.name, String.valueOf(entrants.size())));
        return true;
    }

    public boolean leave(ServerPlayerEntity p) {
        if (state != State.REGISTRATION) return false;
        boolean r = entrants.removeIf(e -> p.getUuid().equals(e.player));
        if (r) tell(Text.translatable("skijump.comp.left", p.getName().getString()));
        return r;
    }

    public boolean hasPlayer(UUID id) {
        for (Entrant e : entrants) if (id.equals(e.player)) return true;
        return false;
    }

    public Entrant currentEntrant() {
        return current;
    }

    public boolean isPlayersTurn(UUID id) {
        return current != null && id.equals(current.player) && !npcPlaying
                && (state == State.ROUND1 || state == State.ROUND2);
    }

    private void addNpcs() {
        int n = ModConfig.get().npcCount;
        List<String> used = new ArrayList<>();
        int count = 0;
        for (dev.skijump.career.RivalState.Rival rv : dev.skijump.career.RivalState.get(world.getServer()).rivals) {
            if (count >= n) break;
            Entrant e = new Entrant();
            e.name = rv.name;
            e.skill = rv.skill;
            e.stats = rv.stats();
            e.rival = rv;
            used.add(rv.name);
            entrants.add(e);
            count++;
        }
        for (; count < n; count++) {
            Entrant e = new Entrant();
            String name;
            int guard = 0;
            do {
                name = randomName(rng);
            } while (used.contains(name) && guard++ < 50);
            used.add(name);
            e.name = name;
            e.skill = 0.25 + rng.nextDouble() * 0.7;
            double base = 0.30 + 0.60 * e.skill;
            e.stats = new JumperStats(base + rng.nextGaussian() * 0.06, base + rng.nextGaussian() * 0.06, base + rng.nextGaussian() * 0.06);
            entrants.add(e);
        }
    }

    // ------------------------------------------------------------------ pętla

    /** Zwraca true, gdy zawody się zakończyły i można je usunąć. */
    boolean tick() {
        switch (state) {
            case REGISTRATION -> {
                regTicks--;
                if (regTicks == 20 * 30 || regTicks == 20 * 10) {
                    tell(Text.translatable("skijump.comp.reg_countdown", String.valueOf(regTicks / 20)));
                }
                if (regTicks <= 0) {
                    boolean anyPlayer = false;
                    for (Entrant e : entrants) if (e.player != null) anyPlayer = true;
                    if (!anyPlayer) {
                        tell(Text.translatable("skijump.comp.cancelled_nobody"));
                        return true;
                    }
                    addNpcs();
                    startRound(1);
                }
            }
            case ROUND1, ROUND2 -> turn();
            case FINISHED -> { return true; }
        }
        return false;
    }

    private void startRound(int r) {
        round = r;
        state = r == 1 ? State.ROUND1 : State.ROUND2;
        if (r == 1) {
            order = new ArrayList<>(entrants);
            Collections.shuffle(order, rng);
        } else {
            List<Entrant> ranked = standings();
            int cut = Math.min(30, ranked.size());
            order = new ArrayList<>(ranked.subList(0, cut));
            Collections.reverse(order);             // najlepszy skacze jako ostatni
        }
        idx = 0;
        current = null;
        pauseTicks = 60;
        tell(Text.translatable("skijump.comp.round_start", String.valueOf(r), String.valueOf(order.size())));
    }

    private void turn() {
        if (pauseTicks > 0) { pauseTicks--; return; }
        if (current == null) {
            if (idx >= order.size()) { endRound(); return; }
            current = order.get(idx++);
            turnTicks = 0;
            if (current.player == null) startNpc(current);
            else announcePlayerTurn(current);
            return;
        }
        if (npcPlaying) return;                      // czekamy na odtworzenie skoku NPC
        // czekamy na gracza
        turnTicks++;
        if (turnTicks == 20 * ModConfig.get().turnTimeoutSeconds / 2) {
            tell(Text.translatable("skijump.comp.turn_waiting", current.name));
        }
        ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(current.player);
        if (turnTicks > 20 * ModConfig.get().turnTimeoutSeconds || p == null) {
            current.dns[round - 1] = true;
            tell(Text.translatable("skijump.comp.dns", current.name));
            current = null;
            pauseTicks = 40;
        }
    }

    private void announcePlayerTurn(Entrant e) {
        ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(e.player);
        Vec3d g = Vec3d.ofCenter(hill.gatePos);
        tell(Text.translatable("skijump.comp.your_turn", e.name, String.valueOf(round)));
        if (p != null) {
            p.sendMessage(Text.translatable("skijump.comp.your_turn_hint", String.valueOf((int) g.x), String.valueOf((int) g.y),
                    String.valueOf((int) g.z)), false);
            world.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.START, SoundCategory.PLAYERS, 1.0f, 1.2f);
        }
    }

    /** Wynik skoku gracza (wywoływany przez sesję). */
    void playerResult(ServerPlayerEntity p, JumpResult r) {
        if (current == null || !p.getUuid().equals(current.player)) return;
        current.res[round - 1] = r;
        report(current, r);
        current = null;
        pauseTicks = 60;
    }

    void playerAborted(ServerPlayerEntity p) {
        if (current == null || !p.getUuid().equals(current.player)) return;
        current.dns[round - 1] = true;
        tell(Text.translatable("skijump.comp.dns", current.name));
        current = null;
        pauseTicks = 40;
    }

    private void report(Entrant e, JumpResult r) {
        List<Entrant> st = standings();
        int pos = st.indexOf(e) + 1;
        Text suffix = r.fell ? Text.literal(" (").append(Text.translatable("skijump.comp.fall")).append(Text.literal(")")) : Text.empty();
        tell(Text.translatable("skijump.comp.jump_result", e.name, String.format(Locale.US, "%.1f", r.distance),
                String.format(Locale.US, "%.1f", r.total), String.valueOf(pos), suffix));
        if (!r.fell && e.player != null) {
            hill.submit(e.name, r.distance, r.total);
            HillState.get(world.getServer()).markDirty();
        }
        Vec3d g = hill.toWorld(hill.geometry().xK(), 0, 0);
        world.playSound(null, g.x, g.y, g.z, ModSounds.CROWD, SoundCategory.PLAYERS, 1.5f, 1.0f);
    }

    // ------------------------------------------------------------------ zawodnicy komputerowi

    private void startNpc(Entrant e) {
        npcPlaying = true;
        tell(Text.translatable("skijump.comp.npc_turn", e.name, String.valueOf(round)));
        long seed = world.getTime() * 131L + e.name.hashCode() + round;
        NpcJump nj = NpcJump.compute(world, hill, e.skill, e.stats, refGate, seed, e.rival != null ? e.rival.riskBias() : 0.0);
        JumpResult r = nj.result;
        SkoczekEntity.Frame f0 = nj.frames.get(0);
        SkoczekEntity ent = new SkoczekEntity(ModEntities.SKOCZEK, world);
        ent.refreshPositionAndAngles(f0.x, f0.y, f0.z, f0.yaw, 0f);
        ent.nameAs(e.name);
        ent.setScripted(true);
        world.spawnEntity(ent);
        npcEntity = ent;
        ent.startPlayback(nj.frames, () -> {
            npcPlaying = false;
            ent.discard();
            npcEntity = null;
            e.res[round - 1] = r;
            report(e, r);
            current = null;
            pauseTicks = 50;
        });
    }

    public static String randomName(Random r) {
        return FIRST[r.nextInt(FIRST.length)] + " " + LAST[r.nextInt(LAST.length)];
    }

    // ------------------------------------------------------------------ klasyfikacja

    public List<Entrant> standings() {
        List<Entrant> l = new ArrayList<>(entrants);
        l.sort(Comparator.comparingDouble(Entrant::total).reversed());
        return l;
    }

    private void endRound() {
        current = null;
        if (round == 1) {
            sendStandings(Text.translatable("skijump.comp.after_round1"), 10);
            startRound(2);
        } else {
            finish();
        }
    }

    private void finish() {
        state = State.FINISHED;
        List<Entrant> st = standings();
        sendStandings(Text.translatable("skijump.comp.final"), 10);
        // rywale: rozwój i statystyki spotkań
        dev.skijump.career.RivalState rs = dev.skijump.career.RivalState.get(world.getServer());
        for (int i = 0; i < st.size(); i++) {
            dev.skijump.career.RivalState.Rival rv = st.get(i).rival;
            if (rv == null) continue;
            rv.meetings++;
            if (i == 0) { rv.wins++; rv.podiums++; }
            else if (i < 3) rv.podiums++;
            if (i < Math.max(1, st.size() / 3)) rv.skill = Math.min(0.97, rv.skill + 0.012);
            else if (i > 2 * st.size() / 3) rv.skill = Math.max(0.25, rv.skill - 0.004);
        }
        rs.markDirty();
        for (int i = 0; i < st.size(); i++) {
            Entrant pe = st.get(i);
            if (pe.player == null) continue;
            for (int k = 0; k < i; k++) {
                if (st.get(k).rival != null) {
                    ServerPlayerEntity pp = world.getServer().getPlayerManager().getPlayer(pe.player);
                    if (pp != null) pp.sendMessage(Text.translatable("skijump.rival.beat_you", st.get(k).name, String.valueOf(k + 1)), false);
                    break;
                }
            }
        }
        dev.skijump.career.SeasonState.get(world.getServer()).record(world.getServer(), st);
        CareerState career = CareerState.get(world.getServer());
        for (int i = 0; i < st.size(); i++) {
            Entrant e = st.get(i);
            if (e.player == null) continue;
            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(e.player);
            CareerState.Data d = career.of(e.player);
            int bonus = Math.max(0, 40 - i * 6);
            d.xp += bonus;
            if (i == 0) { d.gold++; d.wins++; }
            else if (i == 1) d.silver++;
            else if (i == 2) d.bronze++;
            career.markDirty();
            if (p != null) {
                p.sendMessage(Text.translatable("skijump.comp.your_place", String.valueOf(i + 1), String.valueOf(bonus)), false);
                if (i == 0) {
                    give(p, new ItemStack(ModItems.MEDAL_GOLD));
                    give(p, new ItemStack(ModBlocks.TROPHY_GOLD));
                    give(p, new ItemStack(Items.DIAMOND, 3));
                } else if (i == 1) {
                    give(p, new ItemStack(ModItems.MEDAL_SILVER));
                    give(p, new ItemStack(ModBlocks.TROPHY_SILVER));
                    give(p, new ItemStack(Items.EMERALD, 5));
                } else if (i == 2) {
                    give(p, new ItemStack(ModItems.MEDAL_BRONZE));
                    give(p, new ItemStack(ModBlocks.TROPHY_BRONZE));
                    give(p, new ItemStack(Items.EMERALD, 2));
                } else if (i < 10) {
                    give(p, new ItemStack(Items.IRON_INGOT, 3));
                }
            }
        }
    }

    private void give(ServerPlayerEntity p, ItemStack s) {
        p.getInventory().offerOrDrop(s);
    }

    void cancel() {
        state = State.FINISHED;
        if (npcEntity != null) npcEntity.discard();
        tell(Text.translatable("skijump.comp.cancelled"));
    }

    public void sendStandings(Text title, int max) {
        tell(title);
        List<Entrant> st = standings();
        for (int i = 0; i < Math.min(max, st.size()); i++) {
            Entrant e = st.get(i);
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < 2; k++) {
                if (e.res[k] != null) sb.append(String.format(Locale.US, "%.1fm ", e.res[k].distance));
                else if (e.dns[k]) sb.append("DNS ");
            }
            tell(Text.translatable("skijump.comp.row", String.valueOf(i + 1), e.name,
                    String.format(Locale.US, "%.1f", e.total()), sb.toString().trim()));
        }
    }

    /** Komunikat do graczy w pobliżu skoczni oraz uczestników. */
    public void tell(Text t) {
        Vec3d c = Vec3d.ofCenter(hill.gatePos);
        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
            boolean near = p.getWorld() == world && p.squaredDistanceTo(c) < 600.0 * 600.0;
            if (near || hasPlayer(p.getUuid())) p.sendMessage(t, false);
        }
    }
}
