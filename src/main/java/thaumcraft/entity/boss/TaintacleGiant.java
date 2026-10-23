package thaumcraft.entity.boss;

import net.minecraft.world.BossEvent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import thaumcraft.entity.monster.ChampionModifier;
import thaumcraft.entity.monster.taint.Taintacle;
import thaumcraft.registry.EldritchContent;

public class TaintacleGiant extends Taintacle {
    private static final EntityDataAccessor<Integer> DATA_ANGER = SynchedEntityData.defineId(TaintacleGiant.class, EntityDataSerializers.INT);
    private final ServerBossEvent bossEvent = new ServerBossEvent(java.util.UUID.randomUUID(), this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    public TaintacleGiant(EntityType<? extends TaintacleGiant> type, Level level) {
        super(type, level);
        this.xpReward = 20;
        this.setCanPickUpLoot(false);
        this.bossEvent.setVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Taintacle.createAttributes().add(Attributes.MAX_HEALTH, 125.0).add(Attributes.ATTACK_DAMAGE, 9.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANGER, 0);
    }

    public int getAnger() {
        return this.entityData.get(DATA_ANGER);
    }

    public void setAnger(int anger) {
        this.entityData.set(DATA_ANGER, Math.max(0, Math.min(Short.MAX_VALUE, anger)));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.getAnger() > 0 && this.random.nextInt(15) == 0) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER, this.getX() + (this.random.nextFloat() - 0.5F) * this.getBbWidth(), this.getY() + this.getBbHeight() + this.random.nextFloat() * 0.5F, this.getZ() + (this.random.nextFloat() - 0.5F) * this.getBbWidth(), 0.0, 0.02, 0.0);
            }
            return;
        }
        if (this.getAnger() > 0) {
            this.setAnger(this.getAnger() - 1);
        }
        if (this.tickCount % 30 == 0) {
            this.heal(1.0F);
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setVisible(true);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (damage > 35.0F) {
            if (this.getAnger() == 0) {
                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, (int) (damage / 15.0F)));
                this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, (int) (damage / 40.0F)));
                this.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, (int) (damage / 40.0F)));
                this.setAnger(200);
                if (source.getEntity() instanceof ServerPlayer player) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(this.getDisplayName().getString() + " " + net.minecraft.network.chat.Component.translatable("tc.boss.enrage").getString()));
                }
            }
            damage = 35.0F;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        ChampionModifier.assign(this, this.random.nextInt(13), true);
        return result;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        if (level.getEntitiesOfClass(TaintacleGiant.class, this.getBoundingBox().inflate(48.0)).stream().noneMatch(entity -> entity != this && entity.isAlive())) {
            this.spawnAtLocation(level, new ItemStack(EldritchContent.PRIMORDIAL_PEARL.get()));
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("BossAnger", this.getAnger());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setAnger(input.getIntOr("BossAnger", 0));
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        this.bossEvent.setVisible(false);
    }
}
