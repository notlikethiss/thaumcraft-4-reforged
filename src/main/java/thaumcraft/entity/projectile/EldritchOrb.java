package thaumcraft.entity.projectile;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import thaumcraft.entity.CombatUtils;
import thaumcraft.fx.Fx;
import thaumcraft.registry.ModEntities;

public class EldritchOrb extends ThrowableProjectile {
    public EldritchOrb(EntityType<? extends EldritchOrb> type, Level level) {
        super(type, level);
    }

    public static EldritchOrb create(ServerLevel level, Monster shooter, LivingEntity target, boolean left) {
        EldritchOrb orb = new EldritchOrb(ModEntities.ELDRITCH_ORB.get(), level);
        CombatUtils.launch(orb, shooter, 1.0F);
        double offsetAngle = shooter.getYRot() + (left ? 90.0F : 180.0F);
        double side = Math.cos(offsetAngle * Math.PI / 180.0) * 0.5;
        double forward = Math.sin(offsetAngle * Math.PI / 180.0) * 0.5;
        orb.absMoveTo(orb.getX() - side, orb.getY() - shooter.getBbHeight() * 0.057777777, orb.getZ() - forward, orb.getYRot(), orb.getXRot());
        CombatUtils.aim(orb, target.getX() + target.getDeltaMovement().x - shooter.getX(), target.getY() - shooter.getY() - target.getBbHeight() / 2.0, target.getZ() + target.getDeltaMovement().z - shooter.getZ(), 1.0F, 2.0F);
        return orb;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    protected void onHit(HitResult hit) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.getOwner() instanceof LivingEntity owner) {
            DamageSource source = level.damageSources().indirectMagic(this, owner);
            for (Entity entity : level.getEntities(owner, this.getBoundingBox().inflate(2.0))) {
                if (entity instanceof LivingEntity living && entity.getType().getTags().noneMatch(EntityTypeTags.UNDEAD::equals)) {
                    living.hurt(source, (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.666F);
                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160), owner);
                }
            }
            level.playSound(null, this.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.5F, 2.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.8F);
            level.broadcastEntityEvent(this, (byte) 16);
        }
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16 && this.level().isClientSide()) {
            Fx.get().eldritchOrbBurst(this.level(), this);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        return false;
    }
}
