package thaumcraft.entity.golem.goal.work;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import thaumcraft.Thaumcraft;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.projectile.DartProjectile;

public final class GolemCombatGoals {
    private GolemCombatGoals() {
    }

    public static void install(GolemBase golem, net.minecraft.world.entity.ai.goal.GoalSelector goals, net.minecraft.world.entity.ai.goal.GoalSelector targets, boolean butcher) {
        goals.addGoal(3, new MeleeGoal(golem));
        if (golem.getGolemDecoration().contains("R")) goals.addGoal(2, new DartGoal(golem));
        if (!butcher) targets.addGoal(1, new HurtByGoal(golem));
        targets.addGoal(2, new NearestTargetGoal(golem, butcher));
    }

    private static boolean isValidTarget(GolemBase golem, LivingEntity target, boolean butcher) {
        if (!target.isAlive() || target == golem || target instanceof GolemBase || target instanceof IronGolem || target instanceof SnowGolem) return false;
        if (golem.getOwnerUUID() != null && golem.getOwnerUUID().equals(target.getUUID())) return false;
        if (!golem.isWithinHome(target.blockPosition())) return false;
        if (butcher) return target instanceof Animal animal && !animal.isBaby() && !(target instanceof Enemy) && (!(animal instanceof net.minecraft.world.entity.TamableAnimal tameable) || !tameable.isTame());
        boolean[] toggles = golem.getToggles();
        if (target instanceof Creeper) return !toggles[4] && golem.getUpgradeAmount(4) > 0;
        if (target instanceof Enemy) return !toggles[1];
        if (target instanceof Player) return !toggles[3] && golem.getUpgradeAmount(4) > 0;
        if (target instanceof Animal animal) {
            return !toggles[2]
                && golem.getUpgradeAmount(4) > 0
                && !animal.isBaby()
                && (!(animal instanceof net.minecraft.world.entity.TamableAnimal tameable) || !tameable.isTame());
        }
        return false;
    }

    private static AABB targetBox(GolemBase golem) {
        return golem.getBoundingBox().inflate(golem.getRange(), 4.0, golem.getRange());
    }

    private static final class MeleeGoal extends Goal {
        private final GolemBase golem;
        private @javax.annotation.Nullable LivingEntity target;
        private int attackTick;
        private int repath;

        private MeleeGoal(GolemBase golem) {
            this.golem = golem;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity current = golem.getTarget();
            if (!golem.isWorking() || current == null || !isValidTarget(golem, current, golem.getCore() == 9)) {
                if (current != null && !isValidTarget(golem, current, golem.getCore() == 9)) golem.setTarget(null);
                return false;
            }
            target = current;
            return golem.getNavigation().createPath(target, 0) != null;
        }

        @Override
        public boolean canContinueToUse() {
            return golem.isWorking() && target != null && isValidTarget(golem, target, golem.getCore() == 9) && !golem.getNavigation().isDone();
        }

        @Override
        public void start() {
            if (target != null) golem.getNavigation().moveTo(target, 1.0);
            repath = 0;
        }

        @Override
        public void stop() {
            target = null;
            golem.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (target == null) return;
            golem.getLookControl().setLookAt(target, 30, 30);
            if (golem.getSensing().hasLineOfSight(target) && --repath <= 0) {
                repath = 4 + golem.getRandom().nextInt(7);
                golem.getNavigation().moveTo(target, 1.0);
            }
            attackTick = Math.max(attackTick - 1, 0);
            double reach = target.getBbWidth() * 2.0F;
            double attackRange = reach * reach + 1.0;
            if (golem.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ()) <= attackRange
                && target.getBoundingBox().maxY > golem.getBoundingBox().minY
                && target.getBoundingBox().minY < golem.getBoundingBox().maxY
                && attackTick <= 0) {
                attackTick = golem.getAttackSpeed();
                golem.doHurtTarget(target);
            }
        }
    }

    private static final class DartGoal extends Goal {
        private final GolemBase golem;
        private @javax.annotation.Nullable LivingEntity target;
        private final int maxAttackTime;
        private int attackTime;

        private DartGoal(GolemBase golem) {
            this.golem = golem;
            this.maxAttackTime = 30 - golem.getUpgradeAmount(0) * 8;
            this.attackTime = maxAttackTime / 2;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity current = golem.getTarget();
            if (!golem.isWorking() || current == null || !isValidTarget(golem, current, golem.getCore() == 9)) {
                if (current != null && !isValidTarget(golem, current, golem.getCore() == 9)) golem.setTarget(null);
                return false;
            }
            if (golem.distanceToSqr(current.getX(), current.getBoundingBox().minY, current.getZ()) < 9.0) return false;
            target = current;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return golem.isWorking() && target != null && isValidTarget(golem, target, golem.getCore() == 9) && !golem.getNavigation().isDone();
        }

        @Override
        public void stop() {
            target = null;
            attackTime = maxAttackTime / 2;
        }

        @Override
        public void tick() {
            if (target == null) return;
            golem.getNavigation().moveTo(target, 1.0);
            if (!golem.getSensing().hasLineOfSight(target)) return;
            golem.getLookControl().setLookAt(target, 30, 30);
            attackTime = Math.max(attackTime - 1, 0);
            double range = golem.getRange() * 0.8F;
            if (attackTime <= 0 && golem.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ()) <= range * range) {
                DartProjectile dart = new DartProjectile(golem.level(), golem, target, 1.6F, 7.0F - golem.getUpgradeAmount(3) * 1.75F);
                dart.setBaseDamage(golem.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.4);
                golem.level().addFreshEntity(dart);
                golem.level().playSound(null, golem.blockPosition(), BuiltInRegistries.SOUND_EVENT.get(Thaumcraft.id("golemironshoot")), net.minecraft.sounds.SoundSource.HOSTILE, 0.5F, 1.0F / (golem.getRandom().nextFloat() * 0.4F + 0.6F));
                golem.startLeftArmTimer();
                attackTime = maxAttackTime;
            }
        }
    }

    private static final class HurtByGoal extends Goal {
        private final GolemBase golem;
        private int lastTimestamp;
        private @javax.annotation.Nullable LivingEntity target;

        private HurtByGoal(GolemBase golem) {
            this.golem = golem;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LivingEntity attacker = golem.getLastHurtByMob();
            if (!golem.isWorking() || attacker == null || golem.getLastHurtByMobTimestamp() == lastTimestamp || !isValidTarget(golem, attacker, false)) return false;
            target = attacker;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return golem.isWorking() && target != null && isValidTarget(golem, target, golem.getCore() == 9);
        }

        @Override
        public void start() {
            if (target != null) golem.setTarget(target);
            lastTimestamp = golem.getLastHurtByMobTimestamp();
        }

        @Override
        public void stop() {
            target = null;
        }
    }

    private static final class NearestTargetGoal extends Goal {
        private final GolemBase golem;
        private final boolean butcher;
        private @javax.annotation.Nullable LivingEntity target;
        private int nextSearch;

        private NearestTargetGoal(GolemBase golem, boolean butcher) {
            this.golem = golem;
            this.butcher = butcher;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!golem.isWorking() || golem.tickCount < nextSearch) return false;
            nextSearch = golem.tickCount + 10;
            LivingEntity current = golem.getTarget();
            if (current != null && current.isAlive() && isValidTarget(golem, current, butcher)) return false;
            List<LivingEntity> candidates = golem.level().getEntitiesOfClass(LivingEntity.class, targetBox(golem), entity -> isValidTarget(golem, entity, butcher));
            if (butcher) {
                candidates.sort(Comparator.comparingInt((LivingEntity entity) -> entity.tickCount).reversed());
                for (LivingEntity candidate : candidates) {
                    long sameKind = candidates.stream().filter(other -> other.getType() == candidate.getType()).count();
                    if (sameKind > 2) {
                        target = candidate;
                        return true;
                    }
                }
            } else {
                candidates.sort(Comparator.comparingDouble(golem::distanceToSqr));
                if (!candidates.isEmpty()) {
                    target = candidates.getFirst();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return golem.isWorking() && target != null && isValidTarget(golem, target, golem.getCore() == 9);
        }

        @Override
        public void start() {
            if (target != null) golem.setTarget(target);
        }

        @Override
        public void stop() {
            if (golem.getTarget() == target) golem.setTarget(null);
            target = null;
        }
    }
}
