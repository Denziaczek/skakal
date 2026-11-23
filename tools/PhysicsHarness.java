import dev.skijump.physics.*;
import java.util.Locale;

public class PhysicsHarness {
    static Terrain analytic(HillGeometry h) {
        return (x, yHint, z) -> h.landingY(x);
    }

    static JumpSimulation run(HillGeometry h, int gate, double skill, long seed, double head, double side, double gust,
                              Double forceErr, GearStats gear, JumperStats st, boolean trace) {
        WindField wf = new WindField(seed);
        wf.baseHead = head; wf.baseSide = side; wf.gust = gust;
        JumpSimulation sim = new JumpSimulation(h, analytic(h), wf, gear, st, gate, HillGeometry.MAX_GATE, 1.0);
        AutoPilot ap = new AutoPilot(skill, seed); ap.manualJump = forceErr != null;
        ControlInput in = new ControlInput();
        sim.begin();
        double dt = 0.05;
        int tick = 0;
        while (!sim.isFinished() && tick < 2000) {
            if (forceErr != null && sim.armed() && -sim.msToIdeal() >= forceErr - 30.0) sim.pressJump(forceErr);
            ap.control(sim, dt, in);
            sim.step(dt, in);
            if (trace && sim.phase == JumpSimulation.Phase.FLIGHT && tick % 4 == 0)
                System.out.printf(Locale.US, "t=%.2f x=%.1f y=%.1f vx=%.1f vy=%.1f alpha=%.1f clr=%.1f%n", sim.flightTime, sim.x, sim.y, sim.vx, sim.vy, sim.alpha, sim.clearance);
            tick++;
        }
        return sim;
    }

    public static void main(String[] a) {
        double[][] hills = {{40, 45}, {65, 72}, {90, 100}, {125, 140}, {200, 240}};
        for (double[] kh : hills) {
            HillGeometry h = HillGeometry.forK(kh[0], kh[1]);
            System.out.printf(Locale.US, "== K%.0f HS%.0f  inrun D=%.1f H=%.1f xK=%.1f%n", kh[0], kh[1], h.inrunD, h.inrunH, h.xK());
            for (int gate : new int[]{6, 12, 18, 24}) {
                JumpSimulation s = run(h, gate, 1.0, 5, 0, 0, 0, 0.0, GearStats.STANDARD, JumperStats.AVERAGE, false);
                System.out.printf(Locale.US, " gate %2d: v_edge=%.1f m/s dist=%.1f fell=%b imp=%.1f tele=%.2f ft=%.2f%n", gate, s.takeoffSpeed, s.distance, s.fell, s.impactSpeed, s.telemarkQuality, s.flightTime);
            }
        }
    }
}
