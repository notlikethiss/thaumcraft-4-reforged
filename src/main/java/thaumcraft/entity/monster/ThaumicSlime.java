package thaumcraft.entity.monster;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import thaumcraft.compat.ValueInput;
import thaumcraft.compat.ValueOutput;
import net.minecraft.world.phys.Vec3;
import thaumcraft.entity.TaintedMob;
import thaumcraft.registry.ModEntities;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModSounds;

public class ThaumicSlime extends Monster implements Enemy, TaintedMob {
    private static final EntityDataAccessor<Integer> DATA_SIZE = SynchedEntityData.defineId(ThaumicSlime.class, EntityDataSerializers.INT);
    private static final TargetingConditions PLAYER_TARGETING = TargetingConditions.forCombat().range(16.0);
    private int jumpDelay;
    private int launched = 10;
    private int spitCounter = 100;
    private boolean splitOnDeath;
    public float squishAmount;
    public float squishFactor;
    public float prevSquishFactor;

    public ThaumicSlime(EntityType<? extends ThaumicSlime> type, Level level) {
        super(type, level);
        this.jumpDelay = this.random.nextInt(20) + 10;
        this.setSlimeSize(1 << this.random.nextInt(3));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_SIZE, 1);
    }

    public int getSlimeSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public void setSlimeSize(int size) {
        int bounded = Math.max(1, Math.min(100, size));
        this.entityData.set(DATA_SIZE, bounded);
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(bounded);
        this.setHealth(bounded);
        this.xpReward = (int) Math.sqrt(bounded);
        this.refreshDimensions();
    }

    @Override
    public EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        float diameter = 0.25F * (float) Math.sqrt(this.getSlimeSize()) + 0.25F;
        return EntityDimensions.scalable(diameter, diameter);
    }

    @Override
    protected void customServerAiStep() {
        if (!(this.level() instanceof ServerLevel level)) return;
        super.customServerAiStep();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
            return;
        }
        Player player = level.getNearestPlayer(PLAYER_TARGETING, this);
        ThaumicSlime closestSlime = null;
        if (player != null) {
            this.spitCounter = Math.max(0, this.spitCounter - 1);
            if (this.onGround()) {
                this.lookAt(player, 10.0F, 20.0F);
            }
            if (this.distanceTo(player) > 4.0F && this.spitCounter == 0 && this.getSlimeSize() > 3) {
                this.spitAt(player);
            }
        } else {
            closestSlime = this.getClosestMergableSlime(level);
            if (closestSlime != null) {
                if (this.onGround()) {
                    this.lookAt(closestSlime, 10.0F, 20.0F);
                }
                if (this.distanceTo(closestSlime) < this.getBbWidth() + closestSlime.getBbWidth()) {
                    closestSlime.setSlimeSize(Math.min(100, closestSlime.getSlimeSize() + this.getSlimeSize()));
                    this.discard();
                    return;
                }
            }
        }
        if (this.onGround()) {
            if (this.jumpDelay-- <= 0) {
                this.jumpDelay = this.random.nextInt(16) + 8;
                if (player != null) {
                    this.jumpDelay /= 3;
                }
                this.jumpFromGround();
                double speed = Math.sqrt(this.getSlimeSize()) * 0.1;
                if (player != null || closestSlime != null) {
                    LivingEntity target = player != null ? player : closestSlime;
                    Vec3 direction = target.position().subtract(this.position()).normalize();
                    this.setDeltaMovement(this.getDeltaMovement().add(direction.x * speed, 0.0, direction.z * speed));
                } else {
                    double angle = this.random.nextDouble() * Math.PI * 2.0;
                    this.setDeltaMovement(this.getDeltaMovement().add(Math.cos(angle) * speed, 0.0, Math.sin(angle) * speed));
                }
                this.faceMovement();
                if (this.getSlimeSize() > 3) {
                    this.playSound(SoundEvents.SLIME_JUMP, this.getSoundVolume(), 1.0F);
                }
            }
        }
        this.attackNearbyPlayers(level);
    }

    private void faceMovement() {
        Vec3 movement = this.getDeltaMovement();
        if (movement.horizontalDistanceSqr() > 1.0E-7) {
            this.setYRot((float) (Mth.atan2(movement.z, movement.x) * Mth.RAD_TO_DEG) - 90.0F);
            this.setYBodyRot(this.getYRot());
            this.setYHeadRot(this.getYRot());
        }
    }

    @Override
    protected float tickHeadTurn(float yBodyRot, float animStep) {
        this.setYBodyRot(this.getYRot());
        this.setYHeadRot(this.getYRot());
        return animStep;
    }

    private void spitAt(Player target) {
        this.spitCounter = 101;
        ThaumicSlime spit = ModEntities.THAUMIC_SLIME.get().create(this.level());
        if (spit != null && this.level() instanceof ServerLevel level) {
            spit.setSlimeSize(1);
            double y = this.getBoundingBox().getCenter().y;
            double dx = target.getX() - this.getX();
            double dy = target.getBoundingBox().minY + target.getBbHeight() / 3.0 - y;
            double dz = target.getZ() - this.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            if (horizontal >= 1.0E-7) {
                double nx = dx / horizontal;
                double nz = dz / horizontal;
                spit.setPos(this.getX() + nx, y, this.getZ() + nz);
                double speed = horizontal * 0.2;
                spit.setPos(this.getX() + nx, y, this.getZ() + nz);
                Vec3 direction = new Vec3(dx, dy + speed, dz).normalize();
                direction = direction.add(
                    this.random.nextGaussian() * 0.0075,
                    this.random.nextGaussian() * 0.0075,
                    this.random.nextGaussian() * 0.0075
                ).scale(1.5);
                spit.setDeltaMovement(direction);
                spit.faceMovement();
                if (level.addFreshEntity(spit)) {
                    this.setSlimeSize(this.getSlimeSize() - 1);
                    this.playSound(ModSounds.GORE.get(), 1.0F, 0.8F);
                }
            }
        }
    }

    private @Nullable ThaumicSlime getClosestMergableSlime(ServerLevel level) {
        List<ThaumicSlime> slimes = level.getEntitiesOfClass(
            ThaumicSlime.class,
            this.getBoundingBox().inflate(16.0, 8.0, 16.0),
            slime -> slime != this && slime.tickCount > 100 && slime.getSlimeSize() < 100
        );
        ThaumicSlime closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (ThaumicSlime slime : slimes) {
            double distance = this.distanceToSqr(slime);
            if (distance < closestDistance) {
                closest = slime;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private void attackNearbyPlayers(ServerLevel level) {
        int size = (int) Math.max(1.0, Math.sqrt(this.getSlimeSize()));
        if (this.launched > 0 && size == 2) {
            size = 3;
        }
        double reach = 0.8 * size;
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(reach))) {
            if (this.hasLineOfSight(player) && this.distanceToSqr(player) < reach * reach) {
                player.hurt(this.damageSources().mobAttack(this), this.getSlimeSize());
            }
        }
    }

    @Override
    public void tick() {
        boolean wasOnGround = this.onGround();
        this.squishFactor += (this.squishAmount - this.squishFactor) * 0.5F;
        this.prevSquishFactor = this.squishFactor;
        super.tick();
        int size = (int) Math.sqrt(this.getSlimeSize());
        if (this.launched > 0) {
            this.launched--;
            if (this.level().isClientSide()) {
                for (int index = 0; index < size * (this.launched + 1); index++) {
                    this.emitSlimeParticles(size);
                }
            }
        }
        if (this.onGround() && !wasOnGround) {
            if (this.level().isClientSide()) {
                for (int index = 0; index < size * 8; index++) {
                    this.emitSlimeParticles(size);
                }
            }
            this.playSound(SoundEvents.SLIME_SQUISH, this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) / 0.8F);
            this.squishAmount = -0.5F;
        } else if (!this.onGround() && wasOnGround) {
            this.squishAmount = 1.0F;
        }
        this.squishAmount *= 0.6F;
    }

    private void emitSlimeParticles(int size) {
        double x = this.getX() + (this.random.nextDouble() * 2.0 - 1.0) * this.getBbWidth() * 0.5;
        double y = this.getY();
        double z = this.getZ() + (this.random.nextDouble() * 2.0 - 1.0) * this.getBbWidth() * 0.5;
        this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SLIME_BALL)), x, y, z,
            (this.random.nextDouble() * 2.0 - 1.0) * 0.08 * size, this.random.nextDouble() * 0.08 * size,
            (this.random.nextDouble() * 2.0 - 1.0) * 0.08 * size);
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.splitOnDeath && this.getHealth() <= 0.0F && this.level() instanceof ServerLevel level) {
            this.splitOnDeath = true;
            int count = (int) Math.sqrt(this.getSlimeSize());
            if (count > 1) {
                for (int index = 0; index < count; index++) {
                    ThaumicSlime child = ModEntities.THAUMIC_SLIME.get().create(level);
                    if (child != null) {
                        float offsetX = (index % 2 - 0.5F) * count / 4.0F;
                        float offsetZ = (index / 2 - 0.5F) * count / 4.0F;
                        child.setSlimeSize(1);
                        child.setPos(this.getX() + offsetX, this.getY() + 0.5, this.getZ() + offsetZ);
                        child.setYRot(this.random.nextFloat() * 360.0F);
                        level.addFreshEntity(child);
                    }
                }
            }
        }
        super.die(damageSource);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        if (this.getSlimeSize() < 3 && this.random.nextInt(3) == 0) {
            this.spawnAtLocation(new net.minecraft.world.item.ItemStack(ModItems.TAINTED_GOO.get()));
        }
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.1F * (float) Math.sqrt(this.getSlimeSize());
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        thaumcraft.compat.ValueOutput output = thaumcraft.compat.ValueOutput.of(tag, this.registryAccess());
        super.addAdditionalSaveData(tag);
        output.putInt("Size", this.getSlimeSize() - 1);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        thaumcraft.compat.ValueInput input = thaumcraft.compat.ValueInput.of(tag, this.registryAccess());
        super.readAdditionalSaveData(tag);
        this.setSlimeSize(input.getIntOr("Size", 0) + 1);
    }
}
