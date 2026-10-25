package thaumcraft.test;

import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.function.Consumer;
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
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.eldritch.EldritchBlockEntity;
import thaumcraft.eldritch.EldritchPortalTravel;
import thaumcraft.node.NodeBlockEntity;
import thaumcraft.node.NodeType;
import thaumcraft.registry.EldritchContent;
import thaumcraft.registry.ModBlocks;
import thaumcraft.research.ResearchManager;
import thaumcraft.wand.Wands;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.WandRegistry;
import thaumcraft.wand.WandTriggers;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class EldritchGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final List<String> TESTS = List.of("eldritch_eye_consumption_and_state", "eldritch_lock_key_is_consumed_once", "primordial_pearl_is_consumed_at_a_node", "eldritch_altar_vis_activation");

    static {
        FUNCTIONS.register(TESTS.get(0), () -> EldritchGameTests::eyeConsumptionAndState);
        FUNCTIONS.register(TESTS.get(1), () -> EldritchGameTests::lockKeyConsumedOnce);
        FUNCTIONS.register(TESTS.get(2), () -> EldritchGameTests::pearlUseAtNode);
        FUNCTIONS.register(TESTS.get(3), () -> EldritchGameTests::altarVisActivation);
    }

    private EldritchGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(Thaumcraft.id("eldritch_default"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name));
            int timeout = name.equals("eldritch_altar_vis_activation") ? 1200 : 100;
            event.registerTest(Thaumcraft.id(name), new FunctionGameTestInstance(function, new TestData<>(environment, Thaumcraft.id("test/platform"), timeout, 0, true)));
        }
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.mockServerPlayer(helper, GameType.SURVIVAL);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(player.getGameProfile(), false);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, cookie) {
            @Override
            public void send(Packet<?> packet) {
            }
        };
        helper.getLevel().addNewPlayer(player);
        player.connection.markClientLoaded();
        return player;
    }

    private static void eyeConsumptionAndState(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, EldritchContent.ELDRITCH_ALTAR.get().defaultBlockState());
        ServerPlayer player = mockPlayer(helper);
        player.absSnapTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0.0F, 0.0F);
        ItemStack eyes = new ItemStack(EldritchContent.ELDRITCH_EYE.get(), 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, eyes);
        for (int index = 0; index < 4; index++) {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
            helper.getLevel().getBlockState(pos).useItemOn(eyes, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        }
        EldritchBlockEntity altar = (EldritchBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertValueEqual(altar.eyeCount(), 4, "altar should accept exactly four eyes");
        helper.assertTrue(eyes.isEmpty(), "each altar eye insertion should consume one item");
        net.minecraft.nbt.CompoundTag saved = altar.saveWithoutMetadata(helper.getLevel().registryAccess());
        EldritchBlockEntity loaded = new EldritchBlockEntity(pos, helper.getLevel().getBlockState(pos));
        loaded.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertValueEqual(loaded.eyeCount(), 4, "eye count should persist");
        helper.succeed();
    }

    private static void lockKeyConsumedOnce(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, EldritchContent.ANCIENT_LOCKING_MECHANISM.get().defaultBlockState());
        ServerPlayer player = mockPlayer(helper);
        player.absSnapTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0.0F, 0.0F);
        ItemStack key = new ItemStack(EldritchContent.RUNED_TABLET.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, key);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
        helper.getLevel().getBlockState(pos).useItemOn(key, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(key.isEmpty(), "activating the lock should consume one tablet");
        EldritchBlockEntity lock = (EldritchBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(!lock.trigger(), "a triggered lock must reject a second trigger");
        net.minecraft.nbt.CompoundTag saved = lock.saveWithoutMetadata(helper.getLevel().registryAccess());
        EldritchBlockEntity loaded = new EldritchBlockEntity(pos, helper.getLevel().getBlockState(pos));
        loaded.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(!loaded.trigger(), "triggered lock state should persist across reload");
        helper.succeed();
    }

    private static void pearlUseAtNode(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.AURA_NODE.get().defaultBlockState());
        NodeBlockEntity node = (NodeBlockEntity) helper.getLevel().getBlockEntity(pos);
        node.setNodeType(NodeType.DARK);
        AspectList aspects = new AspectList().add(Aspect.AIR, 6).add(Aspect.FIRE, 6).add(Aspect.EARTH, 6).add(Aspect.WATER, 6).add(Aspect.ORDER, 6).add(Aspect.ENTROPY, 6);
        node.setAspects(aspects);
        ServerPlayer player = mockPlayer(helper);
        player.absSnapTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0.0F, 0.0F);
        ItemStack pearl = new ItemStack(EldritchContent.PRIMORDIAL_PEARL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, pearl);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
        EldritchContent.PRIMORDIAL_PEARL.get().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(pearl.isEmpty(), "pearl should be consumed when used on a node");
        helper.succeed();
    }

    private static void altarVisActivation(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(4, 4, 4)).offset(64000, 0, 64000);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                level.setBlockAndUpdate(center.offset(x, -1, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                for (int y = 0; y <= 8; y++) {
                    level.setBlockAndUpdate(center.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
            }
        }
        helper.assertTrue(thaumcraft.world.gen.NodeHooks.eldritchRing(level, new java.util.Random(37L), center.getX() >> 4, center.getZ() >> 4, center.getX(), center.getY(), center.getZ()), "natural obelisk generation failed");
        BlockPos pos = center.above();
        EldritchBlockEntity altar = (EldritchBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(level.getBlockEntity(pos.above()) instanceof NodeBlockEntity node && node.getNodeType() == NodeType.DARK, "natural obelisk has no dark node");
        helper.assertTrue(helper.getLevel().getServer().getLevel(thaumcraft.world.dim.OuterLands.OUTER_LANDS) != null, "test preset must include Outer Lands");
        ServerPlayer player = mockPlayer(helper);
        player.absSnapTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0.0F, 0.0F);
        player.setNoGravity(true);
        player.getAbilities().invulnerable = true;
        ItemStack eyes = new ItemStack(EldritchContent.ELDRITCH_EYE.get(), 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, eyes);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
        for (int index = 0; index < 4; index++) {
            player.gameMode.useItemOn(player, level, eyes, InteractionHand.MAIN_HAND, hit);
        }
        helper.assertTrue(eyes.isEmpty() && altar.eyeCount() == 4, "natural altar did not accept all four eyes");
        ItemStack wand = Wands.create(WandRegistry.CAP_SILVER, WandRegistry.ROD_SILVERWOOD, false);
        Wands.fill(wand);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        player.gameMode.useItemOn(player, level, wand, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(!altar.isOpen() && level.getBlockState(pos.above()).is(ModBlocks.AURA_NODE.get()), "altar activated without OCULUS research");
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            helper.assertValueEqual(Wands.getVis(wand, aspect), Wands.getMaxVis(wand), "rejected activation consumed vis");
        }
        ResearchManager.complete(player, "OCULUS");
        var outer = level.getServer().getLevel(thaumcraft.world.dim.OuterLands.OUTER_LANDS);
        helper.assertTrue(!thaumcraft.world.dim.MazeHandler.isReady(outer, pos.getX() >> 4, pos.getZ() >> 4), "maze rooms should still be generating");
        helper.startSequence().thenExecute(() -> {
            EldritchBlockEntity currentAltar = (EldritchBlockEntity) level.getBlockEntity(pos);
            Wands.storeVis(wand, Aspect.AIR, 0);
            player.gameMode.useItemOn(player, level, wand, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(!currentAltar.isOpen() && level.getBlockState(pos.above()).is(ModBlocks.AURA_NODE.get()), "altar activated without enough vis");
            for (Aspect aspect : Aspect.getPrimalAspects()) {
                helper.assertValueEqual(Wands.getVis(wand, aspect), aspect == Aspect.AIR ? 0 : Wands.getMaxVis(wand), "failed activation partially consumed vis");
            }
            Wands.fill(wand);
            player.gameMode.useItemOn(player, level, wand, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(currentAltar.isOpen(), "four-eyed altar did not activate: block=" + level.getBlockState(pos) + ", node=" + level.getBlockEntity(pos.above()) + ", eyes=" + currentAltar.eyeCount() + ", research=" + ResearchManager.isComplete(player, "OCULUS") + ", held=" + player.getMainHandItem() + ", alive=" + player.isAlive() + ", trigger=" + WandTriggers.hasTrigger(level.getBlockState(pos)));
            helper.assertTrue(helper.getLevel().getBlockState(pos.above()).is(EldritchContent.ELDRITCH_PORTAL.get()), "altar activation should replace the dark node with a portal");
            for (Aspect aspect : Aspect.getPrimalAspects()) {
                int cost = (int)(10000 * ((WandItem) wand.getItem()).getConsumptionModifier(wand, player, aspect, true));
                helper.assertValueEqual(Wands.getVis(wand, aspect), 10000 - cost, "altar activation should consume 100 vis of each primal aspect");
            }
            helper.assertTrue(level.getBlockEntity(pos) == currentAltar, "activation destroyed the altar");
            for (int y = 2; y <= 6; y++) {
                helper.assertTrue(level.getBlockState(pos.above(y)).is(EldritchContent.ELDRITCH_OBELISK.get()), "activation destroyed the obelisk");
            }
            AspectList remaining = Wands.getAllVis(wand).copy();
            player.gameMode.useItemOn(player, level, wand, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(remaining.equals(Wands.getAllVis(wand)), "repeated activation consumed vis again");
            helper.assertTrue(EldritchPortalTravel.tryTravel(helper.getLevel(), pos.above(), player), "ready portal should transfer the player");
            helper.assertTrue(player.level().dimension().equals(thaumcraft.world.dim.OuterLands.OUTER_LANDS), "portal should enter the Outer Lands");
            BlockPos entrance = thaumcraft.world.dim.MazeHandler.entrancePos(outer, pos.above());
            var maze = outer.getDataStorage().computeIfAbsent(thaumcraft.platform.OuterLandsData.TYPE).maze((pos.getX() >> 4) + "," + (pos.getZ() >> 4));
            helper.assertTrue(maze.placed().contains(new thaumcraft.world.dim.CellLoc(entrance.getX() >> 4, entrance.getZ() >> 4)), "portal entry should build the entrance room");
            helper.assertTrue(ResearchManager.isComplete(player, "ENTEROUTER"), "first portal entry should complete ENTEROUTER research");
            helper.assertValueEqual(player.getPortalCooldown(), 100, "portal transfer should set its cooldown");
            helper.assertTrue(!EldritchPortalTravel.tryTravel(player.level(), player.blockPosition(), player), "portal cooldown should prevent an immediate second transfer");
        }).thenSucceed();
    }
}
