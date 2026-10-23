package thaumcraft.entity.projectile;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.registry.ModEntities;
import thaumcraft.registry.ModItems;

public class PrimalArrow extends AbstractArrow {
    public static final ResourceKey<DamageType> AIR_ARROW = damageType("airarrow");
    public static final ResourceKey<DamageType> FIRE_ARROW = damageType("firearrow");
    public static final ResourceKey<DamageType> ORDER_ARROW = damageType("orderarrow");
    private static final EntityDataAccessor<Byte> TYPE = SynchedEntityData.defineId(PrimalArrow.class, EntityDataSerializers.BYTE);
    private static final double BASE_DAMAGE = 2.1;
    private static final int GROUND_LIFE = 100;

    private double damage = BASE_DAMAGE;
    private int knockback;
    private int groundTicks;

    public PrimalArrow(EntityType<? extends PrimalArrow> type, Level level) {
        super(type, level);
    }

    public PrimalArrow(Level level, LivingEntity shooter, float speed, int type, @Nullable ItemStack weapon) {
        super(ModEntities.PRIMAL_ARROW.get(), shooter, level, arrowStack(type), weapon);
        setArrowType(type);
        pickup = Pickup.DISALLOWED;
        float yaw = shooter.getYRot();
        float pitch = shooter.getXRot();
        Vec3 look = shooter.getViewVector(1.0F);
        setPos(
            shooter.getX() - Mth.cos(yaw / 180.0F * (float) Math.PI) * 0.16F + look.x,
            shooter.getEyeY() - 0.1F + look.y,
            shooter.getZ() - Mth.sin(yaw / 180.0F * (float) Math.PI) * 0.16F + look.z
        );
        double motionX = -Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
        double motionZ = Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
        double motionY = -Mth.sin(pitch / 180.0F * (float) Math.PI);
        shoot(motionX, motionY, motionZ, speed * 1.5F, 1.0F);
    }

    public PrimalArrow(Level level, LivingEntity shooter, int type, @Nullable ItemStack weapon) {
        super(ModEntities.PRIMAL_ARROW.get(), shooter, level, arrowStack(type), weapon);
        setArrowType(type);
        pickup = Pickup.DISALLOWED;
    }

    public PrimalArrow(Level level, double x, double y, double z, int type) {
        super(ModEntities.PRIMAL_ARROW.get(), x, y, z, level, arrowStack(type), null);
        setArrowType(type);
        pickup = Pickup.DISALLOWED;
    }

    private static ResourceKey<DamageType> damageType(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Thaumcraft.id(name));
    }

    public static ItemStack arrowStack(int type) {
        return new ItemStack(switch (type) {
            case 1 -> ModItems.FIRE_ARROW.get();
            case 2 -> ModItems.WATER_ARROW.get();
            case 3 -> ModItems.EARTH_ARROW.get();
            case 4 -> ModItems.ORDER_ARROW.get();
            case 5 -> ModItems.ENTROPY_ARROW.get();
            default -> ModItems.AIR_ARROW.get();
        });
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, (byte) 0);
    }

    public int getArrowType() {
        return entityData.get(TYPE);
    }

    public void setArrowType(int type) {
        entityData.set(TYPE, (byte) type);
    }

    public void setKnockback(int knockback) {
        this.knockback = knockback;
    }

    public void addDamage(double amount) {
        damage += amount;
    }

    private double getTypedDamage() {
        return switch (getArrowType()) {
            case 3 -> damage * 1.5;
            case 4, 5 -> damage * 0.8;
            default -> damage;
        };
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return arrowStack(getArrowType());
    }

    @Override
    protected void tickDespawn() {
        groundTicks++;
        if (groundTicks >= GROUND_LIFE) {
            discard();
        }
    }

    private DamageSource source(ResourceKey<DamageType> key) {
        Entity owner = getOwner();
        return damageSources().source(key, this, owner != null ? owner : this);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        Entity entity = hitResult.getEntity();
        Entity owner = getOwner();
        int amount = Mth.ceil(getDeltaMovement().length() * getTypedDamage());
        int type = getArrowType();
        int fire = isOnFire() && type != 2 ? 5 : 0;
        if (isCritArrow()) {
            amount += random.nextInt(amount / 2 + 2);
        }
        DamageSource source = switch (type) {
            case 0 -> source(AIR_ARROW);
            case 1 -> source(FIRE_ARROW);
            case 4 -> source(ORDER_ARROW);
            default -> damageSources().arrow(this, owner != null ? owner : this);
        };
        if (type == 1) {
            fire += 5;
        }
        if (entity instanceof LivingEntity living && level() instanceof ServerLevel) {
            if (type == 2) {
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 4));
            } else if (type == 5) {
                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 100));
            } else if (type == 4) {
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 4));
            }
        }
        if (fire > 0 && !(entity instanceof EnderMan)) {
            entity.igniteForSeconds(fire);
        }
        if (entity.hurtOrSimulate(source, amount)) {
            if (entity instanceof LivingEntity living) {
                if (knockback > 0) {
                    Vec3 motion = getDeltaMovement();
                    double horizontal = motion.horizontalDistance();
                    if (horizontal > 0.0) {
                        living.push(motion.x * knockback * 0.6F / horizontal, 0.1, motion.z * knockback * 0.6F / horizontal);
                    }
                }
                if (owner instanceof LivingEntity livingOwner) {
                    livingOwner.setLastHurtMob(entity);
                }
                doPostHurtEffects(living);
            }
            playSound(getHitGroundSoundEvent(), 1.0F, 1.2F / (random.nextFloat() * 0.2F + 0.9F));
            if (!(entity instanceof EnderMan)) {
                discard();
            }
        } else {
            setDeltaMovement(getDeltaMovement().scale(-0.1));
            setYRot(getYRot() + 180.0F);
            yRotO += 180.0F;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("type", (byte) getArrowType());
        output.putDouble("primal_damage", damage);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setArrowType(input.getByteOr("type", (byte) 0));
        damage = input.getDoubleOr("primal_damage", BASE_DAMAGE);
    }
}
