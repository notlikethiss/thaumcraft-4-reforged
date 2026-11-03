package thaumcraft.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import thaumcraft.blockentity.TcBlockEntity;
import thaumcraft.registry.ModBlockEntities;

public class BellowsBlockEntity extends TcBlockEntity {
    private float inflation = 1.0F;
    private float prevInflation = 1.0F;
    private boolean inflating;
    private boolean firstRun = true;
    private boolean onVanillaFurnace;
    private int delay;

    public BellowsBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.BELLOWS.get(), pos, state);
    }

    protected BellowsBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public float getInflation() {
        return inflation;
    }

    public float getPrevInflation() {
        return prevInflation;
    }

    public boolean isOnVanillaFurnace() {
        return onVanillaFurnace;
    }

    public void detectVanillaFurnace(BlockState state) {
        if (level == null) {
            return;
        }
        Direction facing = state.getValue(BellowsBlock.FACING);
        onVanillaFurnace = level.getBlockState(worldPosition.relative(facing)).is(net.minecraft.world.level.block.Blocks.FURNACE);
        sync();
    }

    public static int getBellows(Level level, BlockPos pos, Direction[] directions) {
        int bellows = 0;
        for (Direction direction : directions) {
            BlockPos neighbour = pos.relative(direction);
            BlockState state = level.getBlockState(neighbour);
            if (state.getBlock() instanceof BellowsBlock
                && state.getValue(BellowsBlock.FACING) == direction.getOpposite()
                && level.getBlockEntity(neighbour) instanceof BellowsBlockEntity
                && !level.hasNeighborSignal(neighbour)) {
                bellows++;
            }
        }
        return bellows;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, BellowsBlockEntity bellows) {
        if (level.hasNeighborSignal(pos)) {
            return;
        }
        RandomSource random = level.getRandom();
        if (bellows.firstRun) {
            bellows.inflation = 0.35F + random.nextFloat() * 0.55F;
        }
        bellows.firstRun = false;
        bellows.prevInflation = bellows.inflation;
        if (bellows.inflation > 0.35F && !bellows.inflating) {
            bellows.inflation -= 0.075F;
        }
        if (bellows.inflation <= 0.35F && !bellows.inflating) {
            bellows.inflating = true;
        }
        if (bellows.inflation < 1.0F && bellows.inflating) {
            bellows.inflation += 0.025F;
        }
        if (bellows.inflation >= 1.0F && bellows.inflating) {
            bellows.inflating = false;
            level.playLocalSound(pos, SoundEvents.GHAST_SHOOT, SoundSource.BLOCKS, 0.01F, 0.5F + (random.nextFloat() - random.nextFloat()) * 0.2F, false);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BellowsBlockEntity bellows) {
        if (!bellows.onVanillaFurnace || level.hasNeighborSignal(pos)) {
            return;
        }
        bellows.delay++;
        if (bellows.delay < 2) {
            return;
        }
        bellows.delay = 0;
        BlockPos target = pos.relative(state.getValue(BellowsBlock.FACING));
        if (level.getBlockState(target).is(net.minecraft.world.level.block.Blocks.FURNACE) && level.getBlockEntity(target) instanceof AbstractFurnaceBlockEntity furnace) {
            CompoundTag tag = furnace.saveCustomOnly(level.registryAccess());
            int spent = tag.getIntOr("cooking_time_spent", 0);
            if (spent > 0 && spent < 199) {
                tag.putInt("cooking_time_spent", spent + 1);
                ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag);
                furnace.loadCustomOnly(input);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        onVanillaFurnace = input.getBooleanOr("onVanillaFurnace", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("onVanillaFurnace", onVanillaFurnace);
    }
}
