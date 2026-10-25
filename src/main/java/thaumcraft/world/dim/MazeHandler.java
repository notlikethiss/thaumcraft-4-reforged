package thaumcraft.world.dim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.platform.OuterLandsData;
import thaumcraft.registry.ModBlocks;
import thaumcraft.world.dim.OuterLands;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class MazeHandler {
    private static final ConcurrentLinkedQueue<MazeRequest> REQUESTS = new ConcurrentLinkedQueue<>();
    private static final Map<ServerSeed, Set<CellLoc>> MAZE_CELLS = new ConcurrentHashMap<>();
    private static final Object NATURAL_RING_LOCK = new Object();

    private MazeHandler() {
    }

    public static boolean ensureMaze(ServerLevel level, BlockPos entrance) {
        RandomSource random = level.getRandom();
        int width = 15 + random.nextInt(8) * 2;
        int height = 15 + random.nextInt(8) * 2;
        long seed = random.nextLong();
        return ensureMaze(level, entrance.getX() >> 4, entrance.getZ() >> 4, width, height, seed);
    }

    public static boolean ensureMaze(ServerLevel level, int originChunkX, int originChunkZ, long requestedSeed) {
        return ensureMaze(level, originChunkX, originChunkZ, 0, 0, requestedSeed);
    }

    public static boolean ensureMaze(ServerLevel level, int originChunkX, int originChunkZ, int width, int height, long requestedSeed) {
        return requestMaze(level, originChunkX, originChunkZ, width, height, requestedSeed, true);
    }

    private static boolean requestMaze(ServerLevel level, int originChunkX, int originChunkZ, int width, int height, long requestedSeed, boolean startGeneration) {
        synchronized (NATURAL_RING_LOCK) {
            OuterLandsData data = data(level);
            String id = mazeId(originChunkX, originChunkZ);
            OuterLandsData.MazeState state = data.maze(id);
            if (state == null) {
                // An eye can be inserted before the next tick drains the worldgen queue.
                MazeRequest reservation = REQUESTS.stream()
                    .filter(request -> request.server() == level.getServer() && request.worldSeed() == level.getSeed()
                        && request.originChunkX() == originChunkX && request.originChunkZ() == originChunkZ)
                    .findFirst().orElse(null);
                if (reservation != null) {
                    width = reservation.width();
                    height = reservation.height();
                    requestedSeed = reservation.seed();
                }
                OuterLandsData.MazeState requestedMaze = createMaze(originChunkX, originChunkZ, width, height, requestedSeed, startGeneration);
                OuterLandsData.MazeState overlap = data.mazes().values().stream()
                    .filter(existing -> Math.abs(existing.originX() - originChunkX) <= (existing.width() + requestedMaze.width()) / 2
                        && Math.abs(existing.originZ() - originChunkZ) <= (existing.height() + requestedMaze.height()) / 2)
                    .min(Comparator.comparingLong(existing -> {
                        long dx = existing.originX() - (long) originChunkX;
                        long dz = existing.originZ() - (long) originChunkZ;
                        return dx * dx + dz * dz;
                    }))
                    .orElse(null);
                if (overlap == null) {
                    data.putMaze(id, requestedMaze);
                    state = requestedMaze;
                } else {
                    String overlapId = data.mazes().entrySet().stream()
                        .filter(entry -> entry.getValue() == overlap)
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(id);
                    data.putAlias(id, overlapId);
                    state = overlap;
                }
            }
            if (startGeneration && !state.generationStarted()) {
                state = state.withGenerationStarted();
                data.updateMaze(id, state);
            }
            publishSnapshot(level);
            return state.generationStarted();
        }
    }

    public static void placeAround(ServerLevel level, BlockPos center, int radius) {
        OuterLandsData data = data(level);
        int chunkX = center.getX() >> 4;
        int chunkZ = center.getZ() >> 4;
        for (Map.Entry<String, OuterLandsData.MazeState> entry : data.mazes().entrySet()) {
            if (!entry.getValue().generationStarted()) {
                continue;
            }
            for (OuterLandsData.CellData cell : entry.getValue().cells()) {
                CellLoc location = cell.location();
                if (Math.abs(location.x() - chunkX) > radius || Math.abs(location.z() - chunkZ) > radius) {
                    continue;
                }
                OuterLandsData.MazeState current = data.maze(entry.getKey());
                if (!current.placed().contains(location)) {
                    placeCell(level, data, entry.getKey(), current, cell);
                }
            }
        }
    }

    private static void placeCell(ServerLevel level, OuterLandsData data, String id, OuterLandsData.MazeState maze, OuterLandsData.CellData cell) {
        CellLoc location = cell.location();
        level.getChunk(location.x(), location.z());
        RandomSource random = RandomSource.create(maze.seed() ^ ((long) location.x() * 341873128712L) ^ ((long) location.z() * 132897987541L));
        MazeRooms.generateCell(level, random, location.x(), location.z(), new Cell(cell.value()));
        data.updateMaze(id, data.maze(id).withPlaced(location));
    }

    public static Cell getCell(ServerLevel level, int chunkX, int chunkZ) {
        CellLoc location = new CellLoc(chunkX, chunkZ);
        for (OuterLandsData.MazeState maze : data(level).mazes().values()) {
            for (OuterLandsData.CellData cell : maze.cells()) {
                if (cell.location().equals(location)) {
                    return new Cell(cell.value());
                }
            }
        }
        return null;
    }

    public static Cell getCell(ServerLevel level, BlockPos pos) {
        return getCell(level, pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static boolean isReady(ServerLevel level, int originChunkX, int originChunkZ) {
        OuterLandsData.MazeState state = data(level).maze(mazeId(originChunkX, originChunkZ));
        return state != null && isReady(state);
    }

    public static BlockPos entrancePos(ServerLevel level, int originChunkX, int originChunkZ) {
        OuterLandsData.MazeState maze = data(level).maze(mazeId(originChunkX, originChunkZ));
        if (maze == null) {
            return null;
        }
        for (OuterLandsData.CellData entry : maze.cells()) {
            Cell cell = new Cell(entry.value());
            if (cell.feature == 1) {
                return new BlockPos(entry.location().x() * 16 + 8, 54, entry.location().z() * 16 + 8);
            }
        }
        return null;
    }

    public static BlockPos entrancePos(ServerLevel level, BlockPos origin) {
        return entrancePos(level, origin.getX() >> 4, origin.getZ() >> 4);
    }

    public static int nextBoss(ServerLevel level, RandomSource random) {
        OuterLandsData data = data(level);
        return data.nextBoss(random);
    }

    public static void generateBossRoom(ServerLevel level, BlockPos lockPos, thaumcraft.registry.EldritchEntities.BossKind kind) {
        Arena arena = findArena(level, lockPos);
        int centerX = arena.chunkX();
        int centerZ = arena.chunkZ();
        int exit = arena.exit();
        int x = centerX * 16 + 16;
        int y = 50;
        int z = centerZ * 16 + 16;
        RandomSource random = level.getRandom();
        switch (kind) {
            case GOLEM -> generateGolemArena(level, random, x, y, z, exit);
            case WARDEN -> generateWardenArena(level, random, x, y, z, exit);
            case CULTIST_PORTAL -> generateCultistArena(level, random, x, y, z);
            case TAINTACLE_GIANT -> generateTaintArena(level, random, x, y, z);
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = lockPos.offset(dx, dy, dz);
                    if (level.getBlockState(pos).is(thaumcraft.registry.ModBlocks.STATIC_FIELD.get())
                        || level.getBlockState(pos).is(thaumcraft.registry.ModBlocks.SAPPING_FIELD.get())
                        || level.getBlockState(pos).is(thaumcraft.registry.ModBlocks.IMPASSABLE_FIELD.get())) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
    }

    public static BlockPos arenaCenter(ServerLevel level, BlockPos lockPos) {
        Arena arena = findArena(level, lockPos);
        return new BlockPos(arena.chunkX() * 16 + 16, 50, arena.chunkZ() * 16 + 16);
    }

    public static BlockPos bossSpawnPos(ServerLevel level, BlockPos lockPos, thaumcraft.registry.EldritchEntities.BossKind kind) {
        Arena arena = findArena(level, lockPos);
        int x = arena.chunkX() * 16 + 16;
        int z = arena.chunkZ() * 16 + 16;
        if (kind == thaumcraft.registry.EldritchEntities.BossKind.WARDEN) {
            int dx = exitDx(arena.exit());
            int dz = exitDz(arena.exit());
            return new BlockPos(x + dx, 53, z + dz);
        }
        int y = kind == thaumcraft.registry.EldritchEntities.BossKind.CULTIST_PORTAL ? 52 : 53;
        return new BlockPos(x, y, z);
    }

    private static Arena findArena(ServerLevel level, BlockPos lockPos) {
        int lockX = lockPos.getX() >> 4;
        int lockZ = lockPos.getZ() >> 4;
        int centerX = lockX;
        int centerZ = lockZ;
        int exit = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Cell cell = getCell(level, lockX + dx, lockZ + dz);
                if (cell == null) continue;
                if (cell.feature == 2) {
                    centerX = lockX + dx;
                    centerZ = lockZ + dz;
                }
                if (cell.feature >= 2 && cell.feature <= 5 && (cell.north || cell.south || cell.east || cell.west)) exit = cell.feature;
            }
        }
        return new Arena(centerX, centerZ, exit);
    }

    private record Arena(int chunkX, int chunkZ, int exit) {
    }

    private static int exitDx(int exit) {
        return exit == 2 || exit == 4 ? 8 : exit == 3 || exit == 5 ? -8 : 0;
    }

    private static int exitDz(int exit) {
        return exit == 2 || exit == 3 ? 8 : exit == 4 || exit == 5 ? -8 : 0;
    }

    private static void generateGolemArena(ServerLevel level, RandomSource random, int x, int y, int z, int exit) {
        int dx = exitDx(exit);
        int dz = exitDz(exit);
        GenCommon.genObelisk(level, x + dx, y + 4, z + dz);
        GenCommon.genObelisk(level, x - dx, y + 4, z + dz);
        GenCommon.genObelisk(level, x + dx, y + 4, z - dz);
        for (int[] point : new int[][]{{dx,dz},{-dx,dz},{dx,-dz}}) set(level, x + point[0], y + 2, z + point[1], thaumcraft.registry.EldritchContent.ELDRITCH_CAPSTONE.get().defaultBlockState());
        int[][] pedestals = {{2, 2, 2}, {0, -1, 1}, {3, 3, 3}};
        for (int a = 0; a < 3; a++) for (int b = 0; b < 3; b++) {
            set(level, x - 1 + b, y + 2, z - 1 + a, pedestal(pedestals[a][b]));
        }
        for (int a = -10; a <= 10; a++) for (int b = -10; b <= 10; b++) {
            if ((Math.abs(a) > 2 && Math.abs(b) > 2) && random.nextFloat() < 0.15F && level.isEmptyBlock(new BlockPos(x + a, y + 2, z + b))) {
                set(level, x + a, y + 2, z + b, random.nextFloat() < 0.3F ? lootBlock(random) : lootUrn(random));
            }
        }
    }

    private static void generateWardenArena(ServerLevel level, RandomSource random, int x, int y, int z, int exit) {
        int dx = exitDx(exit);
        int dz = exitDz(exit);
        GenCommon.genObelisk(level, x + dx, y + 4, z);
        GenCommon.genObelisk(level, x, y + 4, z + dz);
        set(level, x + dx, y + 2, z, thaumcraft.registry.EldritchContent.ELDRITCH_CAPSTONE.get().defaultBlockState());
        set(level, x, y + 2, z + dz, thaumcraft.registry.EldritchContent.ELDRITCH_CAPSTONE.get().defaultBlockState());
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
            if (a != 0 && b != 0 && random.nextFloat() < 0.9F) set(level, x + dx + a, y + 2, z + b, lootUrn(random));
            if (a != 0 && b != 0 && random.nextFloat() < 0.9F) set(level, x + a, y + 2, z + dz + b, lootUrn(random));
        }
        for (int a : new int[]{-2,2}) for (int b : new int[]{-2,2}) {
            set(level, x + a, y + 3, z + b, thaumcraft.registry.EldritchContent.RUNED_STONE.get().defaultBlockState());
            set(level, x + a, y + 2, z + b, ModBlocks.ANCIENT_STONE_PEDESTAL.get().defaultBlockState());
        }
        int[][] pedestals = {{2, 2, 2}, {0, -1, 1}, {3, 3, 3}};
        for (int a = 0; a < 3; a++) for (int b = 0; b < 3; b++) {
            set(level, x + dx - 1 + b, y + 2, z + dz - 1 + a, pedestal(pedestals[a][b]));
        }
    }

    private static net.minecraft.world.level.block.state.BlockState pedestal(int meta) {
        if (meta < 0) return thaumcraft.registry.EldritchContent.GLOWING_CRUSTED_STONE.get().defaultBlockState();
        net.minecraft.core.Direction facing = switch (meta) {
            case 0 -> net.minecraft.core.Direction.EAST;
            case 1 -> net.minecraft.core.Direction.WEST;
            case 2 -> net.minecraft.core.Direction.SOUTH;
            default -> net.minecraft.core.Direction.NORTH;
        };
        return ModBlocks.ANCIENT_STONE_STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, Half.BOTTOM);
    }

    private static void generateCultistArena(ServerLevel level, RandomSource random, int x, int y, int z) {
        for (int a = -4; a <= 4; a++) for (int b = -4; b <= 4; b++) {
            if ((Math.abs(a) != 2 && Math.abs(b) != 2 || random.nextBoolean())
                && (Math.abs(a) != 3 && Math.abs(b) != 3 || random.nextFloat() <= 0.33F)
                && (Math.abs(a) != 4 && Math.abs(b) != 4 || random.nextFloat() <= 0.25F)) {
                set(level, x + b, y + 1, z + a, thaumcraft.registry.EldritchContent.ANCIENT_DOORWAY.get().defaultBlockState());
            }
        }
        for (int a = 0; a < 5; a++) for (int b = 0; b < 5; b++) {
            if (a == 0 || a == 4 || b == 0 || b == 4) {
                int px = x - 8 + b * 4, pz = z - 8 + a * 4;
                set(level, px, y + 2, pz, ModBlocks.ANCIENT_STONE.get().defaultBlockState());
                set(level, px, y + 3, pz, thaumcraft.registry.EldritchContent.GLYPHED_STONE.get().defaultBlockState());
                set(level, px, y + 4, pz, ModBlocks.ANCIENT_STONE_SLAB.get().defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.BOTTOM));
                set(level, px, y + 10, pz, ModBlocks.ANCIENT_STONE.get().defaultBlockState());
                set(level, px, y + 9, pz, thaumcraft.registry.EldritchContent.GLYPHED_STONE.get().defaultBlockState());
                set(level, px, y + 8, pz, ModBlocks.ANCIENT_STONE_SLAB.get().defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP));
            }
        }
    }

    private static void generateTaintArena(ServerLevel level, RandomSource random, int x, int y, int z) {
        for (int a = -12; a <= 12; a++) for (int b = -12; b <= 12; b++) {
            thaumcraft.taint.TaintBiomes.set(level, new BlockPos(x + b, y + 2, z + a), thaumcraft.taint.TaintBiomes.TAINTED_LAND);
            for (int dy = 0; dy < 9; dy++) {
                BlockPos pos = new BlockPos(x + b, y + 2 + dy, z + a);
                if (level.isEmptyBlock(pos) && isAdjacentToSolid(level, pos) && random.nextInt(3) != 0) set(level, pos.getX(), pos.getY(), pos.getZ(), ModBlocks.FIBROUS_TAINT.get().defaultBlockState());
            }
            if (random.nextFloat() < 0.15F) {
                set(level, x + b, y + 2, z + a, ModBlocks.CRUSTED_TAINT.get().defaultBlockState());
                if (random.nextFloat() < 0.2F) set(level, x + b, y + 3, z + a, ModBlocks.CRUSTED_TAINT.get().defaultBlockState());
            }
            if ((Math.abs(a) != 4 && Math.abs(b) != 4 || random.nextBoolean())
                && (Math.abs(a) < 5 && Math.abs(b) < 5 || random.nextFloat() <= 0.33F)
                && (Math.abs(a) < 7 && Math.abs(b) < 7 || random.nextFloat() <= 0.25F)) {
                set(level, x + b, y + 1, z + a, ModBlocks.TAINTED_SOIL.get().defaultBlockState());
            }
        }
    }

    private static boolean isAdjacentToSolid(ServerLevel level, BlockPos pos) {
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) if (level.getBlockState(pos.relative(direction)).canOcclude()) return true;
        return false;
    }

    private static net.minecraft.world.level.block.state.BlockState lootUrn(RandomSource random) {
        float roll = random.nextFloat();
        thaumcraft.block.LootBlock.Tier tier = roll < 0.1F ? thaumcraft.block.LootBlock.Tier.RARE : roll < 0.3F ? thaumcraft.block.LootBlock.Tier.UNCOMMON : thaumcraft.block.LootBlock.Tier.COMMON;
        return ModBlocks.LOOT_URN.get().defaultBlockState().setValue(thaumcraft.block.LootBlock.TIER, tier);
    }

    private static net.minecraft.world.level.block.state.BlockState lootBlock(RandomSource random) {
        float roll = random.nextFloat();
        thaumcraft.block.LootBlock.Tier tier = roll < 0.05F ? thaumcraft.block.LootBlock.Tier.RARE : roll < 0.2F ? thaumcraft.block.LootBlock.Tier.UNCOMMON : thaumcraft.block.LootBlock.Tier.COMMON;
        return ModBlocks.LOOT_CRATE.get().defaultBlockState().setValue(thaumcraft.block.LootBlock.TIER, tier);
    }

    private static void set(ServerLevel level, int x, int y, int z, net.minecraft.world.level.block.state.BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, 3);
    }

    private static OuterLandsData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(OuterLandsData.TYPE);
    }

    private static String mazeId(int chunkX, int chunkZ) {
        return chunkX + "," + chunkZ;
    }

    private static OuterLandsData.MazeState createMaze(int originChunkX, int originChunkZ, int requestedWidth, int requestedHeight, long requestedSeed, boolean startGeneration) {
        long seed = requestedSeed;
        Random random = new Random(seed);
        int width = requestedWidth > 0 ? requestedWidth : 15 + random.nextInt(8) * 2;
        int height = requestedHeight > 0 ? requestedHeight : 15 + random.nextInt(8) * 2;
        long mazeSeed = seed;
        MazeGenerator generator = new MazeGenerator(width, height, mazeSeed);
        while (!generator.generate()) {
            generator = new MazeGenerator(width, height, ++mazeSeed);
        }
        int column = originChunkX - (1 + width / 2);
        int row = originChunkZ - (1 + height / 2);
        List<OuterLandsData.CellData> cells = new ArrayList<>();
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                if (generator.grid[z][x] > 0) {
                    cells.add(new OuterLandsData.CellData(new CellLoc(column + x, row + z), (short) generator.grid[z][x]));
                }
            }
        }
        cells.sort(Comparator.comparing((OuterLandsData.CellData cell) -> cell.location().z())
            .thenComparing(cell -> cell.location().x()));
        return new OuterLandsData.MazeState(seed, originChunkX, originChunkZ, width, height, cells, List.of(), startGeneration);
    }

    private static boolean isReady(OuterLandsData.MazeState maze) {
        return maze.generationStarted() && maze.placed().size() >= maze.cells().size();
    }

    public static boolean reserveNaturalRing(MinecraftServer server, long worldSeed, int originChunkX, int originChunkZ, int width, int height, long seed) {
        synchronized (NATURAL_RING_LOCK) {
            ServerSeed key = new ServerSeed(server, worldSeed);
            if (naturalRingOverlaps(key, originChunkX, originChunkZ, width, height)) {
                return false;
            }
            OuterLandsData.MazeState maze = createMaze(originChunkX, originChunkZ, width, height, seed, false);
            MazeRequest request = new MazeRequest(server, worldSeed, originChunkX, originChunkZ, width, height, seed,
                maze.cells().stream().map(OuterLandsData.CellData::location).collect(java.util.stream.Collectors.toUnmodifiableSet()));
            REQUESTS.add(request);
            return true;
        }
    }

    public static boolean naturalRingInRange(MinecraftServer server, long worldSeed, int originChunkX, int originChunkZ, int width, int height) {
        synchronized (NATURAL_RING_LOCK) {
            return naturalRingOverlaps(new ServerSeed(server, worldSeed), originChunkX, originChunkZ, width, height);
        }
    }

    private static boolean naturalRingOverlaps(ServerSeed key, int chunkX, int chunkZ, int width, int height) {
        if (hasCellInRange(MAZE_CELLS.getOrDefault(key, Set.of()), chunkX, chunkZ, width, height)) {
            return true;
        }
        for (MazeRequest request : REQUESTS) {
            if (request.server() == key.server() && request.worldSeed() == key.worldSeed()
                && hasCellInRange(request.cells(), chunkX, chunkZ, width, height)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCellInRange(Set<CellLoc> cells, int chunkX, int chunkZ, int width, int height) {
        for (CellLoc cell : cells) {
            if (cell.x() >= chunkX - width && cell.x() <= chunkX + width
                && cell.z() >= chunkZ - height && cell.z() <= chunkZ + height) {
                return true;
            }
        }
        return false;
    }

    public static void publishSnapshot(ServerLevel level) {
        synchronized (NATURAL_RING_LOCK) {
            Set<CellLoc> cells = new HashSet<>();
            for (OuterLandsData.MazeState maze : data(level).mazes().values()) {
                for (OuterLandsData.CellData cell : maze.cells()) {
                    cells.add(cell.location());
                }
            }
            MAZE_CELLS.put(new ServerSeed(level.getServer(), level.getSeed()), Set.copyOf(cells));
        }
    }

    public static boolean mazesInRange(ServerLevel level, int chunkX, int chunkZ, int width, int height) {
        for (int x = -width; x <= width; x++) {
            for (int z = -height; z <= height; z++) {
                if (getCell(level, chunkX + x, chunkZ + z) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void placeQueuedCells(ServerTickEvent.Post event) {
        for (MazeRequest request : REQUESTS) {
            if (request.server() != event.getServer()) {
                continue;
            }
            for (ServerLevel candidate : event.getServer().getAllLevels()) {
                if (candidate.dimension().equals(OuterLands.OUTER_LANDS) && candidate.getSeed() == request.worldSeed()) {
                    synchronized (NATURAL_RING_LOCK) {
                        requestMaze(candidate, request.originChunkX(), request.originChunkZ(), request.width(), request.height(), request.seed(), false);
                        REQUESTS.remove(request);
                    }
                    break;
                }
            }
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.dimension().equals(OuterLands.OUTER_LANDS)) {
                continue;
            }
            OuterLandsData data = level.getDataStorage().get(OuterLandsData.TYPE);
            if (data == null) {
                continue;
            }
            List<CellLoc> players = level.players().stream()
                .map(player -> new CellLoc(player.getBlockX() >> 4, player.getBlockZ() >> 4))
                .toList();
            String pendingId = null;
            OuterLandsData.CellData pending = null;
            long pendingDistance = Long.MAX_VALUE;
            for (Map.Entry<String, OuterLandsData.MazeState> entry : data.mazes().entrySet()) {
                OuterLandsData.MazeState maze = entry.getValue();
                if (!maze.generationStarted()) {
                    continue;
                }
                Set<CellLoc> completed = new HashSet<>(maze.placed());
                for (OuterLandsData.CellData cell : maze.cells()) {
                    if (completed.contains(cell.location())) {
                        continue;
                    }
                    long distance = distanceToPlayers(cell.location(), players);
                    if (distance < pendingDistance) {
                        pendingId = entry.getKey();
                        pending = cell;
                        pendingDistance = distance;
                    }
                }
                if (pending != null && players.isEmpty()) {
                    break;
                }
            }
            if (pending != null) {
                placeCell(level, data, pendingId, data.maze(pendingId), pending);
                return;
            }
        }
    }

    private static long distanceToPlayers(CellLoc cell, List<CellLoc> players) {
        long nearest = players.isEmpty() ? 0L : Long.MAX_VALUE - 1;
        for (CellLoc player : players) {
            long dx = cell.x() - (long) player.x();
            long dz = cell.z() - (long) player.z();
            nearest = Math.min(nearest, dx * dx + dz * dz);
        }
        return nearest;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level.dimension().equals(OuterLands.OUTER_LANDS)) {
                publishSnapshot(level);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(OuterLands.OUTER_LANDS)) {
            publishSnapshot(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        synchronized (NATURAL_RING_LOCK) {
            REQUESTS.removeIf(request -> request.server() == event.getServer());
            MAZE_CELLS.keySet().removeIf(key -> key.server() == event.getServer());
        }
    }

    private record ServerSeed(MinecraftServer server, long worldSeed) {
    }

    private record MazeRequest(MinecraftServer server, long worldSeed, int originChunkX, int originChunkZ, int width, int height, long seed, Set<CellLoc> cells) {
    }
}
