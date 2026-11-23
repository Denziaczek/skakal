package dev.skijump.mixin.client;

import dev.skijump.client.SkiPose;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Układ V nart (nogi), pozycja najazdowa i telemark gracza. */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void skijump$limbs(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress,
                               float headYaw, float headPitch, CallbackInfo ci) {
        SkiPose.applyModel((PlayerEntityModel<?>) (Object) this, entity);
    }
}
