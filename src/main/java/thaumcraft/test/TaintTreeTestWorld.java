package thaumcraft.test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import thaumcraft.taint.TaintBiomes;

final class TaintTreeTestWorld {
    final Map<BlockPos, BlockState> blocks = new HashMap<>();
    final WorldGenLevel level;
    private final Block ground;
    private final boolean water;

    @SuppressWarnings("unchecked")
    TaintTreeTestWorld(GameTestHelper helper, Block ground, boolean water) {
        this.ground = ground;
        this.water = water;
        var registries = helper.getLevel().registryAccess();
        var biome = registries.lookupOrThrow(Registries.BIOME).getOrThrow(TaintBiomes.TAINTED_LAND);
        var random = RandomSource.create(17L);
        level = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "registryAccess" -> registries;
                case "getBiome" -> biome;
                case "getMinY", "getMinBuildHeight" -> 0;
                case "getMaxY", "getMaxBuildHeight" -> 256;
                case "getHeight" -> {
                    if (args == null || args.length == 0) yield 256;
                    var type = (Heightmap.Types) args[0];
                    int x = (int) args[1];
                    int z = (int) args[2];
                    int y = 255;
                    while (y >= 0 && !type.isOpaque().test(state(new BlockPos(x, y, z)))) y--;
                    yield y + 1;
                }
                case "isOutsideBuildHeight" -> {
                    int y = args[0] instanceof BlockPos pos ? pos.getY() : (int) args[0];
                    yield y < 0 || y >= 256;
                }
                case "getBlockState" -> state((BlockPos) args[0]);
                case "getFluidState" -> state((BlockPos) args[0]).getFluidState();
                case "isEmptyBlock" -> state((BlockPos) args[0]).isAir();
                case "isStateAtPosition" -> ((Predicate<BlockState>) args[1]).test(state((BlockPos) args[0]));
                case "isFluidAtPosition" -> ((Predicate<FluidState>) args[1]).test(state((BlockPos) args[0]).getFluidState());
                case "setBlock" -> {
                    blocks.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                    yield true;
                }
                case "ensureCanWrite" -> true;
                case "getRandom" -> random;
                case "scheduleTick" -> null;
                case "isClientSide" -> false;
                default -> throw new UnsupportedOperationException(method.toString());
            };
        });
    }

    BlockState state(BlockPos pos) {
        return blocks.getOrDefault(pos, (pos.getY() < 64 ? ground : water && pos.getY() < 67 ? Blocks.WATER : Blocks.AIR).defaultBlockState());
    }

    long logs() {
        return blocks.values().stream().filter(state -> state.is(Blocks.OAK_LOG)).count();
    }

    void assertWaterUnchanged(GameTestHelper helper) {
        helper.assertValueEqual(logs(), 0L, "tainted trees generated above water");
        helper.assertTrue(blocks.values().stream().noneMatch(state -> state.is(Blocks.OAK_LEAVES)), "tainted tree leaves generated above water");
        for (var entry : blocks.entrySet()) {
            int y = entry.getKey().getY();
            if (y >= 64 && y < 67) helper.assertTrue(entry.getValue().is(Blocks.WATER), "tree replaced water at " + entry.getKey());
        }
    }
}
