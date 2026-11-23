package dev.skijump.net;

import net.minecraft.network.PacketByteBuf;

/** Pakiet klient -> serwer: sterowanie zawodnika (wysyłane co tick sesji). */
public final class InputData {
    public float pitch, roll, jumpErr;
    public boolean crouch, vOpen, jump, telemark, abort;

    public void write(PacketByteBuf buf) {
        buf.writeFloat(pitch);
        buf.writeFloat(roll);
        buf.writeFloat(jumpErr);
        int flags = (crouch ? 1 : 0) | (vOpen ? 2 : 0) | (jump ? 4 : 0) | (telemark ? 8 : 0) | (abort ? 16 : 0);
        buf.writeByte(flags);
    }

    public static InputData read(PacketByteBuf buf) {
        InputData d = new InputData();
        d.pitch = clamp(buf.readFloat(), -1, 1);
        d.roll = clamp(buf.readFloat(), -1, 1);
        d.jumpErr = clamp(buf.readFloat(), -1000, 1000);
        int f = buf.readByte();
        d.crouch = (f & 1) != 0;
        d.vOpen = (f & 2) != 0;
        d.jump = (f & 4) != 0;
        d.telemark = (f & 8) != 0;
        d.abort = (f & 16) != 0;
        return d;
    }

    private static float clamp(float v, float lo, float hi) {
        if (Float.isNaN(v)) return 0;
        return Math.max(lo, Math.min(hi, v));
    }
}
