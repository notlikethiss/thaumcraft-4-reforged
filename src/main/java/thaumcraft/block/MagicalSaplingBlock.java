package thaumcraft.block;

import java.util.Random;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.world.tree.GreatwoodTreeGenerator;
import thaumcraft.world.tree.SilverwoodTreeGenerator;

public class MagicalSaplingBlock extends VegetationBlock implements BonemealableBlock {
    public static final IntegerProperty STAGE = BlockStateProperties.STAGE;
    private static final VoxelShape SHAPE = Block.box(1.6, 0.0, 1.6, 14.4, 12.8, 14.4);
    public static final MapCodec<MagicalSaplingBlock> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(Codec.BOOL.fieldOf("silverwood").forGetter(MagicalSaplingBlock::isSilverwood), propertiesCodec()).apply(instance, MagicalSaplingBlock::new)
    );
    private final boolean silverwood;

    public MagicalSaplingBlock(boolean silverwood, Properties properties) {
        super(properties);
        this.silverwood = silverwood;
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected MapCodec<? extends VegetationBlock> codec() {
        return CODEC;
    }

    public boolean isSilverwood() {
        return silverwood;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return PlantSoil.PLAINS.canSustain(state, level, pos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return (level.getRawBrightness(pos, 0) >= 8 || level.canSeeSky(pos)) && super.canSurvive(state, level, pos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }
        if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(silverwood ? 50 : 25) == 0) {
            grow(level, pos, state, random);
        }
    }

    public void grow(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        Random generatorRandom = new Random(random.nextLong());
        boolean grown = silverwood
            ? new SilverwoodTreeGenerator(true, 7, 5).generate(level, generatorRandom, pos)
            : new GreatwoodTreeGenerator(true).generate(level, generatorRandom, pos, false);
        if (!grown) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return level.getRandom().nextFloat() < 0.45;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.cycle(STAGE), Block.UPDATE_NONE | 4);
        } else {
            grow(level, pos, state, random);
        }
    }
}
