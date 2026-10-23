package thaumcraft.entity.monster.taint;

import net.minecraft.world.entity.EntityType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import java.util.List;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.IShearable;
import net.minecraft.world.level.Level;
import thaumcraft.registry.ModBlocks;
import thaumcraft.taint.TaintBiomes;
import java.util.EnumSet;

public class TaintedSheep extends TaintedAnimalMonster implements IShearable {
    private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(TaintedSheep.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_GRAZE_TICKS = SynchedEntityData.defineId(TaintedSheep.class, EntityDataSerializers.INT);
    private static final byte SHEARED_FLAG = 16;
    public TaintedSheep(EntityType<? extends TaintedSheep> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TaintedAnimalMonster.createAttributes(20.0, 0.25, 3.0, 1.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SHEEP_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.SHEEP_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHEEP_HURT;
    }

    @Override
    public float getVoicePitch() {
        return 0.7F;
    }

    @Override
    protected boolean targetsAnimals() {
        return false;
    }

    @Override
    protected int playerAttackPriority() {
        return 3;
    }

    @Override
    protected int villagerTargetPriority() {
        return 3;
    }

    @Override
    protected int playerTargetPriority() {
        return 3;
    }

    @Override
    protected int wanderGoalPriority() {
        return 6;
    }

    @Override
    protected int watchGoalPriority() {
        return 7;
    }

    @Override
    protected int idleGoalPriority() {
        return 8;
    }

    @Override
    protected float watchRange() {
        return 6.0F;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_FLAGS, (byte) 0);
        entityData.define(DATA_GRAZE_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new TaintGrazingGoal());
    }

    public boolean isSheared() {
        return (this.entityData.get(DATA_FLAGS) & SHEARED_FLAG) != 0;
    }

    public void setSheared(boolean sheared) {
        byte flags = this.entityData.get(DATA_FLAGS);
        this.entityData.set(DATA_FLAGS, (byte) (sheared ? flags | SHEARED_FLAG : flags & ~SHEARED_FLAG));
    }

    public int getGrazeTicks() {
        return this.entityData.get(DATA_GRAZE_TICKS);
    }

    @Override
    public boolean isShearable(Player player, ItemStack item, Level level, BlockPos pos) {
        return !this.isSheared();
    }

    @Override
    public List<ItemStack> onSheared(Player player, ItemStack item, Level level, BlockPos pos) {
        this.setSheared(true);
        int count = 1 + this.random.nextInt(3);
        return List.of(new ItemStack(Blocks.BLACK_WOOL, count));
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.dropTaintResource(level, 3, 0.5F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Sheared", this.isSheared());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setSheared(input.getBooleanOr("Sheared", false));
    }

    private final class TaintGrazingGoal extends Goal {
        private int timer;

        private TaintGrazingGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (TaintedSheep.this.random.nextInt(250) != 0) {
                return false;
            }
            BlockPos pos = TaintedSheep.this.blockPosition();
            return TaintedSheep.this.level().getBlockState(pos).is(Blocks.SHORT_GRASS)
                || TaintedSheep.this.level().getBlockState(pos.below()).is(Blocks.GRASS_BLOCK);
        }

        @Override
        public void start() {
            this.timer = 40;
            TaintedSheep.this.entityData.set(DATA_GRAZE_TICKS, this.timer);
            TaintedSheep.this.getNavigation().stop();
        }

        @Override
        public void stop() {
            TaintedSheep.this.entityData.set(DATA_GRAZE_TICKS, 0);
        }

        @Override
        public boolean canContinueToUse() {
            return this.timer > 0;
        }

        @Override
        public void tick() {
            this.timer--;
            TaintedSheep.this.entityData.set(DATA_GRAZE_TICKS, Math.max(this.timer, 0));
            if (this.timer != 4 || !(TaintedSheep.this.level() instanceof ServerLevel level)) {
                return;
            }
            BlockPos pos = TaintedSheep.this.blockPosition();
            BlockPos grass = level.getBlockState(pos).is(Blocks.SHORT_GRASS) ? pos : pos.below();
            if (!level.getBlockState(grass).is(Blocks.SHORT_GRASS) && !level.getBlockState(grass).is(Blocks.GRASS_BLOCK)) {
                return;
            }
            if (grass.equals(pos)) {
                level.levelEvent(2001, grass, net.minecraft.world.level.block.Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()) + 4096);
                level.removeBlock(grass, false);
                level.setBlockAndUpdate(pos, ModBlocks.FIBROUS_TAINT.get().defaultBlockState());
            } else if (level.getBlockState(grass).is(Blocks.GRASS_BLOCK)) {
                level.levelEvent(2001, grass, net.minecraft.world.level.block.Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
                level.setBlockAndUpdate(pos, ModBlocks.FIBROUS_TAINT.get().defaultBlockState());
            }
            TaintBiomes.set(level, pos, TaintBiomes.TAINTED_LAND);
        }
    }
}
