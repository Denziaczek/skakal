package dev.skijump.client;

import dev.skijump.entity.SkoczekEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;

public class SkoczekModel extends BipedEntityModel<SkoczekEntity> {
    public SkoczekModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setAngles(SkoczekEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        SkiPose.applyModel(this, entity);
    }
}
