package thaumcraft.world.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.block.PlantSoil;
import thaumcraft.node.NodeGenerator;
import thaumcraft.registry.ModBlocks;

public class SilverwoodTreeGenerator {
    private final boolean notify;
    private final int minTreeHeight;
    private final int randomTreeHeight;
    private final boolean worldgen;

    public SilverwoodTreeGenerator(boolean notify, int minTreeHeight, int randomTreeHeight) {
        this.notify = notify;
        this.worldgen = !notify;
        this.minTreeHeight = minTreeHeight;
        this.randomTreeHeight = randomTreeHeight;
    }

    public boolean isWorldgen() {
        return worldgen;
    }

    private void place(LevelAccessor level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, notify ? Block.UPDATE_ALL : Block.UPDATE_CLIENTS);
    }

    private static boolean canReplaceWithLeaves(BlockState state) {
        return state.isAir() || state.is(BlockTags.REPLACEABLE_BY_TREES);
    }

    private static boolean isFree(BlockState state) {
        return state.isAir() || state.is(BlockTags.LEAVES) || state.canBeReplaced();
    }

    public boolean generate(LevelAccessor level, Random random, BlockPos origin) {
        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();
        int height = random.nextInt(randomTreeHeight) + minTreeHeight;
        if (y < level.getMinBuildHeight() + 1 || y + height + 1 > level.getMaxBuildHeight()) {
            return false;
        }
        for (int layerY = y; layerY <= y + 1 + height; layerY++) {
            int spread = 1;
            if (layerY == y) {
                spread = 0;
            }
            if (layerY >= y + 1 + height - 2) {
                spread = 3;
            }
            for (int scanX = x - spread; scanX <= x + spread; scanX++) {
                for (int scanZ = z - spread; scanZ <= z + spread; scanZ++) {
                    BlockPos scanPos = new BlockPos(scanX, layerY, scanZ);
                    if (level.isOutsideBuildHeight(scanPos)) {
                        return false;
                    }
                    if (!isFree(level.getBlockState(scanPos)) && layerY > y) {
                        return false;
                    }
                }
            }
        }
        BlockPos soilPos = origin.below();
        BlockState soil = level.getBlockState(soilPos);
        if (!PlantSoil.PLAINS.canSustain(soil, level, soilPos) || y >= level.getMaxBuildHeight() - height - 1) {
            return false;
        }
        if (soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.FARMLAND) || soil.is(Blocks.MYCELIUM)) {
            level.setBlock(soilPos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        BlockState leaves = ModBlocks.SILVERWOOD_LEAVES.get().defaultBlockState();
        BlockState log = ModBlocks.SILVERWOOD_LOG.get().defaultBlockState();
        BlockState logX = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState logZ = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        List<BlockPos> placedLeaves = new ArrayList<>();
        int start = y + height - 5;
        int end = y + height + 3 + random.nextInt(3);
        for (int layerY = start; layerY <= end; layerY++) {
            int centerY = Mth.clamp(layerY, y + height - 3, y + height);
            for (int leafX = x - 5; leafX <= x + 5; leafX++) {
                for (int leafZ = z - 5; leafZ <= z + 5; leafZ++) {
                    double distanceX = leafX - x;
                    double distanceY = layerY - centerY;
                    double distanceZ = leafZ - z;
                    double distance = distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ;
                    BlockPos leafPos = new BlockPos(leafX, layerY, leafZ);
                    if (distance < 10 + random.nextInt(8) && !level.isOutsideBuildHeight(leafPos) && canReplaceWithLeaves(level.getBlockState(leafPos))) {
                        place(level, leafX, layerY, leafZ, leaves);
                        placedLeaves.add(leafPos);
                    }
                }
            }
        }
        int chance = (int) (height * 1.5);
        boolean lastBlock = false;
        int offset;
        for (offset = 0; offset < height; offset++) {
            BlockPos trunkPos = new BlockPos(x, y + offset, z);
            if (isFree(level.getBlockState(trunkPos))) {
                if (offset > 0 && !lastBlock && random.nextInt(chance) == 0) {
                    placeKnot(level, trunkPos, random);
                    chance += height;
                    lastBlock = true;
                } else {
                    place(level, x, y + offset, z, log);
                    lastBlock = false;
                }
                place(level, x - 1, y + offset, z, log);
                place(level, x + 1, y + offset, z, log);
                place(level, x, y + offset, z - 1, log);
                place(level, x, y + offset, z + 1, log);
            }
        }
        place(level, x, y + offset, z, log);
        place(level, x - 1, y, z - 1, log);
        place(level, x + 1, y, z + 1, log);
        place(level, x - 1, y, z + 1, log);
        place(level, x + 1, y, z - 1, log);
        if (random.nextInt(3) != 0) {
            place(level, x - 1, y + 1, z - 1, log);
        }
        if (random.nextInt(3) != 0) {
            place(level, x + 1, y + 1, z + 1, log);
        }
        if (random.nextInt(3) != 0) {
            place(level, x - 1, y + 1, z + 1, log);
        }
        if (random.nextInt(3) != 0) {
            place(level, x + 1, y + 1, z - 1, log);
        }
        place(level, x - 2, y, z, logX);
        place(level, x + 2, y, z, logX);
        place(level, x, y, z - 2, logZ);
        place(level, x, y, z + 2, logZ);
        place(level, x - 2, y - 1, z, log);
        place(level, x + 2, y - 1, z, log);
        place(level, x, y - 1, z - 2, log);
        place(level, x, y - 1, z + 2, log);
        place(level, x - 1, y + (height - 4), z - 1, log);
        place(level, x + 1, y + (height - 4), z + 1, log);
        place(level, x - 1, y + (height - 4), z + 1, log);
        place(level, x + 1, y + (height - 4), z - 1, log);
        if (random.nextInt(3) == 0) {
            place(level, x - 1, y + (height - 5), z - 1, log);
        }
        if (random.nextInt(3) == 0) {
            place(level, x + 1, y + (height - 5), z + 1, log);
        }
        if (random.nextInt(3) == 0) {
            place(level, x - 1, y + (height - 5), z + 1, log);
        }
        if (random.nextInt(3) == 0) {
            place(level, x + 1, y + (height - 5), z - 1, log);
        }
        place(level, x - 2, y + (height - 4), z, logX);
        place(level, x + 2, y + (height - 4), z, logX);
        place(level, x, y + (height - 4), z - 2, logZ);
        place(level, x, y + (height - 4), z + 2, logZ);
        for (BlockPos leafPos : placedLeaves) {
            if (level.getBlockState(leafPos).is(leaves.getBlock())) {
                level.scheduleTick(leafPos, leaves.getBlock(), 1);
            }
        }
        return true;
    }

    private static void placeKnot(LevelAccessor level, BlockPos pos, Random random) {
        level.setBlock(pos, ModBlocks.SILVERWOOD_KNOT.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        NodeGenerator.createRandomNodeAt(level, pos, random, true, false, false);
    }
}
