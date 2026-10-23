package thaumcraft.entity.monster;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import org.jspecify.annotations.Nullable;
import thaumcraft.entity.monster.Cultist;
import thaumcraft.entity.monster.eldritch.EldritchCrab;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModSounds;

public class InhabitedZombie extends Zombie {
    public InhabitedZombie(EntityType<? extends InhabitedZombie> type, Level level) {
        super(type, level);
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Cultist.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        net.minecraft.world.entity.EntitySpawnReason spawnReason,
        @Nullable SpawnGroupData spawnGroupData
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnReason, spawnGroupData);
        float chance = level.getDifficulty() == Difficulty.HARD ? 0.9F : 0.6F;
        this.setItemSlot(EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(ModItems.CRIMSON_CULT_HELM.get()));
        if (this.random.nextFloat() <= chance) {
            this.setItemSlot(EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(ModItems.CRIMSON_CULT_CHESTPLATE.get()));
        }
        if (this.random.nextFloat() <= chance) {
            this.setItemSlot(EquipmentSlot.LEGS, new net.minecraft.world.item.ItemStack(ModItems.CRIMSON_CULT_GREAVES.get()));
        }
        this.setItemSlot(EquipmentSlot.FEET, net.minecraft.world.item.ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
        this.setBaby(false);
        this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0.0);
        return result;
    }

    @Override
    protected void tickDeath() {
        if (this.level() instanceof ServerLevel serverLevel && this.deathTime == 0) {
            EldritchCrab.spawnFromInhabitedZombie(serverLevel, this);
            this.dropExperience(serverLevel, this.getLastHurtByPlayer());
            serverLevel.sendParticles(
                ParticleTypes.EXPLOSION,
                this.getX(),
                this.getY() + this.getBbHeight() / 2.0,
                this.getZ(),
                20,
                this.getBbWidth(),
                this.getBbHeight() / 2.0,
                this.getBbWidth(),
                0.02
            );
            this.discard();
            return;
        }
        super.tickDeath();
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity entity, DamageSource source) {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CRAB_TALK.get();
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason spawnReason) {
        if (level instanceof ServerLevel serverLevel
            && !serverLevel.getEntitiesOfClass(InhabitedZombie.class, this.getBoundingBox().inflate(32.0, 16.0, 32.0)).isEmpty()) {
            return false;
        }
        return super.checkSpawnRules(level, spawnReason);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
    }
}
