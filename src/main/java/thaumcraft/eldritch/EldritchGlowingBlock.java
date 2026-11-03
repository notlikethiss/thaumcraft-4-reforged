package thaumcraft.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class EldritchGlowingBlock extends EldritchBlock {
    public static final IntegerProperty BOUNDS = IntegerProperty.create("bounds", 0, 63);

    public EldritchGlowingBlock(int kind, Properties properties) {
        super(kind, properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BOUNDS);
    }

    private BlockState shapeFor(BlockState state, LevelReader level, BlockPos pos) {
        int bounds = 0;
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).isSolid()) {
                bounds |= 1 << direction.ordinal();
            }
        }
        return state.setValue(BOUNDS, bounds);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return shapeFor(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        BlockState updated = shapeFor(state, level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, 3);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return shapeFor(state, level, pos);
    }
}
