package dev.skijump.mixin.client;

import dev.skijump.client.SkiPose;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pochylenie i przechył ciała skoczka (gracz w gondoli lub NPC) podczas rozbiegu, lotu i lądowania. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "setupTransforms", at = @At("TAIL"), require = 0)
    private void skijump$tilt(LivingEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw,
                              float tickDelta, CallbackInfo ci) {
        SkiPose.transform(entity, matrices);
    }
}
