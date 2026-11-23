package dev.skijump.physics;

/** Statystyki zawodnika w skali 0..1 (w grze 0..100). */
public final class JumperStats {
    public final double strength;  // siła wybicia
    public final double balance;   // balans
    public final double landing;   // technika lądowania

    public JumperStats(double strength, double balance, double landing) {
        this.strength = clamp01(strength);
        this.balance = clamp01(balance);
        this.landing = clamp01(landing);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

    public static final JumperStats AVERAGE = new JumperStats(0.5, 0.5, 0.5);
}
