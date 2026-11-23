package dev.skijump.client;

import dev.skijump.entity.PoseSource;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;

/** Poza skoczka: pochylenie ciała, przechył, układ V nart (nogi), telemark - dla graczy w gondoli i NPC. */
public final class SkiPose {
    private SkiPose() { }

    public static PoseSource sourceOf(LivingEntity e) {
        if (e instanceof PoseSource ps) return ps;
        Entity v = e.getVehicle();
        if (v instanceof PoseSource ps) return ps;
        return null;
    }

    /** Obrót całego modelu (wywoływane z mixinu LivingEntityRenderer.setupTransforms). */
    public static void transform(LivingEntity e, MatrixStack m) {
        PoseSource ps = sourceOf(e);
        if (ps == null) return;
        int ph = ps.posePhase();
        float lean;
        float roll = 0f;
        float tumble = 0f;
        switch (ph) {
            case 1 -> lean = 52f;
            case 2 -> {
                lean = Math.max(20f, Math.min(88f, 78f - 0.6f * ps.posePitch()));
                roll = ps.poseRoll();
            }
            case 3 -> {
                lean = ps.poseTelemark() ? 14f : 28f;
                tumble = ps.poseTumble();
                roll = ps.poseRoll() * 0.5f;
            }
            default -> {
                return;
            }
        }
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-lean));
        if (tumble != 0f) m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-tumble));
    }

    /** Ułożenie kończyn (nogi w V, ręce wzdłuż ciała, telemark). */
    public static void applyModel(BipedEntityModel<?> model, LivingEntity e) {
        PoseSource ps = sourceOf(e);
        if (ps == null) return;
        int ph = ps.posePhase();
        if (ph < 1 || ph > 3) return;
        float v = Math.max(0f, Math.min(1f, ps.poseV()));
        ModelPart ll = model.leftLeg, rl = model.rightLeg, la = model.leftArm, ra = model.rightArm;
        switch (ph) {
            case 1 -> {                                  // pozycja najazdowa
                ll.pitch = -0.45f; rl.pitch = -0.45f;
                ll.roll = 0f; rl.roll = 0f;
                la.pitch = 0.9f; ra.pitch = 0.9f;
                la.roll = 0f; ra.roll = 0f;
            }
            case 2 -> {                                  // lot: nogi w V, ręce przy ciele
                float spread = 0.06f + 0.30f * v;
                ll.pitch = 0f; rl.pitch = 0f;
                ll.roll = -spread; rl.roll = spread;
                la.pitch = 0.2f; ra.pitch = 0.2f;
                la.roll = -0.08f; ra.roll = 0.08f;
            }
            default -> {                                 // lądowanie
                if (ps.poseTelemark()) {
                    ll.pitch = -0.55f; rl.pitch = 0.5f;
                    la.pitch = 0.35f; ra.pitch = -0.35f;
                } else {
                    ll.pitch = 0f; rl.pitch = 0f;
                    la.pitch = 0.2f; ra.pitch = 0.2f;
                }
                ll.roll = -0.05f; rl.roll = 0.05f;
                float fl = ps.poseTumble();
                if (fl != 0f) {
                    la.pitch = (float) Math.sin(Math.toRadians(fl)) * 2.0f;
                    ra.pitch = (float) -Math.sin(Math.toRadians(fl)) * 2.0f;
                }
            }
        }
        if (model instanceof PlayerEntityModel<?> pm) {
            pm.leftPants.copyTransform(pm.leftLeg);
            pm.rightPants.copyTransform(pm.rightLeg);
            pm.leftSleeve.copyTransform(pm.leftArm);
            pm.rightSleeve.copyTransform(pm.rightArm);
        }
    }
}
