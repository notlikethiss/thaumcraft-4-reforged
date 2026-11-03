package thaumcraft.device;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nullable;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModSounds;
import thaumcraft.wand.WandInteractable;
import thaumcraft.wand.WandItem;

public class ArcaneDoorBlock extends DoorBlock implements EntityBlock, WandInteractable {
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    public ArcaneDoorBlock(Properties properties) {
        super(BlockSetType.IRON, properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OwnedBlockEntity(pos, state);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        OwnedBlockEntity owned = OwnedBlockEntity.at(level, pos);
        if (stack.getItem() instanceof WandItem && owned != null && owned.isOwner(player.getUUID())) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.ARCANE_DOOR_ITEM.get());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        OwnedBlockEntity owned = OwnedBlockEntity.at(level, pos);
        if (owned == null) {
            return InteractionResult.SUCCESS;
        }
        if (!owned.isOwnerOrHasAccess(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("tc.device.door_refuses"));
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.DOORFAIL.get(), SoundSource.BLOCKS, 0.66F, 1.0F);
        } else {
            BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            BlockState lowerState = level.getBlockState(lower);
            if (lowerState.is(this)) {
                level.setBlock(lower, lowerState.cycle(OPEN), 10);
                playDoorSound(level, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static void playDoorSound(Level level, BlockPos pos) {
        boolean open = Math.random() < 0.5;
        level.playSound(
            null,
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5,
            open ? BlockSetType.OAK.doorOpen() : BlockSetType.OAK.doorClose(),
            SoundSource.BLOCKS,
            1.0F,
            level.getRandom().nextFloat() * 0.1F + 0.9F
        );
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (level.isClientSide() || block != ModBlocks.ARCANE_PRESSURE_PLATE.get()) {
            return;
        }
        List<UUID> users = new ArrayList<>();
        OwnedBlockEntity owned = OwnedBlockEntity.at(level, pos);
        if (owned != null) {
            if (owned.getOwner() != null) {
                users.add(owned.getOwner());
            }
            for (OwnedBlockEntity.Access access : owned.getAccessList()) {
                users.add(access.player());
            }
        }
        int open = 0;
        for (Direction direction : HORIZONTAL) {
            BlockPos platePos = pos.relative(direction);
            BlockState plate = level.getBlockState(platePos);
            if (!plate.is(ModBlocks.ARCANE_PRESSURE_PLATE.get())) {
                continue;
            }
            OwnedBlockEntity plateOwned = OwnedBlockEntity.at(level, platePos);
            if (plateOwned == null || plateOwned.getOwner() == null || !users.contains(plateOwned.getOwner())) {
                continue;
            }
            if (plate.getValue(ArcanePressurePlateBlock.POWERED)) {
                open = 1;
                break;
            }
            open = -1;
        }
        if (open != 0) {
            setPowered(level, pos, state, open == 1);
        }
    }

    private void setPowered(Level level, BlockPos pos, BlockState state, boolean open) {
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockState lowerState = level.getBlockState(lower);
        if (lowerState.is(this) && lowerState.getValue(OPEN) != open) {
            level.setBlock(lower, lowerState.setValue(OPEN, open), 10);
            playDoorSound(level, pos);
        }
    }

    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
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
            level.addFreshEntity(new ItemEntity(
                level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModBlocks.ARCANE_DOOR_ITEM.get())
            ));
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
