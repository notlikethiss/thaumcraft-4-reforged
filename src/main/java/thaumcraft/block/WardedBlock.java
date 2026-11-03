package thaumcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;
import thaumcraft.blockentity.WardedBlockEntity;

public class WardedBlock extends Block implements EntityBlock {
    public WardedBlock(Properties properties) {
        super(properties);
    }

    public static BlockState getDisguise(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof WardedBlockEntity warded ? warded.getDisguise() : Blocks.STONE.defaultBlockState();
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof WardedBlockEntity warded ? warded.getLight() : 0;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(getDisguise(level, pos).getBlock().asItem());
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        BlockState disguise = getDisguise(level, pos);
        if (!disguise.is(this)) {
            disguise.attack(level, pos, player);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        BlockState disguise = getDisguise(level, pos);
        if (!disguise.is(this)) {
            disguise.getBlock().stepOn(level, pos, disguise, entity);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        BlockState disguise = getDisguise(level, pos);
        if (disguise.is(this)) {
            super.fallOn(level, state, pos, entity, fallDistance);
        } else {
            disguise.getBlock().fallOn(level, disguise, pos, entity, fallDistance);
        }
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WardedBlockEntity(pos, state);
    }
}
