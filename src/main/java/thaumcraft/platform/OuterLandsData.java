package thaumcraft.platform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.HolderLookup;
import thaumcraft.world.dim.CellLoc;

public final class OuterLandsData extends SavedData {
    private static final Codec<CellData> CELL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        CellLoc.CODEC.fieldOf("location").forGetter(CellData::location),
        Codec.SHORT.fieldOf("value").forGetter(CellData::value)
    ).apply(instance, CellData::new));
    private static final Codec<MazeState> MAZE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("seed").forGetter(MazeState::seed),
        Codec.INT.fieldOf("origin_x").forGetter(MazeState::originX),
        Codec.INT.fieldOf("origin_z").forGetter(MazeState::originZ),
        Codec.INT.fieldOf("width").forGetter(MazeState::width),
        Codec.INT.fieldOf("height").forGetter(MazeState::height),
        Codec.list(CELL_CODEC).fieldOf("cells").forGetter(MazeState::cells),
        Codec.list(CellLoc.CODEC).fieldOf("placed").forGetter(MazeState::placed),
        Codec.BOOL.optionalFieldOf("generation_started", true).forGetter(MazeState::generationStarted)
    ).apply(instance, MazeState::new));
    public static final Codec<OuterLandsData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.unboundedMap(Codec.STRING, MAZE_CODEC).fieldOf("mazes").forGetter(data -> data.mazes),
        Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("aliases").forGetter(data -> data.aliases),
        Codec.INT.fieldOf("boss_count").forGetter(data -> data.bossCount)
    ).apply(instance, OuterLandsData::new));
    public static final SavedData.Factory<OuterLandsData> TYPE = new SavedData.Factory<>(OuterLandsData::new, (tag, lookup) -> CODEC.parse(lookup.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow());

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        tag.merge((CompoundTag) CODEC.encodeStart(lookup.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow());
        return tag;
    }

    private final Map<String, MazeState> mazes;
    private final Map<String, String> aliases;
    private int bossCount;

    public OuterLandsData() {
        mazes = new HashMap<>();
        aliases = new HashMap<>();
    }

    private OuterLandsData(Map<String, MazeState> mazes, Map<String, String> aliases, int bossCount) {
        this.mazes = new HashMap<>(mazes);
        this.aliases = new HashMap<>(aliases);
        this.bossCount = bossCount;
    }

    public Map<String, MazeState> mazes() {
        return Map.copyOf(mazes);
    }

    public MazeState maze(String id) {
        return mazes.get(aliases.getOrDefault(id, id));
    }

    public void putMaze(String id, MazeState maze) {
        mazes.put(id, maze);
        setDirty();
    }

    public void putAlias(String id, String target) {
        aliases.put(id, target);
        setDirty();
    }

    public String canonicalId(String id) {
        return aliases.getOrDefault(id, id);
    }

    public void updateMaze(String id, MazeState maze) {
        mazes.put(canonicalId(id), maze);
        setDirty();
    }

    public int nextBoss(RandomSource random) {
        bossCount++;
        if (random.nextFloat() < 0.25F) {
            bossCount++;
        }
        setDirty();
        return bossCount % 4;
    }

    public record CellData(CellLoc location, short value) {
    }

    public record MazeState(long seed, int originX, int originZ, int width, int height, List<CellData> cells, List<CellLoc> placed, boolean generationStarted) {
        public MazeState(long seed, int originX, int originZ, int width, int height, List<CellData> cells, List<CellLoc> placed) {
            this(seed, originX, originZ, width, height, cells, placed, true);
        }

        public MazeState {
            cells = new ArrayList<>(cells);
            placed = new ArrayList<>(placed);
        }

        public MazeState withPlaced(CellLoc loc) {
            Set<CellLoc> updated = new HashSet<>(placed);
            updated.add(loc);
            return new MazeState(seed, originX, originZ, width, height, cells, new ArrayList<>(updated), generationStarted);
        }

        public MazeState withGenerationStarted() {
            return new MazeState(seed, originX, originZ, width, height, cells, placed, true);
        }
    }
}
