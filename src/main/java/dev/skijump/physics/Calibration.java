package dev.skijump.physics;

import java.util.ArrayList;

/** Symulacja referencyjna używana do ułożenia profilu zeskoku zgodnie z fizyką lotu. */
final class Calibration {
    private Calibration() { }

    /** Zwraca {x[], y[]} toru lotu idealnego skoku do cięciwy ok. 1.2 * chordStar albo null. */
    static double[][] reference(HillGeometry stub, double chordStar) {
        WindField wf = new WindField(1L);
        Terrain none = (x, yHint, z) -> Double.NaN;
        JumpSimulation sim = new JumpSimulation(stub, none, wf, GearStats.STANDARD, JumperStats.AVERAGE,
                HillGeometry.DEFAULT_GATE, HillGeometry.MAX_GATE, 1.0);
        AutoPilot ap = AutoPilot.perfect();
        ControlInput in = new ControlInput();
        sim.begin();
        ArrayList<double[]> pts = new ArrayList<>();
        pts.add(new double[]{0, 0});
        double dt = 0.05;
        for (int i = 0; i < 1200 && !sim.isFinished(); i++) {
            ap.control(sim, dt, in);
            sim.step(dt, in);
            if (sim.phase == JumpSimulation.Phase.FLIGHT) {
                pts.add(new double[]{sim.x, sim.y});
                if (Math.hypot(sim.x, sim.y) > chordStar * 1.25) break;
            }
        }
        if (pts.size() < 10) return null;
        double[] xs = new double[pts.size()], ys = new double[pts.size()];
        for (int i = 0; i < xs.length; i++) { xs[i] = pts.get(i)[0]; ys[i] = pts.get(i)[1]; }
        return new double[][]{xs, ys};
    }
}
