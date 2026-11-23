package dev.skijump.comp;

import dev.skijump.entity.SkoczekEntity;
import dev.skijump.hill.HillRecord;
import dev.skijump.physics.AutoPilot;
import dev.skijump.physics.ControlInput;
import dev.skijump.physics.GearStats;
import dev.skijump.physics.HillGeometry;
import dev.skijump.physics.JumpResult;
import dev.skijump.physics.JumpSimulation;
import dev.skijump.physics.JumperStats;
import dev.skijump.physics.Scoring;
import dev.skijump.physics.WindField;
import dev.skijump.session.WindManager;
import dev.skijump.session.WorldTerrain;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Skok zawodnika komputerowego: ta sama fizyka co u gracza, wynik + klatki do odtworzenia przez encję Skoczek. */
public final class NpcJump {
    public final List<SkoczekEntity.Frame> frames;
    public final JumpResult result;

    private NpcJump(List<SkoczekEntity.Frame> frames, JumpResult result) {
        this.frames = frames;
        this.result = result;
    }

    public static NpcJump compute(ServerWorld world, HillRecord hill, double skill, JumperStats stats, int refGate, long seed) {
        return compute(world, hill, skill, stats, refGate, seed, 0.0);
    }

    public static NpcJump compute(ServerWorld world, HillRecord hill, double skill, JumperStats stats, int refGate, long seed, double riskBias) {
        HillGeometry geo = hill.geometry();
        double[] loc = WindManager.local(world, hill);
        WindField wf = new WindField(seed);
        wf.baseHead = loc[0];
        wf.baseSide = loc[1];
        wf.gust = loc[2];
        double snow = world.isThundering() ? 1.35 : world.isRaining() ? 1.2 : 0.95;
        double gq = 0.25 + 0.5 * skill;
        GearStats gear = new GearStats(gq, gq, gq * 0.9, gq, gq * 0.8, gq, 0.4);
        JumpSimulation sim = new JumpSimulation(geo, new WorldTerrain(world, hill), wf, gear, stats, hill.gate,
                HillGeometry.MAX_GATE, snow);
        AutoPilot ap = new AutoPilot(skill, seed);
        ap.riskBias = riskBias;
        ControlInput in = new ControlInput();
        List<SkoczekEntity.Frame> frames = new ArrayList<>();
        float yaw = hill.facing.asRotation();
        sim.begin();
        for (int i = 0; i < 20; i++) frames.add(frame(hill, sim, yaw, 0));
        int guard = 0;
        while (!sim.isFinished() && guard++ < 1400) {
            ap.control(sim, 0.05, in);
            sim.step(0.05, in);
            int phase = switch (sim.phase) {
                case SEATED -> 0;
                case INRUN -> 1;
                case FLIGHT -> 2;
                default -> 3;
            };
            frames.add(frame(hill, sim, yaw, phase));
        }
        JumpResult r;
        if (sim.landed) {
            r = Scoring.evaluate(sim, refGate, seed);
        } else {
            r = new JumpResult();
            r.fell = true;
            r.fallReason = "off_course";
            r.gate = sim.gate;
        }
        return new NpcJump(frames, r);
    }

    private static SkoczekEntity.Frame frame(HillRecord hill, JumpSimulation sim, float yaw, int phase) {
        SkoczekEntity.Frame f = new SkoczekEntity.Frame();
        double x, y, z;
        switch (sim.phase) {
            case SEATED, INRUN -> { x = sim.px; y = hill.geometry().inrunY(sim.px); z = 0; }
            default -> { x = sim.x; y = sim.y; z = sim.z; }
        }
        Vec3d p = hill.toWorld(x, y, z);
        f.x = p.x;
        f.y = p.y;
        f.z = p.z;
        f.yaw = yaw;
        f.phase = phase;
        f.pitch = (float) sim.pitch;
        f.roll = (float) sim.roll;
        f.v = (float) sim.vOpen;
        f.tumble = (float) sim.tumble;
        f.telemark = sim.telemarkPressed;
        f.crouch = phase == 1;
        return f;
    }
}
