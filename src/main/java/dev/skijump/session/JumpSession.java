package dev.skijump.session;

import dev.skijump.career.CareerState;
import dev.skijump.comp.CompetitionManager;
import dev.skijump.entity.JumperSeatEntity;
import dev.skijump.hill.HillRecord;
import dev.skijump.hill.HillState;
import dev.skijump.item.GearItem;
import dev.skijump.net.InputData;
import dev.skijump.net.Net;
import dev.skijump.net.StateData;
import dev.skijump.physics.ControlInput;
import dev.skijump.physics.GearStats;
import dev.skijump.physics.HillGeometry;
import dev.skijump.physics.JumpResult;
import dev.skijump.physics.JumpSimulation;
import dev.skijump.physics.JumperStats;
import dev.skijump.physics.Scoring;
import dev.skijump.physics.WindField;
import dev.skijump.registry.ModEntities;
import dev.skijump.registry.ModSounds;
import dev.skijump.registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/**
 * Sesja jednego skoku gracza po stronie serwera. Serwer jest autorytatywny: liczy całą fizykę
 * ({@link JumpSimulation}), przesuwa niewidzialną gondolę, w której siedzi gracz, i wysyła klientowi stan do HUD.
 * Klient wysyła tylko sterowanie (klawisze, timing wybicia).
 */
public final class JumpSession {
    public static final double DT = 0.05;

    public final ServerPlayerEntity player;
    public final ServerWorld world;
    public final HillRecord hill;
    public final HillGeometry geo;
    public final JumpSimulation sim;
    public final JumperSeatEntity seat;
    private final long seed;
    private final int refGate;
    private final boolean competitionJump;
    private final GearStats gear;
    private final ControlInput ctrl = new ControlInput();
    private final float yaw;

    private InputData input = new InputData();
    private boolean startReq, jumpReq, teleReq, abortReq;
    private final boolean guided;
    private int guideStage, graceTicks, runTicks;
    private boolean paused;
    private float jumpErr;
    private int ticks;
    private boolean ended, landedHandled;

    JumpSession(ServerPlayerEntity player, ServerWorld world, HillRecord hill, boolean competitionJump, int refGate, boolean guided) {
        this.guided = guided;
        this.player = player;
        this.world = world;
        this.hill = hill;
        this.geo = hill.geometry();
        this.competitionJump = competitionJump;
        this.refGate = refGate;
        this.seed = world.getTime() * 31L + player.getUuid().hashCode();
        MinecraftServer server = world.getServer();
        this.gear = GearItem.read(player);
        JumperStats stats = CareerState.get(server).of(player.getUuid()).stats();

        double[] loc = WindManager.local(world, hill);
        WindField wf = new WindField(seed);
        wf.baseHead = loc[0];
        wf.baseSide = loc[1];
        wf.baseUp = 0;
        wf.gust = loc[2];
        double snow = world.isThundering() ? 1.35 : world.isRaining() ? 1.2 : 0.95;
        this.sim = new JumpSimulation(geo, new WorldTerrain(world, hill), wf, gear, stats, hill.gate, HillGeometry.MAX_GATE, snow);
        this.yaw = hill.facing.asRotation();

        this.seat = new JumperSeatEntity(ModEntities.SEAT, world);
        Vec3d p = hill.toWorld(sim.px, geo.inrunY(sim.px), 0);
        seat.refreshPositionAndAngles(p.x, p.y, p.z, yaw, 0f);
        world.spawnEntity(seat);
        player.startRiding(seat, true);
        seat.setSkiPose(0, 0f, 0f, 0f, 0f, false, false);
        Net.sendState(player, buildState());
        world.playSound(null, p.x, p.y, p.z, ModSounds.START, SoundCategory.PLAYERS, 0.6f, 1.0f);
    }

    void accept(InputData d) {
        this.input = d;
        if (d.jump) { jumpReq = true; jumpErr = d.jumpErr; }
        if (d.telemark) teleReq = true;
        if (d.abort) abortReq = true;
    }

    public boolean isEnded() {
        return ended;
    }

    void tick() {
        if (ended) return;
        if (player.isRemoved() || player.isDead() || seat.isRemoved() || player.getVehicle() != seat) {
            end(false);
            return;
        }
        if (abortReq) {
            player.sendMessage(Text.translatable("skijump.msg.aborted"), true);
            end(false);
            return;
        }
        ticks++;
        ctrl.pitch = input.pitch;
        ctrl.roll = input.roll;
        ctrl.crouch = true;   // pozycja najazdowa jest automatyczna
        ctrl.vOpen = input.vOpen;

        if (sim.phase == JumpSimulation.Phase.SEATED) {
            if (jumpReq) {
                sim.begin();
                world.playSound(null, seat.getX(), seat.getY(), seat.getZ(), ModSounds.START, SoundCategory.PLAYERS, 0.8f, 1.4f);
            } else if (ticks > 20 * 180) {
                player.sendMessage(Text.translatable("skijump.msg.timeout_seat"), true);
                end(false);
                return;
            }
        } else {
            if (guided) {
                if (paused && jumpReq) { paused = false; graceTicks = 6; }
                if (!paused) checkGuideTrigger();
                if (paused) {
                    // czas zatrzymany: wyjaśniamy mechanikę, symulacja nie idzie naprzód
                    jumpReq = false;
                    teleReq = false;
                    Net.sendState(player, buildState());
                    return;
                }
            }
            if (graceTicks > 0) { graceTicks--; jumpReq = false; }
            if (sim.phase == JumpSimulation.Phase.INRUN) runTicks++;
            if (jumpReq && sim.phase == JumpSimulation.Phase.INRUN) sim.pressJump(jumpErr);
            if (teleReq) sim.pressTelemark();
            sim.step(DT, ctrl);
        }
        jumpReq = false;
        teleReq = false;

        moveSeat();
        effects();
        Net.sendState(player, buildState());
        if (sim.phase == JumpSimulation.Phase.DONE) end(true);
    }

    /** Przewodnik: w kluczowych momentach zatrzymuje czas i wyświetla objaśnienie (wznowienie: Spacja). */
    private void checkGuideTrigger() {
        if (sim.phase == JumpSimulation.Phase.FLIGHT && guideStage < 2) guideStage = 2;
        if (sim.phase == JumpSimulation.Phase.SLIDE && guideStage < 4) guideStage = 4;
        int next = guideStage + 1;
        boolean fire = switch (next) {
            case 1 -> sim.phase == JumpSimulation.Phase.INRUN && runTicks > 24;
            case 2 -> sim.phase == JumpSimulation.Phase.INRUN && sim.msToIdeal() < 520;
            case 3 -> sim.phase == JumpSimulation.Phase.FLIGHT && sim.flightTime > 0.6;
            case 4 -> sim.phase == JumpSimulation.Phase.FLIGHT && !sim.telemarkPressed && sim.timeToContact() < 1.1;
            default -> false;
        };
        if (fire) {
            guideStage = next;
            paused = true;
        }
    }

    private void moveSeat() {
        double x, y, z;
        switch (sim.phase) {
            case SEATED, INRUN -> { x = sim.px; y = geo.inrunY(sim.px); z = 0; }
            default -> { x = sim.x; y = sim.y; z = sim.z; }
        }
        Vec3d p = hill.toWorld(x, y, z);
        seat.setPosition(p.x, p.y, p.z);
        seat.setYaw(yaw);
        seat.setPitch(0f);
        player.setYaw(yaw);
        player.setBodyYaw(yaw);
        player.setHeadYaw(yaw);
        seat.setSkiPose(phaseCode(), (float) sim.pitch, (float) sim.roll, (float) sim.vOpen, (float) sim.tumble,
                sim.telemarkPressed, ctrl.crouch);
    }

    private int phaseCode() {
        return switch (sim.phase) {
            case SEATED -> 0;
            case INRUN -> 1;
            case FLIGHT -> 2;
            default -> 3;
        };
    }

    private void effects() {
        Vec3d p = seat.getPos();
        if (sim.phase == JumpSimulation.Phase.FLIGHT && ticks % 4 == 0) {
            double sp = Math.sqrt(sim.vx * sim.vx + sim.vy * sim.vy);
            world.playSound(null, p.x, p.y, p.z, ModSounds.WIND, SoundCategory.PLAYERS,
                    (float) Math.min(0.7, sp / 40.0), (float) (0.7 + sp / 70.0));
        }
        if (sim.phase == JumpSimulation.Phase.INRUN && ticks % 3 == 0) {
            world.spawnParticles(ParticleTypes.SNOWFLAKE, p.x, p.y + 0.1, p.z, 1, 0.1, 0.05, 0.1, 0.02);
        }
        if (sim.phase == JumpSimulation.Phase.SLIDE) {
            world.spawnParticles(ParticleTypes.SNOWFLAKE, p.x, p.y + 0.2, p.z, 3, 0.3, 0.1, 0.3, 0.05);
        }
        if (sim.landed && !landedHandled) {
            landedHandled = true;
            world.playSound(null, p.x, p.y, p.z, sim.fell ? ModSounds.FALL : ModSounds.LAND, SoundCategory.PLAYERS, 1.0f, 1.0f);
            float vol = sim.distance >= hill.k ? 1.4f : 0.8f;
            world.playSound(null, p.x, p.y, p.z, ModSounds.CROWD, SoundCategory.PLAYERS, vol, sim.fell ? 0.7f : 1.0f);
            world.spawnParticles(sim.fell ? ParticleTypes.CLOUD : ParticleTypes.SNOWFLAKE, p.x, p.y + 0.3, p.z,
                    sim.fell ? 25 : 40, 0.6, 0.3, 0.6, 0.12);
        }
    }

    StateData buildState() {
        StateData s = new StateData();
        s.active = !ended;
        s.phase = phaseCode();
        double[] loc = sim.phase == JumpSimulation.Phase.SEATED ? WindManager.local(world, hill) : null;
        switch (sim.phase) {
            case FLIGHT -> s.speed = (float) Math.sqrt(sim.vx * sim.vx + sim.vy * sim.vy + sim.vz * sim.vz);
            default -> s.speed = (float) sim.speed;
        }
        s.armed = sim.armed();
        s.msToIdeal = sim.phase == JumpSimulation.Phase.INRUN ? (float) sim.msToIdeal() : 0f;
        s.alpha = (float) sim.alpha;
        s.pitch = (float) sim.pitch;
        s.roll = (float) sim.roll;
        s.vOpen = (float) sim.vOpen;
        s.windHead = loc != null ? (float) loc[0] : (float) sim.windHead();
        s.windSide = loc != null ? (float) loc[1] : (float) sim.windSide();
        s.windUp = (float) sim.windUp();
        s.distance = sim.phase == JumpSimulation.Phase.FLIGHT ? (float) Math.hypot(sim.x, sim.y)
                : (sim.landed ? (float) sim.distance : 0f);
        s.clearance = (float) sim.clearance;
        s.timeToContact = sim.phase == JumpSimulation.Phase.FLIGHT ? (float) sim.timeToContact() : 99f;
        s.flightTime = (float) sim.flightTime;
        s.k = (float) hill.k;
        s.hs = (float) hill.hs;
        s.yaw = yaw;
        s.telemark = sim.telemarkPressed;
        s.crouch = ctrl.crouch;
        s.jumped = sim.jumpPressed;
        s.gate = sim.gate;
        s.guide = paused ? guideStage : 0;
        s.tookOff = sim.phase == JumpSimulation.Phase.FLIGHT || sim.phase == JumpSimulation.Phase.SLIDE || sim.phase == JumpSimulation.Phase.DONE;
        s.takeoffErr = sim.jumpPressed ? (float) sim.jumpErrMs : 999f;
        return s;
    }

    /** Zamyka sesję; przy poprawnym zakończeniu liczy wynik, nagrody i rekordy. */
    void end(boolean finished) {
        if (ended) return;
        ended = true;
        SessionManager.remove(player.getUuid());
        StateData off = buildState();
        off.active = false;
        Net.sendState(player, off);
        boolean ok = finished && sim.landed;
        player.stopRiding();
        seat.discard();
        if (!ok) {
            if (competitionJump) CompetitionManager.onPlayerAborted(world, hill, player);
            return;
        }
        finishJump();
    }

    private void finishJump() {
        MinecraftServer server = world.getServer();
        JumpResult r = Scoring.evaluate(sim, refGate, seed);
        CareerState career = CareerState.get(server);
        int rankBefore = CareerState.rankOf(career.of(player.getUuid()).xp);
        int xp = career.applyJump(player.getUuid(), r, hill.k, competitionJump);
        int rankAfter = CareerState.rankOf(career.of(player.getUuid()).xp);

        boolean record = false;
        if (!r.fell) {
            record = hill.submit(player.getName().getString(), r.distance, r.total);
            HillState.get(server).markDirty();
        }
        sendResult(r, xp, record);
        if (!player.isCreative() && dev.skijump.ModConfig.get().survivalHunger) player.addExhaustion(3.0f);
        if (rankAfter > rankBefore) {
            player.sendMessage(Text.translatable("skijump.msg.rank_up",
                    Text.translatable("skijump.rank." + CareerState.RANK_KEYS[rankAfter])), false);
            ItemStack reward = switch (rankAfter) {
                case 1 -> new ItemStack(ModItems.HELMET);
                case 2 -> new ItemStack(ModItems.SKI_IRON);
                case 3 -> new ItemStack(ModItems.SUIT_PRO);
                case 4 -> new ItemStack(ModItems.SKI_COMPOSITE);
                default -> new ItemStack(Items.DIAMOND, 8);
            };
            player.getInventory().offerOrDrop(reward);
            player.sendMessage(Text.translatable("skijump.msg.rank_reward", reward.getName()), false);
        }
        if (r.fell) {
            float dmg = (float) (2.0 + sim.impactSpeed * 0.35) * (float) (1.0 - 0.6 * gear.helmet);
            dmg = Math.min(dmg, Math.min(7.0f, player.getHealth() - 1.0f));
            if (dmg > 0.1f) player.damage(world.getDamageSources().fall(), dmg);
        }
        if (competitionJump) CompetitionManager.onPlayerResult(world, hill, player, r);
    }

    private static String f1(double v) {
        return String.format(Locale.US, "%.1f", v);
    }

    private void sendResult(JumpResult r, int xp, boolean record) {
        StringBuilder marks = new StringBuilder();
        for (int i = 0; i < r.judgeMarks.length; i++) {
            if (i > 0) marks.append(" ");
            marks.append(f1(r.judgeMarks[i]));
        }
        player.sendMessage(Text.translatable("skijump.msg.result", f1(r.distance), String.valueOf((int) hill.k), f1(r.total)), false);
        player.sendMessage(Text.translatable("skijump.msg.result_detail", f1(r.distancePoints), f1(r.styleTotal),
                f1(r.windPoints), f1(r.gatePoints), marks.toString()), false);
        player.sendMessage(Text.translatable("skijump.msg.result_tech", f1(r.takeoffSpeed * 3.6), String.format(Locale.US, "%+.0f", r.takeoffErrMs),
                f1(r.impactSpeed)), false);
        player.sendMessage(comment(r, record), false);
        if (dev.skijump.ModConfig.get().coachTips) {
            for (Text tip : Coach.tips(sim, r)) {
                player.sendMessage(Text.translatable("skijump.coach.prefix").append(tip), false);
            }
        }
        player.sendMessage(Text.translatable("skijump.msg.xp", String.valueOf(xp)), true);
    }

    private Text comment(JumpResult r, boolean record) {
        if (r.fell) return Text.translatable("skijump.comment.fall." + r.fallReason);
        if (record) return Text.translatable("skijump.comment.record", f1(r.distance));
        if (r.distance >= hill.hs) return Text.translatable("skijump.comment.beyond_hs");
        if (r.perfectTakeoff && r.distance >= hill.k) return Text.translatable("skijump.comment.perfect_takeoff");
        if (r.distance >= hill.k + 5) return Text.translatable("skijump.comment.great");
        if (r.distance >= hill.k) return Text.translatable("skijump.comment.good");
        if (sim.landingScore < 0.45) return Text.translatable("skijump.comment.borderline");
        if (r.takeoffErrMs < -50) return Text.translatable("skijump.comment.early");
        if (r.takeoffErrMs > 50) return Text.translatable("skijump.comment.late");
        if (Math.abs(sim.landingZ) > 5) return Text.translatable("skijump.comment.asym");
        if (r.distance >= hill.k * 0.85) return Text.translatable("skijump.comment.short");
        return Text.translatable("skijump.comment.very_short");
    }
}
