package thaumcraft.world.dim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import thaumcraft.registry.ModBlocks;
import thaumcraft.device.CrystalClusterBlock;
import thaumcraft.block.LootBlock;

public class GenNestRoom extends GenCommon {
    void generateRoom(ServerLevel world, RandomSource random, int cx, int cz, int y, Cell cell) {
        int x = cx * 16;
        int z = cz * 16;

        for (int a = 1; a <= 15; a++) {
            for (int b = 1; b <= 15; b++) {
                for (int c = 0; c < 11; c++) {
                    if (a == 1 || a == 15 || b == 1 || b == 15) {
                        placeBlock(world, x + a, y + c, z + b, 1, cell);
                    }
                }
            }
        }

        for (int a = 2; a <= 14; a++) {
            for (int b = 2; b <= 14; b++) {
                for (int c = 1; c < 10; c++) {
                    if ((a == 2 || a == 14 || b == 2 || b == 14)
                        && (a != 2 || b <= 3 || b >= 12 || !cell.west || c >= 10)
                        && (a != 14 || b <= 3 || b >= 12 || !cell.east || c >= 10)
                        && (b != 2 || a <= 3 || a >= 12 || !cell.north || c >= 10)
                        && (b != 14 || a <= 3 || a >= 12 || !cell.south || c >= 10)) {
                        placeBlock(world, x + a, y + c, z + b, 8, cell);
                    }
                }
            }
        }

        for (int a = 3; a <= 13; a++) {
            for (int b = 3; b <= 13; b++) {
                for (int c = 2; c < 9; c++) {
                    if (a == 3 || a == 13 || b == 3 || b == 13) {
                        placeBlock(world, x + a, y + c, z + b, 21, cell);
                    }

                    if ((a == 4 && !cell.west || a == 12 && !cell.east || b == 4 && !cell.north || b == 12 && !cell.south) && random.nextBoolean()) {
                        placeBlock(world, x + a, y + c, z + b, 21, cell);
                    }
                }
            }
        }

        for (int a = 2; a <= 14; a++) {
            for (int b = 2; b <= 14; b++) {
                placeBlock(world, x + a, y - 1, z + b, 1, cell);
                placeBlock(world, x + a, y, z + b, 8, cell);
                placeBlock(world, x + a, y + 1, z + b, 21, cell);
                placeBlock(world, x + a, y + 11, z + b, 1, cell);
                placeBlock(world, x + a, y + 10, z + b, 8, cell);
                placeBlock(world, x + a, y + 9, z + b, 21, cell);
                if (random.nextBoolean()) {
                    placeBlock(world, x + a, y + 8, z + b, 21, cell);
                } else if (random.nextBoolean() && world.isEmptyBlock(new BlockPos(x + a, y + 8, z + b))) {
                    set(world, x + a, y + 8, z + b, ModBlocks.STRANGE_CRYSTALS.get().defaultBlockState().setValue(CrystalClusterBlock.FACING, Direction.DOWN));
                }
            }
        }

        placeBlock(world, x + 8, y + 2, z + 8, 21, cell);
        placeBlock(world, x + 8, y + 3, z + 8, 21, cell);
        placeBlock(world, x + 8, y + 4, z + 8, 21, cell);
        placeBlock(world, x + 7, y + 2, z + 8, 21, cell);
        placeBlock(world, x + 8, y + 2, z + 7, 21, cell);
        placeBlock(world, x + 9, y + 2, z + 8, 21, cell);
        placeBlock(world, x + 8, y + 2, z + 9, 21, cell);
        if (random.nextBoolean()) {
            placeBlock(world, x + 7, y + 3, z + 8, 21, cell);
        }

        if (random.nextBoolean()) {
            placeBlock(world, x + 8, y + 3, z + 7, 21, cell);
        }

        if (random.nextBoolean()) {
            placeBlock(world, x + 9, y + 3, z + 8, 21, cell);
        }

        if (random.nextBoolean()) {
            placeBlock(world, x + 8, y + 3, z + 9, 21, cell);
        }

        if (random.nextBoolean()) {
            placeBlock(world, x + 8, y + 5, z + 8, 7, cell);
        }

        placeBlock(world, x + 8, y + 8, z + 8, 21, cell);
        placeBlock(world, x + 8, y + 7, z + 8, 21, cell);
        placeBlock(world, x + 8, y + 6, z + 8, 21, cell);
        placeBlock(world, x + 7, y + 8, z + 8, 21, cell);
        placeBlock(world, x + 8, y + 8, z + 7, 21, cell);
        placeBlock(world, x + 9, y + 8, z + 8, 21, cell);
        placeBlock(world, x + 8, y + 8, z + 9, 21, cell);
        if (random.nextBoolean()) {
            placeBlock(world, x + 7, y + 7, z + 8, 21, cell);
        }

        if (random.nextBoolean()) {
            placeBlock(world, x + 8, y + 7, z + 7, 21, cell);
        }

        if (random.nextBoolean()) {
            placeBlock(world, x + 9, y + 7, z + 8, 21, cell);
        }

        if (random.nextBoolean()) {
            placeBlock(world, x + 8, y + 7, z + 9, 21, cell);
        }

        generateConnections(world, random, cx, cz, y, cell, 3, true);

        for (int a = -5; a <= 5; a++) {
            for (int b = -5; b <= 5; b++) {
                BlockPos pos = new BlockPos(x + 8 + a, y + 2, z + 8 + b);
                if (random.nextFloat() < 0.15F && world.isEmptyBlock(pos)) {
                    float rr = random.nextFloat();
                    LootBlock.Tier tier = rr < 0.15F ? LootBlock.Tier.RARE : rr < 0.4F ? LootBlock.Tier.UNCOMMON : LootBlock.Tier.COMMON;
                    var lootBlock = random.nextFloat() < 0.2F ? ModBlocks.LOOT_CRATE.get() : ModBlocks.LOOT_URN.get();
                    set(world, pos.getX(), pos.getY(), pos.getZ(), lootBlock.defaultBlockState().setValue(LootBlock.TIER, tier));
                }
            }
        }
    }
}
