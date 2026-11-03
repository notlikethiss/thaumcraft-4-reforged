package thaumcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import thaumcraft.registry.ModEffects;
import thaumcraft.fx.Fx;
import thaumcraft.research.Warp;
import thaumcraft.research.WarpType;

public class PurifyingFluidBlock extends LiquidBlock {
    public PurifyingFluidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level instanceof ServerLevel serverLevel && state.getFluidState().isSource() && entity instanceof Player player && !player.hasEffect(ModEffects.WARP_WARD)) {
            int warp = Warp.get(player, WarpType.PERMANENT);
            int divisor = Math.max(1, (int) Math.sqrt(warp));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.WARP_WARD, Math.min(32000, 200000 / divisor), 0, true, true));
            serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Fx.get().purifyingFluid(level, pos, state.getValue(LEVEL), random);
        super.animateTick(state, level, pos, random);
    }
}
