package thaumcraft.world.biome;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.Thaumcraft;
import thaumcraft.block.ManaPodBlock;
import thaumcraft.blockentity.ManaPodBlockEntity;
import thaumcraft.registry.ModBlocks;
import thaumcraft.world.tree.BigMagicTreeGenerator;
import thaumcraft.world.tree.GreatwoodTreeGenerator;
import thaumcraft.world.tree.SilverwoodTreeGenerator;

public class MagicalForestDecorationFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceKey<Biome> BIOME = ResourceKey.create(
        Registries.BIOME,
        Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "magical_forest")
    );

    public MagicalForestDecorationFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    private static boolean inBiome(WorldGenLevel level, BlockPos pos) {
        return level.getBiome(pos).is(BIOME);
    }

    private static BlockPos surface(WorldGenLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        RandomSource randomSource = context.random();
        BlockPos origin = context.origin();
        Random random = new Random(randomSource.nextLong());
        int x = origin.getX();
        int z = origin.getZ();
        placeTrees(level, random, x, z);
        int blobs = random.nextInt(3);
        for (int index = 0; index < blobs; index++) {
            BlockPos pos = surface(level, x + random.nextInt(16), z + random.nextInt(16));
            if (inBiome(level, pos)) {
                placeBlob(level, random, pos);
            }
        }
        for (int cellX = 0; cellX < 4; cellX++) {
            for (int cellZ = 0; cellZ < 4; cellZ++) {
                BlockPos pos = surface(level, x + cellX * 4 + 1 + random.nextInt(3), z + cellZ * 4 + 1 + random.nextInt(3));
                if (random.nextInt(40) == 0 && inBiome(level, pos)) {
                    placeMushroom(level, chunkGenerator, randomSource, random, pos);
                }
            }
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            placeManaPod(level, random, x + random.nextInt(16), 64, z + random.nextInt(16));
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            placeVishroom(level, x + random.nextInt(16), z + random.nextInt(16));
        }
        return true;
    }

    private static void placeTrees(WorldGenLevel level, Random random, int x, int z) {
        int count = 2;
        if (random.nextInt(10) == 0) {
            count++;
        }
        for (int index = 0; index < count; index++) {
            BlockPos pos = surface(level, x + random.nextInt(16), z + random.nextInt(16));
            if (!inBiome(level, pos)) {
                continue;
            }
            if (random.nextInt(14) == 0) {
                new SilverwoodTreeGenerator(false, 8, 5).generate(level, random, pos);
            } else if (random.nextInt(10) == 0) {
                new GreatwoodTreeGenerator(false).generate(level, random, pos);
            } else {
                new BigMagicTreeGenerator(false).generate(level, random, pos);
            }
        }
    }

    private static void placeBlob(WorldGenLevel level, Random random, BlockPos start) {
        BlockPos.MutableBlockPos pos = start.mutable();
        while (pos.getY() > level.getMinY() + 3) {
            BlockState below = level.getBlockState(pos.below());
            if (below.is(BlockTags.DIRT) || below.is(BlockTags.BASE_STONE_OVERWORLD)) {
                break;
            }
            pos.move(0, -1, 0);
        }
        if (pos.getY() <= level.getMinY() + 3) {
            return;
        }
        BlockState mossy = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        for (int pass = 0; pass < 3; pass++) {
            int radiusX = random.nextInt(2);
            int radiusY = random.nextInt(2);
            int radiusZ = random.nextInt(2);
            float limit = (radiusX + radiusY + radiusZ) * 0.333F + 0.5F;
            BlockPos center = pos.immutable();
            for (BlockPos target : BlockPos.betweenClosed(center.offset(-radiusX, -radiusY, -radiusZ), center.offset(radiusX, radiusY, radiusZ))) {
                double dx = target.getX() - center.getX();
                double dy = target.getY() - center.getY();
                double dz = target.getZ() - center.getZ();
                if (dx * dx + dy * dy + dz * dz <= limit * limit) {
                    level.setBlock(target, mossy, Block.UPDATE_CLIENTS);
                }
            }
            pos.move(-1 + random.nextInt(2), -random.nextInt(2), -1 + random.nextInt(2));
        }
    }

    private static void placeMushroom(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource randomSource, Random random, BlockPos pos) {
        String name = random.nextBoolean() ? "huge_red_mushroom" : "huge_brown_mushroom";
        ResourceKey<ConfiguredFeature<?, ?>> key = ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.withDefaultNamespace(name));
        level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(key).ifPresent(reference -> reference.value().place(level, chunkGenerator, randomSource, pos));
    }

    private static void placeManaPod(WorldGenLevel level, Random random, int startX, int startY, int startZ) {
        int x = startX;
        int z = startZ;
        int limit = Math.min(128, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z));
        for (int y = startY; y < limit; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.isEmptyBlock(pos) && level.isEmptyBlock(pos.below())) {
                if (ManaPodBlock.canHangFrom(level, pos.above())) {
                    BlockState state = ModBlocks.MANA_POD.get().defaultBlockState().setValue(ManaPodBlock.AGE, 2 + random.nextInt(5));
                    level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                    if (level.getBlockEntity(pos) instanceof ManaPodBlockEntity pod) {
                        pod.checkGrowth();
                    }
                    return;
                }
            } else {
                x = startX + random.nextInt(4) - random.nextInt(4);
                z = startZ + random.nextInt(4) - random.nextInt(4);
            }
        }
    }

    private static void placeVishroom(WorldGenLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        while (y > 50 && !level.getBlockState(new BlockPos(x, y, z)).is(Blocks.GRASS_BLOCK)) {
            y--;
        }
        BlockPos ground = new BlockPos(x, y, z);
        BlockPos pos = ground.above();
        if (!level.getBlockState(ground).is(Blocks.GRASS_BLOCK) || !level.getBlockState(pos).canBeReplaced() || !inBiome(level, pos)) {
            return;
        }
        for (BlockPos neighbour : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (!neighbour.equals(pos) && level.getBlockState(neighbour).is(BlockTags.LOGS)) {
                level.setBlock(pos, ModBlocks.VISHROOM.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                return;
            }
        }
    }
}
