package thaumcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import thaumcraft.blockentity.WandPedestalBlockEntity;
import thaumcraft.registry.ModBlockEntities;
import thaumcraft.registry.ModBlocks;
import thaumcraft.wand.VisStorage;

public class WandPedestalBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape COLLISION = Shapes.or(
        Block.box(0, 0, 0, 16, 4, 16),
        Block.box(4, 8, 4, 12, 16, 12),
        Block.box(2, 4, 2, 14, 8, 14)
    );

    public WandPedestalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WandPedestalBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.WAND_PEDESTAL.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> WandPedestalBlockEntity.serverTick(tickLevel, pos, tickState, (WandPedestalBlockEntity) entity);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof WandPedestalBlockEntity pedestal ? pedestal.getComparatorSignal() : 0;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WandPedestalBlockEntity pedestal) {
            BlockState above = level.getBlockState(pos.above());
            if (!above.isAir() && !above.is(ModBlocks.COMPOUND_RECHARGE_FOCUS.get()) && !pedestal.getStack().isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, pedestal.removeStack());
            }
        }
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        return interact(level, pos, player, hand, stack);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return interact(level, pos, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    public static InteractionResult interact(Level level, BlockPos pedestalPos, Player player, InteractionHand hand, ItemStack held) {
        if (held.is(ModBlocks.COMPOUND_RECHARGE_FOCUS.get().asItem())) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pedestalPos) instanceof WandPedestalBlockEntity pedestal)) {
            return InteractionResult.PASS;
        }
        if (!pedestal.getStack().isEmpty()) {
            if (!level.isClientSide()) {
                Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), pedestal.removeStack());
                level.playSound(null, pedestalPos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.7F + 1.0F) * 1.5F);
            }
            return InteractionResult.SUCCESS;
        }
        if (VisStorage.isStorage(held)) {
            if (!level.isClientSide()) {
                pedestal.setStack(held.copyWithCount(1));
                held.shrink(1);
                player.getInventory().setChanged();
                level.playSound(null, pedestalPos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.7F + 1.0F) * 1.6F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
