package thaumcraft.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;
import thaumcraft.registry.ModBlockEntities;
import thaumcraft.registry.ModItems;

public class EssentiaBufferBlock extends Block implements EntityBlock {
    public EssentiaBufferBlock(Properties properties) {
        super(properties);
        registerDefaultState(TubeArms.noArms(stateDefinition.any()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TubeArms.DOWN, TubeArms.UP, TubeArms.NORTH, TubeArms.SOUTH, TubeArms.WEST, TubeArms.EAST);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssentiaBufferBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.ESSENTIA_BUFFER.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> EssentiaBufferBlockEntity.tick(tickLevel, pos, tickState, (EssentiaBufferBlockEntity) entity);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return TubeArms.withArms(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (!level.isClientSide()) {
            TubeArms.refresh(level, pos);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction directionToNeighbour, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return state.setValue(TubeArms.property(directionToNeighbour), TubeArms.compute(level, pos, state, directionToNeighbour));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context.isHoldingItem(ModItems.WAND.get()) && level.getBlockEntity(pos) instanceof EssentiaBufferBlockEntity buffer) {
            return TubeArms.traceShape(buffer.traceBoxes());
        }
        return TubeArms.shape(state, 0.25, 0.75);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TubeArms.shape(state, 0.25, 0.75);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof EssentiaBufferBlockEntity buffer ? buffer.getComparatorSignal() : 0;
    }
}
