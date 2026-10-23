package thaumcraft.world.tree;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import thaumcraft.block.PlantSoil;
import thaumcraft.registry.ModBlocks;

public class GreatwoodTreeGenerator extends BranchedTreeGenerator {
    public GreatwoodTreeGenerator(boolean notify) {
        super(notify);
        heightAttenuation = 0.618;
        branchSlope = 0.38;
        scaleWidth = 1.2;
        leafDensity = 0.9;
        trunkSize = 2;
        heightLimitLimit = 11;
        leafDistanceLimit = 4;
    }

    @Override
    protected BlockState leafState() {
        return ModBlocks.GREATWOOD_LEAVES.get().defaultBlockState();
    }

    @Override
    protected BlockState logState() {
        return ModBlocks.GREATWOOD_LOG.get().defaultBlockState();
    }

    private boolean validTreeLocation(int offsetX, int offsetZ) {
        int[] from = {basePos[0] + offsetX, basePos[1], basePos[2] + offsetZ};
        int[] to = {basePos[0] + offsetX, basePos[1] + heightLimit - 1, basePos[2] + offsetZ};
        BlockPos soilPos = new BlockPos(basePos[0] + offsetX, basePos[1] - 1, basePos[2] + offsetZ);
        if (level.isOutsideBuildHeight(soilPos) || !PlantSoil.PLAINS.canSustain(level.getBlockState(soilPos), level, soilPos)) {
            return false;
        }
        int obstruction = checkBlockLine(from, to);
        if (obstruction == -1) {
            return true;
        }
        if (obstruction < 6) {
            return false;
        }
        heightLimit = obstruction;
        return true;
    }

    public boolean generate(LevelAccessor level, Random random, BlockPos pos) {
        return generate(level, random, pos, random.nextInt(8) == 0);
    }

    public boolean generate(LevelAccessor level, Random random, BlockPos pos, boolean spiders) {
        this.level = level;
        rand.setSeed(random.nextLong());
        basePos[0] = pos.getX();
        basePos[1] = pos.getY();
        basePos[2] = pos.getZ();
        if (heightLimit == 0) {
            heightLimit = heightLimitLimit + rand.nextInt(heightLimitLimit);
        }
        boolean valid = false;
        search:
        for (int shiftX = -1; shiftX < 2; shiftX++) {
            for (int shiftZ = -1; shiftZ < 2; shiftZ++) {
                boolean fits = true;
                for (int partX = 0; partX < trunkSize && fits; partX++) {
                    for (int partZ = 0; partZ < trunkSize; partZ++) {
                        if (!validTreeLocation(partX + shiftX, partZ + shiftZ)) {
                            fits = false;
                            break;
                        }
                    }
                }
                if (fits) {
                    valid = true;
                    basePos[0] = basePos[0] + shiftX;
                    basePos[2] = basePos[2] + shiftZ;
                    break search;
                }
            }
        }
        if (!valid) {
            return false;
        }
        generateLeafNodeList();
        generateLeaves();
        generateLeafNodeBases();
        generateTrunk();
        scaleWidth = 1.66;
        basePos[0] = pos.getX();
        basePos[1] = pos.getY() + height;
        basePos[2] = pos.getZ();
        generateLeafNodeList();
        generateLeaves();
        generateLeafNodeBases();
        generateTrunk();
        if (spiders) {
            generateSpiderNest(random, pos);
        }
        scheduleLeafUpdates();
        return true;
    }

    private void generateSpiderNest(Random random, BlockPos pos) {
        BlockPos spawnerPos = pos.below();
        level.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity spawner = level.getBlockEntity(spawnerPos);
        if (spawner instanceof SpawnerBlockEntity spawnerEntity) {
            spawnerEntity.setEntityId(EntityType.CAVE_SPIDER, level.getRandom());
            for (int attempt = 0; attempt < 50; attempt++) {
                BlockPos webPos = new BlockPos(
                    pos.getX() - 7 + random.nextInt(14),
                    pos.getY() + random.nextInt(10),
                    pos.getZ() - 7 + random.nextInt(14)
                );
                if (level.isEmptyBlock(webPos) && (isTouching(webPos, leafState().getBlock()) || isTouching(webPos, logState().getBlock()))) {
                    level.setBlock(webPos, Blocks.COBWEB.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            BlockPos chestPos = pos.below(2);
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(chestPos) instanceof RandomizableContainerBlockEntity chest) {
                chest.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, rand.nextLong());
            }
        }
    }

    private boolean isTouching(BlockPos pos, Block block) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(block)) {
                return true;
            }
        }
        return false;
    }
}
