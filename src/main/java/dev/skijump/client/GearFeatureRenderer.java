package dev.skijump.client;

import dev.skijump.SkiJumpMod;
import dev.skijump.entity.SkoczekEntity;
import dev.skijump.registry.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
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
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Kask, kombinezon i buty narciarskie jako własne bryły (muszla kasku z wizjerem, kołnierz, pas, rękawice, mankiety...),
 * zamiast płaskiej tekstury pancerza. Tekstura to 4 pola 64x32: R0 baza, R1 akcent, R2 ciemne, R3 grafit/jasne.
 */
public class GearFeatureRenderer<T extends LivingEntity, M extends BipedEntityModel<T>> extends FeatureRenderer<T, M> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(SkiJumpMod.id("gear"), "main");
    private static final Identifier HELMET_TEX = SkiJumpMod.id("textures/entity/gear_helmet.png");
    private static final Identifier SUIT_STD = SkiJumpMod.id("textures/entity/gear_suit_standard.png");
    private static final Identifier SUIT_PRO = SkiJumpMod.id("textures/entity/gear_suit_pro.png");
    private static final String[] HELMET = {"h_shell", "h_rim", "h_visor", "h_crest", "h_earL", "h_earR", "h_back"};
    private static final String[] TORSO = {"s_torso", "s_collar", "s_stripe", "s_belt", "s_num"};
    private static final String[] ARM_R = {"s_armR", "s_cuffR", "s_gloveR"};
    private static final String[] ARM_L = {"s_armL", "s_cuffL", "s_gloveL"};
    private static final String[] LEG_R = {"s_legR", "s_lstripeR"};
    private static final String[] LEG_L = {"s_legL", "s_lstripeL"};
    private static final String[] BOOT = {"b_boot", "b_rim"};

    private final ModelPart root;

    public GearFeatureRenderer(FeatureRendererContext<T, M> context, EntityModelLoader loader) {
        super(context);
        this.root = loader.getModelPart(LAYER);
    }

    public static void registerLayer() {
        EntityModelLayerRegistry.registerModelLayer(LAYER, GearFeatureRenderer::createLayer);
    }

    private static void box(ModelPartData r, String name, int u, int v, float x, float y, float z, float w, float h, float d) {
        r.addChild(name, ModelPartBuilder.create().uv(u, v).cuboid(x, y, z, w, h, d), ModelTransform.NONE);
    }

    public static TexturedModelData createLayer() {
        ModelData data = new ModelData();
        ModelPartData r = data.getRoot();
        // kask (współrzędne względem głowy)
        box(r, "h_shell", 0, 0, -4.7f, -8.9f, -4.7f, 9.4f, 6.2f, 9.4f);
        box(r, "h_rim", 64, 0, -4.9f, -3.1f, -4.9f, 9.8f, 1.0f, 9.8f);
        box(r, "h_visor", 0, 32, -3.9f, -6.4f, -5.1f, 7.8f, 2.6f, 0.8f);
        box(r, "h_crest", 64, 0, -0.6f, -10.0f, -3.5f, 1.2f, 1.2f, 7.0f);
        box(r, "h_earL", 64, 32, 4.6f, -5.6f, -1.8f, 0.7f, 4.2f, 3.6f);
        box(r, "h_earR", 64, 32, -5.3f, -5.6f, -1.8f, 0.7f, 4.2f, 3.6f);
        box(r, "h_back", 64, 0, -3.0f, -4.6f, 4.6f, 6.0f, 2.6f, 0.9f);
        // kombinezon: tułów
        box(r, "s_torso", 0, 0, -4.4f, -0.2f, -2.4f, 8.8f, 12.2f, 4.8f);
        box(r, "s_collar", 64, 0, -2.6f, -0.9f, -2.7f, 5.2f, 1.3f, 5.4f);
        box(r, "s_stripe", 64, 0, -0.9f, 0.5f, -2.65f, 1.8f, 9.0f, 0.3f);
        box(r, "s_belt", 64, 0, -4.5f, 8.6f, -2.5f, 9.0f, 1.6f, 5.0f);
        box(r, "s_num", 64, 32, -2.2f, 2.0f, 2.45f, 4.4f, 4.0f, 0.3f);
        // ręce (prawa: x -3..1, lewa: x -1..3)
        box(r, "s_armR", 0, 0, -3.4f, -2.3f, -2.4f, 4.8f, 8.6f, 4.8f);
        box(r, "s_cuffR", 64, 0, -3.5f, 6.0f, -2.5f, 5.0f, 1.4f, 5.0f);
        box(r, "s_gloveR", 0, 32, -3.4f, 7.4f, -2.4f, 4.8f, 2.8f, 4.8f);
        box(r, "s_armL", 0, 0, -1.4f, -2.3f, -2.4f, 4.8f, 8.6f, 4.8f);
        box(r, "s_cuffL", 64, 0, -1.5f, 6.0f, -2.5f, 5.0f, 1.4f, 5.0f);
        box(r, "s_gloveL", 0, 32, -1.4f, 7.4f, -2.4f, 4.8f, 2.8f, 4.8f);
        // nogi
        box(r, "s_legR", 0, 0, -2.4f, -0.3f, -2.4f, 4.8f, 9.0f, 4.8f);
        box(r, "s_lstripeR", 64, 0, -2.6f, 0.2f, -0.8f, 0.3f, 8.5f, 1.6f);
        box(r, "s_legL", 0, 0, -2.4f, -0.3f, -2.4f, 4.8f, 9.0f, 4.8f);
        box(r, "s_lstripeL", 64, 0, 2.3f, 0.2f, -0.8f, 0.3f, 8.5f, 1.6f);
        // buty narciarskie
        box(r, "b_boot", 64, 32, -2.5f, 8.8f, -2.7f, 5.0f, 3.4f, 5.4f);
        box(r, "b_rim", 64, 0, -2.55f, 8.5f, -2.75f, 5.1f, 0.7f, 5.5f);
        return TexturedModelData.of(data, 128, 64);
    }

    private void draw(VertexConsumer vc, MatrixStack m, int light, String[] names) {
        for (String n : names) {
            root.getChild(n).render(m, vc, light, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);
        }
    }

    private static boolean isSki(ItemStack s) {
        Item i = s.getItem();
        return i == ModItems.SKI_WOOD || i == ModItems.SKI_IRON || i == ModItems.SKI_COMPOSITE;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                       float headYaw, float headPitch) {
        boolean npc = entity instanceof SkoczekEntity;
        ItemStack head = entity.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chest = entity.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack feet = entity.getEquippedStack(EquipmentSlot.FEET);
        Identifier suitTex = npc ? SUIT_PRO : chest.isOf(ModItems.SUIT_PRO) ? SUIT_PRO : chest.isOf(ModItems.SUIT_STANDARD) ? SUIT_STD : null;
        boolean helmet = npc || head.isOf(ModItems.HELMET);
        boolean boots = npc || isSki(feet);
        M m = getContextModel();

        if (helmet) {
            VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(HELMET_TEX));
            matrices.push();
            m.head.rotate(matrices);
            draw(vc, matrices, light, HELMET);
            matrices.pop();
        }
        if (suitTex != null) {
            VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(suitTex));
            matrices.push(); m.body.rotate(matrices); draw(vc, matrices, light, TORSO); matrices.pop();
            matrices.push(); m.rightArm.rotate(matrices); draw(vc, matrices, light, ARM_R); matrices.pop();
            matrices.push(); m.leftArm.rotate(matrices); draw(vc, matrices, light, ARM_L); matrices.pop();
            matrices.push(); m.rightLeg.rotate(matrices); draw(vc, matrices, light, LEG_R); matrices.pop();
            matrices.push(); m.leftLeg.rotate(matrices); draw(vc, matrices, light, LEG_L); matrices.pop();
        }
        if (boots) {
            VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(HELMET_TEX));
            matrices.push(); m.rightLeg.rotate(matrices); draw(vc, matrices, light, BOOT); matrices.pop();
            matrices.push(); m.leftLeg.rotate(matrices); draw(vc, matrices, light, BOOT); matrices.pop();
        }
    }
}
