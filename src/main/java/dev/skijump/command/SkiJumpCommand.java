package dev.skijump.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.skijump.ModConfig;
import dev.skijump.career.CareerState;
import dev.skijump.comp.CompetitionManager;
import dev.skijump.hill.HillBuilder;
import dev.skijump.hill.HillPreset;
import dev.skijump.hill.HillRecord;
import dev.skijump.hill.HillState;
import dev.skijump.session.SessionManager;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Locale;

/** /skijump place|list|hills|top|rank|gate|comp|abort|remove|reload */
public final class SkiJumpCommand {
    private SkiJumpCommand() { }

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("skijump")
                .then(CommandManager.literal("place").requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .suggests((c, b) -> CommandSource.suggestMatching(HillPreset.ALL.stream().map(p -> p.id), b))
                                .executes(c -> place(c, ModConfig.get().hillScale))
                                .then(CommandManager.argument("scale", DoubleArgumentType.doubleArg(0.1, 2.0))
                                        .executes(c -> place(c, DoubleArgumentType.getDouble(c, "scale"))))))
                .then(CommandManager.literal("tutorial").executes(c -> {
                    dev.skijump.career.Tutorial.send(c.getSource().getPlayerOrThrow());
                    return 1;
                }))
                .then(CommandManager.literal("help").executes(c -> {
                    dev.skijump.career.Tutorial.send(c.getSource().getPlayerOrThrow());
                    return 1;
                }))
                .then(CommandManager.literal("guide")
                        .then(CommandManager.literal("on").executes(c -> setGuide(c, 1)))
                        .then(CommandManager.literal("off").executes(c -> setGuide(c, 2)))
                        .then(CommandManager.literal("auto").executes(c -> setGuide(c, 0))))
                .then(CommandManager.literal("rivals").executes(SkiJumpCommand::rivals))
                .then(CommandManager.literal("season").executes(SkiJumpCommand::season))
                .then(CommandManager.literal("locate").executes(SkiJumpCommand::locate))
                .then(CommandManager.literal("list").executes(SkiJumpCommand::list))
                .then(CommandManager.literal("hills").executes(SkiJumpCommand::hills))
                .then(CommandManager.literal("top").executes(c -> withHill(c, (p, h) -> CompetitionManager.sendBoard(p, h))))
                .then(CommandManager.literal("rank").executes(SkiJumpCommand::rank))
                .then(CommandManager.literal("abort").executes(c -> {
                    SessionManager.abort(c.getSource().getPlayerOrThrow());
                    return 1;
                }))
                .then(CommandManager.literal("gate").requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.argument("n", IntegerArgumentType.integer(1, HillRecord.MAX_GATE))
                                .executes(c -> {
                                    int n = IntegerArgumentType.getInteger(c, "n");
                                    return withHill(c, (p, h) -> {
                                        h.gate = n;
                                        HillState.get(p.getServer()).markDirty();
                                        p.sendMessage(Text.translatable("skijump.msg.gate_set", String.valueOf(n), String.valueOf(HillRecord.MAX_GATE)), false);
                                    });
                                })))
                .then(CommandManager.literal("remove").requires(s -> s.hasPermissionLevel(2)).executes(c -> withHill(c, (p, h) -> {
                    HillState.get(p.getServer()).remove(h);
                    p.sendMessage(Text.translatable("skijump.msg.hill_removed", h.name), false);
                })))
                .then(CommandManager.literal("reload").requires(s -> s.hasPermissionLevel(2)).executes(c -> {
                    ModConfig.load();
                    c.getSource().sendFeedback(() -> Text.translatable("skijump.msg.reloaded"), false);
                    return 1;
                }))
                .then(CommandManager.literal("comp")
                        .then(CommandManager.literal("start").executes(c -> withHill(c, CompetitionManager::start)))
                        .then(CommandManager.literal("join").executes(c -> withHill(c, CompetitionManager::join)))
                        .then(CommandManager.literal("leave").executes(c -> withHill(c, CompetitionManager::leave)))
                        .then(CommandManager.literal("status").executes(c -> withHill(c, CompetitionManager::status)))
                        .then(CommandManager.literal("cancel").requires(s -> s.hasPermissionLevel(2))
                                .executes(c -> withHill(c, (p, h) -> CompetitionManager.cancel(h))))));
    }

    private interface HillAction {
        void run(ServerPlayerEntity p, HillRecord h);
    }

    private static int withHill(CommandContext<ServerCommandSource> c, HillAction a) throws CommandSyntaxException {
        ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
        HillRecord h = HillState.get(p.getServer()).nearest(p.getServerWorld(), p.getBlockPos(), 160);
        if (h == null) {
            p.sendMessage(Text.translatable("skijump.msg.no_hill_near"), false);
            return 0;
        }
        a.run(p, h);
        return 1;
    }

    private static int place(CommandContext<ServerCommandSource> c, double scale) throws CommandSyntaxException {
        ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(c, "name");
        HillPreset preset = HillPreset.find(name);
        if (preset == null) {
            p.sendMessage(Text.translatable("skijump.msg.unknown_preset", name), false);
            return 0;
        }
        BlockPos edge = p.getBlockPos();
        Direction f = p.getHorizontalFacing();
        HillRecord rec = HillBuilder.start(p.getServerWorld(), preset, edge, f, scale, p.getUuid());
        p.sendMessage(Text.translatable("skijump.msg.hill_started", Text.translatable("skijump.hill." + preset.id), rec.id), false);
        return 1;
    }

    private static int setGuide(CommandContext<ServerCommandSource> c, int mode) throws CommandSyntaxException {
        ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
        CareerState cs = CareerState.get(p.getServer());
        cs.of(p.getUuid()).guideMode = mode;
        cs.markDirty();
        p.sendMessage(Text.translatable("skijump.guide.mode." + mode), false);
        return 1;
    }

    private static int rivals(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
        p.sendMessage(Text.translatable("skijump.rival.header"), false);
        for (dev.skijump.career.RivalState.Rival v : dev.skijump.career.RivalState.get(p.getServer()).rivals) {
            p.sendMessage(Text.translatable("skijump.rival.row", v.name, Text.translatable("skijump.rival.type." + v.type),
                    String.valueOf(Math.round(v.skill * 100)), String.valueOf(v.wins), String.valueOf(v.podiums), String.valueOf(v.meetings)), false);
        }
        return 1;
    }

    private static int season(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
        dev.skijump.career.SeasonState s = dev.skijump.career.SeasonState.get(p.getServer());
        p.sendMessage(Text.translatable("skijump.season.title", String.valueOf(s.season), String.valueOf(s.events),
                String.valueOf(ModConfig.get().seasonLength)), false);
        int i = 1;
        for (java.util.Map.Entry<String, Double> e : s.ranking()) {
            if (i > 8) break;
            p.sendMessage(Text.translatable("skijump.season.row", String.valueOf(i++), e.getKey(), String.format(Locale.US, "%.0f", e.getValue())), false);
        }
        if (s.points.isEmpty()) p.sendMessage(Text.translatable("skijump.season.empty"), false);
        return 1;
    }

    private static int locate(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
        net.minecraft.server.world.ServerWorld w = p.getServerWorld();
        HillRecord h = HillState.get(p.getServer()).nearest(w, p.getBlockPos(), 1.0e6);
        if (h != null) {
            double dist = Math.sqrt(h.edge.getSquaredDistance(p.getBlockPos()));
            p.sendMessage(Text.translatable("skijump.msg.locate_hill", CompetitionManager.hillName(h), String.valueOf(h.edge.getX()),
                    String.valueOf(h.edge.getY()), String.valueOf(h.edge.getZ()), String.valueOf((int) dist)), false);
        }
        try {
            BlockPos s = w.locateStructure(net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.STRUCTURE,
                    dev.skijump.SkiJumpMod.id("skijump_hill")), p.getBlockPos(), 100, false);
            if (s != null) {
                p.sendMessage(Text.translatable("skijump.msg.locate_structure", String.valueOf(s.getX()), String.valueOf(s.getZ()),
                        String.valueOf((int) Math.sqrt(s.getSquaredDistance(p.getBlockPos())))), false);
            } else if (h == null) {
                p.sendMessage(Text.translatable("skijump.msg.locate_none"), false);
            }
        } catch (Exception ex) {
            p.sendMessage(Text.translatable("skijump.msg.locate_none"), false);
        }
        return 1;
    }

    private static int list(CommandContext<ServerCommandSource> c) {
        ServerCommandSource s = c.getSource();
        s.sendFeedback(() -> Text.translatable("skijump.msg.list_header"), false);
        for (HillPreset p : HillPreset.ALL) {
            s.sendFeedback(() -> Text.translatable("skijump.msg.list_row", p.id, Text.translatable("skijump.hill." + p.id),
                    String.valueOf((int) p.k), String.valueOf((int) p.hs),
                    Text.translatable("skijump.rank." + CareerState.RANK_KEYS[Math.min(5, p.rank)])), false);
        }
        return 1;
    }

    private static int hills(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
        HillState st = HillState.get(p.getServer());
        if (st.all().isEmpty()) {
            p.sendMessage(Text.translatable("skijump.msg.no_hills"), false);
            return 0;
        }
        for (HillRecord h : st.all()) {
            p.sendMessage(Text.translatable("skijump.msg.hills_row", h.id, CompetitionManager.hillName(h),
                    String.valueOf((int) h.k), String.valueOf((int) h.hs),
                    h.edge.getX() + " " + h.edge.getY() + " " + h.edge.getZ(), String.valueOf(h.gate)), false);
        }
        return 1;
    }

    private static int rank(CommandContext<ServerCommandSource> c) throws CommandSyntaxException {
        ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
        CareerState.Data d = CareerState.get(p.getServer()).of(p.getUuid());
        int r = CareerState.rankOf(d.xp);
        String next = r + 1 < CareerState.RANK_XP.length ? String.valueOf(CareerState.RANK_XP[r + 1]) : "-";
        p.sendMessage(Text.translatable("skijump.msg.rank", Text.translatable("skijump.rank." + CareerState.RANK_KEYS[r]),
                String.valueOf(d.xp), next), false);
        p.sendMessage(Text.translatable("skijump.msg.stats", String.format(Locale.US, "%.0f", d.strength),
                String.format(Locale.US, "%.0f", d.balance), String.format(Locale.US, "%.0f", d.landing)), false);
        p.sendMessage(Text.translatable("skijump.msg.career", String.valueOf(d.jumps), String.format(Locale.US, "%.1f", d.bestDistance),
                String.valueOf(d.gold), String.valueOf(d.silver), String.valueOf(d.bronze)), false);
        return 1;
    }
}
