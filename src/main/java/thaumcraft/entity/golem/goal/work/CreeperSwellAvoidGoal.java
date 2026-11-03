package thaumcraft.entity.golem.goal.work;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import thaumcraft.entity.golem.GolemBase;

public final class CreeperSwellAvoidGoal extends Goal {
    private final GolemBase golem;
    private Creeper creeper;
    private Path path;
    private Vec3 destination;
    private double farSpeed;
    private double nearSpeed;

    public CreeperSwellAvoidGoal(GolemBase golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!golem.isWorking()) return false;
        if (farSpeed == 0) {
            farSpeed = 1.125;
            nearSpeed = 1.25;
        }
        List<Creeper> candidates = golem.level().getEntitiesOfClass(Creeper.class, golem.getBoundingBox().inflate(5, 3, 5));
        if (candidates.isEmpty()) return false;
        creeper = candidates.getFirst();
        if (creeper.getSwellDir() != 1 || !golem.getSensing().hasLineOfSight(creeper)) return false;
        Vec3 origin = creeper.position();
        for (int attempt = 0; attempt < 16; attempt++) {
            int x = golem.getRandom().nextInt(33) - 16;
            int z = golem.getRandom().nextInt(33) - 16;
            int y = golem.getRandom().nextInt(15) - 7;
            BlockPos pos = golem.blockPosition().offset(x, y, z);
            Vec3 candidate = Vec3.atBottomCenterOf(pos);
            if (creeper.distanceToSqr(candidate) < creeper.distanceToSqr(golem)) continue;
            Path candidatePath = golem.getNavigation().createPath(pos, 0);
            if (candidatePath != null) {
                destination = candidate;
                path = candidatePath;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return golem.isWorking() && !golem.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (destination == null) return;
        Vec3 away = destination.subtract(golem.position());
        double length = Math.sqrt(away.x * away.x + away.z * away.z);
        if (length > 0) golem.setDeltaMovement(golem.getDeltaMovement().add(away.x / length * 0.8, 0.3, away.z / length * 0.8));
        if (path != null) golem.getNavigation().moveTo(path, nearSpeed);
    }

    @Override
    public void tick() {
        if (creeper != null) {
            golem.getNavigation().setSpeedModifier(golem.distanceToSqr(creeper) < 49 ? nearSpeed : farSpeed);
        }
    }

    @Override
    public void stop() {
        creeper = null;
        path = null;
        destination = null;
    }
}
