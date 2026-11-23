package dev.skijump.hill;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Zapis skoczni i rekordów w danych świata. */
public final class HillState extends PersistentState {
    private final List<HillRecord> hills = new ArrayList<>();

    public static HillState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(HillState::fromNbt, HillState::new, "skijump_hills");
    }

    public static HillState fromNbt(NbtCompound nbt) {
        HillState s = new HillState();
        NbtList list = nbt.getList("hills", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) s.hills.add(HillRecord.fromNbt(list.getCompound(i)));
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (HillRecord h : hills) list.add(h.toNbt());
        nbt.put("hills", list);
        return nbt;
    }

    public List<HillRecord> all() {
        return hills;
    }

    public HillRecord add(HillRecord h) {
        if (h.id.isEmpty()) h.id = UUID.randomUUID().toString().substring(0, 8);
        hills.add(h);
        markDirty();
        return h;
    }

    public void remove(HillRecord h) {
        hills.remove(h);
        markDirty();
    }

    public HillRecord byId(String id) {
        for (HillRecord h : hills) if (h.id.equalsIgnoreCase(id)) return h;
        return null;
    }

    public HillRecord byGate(ServerWorld world, BlockPos pos) {
        String dim = world.getRegistryKey().getValue().toString();
        for (HillRecord h : hills) {
            if (h.dim.equals(dim) && h.gatePos.getSquaredDistance(pos) <= 9) return h;
        }
        return null;
    }

    /** Najbliższa skocznia (liczona do krawędzi progu) w promieniu maxDist bloków. */
    public HillRecord nearest(ServerWorld world, BlockPos pos, double maxDist) {
        String dim = world.getRegistryKey().getValue().toString();
        HillRecord best = null;
        double bestD = Double.MAX_VALUE;
        for (HillRecord h : hills) {
            if (!h.dim.equals(dim)) continue;
            double d = Math.min(h.edge.getSquaredDistance(pos), h.gatePos.getSquaredDistance(pos));
            double reach = Math.max(maxDist, h.k * h.scale * 1.4);
            if (d <= reach * reach && d < bestD) {
                bestD = d;
                best = h;
            }
        }
        return best;
    }
}
