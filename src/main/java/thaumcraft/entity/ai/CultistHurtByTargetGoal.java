package thaumcraft.entity.ai;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.phys.AABB;
import thaumcraft.entity.monster.Cultist;
import thaumcraft.entity.monster.CultistCleric;

public final class CultistHurtByTargetGoal extends HurtByTargetGoal {
    public CultistHurtByTargetGoal(PathfinderMob mob) {
        super(mob);
        this.setAlertOthers();
    }

    @Override
    protected void alertOthers() {
        LivingEntity attacker = this.mob.getLastHurtByMob();
        if (attacker == null) {
            return;
        }
        double range = this.getFollowDistance();
        AABB area = AABB.unitCubeFromLowerCorner(this.mob.position()).inflate(range, 10.0, range);
        for (Cultist other : this.mob.level().getEntitiesOfClass(Cultist.class, area, EntitySelector.NO_SPECTATORS)) {
            if (other == this.mob || other.getTarget() != null || other.isAlliedTo(attacker)) {
                continue;
            }
            if (other instanceof CultistCleric cleric && cleric.isRitualist()) {
                if (this.mob.getRandom().nextInt(3) == 0) {
                    cleric.setRitualist(false);
                    cleric.setTarget(attacker);
                }
                continue;
            }
            other.setTarget(attacker);
        }
    }
}
