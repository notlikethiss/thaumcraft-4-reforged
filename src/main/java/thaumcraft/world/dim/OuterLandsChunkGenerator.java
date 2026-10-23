package thaumcraft.world.dim;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class OuterLandsChunkGenerator extends ChunkGenerator {
    public static final MapCodec<OuterLandsChunkGenerator> CODEC = BiomeSource.CODEC
        .fieldOf("biome_source")
        .xmap(OuterLandsChunkGenerator::new, generator -> generator.biomeSource)
        .stable();

    public OuterLandsChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return 256;
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState randomState, ChunkAccess protoChunk) {
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess centerChunk) {
        return CompletableFuture.completedFuture(centerChunk);
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
        return heightAccessor.getMinY();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        BlockState[] blocks = new BlockState[heightAccessor.getHeight()];
        java.util.Arrays.fill(blocks, Blocks.AIR.defaultBlockState());
        return new NoiseColumn(heightAccessor.getMinY(), blocks);
    }

    @Override
    public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos) {
    }
}
