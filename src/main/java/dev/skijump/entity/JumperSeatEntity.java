package dev.skijump.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.world.World;

/**
 * Niewidzialna "gondola" skoczka. Serwer przesuwa ją zgodnie z symulacją, gracz jest pasażerem.
 * Dzięki temu ruch jest płynny (interpolacja encji), serwer jest autorytatywny, a inni gracze widzą skok.
 */
public class JumperSeatEntity extends Entity implements PoseSource {
    private static final TrackedData<Integer> PHASE = DataTracker.registerData(JumperSeatEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> FLAGS = DataTracker.registerData(JumperSeatEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> PITCH_T = DataTracker.registerData(JumperSeatEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> ROLL_T = DataTracker.registerData(JumperSeatEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> V_T = DataTracker.registerData(JumperSeatEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> TUMBLE_T = DataTracker.registerData(JumperSeatEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public JumperSeatEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    @Override
    protected void initDataTracker() {
        dataTracker.startTracking(PHASE, 0);
        dataTracker.startTracking(FLAGS, 0);
        dataTracker.startTracking(PITCH_T, 0f);
        dataTracker.startTracking(ROLL_T, 0f);
        dataTracker.startTracking(V_T, 0f);
        dataTracker.startTracking(TUMBLE_T, 0f);
    }

    public void setSkiPose(int phase, float pitch, float roll, float v, float tumble, boolean telemark, boolean crouch) {
        dataTracker.set(PHASE, phase);
        dataTracker.set(PITCH_T, pitch);
        dataTracker.set(ROLL_T, roll);
        dataTracker.set(V_T, v);
        dataTracker.set(TUMBLE_T, tumble);
        dataTracker.set(FLAGS, (telemark ? 1 : 0) | (crouch ? 2 : 0));
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient && age > 60 && getPassengerList().isEmpty()) discard();
    }

    @Override
    public double getMountedHeightOffset() {
        return 0.35;
    }

    public boolean shouldRiderSit() {
        return posePhase() == 0;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) { }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) { }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }

    @Override public int posePhase() { return dataTracker.get(PHASE); }
    @Override public float posePitch() { return dataTracker.get(PITCH_T); }
    @Override public float poseRoll() { return dataTracker.get(ROLL_T); }
    @Override public float poseV() { return dataTracker.get(V_T); }
    @Override public float poseTumble() { return dataTracker.get(TUMBLE_T); }
    @Override public boolean poseTelemark() { return (dataTracker.get(FLAGS) & 1) != 0; }
    @Override public boolean poseCrouch() { return (dataTracker.get(FLAGS) & 2) != 0; }
}
