package thaumcraft.entity.monster.taint;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.registry.ModItems;
import thaumcraft.entity.TaintedMob;

public abstract class TaintedAnimalMonster extends Monster implements TaintedMob {
    protected TaintedAnimalMonster(EntityType<? extends TaintedAnimalMonster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes(double health, double speed, double damage, double armor) {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, health)
            .add(Attributes.MOVEMENT_SPEED, speed)
            .add(Attributes.ATTACK_DAMAGE, damage)
            .add(Attributes.ARMOR, armor);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.addAttackGoal(this.playerAttackPriority(), Player.class);
        if (this.targetsVillagers()) {
            this.addAttackGoal(this.villagerAttackPriority(), Villager.class);
        }
        if (this.targetsAnimals()) {
            this.addAttackGoal(this.animalAttackPriority(), Animal.class);
        }
        this.goalSelector.addGoal(this.wanderGoalPriority(), new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(this.watchGoalPriority(), new LookAtPlayerGoal(this, Player.class, this.watchRange()));
        this.goalSelector.addGoal(this.idleGoalPriority(), new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.addTargetGoal(this.playerTargetPriority(), Player.class);
        if (this.targetsVillagers()) {
            this.addTargetGoal(this.villagerTargetPriority(), Villager.class);
        }
        if (this.targetsAnimals()) {
            this.addTargetGoal(this.animalTargetPriority(), Animal.class);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.tickCount < 5) {
            for (int index = 0; index < 10; index++) {
                this.level().addParticle(ParticleTypes.SPLASH, this.getX() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                    this.getY() + this.random.nextDouble() * this.getBbHeight(), this.getZ() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                    0.0, 0.0, 0.0);
            }
        }
    }

    protected int playerAttackPriority() {
        return 2;
    }

    protected int villagerAttackPriority() {
        return 3;
    }

    protected int animalAttackPriority() {
        return 8;
    }

    protected int playerTargetPriority() {
        return 2;
    }

    protected int villagerTargetPriority() {
        return 2;
    }

    protected int animalTargetPriority() {
        return 8;
    }

    protected int wanderGoalPriority() {
        return 6;
    }

    protected int watchGoalPriority() {
        return 7;
    }

    protected int idleGoalPriority() {
        return 8;
    }

    protected float watchRange() {
        return 8.0F;
    }

    private <T extends LivingEntity> void addAttackGoal(int priority, Class<T> targetClass) {
        this.goalSelector.addGoal(priority, new TaintedMeleeAttackGoal(this, targetClass));
    }

    private <T extends LivingEntity> void addTargetGoal(int priority, Class<T> targetClass) {
        this.targetSelector.addGoal(priority, new NearestAttackableTargetGoal<>(this, targetClass, true));
    }

    private static final class TaintedMeleeAttackGoal extends MeleeAttackGoal {
        private final TaintedAnimalMonster mob;
        private final Class<? extends LivingEntity> targetClass;

        private TaintedMeleeAttackGoal(TaintedAnimalMonster mob, Class<? extends LivingEntity> targetClass) {
            super(mob, 1.0, false);
            this.mob = mob;
            this.targetClass = targetClass;
        }

        @Override
        public boolean canUse() {
            return this.targetClass.isInstance(this.mob.getTarget()) && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.targetClass.isInstance(this.mob.getTarget()) && super.canContinueToUse();
        }
    }

    protected boolean targetsVillagers() {
        return true;
    }

    protected boolean targetsAnimals() {
        return true;
    }

    protected void dropTaintResource(ServerLevel level, int chanceDenominator, float gooChance) {
        if (level.getRandom().nextInt(chanceDenominator) == 0) {
            Item item = level.getRandom().nextFloat() < gooChance ? ModItems.TAINTED_GOO.get() : ModItems.TAINT_TENDRIL.get();
            this.spawnAtLocation(new ItemStack(item));
        }
    }
}
