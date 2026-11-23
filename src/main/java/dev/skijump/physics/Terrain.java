package dev.skijump.physics;

/** Źródło wysokości podłoża w układzie lokalnym skoczni (metry "wirtualne"). */
public interface Terrain {
    /**
     * Wysokość powierzchni zbocza w punkcie (x, z) względem krawędzi progu (y = 0 na krawędzi).
     * Zwraca {@link Double#NaN}, jeśli podłoże jest nieznane (np. niezaładowany chunk).
     * @param yHint aktualna wysokość skoczka - implementacja szuka powierzchni tuż pod nią
     */
    double groundY(double x, double yHint, double z);
}
