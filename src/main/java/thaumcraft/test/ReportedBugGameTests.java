package thaumcraft.test;

import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.crafting.InfusionCrafting;
import thaumcraft.eldritch.EldritchRingGenerator;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.golem.GolemKeys;
import thaumcraft.entity.golem.Marker;
import thaumcraft.infusion.PedestalBlockEntity;
import thaumcraft.network.BellResetPayload;
import thaumcraft.node.Node;
import thaumcraft.node.NodeModifier;
import thaumcraft.node.NodeType;
import thaumcraft.registry.EldritchContent;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModEntities;
import thaumcraft.registry.ModItems;
import thaumcraft.wand.WandRegistry;
import thaumcraft.wand.Wands;
import thaumcraft.wand.focus.BlockSwapper;
import thaumcraft.wand.focus.FocusUtils;
import thaumcraft.wand.focus.TradeFocus;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ReportedBugGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final List<String> TESTS = List.of("trade_focus_rejects_double_plant", "trade_focus_clears_on_sneak_air", "trade_focus_keeps_unplaceable_item", "bell_reset_clears_markers", "golem_placed_on_pedestal", "ring_accepts_short_grass", "guardian_spawn_light_limit", "node_place_replaces_plant", "sanity_checker_infusion");

    static {
        FUNCTIONS.register(TESTS.get(0), () -> ReportedBugGameTests::rejectsDoublePlant);
        FUNCTIONS.register(TESTS.get(1), () -> ReportedBugGameTests::clearsOnSneakAir);
        FUNCTIONS.register(TESTS.get(2), () -> ReportedBugGameTests::keepsUnplaceableItem);
        FUNCTIONS.register(TESTS.get(3), () -> ReportedBugGameTests::bellReset);
        FUNCTIONS.register(TESTS.get(4), () -> ReportedBugGameTests::golemOnPedestal);
        FUNCTIONS.register(TESTS.get(5), () -> ReportedBugGameTests::ringOnShortGrass);
        FUNCTIONS.register(TESTS.get(6), () -> ReportedBugGameTests::guardianLight);
        FUNCTIONS.register(TESTS.get(7), () -> ReportedBugGameTests::nodePlace);
        FUNCTIONS.register(TESTS.get(8), () -> ReportedBugGameTests::sanityChecker);
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(Thaumcraft.id("reported_bugs_default"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name));
            event.registerTest(Thaumcraft.id(name), new FunctionGameTestInstance(function, new TestData<>(environment, Thaumcraft.id("test/platform"), 200, 0, true)));
        }
    }

    private static ServerPlayer player(GameTestHelper helper, GameType gameType) {
        ServerPlayer player = TestPlayers.mockServerPlayer(helper, gameType);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override
            public void send(Packet<?> packet) {
            }
        };
        helper.getLevel().addNewPlayer(player);
        player.connection.markClientLoaded();
        return player;
    }

    private static ItemStack tradeWand() {
        ItemStack wand = Wands.create(WandRegistry.CAP_IRON, WandRegistry.ROD_WOOD, false);
        Wands.setFocus(wand, new ItemStack(ModItems.FOCUS_EQUAL_TRADE.get()));
        Wands.fill(wand);
        return wand;
    }

    private static void rejectsDoublePlant(GameTestHelper helper) {
        var upper = Blocks.LARGE_FERN.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
        helper.assertFalse(TradeFocus.canPick(upper), "upper half of a large fern can be picked");
        helper.assertFalse(TradeFocus.canPick(Blocks.LARGE_FERN.defaultBlockState()), "lower half of a large fern can be picked");
        helper.assertFalse(TradeFocus.canPick(Blocks.OAK_DOOR.defaultBlockState()), "door can be picked");
        helper.assertTrue(TradeFocus.canPick(Blocks.STONE.defaultBlockState()), "stone cannot be picked");
        helper.assertTrue(TradeFocus.canPick(Blocks.FERN.defaultBlockState()), "fern cannot be picked");
        helper.succeed();
    }

    private static void clearsOnSneakAir(GameTestHelper helper) {
        var level = helper.getLevel();
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.setXRot(-90.0F);
        player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).setBaseValue(2.0);
        ItemStack wand = tradeWand();
        TradeFocus.storePickedBlock(wand, new ItemStack(Items.LARGE_FERN));
        helper.assertTrue(FocusUtils.getTargetBlock(level, player, false, player.blockInteractionRange()) == null, "player is not looking at air");
        TradeFocus focus = (TradeFocus) ModItems.FOCUS_EQUAL_TRADE.get();
        player.setShiftKeyDown(false);
        focus.onFocusRightClick(wand, level, player, null);
        helper.assertFalse(TradeFocus.getPickedBlock(wand).isEmpty(), "right click at air without sneaking cleared the picked block");
        player.setShiftKeyDown(true);
        focus.onFocusRightClick(wand, level, player, null);
        helper.assertTrue(TradeFocus.getPickedBlock(wand).isEmpty(), "sneak right click at air did not clear the picked block");
        helper.succeed();
    }

    private static void keepsUnplaceableItem(GameTestHelper helper) {
        var level = helper.getLevel();
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        BlockPos targetOffset = new BlockPos(3, 1, 3);
        helper.setBlock(targetOffset, Blocks.STONE);
        BlockPos target = helper.absolutePos(targetOffset);
        player.setPos(target.getX() + 0.5, target.getY() + 1.0, target.getZ() - 1.5);
        ItemStack wand = tradeWand();
        player.getInventory().setItem(0, wand);
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(1, new ItemStack(Items.OAK_SAPLING, 4));
        BlockSwapper.add(level, target, level.getBlockState(target), new ItemStack(Items.OAK_SAPLING), 0, player, 0);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(helper.getBlockState(targetOffset).is(Blocks.STONE), "sapling replaced stone without support");
            helper.assertValueEqual(player.getInventory().getItem(1).getCount(), 4, "unplaceable sapling was consumed");
            helper.succeed();
        });
    }

    private static void bellReset(GameTestHelper helper) {
        var level = helper.getLevel();
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        GolemBase golem = helper.spawn(ModEntities.GOLEM.get(), new BlockPos(3, 1, 3));
        BlockPos marked = helper.absolutePos(new BlockPos(1, 0, 1));
        golem.setMarkers(List.of(new Marker(GlobalPos.of(level.dimension(), marked), Direction.UP, -1)));
        ItemStack bell = new ItemStack(BuiltInRegistries.ITEM.getValue(Thaumcraft.id("golem_bell")));
        GolemKeys.BELL.set(bell, new GolemKeys.BellSelection(golem.getUUID(), GlobalPos.of(level.dimension(), golem.getHomePosition()), golem.getHomeFacing(), golem.getMarkers()));
        player.setItemInHand(InteractionHand.MAIN_HAND, bell);
        BellResetPayload.handle(new BellResetPayload(), player);
        helper.assertTrue(golem.getMarkers().isEmpty(), "bell reset left golem markers");
        GolemKeys.BellSelection selection = GolemKeys.BELL.get(player.getMainHandItem());
        helper.assertTrue(selection != null && selection.markers().isEmpty(), "bell reset left markers on the bell");
        helper.succeed();
    }

    private static void golemOnPedestal(GameTestHelper helper) {
        var level = helper.getLevel();
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        BlockPos pedestalOffset = new BlockPos(3, 1, 3);
        helper.setBlock(pedestalOffset, ModBlocks.ARCANE_PEDESTAL.get());
        BlockPos pedestal = helper.absolutePos(pedestalOffset);
        player.setPos(pedestal.getX() + 0.5, pedestal.getY(), pedestal.getZ() - 1.5);
        Item golem = BuiltInRegistries.ITEM.getValue(Thaumcraft.id("wood_golem"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(golem));
        player.setShiftKeyDown(true);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pedestal).add(0.0, 0.5, 0.0), Direction.UP, pedestal, false);
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(level.getBlockEntity(pedestal) instanceof PedestalBlockEntity entity && entity.getStack().is(golem), "sneak click did not put the golem on the pedestal");
        helper.assertTrue(level.getEntitiesOfClass(GolemBase.class, helper.getBounds()).isEmpty(), "sneak click on the pedestal spawned a golem");
        helper.succeed();
    }

    private static void ringOnShortGrass(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(3, 3, 3)).offset(32000, 0, -32000);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                level.setBlock(center.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(center.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                level.setBlock(center.offset(x, 1, z), ((x + z) & 1) == 0 ? Blocks.SHORT_GRASS.defaultBlockState() : Blocks.FERN.defaultBlockState(), 3);
                for (int y = 2; y <= 9; y++) {
                    level.setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        helper.assertTrue(EldritchRingGenerator.generate(level, new Random(41L), center.getX() >> 4, center.getZ() >> 4, center.getX(), center.getY(), center.getZ()),
            "ring was rejected on grass covered with short grass and ferns");
        helper.assertTrue(level.getBlockState(center.above()).is(EldritchContent.ELDRITCH_ALTAR.get()), "ring on grass did not place its altar");
        helper.succeed();
    }

    private static void guardianLight(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos dim = lightBox(helper, 1, 12);
        BlockPos bright = lightBox(helper, 5, 13);
        helper.runAfterDelay(5, () -> {
            helper.assertValueEqual(level.getMaxLocalRawBrightness(dim), 12, "light level 12 setup");
            helper.assertValueEqual(level.getMaxLocalRawBrightness(bright), 13, "light level 13 setup");
            for (int seed = 0; seed < 16; seed++) {
                helper.assertTrue(SpawnPlacements.checkSpawnRules(ModEntities.ELDRITCH_GUARDIAN.get(), level, EntitySpawnReason.NATURAL, dim, RandomSource.create(seed)),
                    "guardian spawn rejected at light 12");
            }
            helper.assertFalse(SpawnPlacements.checkSpawnRules(ModEntities.ELDRITCH_GUARDIAN.get(), level, EntitySpawnReason.NATURAL, bright, RandomSource.create(0)),
                "guardian spawn allowed at light 13");
            helper.succeed();
        });
    }

    private static BlockPos lightBox(GameTestHelper helper, int x, int light) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(x, 2, 3));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    boolean shell = dx != 0 || dz != 0 || dy == -1 || dy == 2;
                    level.setBlockAndUpdate(pos.offset(dx, dy, dz), shell ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
        level.setBlockAndUpdate(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, light));
        return pos;
    }

    private static void nodePlace(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos groundOffset = new BlockPos(3, 1, 3);
        helper.setBlock(groundOffset, Blocks.GRASS_BLOCK);
        helper.setBlock(groundOffset.above(), Blocks.SHORT_GRASS);
        BlockPos pos = helper.absolutePos(groundOffset.above());
        level.getServer().getCommands().performPrefixedCommand(
            level.getServer().createCommandSourceStack().withLevel(level),
            "thaumcraft node place " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " pure bright"
        );
        helper.assertTrue(level.getBlockEntity(pos) instanceof Node node && node.getNodeType() == NodeType.PURE && node.getNodeModifier() == NodeModifier.BRIGHT,
            "node place did not create a pure bright node over short grass");
        helper.succeed();
    }

    private static void sanityChecker(GameTestHelper helper) {
        ItemStack output = InfusionCrafting.result(helper.getLevel(), InfusionCrafting.TYPE_RECIPE, "SanityCheck", new ItemStack(ModItems.THAUMOMETER.get()));
        helper.assertTrue(output.is(ModItems.SANITY_CHECKER.get()), "sanity checker infusion has no output");
        helper.assertValueEqual(output.getMaxStackSize(), 1, "sanity checker stack size");
        helper.succeed();
    }
}
