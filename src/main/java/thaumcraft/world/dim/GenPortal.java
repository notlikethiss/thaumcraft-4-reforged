package thaumcraft.world.dim;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import thaumcraft.registry.EldritchContent;

public class GenPortal extends GenCommon {
    void generatePortal(ServerLevel world, RandomSource random, int cx, int cz, int y, Cell cell) {
        int x = cx * 16;
        int z = cz * 16;

        for (int a = 1; a <= 15; a++) {
            for (int b = 1; b <= 15; b++) {
                for (int c = 0; c < 13; c++) {
                    if (a == 1 || a == 15 || b == 1 || b == 15) {
                        placeBlock(world, x + a, y + c, z + b, 1, cell);
                    }
                }
            }
        }

        for (int a = 2; a <= 14; a++) {
            for (int b = 2; b <= 14; b++) {
                for (int c = 1; c < 12; c++) {
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
                for (int c = 2; c < 11; c++) {
                    if ((a == 3 || a == 13 || b == 3 || b == 13) && (a > 4 || b > 4) && (a > 4 || b < 12) && (a < 12 || b > 4) && (a < 12 || b < 12)) {
                        placeBlock(world, x + a, y + c, z + b, 2, cell);
                    }
                }
            }
        }

        for (int a = 2; a <= 14; a++) {
            for (int b = 2; b <= 14; b++) {
                placeBlock(world, x + a, y - 1, z + b, 1, cell);
                placeBlock(world, x + a, y, z + b, 8, cell);
                placeBlock(world, x + a, y + 1, z + b, 19, cell);
                placeBlock(world, x + a, y + 13, z + b, 1, cell);
                placeBlock(world, x + a, y + 12, z + b, 8, cell);
                placeBlock(world, x + a, y + 11, z + b, 2, cell);
                if (a > 1 && a < 15 && b > 1 && b < 15) {
                    int q = Math.min(Math.abs(8 - a), Math.abs(8 - b));

                    for (int g = 0; g < q - 1; g++) {
                        placeBlock(world, x + a, y + 1 + g, z + b, 19, cell);
                    }
                }

                if (a > 3 && a < 13 && b > 3 && b < 13) {
                    int q = Math.min(Math.abs(8 - a), Math.abs(8 - b));

                    for (int g = 0; g < q; g++) {
                        placeBlock(world, x + a, y + 11 - g, z + b, 19, cell);
                    }
                }
            }
        }

        for (int g = 0; g < 5; g++) {
            placeBlock(world, x + 6 + g, y + 2, z + 4, 10, Direction.NORTH, cell);
            placeBlock(world, x + 6 + g, y + 2, z + 12, 10, Direction.SOUTH, cell);
            placeBlock(world, x + 12, y + 2, z + 6 + g, 10, Direction.EAST, cell);
            placeBlock(world, x + 4, y + 2, z + 6 + g, 10, Direction.WEST, cell);
        }

        generateConnections(world, random, cx, cz, y, cell, 3, true);

        for (int a = 3; a <= 13; a++) {
            for (int b = 3; b <= 13; b++) {
                for (int c = 1; c < 12; c++) {
                    if (a <= 4 && b <= 4 || a <= 4 && b >= 12 || a >= 12 && b <= 4 || a >= 12 && b >= 12) {
                        placeBlock(world, x + a, y + c, z + b, 9, cell);
                    }
                }
            }
        }

        placeBlock(world, x + 5, y + 3, z + 5, 10, Direction.NORTH, cell);
        placeBlock(world, x + 4, y + 3, z + 5, 10, Direction.NORTH, cell);
        placeBlock(world, x + 5, y + 3, z + 4, 10, Direction.WEST, cell);
        placeBlock(world, x + 5, y + 8, z + 5, 11, Direction.NORTH, cell);
        placeBlock(world, x + 4, y + 8, z + 5, 11, Direction.NORTH, cell);
        placeBlock(world, x + 5, y + 8, z + 4, 11, Direction.WEST, cell);
        placeBlock(world, x + 12, y + 3, z + 5, 10, Direction.NORTH, cell);
        placeBlock(world, x + 11, y + 3, z + 5, 10, Direction.NORTH, cell);
        placeBlock(world, x + 11, y + 3, z + 4, 10, Direction.EAST, cell);
        placeBlock(world, x + 12, y + 8, z + 5, 11, Direction.NORTH, cell);
        placeBlock(world, x + 11, y + 8, z + 5, 11, Direction.NORTH, cell);
        placeBlock(world, x + 11, y + 8, z + 4, 11, Direction.EAST, cell);
        placeBlock(world, x + 5, y + 3, z + 11, 10, Direction.SOUTH, cell);
        placeBlock(world, x + 4, y + 3, z + 11, 10, Direction.SOUTH, cell);
        placeBlock(world, x + 5, y + 3, z + 12, 10, Direction.WEST, cell);
        placeBlock(world, x + 5, y + 8, z + 11, 11, Direction.SOUTH, cell);
        placeBlock(world, x + 4, y + 8, z + 11, 11, Direction.SOUTH, cell);
        placeBlock(world, x + 5, y + 8, z + 12, 11, Direction.WEST, cell);
        placeBlock(world, x + 12, y + 3, z + 11, 10, Direction.SOUTH, cell);
        placeBlock(world, x + 11, y + 3, z + 11, 10, Direction.SOUTH, cell);
        placeBlock(world, x + 11, y + 3, z + 12, 10, Direction.EAST, cell);
        placeBlock(world, x + 12, y + 8, z + 11, 11, Direction.SOUTH, cell);
        placeBlock(world, x + 11, y + 8, z + 11, 11, Direction.SOUTH, cell);
        placeBlock(world, x + 11, y + 8, z + 12, 11, Direction.EAST, cell);
        set(world, x + 8, y + 2, z + 8, EldritchContent.ELDRITCH_CAPSTONE.get().defaultBlockState());
        set(world, x + 8, y + 3, z + 8, EldritchContent.ELDRITCH_PORTAL.get().defaultBlockState());
        genObelisk(world, x + 8, y + 4, z + 8);
    }
}
