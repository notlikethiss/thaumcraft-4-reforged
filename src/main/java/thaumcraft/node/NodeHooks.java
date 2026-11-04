package thaumcraft.node;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import thaumcraft.aspect.Aspect;
import thaumcraft.entity.AspectOrb;
import thaumcraft.entity.monster.GiantBrainyZombie;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModEntities;
import thaumcraft.taint.TaintSpread;

public final class NodeHooks {
    private NodeHooks() {
    }

    public static void spreadTaintFibres(ServerLevel level, BlockPos pos) {
        TaintSpread.spreadFibres(level, pos);
    }

    public static boolean spawnAspectOrb(ServerLevel level, BlockPos pos, Aspect aspect, int amount) {
        return AspectOrb.spawn(level, pos, aspect, amount);
    }

    public static boolean spawnGiantBrainyZombie(ServerLevel level, BlockPos node, BlockPos pos) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        AABB area = new AABB(node).inflate(10.0, 6.0, 10.0);
        if (level.getEntitiesOfClass(GiantBrainyZombie.class, area).size() > 3) {
            return false;
        }
        GiantBrainyZombie zombie = ModEntities.GIANT_BRAINY_ZOMBIE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (zombie == null) {
            return false;
        }
        zombie.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        zombie.setYRot(level.getRandom().nextFloat() * 360.0F);
        if (!zombie.checkSpawnRules(level, EntitySpawnReason.TRIGGERED) || !level.noCollision(zombie) || !level.addFreshEntity(zombie)) return false;
        level.levelEvent(2004, node, 0);
        zombie.spawnAnim();
        return true;
    }

    public static void spillFlux(ServerLevel level, BlockPos pos, boolean gas) {
        if (!level.hasChunkAt(pos) || !level.isEmptyBlock(pos)) {
            return;
        }
        level.setBlockAndUpdate(pos, (gas ? ModBlocks.FLUX_GAS.get() : ModBlocks.FLUX_GOO.get()).withLevel(8));
    }
}
