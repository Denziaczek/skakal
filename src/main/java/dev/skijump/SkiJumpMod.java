package dev.skijump;

import dev.skijump.career.Tutorial;
import dev.skijump.command.SkiJumpCommand;
import dev.skijump.comp.CompetitionManager;
import dev.skijump.hill.HillBuilder;
import dev.skijump.hill.NaturalHills;
import dev.skijump.net.Net;
import dev.skijump.registry.ModBlocks;
import dev.skijump.registry.ModEntities;
import dev.skijump.registry.ModGroups;
import dev.skijump.registry.ModItems;
import dev.skijump.registry.ModSounds;
import dev.skijump.session.SessionManager;
import dev.skijump.session.WindManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Ski Jump - skoki narciarskie w Minecrafcie (Fabric 1.20.1). */
public class SkiJumpMod implements ModInitializer {
    public static final String MOD_ID = "skijump";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModConfig.load();
        ModSounds.init();
        ModBlocks.init();
        ModEntities.init();
        ModItems.init();
        ModItems.initSpawnEgg();
        ModGroups.init();
        Net.initServer();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> SkiJumpCommand.register(dispatcher));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            WindManager.tick(server);
            HillBuilder.tick(server);
            SessionManager.tick(server);
            CompetitionManager.tick(server);
            NaturalHills.tick(server);
            dev.skijump.entity.SkoczekEntity.tickPlaybacks();
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Tutorial.onJoin(handler.player, server));
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> NaturalHills.onChunk(world, chunk));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SessionManager.abort(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            SessionManager.clearAll();
            CompetitionManager.clearAll();
            HillBuilder.clearAll();
            dev.skijump.entity.SkoczekEntity.clearPlaybacks();
        });
        LOGGER.info("Ski Jump zaladowany - powodzenia na belce!");
    }
}
