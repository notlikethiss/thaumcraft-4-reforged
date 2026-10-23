package thaumcraft.world.gen;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.common.Tags;
import thaumcraft.Thaumcraft;
import thaumcraft.registry.ModBlocks;
import thaumcraft.Config;
import thaumcraft.world.tree.GreatwoodTreeGenerator;
import thaumcraft.world.tree.SilverwoodTreeGenerator;

public class ThaumcraftWorldGenFeature extends Feature<NoneFeatureConfiguration> {
    public ThaumcraftWorldGenFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        BlockPos origin = context.origin();
        Random random = new Random(context.random().nextLong());
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        boolean flat = chunkGenerator instanceof FlatLevelSource;
        if (level.getLevel().dimension() == Level.NETHER) {
            generateNether(level, random, chunkX, chunkZ, flat);
        } else if (level.getLevel().dimension() == Level.OVERWORLD) {
            generateSurface(level, random, chunkX, chunkZ, flat);
        }
        return true;
    }

    private static int height(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    private static void generateNether(WorldGenLevel level, Random random, int chunkX, int chunkZ, boolean flat) {
        if (!flat) {
            NodeHooks.netherTotem(level, random, chunkX, chunkZ);
        }
        NodeHooks.netherWildNode(level, random, chunkX, chunkZ);
    }

    private static void generateSurface(WorldGenLevel level, Random random, int chunkX, int chunkZ, boolean flat) {
        boolean auraGen = false;
        if (Config.genTrees() && !flat) {
            generateVegetation(level, random, chunkX, chunkZ);
        }
        OreGeneration.generate(level, random, chunkX, chunkZ);
        if (Config.genAura()) {
            if (NodeHooks.templeNode(level, random, chunkX, chunkZ)) {
                auraGen = true;
            }
            auraGen = NodeHooks.wildNode(level, random, chunkX, chunkZ, auraGen);
        }
        if (Config.genStructure() && !flat) {
            int x = chunkX * 16 + random.nextInt(16) - 8;
            int z = chunkZ * 16 + random.nextInt(16) - 8;
            int y = height(level, x, z) - 9;
            if (random.nextInt(150) == 0) {
                if (MoundGenerator.generate(level, random, x, y, z)) {
                    auraGen = true;
                    NodeHooks.moundNode(level, random, x + 9, y + 8, z + 9);
                }
            } else if (random.nextInt(66) == 0) {
                if (NodeHooks.eldritchRing(level, random, chunkX, chunkZ, x, y + 8, z)) {
                    auraGen = true;
                }
            } else if (random.nextInt(40) == 0) {
                y += 9;
                if (HilltopStonesGenerator.generate(level, random, x, y, z)) {
                    auraGen = true;
                    NodeHooks.hilltopNode(level, random, x, y + 5, z);
                }
            }
            NodeHooks.totem(level, random, chunkX, chunkZ, auraGen);
        }
    }

    private static void generateVegetation(WorldGenLevel level, Random random, int chunkX, int chunkZ) {
        if (random.nextInt(60) == 3) {
            generateSilverwood(level, random, chunkX, chunkZ);
        }
        if (random.nextInt(25) == 7) {
            generateGreatwood(level, random, chunkX, chunkZ);
        }
        int x = chunkX * 16 + random.nextInt(16);
        int z = chunkZ * 16 + random.nextInt(16);
        int y = height(level, x, z);
        Holder<Biome> biome = level.getBiome(new BlockPos(x, y, z));
        boolean sandy = biome.is(Tags.Biomes.IS_DESERT) || biome.is(Tags.Biomes.IS_BADLANDS);
        if (sandy && biome.value().getBaseTemperature() > 1.0F && random.nextInt(30) == 0) {
            generateFlowers(level, random, x, y, z, ModBlocks.CINDERPEARL.get().defaultBlockState());
        }
    }

    public static boolean generateFlowers(WorldGenLevel level, Random random, int x, int y, int z, BlockState flower) {
        for (int i = 0; i < 18; i++) {
            int flowerX = x + random.nextInt(8) - random.nextInt(8);
            int flowerY = y + random.nextInt(4) - random.nextInt(4);
            int flowerZ = z + random.nextInt(8) - random.nextInt(8);
            BlockState below = LegacyGen.get(level, flowerX, flowerY - 1, flowerZ);
            if (LegacyGen.isAir(level, flowerX, flowerY, flowerZ) && (below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.SAND))) {
                LegacyGen.set(level, flowerX, flowerY, flowerZ, flower);
            }
        }
        return true;
    }

    public static boolean supportsSilverwood(Holder<Biome> biome) {
        if (biome.is(Tags.Biomes.IS_MAGICAL)) {
            return !biome.is(Thaumcraft.id("magical_forest")) && !biome.is(Thaumcraft.id("tainted_land"));
        }
        return biome.is(Biomes.WINDSWEPT_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST);
    }

    private static boolean generateSilverwood(WorldGenLevel level, Random random, int chunkX, int chunkZ) {
        int x = chunkX * 16 + random.nextInt(16);
        int z = chunkZ * 16 + random.nextInt(16);
        int y = height(level, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        return supportsSilverwood(level.getBiome(pos)) && new SilverwoodTreeGenerator(false, 7, 4).generate(level, random, pos);
    }

    private static boolean generateGreatwood(WorldGenLevel level, Random random, int chunkX, int chunkZ) {
        int x = chunkX * 16 + random.nextInt(16);
        int z = chunkZ * 16 + random.nextInt(16);
        int y = height(level, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        return BiomeHandler.getBiomeSupportsGreatwood(level.getBiome(pos)) > random.nextFloat()
            && new GreatwoodTreeGenerator(false).generate(level, random, pos, random.nextInt(8) == 0);
    }
}
