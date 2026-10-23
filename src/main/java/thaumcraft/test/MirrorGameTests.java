package thaumcraft.test;

import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectHelper;
import thaumcraft.aspect.AspectList;
import thaumcraft.device.EssentiaMirrorBlockEntity;
import thaumcraft.device.HandMirrorItem;
import thaumcraft.device.HandMirrorMenu;
import thaumcraft.device.MirrorBlock;
import thaumcraft.device.MirrorBlockEntity;
import thaumcraft.device.MirrorKeys;
import thaumcraft.device.ResonatorItem;
import thaumcraft.device.SinisterCone;
import thaumcraft.essentia.EssentiaHandler;
import thaumcraft.essentia.JarBlockEntity;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModItems;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class MirrorGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final String[] TESTS = {
        "mirrors_transport_item",
        "essentia_mirror_forwards_essentia",
        "mirror_item_links_and_drops",
        "hand_mirror_sends_item",
        "resonator_reports_suction",
        "sinister_cone_sees_ahead",
        "device_items_have_aspects",
        "mirror_accepts_piped_items",
        "hand_mirror_menu_transports"
    };

    static {
        FUNCTIONS.register(TESTS[0], () -> MirrorGameTests::mirrorsTransportItem);
        FUNCTIONS.register(TESTS[1], () -> MirrorGameTests::essentiaMirrorForwards);
        FUNCTIONS.register(TESTS[2], () -> MirrorGameTests::mirrorItemLinksAndDrops);
        FUNCTIONS.register(TESTS[3], () -> MirrorGameTests::handMirrorSendsItem);
        FUNCTIONS.register(TESTS[4], () -> MirrorGameTests::resonatorReportsSuction);
        FUNCTIONS.register(TESTS[5], () -> MirrorGameTests::sinisterConeSeesAhead);
        FUNCTIONS.register(TESTS[6], () -> MirrorGameTests::deviceItemsHaveAspects);
        FUNCTIONS.register(TESTS[7], () -> MirrorGameTests::mirrorAcceptsPipedItems);
        FUNCTIONS.register(TESTS[8], () -> MirrorGameTests::handMirrorMenuTransports);
    }

    private MirrorGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(Thaumcraft.id("mirror_default"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name));
            event.registerTest(
                Thaumcraft.id(name),
                new FunctionGameTestInstance(function, new TestData<>(environment, Thaumcraft.id("test/platform"), 100, 0, true))
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
        player.connection.markClientLoaded();
        return player;
    }

    private static BlockPos placeMirror(GameTestHelper helper, BlockPos pos, boolean essentia, Direction facing) {
        BlockPos support = pos.relative(facing.getOpposite());
        helper.setBlock(support, Blocks.STONE);
        MirrorBlock block = essentia ? ModBlocks.ESSENTIA_MIRROR.get() : ModBlocks.MAGIC_MIRROR.get();
        helper.setBlock(pos, block.defaultBlockState().setValue(MirrorBlock.FACING, facing));
        return pos;
    }

    private static List<ItemEntity> itemsNear(GameTestHelper helper, BlockPos pos, double radius) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(radius), ItemEntity::isAlive);
    }

    private static void mirrorsTransportItem(GameTestHelper helper) {
        BlockPos first = placeMirror(helper, new BlockPos(1, 1, 1), false, Direction.EAST);
        BlockPos second = placeMirror(helper, new BlockPos(5, 1, 1), false, Direction.EAST);
        MirrorBlockEntity firstMirror = helper.getBlockEntity(first, MirrorBlockEntity.class);
        MirrorBlockEntity secondMirror = helper.getBlockEntity(second, MirrorBlockEntity.class);
        firstMirror.setLinkTarget(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(second)));
        firstMirror.restoreLink(true);
        helper.assertTrue(firstMirror.isLinked() && secondMirror.isLinked(), "mirrors did not link");
        helper.assertTrue(firstMirror.isLinkValid(true) && secondMirror.isLinkValid(true), "link not valid from both sides");
        helper.spawnItem(Items.DIAMOND, 1.5F, 1.2F, 1.5F);
        helper.succeedWhen(() -> {
            helper.assertTrue(itemsNear(helper, second, 2.0).stream().anyMatch(entity -> entity.getItem().is(Items.DIAMOND)), "item did not come out of the second mirror");
            helper.assertValueEqual(itemsNear(helper, new BlockPos(3, 1, 1), 5.0).stream().filter(entity -> entity.getItem().is(Items.DIAMOND)).mapToInt(entity -> entity.getItem().getCount()).sum(), 1, "diamond count");
        });
    }

    private static void essentiaMirrorForwards(GameTestHelper helper) {
        BlockPos near = placeMirror(helper, new BlockPos(1, 1, 1), true, Direction.EAST);
        BlockPos far = placeMirror(helper, new BlockPos(5, 1, 1), true, Direction.EAST);
        BlockPos jarPos = new BlockPos(6, 1, 1);
        BlockPos consumerPos = new BlockPos(3, 1, 1);
        helper.setBlock(jarPos, ModBlocks.WARDED_JAR.get());
        helper.setBlock(consumerPos, ModBlocks.WARDED_JAR.get());
        JarBlockEntity jar = helper.getBlockEntity(jarPos, JarBlockEntity.class);
        JarBlockEntity consumer = helper.getBlockEntity(consumerPos, JarBlockEntity.class);
        jar.addToContainer(Aspect.AIR, 10);
        EssentiaMirrorBlockEntity nearMirror = helper.getBlockEntity(near, EssentiaMirrorBlockEntity.class);
        EssentiaMirrorBlockEntity farMirror = helper.getBlockEntity(far, EssentiaMirrorBlockEntity.class);
        nearMirror.setLinkTarget(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(far)));
        nearMirror.restoreLink(true);
        helper.assertTrue(nearMirror.isLinked() && farMirror.isLinked(), "essentia mirrors did not link");
        helper.assertTrue(EssentiaHandler.drainEssentia(consumer, Aspect.AIR, Direction.WEST, 8), "essentia was not drained through the mirrors");
        helper.assertValueEqual(jar.getAmount(), 9, "jar amount after one drain");
        helper.assertTrue(!nearMirror.takeFromContainer(Aspect.AIR, 2), "mirror forwarded more than one essentia");
        helper.assertValueEqual(jar.getAmount(), 9, "jar amount after refused drain");
        helper.succeed();
    }

    private static void mirrorItemLinksAndDrops(GameTestHelper helper) {
        BlockPos first = placeMirror(helper, new BlockPos(1, 1, 1), false, Direction.EAST);
        ServerLevel level = helper.getLevel();
        BlockPos firstAbsolute = helper.absolutePos(first);
        ItemStack linkedStack = new ItemStack(ModBlocks.MAGIC_MIRROR_ITEM.get());
        MirrorKeys.LINK.set(linkedStack, GlobalPos.of(level.dimension(), firstAbsolute));
        BlockPos second = new BlockPos(4, 1, 4);
        BlockPos secondAbsolute = helper.absolutePos(second);
        helper.setBlock(second.below(), Blocks.STONE);
        BlockState state = ModBlocks.MAGIC_MIRROR.get().defaultBlockState().setValue(MirrorBlock.FACING, Direction.UP);
        helper.setBlock(second, state);
        ModBlocks.MAGIC_MIRROR.get().setPlacedBy(level, secondAbsolute, state, null, linkedStack);
        MirrorBlockEntity firstMirror = helper.getBlockEntity(first, MirrorBlockEntity.class);
        MirrorBlockEntity secondMirror = helper.getBlockEntity(second, MirrorBlockEntity.class);
        helper.assertTrue(firstMirror.isLinked() && secondMirror.isLinked(), "placing the linked item did not link the mirrors");
        helper.assertTrue(firstMirror.isLinkValid(true), "first mirror link invalid");
        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(state, level, secondAbsolute, secondMirror);
        helper.assertValueEqual(drops.size(), 1, "drop count");
        GlobalPos dropLink = MirrorKeys.LINK.get(drops.get(0));
        helper.assertTrue(dropLink != null && dropLink.pos().equals(firstAbsolute), "dropped mirror does not remember its partner");
        helper.assertTrue(!firstMirror.isLinked(), "partner stayed linked after the pickup");
        helper.succeed();
    }

    private static void handMirrorSendsItem(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        BlockPos target = placeMirror(helper, new BlockPos(5, 1, 1), false, Direction.EAST);
        ItemStack handMirror = new ItemStack(ModItems.HAND_MIRROR.get());
        helper.assertTrue(!HandMirrorItem.transport(handMirror, new ItemStack(Items.APPLE), player, level), "unlinked hand mirror transported");
        MirrorKeys.LINK.set(handMirror, GlobalPos.of(level.dimension(), helper.absolutePos(target)));
        helper.assertTrue(HandMirrorItem.transport(handMirror, new ItemStack(Items.APPLE, 3), player, level), "hand mirror refused a valid target");
        helper.assertTrue(itemsNear(helper, target, 2.0).stream().anyMatch(entity -> entity.getItem().is(Items.APPLE) && entity.getItem().getCount() == 3), "item did not appear at the linked mirror");
        helper.setBlock(target, Blocks.AIR);
        helper.assertTrue(!HandMirrorItem.transport(handMirror, new ItemStack(Items.APPLE), player, level), "hand mirror transported to a missing mirror");
        helper.assertTrue(!MirrorKeys.LINK.has(handMirror), "broken link was kept");
        helper.succeed();
    }

    private static void resonatorReportsSuction(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.WARDED_JAR.get());
        JarBlockEntity jar = helper.getBlockEntity(pos, JarBlockEntity.class);
        List<Component> empty = ResonatorItem.report(jar, jar, Direction.UP);
        helper.assertValueEqual(empty.size(), 1, "messages for an empty jar");
        helper.assertTrue(empty.get(0).getContents() instanceof TranslatableContents contents && contents.getKey().equals("tc.resonator2") && contents.getArgs()[0].equals("32"), "suction message");
        jar.addToContainer(Aspect.FIRE, 5);
        List<Component> filled = ResonatorItem.report(jar, jar, Direction.UP);
        helper.assertValueEqual(filled.size(), 2, "messages for a filled jar");
        helper.assertTrue(filled.get(0).getContents() instanceof TranslatableContents contents && contents.getKey().equals("tc.resonator1") && contents.getArgs()[0].equals("5"), "essentia message");
        helper.succeed();
    }

    private static void mirrorAcceptsPipedItems(GameTestHelper helper) {
        BlockPos first = placeMirror(helper, new BlockPos(1, 1, 1), false, Direction.EAST);
        BlockPos second = placeMirror(helper, new BlockPos(5, 1, 1), false, Direction.EAST);
        MirrorBlockEntity firstMirror = helper.getBlockEntity(first, MirrorBlockEntity.class);
        firstMirror.setLinkTarget(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(second)));
        firstMirror.restoreLink(true);
        Container pipe = firstMirror.exposedItems(Direction.UP);
        helper.assertTrue(pipe != null && pipe.canPlaceItem(0, new ItemStack(Items.EMERALD)), "linked mirror refuses piped items");
        pipe.setItem(0, new ItemStack(Items.EMERALD, 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(pipe.getItem(0).isEmpty(), "piped stack was not forwarded");
            helper.assertTrue(itemsNear(helper, second, 2.0).stream().anyMatch(entity -> entity.getItem().is(Items.EMERALD)), "piped item did not come out of the second mirror");
        });
    }

    private static void handMirrorMenuTransports(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        BlockPos target = placeMirror(helper, new BlockPos(5, 1, 1), false, Direction.EAST);
        ItemStack handMirror = new ItemStack(ModItems.HAND_MIRROR.get());
        MirrorKeys.LINK.set(handMirror, GlobalPos.of(level.dimension(), helper.absolutePos(target)));
        player.getInventory().setItem(0, handMirror);
        HandMirrorMenu menu = new HandMirrorMenu(1, player.getInventory(), handMirror);
        menu.getSlot(0).set(new ItemStack(Items.GOLD_INGOT, 5));
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "transported stack stayed in the slot");
        helper.assertTrue(itemsNear(helper, target, 2.0).stream().anyMatch(entity -> entity.getItem().is(Items.GOLD_INGOT) && entity.getItem().getCount() == 5), "stack did not appear at the linked mirror");
        helper.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(ModItems.HAND_MIRROR.get())), "slot accepts a hand mirror");
        helper.succeed();
    }

    private static void deviceItemsHaveAspects(GameTestHelper helper) {
        for (ItemStack stack : List.of(
            new ItemStack(ModItems.HAND_MIRROR.get()),
            new ItemStack(ModItems.ESSENTIA_RESONATOR.get()),
            new ItemStack(ModItems.SINISTER_LODESTONE.get()),
            new ItemStack(ModBlocks.MAGIC_MIRROR_ITEM.get()),
            new ItemStack(ModBlocks.ESSENTIA_MIRROR_ITEM.get())
        )) {
            AspectList aspects = AspectHelper.getObjectTags(stack);
            helper.assertTrue(aspects != null && aspects.size() > 0, "no aspects for " + stack.getItem());
        }
        helper.succeed();
    }

    private static void sinisterConeSeesAhead(GameTestHelper helper) {
        Vec3 eye = new Vec3(0.0, 64.0, 0.0);
        Vec3 look = new Vec3(1.0, 0.0, 0.0);
        helper.assertTrue(SinisterCone.isVisibleTo(SinisterCone.APERTURE, eye, look, 10.5, 64.0, 0.5, SinisterCone.RANGE), "node ahead not seen");
        helper.assertTrue(!SinisterCone.isVisibleTo(SinisterCone.APERTURE, eye, look, -10.5, 64.0, 0.5, SinisterCone.RANGE), "node behind seen");
        helper.assertTrue(!SinisterCone.isVisibleTo(SinisterCone.APERTURE, eye, look, 10.5, 74.0, 0.5, SinisterCone.RANGE), "node far off axis seen");
        helper.assertTrue(!SinisterCone.isVisibleTo(SinisterCone.APERTURE, eye, look, 300.5, 64.0, 0.5, SinisterCone.RANGE), "node beyond range seen");
        helper.succeed();
    }
}
