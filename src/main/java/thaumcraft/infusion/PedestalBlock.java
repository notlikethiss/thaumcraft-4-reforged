package thaumcraft.infusion;

import thaumcraft.blockentity.TcBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;
import thaumcraft.block.ItemInteractions;

public class PedestalBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 15.84, 12.0);

    public PedestalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PedestalBlockEntity(pos, state);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity entity = level.getBlockEntity(pos);
        return entity != null && entity.triggerEvent(id, param);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal ? pedestal.getComparatorSignal() : 0;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (!level.isClientSide() && !level.getBlockState(pos.above()).isAir() && level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) {
            pedestal.dropContents();
        }
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        return ItemInteractions.skipping(interact(level, pos, player, hand, stack));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return interact(level, pos, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    private static InteractionResult interact(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack held) {
        if (!(level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal)) {
            return InteractionResult.PASS;
        }
        if (!pedestal.getStack().isEmpty()) {
            if (!level.isClientSide()) {
                Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), pedestal.removeStack());
                playPop(level, pos, 1.5F);
            }
            return InteractionResult.SUCCESS;
        }
        if (!held.isEmpty()) {
            if (!level.isClientSide()) {
                pedestal.setStack(held.copyWithCount(1));
                held.shrink(1);
                player.getInventory().setChanged();
                playPop(level, pos, 1.6F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static void playPop(Level level, BlockPos pos, float multiplier) {
        RandomSource random = level.getRandom();
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2F, ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * multiplier);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        TcBlockEntity.beforeRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
