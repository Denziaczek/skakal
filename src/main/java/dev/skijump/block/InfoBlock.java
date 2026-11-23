package dev.skijump.block;

import dev.skijump.comp.CompetitionManager;
import dev.skijump.hill.HillRecord;
import dev.skijump.hill.HillState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Tablica wyników, wieża sędziowska i stacja zawodów (interakcja prawym klikiem). */
public class InfoBlock extends Block {
    public enum Kind { SCOREBOARD, JUDGES, STATION }

    private final Kind kind;

    public InfoBlock(Kind kind, Settings settings) {
        super(settings);
        this.kind = kind;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(world instanceof ServerWorld sw) || !(player instanceof ServerPlayerEntity sp) || hand != Hand.MAIN_HAND) {
            return ActionResult.CONSUME;
        }
        HillRecord hill = HillState.get(sw.getServer()).nearest(sw, pos, 160);
        if (hill == null) {
            sp.sendMessage(Text.translatable("skijump.msg.no_hill_near"), true);
            return ActionResult.CONSUME;
        }
        switch (kind) {
            case SCOREBOARD -> CompetitionManager.sendBoard(sp, hill);
            case JUDGES -> CompetitionManager.sendJudgeInfo(sp, hill);
            case STATION -> CompetitionManager.stationUse(sp, hill);
        }
        return ActionResult.CONSUME;
    }
}
