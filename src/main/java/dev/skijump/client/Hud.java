package dev.skijump.client;

import dev.skijump.net.StateData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.Locale;

/** HUD skoku: prędkość, pasek timingu, wskaźnik balansu, wiatr i odległość na żywo. */
public final class Hud {
    private Hud() { }

    private static String f1(double v) {
        return String.format(Locale.US, "%.1f", v);
    }

    public static void render(DrawContext ctx, float tickDelta) {
        if (!ClientState.active) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        StateData s = ClientState.state;
        TextRenderer tr = mc.textRenderer;
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        // --- prędkość i faza (góra, środek)
        ctx.drawCenteredTextWithShadow(tr, Text.translatable("skijump.hud.speed", f1(s.speed * 3.6), f1(s.speed)), w / 2, 8, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(tr, Text.translatable("skijump.hud.phase." + Math.min(3, Math.max(0, s.phase))), w / 2, 20, 0xFFD060);

        // --- wiatr (prawy górny róg)
        int wx = w - 120, wy = 8;
        ctx.fill(wx - 4, wy - 4, w - 6, wy + 40, 0x80000000);
        String head = s.windHead >= 0 ? "\u2193" : "\u2191";
        ctx.drawTextWithShadow(tr, Text.translatable("skijump.hud.wind"), wx, wy, 0xA0D8FF);
        ctx.drawTextWithShadow(tr, Text.literal(head + " " + f1(Math.abs(s.windHead)) + " m/s  "
                + Text.translatable(s.windHead >= 0 ? "skijump.hud.headwind" : "skijump.hud.tailwind").getString()), wx, wy + 12, s.windHead >= 0 ? 0x80FF80 : 0xFF8080);
        ctx.drawTextWithShadow(tr, Text.literal((s.windSide >= 0 ? "\u2192 " : "\u2190 ") + f1(Math.abs(s.windSide)) + " m/s"), wx, wy + 24, 0xFFFFFF);

        // --- belka i K/HS (lewy górny róg)
        ctx.drawTextWithShadow(tr, Text.translatable("skijump.hud.hill", String.valueOf((int) s.k), String.valueOf((int) s.hs), String.valueOf(s.gate)), 8, 8, 0xFFFFFF);

        switch (s.phase) {
            case 0 -> ctx.drawCenteredTextWithShadow(tr, Text.translatable("skijump.hud.hint_seated"), w / 2, h - 60, 0xFFFFFF);
            case 1 -> inrun(ctx, tr, s, w, h);
            case 2 -> flight(ctx, tr, s, w, h);
            default -> {
                ctx.drawCenteredTextWithShadow(tr, Text.translatable("skijump.hud.distance", f1(s.distance)), w / 2, h - 80, 0xFFE070);
            }
        }
        if (s.phase >= 2 && s.flightTime < 6.0) takeoffFeedback(ctx, tr, s, w);
        if (s.guide > 0) guide(ctx, tr, s, w);
    }

    /** Informacja zwrotna o wybiciu: widać, czy Spacja została zarejestrowana i jaki był błąd w ms. */
    private static void takeoffFeedback(DrawContext ctx, TextRenderer tr, StateData s, int w) {
        if (s.takeoffErr > 900) {
            ctx.drawCenteredTextWithShadow(tr, Text.translatable("skijump.hud.to.none"), w / 2, 34, 0xFF6060);
            return;
        }
        float e = s.takeoffErr;
        String key = Math.abs(e) <= 12 ? "skijump.hud.to.perfect" : Math.abs(e) <= 40 ? "skijump.hud.to.good" : e < 0 ? "skijump.hud.to.early" : "skijump.hud.to.late";
        int col = Math.abs(e) <= 12 ? 0x60FF60 : Math.abs(e) <= 40 ? 0xC0FF60 : 0xFFB050;
        ctx.drawCenteredTextWithShadow(tr, Text.translatable(key, String.format(Locale.US, "%+.0f", e)), w / 2, 34, col);
    }

    /** Panel przewodnika (czas zatrzymany): tytuł, opis i klawisz wznowienia. */
    private static void guide(DrawContext ctx, TextRenderer tr, StateData s, int w) {
        int pw = 330, px = w / 2 - pw / 2, py = 48;
        java.util.List<net.minecraft.text.OrderedText> lines = tr.wrapLines(Text.translatable("skijump.guide." + s.guide + ".text"), pw - 16);
        int ph = 36 + lines.size() * 11 + 8;
        ctx.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, 0xF0101820);
        ctx.fill(px, py, px + pw, py + ph, 0xE0203040);
        ctx.drawTextWithShadow(tr, Text.translatable("skijump.guide." + s.guide + ".title"), px + 8, py + 6, 0xFFD060);
        int y = py + 22;
        for (net.minecraft.text.OrderedText l : lines) {
            ctx.drawTextWithShadow(tr, l, px + 8, y, 0xFFFFFF);
            y += 11;
        }
        ctx.drawTextWithShadow(tr, Text.translatable("skijump.guide.continue"), px + 8, py + ph - 12, 0x80FF80);
    }

    private static void inrun(DrawContext ctx, TextRenderer tr, StateData s, int w, int h) {
        // pasek timingu: kursor zbliża się do środka (idealny moment), zielona strefa = bonus
        int bw = 240, bx = w / 2 - bw / 2, by = h - 62;
        ctx.fill(bx - 2, by - 2, bx + bw + 2, by + 14, 0xA0000000);
        ctx.fill(bx, by, bx + bw, by + 12, 0xFF202830);
        int c = w / 2;
        ctx.fill(c - 22, by, c + 22, by + 12, 0xFF3A6A3A);      // okno dobrego wybicia (+-45 ms)
        ctx.fill(c - 5, by, c + 5, by + 12, 0xFF40E040);        // idealne okno (+-12 ms)
        double pred = ClientState.predictedMsToIdeal();
        int px = (int) Math.round(c + Math.max(-bw / 2.0, Math.min(bw / 2.0, pred * 0.30)));
        boolean jumped = s.jumped;
        ctx.fill(px - 2, by - 3, px + 2, by + 15, jumped ? 0xFF8080FF : 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(tr, Text.translatable("skijump.hud.timing", String.format(Locale.US, "%+.0f", -pred)), w / 2, by - 14, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(tr, Text.translatable("skijump.hud.hint_takeoff"), w / 2, by + 18, 0xE0E0E0);
    }

    private static void flight(DrawContext ctx, TextRenderer tr, StateData s, int w, int h) {
        // wskaźnik balansu (pionowy): kąt natarcia 0..45 deg; zielone optimum, czerwone przeciągnięcie
        int bx = 14, by = h / 2 - 60, bh = 120, bw = 14;
        ctx.fill(bx - 2, by - 2, bx + bw + 2, by + bh + 2, 0xA0000000);
        double a = Math.max(0, Math.min(45, s.alpha));
        int yOf = (int) (by + bh - a / 45.0 * bh);
        ctx.fill(bx, by, bx + bw, by + bh, 0xFF202830);
        ctx.fill(bx, by + bh - (int) (35 / 45.0 * bh) - 0, bx + bw, by, 0xFF6A2A2A);                      // > 35: przeciągnięcie
        ctx.fill(bx, by + bh - (int) (30 / 45.0 * bh), bx + bw, by + bh - (int) (24 / 45.0 * bh), 0xFF3A7A3A); // optimum
        ctx.fill(bx - 3, yOf - 1, bx + bw + 3, yOf + 1, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, Text.translatable("skijump.hud.alpha", String.valueOf((int) Math.round(s.alpha))), bx, by + bh + 6, 0xFFFFFF);

        // przechył: poziomy pasek
        int rw = 160, rx = w / 2 - rw / 2, ry = h - 40;
        ctx.fill(rx - 2, ry - 2, rx + rw + 2, ry + 10, 0xA0000000);
        ctx.fill(rx, ry, rx + rw, ry + 8, 0xFF202830);
        ctx.fill(w / 2 - 1, ry - 2, w / 2 + 1, ry + 10, 0xFF40E040);
        int rpos = (int) Math.round(w / 2.0 + Math.max(-30, Math.min(30, s.roll)) / 30.0 * (rw / 2.0));
        ctx.fill(rpos - 2, ry - 2, rpos + 2, ry + 10, Math.abs(s.roll) > 12 ? 0xFFFF6060 : 0xFFFFFFFF);

        // narty V
        ctx.drawTextWithShadow(tr, Text.translatable(ClientState.vOpen ? "skijump.hud.v_on" : "skijump.hud.v_off"), 14, h / 2 + 90,
                s.vOpen > 0.8 ? 0x80FF80 : 0xFFC060);

        // odległość na żywo + postęp względem K i HS
        ctx.drawCenteredTextWithShadow(tr, Text.translatable("skijump.hud.distance", f1(s.distance)), w / 2, h - 96, 0xFFE070);
        int dw = 200, dx = w / 2 - dw / 2, dy = h - 82;
        double max = s.hs * 1.1;
        ctx.fill(dx, dy, dx + dw, dy + 5, 0xA0202020);
        ctx.fill(dx, dy, dx + (int) (Math.min(1.0, s.distance / max) * dw), dy + 5, 0xFFE0C040);
        ctx.fill(dx + (int) (s.k / max * dw), dy - 2, dx + (int) (s.k / max * dw) + 1, dy + 7, 0xFFFF4040);
        ctx.fill(dx + (int) (s.hs / max * dw), dy - 2, dx + (int) (s.hs / max * dw) + 1, dy + 7, 0xFF4080FF);

        // wysokość nad zboczem i podpowiedź telemarku
        if (s.clearance < 40) {
            ctx.drawCenteredTextWithShadow(tr, Text.translatable("skijump.hud.clearance", f1(Math.max(0, s.clearance))), w / 2, h - 108, 0xC0E0FF);
        }
        if (!s.telemark) {
            boolean now = s.timeToContact < 0.7;
            ctx.drawCenteredTextWithShadow(tr, Text.translatable(now ? "skijump.hud.telemark_now" : "skijump.hud.hint_telemark"), w / 2, h - 22,
                    now ? 0xFFFF40 : 0xE0E0E0);
        } else {
            ctx.drawCenteredTextWithShadow(tr, Text.translatable("skijump.hud.telemark_ready"), w / 2, h - 22, 0x80FF80);
        }
    }
}
