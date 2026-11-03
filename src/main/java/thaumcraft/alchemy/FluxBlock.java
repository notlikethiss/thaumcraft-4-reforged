package thaumcraft.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class FluxBlock extends Block {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 8);
    public static final int QUANTA_PER_BLOCK = 8;

    private final int density;
    private final int densityDirection;
    private final int tickRate;

    protected FluxBlock(int density, int densityDirection, int tickRate, Properties properties) {
        super(properties);
        this.density = density;
        this.densityDirection = densityDirection;
        this.tickRate = tickRate;
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    public int getTickRate() {
        return tickRate;
    }

    public BlockState withLevel(int quanta) {
        return defaultBlockState().setValue(LEVEL, quanta);
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
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return state.getValue(LEVEL) <= 2;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return state.getValue(LEVEL) <= 2;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, tickRate);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        level.scheduleTick(pos, this, tickRate);
    }

    protected int getQuantaValue(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return 0;
        }
        if (!state.is(this)) {
            return -1;
        }
        return state.getValue(LEVEL);
    }

    protected int getQuantaValueBelow(LevelAccessor level, BlockPos pos, int belowThis) {
        int quanta = getQuantaValue(level, pos);
        return quanta >= belowThis ? -1 : quanta;
    }

    private int getDensityAt(LevelAccessor level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof FluxBlock block ? block.density : Integer.MAX_VALUE;
    }

    private boolean displaceIfPossible(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (state.is(this)) {
            return false;
        }
        if (state.is(BlockTags.DOORS) || state.is(BlockTags.SIGNS) || state.is(Blocks.SUGAR_CANE)) {
            return false;
        }
        if (!state.getCollisionShape(level, pos).isEmpty() || state.getDestroySpeed(level, pos) < 0.0F || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL)) {
            return false;
        }
        int otherDensity = getDensityAt(level, pos);
        if (otherDensity == Integer.MAX_VALUE) {
            Block.dropResources(state, level, pos);
            return true;
        }
        return density > otherDensity;
    }

    private void setLevelOrAir(ServerLevel level, BlockPos pos, int quanta, int flags) {
        if (quanta <= 0) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        } else {
            level.setBlock(pos, withLevel(quanta), flags);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean changed = false;
        int quantaRemaining = state.getValue(LEVEL);
        int previousRemaining = quantaRemaining;
        quantaRemaining = tryToFlowVerticallyInto(level, pos, quantaRemaining);
        if (quantaRemaining < 1) {
            return;
        } else if (quantaRemaining != previousRemaining) {
            changed = true;
            if (quantaRemaining == 1) {
                level.setBlock(pos, withLevel(quantaRemaining), Block.UPDATE_CLIENTS);
                return;
            }
        } else if (quantaRemaining == 1) {
            return;
        }
        int lowerThan = quantaRemaining - 1;
        Direction[] sides = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        BlockPos[] neighbours = new BlockPos[4];
        int[] values = new int[4];
        for (int index = 0; index < 4; index++) {
            neighbours[index] = pos.relative(sides[index]);
            if (displaceIfPossible(level, neighbours[index])) {
                level.setBlock(neighbours[index], Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        for (int index = 0; index < 4; index++) {
            values[index] = getQuantaValueBelow(level, neighbours[index], lowerThan);
        }
        int total = quantaRemaining;
        int count = 1;
        for (int value : values) {
            if (value >= 0) {
                count++;
                total += value;
            }
        }
        if (count == 1) {
            if (changed) {
                level.setBlock(pos, withLevel(quantaRemaining), Block.UPDATE_CLIENTS);
            }
            return;
        }
        int each = total / count;
        int remainder = total % count;
        for (int index = 0; index < 4; index++) {
            if (values[index] < 0) {
                continue;
            }
            int newValue = each;
            if (remainder == count || remainder > 1 && random.nextInt(count - remainder) != 0) {
                newValue++;
                remainder--;
            }
            if (newValue != values[index]) {
                setLevelOrAir(level, neighbours[index], newValue, Block.UPDATE_CLIENTS);
                level.scheduleTick(neighbours[index], this, tickRate);
            }
            count--;
        }
        if (remainder > 0) {
            each++;
        }
        level.setBlock(pos, withLevel(each), Block.UPDATE_CLIENTS);
    }

    private int tryToFlowVerticallyInto(ServerLevel level, BlockPos pos, int amountToInput) {
        BlockPos other = pos.offset(0, densityDirection, 0);
        if (level.isOutsideBuildHeight(other)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return 0;
        }
        int amount = getQuantaValueBelow(level, other, QUANTA_PER_BLOCK);
        if (amount >= 0) {
            amount += amountToInput;
            if (amount > QUANTA_PER_BLOCK) {
                level.setBlock(other, withLevel(QUANTA_PER_BLOCK), Block.UPDATE_ALL);
                level.scheduleTick(other, this, tickRate);
                return amount - QUANTA_PER_BLOCK;
            } else if (amount > 0) {
                level.setBlock(other, withLevel(amount), Block.UPDATE_ALL);
                level.scheduleTick(other, this, tickRate);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                return 0;
            }
            return amountToInput;
        }
        int otherDensity = getDensityAt(level, other);
        if (otherDensity == Integer.MAX_VALUE) {
            if (displaceIfPossible(level, other)) {
                level.setBlock(other, withLevel(amountToInput), Block.UPDATE_ALL);
                level.scheduleTick(other, this, tickRate);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                return 0;
            }
            return amountToInput;
        }
        boolean swap = densityDirection < 0 ? otherDensity < density : otherDensity > density;
        if (swap) {
            BlockState otherState = level.getBlockState(other);
            FluxBlock otherBlock = (FluxBlock) otherState.getBlock();
            level.setBlock(other, withLevel(amountToInput), Block.UPDATE_ALL);
            level.setBlock(pos, otherState, Block.UPDATE_ALL);
            level.scheduleTick(other, this, tickRate);
            level.scheduleTick(pos, otherBlock, otherBlock.tickRate);
            return 0;
        }
        return amountToInput;
    }
}
