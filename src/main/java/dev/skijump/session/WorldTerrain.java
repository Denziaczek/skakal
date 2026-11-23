package dev.skijump.session;

import dev.skijump.block.TrackLayerBlock;
import dev.skijump.hill.HillRecord;
import dev.skijump.physics.Terrain;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

/** Podłoże czytane z klocków świata, w układzie lokalnym skoczni (działa też dla skoczni zbudowanych przez gracza). */
public final class WorldTerrain implements Terrain {
    private final ServerWorld world;
    private final HillRecord hill;
    private final BlockPos.Mutable mp = new BlockPos.Mutable();

    public WorldTerrain(ServerWorld world, HillRecord hill) {
        this.world = world;
        this.hill = hill;
    }

    /** Wysokość górnej powierzchni klocka (0..1) lub -1, gdy klocek nie jest podłożem. */
    static double topOf(ServerWorld world, BlockState st, BlockPos pos) {
        if (st.isAir()) return -1;
        if (st.isOf(Blocks.SNOW)) return st.get(SnowBlock.LAYERS) / 8.0;
        if (st.getBlock() instanceof TrackLayerBlock) return st.get(TrackLayerBlock.LAYERS) / 8.0;
        VoxelShape shape = st.getCollisionShape(world, pos);
        if (shape.isEmpty()) return -1;
        return shape.getMax(Direction.Axis.Y);
    }

    @Override
    public double groundY(double x, double yHint, double z) {
        Vec3d w = hill.toWorld(x, 0, z);
        int bx = MathHelper.floor(w.x), bz = MathHelper.floor(w.z);
        if (!world.getChunkManager().isChunkLoaded(bx >> 4, bz >> 4)) return fallback(x);
        double wy = hill.edgeY + yHint * hill.scale;
        int startY = MathHelper.floor(wy) + 4;
        int minY = Math.max(world.getBottomY(), startY - 300);
        for (int by = startY; by >= minY; by--) {
            mp.set(bx, by, bz);
            BlockState st = world.getBlockState(mp);
            if (st.isAir()) continue;
            double top = topOf(world, st, mp);
            if (top < 0) continue;
            return (by + top - hill.edgeY) / hill.scale;
        }
        return fallback(x);
    }

    /** Dla skoczni z presetu profil zeskoku znamy analitycznie - skok poza załadowanymi chunkami liczy się dalej. */
    private double fallback(double x) {
        var g = hill.geometry();
        if (g.analyticLanding && x > 0) return g.landingY(x);
        return Double.NaN;
    }
}
