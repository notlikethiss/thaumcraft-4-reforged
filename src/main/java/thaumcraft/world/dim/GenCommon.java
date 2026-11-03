package thaumcraft.world.dim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.server.level.ServerLevel;
import thaumcraft.block.LootBlock;
import thaumcraft.eldritch.EldritchBlock;
import thaumcraft.registry.EldritchContent;
import thaumcraft.registry.ModBlocks;

public class GenCommon {
    private final java.util.List<BlockPos> decoCommon = new java.util.ArrayList<>();
    private final java.util.List<BlockPos> crabSpawner = new java.util.ArrayList<>();
    private final java.util.List<BlockPos> decoUrn = new java.util.ArrayList<>();
    public static final int BEDROCK = 1;
    public static final int STONE = 2;
    public static final int VOID = 8;
    public static final int AIR_REPL = 9;
    public static final int DOOR_BLOCK = 15;
    public static final int DOOR_LOCK = 16;
    public static final int VOID_DOOR = 17;
    public static final int ROCK = 18;
    public static final int STONE_NOSPAWN = 19;
    public static final int STONE_TRAPPED = 20;
    public static final int CRUST = 21;
    public static final int[][] PAT_CONNECT = new int[][]{
        {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
        {1, 8, 8, 8, 8, 8, 8, 8, 8, 8, 1},
        {1, 8, 8, 2, 2, 2, 2, 2, 8, 8, 1},
        {1, 8, 2, 5, 9, 9, 9, 6, 2, 8, 1},
        {1, 8, 2, 9, 9, 9, 9, 9, 2, 8, 1},
        {1, 8, 2, 9, 9, 9, 9, 9, 2, 8, 1},
        {1, 8, 2, 9, 9, 9, 9, 9, 2, 8, 1},
        {1, 8, 2, 3, 9, 9, 9, 4, 2, 8, 1},
        {1, 8, 8, 2, 2, 2, 2, 2, 8, 8, 1},
        {1, 8, 8, 8, 8, 8, 8, 8, 8, 8, 1},
        {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0}
    };

    protected void placeBlock(ServerLevel level, int x, int y, int z, int code, Cell cell) {
        placeBlock(level, x, y, z, code, Direction.UP, cell);
    }

    protected void placeBlock(ServerLevel level, int x, int y, int z, int code, Direction facing, Cell cell) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = null;
        boolean nothing = level.getBlockState(pos).is(EldritchContent.ELDRITCH_NOTHING.get());
        boolean crust = code == 21;
        var random = level.getRandom();
        switch (code) {
            case BEDROCK:
                if (level.isEmptyBlock(pos)) state = Blocks.BEDROCK.defaultBlockState();
                break;
            case STONE:
                if (cell.feature == 7 && random.nextInt(3) == 0) {
                    crust = true;
                    break;
                }
                if (!nothing) {
                    if (random.nextInt(25) == 0) {
                        boolean crab = cell.feature == 7 || random.nextInt(50) == 0;
                        (crab && (cell.feature == 0 || cell.feature == 7) ? crabSpawner : decoCommon).add(pos);
                    }
                    state = ModBlocks.ANCIENT_STONE.get().defaultBlockState();
                }
                break;
            case 3:
            case 4:
                if (random.nextFloat() < 0.005F) decoUrn.add(pos);
            case 5:
            case 6:
                Direction step = Direction.EAST;
                boolean vertical = facing == Direction.NORTH || facing == Direction.SOUTH;
                if (facing.getAxis().isHorizontal()) {
                    step = code == 3 || code == 5 ? (vertical ? Direction.WEST : Direction.NORTH) : (vertical ? Direction.EAST : Direction.SOUTH);
                }
                state = stair(step, code == 5 || code == 6 ? Half.TOP : Half.BOTTOM);
                break;
            case 7:
                state = EldritchContent.GLOWING_CRUSTED_STONE.get().defaultBlockState();
                break;
            case VOID:
                state = EldritchContent.ELDRITCH_NOTHING.get().defaultBlockState();
                break;
            case AIR_REPL:
                state = Blocks.AIR.defaultBlockState();
                removeDecorations(pos);
                break;
            case 10:
            case 11:
                state = stair(facing.getAxis().isHorizontal() ? facing : Direction.EAST, code == 11 ? Half.TOP : Half.BOTTOM);
                break;
            case DOOR_BLOCK:
                state = EldritchContent.ANCIENT_DOORWAY.get().defaultBlockState();
                removeDecorations(pos);
                break;
            case DOOR_LOCK:
                state = EldritchContent.ANCIENT_LOCKING_MECHANISM.get().defaultBlockState().setValue(thaumcraft.eldritch.EldritchBlock.FACING, facing.getAxis().isHorizontal() ? facing : Direction.NORTH);
                removeDecorations(pos);
                break;
            case VOID_DOOR:
                state = ModBlocks.IMPASSABLE_FIELD.get().defaultBlockState();
                break;
            case ROCK:
                if (!nothing) state = ModBlocks.ANCIENT_ROCK.get().defaultBlockState();
                break;
            case STONE_NOSPAWN:
                if (!nothing) state = ModBlocks.ANCIENT_STONE_NO_SPAWN.get().defaultBlockState();
                break;
            case STONE_TRAPPED:
                if (!nothing) state = EldritchContent.RUNED_STONE.get().defaultBlockState();
                break;
            case 99:
                state = Blocks.BEDROCK.defaultBlockState();
                break;
            default:
                break;
        }
        if (crust && !nothing) {
            state = ModBlocks.CRUSTED_STONE.get().defaultBlockState();
            if (random.nextInt(25) == 0) {
                state = EldritchContent.GLOWING_CRUSTED_STONE.get().defaultBlockState();
            } else if (random.nextInt(25) == 0) {
                boolean crab = cell.feature == 7 || cell.feature == 12 && random.nextBoolean() || random.nextInt(25) == 0;
                if (crab && (cell.feature == 0 || cell.feature == 7 || cell.feature == 12)) crabSpawner.add(pos);
            }
        }
        if (state != null) {
            int flags = state.is(EldritchContent.ELDRITCH_NOTHING.get()) || state.is(Blocks.BEDROCK) || state.isAir() ? 0 : 3;
            level.setBlock(pos, state, flags);
        }
    }

    private void removeDecorations(BlockPos pos) {
        decoCommon.remove(pos);
        crabSpawner.remove(pos);
        decoUrn.remove(pos);
    }

    private BlockState stair(Direction facing, Half half) {
        return ModBlocks.ANCIENT_STONE_STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, half);
    }

    protected static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, 3);
    }

    public void processDecorations(ServerLevel level) {
        var random = level.getRandom();
        for (BlockPos pos : decoUrn) {
            if (level.isEmptyBlock(pos.above())) {
                level.setBlock(pos, ModBlocks.ANCIENT_STONE_PEDESTAL.get().defaultBlockState(), 3);
                float rarity = random.nextFloat();
                LootBlock.Tier tier = rarity < 0.025F ? LootBlock.Tier.RARE : rarity < 0.1F ? LootBlock.Tier.UNCOMMON : LootBlock.Tier.COMMON;
                level.setBlock(pos.above(), ModBlocks.LOOT_URN.get().defaultBlockState().setValue(LootBlock.TIER, tier), 3);
            }
        }
        for (BlockPos pos : decoCommon) {
            int exposed = exposedFaceCount(level, pos);
            if (exposed > 0 && (exposed == 1 || !isBedrockShowing(level, pos)) && !hasNearbyEldritch(level, pos)) {
                int meta = random.nextInt(3) != 0 ? 4 : random.nextInt(8) != 0 ? 5 : 10;
                var block = meta == 4 ? EldritchContent.GLOWING_CRUSTED_STONE.get() : meta == 5 ? EldritchContent.GLYPHED_STONE.get() : EldritchContent.RUNED_STONE.get();
                level.setBlock(pos, block.defaultBlockState(), 3);
                if (meta == 4 && random.nextInt(12) == 0) {
                    for (Direction direction : Direction.values()) {
                        BlockPos target = pos.relative(direction);
                        if (level.isEmptyBlock(target)) {
                            level.setBlock(target, ModBlocks.STRANGE_CRYSTALS.get().defaultBlockState().setValue(thaumcraft.device.CrystalClusterBlock.FACING, direction), 3);
                            break;
                        }
                    }
                }
            }
        }
        for (BlockPos pos : crabSpawner) {
            if (exposedFaceCount(level, pos) == 1 && !hasNearbyEldritch(level, pos)) {
                BlockState state = EldritchContent.CRUSTED_OPENING.get().defaultBlockState();
                for (Direction direction : Direction.values()) {
                    if (level.isEmptyBlock(pos.relative(direction))) {
                        if (state.hasProperty(EldritchBlock.FACING)) state = state.setValue(EldritchBlock.FACING, direction);
                        break;
                    }
                }
                level.setBlock(pos, state, 3);
            }
        }
        decoUrn.clear();
        decoCommon.clear();
        crabSpawner.clear();
    }

    private int exposedFaceCount(ServerLevel level, BlockPos pos) {
        int exposed = 0;
        for (Direction direction : Direction.values()) {
            if (level.isEmptyBlock(pos.relative(direction))) exposed++;
        }
        return exposed;
    }

    private boolean hasNearbyEldritch(ServerLevel level, BlockPos pos) {
        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (!nearby.equals(pos) && level.getBlockState(nearby).getBlock() instanceof EldritchBlock) return true;
        }
        return false;
    }

    static boolean isBedrockShowing(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState opposite = level.getBlockState(pos.relative(direction.getOpposite()));
            if (!level.getBlockState(pos.relative(direction)).isSolidRender() && (opposite.is(Blocks.BEDROCK) || opposite.is(EldritchContent.ELDRITCH_NOTHING.get()))) return true;
        }
        return false;
    }

    public static void genObelisk(ServerLevel level, int x, int y, int z) {
        BlockState state = EldritchContent.ELDRITCH_OBELISK.get().defaultBlockState();
        set(level, x, y, z, state.setValue(EldritchBlock.PART, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
        for (int dy = 1; dy <= 4; dy++) {
            set(level, x, y + dy, z, state.setValue(EldritchBlock.PART, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        }
    }

    public void generateConnections(ServerLevel level, net.minecraft.util.RandomSource random, int cx, int cz, int y, Cell cell, int depth, boolean justTheTip) {
        int x = cx * 16;
        int z = cz * 16;
        if (cell.north) {
            for (int d = 0; d <= depth; d++) {
                int from = d == depth && justTheTip ? 2 : d == depth - 1 && justTheTip ? 1 : 0;
                int to = d == depth && justTheTip ? 9 : d == depth - 1 && justTheTip ? 10 : 11;
                for (int w = from; w < to; w++) for (int h = from; h < to; h++) {
                    if (d != depth || !justTheTip || PAT_CONNECT[h][w] != 8) placeBlock(level, x + 3 + w, y + 10 - h, z + d, PAT_CONNECT[h][w], Direction.NORTH, cell);
                }
            }
        }
        if (cell.south) {
            for (int d = 0; d <= depth; d++) {
                int from = d == depth && justTheTip ? 2 : d == depth - 1 && justTheTip ? 1 : 0;
                int to = d == depth && justTheTip ? 9 : d == depth - 1 && justTheTip ? 10 : 11;
                for (int w = from; w < to; w++) for (int h = from; h < to; h++) {
                    if (d != depth || !justTheTip || PAT_CONNECT[h][w] != 8) placeBlock(level, x + 3 + w, y + 10 - h, z + 16 - d, PAT_CONNECT[h][w], Direction.SOUTH, cell);
                }
            }
        }
        if (cell.east) {
            for (int d = 0; d <= depth; d++) {
                int from = d == depth && justTheTip ? 2 : d == depth - 1 && justTheTip ? 1 : 0;
                int to = d == depth && justTheTip ? 9 : d == depth - 1 && justTheTip ? 10 : 11;
                for (int w = from; w < to; w++) for (int h = from; h < to; h++) {
                    if (d != depth || !justTheTip || PAT_CONNECT[h][w] != 8) placeBlock(level, x + 16 - d, y + 10 - h, z + 3 + w, PAT_CONNECT[h][w], Direction.EAST, cell);
                }
            }
        }
        if (cell.west) {
            for (int d = 0; d <= depth; d++) {
                int from = d == depth && justTheTip ? 2 : d == depth - 1 && justTheTip ? 1 : 0;
                int to = d == depth && justTheTip ? 9 : d == depth - 1 && justTheTip ? 10 : 11;
                for (int w = from; w < to; w++) for (int h = from; h < to; h++) {
                    if (d != depth || !justTheTip || PAT_CONNECT[h][w] != 8) placeBlock(level, x + d, y + 10 - h, z + 3 + w, PAT_CONNECT[h][w], Direction.WEST, cell);
                }
            }
        }
    }
}
