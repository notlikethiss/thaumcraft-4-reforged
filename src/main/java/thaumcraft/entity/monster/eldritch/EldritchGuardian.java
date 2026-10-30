package thaumcraft.entity.monster.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import thaumcraft.entity.monster.Cultist;
import thaumcraft.entity.projectile.EldritchOrb;
import thaumcraft.registry.ModEntities;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModSounds;
import thaumcraft.research.Warp;
import thaumcraft.research.WarpType;
import thaumcraft.aspect.Aspect;
import thaumcraft.item.AspectTooltips;
import thaumcraft.platform.ModNetwork;
import thaumcraft.network.WarpEventPayload;

public class EldritchGuardian extends Monster implements RangedAttackMob, thaumcraft.entity.EldritchMob {
    public float armLiftLeft;
    public float armLiftRight;
    private boolean lastBlast;

    public EldritchGuardian(EntityType<? extends EldritchGuardian> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 50.0)
            .add(Attributes.MAX_ABSORPTION, 25.0)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.ARMOR, 4.0);
    }

    public static boolean spawnForWarp(ServerLevel level, ServerPlayer player, BlockPos pos) {
        EldritchGuardian guardian = ModEntities.ELDRITCH_GUARDIAN.get().create(level, EntitySpawnReason.TRIGGERED);
        if (guardian == null) {
            return false;
        }
        guardian.snapTo(pos.getX(), pos.getY(), pos.getZ(), level.getRandom().nextFloat() * 360.0F, 0.0F);
        guardian.setTarget(player);
        return level.noCollision(guardian) && !level.containsAnyLiquid(guardian.getBoundingBox()) && level.addFreshEntity(guardian);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LongRangeAttackGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Cultist.class, true));
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        return 20;
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty, EntitySpawnReason reason, net.minecraft.world.entity.SpawnGroupData data) {
        var result = super.finalizeSpawn(level, difficulty, reason, data);
        if (level.getLevel().dimension().equals(thaumcraft.world.dim.OuterLands.OUTER_LANDS)) this.setAbsorptionAmount(this.getAbsorptionAmount() + 25.0F);
        return result;
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, EntitySpawnReason reason) {
        return this.level().getEntitiesOfClass(EldritchGuardian.class, this.getBoundingBox().inflate(32.0, 16.0, 32.0), other -> other != this).isEmpty()
            && level instanceof net.minecraft.world.level.ServerLevelAccessor server
            && level.getDifficulty() != Difficulty.PEACEFUL
            && thaumcraft.entity.monster.EldritchSpawnRules.checkGuardian(ModEntities.ELDRITCH_GUARDIAN.get(), server, reason, this.blockPosition(), this.random);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 500;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.hasHome();
    }

    @Override
    protected boolean considersEntityAsAlly(Entity entity) {
        return entity instanceof thaumcraft.entity.EldritchMob || super.considersEntityAsAlly(entity);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return super.hurtServer(level, source, source.is(net.neoforged.neoforge.common.Tags.DamageTypes.IS_MAGIC) ? amount * 0.5F : amount);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && this.getMainHandItem().isEmpty() && this.isOnFire()) {
            int difficulty = level.getDifficulty().getId();
            if (difficulty > 0 && this.random.nextFloat() < difficulty * 0.3F) {
                target.igniteForSeconds(2.0F * difficulty);
            }
        }
        return hit;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        if (this.random.nextBoolean()) {
            var essence = new net.minecraft.world.item.ItemStack(ModItems.WISP_ESSENCE.get());
            AspectTooltips.setSingle(essence, Aspect.UNDEAD, 2);
            this.spawnAtLocation(level, essence);
        }
        if (this.random.nextBoolean()) {
            var essence = new net.minecraft.world.item.ItemStack(ModItems.WISP_ESSENCE.get());
            AspectTooltips.setSingle(essence, Aspect.ELDRITCH, 2);
            this.spawnAtLocation(level, essence);
        }
        int looting = source.getEntity() instanceof net.minecraft.world.entity.LivingEntity killer
            ? net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), killer) : 0;
        if (recentlyHit && this.random.nextInt(200) - looting < 5) this.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(thaumcraft.registry.EldritchContent.ELDRITCH_EYE.get()));
        super.dropCustomDeathLoot(level, source, recentlyHit);
    }

    @Override
    public void performRangedAttack(net.minecraft.world.entity.LivingEntity target, float distanceFactor) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.random.nextFloat() > 0.1F) {
            this.lastBlast = !this.lastBlast;
            EldritchOrb orb = EldritchOrb.create(level, this, target, this.lastBlast);
            this.level().broadcastEntityEvent(this, (byte) (this.lastBlast ? 16 : 15));
            level.addFreshEntity(orb);
            this.playSound(ModSounds.ELDRITCH_GUARDIAN_ATTACK.get(), 2.0F, 1.0F + this.random.nextFloat() * 0.1F);
            return;
        }
        if (!this.hasLineOfSight(target)) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 400), this);
        if (target instanceof ServerPlayer player) {
            Warp.add(player, 1 + this.random.nextInt(3), WarpType.TEMPORARY);
        }
        this.playSound(ModSounds.ELDRITCH_GUARDIAN_SCREECH.get(), 3.0F, 1.0F + this.random.nextFloat() * 0.1F);
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event == 15) {
            this.armLiftLeft = 0.5F;
        } else if (event == 16) {
            this.armLiftRight = 0.5F;
        } else if (event == 17) {
            this.armLiftLeft = 0.9F;
            this.armLiftRight = 0.9F;
        } else {
            super.handleEntityEvent(event);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.level().dimension().equals(thaumcraft.world.dim.OuterLands.OUTER_LANDS) && this.invulnerableTime <= 0 && this.tickCount % 25 == 0 && this.getAbsorptionAmount() < 25.0F) {
            this.setAbsorptionAmount(this.getAbsorptionAmount() + 1.0F);
        }
        if (this.level().isClientSide()) {
            this.armLiftLeft = Math.max(0.0F, this.armLiftLeft - 0.05F);
            this.armLiftRight = Math.max(0.0F, this.armLiftRight - 0.05F);
            this.level().addParticle(ParticleTypes.WITCH, this.getX(), this.getY() + this.getBbHeight() * 0.22, this.getZ(), 0.0, 0.0, 0.0);
        } else if (this.tickCount % 100 == 0 && this.level() instanceof ServerLevel level && this.level().getDifficulty() != Difficulty.EASY) {
            double radius = this.level().getDifficulty() == Difficulty.HARD ? 24.0 : 16.0;
            AABB area = this.getBoundingBox().inflate(radius);
            for (ServerPlayer player : level.players()) {
                if (player.isAlive() && area.contains(player.position())) {
                    ModNetwork.sendToPlayer(player, new WarpEventPayload(1));
                }
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.hasHome() && this.getHomeRadius() > 0) {
            output.putInt("HomeD", this.getHomeRadius());
            output.putInt("HomeX", this.getHomePosition().getX());
            output.putInt("HomeY", this.getHomePosition().getY());
            output.putInt("HomeZ", this.getHomePosition().getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getInt("HomeD").ifPresent(radius -> this.setHomeTo(
            new BlockPos(input.getIntOr("HomeX", 0), input.getIntOr("HomeY", 0), input.getIntOr("HomeZ", 0)),
            radius
        ));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ELDRITCH_GUARDIAN_IDLE.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.5F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.GORE.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ELDRITCH_GUARDIAN_DEATH.get();
    }

    private static final class LongRangeAttackGoal extends RangedAttackGoal {
        private final EldritchGuardian guardian;

        private LongRangeAttackGoal(EldritchGuardian guardian) {
            super(guardian, 1.0, 20, 40, 24.0F);
            this.guardian = guardian;
        }

        @Override
        public boolean canUse() {
            return super.canUse() && this.guardian.getTarget() != null && this.guardian.distanceToSqr(this.guardian.getTarget()) >= 64.0;
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.guardian.getTarget() != null && this.guardian.distanceToSqr(this.guardian.getTarget()) >= 64.0;
        }
    }
}
