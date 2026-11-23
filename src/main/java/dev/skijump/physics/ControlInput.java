package dev.skijump.physics;

/** Sterowanie zawodnika w jednym kroku symulacji. */
public final class ControlInput {
    /** -1..1; dodatnie = pochylenie do przodu (mniejszy kąt natarcia). */
    public double pitch;
    /** -1..1; dodatnie = przechył w prawo. */
    public double roll;
    /** pozycja najazdowa (sneak) */
    public boolean crouch;
    /** narty w układzie V */
    public boolean vOpen;

    public void clear() {
        pitch = 0;
        roll = 0;
        crouch = false;
        vOpen = false;
    }
}
