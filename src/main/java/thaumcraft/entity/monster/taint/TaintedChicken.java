package thaumcraft.entity.monster.taint;

import net.minecraft.world.entity.EntityType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TaintedChicken extends TaintedAnimalMonster {
    public float flap;
    public float flapSpeed;
    public float oldFlapSpeed;
    public float oldFlap;
    public float flapping = 1.0F;
    public TaintedChicken(EntityType<? extends TaintedChicken> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TaintedAnimalMonster.createAttributes(8.0, 0.4, 3.0, 2.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.oldFlap = this.flap;
        this.oldFlapSpeed = this.flapSpeed;
        this.flapSpeed += (this.onGround() ? -1.0F : 4.0F) * 0.3F;
        this.flapSpeed = net.minecraft.util.Mth.clamp(this.flapSpeed, 0.0F, 1.0F);
        if (!this.onGround() && this.flapping < 1.0F) {
            this.flapping = 1.0F;
        }
        this.flapping *= 0.9F;
        Vec3 movement = this.getDeltaMovement();
        if (!this.onGround() && movement.y < 0.0) {
            this.setDeltaMovement(movement.multiply(1.0, 0.9, 1.0));
        }
        this.flap += this.flapping * 2.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CHICKEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.CHICKEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CHICKEN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.7F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.LeapAtTargetGoal(this, 0.3F));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected int wanderGoalPriority() {
        return 3;
    }

    @Override
    protected int watchGoalPriority() {
        return 4;
    }

    @Override
    protected int idleGoalPriority() {
        return 5;
    }

    @Override
    protected float watchRange() {
        return 6.0F;
    }

    @Override
    protected int animalTargetPriority() {
        return 3;
    }

    @Override
    protected int animalAttackPriority() {
        return 3;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.dropTaintResource(level, 1, 0.25F);
    }
}
