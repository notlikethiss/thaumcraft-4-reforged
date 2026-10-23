package thaumcraft.block;

import java.util.function.Supplier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public class MagicalLeavesBlock extends LeavesBlock {
    public static final MapCodec<MagicalLeavesBlock> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().<Supplier<? extends Block>>xmap(block -> () -> block, Supplier::get).fieldOf("sapling").forGetter(leaves -> leaves.sapling),
            Codec.INT.fieldOf("sapling_chance").forGetter(leaves -> leaves.saplingChance),
            Codec.BOOL.fieldOf("sparkle").forGetter(leaves -> leaves.sparkle),
            propertiesCodec()
        ).apply(instance, MagicalLeavesBlock::new)
    );
    private final Supplier<? extends Block> sapling;
    private final int saplingChance;
    private final boolean sparkle;

    public MagicalLeavesBlock(Supplier<? extends Block> sapling, int saplingChance, boolean sparkle, Properties properties) {
        super(0.0F, properties);
        this.sapling = sapling;
        this.saplingChance = saplingChance;
        this.sparkle = sparkle;
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return CODEC;
    }

    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (decaying(state)) {
            if (random.nextInt(saplingChance) == 0) {
                Block.popResource(level, pos, new ItemStack(sapling.get()));
            }
            level.removeBlock(pos, false);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (sparkle && random.nextInt(500) == 0) {
            level.addParticle(
                new DustParticleOptions(0xCCDDEE, 1.0F),
                pos.getX() + 0.5F + random.nextFloat() - random.nextFloat(),
                pos.getY() + 0.5F + random.nextFloat() - random.nextFloat(),
                pos.getZ() + 0.5F + random.nextFloat() - random.nextFloat(),
                0.0,
                0.0,
                0.0
            );
        }
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 30;
    }
}
