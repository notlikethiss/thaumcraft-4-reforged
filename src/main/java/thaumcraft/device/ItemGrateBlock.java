package thaumcraft.device;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;

public class ItemGrateBlock extends Block implements EntityBlock {
    public static final BooleanProperty CLOSED = BooleanProperty.create("closed");
    private static final VoxelShape SHAPE = Block.box(0.0, 13.0, 0.0, 16.0, 16.0, 16.0);

    public ItemGrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CLOSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CLOSED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(CLOSED)) {
            return SHAPE;
        }
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() != null && !(entityContext.getEntity() instanceof ItemEntity)) {
            return SHAPE;
        }
        return Shapes.empty();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        toggle(level, pos, state);
        return InteractionResult.SUCCESS;
    }

    private static void toggle(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide()) {
            level.setBlock(pos, state.cycle(CLOSED), Block.UPDATE_CLIENTS);
            playToggleSound(level, pos);
        }
    }

    private static void playToggleSound(Level level, BlockPos pos) {
        boolean open = level.getRandom().nextFloat() < 0.5F;
        level.playSound(
            null,
            pos,
            open ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE,
            SoundSource.BLOCKS,
            1.0F,
            level.getRandom().nextFloat() * 0.1F + 0.9F
        );
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (!level.isClientSide()) {
            boolean powered = level.hasNeighborSignal(pos);
            if (powered || block.defaultBlockState().isSignalSource()) {
                if (!state.getValue(CLOSED) && powered) {
                    level.setBlock(pos, state.setValue(CLOSED, true), Block.UPDATE_CLIENTS);
                    playToggleSound(level, pos);
                } else if (state.getValue(CLOSED) && !powered) {
                    level.setBlock(pos, state.setValue(CLOSED, false), Block.UPDATE_CLIENTS);
                    playToggleSound(level, pos);
                }
            }
        }
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemGrateBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            thaumcraft.blockentity.TcBlockEntity.beforeRemove(state, level, pos, newState);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
