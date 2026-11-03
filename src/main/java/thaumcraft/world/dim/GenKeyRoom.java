package thaumcraft.world.dim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.registry.EldritchContent;
import thaumcraft.registry.ModEntities;
import thaumcraft.entity.monster.eldritch.EldritchGuardian;
import thaumcraft.entity.monster.ChampionModifier;

public class GenKeyRoom extends GenCommon {
    void generateRoom(ServerLevel world, RandomSource random, int cx, int cz, int y, Cell cell) {
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
                    if (a == 3 || a == 13 || b == 3 || b == 13) {
                        if (c > 3 && c < 9 && (a == 8 || b == 8) || c > 4 && c < 8 && (a == 7 || b == 7 || a == 9 || b == 9)) {
                            if (a != 8 && b != 8 || c != 6) {
                                placeBlock(world, x + a, y + c, z + b, 19, cell);
                            }
                        } else {
                            placeBlock(world, x + a, y + c, z + b, 18, cell);
                        }
                    }
                }
            }
        }

        for (int a = 2; a <= 14; a++) {
            for (int b = 2; b <= 14; b++) {
                placeBlock(world, x + a, y - 1, z + b, 1, cell);
                placeBlock(world, x + a, y, z + b, 8, cell);
                placeBlock(world, x + a, y + 1, z + b, 2, cell);
                placeBlock(world, x + a, y + 13, z + b, 1, cell);
                placeBlock(world, x + a, y + 12, z + b, 8, cell);
                placeBlock(world, x + a, y + 11, z + b, 2, cell);
                if (a > 1 && a < 15 && b > 1 && b < 15) {
                    int q = Math.min(Math.abs(8 - a), Math.abs(8 - b));

                    for (int g = 0; g < q - 1; g++) {
                        placeBlock(world, x + a, y + 1 + g, z + b, 2, cell);
                    }
                }

                if (a > 3 && a < 13 && b > 3 && b < 13) {
                    int q = Math.min(Math.abs(8 - a), Math.abs(8 - b));

                    for (int g = 0; g < q; g++) {
                        placeBlock(world, x + a, y + 11 - g, z + b, 2, cell);
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
        set(world, x + 8, y + 2, z + 8, EldritchContent.ELDRITCH_CAPSTONE.get().defaultBlockState());
        ItemEntity tablet = new ItemEntity(world, x + 8.5, y + 3.5, z + 8.5, new ItemStack(EldritchContent.RUNED_TABLET.get()));
        tablet.setDeltaMovement(0.0, 0.0, 0.0);
        tablet.setDefaultPickUpDelay();
        tablet.setUnlimitedLifetime();
        world.addFreshEntity(tablet);
        int guardianCount = switch (world.getDifficulty()) {
            case HARD -> 4;
            case NORMAL -> 3;
            default -> 2;
        };
        for (int index = 0; index < guardianCount; index++) {
            EldritchGuardian guardian = ModEntities.ELDRITCH_GUARDIAN.get().create(world);
            if (guardian == null) continue;
            double offsetX = (1 + random.nextInt(3)) * (random.nextInt(3) - 1);
            double offsetZ = (1 + random.nextInt(3)) * (random.nextInt(3) - 1);
            guardian.moveTo(x + 8.5 + offsetX, y + 2, z + 8.5 + offsetZ, random.nextFloat() * 360.0F, 0.0F);
            guardian.restrictTo(new BlockPos(x + 8, y + 2, z + 8), 16);
            if (index == 0 && guardianCount >= 4) {
                ChampionModifier.assign(guardian, random.nextInt(13), true);
            }
            world.addFreshEntity(guardian);
        }
    }
}
