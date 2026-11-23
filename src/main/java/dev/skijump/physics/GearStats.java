package dev.skijump.physics;

/** Zsumowane parametry sprzętu (0..1). */
public final class GearStats {
    public final double glide;       // ślizg nart: mniejsze tarcie na rozbiegu
    public final double skiStab;     // stabilność nart
    public final double skiLift;     // dodatkowa nośność nart
    public final double suitLift;    // nośność kombinezonu
    public final double suitDrag;    // redukcja oporu kombinezonu
    public final double suitStab;    // stabilność kombinezonu
    public final double helmet;      // ochrona kasku (0..1)

    public GearStats(double glide, double skiStab, double skiLift, double suitLift, double suitDrag, double suitStab, double helmet) {
        this.glide = glide;
        this.skiStab = skiStab;
        this.skiLift = skiLift;
        this.suitLift = suitLift;
        this.suitDrag = suitDrag;
        this.suitStab = suitStab;
        this.helmet = helmet;
    }

    public static final GearStats NONE = new GearStats(0, 0, 0, 0, 0, 0, 0);
    public static final GearStats STANDARD = new GearStats(0.35, 0.35, 0.3, 0.35, 0.3, 0.35, 0.0);
}
