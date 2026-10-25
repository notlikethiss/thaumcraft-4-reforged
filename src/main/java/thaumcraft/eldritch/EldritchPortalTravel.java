package thaumcraft.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.registry.EldritchContent;
import thaumcraft.platform.Teleport;
import thaumcraft.research.ResearchManager;
import thaumcraft.world.dim.MazeHandler;
import thaumcraft.world.dim.OuterLands;

public final class EldritchPortalTravel {
    private EldritchPortalTravel() {
    }

    public static boolean tryTravel(ServerLevel source, BlockPos portalPos, ServerPlayer player) {
        if (source != player.level() || player.isPassenger() || player.isVehicle()) {
            return false;
        }
        if (player.getPortalCooldown() > 0) {
            player.setPortalCooldown(100);
            return false;
        }
        boolean entering = !source.dimension().equals(OuterLands.OUTER_LANDS);
        ServerLevel destination;
        Vec3 target;
        if (entering) {
            destination = OuterLands.get(source);
            if (destination == null || !MazeHandler.ensureMaze(destination, portalPos)) {
                return false;
            }
            BlockPos entrance = MazeHandler.entrancePos(destination, portalPos);
            if (entrance == null) {
                return false;
            }
            MazeHandler.placeAround(destination, entrance, 1);
            target = arrival(destination, entrance);
        } else {
            destination = source.getServer().overworld();
            BlockPos portal = findPortal(destination, player.position());
            target = portal == null ? safeFallback(destination, player.position()) : arrival(destination, portal);
        }
        player.setPortalCooldown(100);
        Entity moved = Teleport.to(player, destination, target, player.getYRot(), player.getXRot());
        if (!(moved instanceof ServerPlayer transferred)) {
            return false;
        }
        transferred.setPortalCooldown(100);
        if (entering && !ResearchManager.isComplete(transferred, "ENTEROUTER")) {
            ResearchManager.complete(transferred, "ENTEROUTER");
        }
        return true;
    }

    public static @Nullable BlockPos findPortal(ServerLevel level, Vec3 origin) {
        BlockPos center = BlockPos.containing(origin);
        BlockPos nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        int minChunkX = (center.getX() - 128) >> 4;
        int maxChunkX = (center.getX() + 128) >> 4;
        int minChunkZ = (center.getZ() - 128) >> 4;
        int maxChunkZ = (center.getZ() + 128) >> 4;
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                LevelChunk chunk = level.getChunk(x, z);
                for (BlockPos pos : chunk.getBlockEntities().keySet()) {
                    if (Math.abs(pos.getX() - center.getX()) > 128 || Math.abs(pos.getZ() - center.getZ()) > 128 || !chunk.getBlockState(pos).is(EldritchContent.ELDRITCH_PORTAL.get())) {
                        continue;
                    }
                    double candidate = Vec3.atCenterOf(pos).distanceToSqr(origin);
                    if (candidate < distance) {
                        nearest = pos;
                        distance = candidate;
                    }
                }
            }
        }
        return nearest;
    }

    public static Vec3 arrival(ServerLevel level, BlockPos portal) {
        int dx = level.getRandom().nextBoolean() ? 1 : -1;
        int dz = level.getRandom().nextBoolean() ? 1 : -1;
        return new Vec3(portal.getX() + 0.5 + dx, portal.getY(), portal.getZ() + 0.5 + dz);
    }

    public static Vec3 safeFallback(ServerLevel level, Vec3 origin) {
        int x = BlockPos.containing(origin).getX();
        int z = BlockPos.containing(origin).getZ();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 4;
        return new Vec3(origin.x, y, origin.z);
    }
}
