package dev.skijump.physics;

import java.util.Arrays;
import java.util.Random;

/** Punktacja: punkty za odległość, noty sędziów, rekompensata za wiatr i belkę. */
public final class Scoring {
    private Scoring() { }

    /** Punkty za metr zależne od wielkości skoczni (wg K). */
    public static double metersValue(double k) {
        if (k < 25) return 4.8;
        if (k < 30) return 4.4;
        if (k < 35) return 4.0;
        if (k < 40) return 3.6;
        if (k < 50) return 3.2;
        if (k < 60) return 2.8;
        if (k < 70) return 2.4;
        if (k < 80) return 2.2;
        if (k < 100) return 2.0;
        if (k < 170) return 1.8;
        return 1.2;
    }

    public static double distancePoints(double dist, double k) {
        double d = Math.floor(dist * 2.0 + 1e-9) / 2.0;   // rozdzielczość 0.5 m
        return Math.max(0, 60.0 + (d - k) * metersValue(k));
    }

    /** Wiatr w twarz odejmuje punkty, wiatr w plecy dodaje (head &gt; 0 = w twarz). */
    public static double windPoints(double avgHead, double k) {
        if (avgHead >= 0) return -avgHead * k * 0.055;
        return -avgHead * k * 0.070;
    }

    /** Rekompensata za belkę względem belki odniesienia (wyższa belka = mniej punktów). */
    public static double gatePoints(int gate, int refGate, double k) {
        return (refGate - gate) * k * 0.043;
    }

    /** Pełna ocena skoku po jego zakończeniu. Ziarno wpływa tylko na drobne różnice not sędziów. */
    public static JumpResult evaluate(JumpSimulation s, int refGate, long seed) {
        JumpResult r = new JumpResult();
        double k = s.hill.k;
        r.distance = Math.round(s.distance * 2.0) / 2.0;
        r.distancePoints = distancePoints(s.distance, k);
        r.avgHead = s.avgHead();
        r.windPoints = windPoints(r.avgHead, k);
        r.gate = s.gate;
        r.gatePoints = gatePoints(s.gate, refGate, k);
        r.takeoffSpeed = s.takeoffSpeed;
        r.takeoffErrMs = s.jumpPressed ? s.jumpErrMs : 250;
        r.perfectTakeoff = s.perfectTakeoff;
        r.telemarkQuality = s.telemarkQuality;
        r.impactSpeed = s.impactSpeed;
        r.fell = s.fell;
        r.fallReason = s.fallReason;

        double deduct = 0;
        // lot: odchylenie kąta natarcia od optimum i przechyły
        deduct += Math.min(4.0, 0.14 * s.meanAlphaDev() + 0.25 * s.meanAbsRoll() + 0.05 * s.maxAbsRoll);
        if (s.stallTime > 0.3) deduct += Math.min(1.5, s.stallTime * 0.6);
        // styl V
        double vf = s.vFraction();
        if (vf < 0.6) deduct += 2.5 * (1.0 - vf / 0.6);
        // telemark
        double tq = s.telemarkQuality;
        if (!s.telemarkPressed) deduct += 4.0;
        else if (tq > 0.85) deduct += 0.0;
        else if (tq > 0.6) deduct += 0.7;
        else if (tq > 0.3) deduct += 1.8;
        else deduct += 3.0;
        // równowaga przy lądowaniu i znoszenie
        deduct += 2.0 * (1.0 - s.balanceQuality);
        deduct += Math.min(1.0, Math.abs(s.landingZ) * 0.06);
        double base = 20.0 - deduct;
        if (s.fell) base = Math.min(base, 9.0) - 1.0;

        Random rng = new Random(seed * 31L + 7L);
        double[] marks = new double[5];
        for (int i = 0; i < 5; i++) {
            double m = base + rng.nextGaussian() * 0.35;
            m = Math.round(m * 2.0) / 2.0;
            marks[i] = Math.max(0, Math.min(20, m));
        }
        r.judgeMarks = marks.clone();
        double[] sorted = marks.clone();
        Arrays.sort(sorted);
        r.styleTotal = sorted[1] + sorted[2] + sorted[3];
        r.total = Math.max(0, r.distancePoints + r.styleTotal + r.windPoints + r.gatePoints);
        if (s.fell) r.total = Math.max(0, r.total - 6.0);
        return r;
    }
}
