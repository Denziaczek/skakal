package dev.skijump.client;

import dev.skijump.SkiJumpMod;
import dev.skijump.entity.SkoczekEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class SkoczekRenderer extends BipedEntityRenderer<SkoczekEntity, SkoczekModel> {
    private static final Identifier TEXTURE = SkiJumpMod.id("textures/entity/skoczek.png");

    public SkoczekRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new SkoczekModel(ctx.getPart(EntityModelLayers.PLAYER)), 0.5f);
        this.addFeature(new SkiFeatureRenderer<>(this, ctx.getModelLoader()));
        this.addFeature(new GearFeatureRenderer<>(this, ctx.getModelLoader()));
    }

    @Override
    public Identifier getTexture(SkoczekEntity entity) {
        return TEXTURE;
    }
}
