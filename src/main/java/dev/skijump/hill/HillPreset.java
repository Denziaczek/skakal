package dev.skijump.hill;

import dev.skijump.physics.HillGeometry;

import java.util.Arrays;
import java.util.List;

/** Preset skoczni wzorowany na prawdziwej (wymiary orientacyjne). Nazwa w tłumaczeniach: skijump.hill.&lt;id&gt;. */
public final class HillPreset {
    public final String id;
    public final double k;
    public final double hs;
    /** ranga wymagana do odblokowania (0..5) */
    public final int rank;
    private HillGeometry geometry;

    private HillPreset(String id, double k, double hs, int rank) {
        this.id = id;
        this.k = k;
        this.hs = hs;
        this.rank = rank;
    }

    public synchronized HillGeometry geometry() {
        if (geometry == null) geometry = HillGeometry.forK(k, hs);
        return geometry;
    }

    public static final List<HillPreset> ALL = Arrays.asList(
            new HillPreset("training_k40", 40, 45, 0),
            new HillPreset("training_k65", 65, 72, 0),
            new HillPreset("normal_k90", 90, 100, 1),
            new HillPreset("wisla", 120, 134, 2),
            new HillPreset("oslo", 120, 134, 2),
            new HillPreset("innsbruck", 120, 128, 2),
            new HillPreset("oberstdorf", 120, 137, 3),
            new HillPreset("zakopane", 125, 140, 3),
            new HillPreset("garmisch", 125, 142, 3),
            new HillPreset("bischofshofen", 125, 142, 3),
            new HillPreset("willingen", 130, 147, 4),
            new HillPreset("planica", 200, 240, 5),
            new HillPreset("vikersund", 200, 240, 5)
    );

    public static HillPreset find(String id) {
        for (HillPreset p : ALL) if (p.id.equalsIgnoreCase(id)) return p;
        return null;
    }

    /** Presety małe/średnie używane do losowej generacji w świecie. */
    public static HillPreset random(java.util.Random r) {
        HillPreset[] small = {find("training_k40"), find("training_k65"), find("normal_k90")};
        return small[r.nextInt(small.length)];
    }
}
