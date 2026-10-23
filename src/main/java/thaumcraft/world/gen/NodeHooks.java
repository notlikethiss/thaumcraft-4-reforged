package thaumcraft.world.gen;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import thaumcraft.block.ObsidianTotemBlock;
import thaumcraft.node.NodeGenerator;
import thaumcraft.registry.ModBlocks;
import thaumcraft.Config;

public final class NodeHooks {
    private NodeHooks() {
    }

    private static int firstUncoveredY(WorldGenLevel level, int x, int z) {
        int y = 5;
        while (y < level.getMaxY() && !level.isEmptyBlock(new BlockPos(x, y + 1, z))) {
            y++;
        }
        return y;
    }

    private static boolean isTemple(Structure structure) {
        StructureType<?> type = structure.type();
        return type == StructureType.DESERT_PYRAMID || type == StructureType.JUNGLE_TEMPLE || type == StructureType.SWAMP_HUT;
    }

    public static boolean templeNode(WorldGenLevel level, Random random, int chunkX, int chunkZ) {
        StructureManager manager = level.getLevel().structureManager();
        if (level instanceof WorldGenRegion region) {
            manager = manager.forWorldGenRegion(region);
        }
        for (StructureStart start : manager.startsForStructure(new ChunkPos(chunkX, chunkZ), NodeHooks::isTemple)) {
            if (!start.isValid() || start.getChunkPos().x() != chunkX || start.getChunkPos().z() != chunkZ) {
                continue;
            }
            BlockPos center = start.getBoundingBox().getCenter();
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, center.getX(), center.getZ()) + 3;
            NodeGenerator.createRandomNodeAt(level, new BlockPos(center.getX(), y, center.getZ()), random, false, false, false);
            return true;
        }
        return false;
    }

    private static boolean generateWildNodes(WorldGenLevel level, Random random, int chunkX, int chunkZ, boolean auraGen) {
        if (Config.genAura() && random.nextInt(Config.nodeRarity()) == 0 && !auraGen) {
            int x = chunkX * 16 + random.nextInt(16);
            int z = chunkZ * 16 + random.nextInt(16);
            int q = firstUncoveredY(level, x, z);
            if (q < 2) {
                q = 63 + random.nextInt(64) - 32 + firstUncoveredY(level, x, z);
            }
            if (q < 2) {
                q = 32 + random.nextInt(64);
            }
            if (level.isEmptyBlock(new BlockPos(x, q + 1, z))) {
                q++;
            }
            int p = random.nextInt(4);
            BlockPos candidate = new BlockPos(x, q + p, z);
            if (level.isEmptyBlock(candidate) || level.getBlockState(candidate).canBeReplaced()) {
                q += p;
            }
            if (q > level.getMaxY()) {
                return false;
            }
            NodeGenerator.createRandomNodeAt(level, new BlockPos(x, q, z), random, false, false, false);
            return true;
        }
        return false;
    }

    public static boolean wildNode(WorldGenLevel level, Random random, int chunkX, int chunkZ, boolean auraGen) {
        return generateWildNodes(level, random, chunkX, chunkZ, auraGen);
    }

    public static void moundNode(WorldGenLevel level, Random random, int x, int y, int z) {
        NodeGenerator.createRandomNodeAt(level, new BlockPos(x, y, z), random, false, true, false);
    }

    public static void hilltopNode(WorldGenLevel level, Random random, int x, int y, int z) {
        NodeGenerator.createRandomNodeAt(level, new BlockPos(x, y, z), random, false, true, false);
    }

    public static boolean eldritchRing(WorldGenLevel level, Random random, int chunkX, int chunkZ, int x, int y, int z) {
        if (!thaumcraft.eldritch.EldritchRingGenerator.generate(level, random, chunkX, chunkZ, x, y, z)) {
            return false;
        }
        NodeGenerator.createRandomNodeAt(level, new BlockPos(x, y + 2, z), random, false, true, false);
        return true;
    }

    private static boolean isOpen(BlockState state) {
        return state.isAir() || state.is(Blocks.SNOW) || state.is(Blocks.SHORT_GRASS);
    }

    public static void totem(WorldGenLevel level, Random random, int chunkX, int chunkZ, boolean auraGen) {
        if (!Config.genStructure() || auraGen || random.nextInt(Config.nodeRarity() * 10) != 0) {
            return;
        }
        int x = chunkX * 16 + random.nextInt(16);
        int z = chunkZ * 16 + random.nextInt(16);
        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
        if (topY > level.getMaxY()) {
            return;
        }
        if (LegacyGen.get(level, x, topY, z).is(BlockTags.LEAVES)) {
            do {
                topY--;
            } while (!LegacyGen.get(level, x, topY, z).is(Blocks.GRASS_BLOCK) && topY > 40);
        }
        BlockState ground = LegacyGen.get(level, x, topY, z);
        if (ground.is(Blocks.SNOW) || ground.is(Blocks.SHORT_GRASS)) {
            topY--;
            ground = LegacyGen.get(level, x, topY, z);
        }
        if (!(ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.SAND) || ground.is(Blocks.DIRT) || ground.is(Blocks.STONE) || ground.is(Blocks.NETHERRACK))) {
            return;
        }
        int count = 1;
        while (isOpen(LegacyGen.get(level, x, topY + count, z)) && count < 3) {
            count++;
        }
        if (count < 2) {
            return;
        }
        LegacyGen.set(level, x, topY, z, ModBlocks.OBSIDIAN_TILE.get().defaultBlockState());
        count = 1;
        boolean placed = false;
        while (isOpen(LegacyGen.get(level, x, topY + count, z)) && count < 5) {
            LegacyGen.set(level, x, topY + count, z, ModBlocks.OBSIDIAN_TOTEM.get().defaultBlockState());
            if (count > 1 && random.nextInt(4) == 0) {
                chargeTotem(level, random, x, topY + count, z);
                count = 5;
                placed = true;
            }
            if (++count >= 5 && !placed) {
                chargeTotem(level, random, x, topY + 5, z);
            }
        }
        ObsidianTotemBlock totem = ModBlocks.OBSIDIAN_TOTEM.get();
        for (int y = topY + 1; LegacyGen.get(level, x, y, z).is(totem); y++) {
            BlockPos pos = new BlockPos(x, y, z);
            level.setBlock(pos, totem.shapeFor(level, pos), Block.UPDATE_CLIENTS);
        }
    }

    private static void chargeTotem(WorldGenLevel level, Random random, int x, int y, int z) {
        LegacyGen.set(level, x, y, z, ModBlocks.CHARGED_OBSIDIAN_TOTEM.get().defaultBlockState());
        NodeGenerator.createRandomNodeAt(level, new BlockPos(x, y, z), random, false, true, false);
    }

    public static void netherTotem(WorldGenLevel level, Random random, int chunkX, int chunkZ) {
    }

    public static void netherWildNode(WorldGenLevel level, Random random, int chunkX, int chunkZ) {
        generateWildNodes(level, random, chunkX, chunkZ, false);
    }
}
