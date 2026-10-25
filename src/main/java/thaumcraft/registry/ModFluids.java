package thaumcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import thaumcraft.Thaumcraft;
import thaumcraft.block.LiquidDeathBlock;
import thaumcraft.block.PurifyingFluidBlock;
import thaumcraft.platform.FluidPhysics;

public final class ModFluids {
    private static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Thaumcraft.MODID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Thaumcraft.MODID);

    public static final DeferredHolder<FluidType, FluidType> PURIFYING_FLUID_TYPE = FLUID_TYPES.register(
        "purifying_fluid",
        () -> FluidPhysics.create(FluidType.Properties.create().density(1000).viscosity(1000).lightLevel(10).rarity(net.minecraft.world.item.Rarity.RARE))
    );
    public static final DeferredHolder<FluidType, FluidType> LIQUID_DEATH_TYPE = FLUID_TYPES.register(
        "liquid_death",
        () -> FluidPhysics.create(FluidType.Properties.create().density(1000).viscosity(1500).lightLevel(8).rarity(net.minecraft.world.item.Rarity.RARE))
    );

    public static final DeferredHolder<Fluid, PurifyingSource> PURIFYING_FLUID = FLUIDS.register(
        "purifying_fluid",
        () -> new PurifyingSource(purifyingProperties())
    );
    public static final DeferredHolder<Fluid, PurifyingFlowing> FLOWING_PURIFYING_FLUID = FLUIDS.register(
        "flowing_purifying_fluid",
        () -> new PurifyingFlowing(purifyingProperties())
    );
    public static final DeferredHolder<Fluid, LiquidDeathSource> LIQUID_DEATH = FLUIDS.register(
        "liquid_death",
        () -> new LiquidDeathSource(liquidDeathProperties())
    );
    public static final DeferredHolder<Fluid, LiquidDeathFlowing> FLOWING_LIQUID_DEATH = FLUIDS.register(
        "flowing_liquid_death",
        () -> new LiquidDeathFlowing(liquidDeathProperties())
    );

    private ModFluids() {
    }

    private static BaseFlowingFluid.Properties purifyingProperties() {
        return new BaseFlowingFluid.Properties(PURIFYING_FLUID_TYPE, PURIFYING_FLUID, FLOWING_PURIFYING_FLUID)
            .bucket(ModItems.PURIFYING_FLUID_BUCKET)
            .block(ModBlocks.PURIFYING_FLUID)
            .slopeFindDistance(4)
            .levelDecreasePerBlock(1)
            .tickRate(5);
    }

    private static BaseFlowingFluid.Properties liquidDeathProperties() {
        return new BaseFlowingFluid.Properties(LIQUID_DEATH_TYPE, LIQUID_DEATH, FLOWING_LIQUID_DEATH)
            .bucket(ModItems.LIQUID_DEATH_BUCKET)
            .block(ModBlocks.LIQUID_DEATH)
            .slopeFindDistance(4)
            .levelDecreasePerBlock(1)
            .tickRate(7);
    }

    public static final class PurifyingSource extends BaseFlowingFluid.Source {
        private PurifyingSource(Properties properties) {
            super(properties);
        }
    }

    public static final class PurifyingFlowing extends BaseFlowingFluid.Flowing {
        private PurifyingFlowing(Properties properties) {
            super(properties);
        }
    }

    private abstract static class LiquidDeathBase extends BaseFlowingFluid {
        private LiquidDeathBase(Properties properties) {
            super(properties);
        }

        @Override
        public void tick(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
            if (blockState.getBlock() instanceof LiquidDeathBlock) {
                blockState.tick(level, pos, level.getRandom());
            }
        }

        @Override
        public float getOwnHeight(net.minecraft.world.level.material.FluidState state) {
            return getAmount(state) / (float) LiquidDeathBlock.QUANTA_PER_BLOCK;
        }

        @Override
        protected BlockState createLegacyBlock(net.minecraft.world.level.material.FluidState state) {
            int level = isSource(state) ? 0 : LiquidDeathBlock.QUANTA_PER_BLOCK - getAmount(state);
            return ModBlocks.LIQUID_DEATH.get().defaultBlockState().setValue(LiquidBlock.LEVEL, level);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(FlowingFluid.LEVEL);
        }
    }

    public static final class LiquidDeathSource extends LiquidDeathBase {
        private LiquidDeathSource(Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(net.minecraft.world.level.material.FluidState state) {
            return 4;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static final class LiquidDeathFlowing extends LiquidDeathBase {
        private LiquidDeathFlowing(Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(FlowingFluid.LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }

    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
    }
}
