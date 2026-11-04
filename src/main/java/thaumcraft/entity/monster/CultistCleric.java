package thaumcraft.entity.monster;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.entity.ai.AltarFocusGoal;
import thaumcraft.entity.ai.AttackOnCollideGoal;
import thaumcraft.entity.ai.CultistHurtByTargetGoal;
import thaumcraft.entity.ai.LongRangeAttackGoal;
import thaumcraft.entity.projectile.GolemOrb;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModSounds;

public class CultistCleric extends Cultist implements RangedAttackMob {
    private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(CultistCleric.class, EntityDataSerializers.BYTE);

    public CultistCleric(EntityType<? extends CultistCleric> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createCultistAttributes();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLAGS, (byte) 0);
    }

    public boolean isRitualist() {
        return (this.entityData.get(DATA_FLAGS) & 1) != 0;
    }

    public void setRitualist(boolean ritualist) {
        byte flags = this.entityData.get(DATA_FLAGS);
        this.entityData.set(DATA_FLAGS, ritualist ? (byte) (flags | 1) : (byte) (flags & ~1));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AltarFocusGoal(this));
        this.goalSelector.addGoal(2, new LongRangeAttackGoal(this, 2.0, 1.0, 20, 40, 24.0F));
        this.goalSelector.addGoal(3, new AttackOnCollideGoal(this, 1.0, false));
        this.goalSelector.addGoal(4, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new CultistHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        net.minecraft.world.level.ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason spawnReason,
        @Nullable SpawnGroupData spawnGroupData
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnReason, spawnGroupData);
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CRIMSON_CULT_HOOD.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CRIMSON_CULT_ROBE.get()));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CRIMSON_CULT_LEGGINGS.get()));
        if (this.random.nextFloat() < (difficulty.getDifficulty() == Difficulty.HARD ? 0.3F : 0.1F)) {
            this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CRIMSON_CULT_BOOTS.get()));
        }
        return result;
    }

    @Override
    public void performRangedAttack(net.minecraft.world.entity.LivingEntity target, float distanceFactor) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        double dx = target.getX() - this.getX();
        double dy = target.getBoundingBox().minY + target.getBbHeight() / 2.0 - (this.getY() + this.getBbHeight() / 2.0);
        double dz = target.getZ() - this.getZ();
        if (this.random.nextFloat() > 0.66F) {
            GolemOrb orb = GolemOrb.create(level, this, target, true);
            this.level().playSound(null, this.blockPosition(), ModSounds.ELDRITCH_GUARDIAN_ATTACK.get(), SoundSource.HOSTILE, 1.0F, 1.0F + this.random.nextFloat() * 0.1F);
            level.addFreshEntity(orb);
        } else {
            double spread = Math.sqrt(distanceFactor) * 0.5;
            this.level().levelEvent(null, 1018, this.blockPosition(), 0);
            for (int index = 0; index < 3; index++) {
                Vec3 direction = new Vec3(
                    dx + this.random.nextGaussian() * spread,
                    dy,
                    dz + this.random.nextGaussian() * spread
                );
                SmallFireball fireball = new SmallFireball(this.level(), this, direction);
                fireball.setPos(this.getX(), this.getY() + this.getBbHeight() / 2.0 + 0.5, this.getZ());
                level.addFreshEntity(fireball);
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.isRitualist();
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        this.setRitualist(false);
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("Flags", this.entityData.get(DATA_FLAGS));
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_FLAGS, input.getByteOr("Flags", (byte) 0));
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return ModSounds.CHANT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 500;
    }
}
