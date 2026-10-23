package thaumcraft.test;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import io.netty.channel.embedded.EmbeddedChannel;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.blockentity.ResearchTableBlockEntity;
import thaumcraft.eldritch.EldritchRingGenerator;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.golem.GolemSnapshot;
import thaumcraft.entity.golem.GolemType;
import thaumcraft.registry.EldritchContent;
import thaumcraft.platform.OuterLandsData;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModEntities;
import thaumcraft.research.ScanManager;
import thaumcraft.world.dim.CellLoc;
import thaumcraft.world.dim.MazeHandler;
import thaumcraft.world.dim.OuterLands;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ParityCoreGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final String[] TESTS = {"parity_golem_material_scans", "parity_brain_jar_bonus", "parity_rejected_ring_preserves_world", "parity_accepted_ring_reserves_queued_maze"};

    static {
        FUNCTIONS.register(TESTS[0], () -> ParityCoreGameTests::golemMaterialScans);
        FUNCTIONS.register(TESTS[1], () -> ParityCoreGameTests::brainJarBonus);
        FUNCTIONS.register(TESTS[2], () -> ParityCoreGameTests::rejectedRingPreservesWorld);
        FUNCTIONS.register(TESTS[3], () -> ParityCoreGameTests::acceptedRingReservesQueuedMaze);
    }

    private ParityCoreGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Thaumcraft.id("parity_core_default"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name));
            event.registerTest(Thaumcraft.id(name), new FunctionGameTestInstance(function, new TestData<>(environment, Thaumcraft.id("test/platform"), 200, 0, true)));
        }
    }

    private static void golemMaterialScans(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        thaumcraft.research.Knowledge.get(player).discoverAspect(thaumcraft.aspect.Aspect.ENERGY);
        thaumcraft.research.Knowledge.get(player).discoverAspect(thaumcraft.aspect.Aspect.VOID);
        GolemBase wooden = createGolem(helper, GolemType.WOOD);
        GolemBase thaumium = createGolem(helper, GolemType.THAUMIUM);
        ScanManager.Target woodenTarget = ScanManager.resolveEntity(wooden);
        ScanManager.Target thaumiumTarget = ScanManager.resolveEntity(thaumium);
        helper.assertTrue(woodenTarget != null && thaumiumTarget != null, "golem scan targets were not resolved");
        if (woodenTarget == null || thaumiumTarget == null) {
            return;
        }
        helper.assertFalse(woodenTarget.key().equals(thaumiumTarget.key()), "golem materials share a scan key");
        helper.assertTrue(ScanManager.completeScan(player, woodenTarget), "wooden golem scan did not complete");
        helper.assertTrue(ScanManager.hasBeenScanned(player, woodenTarget), "wooden golem scan was not recorded");
        helper.assertFalse(ScanManager.hasBeenScanned(player, thaumiumTarget), "wooden golem scan suppressed the thaumium scan");
        helper.assertTrue(ScanManager.completeScan(player, thaumiumTarget), "thaumium golem scan did not complete");
        helper.assertTrue(ScanManager.hasBeenScanned(player, thaumiumTarget), "thaumium golem scan was not recorded");
        helper.succeed();
    }

    private static GolemBase createGolem(GameTestHelper helper, GolemType type) {
        GolemBase golem = new GolemBase(ModEntities.GOLEM.get(), helper.getLevel());
        golem.configure(type, GolemSnapshot.EMPTY);
        return golem;
    }

    private static ServerPlayer mockServerPlayer(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.mockServerPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(player.getGameProfile(), false);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, cookie) {
            @Override
            public void send(Packet<?> packet) {
            }
        };
        player.connection.markClientLoaded();
        return player;
    }

    private static void brainJarBonus(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos tablePos = helper.absolutePos(new BlockPos(8, 4, 8));
        BlockPos jarPos = tablePos.west(8);
        level.setBlockAndUpdate(tablePos, ModBlocks.RESEARCH_TABLE.get().defaultBlockState());
        level.setBlockAndUpdate(jarPos, ModBlocks.BRAIN_JAR.get().defaultBlockState());
        ResearchTableBlockEntity table = (ResearchTableBlockEntity) level.getBlockEntity(tablePos);
        helper.assertTrue(table != null, "research table block entity was not created");
        if (table == null) {
            return;
        }
        RandomSource random = new FixedBonusRandom();
        for (int tick = 0; tick < 601; tick++) {
            ResearchTableBlockEntity.serverTick(level, tablePos, level.getBlockState(tablePos), table);
        }
        ResearchTableBlockEntity.serverTick(level, tablePos, level.getBlockState(tablePos), table, random);
        helper.assertTrue(table.getBonusAspects().size() > 0, "brain jar did not grant its 1-in-200 table bonus");
        helper.succeed();
    }

    private static void rejectedRingPreservesWorld(GameTestHelper helper) {
        var level = helper.getLevel();
        var outer = level.getServer().getLevel(OuterLands.OUTER_LANDS);
        helper.assertTrue(outer != null, "Outer Lands level is unavailable");
        if (outer == null) {
            return;
        }
        BlockPos center = helper.absolutePos(new BlockPos(8, 8, 8)).offset(16384, 0, 16384);
        int chunkX = center.getX() >> 4;
        int chunkZ = center.getZ() >> 4;
        CellLoc cell = new CellLoc(chunkX, chunkZ);
        OuterLandsData data = outer.getDataStorage().computeIfAbsent(OuterLandsData.TYPE);
        String mazeId = "ring_test_" + chunkX + "_" + chunkZ;
        String aliasId = chunkX + "," + chunkZ;
        data.putMaze(mazeId, new OuterLandsData.MazeState(91L, chunkX, chunkZ, 1, 1,
            List.of(new OuterLandsData.CellData(cell, (short) 1)), List.of()));
        data.putAlias(aliasId, mazeId);
        helper.assertTrue(data.canonicalId(aliasId).equals(mazeId) && data.maze(aliasId) == data.mazes().get(mazeId), "saved maze alias did not resolve to its canonical maze");
        var encoded = OuterLandsData.CODEC.encodeStart(JsonOps.INSTANCE, data).result().orElseThrow();
        OuterLandsData restored = OuterLandsData.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        helper.assertTrue(restored.canonicalId(aliasId).equals(mazeId) && restored.maze(aliasId).cells().contains(new OuterLandsData.CellData(cell, (short) 1)),
            "maze cells and aliases did not survive saved-data codec round trip");
        data.putMaze(mazeId, restored.maze(aliasId));
        data.putAlias(aliasId, restored.canonicalId(aliasId));
        MazeHandler.publishSnapshot(outer);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                level.setBlockAndUpdate(center.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y <= 8; y++) {
                    level.setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        BlockPos observed = center.offset(-3, 0, -2);
        Random random = new Random(13L);
        Random control = new Random(13L);
        control.nextInt(6);
        control.nextInt(6);
        helper.assertFalse(EldritchRingGenerator.generate(level, random, chunkX, chunkZ, center.getX(), center.getY(), center.getZ()),
            "ring generator accepted a candidate inside a saved maze range");
        helper.assertValueEqual(random.nextLong(), control.nextLong(), "saved-overlap ring rejection consumed decoration randomness");
        helper.assertTrue(level.getBlockState(observed).isAir(), "rejected ring modified an outer block");
        helper.assertTrue(level.getBlockState(center.below()).is(Blocks.STONE), "rejected ring modified the ground");
        data.putMaze(mazeId, new OuterLandsData.MazeState(91L, chunkX, chunkZ, 1, 1, List.of(), List.of()));
        MazeHandler.publishSnapshot(outer);
        prepareRingSite(level, center);
        helper.assertTrue(EldritchRingGenerator.generate(level, new Random(17L), chunkX, chunkZ, center.getX(), center.getY(), center.getZ()),
            "republished snapshot retained a maze cell after its saved state was cleared");
        helper.succeed();
    }

    private static void acceptedRingReservesQueuedMaze(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(8, 8, 8)).offset(32000, 0, 32000);
        int chunkX = center.getX() >> 4;
        int chunkZ = center.getZ() >> 4;
        var outer = level.getServer().getLevel(OuterLands.OUTER_LANDS);
        helper.assertTrue(outer != null, "Outer Lands level is unavailable");
        if (outer == null) {
            return;
        }
        helper.assertFalse(MazeHandler.mazesInRange(outer, chunkX, chunkZ, 21, 21), "isolated ring request origin overlaps a saved maze");
        prepareRingSite(level, center);
        assertRingSite(helper, level, center);
        helper.assertTrue(EldritchRingGenerator.generate(level, new Random(29L), chunkX + 1, chunkZ + 1, center.getX(), center.getY(), center.getZ()),
            "ring candidate with no nearby saved maze or pending request was rejected");
        helper.assertTrue(level.getBlockState(center.above()).is(EldritchContent.ELDRITCH_ALTAR.get()), "accepted ring did not place its altar");

        prepareRingSite(level, center);
        BlockPos observed = center.offset(-3, 0, -2);
        Random random = new Random(31L);
        Random control = new Random(31L);
        control.nextInt(6);
        control.nextInt(6);
        helper.assertFalse(EldritchRingGenerator.generate(level, random, chunkX, chunkZ, center.getX(), center.getY(), center.getZ()),
            "second ring overlapping the first queued maze request was accepted");
        helper.assertValueEqual(random.nextLong(), control.nextLong(), "queued-overlap ring rejection consumed decoration randomness");
        helper.assertTrue(level.getBlockState(observed).isAir(), "queued-overlap rejection modified the ring site");
        helper.assertTrue(level.getBlockState(center.below()).is(Blocks.STONE), "queued-overlap rejection modified the ground");
        helper.runAfterDelay(2, () -> {
            var state = outer.getDataStorage().computeIfAbsent(OuterLandsData.TYPE).maze(chunkX + "," + chunkZ);
            helper.assertTrue(state != null && !state.generationStarted() && state.placed().isEmpty(), "natural altar must reserve its actual chunk without building a maze");
            helper.succeed();
        });
    }

    private static void prepareRingSite(net.minecraft.world.level.WorldGenLevel level, BlockPos center) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                level.setBlock(center.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
                for (int y = 0; y <= 8; y++) {
                    level.setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void assertRingSite(GameTestHelper helper, net.minecraft.world.level.WorldGenLevel level, BlockPos center) {
        BlockPos[] groundPositions = {center.offset(-3, -1, -3), center.below(), center.offset(3, -1, 0), center.offset(3, -1, 3), center.offset(0, -1, 3)};
        BlockPos[] airPositions = {center.offset(-3, 0, -3), center, center.offset(3, 0, 0), center.offset(3, 0, 3), center.offset(0, 0, 3)};
        for (BlockPos pos : groundPositions) {
            helper.assertTrue(level.getBlockState(pos).is(Blocks.STONE), "ring candidate ground is not valid stone");
        }
        for (BlockPos pos : airPositions) {
            helper.assertTrue(level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()), "ring candidate does not have clear spawn space");
        }
    }

    private static final class FixedBonusRandom implements RandomSource {
        @Override
        public RandomSource fork() {
            return this;
        }

        @Override
        public PositionalRandomFactory forkPositional() {
            return new PositionalRandomFactory() {
                @Override
                public RandomSource fromHashOf(String name) {
                    return FixedBonusRandom.this;
                }

                @Override
                public RandomSource fromSeed(long seed) {
                    return FixedBonusRandom.this;
                }

                @Override
                public RandomSource at(int x, int y, int z) {
                    return FixedBonusRandom.this;
                }

                @Override
                public void parityConfigString(StringBuilder builder) {
                }
            };
        }

        @Override
        public void setSeed(long seed) {
        }

        @Override
        public int nextInt() {
            return 0;
        }

        @Override
        public int nextInt(int bound) {
            return bound == 200 ? 0 : bound - 1;
        }

        @Override
        public long nextLong() {
            return 0L;
        }

        @Override
        public boolean nextBoolean() {
            return false;
        }

        @Override
        public float nextFloat() {
            return 0.99F;
        }

        @Override
        public double nextDouble() {
            return 0.99;
        }

        @Override
        public double nextGaussian() {
            return 0.0;
        }
    }
}
