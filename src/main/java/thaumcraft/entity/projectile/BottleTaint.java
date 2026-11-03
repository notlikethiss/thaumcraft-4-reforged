package thaumcraft.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import thaumcraft.registry.ModItems;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import thaumcraft.effect.EffectHooks;
import thaumcraft.entity.TaintedMob;
import thaumcraft.registry.ModBlocks;
import thaumcraft.taint.TaintBiomes;

public class BottleTaint extends ThrowableItemProjectile {
    public BottleTaint(EntityType<? extends BottleTaint> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BOTTLED_TAINT.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
    }

    @Override
    protected void onHit(HitResult hit) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }
        AABB area = getBoundingBox().inflate(5.0);
        for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!(living instanceof TaintedMob) && !living.getType().getTags().anyMatch(net.minecraft.tags.EntityTypeTags.UNDEAD::equals)) {
                EffectHooks.applyTaintPoison(living, 100, 0);
            }
        }
        BlockPos origin = blockPosition();
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = origin.getX() + (int) ((random.nextFloat() - random.nextFloat()) * 5.0F);
            int z = origin.getZ() + (int) ((random.nextFloat() - random.nextFloat()) * 5.0F);
            BlockPos target = new BlockPos(x, origin.getY(), z);
            if (!random.nextBoolean() || TaintBiomes.isTainted(serverLevel, target)) {
                continue;
            }
            TaintBiomes.set(serverLevel, target, TaintBiomes.TAINTED_LAND);
            BlockPos ground = target.below();
            if (serverLevel.getBlockState(ground).isFaceSturdy(serverLevel, ground, net.minecraft.core.Direction.UP)
                && serverLevel.getBlockState(target).canBeReplaced()) {
                serverLevel.setBlockAndUpdate(target, ModBlocks.FIBROUS_TAINT.get().defaultBlockState());
            }
        }
        discard();
    }
}
