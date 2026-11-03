package thaumcraft.entity.golem.goal.work;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.device.CropUtils;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.platform.GolemActions;
import thaumcraft.registry.ModBlocks;

public final class CropHarvestGoal extends Goal {
    private static final int GOLEM_DELAY = 5;
    private static final int[] PLANT_X = {0, 0, 1, 1, -1, 0, -1, -1, 1};
    private static final int[] PLANT_Z = {0, 1, 0, 1, 0, -1, -1, 1, -1};
    private final GolemBase golem;
    private final int range;
    private final List<BlockPos> checklist = new ArrayList<>();
    private BlockPos target;
    private BlockState targetState;
    private int delay = -1;
    private int maxDelay = 1;
    private int step = 1;
    private int count;

    public CropHarvestGoal(GolemBase golem) {
        this.golem = golem;
        this.range = Mth.ceil(golem.getRange() / 4.0F);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!golem.isWorking() || delay >= 0 || golem.tickCount % GOLEM_DELAY != 0 || !golem.getNavigation().isDone()) return false;
        target = findGrownCrop();
        if (target == null) return false;
        targetState = golem.level().getBlockState(target);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return golem.isWorking()
            && target != null
            && golem.level().getBlockState(target) == targetState
            && count-- > 0
            && (delay > 0 || !golem.getNavigation().isDone());
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
        if (golem.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5) > 4.0) return;
        if (delay < 0) {
            delay = (int) Math.max(10.0F, (20.0F - golem.getGolemStrength() * 2.0F) * targetState.getDestroySpeed(golem.level(), target));
            maxDelay = delay;
            step = delay / Math.max(1, Math.round(delay / 6.0F));
        }
        if (delay <= 0) return;
        delay--;
        if (delay > 0 && delay % step == 0 && golem.getNavigation().isDone()) {
            golem.startActionTimer();
            var sound = targetState.getSoundType(golem.level(), target, golem).getBreakSound();
            float volume = targetState.getSoundType(golem.level(), target, golem).getVolume();
            float pitch = targetState.getSoundType(golem.level(), target, golem).getPitch();
            golem.level().playSound(null, target, sound, SoundSource.BLOCKS, (volume + 0.7F) / 8.0F, pitch * 0.5F);
            if (golem.level() instanceof ServerLevel server) server.destroyBlockProgress(golem.getId(), target, (int) (9.0F * (1.0F - (float) delay / maxDelay)));
        }
        if (delay == 0) {
            harvest();
            checkAdjacent();
        }
    }

    @Override
    public void stop() {
        if (target != null && golem.level() instanceof ServerLevel server) server.destroyBlockProgress(golem.getId(), target, -1);
        delay = -1;
        target = null;
        targetState = null;
    }

    private BlockPos findGrownCrop() {
        if (checklist.isEmpty()) {
            BlockPos home = golem.getRestrictCenter();
            for (int x = -range; x <= range; x++) for (int z = -range; z <= range; z++) checklist.add(home.offset(x, 0, z));
            for (int index = checklist.size() - 1; index > 0; index--) {
                Collections.swap(checklist, index, golem.getRandom().nextInt(index + 1));
            }
        }
        BlockPos column = checklist.remove(0);
        BlockPos home = golem.getRestrictCenter();
        for (int y = home.getY() - 3; y <= home.getY() + 3; y++) {
            BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
            if (CropUtils.isGrownCrop(golem.level(), pos)) return pos;
        }
        return null;
    }

    private void checkAdjacent() {
        BlockPos origin = target;
        BlockPos home = golem.getRestrictCenter();
        for (int xOffset = -2; xOffset <= 2; xOffset++) {
            for (int zOffset = -2; zOffset <= 2; zOffset++) {
                for (int yOffset = -1; yOffset <= 1; yOffset++) {
                    BlockPos pos = origin.offset(xOffset, yOffset, zOffset);
                    if (Math.abs(home.getX() - pos.getX()) <= range
                        && Math.abs(home.getY() - pos.getY()) <= range
                        && Math.abs(home.getZ() - pos.getZ()) <= range
                        && CropUtils.isGrownCrop(golem.level(), pos)) {
                        target = pos;
                        targetState = golem.level().getBlockState(pos);
                        delay = -1;
                        start();
                        return;
                    }
                }
            }
        }
    }

    private void harvest() {
        count = 200;
        if (golem.level() instanceof ServerLevel server) {
            if (CropUtils.isClickableCrop(targetState)) GolemActions.activate(golem, target, Direction.DOWN);
            else GolemActions.harvest(golem, target);
            if (!CropUtils.isClickableCrop(targetState) && golem.getUpgradeAmount(4) > 0) replant(server);
        }
        golem.startActionTimer();
    }

    private void replant(ServerLevel server) {
        List<ItemEntity> drops = server.getEntitiesOfClass(
            ItemEntity.class,
            new AABB(golem.getX() - 6, golem.getY() - 6, golem.getZ() - 6, golem.getX() + 6, golem.getY() + 6, golem.getZ() + 6)
        );
        for (ItemEntity entity : drops) {
            if (entity.tickCount >= 2 || entity.distanceToSqr(golem) > 36) continue;
            var toward = golem.position().subtract(entity.position()).normalize().scale(0.25);
            entity.setDeltaMovement(toward.x, 0.075, toward.z);
            ItemStack stack = entity.getItem();
            boolean done = false;
            if (stack.is(Items.COCOA_BEANS)) {
                Direction facing = targetState.hasProperty(HorizontalDirectionalBlock.FACING)
                    ? targetState.getValue(HorizontalDirectionalBlock.FACING)
                    : Direction.NORTH;
                BlockPos support = target.relative(facing);
                BlockState supportState = server.getBlockState(support);
                if (supportState.is(Blocks.JUNGLE_LOG) || supportState.is(Blocks.STRIPPED_JUNGLE_LOG)
                    || supportState.is(Blocks.JUNGLE_WOOD) || supportState.is(Blocks.STRIPPED_JUNGLE_WOOD)) {
                    BlockState cocoa = Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, facing);
                    if (cocoa.canSurvive(server, target)) {
                        server.setBlock(target, cocoa, 3);
                        stack.shrink(1);
                    }
                }
                done = true;
            } else if (stack.is(ModBlocks.MANA_BEAN.get())) {
                if (server.isEmptyBlock(target)) {
                    ItemStack remainder = GolemActions.plant(golem, target.above(), Direction.DOWN, stack.copyWithCount(1));
                    if (remainder.isEmpty()) stack.shrink(1);
                    else if (ModBlocks.MANA_POD.get().defaultBlockState().canSurvive(server, target)) {
                        server.setBlock(target, ModBlocks.MANA_POD.get().defaultBlockState(), 3);
                        stack.shrink(1);
                    }
                }
                done = true;
            } else {
                if (stack.getItem() instanceof BlockItem blockItem && CropUtils.isPlant(blockItem.getBlock().defaultBlockState())) {
                    for (int index = 0; index < PLANT_X.length && !stack.isEmpty(); index++) {
                        BlockPos soil = target.offset(PLANT_X[index], -1, PLANT_Z[index]);
                        ItemStack remainder = GolemActions.plant(golem, soil, Direction.UP, stack.copyWithCount(1));
                        if (remainder.isEmpty()) stack.shrink(1);
                    }
                }
            }
            if (stack.isEmpty()) entity.discard();
            else entity.setItem(stack);
            if (done) break;
        }
    }
}
