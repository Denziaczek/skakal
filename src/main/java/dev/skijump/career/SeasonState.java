package dev.skijump.career;

import dev.skijump.ModConfig;
import dev.skijump.comp.Competition;
import dev.skijump.registry.ModBlocks;
import dev.skijump.registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Sezon (Puchar Świata): punkty za miejsca w zawodach na dowolnych skoczniach, nagroda dla lidera po ostatnich zawodach. */
public final class SeasonState extends PersistentState {
    private static final int[] WC_POINTS = {100, 80, 60, 50, 45, 40, 36, 32, 29, 26, 24, 22, 20, 18, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1};

    public int season = 1;
    public int events = 0;
    public final Map<String, Double> points = new LinkedHashMap<>();

    public static SeasonState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(SeasonState::fromNbt, SeasonState::new, "skijump_season");
    }

    public List<Map.Entry<String, Double>> ranking() {
        List<Map.Entry<String, Double>> l = new ArrayList<>(points.entrySet());
        l.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return l;
    }

    /** Zapisuje wyniki zawodów (lista uczestników od zwycięzcy) i ewentualnie kończy sezon. */
    public void record(MinecraftServer server, List<Competition.Entrant> standings) {
        for (int i = 0; i < standings.size() && i < WC_POINTS.length; i++) {
            points.merge(standings.get(i).name, (double) WC_POINTS[i], Double::sum);
        }
        events++;
        markDirty();
        int len = Math.max(1, ModConfig.get().seasonLength);
        broadcast(server, Text.translatable("skijump.season.event_done", String.valueOf(season), String.valueOf(events), String.valueOf(len)));
        if (events >= len) endSeason(server, standings);
    }

    private void endSeason(MinecraftServer server, List<Competition.Entrant> lastStandings) {
        List<Map.Entry<String, Double>> rk = ranking();
        broadcast(server, Text.translatable("skijump.season.final_title", String.valueOf(season)));
        for (int i = 0; i < Math.min(5, rk.size()); i++) {
            broadcast(server, Text.translatable("skijump.season.row", String.valueOf(i + 1), rk.get(i).getKey(),
                    String.format(Locale.US, "%.0f", rk.get(i).getValue())));
        }
        if (!rk.isEmpty()) {
            String winner = rk.get(0).getKey();
            for (Competition.Entrant e : lastStandings) {
                if (e.player != null && e.name.equals(winner)) {
                    ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.player);
                    CareerState cs = CareerState.get(server);
                    cs.of(e.player).xp += 150;
                    cs.markDirty();
                    if (p != null) {
                        p.getInventory().offerOrDrop(new ItemStack(ModBlocks.TROPHY_GOLD));
                        p.getInventory().offerOrDrop(new ItemStack(ModItems.MEDAL_GOLD));
                        p.getInventory().offerOrDrop(new ItemStack(Items.DIAMOND, 6));
                        p.sendMessage(Text.translatable("skijump.season.reward"), false);
                    }
                }
            }
            broadcast(server, Text.translatable("skijump.season.winner", winner));
        }
        season++;
        events = 0;
        points.clear();
        markDirty();
    }

    private static void broadcast(MinecraftServer server, Text t) {
        server.getPlayerManager().broadcast(t, false);
    }

    public static SeasonState fromNbt(NbtCompound nbt) {
        SeasonState s = new SeasonState();
        s.season = Math.max(1, nbt.getInt("season"));
        s.events = nbt.getInt("events");
        NbtList list = nbt.getList("points", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound c = list.getCompound(i);
            s.points.put(c.getString("n"), c.getDouble("p"));
        }
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("season", season);
        nbt.putInt("events", events);
        NbtList list = new NbtList();
        for (Map.Entry<String, Double> e : points.entrySet()) {
            NbtCompound c = new NbtCompound();
            c.putString("n", e.getKey());
            c.putDouble("p", e.getValue());
            list.add(c);
        }
        nbt.put("points", list);
        return nbt;
    }
}
