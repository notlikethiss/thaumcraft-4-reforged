package thaumcraft.entity.golem;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;
import thaumcraft.Config;
import thaumcraft.alchemy.AlembicBlockEntity;
import thaumcraft.aspect.Aspect;
import thaumcraft.essentia.EssentiaReservoirBlockEntity;
import thaumcraft.essentia.EssentiaTransport;
import thaumcraft.essentia.JarBlockEntity;
import thaumcraft.platform.Transfer;
import thaumcraft.platform.GolemFluids;

public final class LogisticsGoals {
    private LogisticsGoals() {
    }

    public static void install(GolemBase golem, GoalSelector selector) {
        switch (golem.getCore()) {
            case 0 -> {
                selector.addGoal(0, new HomeReplaceGoal(golem));
                selector.addGoal(1, new HomeDepositGoal(golem));
                selector.addGoal(2, new HomeDropGoal(golem));
                selector.addGoal(4, new FillSourceGoal(golem));
            }
            case 1 -> {
                selector.addGoal(0, new HomeReplaceGoal(golem));
                selector.addGoal(1, new MarkerPlaceGoal(golem, false));
                selector.addGoal(2, new MarkerDropGoal(golem));
                selector.addGoal(4, new HomeSourceGoal(golem, 1));
            }
            case 2 -> {
                selector.addGoal(0, new HomeReplaceGoal(golem));
                selector.addGoal(1, new HomeDepositGoal(golem));
                selector.addGoal(2, new PickupGoal(golem));
            }
            case 5 -> {
                FluidPlan plan = new FluidPlan(golem);
                selector.addGoal(1, new FluidHomeDepositGoal(golem));
                selector.addGoal(2, new FluidGatherGoal(golem, plan));
                selector.addGoal(3, new FluidSourceGoal(golem, plan));
            }
            case 6 -> {
                selector.addGoal(1, new EssentiaDepositGoal(golem));
                selector.addGoal(2, new EssentiaGatherGoal(golem));
                selector.addGoal(3, new EssentiaSourceGoal(golem));
            }
            case 8 -> {
                selector.addGoal(0, new HomeReplaceGoal(golem));
                selector.addGoal(4, new HomeSourceGoal(golem, 8));
            }
            case 10 -> {
                selector.addGoal(0, new HomeReplaceGoal(golem));
                selector.addGoal(1, new MarkerPlaceGoal(golem, true));
                selector.addGoal(4, new HomeSourceGoal(golem, 10));
            }
        }
    }

    private abstract static class TravelGoal extends Goal {
        protected final GolemBase golem;
        protected @Nullable BlockPos target;
        private int age;
        private boolean finished;

        private TravelGoal(GolemBase golem) {
            this.golem = golem;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canContinueToUse() {
            return golem.isWorking() && target != null && !finished && age < 200
                && (!golem.getNavigation().isDone() || GolemLogistics.closeTo(golem, target, interactionDistanceSquared()));
        }

        @Override
        public void start() {
            age = 0;
            finished = false;
            navigate();
        }

        @Override
        public void tick() {
            age++;
            if (target == null) {
                finished = true;
                return;
            }
            golem.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
            if (GolemLogistics.closeTo(golem, target, interactionDistanceSquared())) {
                finished = interact(target);
            } else if (golem.getNavigation().isDone() && age % 20 == 0) {
                navigate();
            }
        }

        @Override
        public void stop() {
            target = null;
            golem.getNavigation().stop();
        }

        protected void navigate() {
            if (target != null) {
                golem.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
            }
        }

        protected double interactionDistanceSquared() { return 4.0; }

        protected abstract boolean interact(BlockPos pos);
    }

    private static final class HomeDepositGoal extends TravelGoal {
        private HomeDepositGoal(GolemBase golem) { super(golem); }

        @Override protected double interactionDistanceSquared() { return 5.0; }

        @Override
        public boolean canUse() {
            if (!golem.isWorking() || golem.tickCount % Config.golemDelay() != 0 || golem.getCarried().isEmpty()
                || !GolemLogistics.closeTo(golem, golem.getHomePosition(), 5.0)) return false;
            BlockPos home = GolemLogistics.homeContainer(golem);
            ItemStack remainder = Transfer.insert(golem.level(), home, golem.getHomeFacing(), golem.getCarried(), true);
            if (remainder.getCount() == golem.getCarried().getCount()) return false;
            target = golem.getHomePosition();
            return true;
        }

        @Override
        protected boolean interact(BlockPos pos) {
            BlockPos home = GolemLogistics.homeContainer(golem);
            golem.setCarried(insertItems(golem, home, golem.getHomeFacing(), golem.getCarried()));
            golem.startLeftArmTimer();
            return true;
        }
    }

    private static final class HomeDropGoal extends TravelGoal {
        private HomeDropGoal(GolemBase golem) { super(golem); }

        @Override protected double interactionDistanceSquared() { return 5.0; }

        @Override
        public boolean canUse() {
            if (!golem.isWorking() || golem.getCarried().isEmpty() || !GolemLogistics.closeTo(golem, golem.getHomePosition(), 5.0)
                || Transfer.hasItems(golem.level(), GolemLogistics.homeContainer(golem), golem.getHomeFacing())) return false;
            target = golem.getHomePosition();
            return true;
        }

        @Override
        protected boolean interact(BlockPos pos) {
            dropCarried(golem, pos);
            return true;
        }
    }

    private static final class HomeReplaceGoal extends TravelGoal {
        private HomeReplaceGoal(GolemBase golem) { super(golem); }

        @Override protected double interactionDistanceSquared() { return 5.0; }

        @Override
        public boolean canUse() {
            if (!golem.isWorking() || golem.tickCount % Config.golemDelay() != 0 || golem.getCarried().isEmpty()
                || !GolemLogistics.closeTo(golem, golem.getHomePosition(), 5.0)) return false;
            int core = golem.getCore();
            boolean stillWanted = switch (core) {
                case 1 -> hasMarkerDestination(golem, golem.getCarried());
                case 8 -> hasUseDestination(golem, golem.getCarried());
                case 10 -> hasSortDestination(golem, golem.getCarried());
                default -> GolemLogistics.filters(golem).stream().anyMatch(filter -> golem.matchesFilter(filter, golem.getCarried()));
            };
            if (stillWanted) return false;
            BlockPos home = GolemLogistics.homeContainer(golem);
            if (!Transfer.hasItems(golem.level(), home, golem.getHomeFacing())) return false;
            if (Transfer.insert(golem.level(), home, golem.getHomeFacing(), golem.getCarried(), true).getCount() == golem.getCarried().getCount()) return false;
            target = golem.getHomePosition();
            return true;
        }

        @Override
        protected boolean interact(BlockPos pos) {
            BlockPos home = GolemLogistics.homeContainer(golem);
            golem.setCarried(insertItems(golem, home, golem.getHomeFacing(), golem.getCarried()));
            golem.startLeftArmTimer();
            return true;
        }
    }

    private static final class HomeSourceGoal extends Goal {
        private final GolemBase golem;
        private final int core;

        private HomeSourceGoal(GolemBase golem, int core) {
            this.golem = golem;
            this.core = core;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!golem.isWorking() || !golem.getCarried().isEmpty() || golem.tickCount % Config.golemDelay() != 0
                || !golem.getNavigation().isDone() || !GolemLogistics.closeTo(golem, golem.getHomePosition(), 5.0)) return false;
            BlockPos source = GolemLogistics.homeContainer(golem);
            Direction side = golem.getHomeFacing();
            if (!Transfer.hasItems(golem.level(), source, side)) return false;
            if (core == 10) {
                return stacksAtHome(golem, source, side).stream().anyMatch(stack -> hasSortDestination(golem, stack));
            }
            if (GolemLogistics.filters(golem).isEmpty()) {
                return stacksAtHome(golem, source, side).stream().anyMatch(stack -> core == 8 ? hasUseDestination(golem, stack) : hasMarkerDestination(golem, stack));
            }
            return GolemLogistics.filters(golem).stream().anyMatch(filter -> {
                boolean valid = core == 8 ? hasUseDestination(golem, filter) : hasMarkerDestination(golem, filter);
                return valid && Transfer.extract(golem.level(), source, side, stack -> golem.matchesFilter(filter, stack), golem.getCarrySpace(), true).getCount() > 0;
            });
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            BlockPos source = GolemLogistics.homeContainer(golem);
            Direction side = golem.getHomeFacing();
            List<ItemStack> candidates = core == 10 || GolemLogistics.filters(golem).isEmpty() ? stacksAtHome(golem, source, side) : GolemLogistics.filters(golem);
            for (ItemStack candidate : candidates) {
                Predicate<ItemStack> match = core == 10 ? stack -> golem.matchesFilter(candidate, stack) : stack -> golem.matchesFilter(candidate, stack);
                if (core == 10 && !hasSortDestination(golem, candidate)) continue;
                if (core == 8 && !hasUseDestination(golem, candidate)) continue;
                if (core == 1 && !hasMarkerDestination(golem, candidate)) continue;
                ItemStack extracted = Transfer.extract(golem.level(), source, side, match, golem.getCarrySpace(), false);
                if (!extracted.isEmpty()) {
                    golem.containers().open(source);
                    golem.setCarried(extracted);
                    break;
                }
            }
        }
    }

    private static List<ItemStack> stacksAtHome(GolemBase golem, BlockPos pos, Direction side) {
        return Transfer.contents(golem.level(), pos, side);
    }

    private static final class FillSourceGoal extends TravelGoal {
        private ItemStack selected = ItemStack.EMPTY;
        private Marker source;

        private FillSourceGoal(GolemBase golem) { super(golem); }

        @Override
        public boolean canUse() {
            if (!golem.isWorking() || golem.tickCount % Config.golemDelay() != 0 || !golem.getCarried().isEmpty() || GolemLogistics.filters(golem).isEmpty()) return false;
            double nearest = golem.getRange() * golem.getRange();
            for (ItemStack needed : GolemLogistics.neededAtHome(golem)) {
                for (Marker marker : GolemLogistics.markedItemHandlers(golem, candidate -> GolemLogistics.matchesColor(golem, candidate, needed))) {
                    double distance = golem.distanceToSqr(marker.pos().getX() + 0.5, marker.pos().getY() + 0.5, marker.pos().getZ() + 0.5);
                    if (distance > nearest || GolemLogistics.isHomeBlock(golem, marker.pos())) continue;
                    int amount = Math.min(golem.getCarrySpace(), golem.getToggles()[0] ? golem.getCarrySpace() : needed.getCount());
                    ItemStack found = Transfer.extract(golem.level(), marker.pos(), marker.side(), stack -> golem.matchesFilter(needed, stack), amount, true);
                    if (!found.isEmpty()) {
                        nearest = distance;
                        selected = needed;
                        source = marker;
                    }
                }
            }
            target = source == null ? null : source.pos();
            return source != null;
        }

        @Override
        protected boolean interact(BlockPos pos) {
            if (source == null || selected.isEmpty()) return true;
            ItemStack taken = Transfer.extract(golem.level(), pos, source.side(), stack -> golem.matchesFilter(selected, stack), Math.min(golem.getCarrySpace(), GolemLogistics.missingAtHome(golem, selected)), false);
            if (!taken.isEmpty()) golem.setCarried(taken);
            if (!taken.isEmpty()) golem.containers().open(pos);
            return true;
        }

        @Override
        public void stop() {
            super.stop();
            source = null;
            selected = ItemStack.EMPTY;
        }
    }

    private static final class MarkerPlaceGoal extends TravelGoal {
        private final boolean sorting;
        private Marker destination;

        private MarkerPlaceGoal(GolemBase golem, boolean sorting) {
            super(golem);
            this.sorting = sorting;
        }

        @Override
        public boolean canUse() {
            ItemStack carried = golem.getCarried();
            if (!golem.isWorking() || carried.isEmpty() || !sorting && golem.tickCount % Config.golemDelay() != 0) return false;
            double nearest = golem.getRange() * golem.getRange();
            for (Marker marker : GolemLogistics.markedItemHandlers(golem, candidate -> sorting || GolemLogistics.matchesColor(golem, candidate, carried))) {
                if (GolemLogistics.isHomeBlock(golem, marker.pos())) continue;
                if (sorting && Transfer.count(golem.level(), marker.pos(), marker.side(), stack -> golem.matchesFilter(carried, stack)) == 0) continue;
                if (Transfer.insert(golem.level(), marker.pos(), marker.side(), carried, true).getCount() == carried.getCount()) continue;
                double distance = golem.distanceToSqr(marker.pos().getX() + 0.5, marker.pos().getY() + 0.5, marker.pos().getZ() + 0.5);
                if (distance <= nearest) {
                    nearest = distance;
                    destination = marker;
                }
            }
            if (destination == null) return false;
            target = destination.pos();
            return true;
        }

        @Override
        protected boolean interact(BlockPos pos) {
            if (destination == null) return true;
            golem.setCarried(insertItems(golem, pos, destination.side(), golem.getCarried()));
            golem.startRightArmTimer();
            return true;
        }

        @Override
        public void stop() {
            super.stop();
            destination = null;
        }
    }

    private static final class MarkerDropGoal extends TravelGoal {
        private Marker destination;

        private MarkerDropGoal(GolemBase golem) { super(golem); }

        @Override
        public boolean canUse() {
            if (!golem.isWorking() || golem.getCarried().isEmpty()) return false;
            destination = golem.getMarkers().stream().filter(marker -> marker.inLevel(golem.level()) && GolemLogistics.matchesColor(golem, marker, golem.getCarried()))
                .filter(marker -> golem.level().getBlockEntity(marker.pos()) == null && !GolemLogistics.isHomeBlock(golem, marker.pos()))
                .min(Comparator.comparingDouble(marker -> golem.distanceToSqr(marker.pos().getX() + 0.5, marker.pos().getY() + 0.5, marker.pos().getZ() + 0.5)))
                .orElse(null);
            if (destination == null || golem.distanceToSqr(destination.pos().getX() + 0.5, destination.pos().getY() + 0.5, destination.pos().getZ() + 0.5) > golem.getRange() * golem.getRange()) return false;
            target = destination.pos();
            return true;
        }

        @Override
        protected boolean interact(BlockPos pos) {
            dropCarried(golem, pos);
            return true;
        }

        @Override
        public void stop() {
            super.stop();
            destination = null;
        }
    }

    private static final class PickupGoal extends Goal {
        private final GolemBase golem;
        private final Map<UUID, Long> rejected = new HashMap<>();
        private @Nullable ItemEntity target;
        private @Nullable Path path;
        private int age;

        private PickupGoal(GolemBase golem) {
            this.golem = golem;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!golem.isWorking() || golem.tickCount % Config.golemDelay() != 0) return false;
            double range = golem.getRange();
            rejected.values().removeIf(until -> until <= golem.level().getGameTime());
            target = null;
            path = null;
            var candidates = golem.level().getEntitiesOfClass(ItemEntity.class, new AABB(golem.getHomePosition()).inflate(range), this::canTake);
            candidates.sort(Comparator.comparingDouble(golem::distanceToSqr));
            for (ItemEntity item : candidates) {
                if (rejected.containsKey(item.getUUID())) continue;
                if (canPickUp(item)) {
                    target = item;
                    return true;
                }
                Path approach = golem.getNavigation().createPath(item, 0);
                if (approach != null && approach.canReach()) {
                    target = item;
                    path = approach;
                    return true;
                }
                rejected.put(item.getUUID(), golem.level().getGameTime() + 100);
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() { return golem.isWorking() && age < 200 && target != null && canTake(target) && (!golem.getNavigation().isDone() || canPickUp(target)); }

        @Override
        public void start() {
            age = 0;
            if (path != null) golem.getNavigation().moveTo(path, 1.0);
        }

        private boolean canTake(ItemEntity item) {
            return item.isAlive() && !item.hasPickUpDelay() && golem.getCarrySpace() > 0
                && item.distanceToSqr(Vec3.atCenterOf(golem.getHomePosition())) <= golem.getRange() * golem.getRange()
                && (golem.inventory().isEmpty() || GolemLogistics.filters(golem).stream().anyMatch(filter -> golem.matchesFilter(filter, item.getItem())))
                && (golem.getCarried().isEmpty() || ItemStack.isSameItemSameComponents(golem.getCarried(), item.getItem()));
        }

        private boolean canPickUp(ItemEntity item) {
            return golem.distanceToSqr(item) <= 2.0 && golem.level().clip(new ClipContext(golem.getEyePosition(),
                item.position().add(0, item.getBbHeight() * 0.5, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, golem)).getType() == HitResult.Type.MISS;
        }

        @Override
        public void tick() {
            if (target == null) return;
            age++;
            golem.getLookControl().setLookAt(target, 30, 30);
            if (age % 20 == 0 && !canPickUp(target)) {
                path = golem.getNavigation().createPath(target, 0);
                if (path == null || !path.canReach() || !golem.getNavigation().moveTo(path, 1.0)) {
                    rejected.put(target.getUUID(), golem.level().getGameTime() + 100);
                    target = null;
                    golem.getNavigation().stop();
                    return;
                }
            }
            if (canTake(target) && canPickUp(target)) {
                ItemStack stack = target.getItem();
                int amount = Math.min(stack.getCount(), golem.getCarrySpace());
                if (amount > 0) {
                    ItemStack taken = stack.copyWithCount(amount);
                    stack.shrink(amount);
                    golem.setCarried(golem.getCarried().isEmpty() ? taken : golem.getCarried().copyWithCount(golem.getCarried().getCount() + amount));
                    golem.startActionTimer();
                    golem.level().playSound(null, target.getX(), target.getY(), target.getZ(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                        net.minecraft.sounds.SoundSource.NEUTRAL, 0.2F, ((golem.getRandom().nextFloat() - golem.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                    if (stack.isEmpty()) target.discard();
                }
                target = null;
            }
        }

        @Override
        public void stop() {
            if (target != null && target.isAlive()) rejected.put(target.getUUID(), golem.level().getGameTime() + 100);
            target = null;
            path = null;
            golem.getNavigation().stop();
        }
    }

    private static ItemStack insertItems(GolemBase golem, BlockPos pos, Direction side, ItemStack stack) {
        ItemStack remainder = Transfer.insert(golem.level(), pos, side, stack, false);
        if (remainder.getCount() < stack.getCount()) golem.containers().open(pos);
        return remainder;
    }

    private static final class EssentiaGatherGoal extends Goal {
        private final GolemBase golem;
        private BlockPos source;
        private long delayUntil;

        private EssentiaGatherGoal(GolemBase golem) { this.golem = golem; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override
        public boolean canUse() {
            if (!golem.isWorking() || !golem.getNavigation().isDone() || golem.getEssentiaAmount() >= golem.getCarryLimit() || delayUntil > System.currentTimeMillis()) return false;
            BlockPos home = GolemLogistics.homeContainer(golem);
            if (!GolemLogistics.closeTo(golem, home, 6.0)) return false;
            BlockEntity entity = golem.level().getBlockEntity(home);
            Direction sourceSide = entity instanceof JarBlockEntity || entity instanceof AlembicBlockEntity ? Direction.UP : golem.getHomeFacing();
            if (entity instanceof EssentiaReservoirBlockEntity reservoir) sourceSide = reservoir.getFacing();
            if (entity instanceof EssentiaTransport transport && transport.canOutputTo(sourceSide)
                && transport.getEssentiaAmount(sourceSide) > 0 && accepts(sourceAspect(entity, transport, sourceSide))) {
                source = home;
                return true;
            }
            int selectedY = -1;
            int previousAmount = -1;
            for (int offset = 0; offset <= 5; offset++) {
                BlockPos pos = home.above(offset);
                if (golem.level().getBlockEntity(pos) instanceof AlembicBlockEntity alembic && accepts(alembic.getAspect()) && alembic.getAmount() > previousAmount) {
                    selectedY = offset;
                    previousAmount = alembic.getAmount();
                }
            }
            if (selectedY >= 0) {
                source = home.above(selectedY);
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() { return false; }

        @Override
        public void start() {
            if (source == null) return;
            BlockEntity entity = golem.level().getBlockEntity(source);
            Direction face = golem.getHomeFacing();
            if (entity instanceof JarBlockEntity || entity instanceof AlembicBlockEntity) face = Direction.UP;
            if (entity instanceof EssentiaReservoirBlockEntity reservoir) face = reservoir.getFacing();
            if (entity instanceof EssentiaTransport transport && transport.getEssentiaAmount(face) > 0) {
                Aspect aspect = transport.getEssentiaType(face);
                if (aspect == null) aspect = sourceAspect(entity, transport, face);
                if (aspect != null && accepts(aspect)) {
                    int amount = Math.min(transport.getEssentiaAmount(face), golem.getCarryLimit() - golem.getEssentiaAmount());
                    if (entity instanceof EssentiaReservoirBlockEntity reservoir) amount = Math.min(reservoir.containerContains(aspect), amount);
                    int taken = transport.takeEssentia(aspect, amount, face);
                    if (taken > 0) {
                        golem.setEssentia(aspect, golem.getEssentiaAmount() + taken);
                        golem.startActionTimer();
                    }
                }
            }
            delayUntil = System.currentTimeMillis() + 1000L;
        }

        @Override
        public void stop() { source = null; }

        private boolean accepts(@Nullable Aspect aspect) {
            return golem.getEssentiaAmount() == 0 || golem.getEssentia() == null || golem.getEssentia().equals(aspect);
        }

        private @Nullable Aspect sourceAspect(BlockEntity entity, EssentiaTransport transport, Direction face) {
            Aspect aspect = transport.getEssentiaType(face);
            return aspect == null && entity instanceof EssentiaReservoirBlockEntity reservoir ? reservoir.getEssentiaType(null) : aspect;
        }
    }

    private static final class EssentiaDepositGoal extends Goal {
        private final GolemBase golem;
        private @Nullable EssenceDestination destination;

        private EssentiaDepositGoal(GolemBase golem) { this.golem = golem; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override
        public boolean canUse() {
            if (!golem.isWorking() || golem.getEssentia() == null || golem.getEssentiaAmount() <= 0 || !golem.getNavigation().isDone()) return false;
            destination = findEssentiaDestination(golem);
            return destination != null && GolemLogistics.closeTo(golem, destination.pos(), 4.0);
        }

        @Override public boolean canContinueToUse() { return false; }

        @Override
        public void start() {
            if (destination == null) return;
            BlockEntity entity = golem.level().getBlockEntity(destination.pos());
            Aspect aspect = golem.getEssentia();
            if (entity instanceof JarBlockEntity jar && aspect != null) {
                int remaining = jar.addToContainer(aspect, golem.getEssentiaAmount());
                golem.setEssentia(aspect, remaining);
            } else if (entity instanceof EssentiaReservoirBlockEntity reservoir && aspect != null) {
                Direction face = reservoir.getFacing();
                if (reservoir.getSuctionAmount(face) > 0 && (reservoir.getSuctionType(face) == null || reservoir.getSuctionType(face) == aspect)) {
                    int added = reservoir.addEssentia(aspect, golem.getEssentiaAmount(), face);
                    golem.setEssentia(aspect, golem.getEssentiaAmount() - added);
                }
            } else if (entity instanceof EssentiaTransport transport && aspect != null) {
                for (Marker marker : golem.getMarkers()) {
                    if (marker.inLevel(golem.level()) && marker.pos().equals(destination.pos()) && transport.canInputFrom(marker.side())
                        && transport.getSuctionAmount(marker.side()) > 0 && (transport.getSuctionType(marker.side()) == null || transport.getSuctionType(marker.side()) == aspect)) {
                        int added = transport.addEssentia(aspect, golem.getEssentiaAmount(), marker.side());
                        if (added > 0) golem.setEssentia(aspect, golem.getEssentiaAmount() - added);
                        break;
                    }
                }
            }
            if (golem.getEssentiaAmount() > 0) golem.getNavigation().moveTo(destination.pos().getX() + 0.5, destination.pos().getY(), destination.pos().getZ() + 0.5, 1.0);
        }

        @Override public void stop() { destination = null; }
    }

    private static final class EssentiaSourceGoal extends Goal {
        private final GolemBase golem;
        private @Nullable EssenceDestination destination;

        private EssentiaSourceGoal(GolemBase golem) { this.golem = golem; setFlags(EnumSet.of(Flag.MOVE)); }

        @Override
        public boolean canUse() {
            if (!golem.isWorking() || golem.getEssentia() == null || golem.getEssentiaAmount() <= 0 || !golem.getNavigation().isDone()) return false;
            destination = findEssentiaDestination(golem);
            return destination != null && !GolemLogistics.closeTo(golem, destination.pos(), 4.0);
        }

        @Override public boolean canContinueToUse() { return golem.isWorking() && destination != null && !golem.getNavigation().isDone(); }

        @Override public void start() {
            if (destination != null) golem.getNavigation().moveTo(destination.pos().getX() + 0.5, destination.pos().getY(), destination.pos().getZ() + 0.5, 1.0);
        }

        @Override public void tick() {
            if (destination != null && GolemLogistics.closeTo(golem, destination.pos(), 4.0)) golem.getNavigation().stop();
        }

        @Override public void stop() { destination = null; }
    }

    private static final class FluidHomeDepositGoal extends Goal {
        private final GolemBase golem;

        private FluidHomeDepositGoal(GolemBase golem) { this.golem = golem; setFlags(EnumSet.of(Flag.MOVE)); }

        @Override
        public boolean canUse() {
            FluidStack carried = golem.getFluidCarried();
            if (!golem.isWorking() || carried.isEmpty() || !GolemLogistics.closeTo(golem, golem.getHomePosition(), 5.0)) return false;
            FluidStack remainder = Transfer.insertFluid(golem.level(), GolemLogistics.homeContainer(golem), golem.getHomeFacing(), carried, true);
            if (remainder.getAmount() >= carried.getAmount()) return false;
            return true;
        }

        @Override public boolean canContinueToUse() { return false; }

        @Override
        public void start() {
            FluidStack carried = golem.getFluidCarried();
            if (carried.isEmpty()) return;
            FluidStack remainder = Transfer.insertFluid(golem.level(), GolemLogistics.homeContainer(golem), golem.getHomeFacing(), carried, true);
            if (remainder.getAmount() >= carried.getAmount()) return;
            golem.setFluidCarried(Transfer.insertFluid(golem.level(), GolemLogistics.homeContainer(golem), golem.getHomeFacing(), carried, false));
            golem.startActionTimer();
        }

        @Override public void stop() { }
    }

    private record FluidSource(BlockPos pos, @Nullable Direction side, boolean worldBlock) {
    }

    private static final class FluidPlan {
        private final GolemBase golem;
        private @Nullable FluidSource source;
        private FluidStack fluid = FluidStack.EMPTY;

        private FluidPlan(GolemBase golem) { this.golem = golem; }

        private List<FluidStack> wanted() {
            List<FluidStack> candidates = new ArrayList<>();
            FluidStack carried = golem.getFluidCarried();
            if (!carried.isEmpty()) {
                candidates.add(carried.copy());
            } else if (golem.inventory().isEmpty()) {
                for (Fluid fluid : BuiltInRegistries.FLUID) {
                    if (fluid != Fluids.EMPTY && fluid.defaultFluidState().isSource()) candidates.add(new FluidStack(fluid, 1000));
                }
            } else {
                for (ItemStack item : GolemLogistics.filters(golem)) {
                    FluidStack contained = GolemFluids.contained(item);
                    if (!contained.isEmpty() && candidates.stream().noneMatch(candidate -> FluidStack.isSameFluidSameComponents(candidate, contained))) {
                        candidates.add(contained.copyWithAmount(1000));
                    }
                }
            }
            BlockPos home = GolemLogistics.homeContainer(golem);
            List<FluidStack> accepted = new ArrayList<>();
            for (FluidStack candidate : candidates) {
                if (Transfer.insertFluid(golem.level(), home, golem.getHomeFacing(), candidate, true).getAmount() < candidate.getAmount()) accepted.add(candidate);
            }
            return accepted;
        }

        private @Nullable FluidSource findSource(FluidStack wanted) {
            List<FluidSource> sources = new ArrayList<>();
            for (Marker marker : golem.getMarkers()) {
                if (!marker.inLevel(golem.level()) || golem.getHomePosition().distSqr(marker.pos()) > golem.getRange() * golem.getRange()) continue;
                if (Transfer.extractFluid(golem.level(), marker.pos(), marker.side(), fluid -> FluidStack.isSameFluidSameComponents(fluid, wanted), golem.getFluidCarryLimit(), true).getAmount() > 0) {
                    sources.add(new FluidSource(marker.pos(), marker.side(), false));
                }
                if (FluidStack.isSameFluidSameComponents(GolemFluids.pickup(golem.level(), marker.pos(), true), wanted)) {
                    if (GolemLogistics.closeTo(golem, marker.pos(), 4.0)) sources.add(new FluidSource(marker.pos(), null, true));
                    if (golem.getUpgradeAmount(5) > 0) {
                        for (BlockPos connected : connectedSources(marker.pos(), wanted.getFluid())) {
                            if (golem.getHomePosition().distSqr(connected) <= golem.getRange() * golem.getRange()) return new FluidSource(connected, null, true);
                        }
                        sources.add(new FluidSource(marker.pos(), null, true));
                    } else {
                        sources.add(new FluidSource(marker.pos(), null, true));
                    }
                }
            }
            return sources.stream().min(Comparator.comparingDouble(source -> golem.distanceToSqr(source.pos().getX() + 0.5, source.pos().getY() + 0.5, source.pos().getZ() + 0.5))).orElse(null);
        }

        private List<BlockPos> connectedSources(BlockPos origin, Fluid fluid) {
            List<BlockPos> result = new ArrayList<>();
            List<BlockPos> queue = new ArrayList<>();
            java.util.Set<BlockPos> visited = new HashSet<>();
            queue.add(origin);
            visited.add(origin);
            double radius = golem.getRange() * golem.getRange();
            for (int index = 0; index < queue.size(); index++) {
                BlockPos current = queue.get(index);
                if (current.distSqr(origin) > radius) continue;
                for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    BlockPos next = current.offset(x, y, z);
                    if (current.distSqr(next) > 3 || next.distSqr(origin) > radius || !visited.add(next)) continue;
                    if (!golem.level().hasChunkAt(next) || golem.level().getFluidState(next).getType() != fluid) continue;
                    queue.add(next);
                    if (GolemFluids.pickup(golem.level(), next, true).getAmount() >= 1000) result.add(next);
                }
            }
            result.sort(Comparator.comparingDouble((BlockPos pos) -> pos.distSqr(origin)).reversed());
            return result;
        }
    }

    private static final class FluidGatherGoal extends Goal {
        private final GolemBase golem;
        private final FluidPlan plan;
        private int delay;
        private int gatherTicks;

        private FluidGatherGoal(GolemBase golem, FluidPlan plan) { this.golem = golem; this.plan = plan; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override
        public boolean canUse() {
            if (!golem.isWorking() || golem.getFluidCarried().getAmount() >= golem.getFluidCarryLimit()) return false;
            if (plan.source == null) {
                for (FluidStack wanted : plan.wanted()) {
                    FluidSource source = plan.findSource(wanted);
                    if (source != null && GolemLogistics.closeTo(golem, source.pos(), 4.0)) {
                        plan.source = source;
                        plan.fluid = wanted.copy();
                        break;
                    }
                }
            }
            if (plan.source == null || plan.fluid.isEmpty() || !GolemLogistics.closeTo(golem, plan.source.pos(), 4.0)) return false;
            return true;
        }

        @Override public boolean canContinueToUse() { return golem.isWorking() && plan.source != null && gatherTicks < 20 && plan.fluid.getAmount() > 0; }
        @Override public boolean isInterruptable() { return false; }
        @Override public void start() { delay = 0; gatherTicks = 0; }

        @Override
        public void tick() {
            gatherTicks++;
            if (++delay < 10 || plan.source == null) return;
            delay = 0;
            int free = golem.getFluidCarryLimit() - golem.getFluidCarried().getAmount();
            if (free <= 0) { plan.source = null; return; }
            if (plan.source.worldBlock() && free < 1000) { plan.source = null; return; }
            FluidStack expected = plan.source.worldBlock()
                ? GolemFluids.pickup(golem.level(), plan.source.pos(), true)
                : Transfer.extractFluid(golem.level(), plan.source.pos(), plan.source.side(), fluid -> FluidStack.isSameFluidSameComponents(fluid, plan.fluid), free, true);
            if (expected.isEmpty() || expected.getAmount() > free || !FluidStack.isSameFluidSameComponents(expected, plan.fluid)) {
                plan.source = null;
                return;
            }
            FluidStack gathered = plan.source.worldBlock()
                ? GolemFluids.pickup(golem.level(), plan.source.pos(), false)
                : Transfer.extractFluid(golem.level(), plan.source.pos(), plan.source.side(), fluid -> FluidStack.isSameFluidSameComponents(fluid, plan.fluid), free, false);
            if (!gathered.isEmpty() && (golem.getFluidCarried().isEmpty() || FluidStack.isSameFluidSameComponents(golem.getFluidCarried(), gathered)) && gathered.getAmount() <= free) {
                FluidStack carried = golem.getFluidCarried().isEmpty() ? gathered.copy() : golem.getFluidCarried().copyWithAmount(golem.getFluidCarried().getAmount() + gathered.getAmount());
                golem.setFluidCarried(carried);
                golem.startActionTimer();
                plan.fluid = gathered.copy();
                gatherTicks = 0;
                if (gathered.getAmount() >= 1000 && golem.getFluidCarried().getAmount() > golem.getFluidCarryLimit() - 1000) plan.source = null;
            } else {
                plan.source = null;
            }
        }

        @Override
        public void stop() {
            if (plan.source != null && golem.getFluidCarried().getAmount() >= golem.getFluidCarryLimit()) plan.source = null;
            gatherTicks = 0;
        }
    }

    private static final class FluidSourceGoal extends TravelGoal {
        private final FluidPlan plan;

        private FluidSourceGoal(GolemBase golem, FluidPlan plan) { super(golem); this.plan = plan; }

        @Override
        public boolean canUse() {
            FluidStack carried = golem.getFluidCarried();
            if (!golem.isWorking() || golem.tickCount % Config.golemDelay() != 0 || carried.getAmount() > golem.getFluidCarryLimit() - 1000) return false;
            for (FluidStack wanted : plan.wanted()) {
                FluidSource found = plan.findSource(wanted);
                if (found == null || GolemLogistics.closeTo(golem, found.pos(), 4.0)) continue;
                plan.source = found;
                plan.fluid = wanted.copy();
                target = found.pos();
                return true;
            }
            return false;
        }

        @Override
        protected boolean interact(BlockPos pos) { return true; }

        @Override
        public void stop() { super.stop(); }
    }

    private record EssenceDestination(BlockPos pos, @Nullable Direction side) {
    }

    private static @Nullable EssenceDestination findEssentiaDestination(GolemBase golem) {
        Aspect aspect = golem.getEssentia();
        if (aspect == null || golem.getEssentiaAmount() <= 0) return null;
        List<EssenceDestination> others = new ArrayList<>();
        List<JarBlockEntity> jars = new ArrayList<>();
        for (Marker marker : golem.getMarkers()) {
            if (!marker.inLevel(golem.level())) continue;
            BlockEntity entity = golem.level().getBlockEntity(marker.pos());
            if (entity instanceof JarBlockEntity jar) {
                if (GolemLogistics.closeTo(golem, marker.pos(), golem.getRange() * golem.getRange())) collectConnectedJars(golem, jar, jars);
            } else if (entity instanceof EssentiaReservoirBlockEntity reservoir) {
                Direction face = reservoir.getFacing();
                if (GolemLogistics.closeTo(golem, marker.pos(), golem.getRange() * golem.getRange())
                    && reservoir.getSuctionAmount(face) > 0 && (reservoir.getSuctionType(face) == null || reservoir.getSuctionType(face) == aspect)) {
                    others.add(new EssenceDestination(marker.pos(), face));
                }
            } else if (entity instanceof EssentiaTransport transport && golem.getEssentiaAmount() > 0
                && GolemLogistics.closeTo(golem, marker.pos(), golem.getRange() * golem.getRange())
                && transport.canInputFrom(marker.side()) && transport.getSuctionAmount(marker.side()) > 0
                && (transport.getSuctionType(marker.side()) == null || transport.getSuctionType(marker.side()) == aspect)) {
                others.add(new EssenceDestination(marker.pos(), marker.side()));
            }
        }
        if (!others.isEmpty()) return nearest(golem, others);
        if (jars.isEmpty()) return null;
        List<List<JarBlockEntity>> priorities = new ArrayList<>();
        for (int priority = 0; priority < 7; priority++) priorities.add(new ArrayList<>());
        for (JarBlockEntity jar : jars) {
            boolean filtered = jar.getAspectFilter() != null;
            boolean empty = jar.getAspect() == null || jar.getAmount() == 0;
            boolean partial = jar.getAmount() > 0 && jar.getAmount() < jar.getMaxAmount();
            boolean exact = jar.getAspect() == aspect;
            if (filtered && partial && exact && jar.doesContainerAccept(aspect)) priorities.get(0).add(jar);
            else if (filtered && empty && jar.doesContainerAccept(aspect)) priorities.get(1).add(jar);
            else if (filtered && jar.isVoid() && jar.getAmount() >= jar.getMaxAmount() && exact && jar.doesContainerAccept(aspect)) priorities.get(2).add(jar);
            else if (!filtered && partial && exact && jar.doesContainerAccept(aspect)) priorities.get(3).add(jar);
            else if (!filtered && empty && !jar.isVoid() && jar.doesContainerAccept(aspect)) priorities.get(4).add(jar);
            else if (!filtered && jar.isVoid() && exact && jar.doesContainerAccept(aspect)) priorities.get(5).add(jar);
            else if (!filtered && empty && jar.isVoid() && jar.doesContainerAccept(aspect)) priorities.get(6).add(jar);
        }
        for (List<JarBlockEntity> candidates : priorities) {
            if (!candidates.isEmpty()) {
                List<EssenceDestination> destinations = candidates.stream().map(jar -> new EssenceDestination(jar.getBlockPos(), Direction.UP)).toList();
                return nearest(golem, destinations);
            }
        }
        return null;
    }

    private static void collectConnectedJars(GolemBase golem, JarBlockEntity root, List<JarBlockEntity> jars) {
        ArrayDeque<JarBlockEntity> queue = new ArrayDeque<>();
        HashSet<BlockPos> visited = new HashSet<>();
        queue.add(root);
        double radius = golem.getRange() * golem.getRange();
        while (!queue.isEmpty()) {
            JarBlockEntity jar = queue.removeFirst();
            BlockPos pos = jar.getBlockPos();
            if (!visited.add(pos) || !golem.level().hasChunkAt(pos) || golem.getHomePosition().distSqr(pos) > radius) continue;
            jars.add(jar);
            for (Direction direction : Direction.values()) {
                BlockPos adjacent = pos.relative(direction);
                if (golem.level().hasChunkAt(adjacent) && golem.level().getBlockEntity(adjacent) instanceof JarBlockEntity next && !visited.contains(adjacent)) queue.addLast(next);
            }
        }
    }

    private static @Nullable EssenceDestination nearest(GolemBase golem, List<EssenceDestination> candidates) {
        return candidates.stream().min(Comparator.comparingDouble(candidate -> golem.getHomePosition().distSqr(candidate.pos()))).orElse(null);
    }

    private static boolean hasSortDestination(GolemBase golem, ItemStack stack) {
        for (Marker marker : GolemLogistics.markedItemHandlers(golem, candidate -> true)) {
            if (GolemLogistics.isHomeBlock(golem, marker.pos())) continue;
            if (golem.distanceToSqr(marker.pos().getX() + 0.5, marker.pos().getY() + 0.5, marker.pos().getZ() + 0.5) > golem.getRange() * golem.getRange()) continue;
            if (Transfer.count(golem.level(), marker.pos(), marker.side(), candidate -> golem.matchesFilter(stack, candidate)) > 0
                && Transfer.insert(golem.level(), marker.pos(), marker.side(), stack, true).getCount() < stack.getCount()) return true;
        }
        return false;
    }

    private static boolean hasMarkerDestination(GolemBase golem, ItemStack stack) {
        for (Marker marker : GolemLogistics.markedItemHandlers(golem, candidate -> GolemLogistics.matchesColor(golem, candidate, stack))) {
            if (golem.distanceToSqr(marker.pos().getX() + 0.5, marker.pos().getY() + 0.5, marker.pos().getZ() + 0.5) > golem.getRange() * golem.getRange()) continue;
            if (!GolemLogistics.isHomeBlock(golem, marker.pos())
                && Transfer.insert(golem.level(), marker.pos(), marker.side(), stack, true).getCount() < stack.getCount()) return true;
        }
        for (Marker marker : golem.getMarkers()) {
            if (marker.inLevel(golem.level()) && GolemLogistics.matchesColor(golem, marker, stack)
                && golem.level().getBlockEntity(marker.pos()) == null && !GolemLogistics.isHomeBlock(golem, marker.pos())) return true;
        }
        return false;
    }

    private static boolean hasUseDestination(GolemBase golem, ItemStack stack) {
        for (Marker marker : golem.getMarkers()) {
            if (!marker.inLevel(golem.level()) || !GolemLogistics.matchesColor(golem, marker, stack) || GolemLogistics.isHomeBlock(golem, marker.pos())) continue;
            boolean isAir = golem.level().getBlockState(marker.pos()).isAir();
            if (golem.getToggles()[0] != isAir) continue;
            if (golem.level().getBlockState(marker.pos().relative(marker.side())).isAir()) return true;
        }
        return false;
    }

    private static void dropCarried(GolemBase golem, BlockPos pos) {
        ItemStack carried = golem.getCarried();
        if (carried.isEmpty() || golem.level().isClientSide()) return;
        ItemEntity item = new ItemEntity(golem.level(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, carried.copy());
        item.setDefaultPickUpDelay();
        golem.level().addFreshEntity(item);
        golem.setCarried(ItemStack.EMPTY);
        golem.startActionTimer();
    }
}
