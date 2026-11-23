package dev.skijump.client;

import dev.skijump.SkiJumpMod;
import dev.skijump.entity.SkoczekEntity;
import dev.skijump.registry.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

/** Rysuje długie narty przypięte do nóg (podążają za nogami, więc w locie widać układ V). */
public class SkiFeatureRenderer<T extends LivingEntity, M extends BipedEntityModel<T>> extends FeatureRenderer<T, M> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(SkiJumpMod.id("skis"), "main");
    private static final Identifier WOOD = SkiJumpMod.id("textures/entity/ski_wood.png");
    private static final Identifier IRON = SkiJumpMod.id("textures/entity/ski_iron.png");
    private static final Identifier COMPOSITE = SkiJumpMod.id("textures/entity/ski_composite.png");

    private final ModelPart ski;

    public SkiFeatureRenderer(FeatureRendererContext<T, M> context, EntityModelLoader loader) {
        super(context);
        this.ski = loader.getModelPart(LAYER).getChild("ski");
    }

    public static void registerLayer() {
        EntityModelLayerRegistry.registerModelLayer(LAYER, SkiFeatureRenderer::createLayer);
    }

    public static TexturedModelData createLayer() {
        ModelData data = new ModelData();
        data.getRoot().addChild("ski", ModelPartBuilder.create().uv(0, 0).cuboid(-2f, 0f, -13f, 4f, 1f, 24f),
                ModelTransform.pivot(0f, 12f, 0f));
        return TexturedModelData.of(data, 64, 32);
    }

    private Identifier textureFor(T entity) {
        if (entity instanceof SkoczekEntity) return COMPOSITE;
        Item it = entity.getEquippedStack(EquipmentSlot.FEET).getItem();
        if (it == ModItems.SKI_WOOD) return WOOD;
        if (it == ModItems.SKI_IRON) return IRON;
        if (it == ModItems.SKI_COMPOSITE) return COMPOSITE;
        return null;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                       float headYaw, float headPitch) {
        Identifier tex = textureFor(entity);
        if (tex == null) return;
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
        M model = getContextModel();
        for (ModelPart leg : new ModelPart[]{model.leftLeg, model.rightLeg}) {
            matrices.push();
            leg.rotate(matrices);
            ski.render(matrices, vc, light, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);
            matrices.pop();
        }
    }
}
