package thaumcraft.entity.monster.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import thaumcraft.entity.monster.InhabitedZombie;
import thaumcraft.entity.monster.Cultist;
import thaumcraft.registry.ModEntities;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModSounds;
import thaumcraft.platform.EntityMountPlatform;

public class EldritchCrab extends Monster {
    private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(EldritchCrab.class, EntityDataSerializers.BYTE);
    private int mountedAttackCooldown;

    public EldritchCrab(EntityType<? extends EldritchCrab> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ARMOR, 0.0);
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        return 6;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.63F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Cultist.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLAGS, (byte) 0);
    }

    public boolean hasHelm() {
        return (this.entityData.get(DATA_FLAGS) & 1) != 0;
    }

    public void setHelm(boolean hasHelm) {
        byte flags = this.entityData.get(DATA_FLAGS);
        this.entityData.set(DATA_FLAGS, (byte) (hasHelm ? flags | 1 : flags & ~1));
        this.updateArmorAndSpeed();
    }

    private void updateArmorAndSpeed() {
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(this.hasHelm() ? 0.275 : 0.3);
        this.getAttribute(Attributes.ARMOR).setBaseValue(this.hasHelm() ? 5.0 : 0.0);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason spawnReason,
        @Nullable SpawnGroupData spawnGroupData
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnReason, spawnGroupData);
        Spider.SpiderEffectsGroupData spiderData;
        if (result instanceof Spider.SpiderEffectsGroupData data) {
            spiderData = data;
        } else {
            spiderData = new Spider.SpiderEffectsGroupData();
            if (level.getLevel().getDifficulty() == net.minecraft.world.Difficulty.HARD && this.random.nextFloat() < 0.1F * difficulty.getSpecialMultiplier()) {
                spiderData.setRandomEffect(this.random);
            }
            result = spiderData;
        }
        if (spiderData.effect != null) {
            this.addEffect(new MobEffectInstance(spiderData.effect, -1));
        }
        this.setHelm(level.getLevel().getDifficulty() == net.minecraft.world.Difficulty.HARD || this.random.nextFloat() < 0.33F);
        return result;
    }

    public static @Nullable EldritchCrab spawnFromInhabitedZombie(ServerLevel level, InhabitedZombie zombie) {
        EldritchCrab crab = ModEntities.ELDRITCH_CRAB.get().create(level, EntitySpawnReason.CONVERSION);
        if (crab == null) {
            return null;
        }
        crab.snapTo(zombie.getX(), zombie.getY() + zombie.getEyeHeight(), zombie.getZ(), zombie.getYRot(), zombie.getXRot());
        crab.setHelm(true);
        return level.addFreshEntity(crab) ? crab : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount < 20) {
            this.fallDistance = 0.0F;
        }
        LivingEntity target = this.getLastHurtByMob();
        if (this.getVehicle() == null && target != null && !target.isVehicle() && !this.onGround() && !this.hasHelm()
            && target.isAlive() && this.getY() - target.getY() >= target.getBbHeight() / 2.0 && this.distanceToSqr(target) < 4.0) {
            EntityMountPlatform.startRiding(this, target, true, true);
        }
        if (this.level() instanceof ServerLevel level && this.getVehicle() instanceof LivingEntity vehicle) {
            if (this.mountedAttackCooldown > 0) {
                this.mountedAttackCooldown--;
            }
            if (this.mountedAttackCooldown <= 0) {
                this.mountedAttackCooldown = 10 + this.random.nextInt(10);
                this.doHurtTarget(level, vehicle);
                if (this.getVehicle() == vehicle && this.random.nextFloat() < 0.2F) {
                    this.stopRiding();
                }
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (this.hasHelm() && this.getHealth() / this.getMaxHealth() <= 0.5F) {
            this.setHelm(false);
            this.spawnItemParticles(new ItemStack(ModItems.CRIMSON_CULT_CHESTPLATE.get()), 20);
        }
        return hurt;
    }

    @Override
    public net.minecraft.world.phys.Vec3 getVehicleAttachmentPoint(Entity vehicle) {
        net.minecraft.world.phys.Vec3 point = super.getVehicleAttachmentPoint(vehicle);
        return this.getVehicle() == null ? point : point.add(0.0, 0.5, 0.0);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.0F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CRAB_TALK.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HOSTILE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CRAB_DEATH.get();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            this.playSound(ModSounds.CRAB_CLAW.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
        }
        return hit;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        int looting = source.getEntity() instanceof LivingEntity killer
            ? EnchantmentHelper.getEnchantmentLevel(
                level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),
                killer
            )
            : 0;
        if (recentlyHit && (this.random.nextInt(3) == 0 || this.random.nextInt(1 + looting) > 0)) {
            this.spawnAtLocation(level, new ItemStack(Items.ENDER_PEARL));
        }
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("Flags", this.entityData.get(DATA_FLAGS));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_FLAGS, input.getByteOr("Flags", (byte) 0));
        this.updateArmorAndSpeed();
    }
}
