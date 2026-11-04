package thaumcraft.entity.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;
import thaumcraft.fx.Fx;
import thaumcraft.network.NodeZapPayload;
import thaumcraft.platform.ModNetwork;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModSounds;

public class Wisp extends Mob implements Enemy {
    private static final EntityDataAccessor<String> DATA_TYPE = SynchedEntityData.defineId(Wisp.class, EntityDataSerializers.STRING);
    private static final TargetingConditions AGGRO_TARGETING = TargetingConditions.forCombat().range(16.0).ignoreLineOfSight();
    private static final net.minecraft.resources.ResourceKey<Biome> EERIE = net.minecraft.resources.ResourceKey.create(Registries.BIOME, thaumcraft.Thaumcraft.id("eerie"));
    private static final double ATTACK_RANGE = 16.0;

    private int courseChangeCooldown;
    private double waypointX;
    private double waypointY;
    private double waypointZ;
    private @Nullable LivingEntity targetedEntity;
    private int aggroCooldown;
    private int attackCounter;

    public Wisp(EntityType<? extends Wisp> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.FOLLOW_RANGE, 16.0).add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.scalable(0.9F, 0.9F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_TYPE, "");
    }

    public @Nullable Aspect getAspect() {
        String tag = this.entityData.get(DATA_TYPE);
        for (Aspect aspect : Aspect.values()) {
            if (aspect.getTag().equals(tag)) {
                return aspect;
            }
        }
        return null;
    }

    public void setAspect(Aspect aspect) {
        this.entityData.set(DATA_TYPE, aspect.getTag());
    }

    @Override
    public void travel(Vec3 input) {
        this.travelFlying(input, 0.02F);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected int decreaseAirSupply(int currentSupply) {
        return currentSupply;
    }

    public static boolean checkWispSpawnRules(
        EntityType<? extends Mob> type,
        ServerLevelAccessor level,
        EntitySpawnReason spawnReason,
        BlockPos pos,
        RandomSource random
    ) {
        return level.getDifficulty() != Difficulty.PEACEFUL && Mob.checkMobSpawnRules(type, level, spawnReason, pos, random);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason spawnReason) {
        BlockPos pos = this.blockPosition();
        if (level.getDifficulty() == Difficulty.PEACEFUL || level.getBrightness(LightLayer.SKY, pos) > this.random.nextInt(32)) {
            return false;
        }
        if (level instanceof ServerLevel serverLevel
            && serverLevel.getEntitiesOfClass(Wisp.class, this.getBoundingBox().inflate(16.0)).size() >= 8) {
            return false;
        }
        return level.getBrightness(LightLayer.BLOCK, pos) <= this.random.nextInt(8) && super.checkSpawnRules(level, spawnReason);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 2;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getDirectEntity() instanceof LivingEntity living) {
            this.targetedEntity = living;
            this.aggroCooldown = 200;
        }
        if (source.getEntity() instanceof LivingEntity living) {
            this.targetedEntity = living;
            this.aggroCooldown = 200;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public void tick() {
        super.tick();
        Level level = this.level();
        if (!level.isClientSide()) {
            return;
        }
        if (this.tickCount <= 1 || this.deathTime == 1) {
            Fx.get().shockBurst(level, this.getX(), this.getY() + 0.45, this.getZ(), 1.0F);
        }
        Aspect aspect = this.getAspect();
        if (aspect != null && level.getRandom().nextBoolean()) {
            int color = aspect.getColor();
            Fx.get().sparkle(
                level,
                (float) (this.getX() + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.7F),
                (float) (this.getY() + 0.45 + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.7F),
                (float) (this.getZ() + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.7F),
                0.1F,
                color,
                0.0F
            );
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.getAspect() == null) {
            this.chooseAspect(level);
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
            return;
        }
        double dx = this.waypointX - this.getX();
        double dy = this.waypointY - this.getY();
        double dz = this.waypointZ - this.getZ();
        double distance = dx * dx + dy * dy + dz * dz;
        if (distance < 1.0 || distance > 3600.0) {
            this.waypointX = this.getX() + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0;
            this.waypointY = this.getY() + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0;
            this.waypointZ = this.getZ() + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0;
        }
        if (this.courseChangeCooldown-- <= 0) {
            this.courseChangeCooldown += this.random.nextInt(5) + 2;
            distance = Math.sqrt(distance);
            if (this.isCourseTraversable(level, distance)) {
                this.setDeltaMovement(this.getDeltaMovement().add(dx / distance * 0.1, dy / distance * 0.1, dz / distance * 0.1));
            } else {
                this.waypointX = this.getX();
                this.waypointY = this.getY();
                this.waypointZ = this.getZ();
            }
        }
        if (this.targetedEntity != null && !this.targetedEntity.isAlive()) {
            this.targetedEntity = null;
        }
        this.aggroCooldown--;
        if (this.random.nextInt(1000) == 0 && (this.targetedEntity == null || this.aggroCooldown-- <= 0)) {
            this.targetedEntity = level.getNearestPlayer(AGGRO_TARGETING, this);
            if (this.targetedEntity != null) {
                this.aggroCooldown = 50;
            }
        }
        LivingEntity target = this.targetedEntity;
        if (target != null && target.distanceToSqr(this) < ATTACK_RANGE * ATTACK_RANGE) {
            double tx = target.getX() - this.getX();
            double tz = target.getZ() - this.getZ();
            float yaw = -((float) Mth.atan2(tx, tz)) * 180.0F / (float) Math.PI;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            if (this.hasLineOfSight(target)) {
                this.attackCounter++;
                if (this.attackCounter == 20) {
                    zap(level, target);
                    this.attackCounter = -20 + this.random.nextInt(20);
                }
            } else if (this.attackCounter > 0) {
                this.attackCounter--;
            }
        } else {
            Vec3 motion = this.getDeltaMovement();
            float yaw = -((float) Mth.atan2(motion.x, motion.z)) * 180.0F / (float) Math.PI;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            if (this.attackCounter > 0) {
                this.attackCounter--;
            }
        }
    }

    private void chooseAspect(ServerLevel level) {
        if (level.getBiome(this.blockPosition()).is(EERIE)) {
            Aspect[] eerieAspects = {Aspect.DARKNESS, Aspect.UNDEAD, Aspect.ENTROPY, Aspect.ELDRITCH, Aspect.POISON, Aspect.DEATH};
            this.setAspect(eerieAspects[this.random.nextInt(eerieAspects.length)]);
        } else if (this.random.nextInt(10) != 0) {
            java.util.List<Aspect> aspects = Aspect.getPrimalAspects();
            this.setAspect(aspects.get(this.random.nextInt(aspects.size())));
        } else {
            java.util.List<Aspect> aspects = Aspect.getCompoundAspects();
            this.setAspect(aspects.get(this.random.nextInt(aspects.size())));
        }
    }

    private void zap(ServerLevel level, LivingEntity target) {
        this.playSound(ModSounds.ZAP.get(), 1.0F, 1.1F);
        ModNetwork.sendToNear(
            level,
            null,
            this.getX(),
            this.getY(),
            this.getZ(),
            32.0,
            new NodeZapPayload((float) this.getX(), (float) this.getY(), (float) this.getZ(), (float) target.getX(), (float) (target.getY() + target.getEyeHeight()), (float) target.getZ())
        );
        Vec3 motion = target.getDeltaMovement();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (Math.abs(motion.x) <= 0.1F && Math.abs(motion.y) <= 0.1F && Math.abs(motion.z) <= 0.1F) {
            if (this.random.nextFloat() < 0.66F) {
                target.hurtServer(level, this.damageSources().mobAttack(this), damage + 1.0F);
            }
        } else if (this.random.nextFloat() < 0.4F) {
            target.hurtServer(level, this.damageSources().mobAttack(this), damage);
        }
    }

    private boolean isCourseTraversable(ServerLevel level, double distance) {
        double stepX = (this.waypointX - this.getX()) / distance;
        double stepY = (this.waypointY - this.getY()) / distance;
        double stepZ = (this.waypointZ - this.getZ()) / distance;
        AABB box = this.getBoundingBox();
        for (int i = 1; i < distance; i++) {
            box = box.move(stepX, stepY, stepZ);
            if (!level.noCollision(this, box)) {
                return false;
            }
        }
        BlockPos pos = BlockPos.containing(this.waypointX, this.waypointY, this.waypointZ);
        if (level.getFluidState(pos).is(FluidTags.WATER) || level.getFluidState(pos).is(FluidTags.LAVA)) {
            return false;
        }
        for (int depth = 0; depth < 11; depth++) {
            if (!level.isEmptyBlock(pos.below(depth))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.WISPLIVE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FIRE_EXTINGUISH;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WISPDEAD.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.25F;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        Aspect aspect = this.getAspect();
        if (aspect == null) {
            return;
        }
        ItemStack essence = new ItemStack(ModItems.WISP_ESSENCE.get());
        AspectKeys.ASPECTS.set(essence, new AspectList().add(aspect, 2));
        this.spawnAtLocation(level, essence);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Type", this.entityData.get(DATA_TYPE));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_TYPE, input.getStringOr("Type", ""));
    }
}
