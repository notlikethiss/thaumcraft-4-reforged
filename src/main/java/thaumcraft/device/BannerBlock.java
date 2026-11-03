package thaumcraft.device;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;
import thaumcraft.essentia.PhialItem;

public class BannerBlock extends Block implements EntityBlock {
    private static final VoxelShape FULL = Shapes.block();
    private static final VoxelShape POLE = Shapes.box(0.33, 0.0, 0.33, 0.66, 2.0, 0.66);
    private static final VoxelShape WALL_0 = Shapes.box(0.0, 0.0, 0.0, 1.0, 2.0, 0.25);
    private static final VoxelShape WALL_4 = Shapes.box(0.75, 0.0, 0.0, 1.0, 2.0, 1.0);
    private static final VoxelShape WALL_8 = Shapes.box(0.0, 0.0, 0.75, 1.0, 2.0, 1.0);
    private static final VoxelShape WALL_12 = Shapes.box(0.0, 0.0, 0.0, 0.25, 2.0, 1.0);

    public BannerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BannerBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!(level.getBlockEntity(pos) instanceof BannerBlockEntity banner)) {
            return FULL;
        }
        if (!banner.isOnWall()) {
            return POLE;
        }
        return switch (banner.getFacing()) {
            case 0 -> WALL_0;
            case 4 -> WALL_4;
            case 8 -> WALL_8;
            case 12 -> WALL_12;
            default -> POLE;
        };
    }

    public static ItemStack createDrop(BannerBlock block, BannerBlockEntity banner) {
        ItemStack drop = new ItemStack(block);
        if (banner.getColor() >= 0 || banner.getAspect() != null) {
            WardedKeys.BANNER_COLOR.set(drop, banner.getColor());
            if (banner.getAspect() != null) {
                WardedKeys.BANNER_ASPECT.set(drop, banner.getAspect());
            }
        }
        return drop;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof BannerBlockEntity banner ? createDrop(this, banner) : new ItemStack(this);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof BannerBlockEntity banner) {
            return List.of(createDrop(this, banner));
        }
        return List.of();
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative() && level.getBlockEntity(pos) instanceof BannerBlockEntity banner) {
            Block.popResource(level, pos, createDrop(this, banner));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        if (stack.getItem() instanceof PhialItem) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BannerBlockEntity banner && banner.getColor() >= 0) {
                AspectList aspects = AspectKeys.ASPECTS.get(stack);
                if (aspects != null && !aspects.isEmpty()) {
                    banner.setAspect(aspects.getAspects().get(0));
                    stack.shrink(1);
                }
                banner.sync();
                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundType.WOOL.getStepSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof BannerBlockEntity banner && banner.getColor() >= 0) {
            banner.setAspect(null);
            banner.sync();
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundType.WOOL.getStepSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            thaumcraft.blockentity.TcBlockEntity.beforeRemove(state, level, pos, newState);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
