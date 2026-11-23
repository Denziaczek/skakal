package dev.skijump.hill;

import dev.skijump.ModConfig;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.Structure;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Random;

/**
 * Naturalne skocznie. Świat generuje znacznik struktury "skijump_hill" (dane w data/minecraft/worldgen/structure) -
 * dzięki temu działa /locate structure skijump_hill. Gdy chunk ze startem takiej struktury się załaduje,
 * w tym miejscu budowana jest mała lub średnia skocznia ze skrzynką trenera.
 */
public final class NaturalHills {
    private static final ArrayDeque<Object[]> PENDING = new ArrayDeque<>();

    private NaturalHills() { }

    public static void onChunk(ServerWorld world, Chunk chunk) {
        if (ModConfig.get().naturalHillChance <= 0) return;
        Map<Structure, StructureStart> starts = chunk.getStructureStarts();
        if (starts.isEmpty()) return;
        Registry<Structure> reg = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        for (Map.Entry<Structure, StructureStart> e : starts.entrySet()) {
            Identifier id = reg.getId(e.getKey());
            if (id == null || !id.getPath().equals("skijump_hill")) continue;
            StructureStart st = e.getValue();
            if (!st.hasChildren()) continue;
            BlockPos c = st.getBoundingBox().getCenter();
            PENDING.add(new Object[]{world, c, world.getSeed() ^ c.asLong()});
        }
    }

    /** Przetwarza jedną oczekującą skocznię na tick (poza callbackiem ładowania chunka). */
    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty() || HillBuilder.isBuilding()) return;
        Object[] p = PENDING.poll();
        ServerWorld world = (ServerWorld) p[0];
        BlockPos c = (BlockPos) p[1];
        Random r = new Random((Long) p[2]);
        HillState st = HillState.get(server);
        for (HillRecord h : st.all()) {
            if (h.dim.equals(world.getRegistryKey().getValue().toString()) && h.edge.getSquaredDistance(c) < 250.0 * 250.0) return;
        }
        HillPreset preset = HillPreset.random(r);
        Direction best = null;
        int bestDrop = 4;
        int h0 = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, c.getX(), c.getZ());
        for (Direction d : Direction.Type.HORIZONTAL) {
            int hx = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, c.getX() + d.getOffsetX() * 40, c.getZ() + d.getOffsetZ() * 40);
            if (h0 - hx > bestDrop) { bestDrop = h0 - hx; best = d; }
        }
        if (best == null) best = Direction.Type.HORIZONTAL.random(net.minecraft.util.math.random.Random.create(r.nextLong()));
        BlockPos edge = new BlockPos(c.getX(), h0 + 4, c.getZ());
        HillRecord rec = HillBuilder.start(world, preset, edge, best, Math.min(1.0, ModConfig.get().hillScale), null);
        rec.natural = true;
    }
}
