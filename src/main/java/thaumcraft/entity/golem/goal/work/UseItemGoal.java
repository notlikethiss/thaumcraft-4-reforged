package thaumcraft.entity.golem.goal.work;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.golem.Marker;
import thaumcraft.platform.GolemActions;

public final class UseItemGoal extends Goal {
    private final GolemBase golem;
    private int nextTick;
    private int count;
    private BlockPos target;
    private Direction side;
    private BlockState state;

    public UseItemGoal(GolemBase golem) {
        this.golem = golem;
        this.nextTick = golem.tickCount + golem.getRandom().nextInt(6);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        boolean ignoreItem = !thaumcraft.platform.Transfer.hasItems(golem.level(), golem.getHomePosition().relative(golem.getHomeFacing().getOpposite()), golem.getHomeFacing());
        if (!golem.isWorking()
            || golem.getCarried().isEmpty() && !ignoreItem
            || golem.tickCount < nextTick
            || !golem.getNavigation().isDone()) return false;
        int delay = Math.max(1, 5 - golem.tickCount);
        nextTick = golem.tickCount + delay * 3;
        return findSomething();
    }

    @Override
    public boolean canContinueToUse() {
        return golem.isWorking() && target != null && golem.level().getBlockState(target) == state && count-- > 0 && !golem.getNavigation().isDone();
    }

    @Override
    public void start() {
        count = 200;
        golem.getNavigation().moveTo(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 1.0);
    }

    @Override
    public void tick() {
        if (target == null) return;
        golem.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 30, 30);
        if (golem.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5) <= 4.0) {
            boolean leftClick = golem.getToggles()[1];
            BlockPos clicked = golem.level().isEmptyBlock(target) ? target.relative(side.getOpposite()) : target;
            if (GolemActions.useItem(golem, clicked, side)) {
                if (leftClick) golem.startLeftArmTimer();
                else golem.startRightArmTimer();
            }
            golem.getNavigation().stop();
            count = 0;
        }
    }

    @Override
    public void stop() {
        target = null;
        state = null;
        side = null;
        count = 0;
        golem.getNavigation().stop();
    }

    private boolean findSomething() {
        boolean place = golem.getToggles()[0];
        for (int color : golem.getColorsMatching(golem.getCarried())) {
            for (Marker marker : golem.getMarkers()) {
                if (!marker.inLevel(golem.level()) || marker.color() != color && color != -1) continue;
                BlockPos pos = marker.pos();
                boolean air = golem.level().isEmptyBlock(pos);
                if (place != air) continue;
                if (!golem.level().isEmptyBlock(pos.relative(marker.side()))) continue;
                target = pos;
                side = marker.side();
                state = golem.level().getBlockState(pos);
                return true;
            }
        }
        return false;
    }
}
