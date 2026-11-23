package dev.skijump.physics;

/**
 * Geometria skoczni w układzie lokalnym: x - wzdłuż osi skoczni (do przodu), y - w górę, z - w prawo.
 * Początek układu (0,0) leży na KRAWĘDZI PROGU. Rozbieg to x &lt; 0, zeskok to x &gt; 0.
 * Wszystkie wymiary w metrach "wirtualnych" (skala świata jest nakładana poza fizyką).
 */
public final class HillGeometry {
    public static final double INRUN_ANGLE = Math.toRadians(35.0);
    public static final double TABLE_ANGLE = Math.toRadians(10.5);
    public static final double LAND_MAX_ANGLE = Math.toRadians(36.5);
    public static final int MAX_GATE = 24;

    public final double k;
    public final double hs;
    /** odległość pozioma od szczytu rozbiegu (belka najwyższa) do krawędzi progu */
    public final double inrunD;
    /** długość stołu progu (poziomo) */
    public final double tableLen;
    /** różnica wysokości szczyt rozbiegu - krawędź */
    public final double inrunH;
    public final double halfWidth;
    public final boolean analyticLanding;

    private final double hermY0, hermY1, hermL;
    /** Siatka zeskoku o stałym kroku DX: lg[i] = wysokość powierzchni dla x = i * DX. */
    private double[] lg;
    private static final double DX = 0.25;
    public static final int DEFAULT_GATE = 12;

    private HillGeometry(double k, double hs, double d, double h, double[] trajX, double[] trajY) {
        this.k = k;
        this.hs = hs;
        this.inrunD = d;
        this.tableLen = Math.max(3.0, 0.065 * k);
        this.inrunH = h;
        this.halfWidth = 0.065 * k + 6.0;
        this.analyticLanding = trajX != null;
        this.hermL = d - tableLen;
        this.hermY0 = h;
        this.hermY1 = tableLen * Math.tan(TABLE_ANGLE);
        if (trajX != null) buildLanding(trajX, trajY);
    }

    private static double inrunDFor(double k) { return 0.78 * k + 8.0; }

    private static double inrunHFor(double k, double d) {
        double tl = Math.max(3.0, 0.065 * k);
        return 0.50 * (d - tl) + tl * Math.tan(TABLE_ANGLE);
    }

    /**
     * Skocznia typowa. Zeskok jest KALIBROWANY na podstawie symulacji referencyjnej (idealny skok,
     * środkowa belka, brak wiatru, sprzęt standardowy): referencyjny skok ląduje w połowie między K a HS,
     * a profil zeskoku biegnie tuż pod jego torem lotu. Dzięki temu skocznia pasuje do fizyki dla każdego K.
     */
    public static HillGeometry forK(double k, double hs) {
        double d = inrunDFor(k);
        double h = inrunHFor(k, d);
        HillGeometry stub = new HillGeometry(k, hs, d, h, null, null);
        double[][] traj = Calibration.reference(stub, 0.5 * (k + hs));
        if (traj == null) return new HillGeometry(k, hs, d, h, new double[]{0, 1}, new double[]{0, -0.5});
        return new HillGeometry(k, hs, d, h, traj[0], traj[1]);
    }

    /** Skocznia zdefiniowana przez gracza różdżką (zeskok nieznany - czytany ze świata). */
    public static HillGeometry custom(double d, double h, double k, double hs) {
        return new HillGeometry(k, hs, Math.max(d, 12), Math.max(h, 4), null, null);
    }

    // ---------------- rozbieg ----------------

    private double t(double x) {
        return (x + inrunD) / hermL;
    }

    /** Wysokość toru najazdowego dla x z przedziału [-inrunD, 0]. */
    public double inrunY(double x) {
        if (x >= 0) return -x * Math.tan(TABLE_ANGLE);
        if (x >= -tableLen) return -x * Math.tan(TABLE_ANGLE);
        if (x <= -inrunD) return hermY0 - Math.tan(INRUN_ANGLE) * (x + inrunD);
        double t = t(x), t2 = t * t, t3 = t2 * t;
        double m0 = -Math.tan(INRUN_ANGLE), m1 = -Math.tan(TABLE_ANGLE);
        double h00 = 2 * t3 - 3 * t2 + 1, h10 = t3 - 2 * t2 + t, h01 = -2 * t3 + 3 * t2, h11 = t3 - t2;
        return h00 * hermY0 + h10 * hermL * m0 + h01 * hermY1 + h11 * hermL * m1;
    }

    /** dy/dx toru (ujemne = w dół). */
    public double inrunSlope(double x) {
        if (x >= -tableLen) return -Math.tan(TABLE_ANGLE);
        if (x <= -inrunD) return -Math.tan(INRUN_ANGLE);
        double t = t(x), t2 = t * t;
        double m0 = -Math.tan(INRUN_ANGLE), m1 = -Math.tan(TABLE_ANGLE);
        double dh00 = 6 * t2 - 6 * t, dh10 = 3 * t2 - 4 * t + 1, dh01 = -6 * t2 + 6 * t, dh11 = 3 * t2 - 2 * t;
        return (dh00 * hermY0 + dh10 * hermL * m0 + dh01 * hermY1 + dh11 * hermL * m1) / hermL;
    }

    /** Krzywizna toru (&gt;0 = wklęsły do góry, zwiększa nacisk). */
    public double inrunCurvature(double x) {
        if (x >= -tableLen || x <= -inrunD) return 0;
        double t = t(x);
        double m0 = -Math.tan(INRUN_ANGLE), m1 = -Math.tan(TABLE_ANGLE);
        double d2h00 = 12 * t - 6, d2h10 = 6 * t - 4, d2h01 = -12 * t + 6, d2h11 = 6 * t - 2;
        double ypp = (d2h00 * hermY0 + d2h10 * hermL * m0 + d2h01 * hermY1 + d2h11 * hermL * m1) / (hermL * hermL);
        double yp = inrunSlope(x);
        return ypp / Math.pow(1 + yp * yp, 1.5);
    }

    /** Krok belki startowej (poziomo) - ok. 0.66 m różnicy wysokości na belkę. */
    public double gateStepX() {
        return 1.3 * (inrunD / 105.0);
    }

    public double startX(int gate, int maxGate) {
        int g = Math.max(1, Math.min(maxGate, gate));
        return -inrunD + (maxGate - g) * gateStepX();
    }

    // ---------------- zeskok (kalibrowany) ----------------

    private static double interp(double[] xs, double[] ys, double x) {
        if (x <= xs[0]) return ys[0];
        int n = xs.length;
        if (x >= xs[n - 1]) return ys[n - 1];
        int lo = 0, hi = n - 1;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (xs[mid] <= x) lo = mid; else hi = mid;
        }
        double f = (x - xs[lo]) / Math.max(1e-9, xs[hi] - xs[lo]);
        return ys[lo] + f * (ys[hi] - ys[lo]);
    }

    private void buildLanding(double[] tx, double[] ty) {
        // punkt lądowania referencyjnego skoku (cięciwa = (K+HS)/2)
        double chord = 0.5 * (k + hs);
        double xs = tx[tx.length - 1];
        for (int i = 1; i < tx.length; i++) {
            if (Math.hypot(tx[i], ty[i]) >= chord) { xs = tx[i]; break; }
        }
        double c0 = 0.03 * k;            // wysokość progu nad początkiem zeskoku
        double cm = 0.077 * xs;          // maksymalny odstęp zbocza od toru idealnego skoku (ok. 45% drogi)
        double dSlope = cm / (0.55 * xs);// o tyle zeskok jest łagodniejszy od toru lotu tuż przed lądowaniem
        int nMain = (int) Math.ceil(xs / DX) + 1;
        double[] g = new double[nMain];
        for (int i = 0; i < nMain; i++) {
            double x = i * DX;
            double u = Math.min(1.0, x / xs);
            double c = u < 0.45 ? c0 + (cm - c0) * (u / 0.45) : cm * (1.0 - u) / 0.55;
            g[i] = interp(tx, ty, x) - c;
        }
        // nachylenie w punkcie lądowania
        double y1 = interp(tx, ty, xs - 1.0), y2 = interp(tx, ty, xs + 1.0);
        double slopeAtLand = (y2 - y1) / 2.0 + dSlope;
        double th = Math.atan(-slopeAtLand);
        th = Math.max(Math.toRadians(8), Math.min(LAND_MAX_ANGLE + 0.05, th));
        double sT = 0.55 * k * (th / LAND_MAX_ANGLE);
        double outrun = 0.95 * k;
        // ogon: łuk wypłaszczający + wybieg
        java.util.ArrayList<double[]> tail = new java.util.ArrayList<>();
        double x = xs, y = g[nMain - 1], s = 0, ds = 0.1;
        double totalS = sT + outrun;
        while (s <= totalS) {
            tail.add(new double[]{x, y});
            double ang = s < sT ? th * (1.0 - s / sT) : 0.0;
            x += Math.cos(ang) * ds;
            y -= Math.sin(ang) * ds;
            s += ds;
        }
        double xEnd = tail.get(tail.size() - 1)[0];
        int nTotal = (int) Math.ceil(xEnd / DX) + 1;
        lg = new double[nTotal];
        for (int i = 0; i < nTotal; i++) {
            double xx = i * DX;
            if (i < nMain) { lg[i] = g[i]; continue; }
            // wyszukanie w ogonie
            double yy = tail.get(tail.size() - 1)[1];
            for (int j = 1; j < tail.size(); j++) {
                if (tail.get(j)[0] >= xx) {
                    double[] a = tail.get(j - 1), b = tail.get(j);
                    double f = (xx - a[0]) / Math.max(1e-9, b[0] - a[0]);
                    yy = a[1] + f * (b[1] - a[1]);
                    break;
                }
            }
            lg[i] = yy;
        }
    }

    /** Wysokość zeskoku dla x &gt;= 0. */
    public double landingY(double x) {
        if (!analyticLanding) return Double.NaN;
        if (x <= 0) return -x * Math.tan(TABLE_ANGLE);
        double fi = x / DX;
        int i = (int) fi;
        if (i >= lg.length - 1) return lg[lg.length - 1];
        double f = fi - i;
        return lg[i] + f * (lg[i + 1] - lg[i]);
    }

    /** Kąt nachylenia zeskoku (rad, dodatni = w dół) dla x. */
    public double landingAngle(double x) {
        if (!analyticLanding || x <= 0) return TABLE_ANGLE;
        double a = landingY(x - 1.5), b = landingY(x + 1.5);
        return Math.atan2(a - b, 3.0);
    }

    public double outrunEnd() {
        return analyticLanding ? (lg.length - 1) * DX : 2.4 * k;
    }

    /** x punktu zeskoku, w którym odległość od krawędzi (cięciwa) równa się chord. */
    public double xAtChord(double chord) {
        if (!analyticLanding) return chord * 0.85;
        for (int i = 1; i < lg.length; i++) {
            if (Math.hypot(i * DX, lg[i]) >= chord) return i * DX;
        }
        return (lg.length - 1) * DX;
    }

    public double xK() { return xAtChord(k); }

    public double xHs() { return xAtChord(hs); }
}
