package thaumcraft.entity.monster.taint;

import net.minecraft.world.entity.EntityType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.pig.PigSoundVariants;
import net.minecraft.world.level.Level;

public class TaintedPig extends TaintedAnimalMonster {
    public TaintedPig(EntityType<? extends TaintedPig> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TaintedAnimalMonster.createAttributes(20.0, 0.275, 4.0, 2.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIG_SOUNDS.get(PigSoundVariants.SoundSet.CLASSIC).adultSounds().ambientSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.PIG_SOUNDS.get(PigSoundVariants.SoundSet.CLASSIC).adultSounds().hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIG_SOUNDS.get(PigSoundVariants.SoundSet.CLASSIC).adultSounds().deathSound().value();
    }

    @Override
    public float getVoicePitch() {
        return 0.7F;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected int wanderGoalPriority() {
        return 5;
    }

    @Override
    protected int watchGoalPriority() {
        return 6;
    }

    @Override
    protected int idleGoalPriority() {
        return 7;
    }

    @Override
    protected float watchRange() {
        return 6.0F;
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.dropTaintResource(level, 3, 0.5F);
    }
}
