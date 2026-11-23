package dev.skijump.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import dev.skijump.comp.CompetitionManager;
import dev.skijump.comp.Competition;
import dev.skijump.comp.NpcJump;
import dev.skijump.hill.HillRecord;
import dev.skijump.hill.HillState;
import dev.skijump.physics.JumpResult;
import dev.skijump.physics.JumperStats;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Locale;

/**
 * Komputerowy zawodnik ("Skoczek") z fikcyjnym imieniem. Jego skok liczy ta sama fizyka co skok gracza
 * (bez ingerencji gracza), a encja odtwarza zapisaną trajektorię klatka po klatce.
 */
public class SkoczekEntity extends PathAwareEntity implements PoseSource {
    private static final TrackedData<Integer> PHASE = DataTracker.registerData(SkoczekEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> FLAGS = DataTracker.registerData(SkoczekEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> PITCH_T = DataTracker.registerData(SkoczekEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> ROLL_T = DataTracker.registerData(SkoczekEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> V_T = DataTracker.registerData(SkoczekEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> TUMBLE_T = DataTracker.registerData(SkoczekEntity.class, TrackedDataHandlerRegistry.FLOAT);

    /** Jedna klatka odtwarzanej trajektorii (współrzędne świata). */
    public static final class Frame {
        public double x, y, z;
        public float yaw, pitch, roll, v, tumble;
        public int phase;
        public boolean telemark, crouch;
    }

    private List<Frame> playback;
    private int index;
    private Runnable onDone;
    private boolean scripted;
    private int soloCooldown = 80;

    public SkoczekEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.setInvulnerable(true);
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(PHASE, 4);
        dataTracker.startTracking(FLAGS, 0);
        dataTracker.startTracking(PITCH_T, 0f);
        dataTracker.startTracking(ROLL_T, 0f);
        dataTracker.startTracking(V_T, 0f);
        dataTracker.startTracking(TUMBLE_T, 0f);
    }

    public void nameAs(String name) {
        setCustomName(Text.literal(name));
        setCustomNameVisible(true);
    }

    public void startPlayback(List<Frame> frames, Runnable done) {
        this.playback = frames;
        this.index = 0;
        this.onDone = done;
        if (!PLAYING.contains(this)) PLAYING.add(this);
        setNoGravity(true);
        setAiDisabled(true);
    }

    public boolean isPlaying() {
        return playback != null;
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        if (playback == null) {
            if (!scripted && getWorld() instanceof ServerWorld sw && --soloCooldown <= 0) soloJump(sw);
            return;
        }
    }

    private static final List<SkoczekEntity> PLAYING = new java.util.ArrayList<>();

    /** Wywoływane co tick serwera: odtwarzanie skoków nie zależy od tego, czy chunk z encją jest załadowany. */
    public static void tickPlaybacks() {
        if (PLAYING.isEmpty()) return;
        for (SkoczekEntity e : new java.util.ArrayList<>(PLAYING)) e.advance();
    }

    public static void clearPlaybacks() {
        PLAYING.clear();
    }

    private void advance() {
        if (playback == null) {
            PLAYING.remove(this);
            return;
        }
        if (index < playback.size()) {
            Frame f = playback.get(index++);
            if (isRemoved()) return;
            refreshPositionAndAngles(f.x, f.y, f.z, f.yaw, 0f);
            setBodyYaw(f.yaw);
            setHeadYaw(f.yaw);
            setVelocity(0, 0, 0);
            dataTracker.set(PHASE, f.phase);
            dataTracker.set(PITCH_T, f.pitch);
            dataTracker.set(ROLL_T, f.roll);
            dataTracker.set(V_T, f.v);
            dataTracker.set(TUMBLE_T, f.tumble);
            dataTracker.set(FLAGS, (f.telemark ? 1 : 0) | (f.crouch ? 2 : 0));
        } else {
            Runnable r = onDone;
            playback = null;
            onDone = null;
            PLAYING.remove(this);
            if (!isRemoved()) {
                setNoGravity(false);
                dataTracker.set(PHASE, 4);
            }
            if (r != null) r.run();
        }
    }

    /** Zawodnik sterowany przez zawody (nie skacze sam). */
    public void setScripted(boolean v) {
        this.scripted = v;
    }

    /** Skoczek bez zawodów trenuje: co kilkanaście sekund wchodzi na belkę najbliższej skoczni i skacze. */
    private void soloJump(ServerWorld sw) {
        soloCooldown = 100;
        HillRecord h = HillState.get(sw.getServer()).nearest(sw, getBlockPos(), 150);
        if (h == null || !h.built || CompetitionManager.of(h) != null) return;
        int hash = getUuid().hashCode();
        double skill = 0.35 + (Math.abs(hash % 100) / 100.0) * 0.6;
        double b = 0.3 + 0.6 * skill;
        NpcJump nj = NpcJump.compute(sw, h, skill, new JumperStats(b, b, b), h.gate, sw.getTime() * 17L + hash);
        SkoczekEntity.Frame f0 = nj.frames.get(0);
        refreshPositionAndAngles(f0.x, f0.y, f0.z, f0.yaw, 0f);
        if (!hasCustomName()) nameAs(Competition.randomName(new java.util.Random(hash)));
        final JumpResult r = nj.result;
        final String nm = getName().getString();
        startPlayback(nj.frames, () -> {
            soloCooldown = 300 + sw.random.nextInt(400);
            for (ServerPlayerEntity p : sw.getPlayers()) {
                if (p.squaredDistanceTo(this) < 250.0 * 250.0) {
                    p.sendMessage(Text.translatable("skijump.npc.jump", nm, String.format(Locale.US, "%.1f", r.distance),
                            String.format(Locale.US, "%.1f", r.total)), false);
                }
            }
        });
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override public int posePhase() { return dataTracker.get(PHASE); }
    @Override public float posePitch() { return dataTracker.get(PITCH_T); }
    @Override public float poseRoll() { return dataTracker.get(ROLL_T); }
    @Override public float poseV() { return dataTracker.get(V_T); }
    @Override public float poseTumble() { return dataTracker.get(TUMBLE_T); }
    @Override public boolean poseTelemark() { return (dataTracker.get(FLAGS) & 1) != 0; }
    @Override public boolean poseCrouch() { return (dataTracker.get(FLAGS) & 2) != 0; }
}
