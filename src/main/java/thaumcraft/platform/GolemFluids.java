package thaumcraft.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;

public final class GolemFluids {
    private record DrainRule(java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> matches, Drainable drainable) { }
    private static final java.util.List<DrainRule> DRAINABLE = new java.util.ArrayList<>();
    @FunctionalInterface
    public interface Drainable {
        FluidStack drain(Level level, BlockPos pos, boolean simulate);
    }
    public static void registerDrainable(java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> matches, Drainable drainable) {
        DRAINABLE.add(new DrainRule(matches, drainable));
    }
    private GolemFluids() { }

    public static FluidStack contained(ItemStack stack) {
        if (stack.isEmpty()) return FluidStack.EMPTY;
        return net.neoforged.neoforge.transfer.fluid.FluidUtil.getFirstStackContained(stack);
    }

    public static FluidStack pickup(Level level, BlockPos pos, boolean simulate) {
        if (!level.hasChunkAt(pos)) return FluidStack.EMPTY;
        var state = level.getBlockState(pos);
        for (DrainRule rule : DRAINABLE) if (rule.matches().test(state)) return rule.drainable().drain(level, pos, simulate);
        var fluid = state.getFluidState();
        if (!fluid.isSource() || !(state.getBlock() instanceof BucketPickup pickup)) return FluidStack.EMPTY;
        FluidStack result = new FluidStack(fluid.getType(), 1000);
        if (simulate) return result;
        return pickup.pickupBlock(null, level, pos, state).isEmpty() ? FluidStack.EMPTY : result;
    }

    public static int place(Level level, BlockPos pos, FluidStack fluid, boolean simulate) {
        if (fluid.getAmount() < 1000 || !level.hasChunkAt(pos) || !level.getBlockState(pos).canBeReplaced() || !(fluid.getFluid() instanceof FlowingFluid flowing)) return 0;
        if (!simulate) {
            if (flowing.is(net.minecraft.tags.FluidTags.WATER) && level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES, pos)) return 0;
            if (!level.setBlock(pos, flowing.getSource(false).createLegacyBlock(), 3)) return 0;
        }
        return 1000;
    }
}
