package thaumcraft.entity.monster.taint;

import net.minecraft.world.entity.EntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.level.Level;
import java.util.List;

public class TaintedVillager extends TaintedAnimalMonster {
    private int villageSearchTicks;

    public TaintedVillager(EntityType<? extends TaintedVillager> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TaintedAnimalMonster.createAttributes(30.0, 0.3, 4.0, 0.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.7F;
    }

    @Override
    protected boolean targetsVillagers() {
        return false;
    }

    @Override
    protected boolean targetsAnimals() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected int wanderGoalPriority() {
        return 9;
    }

    @Override
    protected int watchGoalPriority() {
        return 10;
    }

    @Override
    protected int idleGoalPriority() {
        return 10;
    }

    @Override
    protected float watchRange() {
        return 8.0F;
    }

    @Override
    protected void registerGoals() {
        this.getNavigation().setCanOpenDoors(true);
        super.registerGoals();
        this.goalSelector.addGoal(2, new TaintedVillagerIndoorGoal(this));
        this.goalSelector.addGoal(3, new TaintedVillagerRestrictDoorGoal(this));
        this.goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(4, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0, false, 2, () -> true));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (--this.villageSearchTicks > 0) {
            return;
        }
        this.villageSearchTicks = 70 + this.random.nextInt(50);
        List<BlockPos> villagePoints = level.getPoiManager()
            .findAll(type -> type.is(PoiTypeTags.VILLAGE), pos -> true, this.blockPosition(), 32, PoiManager.Occupancy.ANY)
            .toList();
        if (villagePoints.isEmpty()) {
            this.clearHome();
        } else {
            double centerX = villagePoints.stream().mapToInt(BlockPos::getX).average().orElse(this.getBlockX());
            double centerY = villagePoints.stream().mapToInt(BlockPos::getY).average().orElse(this.getBlockY());
            double centerZ = villagePoints.stream().mapToInt(BlockPos::getZ).average().orElse(this.getBlockZ());
            BlockPos villageCenter = BlockPos.containing(centerX, centerY, centerZ);
            int radius = (int) (Math.sqrt(villagePoints.stream().mapToDouble(villageCenter::distSqr).max().orElse(0.0)) * 0.6);
            this.setHomeTo(villageCenter, radius);
        }
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.dropTaintResource(level, 2, 0.5F);
        int looting = source.getEntity() instanceof net.minecraft.world.entity.LivingEntity killer
            ? killer.getMainHandItem().getEnchantmentLevel(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING))
            : 0;
        if (level.getRandom().nextInt(13) < 1 + looting) {
            this.spawnAtLocation(level, thaumcraft.registry.ModItems.GOLD_COIN.get());
        }
    }
}
