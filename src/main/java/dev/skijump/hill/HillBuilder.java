package dev.skijump.hill;

import dev.skijump.ModConfig;
import dev.skijump.SkiJumpMod;
import dev.skijump.block.AnemometerBlock;
import dev.skijump.block.StartGateBlock;
import dev.skijump.block.TrackLayerBlock;
import dev.skijump.physics.HillGeometry;
import dev.skijump.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import dev.skijump.registry.ModItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Generator skoczni z presetu. Skocznia jest "szkieletowa": powierzchnia (warstwowe klocki) o grubości 3
 * plus filary co kilka bloków - dzięki temu nawet mamut to kilkadziesiąt tysięcy klocków, stawianych
 * stopniowo w kolejnych tickach (BuildJob), bez lagów.
 */
public final class HillBuilder {
    private static final List<BuildJob> JOBS = new ArrayList<>();

    private HillBuilder() { }

    /** Rejestruje skocznię i kolejkuje budowę. edgePos = pozycja stóp stojącego na progu (powierzchnia progu). */
    public static HillRecord start(ServerWorld world, HillPreset preset, BlockPos edgePos, Direction facing,
                                   double scale, UUID owner) {
        HillState state = HillState.get(world.getServer());
        HillRecord rec = new HillRecord();
        rec.name = preset.id;
        rec.presetId = preset.id;
        rec.dim = world.getRegistryKey().getValue().toString();
        rec.edge = edgePos;
        rec.edgeY = edgePos.getY();
        rec.facing = facing;
        rec.scale = scale;
        rec.k = preset.k;
        rec.hs = preset.hs;
        rec.rank = preset.rank;
        rec.gate = HillGeometry.DEFAULT_GATE;
        rec.built = false;
        HillGeometry g = preset.geometry();
        // pozycja belki: szczyt rozbiegu
        int aGate = (int) Math.floor(-g.inrunD * scale);
        rec.gatePos = columnPos(rec, aGate, 0, 0);
        state.add(rec);
        JOBS.add(new BuildJob(world, rec, g, owner));
        return rec;
    }

    public static void clearAll() {
        JOBS.clear();
    }

    public static boolean isBuilding() {
        return !JOBS.isEmpty();
    }

    public static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) return;
        BuildJob job = JOBS.get(0);
        if (job.step(ModConfig.get().buildBlocksPerTick)) JOBS.remove(0);
    }

    static BlockPos columnPos(HillRecord r, int a, int b, int y) {
        Direction f = r.facing, rr = f.rotateYClockwise();
        return new BlockPos(r.edge.getX() + a * f.getOffsetX() + b * rr.getOffsetX(), y,
                r.edge.getZ() + a * f.getOffsetZ() + b * rr.getOffsetZ());
    }

    // ------------------------------------------------------------------------------------------

    private static final class BuildJob {
        final ServerWorld world;
        final HillRecord rec;
        final HillGeometry g;
        final UUID owner;
        final double scale;
        final int aMin, aMax, lw, aK, aHs, tableStart;
        int a, b;
        int stage = 0;
        int budget;
        long placed;
        final BlockPos.Mutable mp = new BlockPos.Mutable();

        BuildJob(ServerWorld world, HillRecord rec, HillGeometry g, UUID owner) {
            this.world = world;
            this.rec = rec;
            this.g = g;
            this.owner = owner;
            this.scale = rec.scale;
            this.aMin = (int) Math.floor(-g.inrunD * scale) - 3;
            this.aMax = (int) Math.ceil(g.outrunEnd() * scale);
            this.lw = Math.max(5, (int) Math.ceil(g.halfWidth * scale));
            this.aK = (int) Math.round(g.xK() * scale);
            this.aHs = (int) Math.round(g.xHs() * scale);
            this.tableStart = (int) Math.floor(-g.tableLen * scale);
            this.a = aMin;
            this.b = -lw;
        }

        /** Zwraca true, gdy budowa zakończona. */
        boolean step(int perTick) {
            budget = perTick;
            if (stage == 0) {
                while (budget > 0 && a <= aMax) {
                    if (a <= 0 && Math.abs(b) > 2) {
                        // poza torem rozbiegu - nic do zrobienia
                    } else {
                        column(a, b);
                    }
                    b++;
                    if (b > lw) {
                        b = -lw;
                        a++;
                    }
                }
                if (a > aMax) stage = 1;
                return false;
            }
            structures();
            rec.built = true;
            HillState.get(world.getServer()).markDirty();
            ServerPlayerEntity p = owner == null ? null : world.getServer().getPlayerManager().getPlayer(owner);
            if (p != null) {
                p.sendMessage(Text.translatable("skijump.msg.hill_ready", Text.translatable("skijump.hill." + rec.presetId),
                        (int) rec.k, (int) rec.hs), false);
            }
            SkiJumpMod.LOGGER.info("Skocznia {} gotowa ({} klocków)", rec.presetId, placed);
            return true;
        }

        double surfaceYw(int aa) {
            double xv = aa / scale;
            double yv = aa <= 0 ? g.inrunY(xv) : g.landingY(xv);
            return rec.edgeY + yv * scale;
        }

        void set(int x, int y, int z, BlockState s) {
            mp.set(x, y, z);
            world.setBlockState(mp, s, Block.NOTIFY_LISTENERS);
            budget--;
            placed++;
        }

        boolean passable(int x, int y, int z) {
            mp.set(x, y, z);
            BlockState s = world.getBlockState(mp);
            return s.isAir() || s.getCollisionShape(world, mp).isEmpty();
        }

        void pillar(int x, int yTop, int z) {
            int bottom = Math.max(world.getBottomY() + 1, yTop - 220);
            for (int y = yTop; y >= bottom; y--) {
                if (!passable(x, y, z)) break;
                set(x, y, z, Blocks.STONE_BRICKS.getDefaultState());
            }
        }

        void column(int aa, int bb) {
            BlockPos base = columnPos(rec, aa, bb, 0);
            int x = base.getX(), z = base.getZ();
            if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
                world.getChunk(x >> 4, z >> 4);
            }
            double yw = surfaceYw(aa);
            int ytop = (int) Math.floor(yw - 1e-6);
            int layers = (int) Math.max(1, Math.min(8, Math.round((yw - ytop) * 8.0)));
            int ab = Math.abs(bb);
            boolean inrun = aa <= 0;

            // wyczyszczenie przestrzeni nad powierzchnią (drzewa, wzgórza) - korytarz lotu
            int clear = inrun ? 6 : Math.max(8, (int) (12 * Math.max(scale, 0.5)));
            for (int y = ytop + 1; y <= ytop + clear; y++) {
                mp.set(x, y, z);
                if (!world.getBlockState(mp).isAir()) {
                    world.setBlockState(mp, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                    budget--;
                }
            }
            budget--;

            if (inrun) {
                if (ab <= 1) {
                    boolean table = aa >= tableStart;
                    Block top = table ? ModBlocks.TAKEOFF_TABLE : ModBlocks.INRUN_TRACK;
                    set(x, ytop, z, top.getDefaultState().with(TrackLayerBlock.LAYERS, layers));
                    set(x, ytop - 1, z, Blocks.STONE_BRICKS.getDefaultState());
                    set(x, ytop - 2, z, Blocks.STONE_BRICKS.getDefaultState());
                } else if (ab == 2) {
                    set(x, ytop - 2, z, Blocks.STONE_BRICKS.getDefaultState());
                    set(x, ytop - 1, z, Blocks.STONE_BRICKS.getDefaultState());
                    set(x, ytop, z, Blocks.STONE_BRICKS.getDefaultState());
                    set(x, ytop + 1, z, Blocks.STONE_BRICK_WALL.getDefaultState());
                    if (Math.floorMod(aa, 8) == 0) pillar(x, ytop - 3, z);
                }
                return;
            }
            // zeskok i wybieg
            BlockState top;
            if (aa == aK) top = ModBlocks.K_MARKER.getDefaultState().with(TrackLayerBlock.LAYERS, layers);
            else if (aa == aHs) top = ModBlocks.HS_MARKER.getDefaultState().with(TrackLayerBlock.LAYERS, layers);
            else top = Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, layers);
            set(x, ytop, z, top);
            set(x, ytop - 1, z, Blocks.SNOW_BLOCK.getDefaultState());
            set(x, ytop - 2, z, Blocks.SNOW_BLOCK.getDefaultState());
            if (Math.floorMod(aa, 6) == 0 && Math.floorMod(bb, 6) == 0) pillar(x, ytop - 3, z);
            if (ab == lw) set(x, ytop - 1, z, Blocks.PACKED_ICE.getDefaultState());
        }

        int topY(int aa) {
            return (int) Math.floor(surfaceYw(aa) - 1e-6);
        }

        void placeOnPillar(int aa, int bb, int y, BlockState s) {
            BlockPos p = columnPos(rec, aa, bb, y);
            if (!world.getChunkManager().isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) world.getChunk(p.getX() >> 4, p.getZ() >> 4);
            pillar(p.getX(), y - 1, p.getZ());
            set(p.getX(), y, p.getZ(), s);
        }

        void structures() {
            budget = Integer.MAX_VALUE / 2;
            Direction f = rec.facing;
            // belka startowa na szczycie rozbiegu
            int aGate = (int) Math.floor(-g.inrunD * scale);
            int yGate = topY(aGate) + 1;
            BlockPos gp = columnPos(rec, aGate, 0, yGate);
            set(gp.getX(), gp.getY(), gp.getZ(), ModBlocks.START_GATE.getDefaultState().with(StartGateBlock.FACING, f));
            rec.gatePos = gp;
            // stacja zawodów obok belki
            placeOnPillar(aGate + 2, 3, topY(aGate + 2) + 1, ModBlocks.COMPETITION_STATION.getDefaultState());
            // beczka trenera ze sprzętem startowym (tylko skocznie naturalne - wejście do survivalu)
            if (rec.natural) {
                int ab = aGate + 2;
                int yb = topY(ab) + 1;
                placeOnPillar(ab, -3, yb, Blocks.BARREL.getDefaultState());
                BlockPos bp = columnPos(rec, ab, -3, yb);
                BlockEntity be = world.getBlockEntity(bp);
                if (be instanceof Inventory inv) {
                    inv.setStack(0, new ItemStack(ModItems.SKI_WOOD));
                    inv.setStack(1, new ItemStack(ModItems.SUIT_STANDARD));
                    inv.setStack(2, new ItemStack(ModItems.HELMET));
                    inv.setStack(3, new ItemStack(Items.BREAD, 8));
                    inv.setStack(4, new ItemStack(Items.SNOWBALL, 16));
                }
            }
            // wieża sędziowska przy punkcie K
            int yT = topY(aK) + 1;
            for (int i = 0; i < 4; i++) {
                BlockPos p = columnPos(rec, aK, lw + 2, yT + i);
                set(p.getX(), p.getY(), p.getZ(), ModBlocks.JUDGE_TOWER.getDefaultState());
            }
            BlockPos tp = columnPos(rec, aK, lw + 2, yT);
            pillar(tp.getX(), yT - 1, tp.getZ());
            // wiatromierze po lewej stronie zeskoku
            double[] frac = {0.30, 0.60, 0.90};
            for (double fr : frac) {
                int aa = (int) Math.round(g.xAtChord(g.k * fr) * scale);
                placeOnPillar(aa, -(lw + 2), topY(aa) + 2, ModBlocks.ANEMOMETER.getDefaultState().with(AnemometerBlock.STRENGTH, 0));
            }
            // tablica wyników na wybiegu
            int aS = Math.min(aMax - 2, aK + (int) Math.round(0.25 * g.k * scale));
            placeOnPillar(aS, lw + 2, topY(aS) + 1, ModBlocks.SCOREBOARD.getDefaultState());
        }
    }

    /** Dla porządku: iterator po aktywnych budowach (np. do komend diagnostycznych). */
    public static int pendingJobs() {
        Iterator<BuildJob> it = JOBS.iterator();
        int n = 0;
        while (it.hasNext()) { it.next(); n++; }
        return n;
    }
}
