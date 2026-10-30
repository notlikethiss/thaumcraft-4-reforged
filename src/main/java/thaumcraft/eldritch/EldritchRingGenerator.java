package thaumcraft.eldritch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.registry.EldritchContent;
import thaumcraft.registry.ModBlocks;
import thaumcraft.world.dim.MazeHandler;
import thaumcraft.world.gen.LegacyGen;

public final class EldritchRingGenerator {
    private EldritchRingGenerator() {
    }

    public static boolean generate(WorldGenLevel level, Random random, int chunkX, int chunkZ, int x, int y, int z) {
        // The decoration origin can be in a different chunk from the actual altar.
        chunkX = x >> 4;
        chunkZ = z >> 4;
        int width = 11 + random.nextInt(6) * 2;
        int height = 11 + random.nextInt(6) * 2;
        if (!validSpawn(level, x - 3, y, z - 3)
            || !validSpawn(level, x, y, z)
            || !validSpawn(level, x + 3, y, z)
            || !validSpawn(level, x + 3, y, z + 3)
            || !validSpawn(level, x, y, z + 3)) {
            return false;
        }
        if (MazeHandler.naturalRingInRange(level.getLevel().getServer(), level.getLevel().getSeed(), chunkX, chunkZ, width, height)) {
            return false;
        }

        List<BlockChange> changes = new ArrayList<>();
        List<BannerFacing> bannerFacings = new ArrayList<>();
        Map<BlockPos, BlockState> overlay = new HashMap<>();
        int altarRoll = -1;
        for (int blockX = x - 3; blockX <= x + 3; blockX++) {
            for (int blockZ = z - 3; blockZ <= z + 3; blockZ++) {
                if ((blockX == x - 3 || blockX == x + 3) && (blockZ == z - 3 || blockZ == z + 3)) {
                    continue;
                }
                for (int offsetY = -4; offsetY < 5; offsetY++) {
                    BlockPos pos = new BlockPos(blockX, y + offsetY, blockZ);
                    BlockState previous = overlay.containsKey(pos) ? overlay.get(pos) : level.getBlockState(pos);
                    if (offsetY <= 0 || previous.canBeReplaced() || previous.getCollisionShape(level, pos, net.minecraft.world.phys.shapes.CollisionContext.empty()).isEmpty() || previous.is(BlockTags.LEAVES)) {
                        planBlock(changes, overlay, pos, random.nextInt(4) == 0 ? Blocks.OBSIDIAN.defaultBlockState() : ModBlocks.OBSIDIAN_TILE.get().defaultBlockState());
                    }
                    if (offsetY > 0) {
                        planBlock(changes, overlay, pos, Blocks.AIR.defaultBlockState());
                    }
                }
                if (blockX == x && blockZ == z) {
                    BlockPos altarPos = new BlockPos(x, y + 1, z);
                    placeAltar(changes, overlay, altarPos);
                    planBlock(changes, overlay, new BlockPos(x, y, z), ModBlocks.OBSIDIAN_TILE.get().defaultBlockState());
                    altarRoll = random.nextInt(10);
                    if (altarRoll >= 1 && altarRoll <= 4) {
                        placeCultistBanners(changes, overlay, bannerFacings, x, y + 1, z);
                    }
                    placeObelisk(changes, overlay, new BlockPos(x, y + 3, z));
                } else if (((blockX == x - 3 || blockX == x + 3) && Math.abs((blockZ - z) % 2) == 1
                    || (blockZ == z - 3 || blockZ == z + 3) && Math.abs((blockX - x) % 2) == 1)
                    && Math.abs(blockX - x) != Math.abs(blockZ - z)) {
                    planBlock(changes, overlay, new BlockPos(blockX, y, blockZ), ModBlocks.OBSIDIAN_TILE.get().defaultBlockState());
                    planBlock(changes, overlay, new BlockPos(blockX, y + 1, blockZ), EldritchContent.ELDRITCH_CAPSTONE.get().defaultBlockState());
                }
            }
        }
        long seed = random.nextLong();
        if (!MazeHandler.reserveNaturalRing(level.getLevel().getServer(), level.getLevel().getSeed(), chunkX, chunkZ, width, height, seed)) {
            return false;
        }
        for (BlockChange change : changes) {
            level.setBlock(change.pos(), change.state(), 3);
        }
        BlockPos altarPos = new BlockPos(x, y + 1, z);
        if (level.getBlockEntity(altarPos) instanceof EldritchBlockEntity altar) {
            if (altarRoll >= 1 && altarRoll <= 4) {
                altar.setSpawner(true);
                altar.setSpawnType((byte) 0);
            } else if (altarRoll == 6 || altarRoll == 7) {
                altar.setSpawner(true);
                altar.setSpawnType((byte) 1);
            }
        }
        for (BannerFacing banner : bannerFacings) {
            if (level.getBlockEntity(banner.pos()) instanceof thaumcraft.device.BannerBlockEntity blockEntity) {
                blockEntity.setFacing(banner.facing());
            }
        }
        return true;
    }

    private static void placeCultistBanners(List<BlockChange> changes, Map<BlockPos, BlockState> overlay, List<BannerFacing> facings, int x, int y, int z) {
        int[][] banners = {{x, z + 3, 0}, {x, z - 3, 8}, {x + 3, z, 12}, {x - 3, z, 4}};
        for (int[] banner : banners) {
            BlockPos pos = new BlockPos(banner[0], y, banner[1]);
            planBlock(changes, overlay, pos, thaumcraft.registry.ModBlocks.BANNER.get().defaultBlockState());
            facings.add(new BannerFacing(pos, banner[2]));
        }
    }

    private static void placeAltar(List<BlockChange> changes, Map<BlockPos, BlockState> overlay, BlockPos pos) {
        planBlock(changes, overlay, pos, EldritchContent.ELDRITCH_ALTAR.get().defaultBlockState());
    }

    private static void placeObelisk(List<BlockChange> changes, Map<BlockPos, BlockState> overlay, BlockPos pos) {
        planBlock(changes, overlay, pos, EldritchContent.ELDRITCH_OBELISK.get().defaultBlockState().setValue(EldritchBlock.PART, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
        for (int y = 1; y <= 4; y++) {
            planBlock(changes, overlay, pos.above(y), EldritchContent.ELDRITCH_OBELISK.get().defaultBlockState().setValue(EldritchBlock.PART, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        }
    }

    private static void planBlock(List<BlockChange> changes, Map<BlockPos, BlockState> overlay, BlockPos pos, BlockState state) {
        changes.add(new BlockChange(pos, state));
        overlay.put(pos, state);
    }

    private record BlockChange(BlockPos pos, BlockState state) {
    }

    private record BannerFacing(BlockPos pos, int facing) {
    }

    private static boolean validSpawn(WorldGenLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        int distanceToAir = 0;
        while (!level.getBlockState(pos.above(distanceToAir)).isAir()) {
            if (++distanceToAir > 2) {
                return false;
            }
        }
        BlockPos top = pos.above(distanceToAir - 1);
        if (!level.isEmptyBlock(top.above())) {
            return false;
        }
        BlockState ground = level.getBlockState(top);
        if (isValidGround(ground)) {
            return true;
        }
        return LegacyGen.isSnowOrGrass(ground) && isValidGround(level.getBlockState(top.below()));
    }

    private static boolean isValidGround(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.SAND) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.GRAVEL) || state.is(Blocks.DIRT);
    }
}
