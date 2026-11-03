package thaumcraft.entity.monster.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import thaumcraft.compat.ValueInput;
import thaumcraft.compat.ValueOutput;
import javax.annotation.Nullable;
import thaumcraft.entity.TaintedMob;
import thaumcraft.fx.Fx;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModEntities;
import thaumcraft.registry.ModSounds;

public class TaintSpore extends Monster implements TaintedMob {
    private static final EntityDataAccessor<Integer> DATA_SIZE = SynchedEntityData.defineId(TaintSpore.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ATTACHED = SynchedEntityData.defineId(TaintSpore.class, EntityDataSerializers.BOOLEAN);
    protected int growthTicks;
    public float displaySize;
    @Nullable
    protected BlockPos stalkPos;
    private boolean burst;

    public TaintSpore(EntityType<? extends TaintSpore> type, Level level) {
        super(type, level);
        this.setNoGravity(false);
        this.setSporeSize(2);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    public static @Nullable TaintSpore create(ServerLevel level, BlockPos stalkPos) {
        TaintSpore spore = ModEntities.TAINT_SPORE.get().create(level);
        if (spore == null) {
            return null;
        }
        spore.stalkPos = stalkPos.immutable();
        spore.entityData.set(DATA_ATTACHED, true);
        spore.setNoAi(true);
        spore.absMoveTo(stalkPos.getX() + 0.5, stalkPos.getY() + 1.0, stalkPos.getZ() + 0.5, 0.0F, 0.0F);
        return spore;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_SIZE, 2);
        entityData.define(DATA_ATTACHED, false);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return thaumcraft.registry.ModSounds.SWARM.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return thaumcraft.registry.ModSounds.GORE.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return thaumcraft.registry.ModSounds.GORE.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.1F;
    }

    public void setSporeSize(int size) {
        this.entityData.set(DATA_SIZE, size);
        this.refreshDimensions();
        this.xpReward = size;
    }

    public int getSporeSize() {
        return this.entityData.get(DATA_SIZE);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        if (this instanceof TaintSwarmer) {
            return EntityDimensions.scalable(1.0F, 1.0F);
        }
        float size = Math.max(0.15F * this.getSporeSize(), 0.5F);
        return EntityDimensions.scalable(size, size);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void travel(Vec3 input) {
        if (this.entityData.get(DATA_ATTACHED)) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.travel(new Vec3(0.0, Math.min(input.y, 0.0), 0.0));
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        super.move(type, new Vec3(0.0, Math.min(movement.y, 0.0), 0.0));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.displaySize < this.getSporeSize()) {
                this.displaySize += 0.02F;
            }
            if (!(this instanceof TaintSwarmer)) {
                Fx.get().taintSwarm(this.level(), this, this.getSporeSize() / 3, 0.1F, 10.0F, 0.0F);
            }
            return;
        }
        if (!(this instanceof TaintSwarmer) && this.tickCount % 20 == 0 && !thaumcraft.taint.TaintBiomes.isTainted(this.level(), this.blockPosition())) {
            this.hurt(this.damageSources().starve(), 1.0F);
        }
        if (this instanceof TaintSwarmer) {
            return;
        }
        if (this.getSporeSize() < 10 && this.growthTicks++ == 1200) {
            this.setSporeSize(this.getSporeSize() + 1);
            this.growthTicks = 0;
        }
        if (this.entityData.get(DATA_ATTACHED)) {
            if (this.stalkPos == null || !this.level().getBlockState(this.stalkPos).is(ModBlocks.MATURE_SPORE_STALK.get())) {
                this.burstIntoSpiders();
                return;
            }
            this.absMoveTo(this.stalkPos.getX() + 0.5, this.stalkPos.getY() + 1.0, this.stalkPos.getZ() + 0.5, this.getYRot(), this.getXRot());
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (this.shouldBurstOnDetachment() && (this.stalkPos == null || !this.level().getBlockState(this.stalkPos).is(ModBlocks.MATURE_SPORE_STALK.get()))) {
            this.burstIntoSpiders();
        }
    }

    protected boolean shouldBurstOnDetachment() {
        return this.stalkPos != null;
    }

    @Override
    public void playerTouch(Player player) {
        this.burstIntoSpiders();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.burstIntoSpiders();
    }

    protected void burstIntoSpiders() {
        if (this.burst || this.level().isClientSide()) {
            return;
        }
        this.burst = true;
        ServerLevel level = (ServerLevel) this.level();
        int size = this.getSporeSize();
        int count = size / 3 + level.getRandom().nextInt(size / 2 + 1);
        level.playSound(null, this.blockPosition(), ModSounds.GORE.get(), SoundSource.HOSTILE, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.1F);
        for (int index = 0; index < count; index++) {
            TaintSpider spider = ModEntities.TAINT_SPIDER.get().create(level);
            if (spider == null) {
                continue;
            }
            spider.absMoveTo(this.getX() + level.getRandom().nextFloat() - level.getRandom().nextFloat(), this.getY() + level.getRandom().nextFloat(),
                this.getZ() + level.getRandom().nextFloat() - level.getRandom().nextFloat(), level.getRandom().nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(spider);
        }
        if (this.stalkPos != null && level.getBlockState(this.stalkPos).is(ModBlocks.MATURE_SPORE_STALK.get())) {
            level.setBlockAndUpdate(this.stalkPos, ModBlocks.SPORE_STALK.get().defaultBlockState());
        }
        this.discard();
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        thaumcraft.compat.ValueOutput output = thaumcraft.compat.ValueOutput.of(tag, this.registryAccess());
        super.addAdditionalSaveData(tag);
        output.putInt("SporeSize", this.getSporeSize());
        output.putInt("GrowthTicks", this.growthTicks);
        output.putBoolean("Attached", this.entityData.get(DATA_ATTACHED));
        output.storeNullable("StalkPos", BlockPos.CODEC, this.stalkPos);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        thaumcraft.compat.ValueInput input = thaumcraft.compat.ValueInput.of(tag, this.registryAccess());
        super.readAdditionalSaveData(tag);
        this.setSporeSize(input.getIntOr("SporeSize", 2));
        this.growthTicks = input.getIntOr("GrowthTicks", 0);
        this.entityData.set(DATA_ATTACHED, input.getBooleanOr("Attached", false));
        this.stalkPos = input.read("StalkPos", BlockPos.CODEC).orElse(null);
    }
}
