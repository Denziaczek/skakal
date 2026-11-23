package dev.skijump.client;

import dev.skijump.net.StateData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;

/** Stan sesji skoku po stronie klienta (tylko wizualizacja - fizyka jest na serwerze). */
public final class ClientState {
    public static boolean active;
    public static StateData state = new StateData();
    public static long rxNanos;
    public static boolean vOpen;
    private static Perspective previous;
    private static boolean cameraSet;

    private ClientState() { }

    public static void onState(MinecraftClient mc, StateData d) {
        if (d.active && !active) {
            active = true;
            vOpen = false;
            cameraSet = false;
        }
        state = d;
        rxNanos = System.nanoTime();
        if (d.active && d.phase >= 2 && !cameraSet) {
            // kamera kinowa: widok zza pleców na czas lotu i lądowania
            previous = mc.options.getPerspective();
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            cameraSet = true;
        }
        if (!d.active) end(mc);
    }

    public static void end(MinecraftClient mc) {
        if (cameraSet && previous != null && mc != null) mc.options.setPerspective(previous);
        cameraSet = false;
        active = false;
        vOpen = false;
        KeyBinding.updatePressedStates();
    }

    /** Aktualna szacowana wartość "ms do idealnego momentu wybicia" uwzględniająca czas od ostatniego pakietu. */
    public static double predictedMsToIdeal() {
        double elapsed = (System.nanoTime() - rxNanos) / 1.0e6;
        return state.msToIdeal - elapsed;
    }
}
