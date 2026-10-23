package thaumcraft.entity.projectile;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.entity.CombatUtils;
import thaumcraft.fx.Fx;
import thaumcraft.registry.ModEntities;

public class FrostShard extends ThrowableProjectile {
    private static final EntityDataAccessor<Float> DATA_DAMAGE = SynchedEntityData.defineId(FrostShard.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_FROSTY = SynchedEntityData.defineId(FrostShard.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_FRAGILE = SynchedEntityData.defineId(FrostShard.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_BOUNCE = SynchedEntityData.defineId(FrostShard.class, EntityDataSerializers.FLOAT);

    public int bounceLimit = 3;

    public FrostShard(EntityType<? extends FrostShard> type, Level level) {
        super(type, level);
    }

    public static FrostShard create(Level level, LivingEntity shooter, float scatter) {
        FrostShard shard = new FrostShard(ModEntities.FROST_SHARD.get(), level);
        CombatUtils.launch(shard, shooter, 1.0F, scatter);
        return shard;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_DAMAGE, 0.0F);
        builder.define(DATA_FROSTY, (byte) 0);
        builder.define(DATA_FRAGILE, false);
        builder.define(DATA_BOUNCE, 0.5F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_DAMAGE.equals(accessor)) {
            this.refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float size = 0.15F + this.entityData.get(DATA_DAMAGE) * 0.15F;
        return EntityDimensions.fixed(size, size);
    }

    public void setDamage(float damage) {
        this.entityData.set(DATA_DAMAGE, damage);
        this.refreshDimensions();
    }

    public float getDamage() {
        return this.entityData.get(DATA_DAMAGE);
    }

    public void setFrosty(int frosty) {
        this.entityData.set(DATA_FROSTY, (byte) frosty);
    }

    public int getFrosty() {
        return this.entityData.get(DATA_FROSTY);
    }

    public void setFragile(boolean fragile) {
        this.entityData.set(DATA_FRAGILE, fragile);
    }

    public boolean isFragile() {
        return this.entityData.get(DATA_FRAGILE);
    }

    public void setBounce(double bounce) {
        this.entityData.set(DATA_BOUNCE, (float) bounce);
    }

    public double getBounce() {
        return this.entityData.get(DATA_BOUNCE);
    }

    @Override
    protected double getDefaultGravity() {
        return this.isFragile() ? 0.015 : 0.05;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.getFrosty() > 0) {
            float size = this.getDamage() / 10.0F;
            for (int count = 0; count < this.getFrosty(); count++) {
                Fx.get().sparkle(
                    this.level(),
                    (float) this.getX() - size + this.random.nextFloat() * (size * 2.0F),
                    (float) this.getY() - size + this.random.nextFloat() * (size * 2.0F),
                    (float) this.getZ() - size + this.random.nextFloat() * (size * 2.0F),
                    0.4F,
                    6,
                    0.005F
                );
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity target = hit.getEntity();
        int offsetX = Mth.floor(this.getX()) - Mth.floor(target.getX());
        int offsetY = Mth.floor(this.getY()) - Mth.floor(target.getY());
        int offsetZ = Mth.floor(this.getZ()) - Mth.floor(target.getZ());
        Vec3 movement = this.getDeltaMovement();
        double motionX = movement.x;
        double motionY = movement.y;
        double motionZ = movement.z;
        if (offsetZ != 0) {
            motionZ *= -1.0;
        }
        if (offsetX != 0) {
            motionX *= -1.0;
        }
        if (offsetY != 0) {
            motionY *= -0.9;
        }
        this.setDeltaMovement(motionX * 0.66, motionY * 0.66, motionZ * 0.66);
        this.burst(Blocks.ICE.defaultBlockState(), (int) Math.ceil(this.getDamage()));
        this.rebound();
        if (this.level() instanceof ServerLevel level) {
            Vec3 targetMovement = target.getDeltaMovement();
            target.hurtServer(level, level.damageSources().thrown(this, this.getOwner()), this.getDamage());
            if (target instanceof LivingEntity living && this.getFrosty() > 0) {
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, this.getFrosty() - 1), this);
            }
            if (this.isFragile()) {
                target.invulnerableTime = 0;
                this.discard();
                this.playBreakSound(Blocks.ICE.defaultBlockState());
                Vec3 changed = target.getDeltaMovement();
                target.setDeltaMovement(targetMovement.add(changed.subtract(targetMovement).scale(0.1)));
            }
        }
        this.finishBounce();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        BlockState state = this.level().getBlockState(hit.getBlockPos());
        Direction direction = hit.getDirection();
        Vec3 movement = this.getDeltaMovement();
        double motionX = movement.x;
        double motionY = movement.y;
        double motionZ = movement.z;
        if (direction.getStepZ() != 0) {
            motionZ *= -1.0;
        }
        if (direction.getStepX() != 0) {
            motionX *= -1.0;
        }
        if (direction.getStepY() != 0) {
            motionY *= -0.9;
        }
        this.setDeltaMovement(motionX, motionY, motionZ);
        this.playBreakSound(state);
        this.burst(state, (int) Math.ceil(this.getDamage()));
        this.rebound();
        this.finishBounce();
    }

    private void rebound() {
        Vec3 movement = this.getDeltaMovement().scale(this.getBounce());
        this.setDeltaMovement(movement);
        double length = movement.length();
        if (length > 1.0E-7) {
            this.setPos(this.position().subtract(movement.scale(0.05F / length)));
        }
        this.markHurt();
    }

    private void finishBounce() {
        if (this.bounceLimit-- <= 0) {
            this.discard();
            this.playBreakSound(Blocks.ICE.defaultBlockState());
            this.burst(Blocks.ICE.defaultBlockState(), (int) Math.ceil(8.0F * this.getDamage()));
        }
    }

    private void playBreakSound(BlockState state) {
        if (!this.level().isClientSide()) {
            SoundType soundType = state.getSoundType();
            this.playSound(soundType.getBreakSound(), 0.3F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
        }
    }

    private void burst(BlockState state, int count) {
        if (this.level() instanceof ServerLevel level && count > 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), this.getX(), this.getY(), this.getZ(), count, 0.2, 0.1, 0.2, 0.15);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("damage", this.getDamage());
        output.putBoolean("fragile", this.isFragile());
        output.putInt("frost", this.getFrosty());
        output.putFloat("bounce", (float) this.getBounce());
        output.putInt("bounceLimit", this.bounceLimit);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setDamage(input.getFloatOr("damage", 0.0F));
        this.setFragile(input.getBooleanOr("fragile", false));
        this.setFrosty(input.getIntOr("frost", 0));
        this.setBounce(input.getFloatOr("bounce", 0.5F));
        this.bounceLimit = input.getIntOr("bounceLimit", 3);
    }
}
