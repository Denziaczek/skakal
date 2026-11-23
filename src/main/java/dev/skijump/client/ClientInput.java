package dev.skijump.client;

import dev.skijump.net.InputData;
import dev.skijump.net.Net;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.lwjgl.glfw.GLFW;

/**
 * Zbiera sterowanie gracza podczas skoku i wysyła je na serwer. Klawisze vanilla (W/S/A/D, spacja, Shift)
 * są odczytywane "na surowo" i wyciszane, żeby nie wysiadać z gondoli (Shift) ani nie ruszać postacią.
 */
public final class ClientInput {
    private static final float REF_PITCH = 14.0f;
    private static boolean wasJump, wasTele, wasAbort, wasV;

    private ClientInput() { }

    private static boolean down(KeyBinding kb, long win) {
        InputUtil.Key k = KeyBindingHelper.getBoundKeyOf(kb);
        if (k == null || k.getCode() < 0) return false;
        if (k.getCategory() == InputUtil.Type.MOUSE) return GLFW.glfwGetMouseButton(win, k.getCode()) == GLFW.GLFW_PRESS;
        if (k.getCategory() == InputUtil.Type.KEYSYM) return InputUtil.isKeyPressed(win, k.getCode());
        return false;
    }

    public static void tick(MinecraftClient mc) {
        if (!ClientState.active || mc.player == null) {
            wasJump = wasTele = wasAbort = wasV = false;
            return;
        }
        long win = mc.getWindow().getHandle();
        boolean gui = mc.currentScreen != null;
        boolean fwd = !gui && down(mc.options.forwardKey, win);
        boolean back = !gui && down(mc.options.backKey, win);
        boolean left = !gui && down(mc.options.leftKey, win);
        boolean right = !gui && down(mc.options.rightKey, win);
        boolean sneak = !gui && down(mc.options.sneakKey, win);
        boolean jump = !gui && down(mc.options.jumpKey, win);
        boolean tele = !gui && down(SkiJumpClient.KEY_TELEMARK, win);
        boolean abort = !gui && down(SkiJumpClient.KEY_ABORT, win);
        boolean attack = !gui && down(mc.options.attackKey, win);
        // krótkie stuknięcia (krótsze niż tick) też liczymy; lewy przycisk myszy działa jak Spacja
        boolean tap = false;
        while (mc.options.jumpKey.wasPressed()) tap = true;
        while (mc.options.attackKey.wasPressed()) tap = true;
        if (gui) tap = false;
        boolean vKey = !gui && down(SkiJumpClient.KEY_V, win);

        // wyciszenie klawiszy vanilla, aby postać nie wysiadała z gondoli i nie "chodziła"
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        mc.options.attackKey.setPressed(false);

        InputData d = new InputData();
        float pitchIn = (fwd ? 1f : 0f) - (back ? 1f : 0f);
        int phase = ClientState.state.phase;
        if (phase >= 2) {
            // sterowanie myszą: ruch w dół = pochylenie do przodu; pozycja kamery jest stale przywracana
            float delta = mc.player.getPitch() - REF_PITCH;
            pitchIn += Math.max(-1f, Math.min(1f, delta * 0.10f));
            mc.player.setPitch(REF_PITCH);
            mc.player.prevPitch = REF_PITCH;
        }
        d.pitch = Math.max(-1f, Math.min(1f, pitchIn));
        d.roll = (right ? 1f : 0f) - (left ? 1f : 0f);
        d.crouch = sneak;

        if (vKey && !wasV) ClientState.vOpen = !ClientState.vOpen;
        wasV = vKey;
        d.vOpen = ClientState.vOpen;

        boolean jumpNow = jump || attack;
        if ((jumpNow && !wasJump) || tap) {
            d.jump = true;
            if (phase == 1) d.jumpErr = (float) -ClientState.predictedMsToIdeal();
        }
        wasJump = jumpNow;
        if (tele && !wasTele) d.telemark = true;
        wasTele = tele;
        if (abort && !wasAbort) d.abort = true;
        wasAbort = abort;

        // widok zablokowany wzdłuż osi skoczni
        mc.player.setYaw(ClientState.state.yaw);
        mc.player.prevYaw = ClientState.state.yaw;

        PacketByteBuf buf = PacketByteBufs.create();
        d.write(buf);
        ClientPlayNetworking.send(Net.INPUT, buf);
    }
}
