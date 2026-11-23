package dev.skijump.client;

import dev.skijump.net.Net;
import dev.skijump.net.StateData;
import dev.skijump.registry.ModBlocks;
import dev.skijump.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SkiJumpClient implements ClientModInitializer {
    public static KeyBinding KEY_V;
    public static KeyBinding KEY_TELEMARK;
    public static KeyBinding KEY_ABORT;

    @Override
    public void onInitializeClient() {
        KEY_V = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.skijump.vstyle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.skijump"));
        KEY_TELEMARK = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.skijump.telemark", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.skijump"));
        KEY_ABORT = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.skijump.abort", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "key.categories.skijump"));

        ClientPlayNetworking.registerGlobalReceiver(Net.STATE, (client, handler, buf, sender) -> {
            StateData d = StateData.read(buf);
            client.execute(() -> ClientState.onState(client, d));
        });
        ClientTickEvents.START_CLIENT_TICK.register(ClientInput::tick);
        HudRenderCallback.EVENT.register(Hud::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientState.end(client));

        SkiFeatureRenderer.registerLayer();
        GearFeatureRenderer.registerLayer();
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, helper, ctx) -> {
            if (renderer instanceof PlayerEntityRenderer pr) {
                helper.register(new SkiFeatureRenderer<>(pr, ctx.getModelLoader()));
                helper.register(new GearFeatureRenderer<>(pr, ctx.getModelLoader()));
            }
        });

        EntityRendererRegistry.register(ModEntities.SEAT, SeatRenderer::new);
        EntityRendererRegistry.register(ModEntities.SKOCZEK, SkoczekRenderer::new);

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ANEMOMETER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TROPHY_GOLD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TROPHY_SILVER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TROPHY_BRONZE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.START_GATE, RenderLayer.getCutout());
    }
}
