package thaumcraft.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
import thaumcraft.registry.ModSounds;

public class BrainJarBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);

    public BrainJarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrainJarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.BRAIN_JAR.get()) {
            return null;
        }
        if (level.isClientSide()) {
            return (tickLevel, pos, tickState, entity) -> BrainJarBlockEntity.clientTick(tickLevel, pos, tickState, (BrainJarBlockEntity) entity);
        }
        return (tickLevel, pos, tickState, entity) -> BrainJarBlockEntity.serverTick(tickLevel, pos, tickState, (BrainJarBlockEntity) entity);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BrainJarBlockEntity ? 2.0F : 0.0F;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof BrainJarBlockEntity jar) {
            jar.setEatDelay(40);
            if (!level.isClientSide()) {
                int released = level.getRandom().nextInt(Math.min(jar.getXp() + 1, 64));
                if (released > 0) {
                    jar.setXp(jar.getXp() - released);
                    jar.release(released);
                    jar.sync();
                }
                level.playSound(null, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, ModSounds.JAR.get(), SoundSource.BLOCKS, 0.2F, 1.0F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof BrainJarBlockEntity jar && jar.getXp() >= BrainJarBlockEntity.XP_MAX) {
            double x = pos.getX() + 0.3 + random.nextFloat() * 0.4F;
            double y = pos.getY() + 0.9;
            double z = pos.getZ() + 0.3 + random.nextFloat() * 0.4F;
            int green = (int) ((0.4F + random.nextFloat() * 0.1F) * 255.0F);
            int blue = (int) ((0.3F + random.nextFloat() * 0.2F) * 255.0F);
            level.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, FastColor.ARGB32.color(128, 0, green, blue)), x, y, z, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BrainJarBlockEntity jar ? jar.getComparatorSignal() : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            thaumcraft.blockentity.TcBlockEntity.beforeRemove(state, level, pos, newState);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
