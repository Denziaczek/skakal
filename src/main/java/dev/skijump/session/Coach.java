package dev.skijump.session;

import dev.skijump.physics.JumpResult;
import dev.skijump.physics.JumpSimulation;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Trener: po skoku wskazuje 1-3 najważniejsze rzeczy do poprawy (wybicie, kąt natarcia, przechył, V, telemark, wiatr). */
public final class Coach {
    private Coach() { }

    private static Text t(String key, Object... args) {
        return Text.translatable("skijump.coach." + key, args);
    }

    public static List<Text> tips(JumpSimulation sim, JumpResult r) {
        List<Text> out = new ArrayList<>();
        if (!sim.jumpPressed) {
            out.add(t("no_takeoff"));
        } else if (r.takeoffErrMs < -30) {
            out.add(t("early", String.valueOf(Math.round(-r.takeoffErrMs))));
        } else if (r.takeoffErrMs > 30) {
            out.add(t("late", String.valueOf(Math.round(r.takeoffErrMs))));
        }
        double mean = sim.meanAlpha();
        if (mean < JumpSimulation.ALPHA_OPT - 6) out.add(t("alpha_low"));
        else if (mean > JumpSimulation.ALPHA_OPT + 5 || sim.stallTime > 0.4) out.add(t("alpha_high"));
        if (sim.meanAbsRoll() > 6) out.add(t("roll"));
        if (sim.vFraction() < 0.5) out.add(t("v"));
        if (!sim.telemarkPressed) out.add(t("telemark_none"));
        else if (sim.telemarkOffsetMs > 400) out.add(t("telemark_early"));
        else if (sim.telemarkOffsetMs < 150) out.add(t("telemark_late"));
        if (Math.abs(r.avgHead) > 2.0) out.add(t(r.avgHead < 0 ? "tailwind" : "headwind"));
        if (out.isEmpty()) out.add(t("great"));
        return out.size() > 3 ? new ArrayList<>(out.subList(0, 3)) : out;
    }
}
