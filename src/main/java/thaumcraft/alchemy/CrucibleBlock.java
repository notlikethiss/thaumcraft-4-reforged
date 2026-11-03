package thaumcraft.alchemy;

import thaumcraft.blockentity.TcBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;
import thaumcraft.registry.ModBlockEntities;
import thaumcraft.wand.Wands;

public class CrucibleBlock extends Block implements EntityBlock {
    public static final String SPECIAL_ITEM_TAG = "thaumcraft_special_item";
    private static final VoxelShape COLLISION = Shapes.or(
        Block.box(0, 0, 0, 16, 5, 16),
        Block.box(0, 0, 0, 2, 13.6, 16),
        Block.box(0, 0, 0, 16, 13.6, 2),
        Block.box(14, 0, 0, 16, 13.6, 16),
        Block.box(0, 0, 14, 16, 13.6, 16)
    );

    public CrucibleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)) {
            return;
        }
        boolean hot = crucible.getHeat() > CrucibleBlockEntity.BOIL_HEAT && crucible.getWaterAmount() > 0;
        if (entity instanceof ItemEntity item && !item.getTags().contains(SPECIAL_ITEM_TAG) && hot) {
            crucible.attemptSmelt(item);
            return;
        }
        if (!crucible.tickContactDelay()) {
            return;
        }
        if (entity instanceof LivingEntity living && hot && level instanceof ServerLevel serverLevel) {
            living.hurt(level.damageSources().inFire(), 1.0F);
            level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.4F, 2.0F + level.getRandom().nextFloat() * 0.4F);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        if (Wands.isWand(stack)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(Items.WATER_BUCKET) && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
            if (!level.isClientSide() && crucible.getWaterAmount() < CrucibleBlockEntity.TANK_CAPACITY) {
                crucible.fillWater(1000);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                RandomSource random = level.getRandom();
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, 0.33F, 1.0F + (random.nextFloat() - random.nextFloat()) * 0.3F);
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
            crucible.resetBellows();
        }
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0 && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible
            && crucible.getWaterAmount() > 0 && crucible.getHeat() > CrucibleBlockEntity.BOIL_HEAT) {
            level.playLocalSound(pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.1F + random.nextFloat() * 0.1F, 1.2F + random.nextFloat() * 0.2F, false);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
            int size = crucible.getAspects().visSize();
            return (int) Math.floor(size / 100.0F * 14.0F) + (size > 0 ? 1 : 0);
        }
        return 0;
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity entity = level.getBlockEntity(pos);
        return entity != null && entity.triggerEvent(id, param);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.CRUCIBLE.get()) {
            return null;
        }
        if (level.isClientSide()) {
            return (tickLevel, pos, tickState, entity) -> CrucibleBlockEntity.clientTick(tickLevel, pos, (CrucibleBlockEntity) entity);
        }
        return (tickLevel, pos, tickState, entity) -> CrucibleBlockEntity.serverTick(tickLevel, pos, (CrucibleBlockEntity) entity);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        TcBlockEntity.beforeRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
