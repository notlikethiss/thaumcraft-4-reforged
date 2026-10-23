package thaumcraft.world.biome;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.taint.TaintBiomes;

public class TaintDecorationFeature extends Feature<NoneFeatureConfiguration> {
    public TaintDecorationFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        if (level.getBiome(origin).is(TaintBiomes.TAINTED_LAND)) {
            TaintDecorationHook.decorate(level, context.random(), origin, context.chunkGenerator());
        }
        return true;
    }
}
