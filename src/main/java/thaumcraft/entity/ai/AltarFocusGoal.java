package thaumcraft.entity.ai;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.eldritch.EldritchBlock;
import thaumcraft.entity.monster.CultistCleric;

public final class AltarFocusGoal extends Goal {
    private final CultistCleric cleric;

    public AltarFocusGoal(CultistCleric cleric) {
        this.cleric = cleric;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    private boolean focused() {
        return this.cleric.isRitualist() && this.cleric.hasRestriction();
    }

    @Override
    public boolean canUse() {
        return this.focused();
    }

    @Override
    public boolean canContinueToUse() {
        return this.focused();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.cleric.getNavigation().stop();
    }

    @Override
    public void tick() {
        BlockPos altar = this.cleric.getRestrictCenter();
        this.cleric.getLookControl().setLookAt(altar.getX() + 0.5, altar.getY() + 1.5, altar.getZ() + 0.5);
        if (this.cleric.tickCount % 40 == 0
            && (altar.distSqr(this.cleric.blockPosition()) > 16.0 || !(this.cleric.level().getBlockState(altar).getBlock() instanceof EldritchBlock))) {
            this.cleric.setRitualist(false);
        }
    }
}
