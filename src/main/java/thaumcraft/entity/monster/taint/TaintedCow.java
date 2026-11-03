package thaumcraft.entity.monster.taint;

import net.minecraft.world.entity.EntityType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.cow.CowSoundVariants;
import net.minecraft.world.level.Level;

public class TaintedCow extends TaintedAnimalMonster {
    public TaintedCow(EntityType<? extends TaintedCow> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TaintedAnimalMonster.createAttributes(40.0, 0.27, 6.0, 0.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.COW_SOUNDS.get(CowSoundVariants.SoundSet.CLASSIC).ambientSound().value();
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.COW_SOUNDS.get(CowSoundVariants.SoundSet.CLASSIC).hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COW_SOUNDS.get(CowSoundVariants.SoundSet.CLASSIC).deathSound().value();
    }

    @Override
    public float getVoicePitch() {
        return 0.7F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
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
        this.dropTaintResource(level, 1, 0.5F);
    }
}
