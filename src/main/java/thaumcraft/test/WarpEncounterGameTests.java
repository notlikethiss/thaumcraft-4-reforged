package thaumcraft.test;

import java.util.List;
import java.util.function.Consumer;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.entity.monster.eldritch.EldritchGuardian;
import thaumcraft.entity.monster.eldritch.MindSpider;
import thaumcraft.registry.ModEntities;
import thaumcraft.research.WarpEncounterHooks;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class WarpEncounterGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final String[] TESTS = {"warp_guardian_spawner", "warp_mind_spider_spawner"};

    static {
        FUNCTIONS.register(TESTS[0], () -> WarpEncounterGameTests::guardianSpawner);
        FUNCTIONS.register(TESTS[1], () -> WarpEncounterGameTests::mindSpiderSpawner);
    }

    private WarpEncounterGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(Thaumcraft.id("warp_encounters"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name));
            event.registerTest(Thaumcraft.id(name), new FunctionGameTestInstance(function, new TestData<>(environment, Thaumcraft.id("test/platform"), 100, 0, true)));
        }
    }

    private static void guardianSpawner(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        positionPlayer(helper, player);
        clearPlatform(helper);
        BlockPos origin = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        long seed = 109L;
        BlockPos blockedPos = candidate(origin, seed);
        helper.getLevel().getRandom().setSeed(seed);
        WarpEncounterHooks.spawnGuardians(helper.getLevel(), player, 1);
        helper.assertTrue(entitiesNear(helper, ModEntities.ELDRITCH_GUARDIAN.get(), blockedPos).isEmpty(), "guardian spawned without a sturdy support block");

        long validSeed = findSeed(origin, helper.getLevel().getMaxY());
        BlockPos validPos = candidate(origin, validSeed);
        helper.getLevel().setBlockAndUpdate(validPos.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().getRandom().setSeed(validSeed);
        WarpEncounterHooks.spawnGuardians(helper.getLevel(), player, 1);
        List<EldritchGuardian> guardians = entitiesNear(helper, ModEntities.ELDRITCH_GUARDIAN.get(), validPos);
        helper.assertValueEqual(guardians.size(), 1, "installed encounter spawner did not create one guardian at the valid candidate");
        helper.assertTrue(guardians.getFirst().getTarget() == player, "guardian did not target the triggering player");
        helper.succeed();
    }

    private static void mindSpiderSpawner(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        positionPlayer(helper, player);
        clearPlatform(helper);
        BlockPos origin = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        long blockedSeed = 109L;
        BlockPos blockedPos = candidate(origin, blockedSeed);
        helper.getLevel().getRandom().setSeed(blockedSeed);
        WarpEncounterHooks.spawnMindSpiders(helper.getLevel(), player, true, 1);
        helper.assertTrue(entitiesNear(helper, ModEntities.MIND_SPIDER.get(), blockedPos).isEmpty(), "mind spider spawned without a sturdy support block");

        long harmlessSeed = findSeed(origin, helper.getLevel().getMaxY());
        BlockPos harmlessPos = candidate(origin, harmlessSeed);
        helper.getLevel().setBlockAndUpdate(harmlessPos.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().getRandom().setSeed(harmlessSeed);
        WarpEncounterHooks.spawnMindSpiders(helper.getLevel(), player, true, 1);
        List<MindSpider> harmlessSpiders = entitiesNear(helper, ModEntities.MIND_SPIDER.get(), harmlessPos);
        helper.assertValueEqual(harmlessSpiders.size(), 1, "installed encounter spawner did not create one harmless spider at the valid candidate");
        MindSpider harmless = harmlessSpiders.getFirst();
        helper.assertTrue(harmless.isHarmless(), "harmless encounter flag was not applied");
        helper.assertValueEqual(harmless.getViewer(), player.getUUID().toString(), "harmless spider viewer UUID was not assigned");
        helper.assertTrue(harmless.getTarget() == player, "harmless spider did not retain its encounter target");
        MindSpider harmlessReloaded = reload(helper, harmless);
        helper.assertTrue(harmlessReloaded.isHarmless(), "harmless flag was not persisted");
        helper.assertValueEqual(harmlessReloaded.getViewer(), player.getUUID().toString(), "viewer UUID was not persisted");

        long hostileSeed = findSeed(origin, helper.getLevel().getMaxY(), harmlessSeed + 1L);
        BlockPos hostilePos = candidate(origin, hostileSeed);
        helper.getLevel().setBlockAndUpdate(hostilePos.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().getRandom().setSeed(hostileSeed);
        WarpEncounterHooks.spawnMindSpiders(helper.getLevel(), player, false, 1);
        List<MindSpider> hostileSpiders = entitiesNear(helper, ModEntities.MIND_SPIDER.get(), hostilePos);
        helper.assertValueEqual(hostileSpiders.size(), 1, "installed encounter spawner did not create one hostile spider at the valid candidate");
        MindSpider hostile = hostileSpiders.getFirst();
        helper.assertFalse(hostile.isHarmless(), "hostile encounter was marked harmless");
        helper.assertTrue(hostile.getViewer().isEmpty(), "hostile spider retained a harmless viewer UUID");
        helper.assertTrue(hostile.getTarget() == player, "hostile spider did not target the triggering player");
        MindSpider hostileReloaded = reload(helper, hostile);
        helper.assertFalse(hostileReloaded.isHarmless(), "hostile flag was not persisted");
        helper.assertTrue(hostileReloaded.getViewer().isEmpty(), "hostile viewer state was not persisted");
        helper.succeed();
    }

    private static ServerPlayer mockServerPlayer(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.mockServerPlayer(helper, GameType.SURVIVAL);
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

    private static void positionPlayer(GameTestHelper helper, ServerPlayer player) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        player.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
    }

    private static long findSeed(BlockPos origin, int maxBuildHeight) {
        return findSeed(origin, maxBuildHeight, 0L);
    }

    private static long findSeed(BlockPos origin, int maxBuildHeight, long startSeed) {
        for (long seed = startSeed; seed < startSeed + 10000L; seed++) {
            BlockPos pos = candidate(origin, seed);
            if (Math.abs(pos.getX() - origin.getX()) <= 12
                && pos.getX() != origin.getX()
                && Math.abs(pos.getY() - origin.getY()) <= 12
                && pos.getY() != origin.getY()
                && Math.abs(pos.getZ() - origin.getZ()) <= 12
                && pos.getZ() != origin.getZ()
                && pos.getY() > 1
                && pos.getY() < maxBuildHeight - 2) {
                return seed;
            }
        }
        throw new IllegalStateException("No in-range encounter candidate seed was found");
    }

    private static void clearPlatform(GameTestHelper helper) {
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                helper.getLevel().setBlockAndUpdate(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static BlockPos candidate(BlockPos origin, long seed) {
        RandomSource random = RandomSource.create(seed);
        return origin.offset(offset(random), offset(random), offset(random));
    }

    private static int offset(RandomSource random) {
        return (random.nextInt(18) + 7) * (random.nextInt(3) - 1);
    }

    private static <T extends Entity> List<T> entitiesNear(GameTestHelper helper, net.minecraft.world.entity.EntityType<T> type, BlockPos pos) {
        return helper.getLevel().getEntities(type, new AABB(pos).inflate(1.0), Entity::isAlive);
    }

    private static MindSpider reload(GameTestHelper helper, MindSpider spider) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        spider.saveWithoutId(output);
        MindSpider loaded = ModEntities.MIND_SPIDER.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(loaded != null, "mind spider reload factory returned null");
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        return loaded;
    }
}
