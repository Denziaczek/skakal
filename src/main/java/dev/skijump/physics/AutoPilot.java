package dev.skijump.physics;

import java.util.Random;

/**
 * Sterownik komputerowego zawodnika (NPC "Skoczek") oraz narzędzie do kalibracji fizyki.
 * Umiejętność 0..1: im wyższa, tym mniejszy błąd timingu, stabilniejsze sterowanie i lepszy telemark.
 */
public final class AutoPilot {
    private final double skill;
    private final Random rng;
    private final double plannedJumpErr;
    private final double alphaBias;
    private final double vDelay;
    private final double telemarkLead;
    private double pitchFilter, rollFilter;
    /** gdy true, wybicie wykonuje kod zewnętrzny (testy) */
    public boolean manualJump;
    /** dodatek do docelowego kąta natarcia: &gt; 0 = ryzykant lecący blisko przeciągnięcia */
    public double riskBias;

    public AutoPilot(double skill, long seed) {
        this(skill, seed, false);
    }

    private AutoPilot(double skill, long seed, boolean perfect) {
        this.skill = Math.max(0, Math.min(1, skill));
        this.rng = new Random(seed);
        double sigma = 8.0 + (1.0 - this.skill) * 55.0;
        double err = rng.nextGaussian() * sigma + (1.0 - this.skill) * 12.0;
        double bias = rng.nextGaussian() * (1.0 - this.skill) * 4.0 - (1.0 - this.skill) * 2.0;
        double vd = 0.35 + rng.nextDouble() * (1.0 - this.skill) * 1.0;
        double tl = 0.28 + rng.nextGaussian() * (1.0 - this.skill) * 0.10;
        this.plannedJumpErr = perfect ? 0.0 : err;
        this.alphaBias = perfect ? 0.0 : bias;
        this.vDelay = perfect ? 0.35 : vd;
        this.telemarkLead = perfect ? 0.28 : tl;
    }

    /** Idealny pilot bez błędów (kalibracja skoczni). */
    public static AutoPilot perfect() {
        return new AutoPilot(1.0, 1L, true);
    }

    /** Wypełnia sterowanie na dany krok i generuje zdarzenia (wybicie, telemark). */
    public void control(JumpSimulation sim, double dt, ControlInput in) {
        in.clear();
        switch (sim.phase) {
            case INRUN -> {
                in.crouch = true;
                if (!manualJump && sim.armed() && -sim.msToIdeal() >= plannedJumpErr - 30.0) sim.pressJump(plannedJumpErr);
            }
            case FLIGHT -> {
                double target = JumpSimulation.ALPHA_OPT - 1.5 + alphaBias + riskBias;
                double noise = rng.nextGaussian() * (1.0 - skill) * 0.25;
                double up = 0.055 * (sim.alpha - target) + 0.010 * sim.pitchRate + noise;
                pitchFilter += (up - pitchFilter) * Math.min(1.0, dt * 8.0);
                in.pitch = Math.max(-1, Math.min(1, pitchFilter));
                double ur = -(0.06 * sim.roll + 0.018 * sim.rollRate) + rng.nextGaussian() * (1.0 - skill) * 0.15;
                rollFilter += (ur - rollFilter) * Math.min(1.0, dt * 8.0);
                in.roll = Math.max(-1, Math.min(1, rollFilter));
                in.vOpen = sim.flightTime > vDelay;
                if (!sim.telemarkPressed && sim.timeToContact() <= telemarkLead) sim.pressTelemark();
            }
            default -> { }
        }
    }
}
