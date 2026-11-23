package dev.skijump.net;

import net.minecraft.network.PacketByteBuf;

/** Pakiet serwer -> klient: stan skoku do HUD-u (klient tylko wizualizuje, fizyka jest na serwerze). */
public final class StateData {
    public boolean active;
    public int phase;                // 0 belka, 1 rozbieg, 2 lot, 3 lądowanie
    public float speed, msToIdeal, alpha, pitch, roll, vOpen, windHead, windSide, windUp;
    public float distance, clearance, timeToContact, flightTime, k, hs, yaw;
    public boolean armed, telemark, crouch, jumped, tookOff;
    public float takeoffErr;
    public int guide;
    public int gate;

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(active);
        buf.writeByte(phase);
        buf.writeFloat(speed);
        buf.writeFloat(msToIdeal);
        buf.writeFloat(alpha);
        buf.writeFloat(pitch);
        buf.writeFloat(roll);
        buf.writeFloat(vOpen);
        buf.writeFloat(windHead);
        buf.writeFloat(windSide);
        buf.writeFloat(windUp);
        buf.writeFloat(distance);
        buf.writeFloat(clearance);
        buf.writeFloat(timeToContact);
        buf.writeFloat(flightTime);
        buf.writeFloat(k);
        buf.writeFloat(hs);
        buf.writeFloat(yaw);
        buf.writeByte((armed ? 1 : 0) | (telemark ? 2 : 0) | (crouch ? 4 : 0) | (jumped ? 8 : 0));
        buf.writeByte(gate);
        buf.writeFloat(takeoffErr);
        buf.writeByte(guide);
        buf.writeBoolean(tookOff);
    }

    public static StateData read(PacketByteBuf buf) {
        StateData d = new StateData();
        d.active = buf.readBoolean();
        d.phase = buf.readByte();
        d.speed = buf.readFloat();
        d.msToIdeal = buf.readFloat();
        d.alpha = buf.readFloat();
        d.pitch = buf.readFloat();
        d.roll = buf.readFloat();
        d.vOpen = buf.readFloat();
        d.windHead = buf.readFloat();
        d.windSide = buf.readFloat();
        d.windUp = buf.readFloat();
        d.distance = buf.readFloat();
        d.clearance = buf.readFloat();
        d.timeToContact = buf.readFloat();
        d.flightTime = buf.readFloat();
        d.k = buf.readFloat();
        d.hs = buf.readFloat();
        d.yaw = buf.readFloat();
        int f = buf.readByte();
        d.armed = (f & 1) != 0;
        d.telemark = (f & 2) != 0;
        d.crouch = (f & 4) != 0;
        d.jumped = (f & 8) != 0;
        d.gate = buf.readByte();
        d.takeoffErr = buf.readFloat();
        d.guide = buf.readByte();
        d.tookOff = buf.readBoolean();
        return d;
    }
}
