package dev.skijump.block;

import dev.skijump.session.WindManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** Wiatromierz: właściwość STRENGTH (0-3) odzwierciedla aktualną siłę wiatru; prawy klik pokazuje dokładne wartości. */
public class AnemometerBlock extends Block {
    public static final IntProperty STRENGTH = IntProperty.of("strength", 0, 3);
    private static final VoxelShape SHAPE = Block.createCuboidShape(5, 0, 5, 11, 16, 11);

    public AnemometerBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(STRENGTH, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(STRENGTH);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!world.isClient) world.scheduleBlockTick(pos, this, 20);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        double speed = WindManager.get(world).speed();
        int s = speed < 1.5 ? 0 : speed < 3.5 ? 1 : speed < 6.0 ? 2 : 3;
        if (s != state.get(STRENGTH)) world.setBlockState(pos, state.with(STRENGTH, s), Block.NOTIFY_LISTENERS);
        world.scheduleBlockTick(pos, this, 20);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient && world instanceof ServerWorld sw) {
            WindManager.WindInfo w = WindManager.get(sw);
            player.sendMessage(Text.translatable("skijump.msg.wind", String.format("%.1f", w.speed()),
                    Text.translatable(WindManager.compassKey(w.dirRad())), String.format("%.1f", w.gust())), false);
        }
        return ActionResult.SUCCESS;
    }
}
