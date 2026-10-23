package thaumcraft.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.BlockGetter;
import org.jspecify.annotations.Nullable;
import thaumcraft.fx.Fx;
import thaumcraft.registry.ModBlockEntities;

public class ArcaneLevitatorBlock extends Block implements EntityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ArcaneLevitatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(POWERED, isGettingPower(context.getLevel(), context.getClickedPos()));
    }

    public static boolean isGettingPower(Level level, BlockPos pos) {
        return level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null && direction.getAxis().isHorizontal();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof ArcaneLevitatorBlockEntity levitator && !isGettingPower(level, pos) && levitator.getRangeAbove() > 0) {
            Fx.get().sparkle(level, pos.getX() + 0.2F + random.nextFloat() * 0.6F, pos.getY() + 1, pos.getZ() + 0.2F + random.nextFloat() * 0.6F, 1.0F, 3, -0.3F);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        updateLifterStack(level, pos);
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        updateLifterStack(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) {
            boolean powered = isGettingPower(level, pos);
            if (level.getBlockEntity(pos) instanceof ArcaneLevitatorBlockEntity levitator && powered != levitator.getLastPowerState()) {
                updateLifterStack(level, pos);
            }
            if (state.getValue(POWERED) != powered) {
                level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            }
        }
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    }

    private void updateLifterStack(Level level, BlockPos pos) {
        for (int count = 1; level.getBlockState(pos.below(count)).is(this); count++) {
            if (level.getBlockEntity(pos.below(count)) instanceof ArcaneLevitatorBlockEntity levitator) {
                levitator.requireUpdate();
            }
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneLevitatorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.ARCANE_LEVITATOR.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> ArcaneLevitatorBlockEntity.tick(tickLevel, pos, (ArcaneLevitatorBlockEntity) entity);
    }
}
