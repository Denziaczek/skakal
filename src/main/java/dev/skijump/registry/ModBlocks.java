package dev.skijump.registry;

import dev.skijump.SkiJumpMod;
import dev.skijump.block.AnemometerBlock;
import dev.skijump.block.InfoBlock;
import dev.skijump.block.StartGateBlock;
import dev.skijump.block.TrackLayerBlock;
import dev.skijump.block.TrophyBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

import java.util.ArrayList;
import java.util.List;

public final class ModBlocks {
    public static final List<Block> ALL = new ArrayList<>();

    public static final Block START_GATE = reg("start_gate", new StartGateBlock(
            AbstractBlock.Settings.create().strength(2.0f, 6.0f).sounds(BlockSoundGroup.WOOD).nonOpaque()));
    public static final Block INRUN_TRACK = reg("inrun_track", new TrackLayerBlock(
            AbstractBlock.Settings.create().strength(1.0f, 3.0f).sounds(BlockSoundGroup.GLASS).nonOpaque().slipperiness(0.98f)));
    public static final Block TAKEOFF_TABLE = reg("takeoff_table", new TrackLayerBlock(
            AbstractBlock.Settings.create().strength(1.5f, 4.0f).sounds(BlockSoundGroup.STONE).nonOpaque()));
    public static final Block K_MARKER = reg("k_marker", new TrackLayerBlock(
            AbstractBlock.Settings.create().strength(1.0f, 3.0f).sounds(BlockSoundGroup.SNOW).nonOpaque()));
    public static final Block HS_MARKER = reg("hs_marker", new TrackLayerBlock(
            AbstractBlock.Settings.create().strength(1.0f, 3.0f).sounds(BlockSoundGroup.SNOW).nonOpaque()));
    public static final Block ANEMOMETER = reg("anemometer", new AnemometerBlock(
            AbstractBlock.Settings.create().strength(1.0f, 2.0f).sounds(BlockSoundGroup.METAL).nonOpaque()));
    public static final Block SCOREBOARD = reg("scoreboard", new InfoBlock(InfoBlock.Kind.SCOREBOARD,
            AbstractBlock.Settings.create().strength(2.0f, 6.0f).sounds(BlockSoundGroup.METAL)));
    public static final Block JUDGE_TOWER = reg("judge_tower", new InfoBlock(InfoBlock.Kind.JUDGES,
            AbstractBlock.Settings.create().strength(2.0f, 6.0f).sounds(BlockSoundGroup.WOOD)));
    public static final Block COMPETITION_STATION = reg("competition_station", new InfoBlock(InfoBlock.Kind.STATION,
            AbstractBlock.Settings.create().strength(2.0f, 6.0f).sounds(BlockSoundGroup.METAL)));
    public static final Block TROPHY_GOLD = reg("trophy_gold", new TrophyBlock(
            AbstractBlock.Settings.create().strength(1.0f, 4.0f).sounds(BlockSoundGroup.METAL).nonOpaque()));
    public static final Block TROPHY_SILVER = reg("trophy_silver", new TrophyBlock(
            AbstractBlock.Settings.create().strength(1.0f, 4.0f).sounds(BlockSoundGroup.METAL).nonOpaque()));
    public static final Block TROPHY_BRONZE = reg("trophy_bronze", new TrophyBlock(
            AbstractBlock.Settings.create().strength(1.0f, 4.0f).sounds(BlockSoundGroup.METAL).nonOpaque()));

    private static Block reg(String name, Block block) {
        Registry.register(Registries.BLOCK, SkiJumpMod.id(name), block);
        Registry.register(Registries.ITEM, SkiJumpMod.id(name), new BlockItem(block, new FabricItemSettings()));
        ALL.add(block);
        return block;
    }

    public static void init() { }

    private ModBlocks() { }
}
