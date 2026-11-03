package thaumcraft.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;
import thaumcraft.wand.WandInteractable;

public class WardedGlassBlock extends TransparentBlock implements EntityBlock, WandInteractable {
    public WardedGlassBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OwnedBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        OwnedBlockEntity owned = OwnedBlockEntity.at(level, pos);
        if (owned != null && by instanceof Player player) {
            owned.setOwner(player.getUUID());
        }
        super.setPlacedBy(level, pos, state, by, stack);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
    }

    @Override
    public InteractionResult onWandUseOn(Level level, BlockPos pos, Player player, ItemStack wand, Direction side) {
        OwnedBlockEntity owned = OwnedBlockEntity.at(level, pos);
        if (WardedConfig.WARDED_STONE && (owned == null || !owned.isOwner(player.getUUID()))) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            BlockState state = level.getBlockState(pos);
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(this)));
            level.levelEvent(2001, pos, Block.getId(state));
            level.removeBlock(pos, false);
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
