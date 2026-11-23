package dev.skijump.physics;

/**
 * Deterministyczna symulacja jednego skoku (czysta Java, bez Minecrafta).
 *
 * Fazy: SEATED (belka) -> INRUN (rozbieg) -> FLIGHT (wybicie + lot) -> SLIDE (lądowanie, hamowanie) -> DONE.
 * Cała fizyka działa w "metrach wirtualnych" w układzie lokalnym skoczni (patrz {@link HillGeometry}).
 * Jedyna losowość pochodzi z {@link WindField} (ziarno wiatru), reszta jest zdeterminowana wejściami gracza.
 */
public final class JumpSimulation {
    public enum Phase { SEATED, INRUN, FLIGHT, SLIDE, DONE }

    // ---- stałe fizyczne ----
    public static final double G = 9.81;
    public static final double RHO = 1.2;
    public static final double MASS = 72.0;           // zawodnik + sprzęt
    public static final double ALPHA_TRIM = 20.0;     // naturalny kąt natarcia (stabilna równowaga)
    public static final double ALPHA_OPT = 27.0;      // kąt natarcia dający najlepszy ślizg
    public static final double ALPHA_STALL = 35.0;    // powyżej - utrata nośności
    public static final double IDEAL_LEAD = 0.12;     // idealny moment wybicia: 120 ms przed krawędzią
    public static final double EDGE_GRACE = 0.12;     // tyle czekamy na spóźnione wybicie za krawędzią (opóźnienie sieci)
    public static final double ARM_TIME = 1.80;       // od tylu sekund przed krawędzią przyjmujemy wybicie
    public static double AREA = 1.6, CL_MAX = 1.2, CD0 = 0.16, CD_K = 0.20;
    /** Wiatr w twarz odbity od zbocza daje składową wznoszącą (wiatr "pod narty"). */
    public static double SLOPE_UPDRAFT = 0.2;
    public static final double PITCH_CTRL = 30.0;     // deg/s^2 przy pełnym wychyleniu
    public static final double ROLL_CTRL = 34.0;

    public final HillGeometry hill;
    public final Terrain terrain;
    public final WindField wind;
    public final GearStats gear;
    public final JumperStats stats;
    public final int gate;
    public final int maxGate;
    public final double snowFactor;

    public Phase phase = Phase.SEATED;
    public double time;
    public double flightTime;
    public double slideTime;

    // rozbieg
    public double px;
    public double speed;
    public double edgeWait;
    public double crouchBlend;

    // lot (układ lokalny: x przód, y góra, z prawo)
    public double x, y, z, vx, vy, vz;
    public double pitch, pitchRate, roll, rollRate, vOpen;
    public double alpha;
    public double clearance = 99;
    public double tumble;

    // zdarzenia
    public boolean jumpPressed;
    public double jumpErrMs;
    public boolean telemarkPressed;
    public double telemarkFlightTime = -1;

    // wybicie
    public double takeoffSpeed;
    public double takeoffQuality;
    public boolean perfectTakeoff;

    // lądowanie
    public boolean landed;
    public boolean fell;
    public String fallReason = "";
    public double distance;
    public double impactSpeed;
    public double telemarkQuality;
    public double telemarkOffsetMs;
    public double balanceQuality;
    public double landingScore;
    public double landingX, landingZ;

    // statystyki lotu do oceny stylu
    public double sumAlpha, sumAlphaDev, sumAbsRoll, maxAbsRoll, vOpenTime, vWindowTime, sumHead, sumSide, stallTime;

    private final double[] w = new double[3];

    public JumpSimulation(HillGeometry hill, Terrain terrain, WindField wind, GearStats gear, JumperStats stats,
                          int gate, int maxGate, double snowFactor) {
        this.hill = hill;
        this.terrain = terrain;
        this.wind = wind;
        this.gear = gear;
        this.stats = stats;
        this.maxGate = maxGate;
        this.gate = Math.max(1, Math.min(maxGate, gate));
        this.snowFactor = snowFactor;
        this.px = hill.startX(this.gate, maxGate);
    }

    public void begin() {
        if (phase == Phase.SEATED) {
            phase = Phase.INRUN;
            speed = 0.3;
        }
    }

    // ------------------------------------------------------------------ zdarzenia

    /** Czas (s) do przejścia krawędzi progu przy aktualnej prędkości. */
    public double timeToEdge() {
        double beta = Math.atan(-hill.inrunSlope(px));
        double vh = Math.max(0.8, speed * Math.cos(beta));
        return -px / vh;
    }

    /** Ile ms zostało do idealnego momentu wybicia (ujemne = już po). */
    public double msToIdeal() {
        return (timeToEdge() - IDEAL_LEAD) * 1000.0;
    }

    public boolean armed() {
        return phase == Phase.INRUN && !jumpPressed && timeToEdge() < ARM_TIME;
    }

    /** Wybicie zgłoszone przez gracza; clientErrMs = błąd względem ideału wg klienta (dodatni = za późno). */
    public boolean pressJump(double clientErrMs) {
        if (!armed()) return false;
        double serverErr = -msToIdeal();
        double err = clamp(clientErrMs, serverErr - 160.0, serverErr + 30.0);
        jumpErrMs = clamp(err, -300.0, 300.0);
        jumpPressed = true;
        return true;
    }

    public boolean pressTelemark() {
        if (phase != Phase.FLIGHT || telemarkPressed || flightTime < 0.4) return false;
        telemarkPressed = true;
        telemarkFlightTime = flightTime;
        return true;
    }

    // ------------------------------------------------------------------ krok

    public void step(double dt, ControlInput in) {
        if (phase == Phase.SEATED || phase == Phase.DONE) return;
        time += dt;
        wind.sample(time, w);
        switch (phase) {
            case INRUN -> stepInrun(dt, in);
            case FLIGHT -> stepFlight(dt, in);
            case SLIDE -> stepSlide(dt);
            default -> { }
        }
    }

    private void stepInrun(double dt, ControlInput in) {
        int n = 4;
        double h = dt / n;
        for (int i = 0; i < n; i++) {
            crouchBlend += ((in.crouch ? 1.0 : 0.0) - crouchBlend) * Math.min(1.0, 4.0 * h);
            double slope = hill.inrunSlope(px);
            double beta = Math.atan(-slope);
            double sinB = Math.sin(beta), cosB = Math.cos(beta);
            double kappa = Math.max(0, hill.inrunCurvature(px));
            double nOverM = G * cosB + speed * speed * kappa;
            double mu = lerp(0.050, 0.018, gear.glide) * snowFactor;
            double cda = lerp(0.55, 0.24, crouchBlend) * (1.0 - 0.10 * gear.suitDrag);
            double vrel = speed + w[0] * cosB;
            double aDrag = 0.5 * RHO * cda * vrel * Math.abs(vrel) / MASS;
            double a = G * sinB - mu * nOverM - aDrag;
            speed = Math.max(0.0, speed + a * h);
            px += speed * cosB * h;
            if (px >= 0) {
                if (!jumpPressed && edgeWait < EDGE_GRACE) {
                    edgeWait += h;      // czekamy chwilę na spóźnione wybicie (tor "przedłużony" za krawędź)
                    continue;
                }
                takeoff();
                return;
            }
        }
    }

    /** Wybicie: błąd czasowy -> prędkość pionowa, kąt natarcia i obrót początkowy. */
    private void takeoff() {
        double err = jumpPressed ? jumpErrMs : 250.0;
        double q = Math.exp(-(err / 110.0) * (err / 110.0));
        perfectTakeoff = jumpPressed && Math.abs(err) <= 12.0;
        takeoffQuality = q;
        double vNmax = 2.5 + 1.0 * stats.strength;
        double vN, a0, rate0;
        if (err < 0) {                       // za wcześnie: niski, płaski skok, obrót "do przodu"
            vN = vNmax * (0.45 + 0.55 * q);
            a0 = 20.0 - 22.0 * (1.0 - q);
            rate0 = -15.0 * (1.0 - q);
        } else {                              // za późno: wysoki, krótki skok, nadmierny kąt natarcia
            double late = Math.min(1.0, err / 90.0);
            vN = vNmax * (0.70 + 0.30 * q) + 1.4 * (1.0 - q) * late;
            a0 = 20.0 + 22.0 * (1.0 - q);
            rate0 = 15.0 * (1.0 - q);
        }
        if (perfectTakeoff) vN += 0.25;
        // słaby timing = gorsze wyprostowanie sylwetki = utrata prędkości (idealne wybicie daje lekki bonus)
        speed *= 1.0 - 0.07 * (1.0 - q);
        if (perfectTakeoff) speed += 0.15;
        double beta = HillGeometry.TABLE_ANGLE;
        double tx = Math.cos(beta), ty = -Math.sin(beta);
        double nx = Math.sin(beta), ny = Math.cos(beta);
        takeoffSpeed = speed;
        x = px;
        y = hill.inrunY(px);
        z = 0;
        vx = speed * tx + vN * nx;
        vy = speed * ty + vN * ny;
        vz = 0;
        double gamma0 = Math.toDegrees(Math.atan2(vy, vx));
        pitch = gamma0 + a0;
        pitchRate = rate0;
        roll = 0;
        rollRate = 0;
        flightTime = 0;
        phase = Phase.FLIGHT;
    }

    // ------------------------------------------------------------------ lot

    private void stepFlight(double dt, ControlInput in) {
        int n = 2;
        double h = dt / n;
        for (int i = 0; i < n && phase == Phase.FLIGHT; i++) flightSub(h, in);
    }

    private double stability() {
        double s = 0.40 * stats.balance + 0.20 * gear.skiStab + 0.25 * gear.suitStab + 0.075;
        return clamp(s, 0, 1);
    }

    private void flightSub(double h, ControlInput in) {
        flightTime += h;
        double head = w[0], side = w[1], up = w[2];

        double vTarget = in.vOpen ? 1.0 : 0.0;
        double rate = (vTarget > vOpen ? 2.2 : 3.0) * h;
        vOpen += clamp(vTarget - vOpen, -rate, rate);

        double slopeF = clamp((40.0 - clearance) / 25.0, 0, 1);
        double upEff = up + SLOPE_UPDRAFT * head * slopeF;
        double rx = vx + head, ry = vy - upEff, rz = vz - side;
        double vv = Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (vv < 1) vv = 1;
        double hx = rx / vv, hy = ry / vv, hz = rz / vv;
        double gamma = Math.toDegrees(Math.atan2(ry, Math.hypot(rx, rz)));
        alpha = pitch - gamma;

        double cl = liftCoeff(alpha);
        double cd = dragCoeff(alpha, cl);
        double q = 0.5 * RHO * vv * vv;
        double area = AREA * (1.0 + 0.22 * vOpen);
        double lift = q * area * cl, drag = q * area * cd;

        double lx0 = -hy * hx, ly0 = 1 - hy * hy, lz0 = -hy * hz;
        double ln = Math.sqrt(lx0 * lx0 + ly0 * ly0 + lz0 * lz0);
        if (ln < 1e-6) { lx0 = 0; ly0 = 1; lz0 = 0; } else { lx0 /= ln; ly0 /= ln; lz0 /= ln; }
        double rtx = hy * lz0 - hz * ly0, rty = hz * lx0 - hx * lz0, rtz = hx * ly0 - hy * lx0;
        double rr = Math.toRadians(roll);
        double cr = Math.cos(rr), sr = Math.sin(rr);
        double lxx = cr * lx0 + sr * rtx, lyy = cr * ly0 + sr * rty, lzz = cr * lz0 + sr * rtz;

        double ax = (lift * lxx - drag * hx) / MASS;
        double ay = (lift * lyy - drag * hy) / MASS - G;
        double az = (lift * lzz - drag * hz) / MASS;

        // turbulencje przy zboczu: im niżej nad zboczem, tym mocniejsze
        double turb = 1.0 + 1.5 * clamp(1.0 - clearance / 10.0, 0, 1);
        double stab = stability();
        double stabR = clamp(stab - 0.18 * vOpen * (1.0 - 0.4 * stab), 0, 1);
        double dUp = up - wind.baseUp, dHead = head - wind.baseHead, dSide = side - wind.baseSide;
        double kP = lerp(-1.0, 2.2, stab);
        double kR = lerp(-0.6, 2.0, stabR);
        double dist = (1.25 - 0.6 * stab) * turb;

        double pAcc = -kP * (alpha - ALPHA_TRIM) - 1.4 * pitchRate - in.pitch * PITCH_CTRL + (dUp * 3.0 + dHead * 1.0) * dist;
        double rAcc = -kR * roll - 1.2 * rollRate + in.roll * ROLL_CTRL + (dSide * 2.5 + dUp * 1.0) * dist + 0.9 * side;

        pitchRate += pAcc * h;
        pitch = clamp(pitch + pitchRate * h, -25, 75);
        rollRate += rAcc * h;
        roll = clamp(roll + rollRate * h, -60, 60);

        vx += ax * h;
        vy += ay * h;
        vz += az * h;
        x += vx * h;
        y += vy * h;
        z += vz * h;

        sumAlpha += alpha * h;
        sumAlphaDev += Math.abs(alpha - ALPHA_OPT) * h;
        sumAbsRoll += Math.abs(roll) * h;
        maxAbsRoll = Math.max(maxAbsRoll, Math.abs(roll));
        sumHead += head * h;
        sumSide += side * h;
        if (alpha > ALPHA_STALL) stallTime += h;
        if (flightTime > 0.8) {
            vWindowTime += h;
            if (vOpen > 0.8) vOpenTime += h;
        }

        double g = terrain.groundY(x, y, z);
        if (!Double.isNaN(g)) {
            clearance = y - g;
            if (flightTime > 0.25 && y <= g) touchdown(g);
        } else {
            clearance = 99;
            if (flightTime > 14.0) finish();
        }
    }

    private double liftCoeff(double a) {
        double clMax = CL_MAX * (1.0 + 0.10 * gear.suitLift + 0.08 * gear.skiLift);
        if (a <= 0) return Math.max(-0.4, 0.02 * a);
        if (a <= ALPHA_STALL) return clMax * Math.sin(Math.PI / 2.0 * a / ALPHA_STALL);
        return clMax * (0.55 + 0.45 * Math.exp(-(a - ALPHA_STALL) / 10.0));
    }

    private double dragCoeff(double a, double cl) {
        double cd0 = (CD0 - 0.05 * gear.suitDrag) * (1.0 + 0.04 * vOpen);
        double cd = cd0 + CD_K * cl * cl;
        if (a > ALPHA_STALL) cd += 0.025 * (a - ALPHA_STALL);
        if (a < 0) cd += 0.01 * (-a);
        return cd;
    }

    /** Przybliżony czas do zetknięcia z zboczem (s); duża liczba, gdy się oddalamy. */
    public double timeToContact() {
        if (phase != Phase.FLIGHT || Double.isNaN(clearance) || clearance > 60) return 99;
        double g1 = terrain.groundY(x + 2.0, y, z);
        double g0 = terrain.groundY(x, y, z);
        if (Double.isNaN(g0) || Double.isNaN(g1)) return 99;
        double groundVy = (g1 - g0) / 2.0 * vx;
        double closing = groundVy - vy;
        if (closing < 0.5) return 99;
        return Math.max(0, clearance / closing);
    }

    // ------------------------------------------------------------------ lądowanie

    private double slopeAngle(double xx) {
        double a = terrain.groundY(xx - 1.5, y + 2, z);
        double b = terrain.groundY(xx + 1.5, y + 2, z);
        if (Double.isNaN(a) || Double.isNaN(b)) return hill.landingAngle(xx);
        return Math.atan2(a - b, 3.0);
    }

    private void touchdown(double ground) {
        y = ground;
        landed = true;
        landingX = x;
        landingZ = z;
        distance = Math.hypot(x, y);
        double th = slopeAngle(x);
        double tsx = Math.cos(th), tsy = -Math.sin(th);
        double nsx = Math.sin(th), nsy = Math.cos(th);
        double vn = vx * nsx + vy * nsy;
        double vt = vx * tsx + vy * tsy;
        impactSpeed = Math.max(0, -vn);

        if (telemarkPressed) {
            double offMs = (flightTime - telemarkFlightTime) * 1000.0;
            telemarkOffsetMs = offMs;
            telemarkQuality = Math.exp(-Math.pow((offMs - 280.0) / 130.0, 2));
        } else {
            telemarkQuality = 0;
        }
        double balDev = Math.abs(roll) / 20.0 + Math.abs(alpha - 20.0) / 38.0 + Math.abs(z) / hill.halfWidth * 0.4;
        balanceQuality = clamp(1.0 - balDev, 0, 1);
        double imp = clamp(impactSpeed / 13.0, 0, 1);
        landingScore = 0.38 * telemarkQuality + 0.24 * balanceQuality + 0.18 * (1.0 - imp) + 0.20 * stats.landing;
        if (distance > hill.hs) landingScore -= (distance - hill.hs) * 0.02;

        fell = false;
        if (Math.abs(z) > hill.halfWidth) { fell = true; fallReason = "off_course"; }
        else if (impactSpeed > 14.5) { fell = true; fallReason = "hard_impact"; }
        else if (landingScore < 0.34) { fell = true; fallReason = telemarkPressed ? "unbalanced" : "no_telemark"; }

        double s = Math.max(0.0, vt * (1.0 - 0.015 * impactSpeed));
        if (fell) s *= 0.55;
        speed = s;
        vx = s * tsx;
        vy = s * tsy;
        phase = Phase.SLIDE;
        slideTime = 0;
    }

    private void stepSlide(double dt) {
        slideTime += dt;
        if (fell) {
            tumble += 520.0 * dt;
            roll = clamp(roll + 0, -60, 60);
        }
        int n = 2;
        double h = dt / n;
        for (int i = 0; i < n; i++) {
            double th = slopeAngle(x);
            double brake = fell ? 7.0 : (x > hill.xK() ? 4.5 : 1.2);
            double a = G * Math.sin(th) * 0.35 - brake;
            speed = Math.max(0, speed + a * h);
            x += speed * Math.cos(th) * h;
            double g = terrain.groundY(x, y + 2, z);
            if (Double.isNaN(g)) { finish(); return; }
            y = g;
        }
        pitch += (0 - pitch) * Math.min(1, 4 * dt);
        roll += (0 - roll) * Math.min(1, 3 * dt);
        if (speed < 0.7 || x > hill.outrunEnd() - 2 || slideTime > 14) finish();
    }

    private void finish() {
        phase = Phase.DONE;
    }

    // ------------------------------------------------------------------ odczyty

    public double windHead() { return w[0]; }
    public double windSide() { return w[1]; }
    public double windUp() { return w[2]; }
    public double avgHead() { return flightTime > 0 ? sumHead / flightTime : 0; }
    public double avgSide() { return flightTime > 0 ? sumSide / flightTime : 0; }
    public double meanAlpha() { return flightTime > 0 ? sumAlpha / flightTime : 0; }
    public double meanAlphaDev() { return flightTime > 0 ? sumAlphaDev / flightTime : 0; }
    public double meanAbsRoll() { return flightTime > 0 ? sumAbsRoll / flightTime : 0; }
    public double vFraction() { return vWindowTime > 0 ? vOpenTime / vWindowTime : 0; }
    public boolean isFinished() { return phase == Phase.DONE; }

    static double clamp(double v, double lo, double hi) { return v < lo ? lo : (v > hi ? hi : v); }
    static double lerp(double a, double b, double t) { return a + (b - a) * t; }
}
