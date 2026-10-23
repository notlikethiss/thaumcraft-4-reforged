package thaumcraft.test;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.blockentity.WandPedestalBlockEntity;
import thaumcraft.node.NodeBlockEntity;
import thaumcraft.node.NodeGenerator;
import thaumcraft.node.NodeType;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModItems;
import thaumcraft.network.PlayerCompletePayload;
import thaumcraft.network.ScanPayload;
import thaumcraft.research.Knowledge;
import thaumcraft.research.PlayerKnowledge;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchManager;
import thaumcraft.research.ScanManager;
import thaumcraft.research.Warp;
import thaumcraft.research.WarpType;
import thaumcraft.wand.WandRegistry;
import thaumcraft.wand.WandKeys;
import thaumcraft.wand.WandTriggers;
import thaumcraft.wand.Wands;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ThaumcraftGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final String[] TESTS = {"wand_taps_node", "pedestal_charges_wand", "node_jar_trigger", "research_warp_split", "research_purchase", "scan_award_caps", "modern_scan_targets", "wand_casting_visual_state", "scan_fluid_targets", "scan_blocks_through_water", "scan_continuous_use", "energized_node_scan_identity", "legacy_energized_node_identity", "released_node_scan_sync", "node_charge_visual_sync", "scan_fire_targets"};

    static {
        FUNCTIONS.register(TESTS[0], () -> ThaumcraftGameTests::wandTapsNode);
        FUNCTIONS.register(TESTS[1], () -> ThaumcraftGameTests::pedestalChargesWand);
        FUNCTIONS.register(TESTS[2], () -> ThaumcraftGameTests::nodeJarTrigger);
        FUNCTIONS.register(TESTS[3], () -> ThaumcraftGameTests::researchWarpSplit);
        FUNCTIONS.register(TESTS[4], () -> ThaumcraftGameTests::researchPurchase);
        FUNCTIONS.register(TESTS[5], () -> ThaumcraftGameTests::scanAwardCaps);
        FUNCTIONS.register(TESTS[6], () -> ThaumcraftGameTests::modernScanTargets);
        FUNCTIONS.register(TESTS[7], () -> ThaumcraftGameTests::wandCastingVisualState);
        FUNCTIONS.register(TESTS[8], () -> ThaumcraftGameTests::scanFluidTargets);
        FUNCTIONS.register(TESTS[9], () -> ThaumcraftGameTests::scanBlocksThroughWater);
        FUNCTIONS.register(TESTS[10], () -> ThaumcraftGameTests::scanContinuousUse);
        FUNCTIONS.register(TESTS[11], () -> ThaumcraftGameTests::energizedNodeScanIdentity);
        FUNCTIONS.register(TESTS[12], () -> ThaumcraftGameTests::legacyEnergizedNodeIdentity);
        FUNCTIONS.register(TESTS[13], () -> ThaumcraftGameTests::releasedNodeScanSync);
        FUNCTIONS.register(TESTS[14], () -> ThaumcraftGameTests::nodeChargeVisualSync);
        FUNCTIONS.register(TESTS[15], () -> ThaumcraftGameTests::scanFireTargets);
    }

    private ThaumcraftGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
            Thaumcraft.id("default"),
            new TestEnvironmentDefinition.AllOf()
        );
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name));
            event.registerTest(
                Thaumcraft.id(name),
                new FunctionGameTestInstance(function, new TestData<>(environment, Thaumcraft.id("test/platform"), 200, 0, true))
            );
        }
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
        return player;
    }

    private static ResearchItem testResearch(String key, int warp) {
        if (ResearchCategories.get("TESTCAT") == null) {
            ResearchCategories.registerCategory("TESTCAT", Thaumcraft.id("textures/misc/blank.png"), Thaumcraft.id("textures/gui/gui_researchback.png"));
        }
        ResearchItem existing = ResearchCategories.getResearch(key);
        if (existing != null) {
            return existing;
        }
        int column = ResearchCategories.allResearch().size() * 2;
        ResearchItem item = new ResearchItem(key, "TESTCAT", new AspectList().add(Aspect.AIR, 5), column, 0, 1, Thaumcraft.id("textures/misc/blank.png"))
            .setSecondary()
            .setWarp(warp);
        item.registerResearchItem();
        return item;
    }

    private static void researchWarpSplit(GameTestHelper helper) {
        testResearch("TESTWARP", 5);
        ServerPlayer player = mockServerPlayer(helper);
        helper.assertTrue(ResearchManager.complete(player, "TESTWARP"), "research not completed");
        helper.assertTrue(!ResearchManager.complete(player, "TESTWARP"), "research completed twice");
        helper.assertValueEqual(Warp.get(player, WarpType.PERMANENT), 3, "permanent warp");
        helper.assertValueEqual(Warp.get(player, WarpType.STICKY), 2, "sticky warp");
        helper.assertValueEqual(Warp.total(player), Knowledge.get(player).getWarpCounter(), "warp counter");
        helper.succeed();
    }

    private static void researchPurchase(GameTestHelper helper) {
        testResearch("TESTBUY", 0);
        ServerPlayer player = mockServerPlayer(helper);
        Knowledge.get(player).setAspectPool(Aspect.AIR, 4);
        ResearchManager.handlePlayerComplete(new PlayerCompletePayload("TESTBUY", ResearchManager.ACTION_PURCHASE), player);
        helper.assertTrue(!ResearchManager.isComplete(player, "TESTBUY"), "bought without enough pool");
        Knowledge.get(player).setAspectPool(Aspect.AIR, 12);
        ResearchManager.handlePlayerComplete(new PlayerCompletePayload("TESTBUY", ResearchManager.ACTION_PURCHASE), player);
        helper.assertTrue(ResearchManager.isComplete(player, "TESTBUY"), "purchase failed");
        helper.assertValueEqual(Knowledge.get(player).getAspectPool(Aspect.AIR), 7, "pool after purchase");
        helper.succeed();
    }

    private static void modernScanTargets(GameTestHelper helper) {
        for (String id : List.of("deepslate", "tuff_brick_wall", "waxed_oxidized_cut_copper_stairs", "lime_concrete", "echo_shard")) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id));
            ScanManager.Target target = ScanManager.resolveStack(new ItemStack(item));
            helper.assertTrue(target != null && target.aspects() != null && target.aspects().size() > 0, "no aspects for " + id);
        }
        for (String id : List.of("axolotl", "warden", "husk")) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id));
            Entity entity = helper.spawn(type, new BlockPos(1, 2, 1));
            ScanManager.Target target = ScanManager.resolveEntity(entity);
            helper.assertTrue(target != null && target.aspects() != null && target.aspects().size() > 0, "no aspects for " + id);
            entity.discard();
        }
        helper.succeed();
    }

    private static void scanAwardCaps(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        PlayerKnowledge knowledge = Knowledge.get(player);
        knowledge.setAspectPool(Aspect.FIRE, 10);
        helper.assertValueEqual(ResearchManager.awardAspect(player, Aspect.FIRE, 4), 4, "normal award");
        knowledge.setAspectPool(Aspect.FIRE, 100);
        helper.assertValueEqual(ResearchManager.awardAspect(player, Aspect.FIRE, 16), 4, "award above cap");
        knowledge.setAspectPool(Aspect.FIRE, 125);
        helper.assertValueEqual(ResearchManager.awardAspect(player, Aspect.FIRE, 16), 1, "award above 125");
        helper.assertValueEqual(knowledge.getAspectPool(Aspect.FIRE), 126, "pool after awards");
        helper.succeed();
    }

    private static void scanContinuousUse(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 3, 2.5)));
        player.setNoGravity(true);
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = new ItemStack(ModItems.THAUMOMETER.get());
            player.setItemInHand(hand, stack);
            InteractionResult result = stack.getItem().use(helper.getLevel(), player, hand);
            helper.assertTrue(result instanceof InteractionResult.Success success && success.swingSource() == net.minecraft.world.InteractionResult.SwingSource.NONE, "scanning must start without a hand swing");
            for (int tick = 0; tick < 60; tick++) {
                player.tick();
            }
            helper.assertTrue(player.isUsingItem() && player.getUsedItemHand() == hand, "holding the scanner must not end and restart every 25 ticks");
            helper.assertValueEqual(player.getTicksUsingItem(), 60, "held scanner use must advance continuously");
            player.releaseUsingItem();
            helper.assertFalse(player.isUsingItem(), "releasing the scanner must end use");
            stack.getItem().use(helper.getLevel(), player, hand);
            player.setItemInHand(hand, ItemStack.EMPTY);
            player.tick();
            helper.assertFalse(player.isUsingItem(), "changing the held item must end scanner use");
        }
        helper.succeed();
    }

    private static void scanFluidTargets(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        player.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        ScanManager.Target water = ScanManager.resolveBlock(helper.getLevel(), pos);
        helper.assertTrue(water != null && water.aspects() != null, "water must be scannable without an item");
        helper.assertValueEqual(water.aspects().size(), 1, "water aspect count");
        helper.assertValueEqual(water.aspects().getAmount(Aspect.WATER), 4, "water aspects");
        helper.assertTrue(!water.key().equals(ScanManager.itemKey(new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET))), "water is not a bucket");
        int before = Knowledge.get(player).getAspectPool(Aspect.WATER);
        ScanPayload payload = new ScanPayload(ScanManager.KIND_BLOCK, pos, 0);
        ScanManager.handleScan(payload, player);
        helper.assertTrue(ScanManager.hasBeenScanned(player, water), "water scan must be saved");
        helper.assertValueEqual(Knowledge.get(player).getAspectPool(Aspect.WATER), before + 4, "water scan award");
        ScanManager.handleScan(payload, player);
        helper.assertValueEqual(Knowledge.get(player).getAspectPool(Aspect.WATER), before + 4, "water must not award twice");
        helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3));
        helper.assertTrue(ScanManager.hasBeenScanned(player, ScanManager.resolveBlock(helper.getLevel(), pos)), "flowing water must share the scan");
        helper.getLevel().setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
        ScanManager.Target lava = ScanManager.resolveBlock(helper.getLevel(), pos);
        helper.assertTrue(lava != null && lava.aspects() != null, "lava must be scannable without an item");
        helper.assertValueEqual(lava.aspects().size(), 2, "lava aspect count");
        helper.assertValueEqual(lava.aspects().getAmount(Aspect.FIRE), 3, "lava fire aspects");
        helper.assertValueEqual(lava.aspects().getAmount(Aspect.EARTH), 1, "lava earth aspects");
        helper.assertTrue(!ScanManager.hasBeenScanned(player, lava), "water must not mark lava scanned");
        helper.succeed();
    }

    private static void scanFireTargets(GameTestHelper helper) {
        var level = helper.getLevel();
        ServerPlayer player = mockServerPlayer(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        level.setBlockAndUpdate(pos.below(), Blocks.NETHERRACK.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
        level.setBlockAndUpdate(pos.south(), Blocks.STONE.defaultBlockState());
        player.snapTo(pos.getX() + 0.5, pos.getY() + 0.5 - player.getEyeHeight(), pos.getZ() - 1.5, 0.0F, 0.0F);
        var hit = ScanManager.pickBlock(level, player);
        helper.assertTrue(hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos), "scanner must target the flame rather than the block behind it");
        var target = ScanManager.resolveChecked(player, new ScanPayload(ScanManager.KIND_BLOCK, pos, 0));
        helper.assertTrue(target != null && target.key().equals("minecraft:fire") && target.clueStack().isEmpty(), "itemless fire must be a scan target");
        helper.assertValueEqual(target.aspects().getAmount(Aspect.FIRE), 4, "original fire aspect amount");
        int pool = Knowledge.get(player).getAspectPool(Aspect.FIRE);
        helper.assertTrue(ScanManager.completeScan(player, target), "fire scan did not complete");
        helper.assertValueEqual(Knowledge.get(player).getAspectPool(Aspect.FIRE), pool + 4, "fire scan award");
        level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState().setValue(net.minecraft.world.level.block.FireBlock.AGE, 8));
        helper.assertTrue(ScanManager.hasBeenScanned(player, ScanManager.resolveBlock(level, pos)), "fire age changed the scan identity");
        ScanManager.handleScan(new ScanPayload(ScanManager.KIND_BLOCK, pos, 0), player);
        helper.assertValueEqual(Knowledge.get(player).getAspectPool(Aspect.FIRE), pool + 4, "repeated fire scan awarded aspects");
        level.setBlockAndUpdate(pos.north(), Blocks.STONE.defaultBlockState());
        hit = ScanManager.pickBlock(level, player);
        helper.assertTrue(hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos.north()), "fire targeting must respect solid obstructions");
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() - 10.0);
        helper.assertTrue(ScanManager.resolveChecked(player, new ScanPayload(ScanManager.KIND_BLOCK, pos, 0)) == null, "server accepted fire outside scan range");
        helper.succeed();
    }

    private static void scanBlocksThroughWater(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        BlockPos waterPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos floorPos = waterPos.below();
        helper.getLevel().setBlockAndUpdate(floorPos, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(waterPos, Blocks.WATER.defaultBlockState());
        player.snapTo(waterPos.getX() + 0.5, waterPos.getY() + 0.5, waterPos.getZ() + 0.5, 0.0F, 90.0F);
        var hit = ScanManager.pickBlock(helper.getLevel(), player);
        helper.assertTrue(hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(waterPos), "unscanned water surface must be targetable");
        ScanManager.Target water = ScanManager.resolveBlock(helper.getLevel(), waterPos);
        helper.assertTrue(ScanManager.completeScan(player, water), "water scan must complete");
        hit = ScanManager.pickBlock(helper.getLevel(), player);
        helper.assertTrue(hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(floorPos), "scanned water must not hide the bottom");
        player.setPos(waterPos.getX() + 0.5, waterPos.getY() - player.getEyeHeight() + 0.5, waterPos.getZ() + 0.5);
        hit = ScanManager.pickBlock(helper.getLevel(), player);
        helper.assertTrue(hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(floorPos), "submerged scanner must target the bottom");
        ScanManager.Target floor = ScanManager.resolveChecked(player, new ScanPayload(ScanManager.KIND_BLOCK, floorPos, 0));
        helper.assertTrue(floor != null && floor.aspects() != null, "server must accept the underwater block");
        helper.getLevel().setBlockAndUpdate(waterPos, Blocks.OAK_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true));
        helper.assertTrue(!ScanManager.resolveBlock(helper.getLevel(), waterPos).key().equals(water.key()), "waterlogged blocks must keep their own scan");
        helper.succeed();
    }

    private static AspectList primals(int amount) {
        AspectList aspects = new AspectList();
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            aspects.add(aspect, amount);
        }
        return aspects;
    }

    private static int totalVis(ItemStack wand) {
        return Wands.getAllVis(wand).visSize();
    }

    private static void wandCastingVisualState(GameTestHelper helper) {
        ItemStack wand = Wands.create(WandRegistry.CAP_IRON, WandRegistry.ROD_WOOD, false);
        Wands.setFocus(wand, new ItemStack(ModItems.FOCUS_FIRE.get()));
        Wands.storeVis(wand, Aspect.FIRE, 1000);
        ItemStack update = wand.copy();
        Wands.storeVis(update, Aspect.FIRE, 900);
        helper.assertTrue(wand.getUseAnimation() == ItemUseAnimation.NONE, "casting must not apply the vanilla bow transform");
        helper.assertTrue(!wand.getItem().shouldCauseReequipAnimation(wand, update, false), "spending vis must not lower the wand");
        helper.assertValueEqual(1000, Wands.getVis(wand, Aspect.FIRE), "appearance comparison must not modify vis");
        helper.assertValueEqual(900, Wands.getVis(update, Aspect.FIRE), "appearance comparison must not modify the update");
        WandKeys.TARGET.set(update, BlockPos.ZERO);
        helper.assertTrue(!wand.getItem().shouldCauseReequipAnimation(wand, update, false), "node target updates must not lower the wand");
        helper.assertTrue(wand.getItem().shouldCauseReequipAnimation(wand, update, true), "changing slots must keep equip animation");
        Wands.setFocus(update, new ItemStack(ModItems.FOCUS_FROST.get()));
        helper.assertTrue(wand.getItem().shouldCauseReequipAnimation(wand, update, false), "changing focus must update the visible wand");
        helper.succeed();
    }

    private static void wandTapsNode(GameTestHelper helper) {
        BlockPos nodePos = helper.absolutePos(new BlockPos(3, 2, 3));
        NodeGenerator.createNodeAt(helper.getLevel(), nodePos, NodeType.NORMAL, null, primals(20));
        NodeBlockEntity node = helper.getBlockEntity(new BlockPos(3, 2, 3), NodeBlockEntity.class);
        int before = node.getAspects().visSize();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.snapTo(nodePos.getX() + 0.5, nodePos.getY() + 0.5 - player.getEyeHeight(), nodePos.getZ() + 2.5, 180.0F, 0.0F);
        player.setYHeadRot(180.0F);
        ItemStack wand = Wands.create(WandRegistry.CAP_IRON, WandRegistry.ROD_WOOD, false);
        for (int tick = 5; tick <= 50; tick += 5) {
            node.onUsingWandTick(wand, player, tick);
        }
        helper.assertTrue(totalVis(wand) > 0, "wand received no vis");
        helper.assertTrue(node.getAspects().visSize() < before, "node lost no vis");
        helper.assertValueEqual(before - node.getAspects().visSize(), totalVis(wand) / 100, "vis moved");
        helper.succeed();
    }

    private static NodeBlockEntity copyNode(GameTestHelper helper, NodeBlockEntity node) {
        NodeBlockEntity copy = new NodeBlockEntity(node.getBlockPos(), node.getBlockState());
        copy.setLevel(helper.getLevel());
        copy.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), node.getUpdateTag(helper.getLevel().registryAccess())));
        return copy;
    }

    private static void energizedNodeScanIdentity(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 3));
        level.setBlockAndUpdate(pos.below(), ModBlocks.NODE_STABILIZER.get().defaultBlockState());
        level.setBlockAndUpdate(pos.above(), ModBlocks.NODE_TRANSDUCER.get().defaultBlockState());
        NodeGenerator.createNodeAt(level, pos, NodeType.NORMAL, null, new AspectList().add(Aspect.AIR, 100).add(Aspect.FIRE, 64));
        NodeBlockEntity original = (NodeBlockEntity) level.getBlockEntity(pos);
        String id = original.getId();
        ServerPlayer player = mockServerPlayer(helper);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2);
        var target = ScanManager.resolveNode(level, pos);
        helper.assertTrue(ScanManager.completeScan(player, target), "initial node scan failed");
        int pool = Knowledge.get(player).getAspectPool(Aspect.AIR);
        level.setBlockAndUpdate(pos.above(2), Blocks.REDSTONE_BLOCK.defaultBlockState());
        var transducer = (thaumcraft.node.NodeTransducerBlockEntity) level.getBlockEntity(pos.above());
        for (int tick = 0; tick < 120; tick++) {
            thaumcraft.node.NodeTransducerBlockEntity.tick(level, pos.above(), transducer.getBlockState(), transducer);
        }
        helper.assertTrue(transducer.getStatus() == 1 && transducer.getCount() == 120, "conversion did not start");
        level.removeBlock(pos.above(2), false);
        for (int tick = 0; tick < 60; tick++) {
            thaumcraft.node.NodeTransducerBlockEntity.tick(level, pos.above(), transducer.getBlockState(), transducer);
        }
        helper.assertTrue(transducer.getCount() == 60 && level.getBlockEntity(pos) == original && original.getId().equals(id), "interruption did not roll back conversion progress");
        level.setBlockAndUpdate(pos.above(2), Blocks.REDSTONE_BLOCK.defaultBlockState());
        for (int tick = 0; tick <= 1000; tick++) {
            thaumcraft.node.NodeTransducerBlockEntity.tick(level, pos.above(), transducer.getBlockState(), transducer);
        }
        helper.assertTrue(level.getBlockEntity(pos) instanceof thaumcraft.node.EnergizedNodeBlockEntity, "transducer did not energize the node");
        var energized = (thaumcraft.node.EnergizedNodeBlockEntity) level.getBlockEntity(pos);
        target = ScanManager.resolveBlock(level, pos);
        helper.assertTrue(target != null && target.kind() == thaumcraft.research.ScanKind.PHENOMENON, "energized node is not a scan target");
        helper.assertTrue(id.equals(energized.getId()) && target.key().equals("NODE" + id), "conversion changed scan identity");
        helper.assertValueEqual(energized.getAspects().getAmount(Aspect.AIR), 10, "converted air CV");
        helper.assertValueEqual(energized.getAspects().getAmount(Aspect.FIRE), 8, "converted fire CV");
        helper.assertTrue(!(energized instanceof thaumcraft.wand.WandInteractable), "CV node must not allow direct wand tapping");
        ScanManager.handleScan(new ScanPayload(ScanManager.KIND_NODE, pos, 0), player);
        helper.assertTrue(ScanManager.hasBeenScanned(player, target), "conversion lost scan knowledge");
        helper.assertValueEqual(Knowledge.get(player).getAspectPool(Aspect.AIR), pool, "conversion awarded a second scan");
        ServerPlayer fresh = mockServerPlayer(helper);
        fresh.setPos(player.position());
        ScanManager.handleScan(new ScanPayload(ScanManager.KIND_NODE, pos, 0), fresh);
        helper.assertTrue(ScanManager.hasBeenScanned(fresh, target), "new player could not scan the CV node");
        var copy = new thaumcraft.node.EnergizedNodeBlockEntity(pos, energized.getBlockState());
        copy.setLevel(level);
        copy.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), energized.getUpdateTag(level.registryAccess())));
        helper.assertTrue(copy.getId().equals(id) && copy.getAspects().equals(energized.getAspects()), "energized node data did not synchronize");
        level.removeBlock(pos.above(2), false);
        for (int tick = 0; tick < 951; tick++) {
            thaumcraft.node.NodeTransducerBlockEntity.tick(level, pos.above(), transducer.getBlockState(), transducer);
        }
        helper.assertTrue(level.getBlockEntity(pos) instanceof NodeBlockEntity, "node did not revert after power removal");
        NodeBlockEntity restored = (NodeBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(restored.getId().equals(id), "reversion changed scan identity");
        helper.assertTrue(restored.getAspectsBase().equals(new AspectList().add(Aspect.AIR, 100).add(Aspect.FIRE, 64)), "reversion lost base aspects");
        helper.assertValueEqual(restored.getAspects().visSize(), 0, "reverted node should start depleted");
        helper.assertTrue(copyNode(helper, restored).getId().equals(id), "reverted identity did not synchronize");
        helper.succeed();
    }

    private static void legacyEnergizedNodeIdentity(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos firstPos = helper.absolutePos(new BlockPos(2, 2, 2));
        var first = new thaumcraft.node.EnergizedNodeBlockEntity(firstPos, ModBlocks.ENERGIZED_AURA_NODE.get().defaultBlockState());
        first.setLevel(level);
        String id = first.getId();
        var second = new thaumcraft.node.EnergizedNodeBlockEntity(firstPos.east(), first.getBlockState());
        second.setLevel(level);
        second.setId("");
        helper.assertTrue(!id.isEmpty() && !id.equals("blank") && !id.equals(second.getId()), "legacy nodes share a blank scan identity");
        var copy = new thaumcraft.node.EnergizedNodeBlockEntity(firstPos, first.getBlockState());
        copy.setLevel(level);
        copy.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), first.saveWithoutMetadata(level.registryAccess())));
        helper.assertTrue(id.equals(copy.getId()), "legacy node identity did not persist");
        helper.succeed();
    }

    private static void releasedNodeScanSync(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 3));
        ServerPlayer player = mockServerPlayer(helper);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2);
        AspectList aspects = new AspectList().add(Aspect.AIR, 37).add(Aspect.FIRE, 19);
        String id = "jar:" + pos;
        ItemStack jarStack = thaumcraft.item.NodeJarItem.create(aspects, NodeType.PURE, thaumcraft.node.NodeModifier.BRIGHT, id);
        level.setBlockAndUpdate(pos, ModBlocks.NODE_JAR.get().defaultBlockState());
        ModBlocks.NODE_JAR.get().setPlacedBy(level, pos, level.getBlockState(pos), player, jarStack);
        var jarTarget = ScanManager.resolveNode(level, pos);
        var jar = (thaumcraft.blockentity.NodeJarBlockEntity) level.getBlockEntity(pos);
        var jarCopy = new thaumcraft.blockentity.NodeJarBlockEntity(pos, jar.getBlockState());
        jarCopy.setLevel(level);
        jarCopy.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), jar.getUpdateTag(level.registryAccess())));
        helper.assertTrue(jarCopy.getAspects().equals(aspects) && jarCopy.getAspectsBase().equals(aspects), "jar contents did not reach the client");
        helper.assertTrue(jarCopy.getId().equals(id) && jarCopy.getNodeType() == NodeType.PURE && jarCopy.getNodeModifier() == thaumcraft.node.NodeModifier.BRIGHT, "jar metadata did not reach the client");
        helper.assertTrue(ScanManager.resolveBlock(level, pos).key().equals(jarTarget.key()), "targeting the jar did not resolve its node");
        helper.assertTrue(ScanManager.resolveChecked(player, new ScanPayload(ScanManager.KIND_NODE, pos, 0)) != null, "server rejected a jar scan in range");
        for (Aspect aspect : Aspect.values()) Knowledge.get(player).discoverAspect(aspect);
        helper.assertTrue(ScanManager.completeScan(player, jarTarget), "jar scan failed");
        int pool = Knowledge.get(player).getAspectPool(Aspect.AIR);
        ItemStack wand = Wands.create(WandRegistry.CAP_IRON, WandRegistry.ROD_WOOD, false);
        ((thaumcraft.block.NodeJarBlock) ModBlocks.NODE_JAR.get()).onWandUseOn(level, pos, player, wand, Direction.UP);
        helper.assertTrue(level.getBlockEntity(pos) instanceof NodeBlockEntity, "jar did not release its node");
        NodeBlockEntity node = (NodeBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(node.getAspects().equals(aspects) && node.getAspectsBase().equals(aspects), "release lost aspects");
        helper.assertTrue(node.getNodeType() == NodeType.PURE && node.getNodeModifier() == thaumcraft.node.NodeModifier.BRIGHT && node.getId().equals(id), "release lost node metadata");
        NodeBlockEntity copy = copyNode(helper, node);
        helper.assertTrue(copy.getAspects().equals(aspects) && copy.getId().equals(id), "released node does not synchronize");
        var target = ScanManager.resolveNode(level, pos);
        helper.assertTrue(target.key().equals(jarTarget.key()) && ScanManager.hasBeenScanned(player, target), "release lost scan identity");
        ScanManager.handleScan(new ScanPayload(ScanManager.KIND_NODE, pos, 0), player);
        helper.assertValueEqual(Knowledge.get(player).getAspectPool(Aspect.AIR), pool, "release awarded a second scan");
        ServerPlayer fresh = mockServerPlayer(helper);
        for (Aspect aspect : Aspect.values()) Knowledge.get(fresh).discoverAspect(aspect);
        helper.assertTrue(!ScanManager.hasBeenScanned(fresh, target) && ScanManager.completeScan(fresh, target), "unscanned released node cannot be scanned");
        helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(1)).isEmpty(), "releasing a node duplicated its jar");
        helper.succeed();
    }

    private static void nodeChargeVisualSync(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 3));
        level.setBlockAndUpdate(pos.below(), ModBlocks.NODE_STABILIZER.get().defaultBlockState());
        NodeGenerator.createNodeAt(level, pos, NodeType.NORMAL, null, new AspectList().add(Aspect.FIRE, 20));
        NodeBlockEntity node = (NodeBlockEntity) level.getBlockEntity(pos);
        ServerPlayer player = mockServerPlayer(helper);
        level.addNewPlayer(player);
        player.setNoGravity(true);
        player.snapTo(pos.getX() + 0.5, pos.getY() + 0.5 - player.getEyeHeight(), pos.getZ() + 2.5, 180.0F, 0.0F);
        player.setYHeadRot(180.0F);
        ItemStack wand = Wands.create(WandRegistry.CAP_IRON, WandRegistry.ROD_WOOD, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        node.onWandUse(level, pos, player, wand);
        player.startUsingItem(InteractionHand.MAIN_HAND);
        int before = node.getAspects().getAmount(Aspect.FIRE);
        node.onUsingWandTick(wand, player, 5);
        helper.assertValueEqual(Wands.getVis(wand, Aspect.FIRE), 100, "stabilized node did not charge the held wand");
        helper.assertValueEqual(node.getAspects().getAmount(Aspect.FIRE), before - 1, "held wand did not drain exactly one vis");
        NodeBlockEntity copy = copyNode(helper, node);
        helper.assertTrue(copy.getDrainEntity() == player && copy.getDrainCollision() != null, "charging beam state did not reach an observing client");
        helper.assertValueEqual(copy.getDrainColor(), Aspect.FIRE.getColor(), "charging beam has the wrong color");
        helper.assertTrue(!node.saveWithoutMetadata(level.registryAccess()).contains("drainEntity"), "temporary beam entity ID leaked into world saves");
        player.releaseUsingItem();
        copy = copyNode(helper, node);
        helper.assertTrue(copy.getDrainEntity() == null && copy.getDrainCollision() == null, "releasing the wand left a stale beam");
        player.startUsingItem(InteractionHand.MAIN_HAND);
        node.onWandUse(level, pos, player, wand);
        Wands.fill(wand);
        node.onUsingWandTick(wand, player, 10);
        helper.assertTrue(copyNode(helper, node).getDrainEntity() == null, "a full wand must not display a charging beam");
        Wands.storeVis(wand, Aspect.FIRE, 0);
        player.setYRot(0.0F);
        player.setYHeadRot(0.0F);
        int remaining = node.getAspects().getAmount(Aspect.FIRE);
        node.onUsingWandTick(wand, player, 15);
        helper.assertValueEqual(Wands.getVis(wand, Aspect.FIRE), 0, "looking away still charged the wand");
        helper.assertValueEqual(node.getAspects().getAmount(Aspect.FIRE), remaining, "looking away drained the node");
        helper.assertTrue(!player.isUsingItem(), "looking away did not stop wand use");
        helper.succeed();
    }

    private static void pedestalChargesWand(GameTestHelper helper) {
        NodeGenerator.createNodeAt(helper.getLevel(), helper.absolutePos(new BlockPos(3, 4, 3)), NodeType.NORMAL, null, primals(20));
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.WAND_RECHARGE_PEDESTAL.get());
        WandPedestalBlockEntity pedestal = helper.getBlockEntity(new BlockPos(1, 1, 1), WandPedestalBlockEntity.class);
        ItemStack wand = Wands.create(WandRegistry.CAP_GOLD, WandRegistry.ROD_GREATWOOD, false);
        pedestal.setStack(wand);
        helper.succeedWhen(() -> helper.assertTrue(totalVis(pedestal.getStack()) >= 600, "pedestal did not charge the wand"));
    }

    private static void nodeJarTrigger(GameTestHelper helper) {
        ItemStack jar = thaumcraft.item.NodeJarItem.create(primals(15), NodeType.HUNGRY, null, "name_test");
        helper.assertTrue(jar.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name
            && name.getKey().equals("block.thaumcraft.node_jar"), "jar item does not use the translated block name");
        BlockPos center = new BlockPos(3, 2, 3);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -1; y <= 1; y++) {
                    if (x != 0 || y != 0 || z != 0) {
                        helper.setBlock(center.offset(x, y, z), Blocks.GLASS);
                    }
                }
                helper.setBlock(center.offset(x, 2, z), Blocks.OAK_SLAB);
            }
        }
        NodeGenerator.createNodeAt(helper.getLevel(), helper.absolutePos(center), NodeType.HUNGRY, null, primals(15));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Knowledge.get(player).addResearch("NODEJAR");
        ItemStack wand = Wands.create(WandRegistry.CAP_GOLD, WandRegistry.ROD_SILVERWOOD, false);
        Wands.fill(wand);
        boolean done = WandTriggers.perform(helper.getLevel(), wand, player, helper.absolutePos(center.offset(1, 1, 1)), Direction.UP);
        helper.assertTrue(done, "trigger did not fire");
        helper.assertBlockPresent(ModBlocks.NODE_JAR.get(), center);
        var captured = (thaumcraft.blockentity.NodeJarBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(center));
        helper.assertTrue(captured.getAspects().equals(primals(15)), "capture lost the node aspects");
        helper.assertTrue(!captured.getId().isEmpty(), "capture lost the node scan identity");
        var scan = ScanManager.resolveBlock(helper.getLevel(), helper.absolutePos(center));
        helper.assertTrue(scan != null && scan.key().equals("NODE" + captured.getId()) && scan.aspects() != null, "captured jar cannot be scanned");
        helper.assertBlockPresent(Blocks.AIR, center.offset(1, 1, 1));
        helper.assertValueEqual(Wands.getVis(wand, Aspect.AIR), Wands.getMaxVis(wand) - 7000, "air vis left");
        helper.succeed();
    }
}
