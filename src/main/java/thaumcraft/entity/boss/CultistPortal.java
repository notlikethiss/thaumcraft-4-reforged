package thaumcraft.entity.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thaumcraft.compat.ValueInput;
import thaumcraft.compat.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import thaumcraft.entity.monster.CultistCleric;
import thaumcraft.registry.ModSounds;
import thaumcraft.registry.EldritchEntities;
import thaumcraft.registry.EldritchContent;
import net.minecraft.world.item.ItemStack;

public class CultistPortal extends Monster {
    private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(CultistPortal.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PULSE = SynchedEntityData.defineId(CultistPortal.class, EntityDataSerializers.INT);
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private int stage;
    private int stageCounter = 200;
    private int pulse;
    private boolean finalLeaderSpawned;

    public CultistPortal(EntityType<? extends CultistPortal> type, Level level) {
        super(type, level);
        this.xpReward = 30;
        this.setNoGravity(true);
        this.setNoAi(true);
        this.setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ATTACK_DAMAGE, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 5.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STAGE, 0);
        builder.define(DATA_PULSE, 0);
    }

    public int getStage() {
        return this.entityData.get(DATA_STAGE);
    }

    public int getPulse() {
        return this.entityData.get(DATA_PULSE);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setVisible(this.level().hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 64.0));
        if (this.pulse > 0) {
            this.pulse--;
            this.entityData.set(DATA_PULSE, this.pulse);
        }
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(1.75))) {
            if (this.distanceToSqr(player) < 3.0 && player.hurt(level.damageSources().indirectMagic(this, this), 8.0F)) {
                this.playSound(ModSounds.ZAP.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
            }
        }
        if (this.stageCounter <= 0) {
            if (level.getNearestPlayer(this, 48.0) == null) {
                this.stageCounter = 30 + this.random.nextInt(30);
            } else {
            this.pulse = 10;
            this.entityData.set(DATA_PULSE, this.pulse);
            this.playSound(ModSounds.WANDFAIL.get(), 1.0F, 1.0F);
            if (this.stage <= 4) {
                this.stageCounter = 15 + this.random.nextInt(Math.max(1, 10 - this.stage)) - this.stage;
                this.spawnMinion(level);
            } else if (this.stage <= 11) {
                int timing = level.getEntitiesOfClass(thaumcraft.entity.monster.Cultist.class, this.getBoundingBox().inflate(32.0)).size() * 20;
                this.stageCounter = timing + this.random.nextInt(5 + timing / 3);
                this.spawnMinion(level);
            } else if (this.stage == 12) {
                this.stageCounter = 50 + level.getEntitiesOfClass(thaumcraft.entity.monster.Cultist.class, this.getBoundingBox().inflate(32.0)).size() * 40 + this.random.nextInt(50);
                this.spawnLeader(level);
                this.finalLeaderSpawned = true;
            } else {
                int timing = level.getEntitiesOfClass(thaumcraft.entity.monster.Cultist.class, this.getBoundingBox().inflate(32.0)).size() * 20;
                this.stageCounter = timing + this.random.nextInt(5 + timing / 3);
                this.spawnMinion(level);
                this.hurt(level.damageSources().fellOutOfWorld(), 5 + this.random.nextInt(5));
            }
            this.stage++;
            this.entityData.set(DATA_STAGE, this.stage);
            }
        } else {
            this.stageCounter--;
            if (this.stageCounter == 160 && this.stage == 0) {
                this.pulse = 10;
                this.entityData.set(DATA_PULSE, this.pulse);
                BlockPos center = new BlockPos((int) this.getX(), (int) this.getY(), (int) this.getZ());
                placeBanner(level, center.relative(net.minecraft.core.Direction.NORTH, 6), 8);
                placeBanner(level, center.relative(net.minecraft.core.Direction.SOUTH, 6), 0);
                placeBanner(level, center.relative(net.minecraft.core.Direction.WEST, 6), 4);
                placeBanner(level, center.relative(net.minecraft.core.Direction.EAST, 6), 12);
            }
            if (this.stageCounter > 20 && this.stageCounter < 150 && this.stage == 0 && this.stageCounter % 13 == 0) {
                int centerX = (int) this.getX();
                int centerZ = (int) this.getZ();
                int x = centerX + this.random.nextInt(5) - this.random.nextInt(5);
                int z = centerZ + this.random.nextInt(5) - this.random.nextInt(5);
                BlockPos cratePos = new BlockPos(x, (int) this.getY(), z);
                if (x != centerX && z != centerZ && level.isEmptyBlock(cratePos)) {
                    this.pulse = 10;
                    this.entityData.set(DATA_PULSE, this.pulse);
                    float tierRoll = level.getRandom().nextFloat();
                    net.minecraft.world.level.block.state.BlockState crate = thaumcraft.registry.ModBlocks.LOOT_CRATE.get().defaultBlockState()
                        .setValue(thaumcraft.block.LootBlock.TIER, tierRoll < 0.05F
                            ? thaumcraft.block.LootBlock.Tier.RARE
                            : tierRoll < 0.2F ? thaumcraft.block.LootBlock.Tier.UNCOMMON : thaumcraft.block.LootBlock.Tier.COMMON);
                    level.setBlock(cratePos, crate, 3);
                    this.playSound(ModSounds.WANDFAIL.get(), 1.0F, 1.0F);
                }
            }
        }
        if (this.stage < 12) {
            this.heal(1.0F);
        }
    }

    private void spawnMinion(ServerLevel level) {
        var type = this.random.nextFloat() > 0.33F ? thaumcraft.registry.ModEntities.CULTIST_KNIGHT.get() : thaumcraft.registry.ModEntities.CULTIST_CLERIC.get();
        var cultist = type.create(level);
        if (cultist == null) {
            return;
        }
        cultist.moveTo(this.getX() + this.random.nextFloat() - this.random.nextFloat(), this.getY() + 0.25, this.getZ() + this.random.nextFloat() - this.random.nextFloat(), this.getYRot(), 0.0F);
        cultist.restrictTo(this.blockPosition(), 32);
        cultist.finalizeSpawn(level, level.getCurrentDifficultyAt(this.blockPosition()), net.minecraft.world.entity.MobSpawnType.TRIGGERED, null);
        if (cultist instanceof CultistCleric cleric) {
            cleric.setRitualist(true);
        }
        level.addFreshEntity(cultist);
        cultist.playSound(ModSounds.WANDFAIL.get(), 1.0F, 1.0F);
    }

    private void spawnLeader(ServerLevel level) {
        CultistLeader leader = EldritchEntities.CULTIST_LEADER.get().create(level);
        if (leader == null) {
            return;
        }
        leader.moveTo(this.getX() + this.random.nextFloat() - this.random.nextFloat(), this.getY() + 0.25, this.getZ() + this.random.nextFloat() - this.random.nextFloat(), this.getYRot(), 0.0F);
        leader.finalizeSpawn(level, level.getCurrentDifficultyAt(this.blockPosition()), net.minecraft.world.entity.MobSpawnType.TRIGGERED, null);
        leader.restrictTo(this.blockPosition(), 32);
        level.addFreshEntity(leader);
        leader.playSound(ModSounds.WANDFAIL.get(), 1.0F, 1.0F);
    }

    private void placeBanner(ServerLevel level, BlockPos pos, int facing) {
        level.setBlock(pos, thaumcraft.registry.ModBlocks.BANNER.get().defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof thaumcraft.device.BannerBlockEntity banner) {
            banner.setFacing(facing);
            banner.sync();
            this.playSound(ModSounds.WANDFAIL.get(), 1.0F, 1.0F);
        }
    }

    @Override
    public void push(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        boolean hit = super.hurt(source, damage);
        if (hit) {
            this.pulse = 10;
            this.entityData.set(DATA_PULSE, 10);
        }
        return hit;
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
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        ValueOutput output = ValueOutput.of(tag, registryAccess());
        super.addAdditionalSaveData(tag);
        output.putInt("Stage", this.stage);
        output.putInt("StageCounter", this.stageCounter);
        output.putBoolean("LeaderSpawned", this.finalLeaderSpawned);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        ValueInput input = ValueInput.of(tag, registryAccess());
        super.readAdditionalSaveData(tag);
        this.stage = Math.max(0, input.getIntOr("Stage", 0));
        this.entityData.set(DATA_STAGE, this.stage);
        this.stageCounter = Math.max(0, input.getIntOr("StageCounter", 200));
        this.finalLeaderSpawned = input.getBooleanOr("LeaderSpawned", this.stage > 12);
    }

    @Override
    public void die(DamageSource cause) {
        if (!this.level().isClientSide()) {
            this.level().explode(this, this.damageSources().explosion(this, this), null, this.getX(), this.getY(), this.getZ(), 2.0F, false, Level.ExplosionInteraction.NONE);
        }
        super.die(cause);
        this.bossEvent.setVisible(false);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.spawnAtLocation(new ItemStack(EldritchContent.PRIMORDIAL_PEARL.get()));
    }
}
