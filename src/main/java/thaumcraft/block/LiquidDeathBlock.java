package thaumcraft.block;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;
import thaumcraft.damage.ModDamageTypes;
import thaumcraft.registry.ModFluids;
import thaumcraft.fx.Fx;

public class LiquidDeathBlock extends LiquidBlock {
    public static final int QUANTA_PER_BLOCK = 4;
    private static final int TICK_RATE = 7;
    private static final int SLOPE_FIND_DISTANCE = 4;
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    public LiquidDeathBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        int level = state.getValue(LEVEL);
        int quanta = level == 0 ? QUANTA_PER_BLOCK : QUANTA_PER_BLOCK - level;
        if (quanta <= 0) {
            return net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();
        }
        return quanta == QUANTA_PER_BLOCK
            ? ModFluids.LIQUID_DEATH.get().getSource(false)
            : ModFluids.FLOWING_LIQUID_DEATH.get().getFlowing(quanta, false);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return Shapes.block();
    }

    @Override
    protected boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
        return getQuanta(state) <= 2;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return getQuanta(state) <= 2;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Fx.get().liquidDeath(level, pos, state.getValue(LEVEL), random);
        super.animateTick(state, level, pos, random);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, TICK_RATE);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        level.scheduleTick(pos, this, TICK_RATE);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int quanta = getQuanta(state);
        int remaining = flowVertically(level, pos, quanta);
        if (remaining <= 0) {
            return;
        }
        remaining = flowTowardsDrain(level, pos, remaining, random);
        if (remaining <= 0) {
            return;
        }
        if (remaining == 1 && remaining == quanta) {
            return;
        }
        if (remaining != quanta && remaining == 1) {
            setQuantaOrAir(level, pos, remaining);
            return;
        }
        int lowerThan = remaining - 1;
        BlockPos[] neighbours = new BlockPos[SIDES.length];
        int[] amounts = new int[SIDES.length];
        for (int index = 0; index < SIDES.length; index++) {
            neighbours[index] = pos.relative(SIDES[index]);
            amounts[index] = capacity(level, neighbours[index]) > 0
                ? Math.max(0, getQuantaValue(level, neighbours[index])) : -1;
            if (amounts[index] >= lowerThan) {
                amounts[index] = -1;
            }
        }
        int total = remaining;
        int count = 1;
        for (int amount : amounts) {
            if (amount >= 0) {
                count++;
                total += amount;
            }
        }
        if (count == 1) {
            if (remaining != quanta) {
                setQuantaOrAir(level, pos, remaining);
            }
            return;
        }
        int each = total / count;
        int remainder = total % count;
        for (int index = 0; index < amounts.length; index++) {
            if (amounts[index] < 0) {
                continue;
            }
            int newAmount = each;
            if (remainder == count || remainder > 1 && random.nextInt(count - remainder) != 0) {
                newAmount++;
                remainder--;
            }
            if (newAmount != amounts[index]) {
                setQuantaOrAir(level, neighbours[index], newAmount);
                level.scheduleTick(neighbours[index], this, TICK_RATE);
            }
            count--;
        }
        if (remainder > 0) {
            each++;
        }
        setQuantaOrAir(level, pos, each);
    }

    private int flowTowardsDrain(ServerLevel level, BlockPos pos, int amount, RandomSource random) {
        int[] distances = new int[SIDES.length];
        int[] capacities = new int[SIDES.length];
        int[] transfers = new int[SIDES.length];
        int shortest = Integer.MAX_VALUE;
        for (int index = 0; index < SIDES.length; index++) {
            BlockPos neighbor = pos.relative(SIDES[index]);
            capacities[index] = capacity(level, neighbor);
            distances[index] = Integer.MAX_VALUE;
            if (capacities[index] > 0) {
                Set<BlockPos> visited = new HashSet<>();
                visited.add(pos);
                distances[index] = distanceToDrain(level, neighbor, 1, visited);
                shortest = Math.min(shortest, distances[index]);
            }
        }
        if (shortest == Integer.MAX_VALUE) {
            return amount;
        }
        int remaining = amount;
        int start = random.nextInt(SIDES.length);
        boolean transferred;
        do {
            transferred = false;
            for (int offset = 0; offset < SIDES.length && remaining > 0; offset++) {
                int index = (start + offset) % SIDES.length;
                if (distances[index] == shortest && transfers[index] < capacities[index]) {
                    transfers[index]++;
                    remaining--;
                    transferred = true;
                }
            }
        } while (remaining > 0 && transferred);
        setQuantaOrAir(level, pos, remaining);
        for (int index = 0; index < SIDES.length; index++) {
            if (transfers[index] > 0) {
                BlockPos neighbor = pos.relative(SIDES[index]);
                int existing = Math.max(0, getQuantaValue(level, neighbor));
                setQuantaOrAir(level, neighbor, existing + transfers[index]);
                level.scheduleTick(neighbor, this, TICK_RATE);
            }
        }
        return remaining;
    }

    private int distanceToDrain(ServerLevel level, BlockPos pos, int distance, Set<BlockPos> visited) {
        if (!visited.add(pos)) {
            return Integer.MAX_VALUE;
        }
        int shortest = capacity(level, pos.below()) > 0 ? distance : Integer.MAX_VALUE;
        if (shortest == Integer.MAX_VALUE && distance < SLOPE_FIND_DISTANCE) {
            for (Direction side : SIDES) {
                BlockPos neighbor = pos.relative(side);
                if (!visited.contains(neighbor) && capacity(level, neighbor) > 0) {
                    shortest = Math.min(shortest, distanceToDrain(level, neighbor, distance + 1, visited));
                }
            }
        }
        visited.remove(pos);
        return shortest;
    }

    private int capacity(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) {
            return 0;
        }
        BlockState state = level.getBlockState(pos);
        return state.is(this) ? QUANTA_PER_BLOCK - getQuanta(state) : canDisplace(level, pos) ? QUANTA_PER_BLOCK : 0;
    }

    private int flowVertically(ServerLevel level, BlockPos pos, int amountToInput) {
        BlockPos below = pos.below();
        if (capacity(level, below) == 0) {
            return amountToInput;
        }
        int amountBelow = getQuantaValueBelow(level, below, QUANTA_PER_BLOCK);
        if (amountBelow >= 0) {
            int total = amountBelow + amountToInput;
            int down = Math.min(QUANTA_PER_BLOCK, total);
            setQuantaOrAir(level, below, down);
            level.scheduleTick(below, this, TICK_RATE);
            int remaining = total - down;
            setQuantaOrAir(level, pos, remaining);
            return remaining;
        }
        if (canDisplace(level, below)) {
            setQuantaOrAir(level, below, amountToInput);
            level.scheduleTick(below, this, TICK_RATE);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return 0;
        }
        return amountToInput;
    }

    private int getQuantaValueBelow(LevelAccessor level, BlockPos pos, int belowThis) {
        int amount = getQuantaValue(level, pos);
        return amount >= belowThis ? -1 : amount;
    }

    private int getQuantaValue(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return 0;
        }
        return state.is(this) ? getQuanta(state) : -1;
    }

    private int getQuanta(BlockState state) {
        int level = state.getValue(LEVEL);
        return level == 0 ? QUANTA_PER_BLOCK : Math.max(0, QUANTA_PER_BLOCK - level);
    }

    private BlockState withQuanta(int amount) {
        if (amount >= QUANTA_PER_BLOCK) {
            return defaultBlockState();
        }
        return defaultBlockState().setValue(LEVEL, QUANTA_PER_BLOCK - amount);
    }

    private void setQuantaOrAir(ServerLevel level, BlockPos pos, int amount) {
        BlockState previous = level.getBlockState(pos);
        if (amount > 0 && !previous.isAir() && !previous.is(this)) {
            Block.dropResources(previous, level, pos, level.getBlockEntity(pos));
        }
        level.setBlock(pos, amount <= 0 ? Blocks.AIR.defaultBlockState() : withQuanta(amount), Block.UPDATE_ALL);
    }

    private boolean canDisplace(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (state.is(this) || state.is(BlockTags.DOORS) || state.is(BlockTags.SIGNS) || state.is(Blocks.SUGAR_CANE)) {
            return false;
        }
        if (!state.getCollisionShape(level, pos).isEmpty() || state.getDestroySpeed(level, pos) < 0.0F || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL)) {
            return false;
        }
        return true;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level instanceof ServerLevel serverLevel && entity instanceof LivingEntity living) {
            int quanta = getQuanta(state);
            living.hurtServer(serverLevel, serverLevel.damageSources().source(ModDamageTypes.DISSOLVE), quanta);
        }
    }

}
