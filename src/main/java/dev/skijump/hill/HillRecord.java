package dev.skijump.hill;

import dev.skijump.physics.HillGeometry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Zarejestrowana w świecie skocznia (preset lub zdefiniowana różdżką). */
public final class HillRecord {
    public static final int MAX_GATE = HillGeometry.MAX_GATE;

    public static final class Entry {
        public String name;
        public double distance;
        public double points;

        public Entry(String name, double distance, double points) {
            this.name = name;
            this.distance = distance;
            this.points = points;
        }
    }

    public String id = "";
    public String name = "";
    public String presetId = "custom";
    public String dim = "minecraft:overworld";
    public BlockPos edge = BlockPos.ORIGIN;      // kolumna krawędzi progu
    public double edgeY;                         // wysokość powierzchni na krawędzi progu
    public Direction facing = Direction.NORTH;
    public double scale = 1.0;
    public BlockPos gatePos = BlockPos.ORIGIN;   // blok belki startowej
    public int gate = HillGeometry.DEFAULT_GATE;
    public double k, hs;
    public double customD, customH;              // tylko dla skoczni własnych
    public boolean built = true;
    public boolean natural;                      // wygenerowana przez świat (ma beczkę trenera)
    public int rank = 0;
    public double recordDist;
    public String recordHolder = "";
    public final List<Entry> top = new ArrayList<>();

    private transient HillGeometry cachedGeometry;

    public boolean isCustom() {
        return "custom".equals(presetId);
    }

    public HillGeometry geometry() {
        if (cachedGeometry == null) {
            if (isCustom()) cachedGeometry = HillGeometry.custom(customD, customH, k, hs);
            else {
                HillPreset p = HillPreset.find(presetId);
                cachedGeometry = p != null ? p.geometry() : HillGeometry.forK(k, hs);
            }
        }
        return cachedGeometry;
    }

    /** Układ lokalny skoczni (wirtualne metry) -> współrzędne świata. */
    public Vec3d toWorld(double x, double y, double z) {
        Direction r = facing.rotateYClockwise();
        double wx = edge.getX() + 0.5 + (x * facing.getOffsetX() + z * r.getOffsetX()) * scale;
        double wz = edge.getZ() + 0.5 + (x * facing.getOffsetZ() + z * r.getOffsetZ()) * scale;
        return new Vec3d(wx, edgeY + y * scale, wz);
    }

    public Vec3d edgeCenter() {
        return toWorld(0, 0, 0);
    }

    /** Rejestruje wynik gracza w tabeli najlepszych i rekordzie skoczni. Zwraca true przy nowym rekordzie. */
    public boolean submit(String who, double distance, double points) {
        top.add(new Entry(who, distance, points));
        top.sort((a, b) -> Double.compare(b.points, a.points));
        while (top.size() > 8) top.remove(top.size() - 1);
        if (distance > recordDist + 0.01) {
            recordDist = distance;
            recordHolder = who;
            return true;
        }
        return false;
    }

    public NbtCompound toNbt() {
        NbtCompound t = new NbtCompound();
        t.putString("id", id);
        t.putString("name", name);
        t.putString("preset", presetId);
        t.putString("dim", dim);
        t.putLong("edge", edge.asLong());
        t.putDouble("edgeY", edgeY);
        t.putInt("facing", facing.getHorizontal());
        t.putDouble("scale", scale);
        t.putLong("gatePos", gatePos.asLong());
        t.putInt("gate", gate);
        t.putDouble("k", k);
        t.putDouble("hs", hs);
        t.putDouble("cd", customD);
        t.putDouble("ch", customH);
        t.putBoolean("built", built);
        t.putBoolean("nat", natural);
        t.putInt("rank", rank);
        t.putDouble("recDist", recordDist);
        t.putString("recHolder", recordHolder);
        NbtList list = new NbtList();
        for (Entry e : top) {
            NbtCompound c = new NbtCompound();
            c.putString("n", e.name);
            c.putDouble("d", e.distance);
            c.putDouble("p", e.points);
            list.add(c);
        }
        t.put("top", list);
        return t;
    }

    public static HillRecord fromNbt(NbtCompound t) {
        HillRecord h = new HillRecord();
        h.id = t.getString("id");
        h.name = t.getString("name");
        h.presetId = t.getString("preset");
        h.dim = t.getString("dim");
        h.edge = BlockPos.fromLong(t.getLong("edge"));
        h.edgeY = t.getDouble("edgeY");
        h.facing = Direction.fromHorizontal(t.getInt("facing"));
        h.scale = t.getDouble("scale");
        h.gatePos = BlockPos.fromLong(t.getLong("gatePos"));
        h.gate = t.getInt("gate");
        h.k = t.getDouble("k");
        h.hs = t.getDouble("hs");
        h.customD = t.getDouble("cd");
        h.customH = t.getDouble("ch");
        h.built = t.getBoolean("built");
        h.natural = t.getBoolean("nat");
        h.rank = t.getInt("rank");
        h.recordDist = t.getDouble("recDist");
        h.recordHolder = t.getString("recHolder");
        NbtList list = t.getList("top", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound c = list.getCompound(i);
            h.top.add(new Entry(c.getString("n"), c.getDouble("d"), c.getDouble("p")));
        }
        return h;
    }
}
