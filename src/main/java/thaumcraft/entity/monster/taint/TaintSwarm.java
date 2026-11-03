package thaumcraft.entity.monster.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.compat.ValueInput;
import thaumcraft.compat.ValueOutput;
import javax.annotation.Nullable;
import thaumcraft.entity.TaintedMob;
import thaumcraft.damage.ModDamageTypes;
import thaumcraft.fx.Fx;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModSounds;
import thaumcraft.taint.TaintBiomes;

public class TaintSwarm extends Monster implements TaintedMob {
    private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(TaintSwarm.class, EntityDataSerializers.BYTE);
    private static final byte SUMMONED = 2;
    @Nullable
    private BlockPos flightTarget;
    @Nullable
    private LivingEntity target;
    private int damageBonus;
    private int attackTime;
    private int noTargetTicks;

    public TaintSwarm(EntityType<? extends TaintSwarm> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_FLAGS, (byte) 0);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SWARM_ATTACK.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SWARM_ATTACK.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.1F;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.scalable(2.0F, 2.0F);
    }

    public boolean isSummoned() {
        return (this.entityData.get(DATA_FLAGS) & SUMMONED) != 0;
    }

    public void setSummoned(boolean summoned) {
        byte flags = this.entityData.get(DATA_FLAGS);
        this.entityData.set(DATA_FLAGS, (byte) (summoned ? flags | SUMMONED : flags & ~SUMMONED));
    }

    public void setDamageBonus(int damageBonus) {
        this.damageBonus = damageBonus;
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2.0 + damageBonus);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide()) {
            this.tickAttack();
            this.tickTarget();
            this.tickFlight();
        }
        super.tick();
        if (this.level().isClientSide()) {
            Fx.get().taintSwarm(this.level(), this, 25, 0.22F, 15.0F, 0.08F);
        }
        this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.6, 1.0));
    }

    private void tickAttack() {
        if (this.attackTime > 0) {
            this.attackTime--;
        }
        if (this.target == null || !this.target.isAlive() || this.target instanceof TaintedMob) {
            return;
        }
        if (this.attackTime <= 0 && this.distanceTo(this.target) < 3.0 && verticalOverlap(this, this.target)) {
            this.attackTime = 10 + this.random.nextInt(5);
            Vec3 movement = this.target.getDeltaMovement();
            if (this.target.hurt(this.level().damageSources().source(ModDamageTypes.SWARM, this),
                (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                this.target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            }
            this.target.setDeltaMovement(movement);
            this.level().playSound(null, this.blockPosition(), ModSounds.SWARM_ATTACK.get(), SoundSource.HOSTILE, 0.3F,
                0.9F + this.random.nextFloat() * 0.2F);
        }
    }

    private void tickTarget() {
        if (this.target != null && (!this.target.isAlive() || this.target instanceof Player player && player.getAbilities().invulnerable)) {
            this.target = null;
        }
        if (this.target == null && !this.isSummoned()) {
            this.target = this.level().getEntitiesOfClass(Player.class, new AABB(this.blockPosition()).inflate(12.0),
                player -> player.isAlive() && !player.getAbilities().invulnerable && !player.isCreative() && !player.isSpectator()
                    && this.distanceToSqr(player) < 144.0)
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        }
        if (this.target == null && this.isSummoned()) {
            this.noTargetTicks++;
            this.hurt(this.damageSources().generic(), 5.0F);
        } else if (this.target != null) {
            this.noTargetTicks = 0;
        }
    }

    private void tickFlight() {
        if (this.target != null) {
            Vec3 delta = new Vec3(this.target.getX() - this.getX(), this.target.getY() + this.target.getEyeHeight() - this.getY(), this.target.getZ() - this.getZ());
            this.adjustMotion(delta, 0.025);
            return;
        }
        if (this.flightTarget != null && (!this.level().isEmptyBlock(this.flightTarget) || this.flightTarget.getY() < 1
            || this.flightTarget.getY() > this.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, this.flightTarget.getX(), this.flightTarget.getZ()) + 8
            || !TaintBiomes.isTainted(this.level(), this.flightTarget))) {
            this.flightTarget = null;
        }
        if (this.flightTarget == null || this.random.nextInt(30) == 0 || this.flightTarget.distToCenterSqr(this.position()) < 4.0) {
            this.flightTarget = this.blockPosition().offset(this.random.nextInt(7) - this.random.nextInt(7), this.random.nextInt(6) - 2,
                this.random.nextInt(7) - this.random.nextInt(7));
        }
        Vec3 delta = new Vec3(this.flightTarget.getX() + 0.5 - this.getX(), this.flightTarget.getY() + 0.1 - this.getY(), this.flightTarget.getZ() + 0.5 - this.getZ());
        this.adjustMotion(delta, 0.015);
    }

    private void adjustMotion(Vec3 delta, double horizontalFactor) {
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(
            motion.x + (Math.signum(delta.x) * 0.5 - motion.x) * horizontalFactor,
            motion.y + (Math.signum(delta.y) * 0.7 - motion.y) * 0.1,
            motion.z + (Math.signum(delta.z) * 0.5 - motion.z) * horizontalFactor
        );
        float yaw = (float) (Math.atan2(this.getDeltaMovement().z, this.getDeltaMovement().x) * 180.0 / Math.PI) - 90.0F;
        this.setYRot(this.getYRot() + Mth.wrapDegrees(yaw - this.getYRot()));
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (level.getRandom().nextBoolean()) {
            this.spawnAtLocation(ModItems.TAINTED_GOO.get());
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        thaumcraft.compat.ValueOutput output = thaumcraft.compat.ValueOutput.of(tag, this.registryAccess());
        super.addAdditionalSaveData(tag);
        output.putByte("Flags", this.entityData.get(DATA_FLAGS));
        output.putInt("DamageBonus", this.damageBonus);
        output.storeNullable("FlightTarget", BlockPos.CODEC, this.flightTarget);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        thaumcraft.compat.ValueInput input = thaumcraft.compat.ValueInput.of(tag, this.registryAccess());
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_FLAGS, input.getByteOr("Flags", (byte) 0));
        this.setDamageBonus(input.getIntOr("DamageBonus", 0));
        this.flightTarget = input.read("FlightTarget", BlockPos.CODEC).orElse(null);
    }

    private static boolean verticalOverlap(net.minecraft.world.entity.Entity left, net.minecraft.world.entity.Entity right) {
        return right.getBoundingBox().maxY > left.getBoundingBox().minY && right.getBoundingBox().minY < left.getBoundingBox().maxY;
    }
}
