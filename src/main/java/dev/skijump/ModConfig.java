package dev.skijump;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Konfiguracja moda (plik config/skijump.json). */
public final class ModConfig {
    /** Skala skoczni stawianych z presetów (1.0 = wymiary rzeczywiste; mniejsza = mniej klocków). */
    public double hillScale = 1.0;
    /** Ile komputerowych zawodników ("Skoczek") dopisać do zawodów. */
    public int npcCount = 11;
    /** Czas zapisów do zawodów w sekundach. */
    public int registrationSeconds = 45;
    /** Ile sekund gracz ma na rozpoczęcie skoku, gdy jest jego kolej. */
    public int turnTimeoutSeconds = 90;
    /** Czy wymagać rangi do odblokowania skoczni (operatorzy i creative zawsze mają dostęp). */
    public boolean enforceRanks = true;
    /** Mnożnik siły wiatru (0 = brak wiatru). */
    public double windStrength = 1.0;
    /** Limit klocków stawianych na tick podczas budowy skoczni. */
    public int buildBlocksPerTick = 1500;
    /** Naturalne skocznie w świecie (struktura skijump_hill): &gt; 0 włączone, 0 wyłączone. */
    public double naturalHillChance = 1.0;
    /** Survival: skok męczy (wymaga min. 4 punktów głodu i zużywa jedzenie). */
    public boolean survivalHunger = true;
    /** Największy punkt K skoczni, którą gracz w survivalu może postawić Planem skoczni (za surowce). */
    public int survivalBlueprintMaxK = 90;
    /** Ile pierwszych skoków gracza przebiega z przewodnikiem (zatrzymywanie czasu i objaśnienia). */
    public int guidedFirstJumps = 2;
    /** Porady trenera po skoku. */
    public boolean coachTips = true;
    /** Ile zawodów trwa sezon (Puchar Świata). */
    public int seasonLength = 4;

    private static ModConfig instance = new ModConfig();

    public static ModConfig get() {
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("skijump.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (Files.exists(path)) {
            try (Reader r = Files.newBufferedReader(path)) {
                ModConfig c = gson.fromJson(r, ModConfig.class);
                if (c != null) instance = c;
            } catch (Exception e) {
                SkiJumpMod.LOGGER.warn("Nie udało się wczytać konfiguracji, używam domyślnej", e);
            }
        }
        instance.hillScale = Math.max(0.1, Math.min(2.0, instance.hillScale));
        instance.npcCount = Math.max(0, Math.min(60, instance.npcCount));
        instance.buildBlocksPerTick = Math.max(100, instance.buildBlocksPerTick);
        try (Writer w = Files.newBufferedWriter(path)) {
            gson.toJson(instance, w);
        } catch (IOException e) {
            SkiJumpMod.LOGGER.warn("Nie udało się zapisać konfiguracji", e);
        }
    }
}
