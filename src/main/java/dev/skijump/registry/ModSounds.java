package dev.skijump.registry;

import dev.skijump.SkiJumpMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;

public final class ModSounds {
    public static final SoundEvent WIND = reg("wind");
    public static final SoundEvent CROWD = reg("crowd");
    public static final SoundEvent LAND = reg("land");
    public static final SoundEvent FALL = reg("fall");
    public static final SoundEvent START = reg("start");

    private static SoundEvent reg(String name) {
        SoundEvent e = SoundEvent.of(SkiJumpMod.id(name));
        return Registry.register(Registries.SOUND_EVENT, SkiJumpMod.id(name), e);
    }

    public static void init() { }

    private ModSounds() { }
}
