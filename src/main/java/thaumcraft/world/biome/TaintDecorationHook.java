package thaumcraft.world.biome;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import thaumcraft.registry.ModBlocks;
import thaumcraft.taint.TaintBiomes;

public final class TaintDecorationHook {
    private TaintDecorationHook() {
    }

    public static void decorate(WorldGenLevel level, RandomSource random, BlockPos chunkOrigin, net.minecraft.world.level.chunk.ChunkGenerator chunkGenerator) {
        int originX = chunkOrigin.getX();
        int originZ = chunkOrigin.getZ();
        int trees = 2 + (random.nextInt(10) == 0 ? 1 : 0);
        for (int tree = 0; tree < trees; tree++) {
            int x = originX + random.nextInt(16) + 8;
            int z = originZ + random.nextInt(16) + 8;
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
            if (random.nextInt(8) == 0) {
                new thaumcraft.world.tree.BigMagicTreeGenerator(false).generate(level, new java.util.Random(random.nextLong()), pos);
            } else {
                level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE)
                    .get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE, net.minecraft.resources.Identifier.withDefaultNamespace("oak")))
                    .ifPresent(feature -> feature.value().place(level, chunkGenerator, random, pos));
            }
        }
        int blobs = random.nextInt(3);
        for (int index = 0; index < blobs; index++) {
            int x = originX + random.nextInt(16) + 8;
            int z = originZ + random.nextInt(16) + 8;
            BlockPos center = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
            placeCrustBlob(level, random, center);
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
            BlockPos surface = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(surface);
            if (state.is(Blocks.GRASS_BLOCK) && level.getBiome(surface).is(TaintBiomes.TAINTED_LAND)) {
                level.setBlock(surface.above(), ModBlocks.FIBROUS_TAINT.get().defaultBlockState(), 2);
            } else if (state.canBeReplaced() && level.getBlockState(surface.below()).is(Blocks.GRASS_BLOCK)
                && level.getBiome(surface).is(TaintBiomes.TAINTED_LAND)) {
                level.setBlock(surface, ModBlocks.FIBROUS_TAINT.get().defaultBlockState(), 2);
            }
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
            if (!level.getBiome(pos).is(TaintBiomes.TAINTED_LAND)) {
                thaumcraft.platform.BiomeEdit.setWorldGenColumn(level, pos, level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).getOrThrow(TaintBiomes.TAINTED_LAND));
            }
            if (!level.isEmptyBlock(pos)) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (level.getBlockState(neighbor).isFaceSturdy(level, neighbor, direction.getOpposite())) {
                    level.setBlock(pos, ModBlocks.FIBROUS_TAINT.get().defaultBlockState(), 2);
                    break;
                }
            }
        }
    }

    private static void placeCrustBlob(WorldGenLevel level, RandomSource random, BlockPos center) {
        if (!level.getBiome(center).is(TaintBiomes.TAINTED_LAND)) {
            return;
        }
        int radius = 2 + random.nextInt(2);
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radius * radius + random.nextInt(3)) {
                        continue;
                    }
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK)) {
                        level.setBlock(pos, ModBlocks.CRUSTED_TAINT.get().defaultBlockState(), 2);
                    }
                }
            }
        }
    }
}
