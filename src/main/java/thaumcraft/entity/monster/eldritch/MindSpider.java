package thaumcraft.entity.monster.eldritch;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thaumcraft.compat.ValueInput;
import thaumcraft.compat.ValueOutput;
import thaumcraft.registry.ModEntities;

public class MindSpider extends Spider {
    private static final EntityDataAccessor<Boolean> DATA_HARMLESS = SynchedEntityData.defineId(MindSpider.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_VIEWER = SynchedEntityData.defineId(MindSpider.class, EntityDataSerializers.STRING);
    private int lifeSpan = Integer.MAX_VALUE;

    public MindSpider(EntityType<? extends MindSpider> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes()
            .add(Attributes.MAX_HEALTH, 1.0)
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    public static boolean spawnForWarp(ServerLevel level, ServerPlayer player, BlockPos pos, boolean harmless) {
        MindSpider spider = ModEntities.MIND_SPIDER.get().create(level);
        if (spider == null) {
            return false;
        }
        spider.absMoveTo(pos.getX(), pos.getY(), pos.getZ(), player.getYRot(), player.getXRot());
        spider.setTarget(player);
        spider.setHarmless(harmless);
        if (harmless) {
            spider.setViewer(player.getUUID().toString());
        }
        return level.noCollision(spider) && !level.containsAnyLiquid(spider.getBoundingBox()) && level.addFreshEntity(spider);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, target ->
            target instanceof ServerPlayer
        ));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HARMLESS, false);
        builder.define(DATA_VIEWER, "");
    }

    public String getViewer() {
        return this.entityData.get(DATA_VIEWER);
    }

    public void setViewer(String player) {
        this.entityData.set(DATA_VIEWER, player == null ? "" : player);
    }

    public boolean isHarmless() {
        return this.entityData.get(DATA_HARMLESS);
    }

    public void setHarmless(boolean harmless) {
        this.entityData.set(DATA_HARMLESS, harmless);
        if (harmless) {
            this.lifeSpan = 1200;
            if (this.getTarget() instanceof Player player) {
                this.setViewer(player.getUUID().toString());
            }
        } else {
            this.lifeSpan = Integer.MAX_VALUE;
            this.setViewer("");
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return !this.isHarmless() && super.doHurtTarget(target);
    }

    @Override
    public float getVoicePitch() {
        return 0.7F;
    }

    @Override
    public boolean isInvisibleTo(Player player) {
        String viewer = this.getViewer();
        return !viewer.isEmpty() && !viewer.equals(player.getUUID().toString()) || super.isInvisibleTo(player);
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected int getBaseExperienceReward() {
        return this.isHarmless() ? 0 : 1;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (this.tickCount > this.lifeSpan) {
                this.discard();
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        thaumcraft.compat.ValueOutput output = thaumcraft.compat.ValueOutput.of(tag, this.registryAccess());
        super.addAdditionalSaveData(tag);
        output.putBoolean("harmless", this.isHarmless());
        output.putString("viewer", this.getViewer());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        thaumcraft.compat.ValueInput input = thaumcraft.compat.ValueInput.of(tag, this.registryAccess());
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_HARMLESS, input.getBooleanOr("harmless", false));
        this.entityData.set(DATA_VIEWER, input.getStringOr("viewer", ""));
    }
}
