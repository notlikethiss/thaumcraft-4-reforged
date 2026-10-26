package thaumcraft.entity;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.registry.ModEntities;
import thaumcraft.wand.Wands;

public final class AspectOrb extends Entity {
    private static final EntityDataAccessor<String> DATA_ASPECT = SynchedEntityData.defineId(AspectOrb.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_VALUE = SynchedEntityData.defineId(AspectOrb.class, EntityDataSerializers.INT);
    private int age;
    private int cooldown;
    private int health = 5;
    @Nullable private Player closestPlayer;

    public AspectOrb(EntityType<? extends AspectOrb> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static @Nullable AspectOrb create(Level level, double x, double y, double z, Aspect aspect, int amount) {
        AspectOrb orb = ModEntities.ASPECT_ORB.get().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (orb == null || aspect == null || amount <= 0) return null;
        orb.entityData.set(DATA_ASPECT, aspect.getTag());
        orb.entityData.set(DATA_VALUE, amount);
        orb.setPos(x, y, z);
        orb.setDeltaMovement((orb.random.nextDouble() - 0.5) * 0.4, orb.random.nextDouble() * 0.4, (orb.random.nextDouble() - 0.5) * 0.4);
        return orb;
    }

    public static boolean spawn(ServerLevel level, BlockPos pos, Aspect aspect, int amount) {
        AspectOrb orb = create(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, aspect, amount);
        return orb != null && level.addFreshEntity(orb);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ASPECT, Aspect.AIR.getTag());
        builder.define(DATA_VALUE, 1);
    }

    public Aspect getAspect() {
        Aspect aspect = Aspect.getAspect(entityData.get(DATA_ASPECT));
        return aspect == null ? Aspect.AIR : aspect;
    }

    public int getAspectValue() { return entityData.get(DATA_VALUE); }
    public int getAge() { return age; }

    @Override public void tick() {
        super.tick();
        if (cooldown > 0) cooldown--;
        if (level().isClientSide()) return;
        Player touching = level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.2), Player::isAlive).stream().findFirst().orElse(null);
        if (touching != null) playerTouch(touching);
        if (level().getBlockState(blockPosition()).is(Blocks.LAVA)) {
            setDeltaMovement((random.nextDouble() - 0.5) * 0.4, 0.2, (random.nextDouble() - 0.5) * 0.4);
            level().playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.4F, 2.0F);
        }
        if (age % 5 == 0 && closestPlayer == null) {
            closestPlayer = level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(8.0), p -> hasRoom(p))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        }
        if (closestPlayer != null) {
            if (!closestPlayer.isAlive() || closestPlayer.distanceToSqr(this) > 100.0 || !hasRoom(closestPlayer)) closestPlayer = null;
            else {
                Vec3 delta = new Vec3(closestPlayer.getX() - getX(), closestPlayer.getEyeY() - getY(), closestPlayer.getZ() - getZ()).scale(1.0 / 8.0);
                double distance = delta.length();
                if (distance > 0.0 && distance < 1.0) setDeltaMovement(getDeltaMovement().add(delta.scale((1.0 - distance) * (1.0 - distance) * 0.1 / distance)));
            }
        }
        setDeltaMovement(getDeltaMovement().add(0.0, -0.03, 0.0));
        move(MoverType.SELF, getDeltaMovement());
        double friction = onGround() ? 0.58800006 : 0.98;
        Vec3 velocity = getDeltaMovement().multiply(friction, 0.98, friction);
        if (onGround()) velocity = new Vec3(velocity.x, velocity.y * -0.9, velocity.z);
        setDeltaMovement(velocity);
        if (++age >= 150) discard();
    }

    private boolean hasRoom(Player player) {
        return getAspect().isPrimal() && wandSlot(player) >= 0;
    }

    private int wandSlot(Player player) {
        for (int slot = 0; slot < 9; slot++) {
            var stack = player.getInventory().getItem(slot);
            if (Wands.isWand(stack) && Wands.getVis(stack, getAspect()) < Wands.getMaxVis(stack)) return slot;
        }
        return -1;
    }

    @Override public void playerTouch(Player player) {
        if (!(level() instanceof ServerLevel serverLevel) || cooldown > 0 || player.takeXpDelay > 0 || !hasRoom(player)) return;
        int slot = wandSlot(player);
        if (slot < 0) return;
        var wand = player.getInventory().getItem(slot);
        Wands.addVis(wand, getAspect(), getAspectValue(), true);
        player.takeXpDelay = 2;
        serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.1F, 0.5F + random.nextFloat() * 0.2F);
        discard();
    }

    @Override protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Health", health);
        output.putInt("Age", age);
        output.putInt("Value", getAspectValue());
        output.putString("Aspect", getAspect().getTag());
    }

    @Override protected void readAdditionalSaveData(ValueInput input) {
        health = input.getIntOr("Health", 5);
        age = input.getIntOr("Age", 0);
        entityData.set(DATA_VALUE, Math.max(1, input.getIntOr("Value", 1)));
        Aspect loaded = Aspect.getAspect(input.getStringOr("Aspect", Aspect.AIR.getTag()));
        entityData.set(DATA_ASPECT, loaded == null ? Aspect.AIR.getTag() : loaded.getTag());
    }

    @Override public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        health -= (int) amount;
        if (health <= 0) discard();
        return false;
    }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean canBeCollidedWith(@Nullable Entity entity) { return false; }
}
