package thaumcraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CustomPlantBlock extends VegetationBlock {
    public enum Kind {
        SHIMMERLEAF(PlantSoil.PLAINS),
        CINDERPEARL(PlantSoil.DESERT),
        ETHEREAL_BLOOM(PlantSoil.CAVE),
        VISHROOM(PlantSoil.CAVE);

        private final PlantSoil soil;

        Kind(PlantSoil soil) {
            this.soil = soil;
        }
    }

    public static final MapCodec<CustomPlantBlock> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(Codec.STRING.xmap(Kind::valueOf, Kind::name).fieldOf("kind").forGetter(CustomPlantBlock::getKind), propertiesCodec()).apply(instance, CustomPlantBlock::new)
    );
    private static final VoxelShape SHAPE = Block.box(1.6, 0.0, 1.6, 14.4, 12.8, 14.4);
    private final Kind kind;

    public CustomPlantBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected MapCodec<? extends VegetationBlock> codec() {
        return CODEC;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return kind.soil.canSustain(state, level, pos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return (level.getRawBrightness(pos, 0) >= 8 || level.canSeeSky(pos)) && super.canSurvive(state, level, pos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (kind == Kind.VISHROOM && !level.isClientSide() && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
        }
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (kind == Kind.SHIMMERLEAF && random.nextInt(3) == 0) {
            int color = (int) ((0.3F + random.nextFloat() * 0.3F) * 255.0F) << 16
                | (int) ((0.7F + random.nextFloat() * 0.3F) * 255.0F) << 8
                | (int) ((0.7F + random.nextFloat() * 0.3F) * 255.0F);
            level.addParticle(
                new DustParticleOptions(color, 0.6F),
                pos.getX() + 0.5F + (random.nextFloat() - random.nextFloat()) * 0.1F,
                pos.getY() + 0.5F + (random.nextFloat() - random.nextFloat()) * 0.15F,
                pos.getZ() + 0.5F + (random.nextFloat() - random.nextFloat()) * 0.1F,
                0.0,
                0.0,
                0.0
            );
        } else if (kind == Kind.CINDERPEARL && random.nextBoolean()) {
            double x = pos.getX() + 0.5F + (random.nextFloat() - random.nextFloat()) * 0.1F;
            double y = pos.getY() + 0.6F + (random.nextFloat() - random.nextFloat()) * 0.1F;
            double z = pos.getZ() + 0.5F + (random.nextFloat() - random.nextFloat()) * 0.1F;
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
        } else if (kind == Kind.VISHROOM && random.nextInt(3) == 0) {
            level.addParticle(
                new DustParticleOptions(0x804CCC, 0.5F),
                pos.getX() + 0.5F + (random.nextFloat() - random.nextFloat()) * 0.4F,
                pos.getY() + 0.3F,
                pos.getZ() + 0.5F + (random.nextFloat() - random.nextFloat()) * 0.4F,
                0.0,
                -0.015,
                0.0
            );
        }
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 100;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }
}
