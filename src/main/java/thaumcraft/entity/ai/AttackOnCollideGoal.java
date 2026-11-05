package thaumcraft.entity.ai;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public final class AttackOnCollideGoal extends MeleeAttackGoal {
    private int attackTick;

    public AttackOnCollideGoal(PathfinderMob mob, double speed, boolean longMemory) {
        super(mob, speed, longMemory);
    }

    @Override
    public void start() {
        super.start();
        this.attackTick = 0;
    }

    @Override
    public void tick() {
        if (this.attackTick > 0) {
            this.attackTick--;
        }
        super.tick();
    }

    @Override
    protected boolean isTimeToAttack() {
        return this.attackTick <= 0;
    }

    @Override
    protected void resetAttackCooldown() {
        this.attackTick = this.adjustedTickDelay(10);
    }
}
