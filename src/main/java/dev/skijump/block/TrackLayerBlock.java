package dev.skijump.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Warstwowy blok toru/progu/znaczników (1-8 ósmych wysokości, jak śnieg). Dzięki temu profil skoczni
 * (rozbieg, próg, zeskok) jest gładki mimo siatki klocków. Fizyka skoku czyta wysokość z właściwości LAYERS.
 */
public class TrackLayerBlock extends Block {
    public static final IntProperty LAYERS = Properties.LAYERS;
    private static final VoxelShape[] SHAPES = new VoxelShape[9];

    static {
        for (int i = 1; i <= 8; i++) SHAPES[i] = Block.createCuboidShape(0, 0, 0, 16, i * 2, 16);
        SHAPES[0] = SHAPES[1];
    }

    public TrackLayerBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(LAYERS, 8));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(LAYERS)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(LAYERS)];
    }
}
