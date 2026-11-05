package thaumcraft.essentia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectSource;
import thaumcraft.device.EssentiaMirrorBlockEntity;
import thaumcraft.network.EssentiaSourceFxPayload;
import thaumcraft.platform.ModNetwork;

public final class EssentiaHandler {
    private static final long DELAY_TICKS = 100L;
    private static final Map<GlobalPos, List<BlockPos>> SOURCES = new HashMap<>();
    private static final Map<GlobalPos, Long> SOURCES_DELAY = new HashMap<>();

    private EssentiaHandler() {
    }

    public static boolean drainEssentia(BlockEntity tile, Aspect aspect, @Nullable Direction direction, int range) {
        return drainEssentia(tile, aspect, direction, range, false);
    }

    public static boolean drainEssentia(BlockEntity tile, Aspect aspect, @Nullable Direction direction, int range, boolean ignoreMirror) {
        Level level = tile.getLevel();
        if (level == null) {
            return false;
        }
        GlobalPos tileLocation = GlobalPos.of(level.dimension(), tile.getBlockPos());
        if (!SOURCES.containsKey(tileLocation)) {
            findSources(level, tileLocation, direction, range);
            return SOURCES.containsKey(tileLocation) && drainEssentia(tile, aspect, direction, range, ignoreMirror);
        }
        for (BlockPos source : SOURCES.get(tileLocation)) {
            if (!(level.getBlockEntity(source) instanceof AspectSource sourceTile)) {
                break;
            }
            if (!(ignoreMirror && sourceTile instanceof EssentiaMirrorBlockEntity) && sourceTile.takeFromContainer(aspect, 1)) {
                if (level instanceof ServerLevel serverLevel) {
                    BlockPos position = tile.getBlockPos();
                    ModNetwork.sendToNear(
                        serverLevel,
                        null,
                        position.getX(),
                        position.getY(),
                        position.getZ(),
                        32.0,
                        new EssentiaSourceFxPayload(
                            position,
                            (byte) (position.getX() - source.getX()),
                            (byte) (position.getY() - source.getY()),
                            (byte) (position.getZ() - source.getZ()),
                            aspect.getColor()
                        )
                    );
                }
                return true;
            }
        }
        SOURCES.remove(tileLocation);
        SOURCES_DELAY.put(tileLocation, level.getGameTime() + DELAY_TICKS);
        return false;
    }

    public static boolean findEssentia(BlockEntity tile, Aspect aspect, @Nullable Direction direction, int range) {
        Level level = tile.getLevel();
        if (level == null) {
            return false;
        }
        GlobalPos tileLocation = GlobalPos.of(level.dimension(), tile.getBlockPos());
        if (!SOURCES.containsKey(tileLocation)) {
            findSources(level, tileLocation, direction, range);
            return SOURCES.containsKey(tileLocation) && findEssentia(tile, aspect, direction, range);
        }
        for (BlockPos source : SOURCES.get(tileLocation)) {
            if (!(level.getBlockEntity(source) instanceof AspectSource sourceTile)) {
                break;
            }
            if (sourceTile.doesContainerContainAmount(aspect, 1)) {
                return true;
            }
        }
        SOURCES.remove(tileLocation);
        SOURCES_DELAY.put(tileLocation, level.getGameTime() + DELAY_TICKS);
        return false;
    }

    private static void findSources(Level level, GlobalPos tileLocation, @Nullable Direction direction, int range) {
        Long delayUntil = SOURCES_DELAY.get(tileLocation);
        if (delayUntil != null) {
            if (delayUntil > level.getGameTime()) {
                return;
            }
            SOURCES_DELAY.remove(tileLocation);
        }
        BlockPos origin = tileLocation.pos();
        BlockEntity requester = level.getBlockEntity(origin);
        List<BlockPos> sourceList = new ArrayList<>();
        int start = 0;
        Direction scanDirection = direction;
        if (scanDirection == null) {
            start = -range;
            scanDirection = Direction.UP;
        }
        for (int first = -range; first <= range; first++) {
            for (int second = -range; second <= range; second++) {
                for (int depth = start; depth < range; depth++) {
                    if (first == 0 && second == 0 && depth == 0) {
                        continue;
                    }
                    BlockPos target;
                    if (scanDirection.getStepY() != 0) {
                        target = origin.offset(first, depth * scanDirection.getStepY(), second);
                    } else if (scanDirection.getStepX() == 0) {
                        target = origin.offset(first, second, depth * scanDirection.getStepZ());
                    } else {
                        target = origin.offset(depth * scanDirection.getStepX(), first, second);
                    }
                    if (level.hasChunkAt(target) && level.getBlockEntity(target) instanceof AspectSource candidate && !isLinkedBack(requester, candidate, tileLocation)) {
                        sourceList.add(target);
                    }
                }
            }
        }
        if (sourceList.isEmpty()) {
            SOURCES_DELAY.put(tileLocation, level.getGameTime() + DELAY_TICKS);
        } else {
            SOURCES.put(tileLocation, sourceList);
        }
    }

    private static boolean isLinkedBack(@Nullable BlockEntity requester, AspectSource candidate, GlobalPos requesterLocation) {
        return requester instanceof EssentiaMirrorBlockEntity
            && candidate instanceof EssentiaMirrorBlockEntity mirror
            && requesterLocation.equals(mirror.getLink());
    }

    static void clearLevel(Level level) {
        SOURCES.keySet().removeIf(location -> location.dimension().equals(level.dimension()));
        SOURCES_DELAY.keySet().removeIf(location -> location.dimension().equals(level.dimension()));
    }

    static void clearAll() {
        SOURCES.clear();
        SOURCES_DELAY.clear();
    }
}
