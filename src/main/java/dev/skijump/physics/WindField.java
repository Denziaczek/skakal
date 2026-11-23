package dev.skijump.physics;

import java.util.Random;

/**
 * Pole wiatru nad skocznią (czysta Java, bez zależności od Minecrafta).
 *
 * Wiatr = składowa stała (baseHead/baseSide/baseUp) + porywy będące sumą kilku sinusoid
 * o losowych częstotliwościach i fazach. Dzięki temu porywy zmieniają się w czasie w sposób
 * płynny, a jednocześnie pole jest deterministyczne dla danego ziarna (seed).
 *
 * Konwencja (układ lokalny skoczka): head > 0 = wiatr w twarz (pod narty),
 * head < 0 = wiatr w plecy; side > 0 = wiatr wieje na prawą stronę skoczka; up > 0 = prąd wznoszący.
 */
public final class WindField {
    public double baseHead;
    public double baseSide;
    public double baseUp;
    /** amplituda porywów w m/s */
    public double gust;

    private final double[][] freq = new double[3][3];
    private final double[][] phase = new double[3][3];
    private static final double[] WEIGHT = {0.55, 0.30, 0.15};
    private static final double[] CHANNEL_SCALE = {1.0, 0.7, 0.35};

    public WindField(long seed) {
        Random r = new Random(seed);
        for (int c = 0; c < 3; c++) {
            for (int k = 0; k < 3; k++) {
                // częstotliwości: wolne (0.07-0.3 Hz), średnie (0.3-0.7 Hz), szybkie (0.7-1.4 Hz)
                double lo = new double[]{0.07, 0.30, 0.70}[k];
                double hi = new double[]{0.30, 0.70, 1.40}[k];
                freq[c][k] = lo + r.nextDouble() * (hi - lo);
                phase[c][k] = r.nextDouble() * Math.PI * 2.0;
            }
        }
    }

    /** Kopia pola (zamrożony stan składowych stałych na czas jednego skoku). */
    public WindField copy(long seed) {
        WindField w = new WindField(seed);
        w.baseHead = baseHead;
        w.baseSide = baseSide;
        w.baseUp = baseUp;
        w.gust = gust;
        for (int c = 0; c < 3; c++) {
            System.arraycopy(freq[c], 0, w.freq[c], 0, 3);
            System.arraycopy(phase[c], 0, w.phase[c], 0, 3);
        }
        return w;
    }

    /** Wypełnia out[0]=head, out[1]=side, out[2]=up dla czasu t (sekundy). */
    public void sample(double t, double[] out) {
        double[] base = {baseHead, baseSide, baseUp};
        for (int c = 0; c < 3; c++) {
            double g = 0;
            for (int k = 0; k < 3; k++) {
                g += WEIGHT[k] * Math.sin(2.0 * Math.PI * freq[c][k] * t + phase[c][k]);
            }
            out[c] = base[c] + g * gust * CHANNEL_SCALE[c];
        }
    }

    /** Płynnie zbliża składowe stałe do wartości docelowych (wywoływane co tick). */
    public void approach(double tHead, double tSide, double tUp, double tGust, double rate) {
        baseHead += (tHead - baseHead) * rate;
        baseSide += (tSide - baseSide) * rate;
        baseUp += (tUp - baseUp) * rate;
        gust += (tGust - gust) * rate;
    }
}
