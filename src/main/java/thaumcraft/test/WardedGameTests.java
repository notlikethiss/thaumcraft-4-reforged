package thaumcraft.test;

import io.netty.channel.embedded.EmbeddedChannel;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.device.ArcaneDoorItem;
import thaumcraft.device.ArcanePressurePlateBlock;
import thaumcraft.device.BannerBlock;
import thaumcraft.device.BannerBlockEntity;
import thaumcraft.device.BannerItem;
import thaumcraft.device.BrainJarBlockEntity;
import thaumcraft.device.WardedKeys;
import thaumcraft.device.HungryChestBlockEntity;
import thaumcraft.device.OwnedBlockEntity;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModItems;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class WardedGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final String[] TESTS = {
        "arcane_door_access", "arcane_pressure_plate_settings", "hungry_chest_absorbs_item", "brain_jar_collects_xp", "banner_keeps_color_and_aspect", "warded_glass_wand_pickup"
    };

    static {
        FUNCTIONS.register(TESTS[0], () -> WardedGameTests::arcaneDoorAccess);
        FUNCTIONS.register(TESTS[1], () -> WardedGameTests::pressurePlateSettings);
        FUNCTIONS.register(TESTS[2], () -> WardedGameTests::hungryChestAbsorbs);
        FUNCTIONS.register(TESTS[3], () -> WardedGameTests::brainJarCollectsXp);
        FUNCTIONS.register(TESTS[4], () -> WardedGameTests::bannerKeepsData);
        FUNCTIONS.register(TESTS[5], () -> WardedGameTests::wardedGlassWandPickup);
    }

    private WardedGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(Thaumcraft.id("warded_default"), new TestEnvironmentDefinition.AllOf());
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

    private static BlockHitResult hit(BlockPos pos, Direction side) {
        return new BlockHitResult(Vec3.atCenterOf(pos), side, pos, false);
    }

    private static void clear(ServerLevel level, BlockPos pos, int height) {
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        for (int index = 0; index < height; index++) {
            level.setBlockAndUpdate(pos.above(index), Blocks.AIR.defaultBlockState());
        }
    }

    private static ItemStack boundKey(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (WardedKeys.KEY_LINK.has(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void arcaneDoorAccess(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = mockServerPlayer(helper);
        ServerPlayer stranger = mockServerPlayer(helper);
        BlockPos base = helper.absolutePos(new BlockPos(2, 2, 2));
        clear(level, base, 3);
        ArcaneDoorItem.place(level, base, 0, owner);
        BlockPos top = base.above();
        helper.assertTrue(level.getBlockState(base).is(ModBlocks.ARCANE_DOOR.get()), "no lower door half");
        helper.assertTrue(level.getBlockState(top).getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER, "no upper door half");
        helper.assertTrue(OwnedBlockEntity.at(level, base).isOwner(owner.getUUID()), "lower half owner");
        helper.assertTrue(OwnedBlockEntity.at(level, top).isOwner(owner.getUUID()), "upper half owner");
        level.getBlockState(top).useWithoutItem(level, stranger, hit(top, Direction.NORTH));
        helper.assertFalse(level.getBlockState(base).getValue(DoorBlock.OPEN), "stranger opened the door");
        level.getBlockState(base).useWithoutItem(level, owner, hit(base, Direction.NORTH));
        helper.assertTrue(level.getBlockState(base).getValue(DoorBlock.OPEN), "owner could not open the door");
        helper.assertTrue(level.getBlockState(top).getValue(DoorBlock.OPEN), "upper half did not follow");
        level.getBlockState(base).useWithoutItem(level, owner, hit(base, Direction.NORTH));
        helper.assertFalse(level.getBlockState(base).getValue(DoorBlock.OPEN), "owner could not close the door");

        ItemStack blank = new ItemStack(ModItems.IRON_ARCANE_KEY.get(), 2);
        stranger.setItemInHand(InteractionHand.MAIN_HAND, blank);
        ModItems.IRON_ARCANE_KEY.get().onItemUseFirst(blank, new UseOnContext(level, stranger, InteractionHand.MAIN_HAND, blank, hit(base, Direction.NORTH)));
        helper.assertTrue(boundKey(stranger).isEmpty(), "stranger bound a key");
        owner.setItemInHand(InteractionHand.MAIN_HAND, blank);
        ModItems.IRON_ARCANE_KEY.get().onItemUseFirst(blank, new UseOnContext(level, owner, InteractionHand.MAIN_HAND, blank, hit(top, Direction.NORTH)));
        ItemStack bound = boundKey(owner);
        helper.assertFalse(bound.isEmpty(), "owner got no bound key");
        helper.assertValueEqual(WardedKeys.KEY_LINK.get(bound).pos(), base, "key location is the lower half");
        helper.assertValueEqual(blank.getCount(), 1, "blank key not consumed");

        stranger.setItemInHand(InteractionHand.MAIN_HAND, bound);
        ModItems.IRON_ARCANE_KEY.get().onItemUseFirst(bound, new UseOnContext(level, stranger, InteractionHand.MAIN_HAND, bound, hit(top, Direction.NORTH)));
        helper.assertTrue(OwnedBlockEntity.at(level, base).hasAccess(stranger.getUUID(), OwnedBlockEntity.IRON), "lower half access");
        helper.assertTrue(OwnedBlockEntity.at(level, top).hasAccess(stranger.getUUID(), OwnedBlockEntity.IRON), "upper half access");
        helper.assertFalse(OwnedBlockEntity.at(level, base).hasAccess(stranger.getUUID(), OwnedBlockEntity.GOLD), "iron key gave gold access");
        level.getBlockState(top).useWithoutItem(level, stranger, hit(top, Direction.NORTH));
        helper.assertTrue(level.getBlockState(base).getValue(DoorBlock.OPEN), "key holder could not open the door");

        ItemStack secondBlank = new ItemStack(ModItems.GOLD_ARCANE_KEY.get());
        stranger.setItemInHand(InteractionHand.MAIN_HAND, secondBlank);
        ModItems.GOLD_ARCANE_KEY.get().onItemUseFirst(secondBlank, new UseOnContext(level, stranger, InteractionHand.MAIN_HAND, secondBlank, hit(base, Direction.NORTH)));
        helper.assertTrue(boundKey(stranger).isEmpty(), "iron key holder bound a gold key");
        helper.succeed();
    }

    private static void pressurePlateSettings(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = mockServerPlayer(helper);
        ServerPlayer stranger = mockServerPlayer(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        clear(level, pos, 2);
        level.setBlockAndUpdate(pos, ModBlocks.ARCANE_PRESSURE_PLATE.get().defaultBlockState());
        OwnedBlockEntity owned = OwnedBlockEntity.at(level, pos);
        owned.setOwner(owner.getUUID());
        helper.assertTrue(ArcanePressurePlateBlock.counts(owner, 0, owned), "setting 0 ignores owner");
        helper.assertTrue(ArcanePressurePlateBlock.counts(stranger, 0, owned), "setting 0 ignores stranger");
        helper.assertFalse(ArcanePressurePlateBlock.counts(owner, 1, owned), "setting 1 triggers on owner");
        helper.assertTrue(ArcanePressurePlateBlock.counts(stranger, 1, owned), "setting 1 ignores stranger");
        helper.assertTrue(ArcanePressurePlateBlock.counts(owner, 2, owned), "setting 2 ignores owner");
        helper.assertFalse(ArcanePressurePlateBlock.counts(stranger, 2, owned), "setting 2 triggers on stranger");
        owned.addAccess(OwnedBlockEntity.IRON, stranger.getUUID());
        helper.assertFalse(ArcanePressurePlateBlock.counts(stranger, 1, owned), "setting 1 triggers on key holder");
        helper.assertTrue(ArcanePressurePlateBlock.counts(stranger, 2, owned), "setting 2 ignores key holder");

        BlockPos doorPos = pos.east();
        clear(level, doorPos, 3);
        ArcaneDoorItem.place(level, doorPos, 0, owner);
        helper.assertFalse(level.getBlockState(doorPos).getValue(DoorBlock.OPEN), "door starts open");
        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, new ItemStack(Items.STICK));
        level.addFreshEntity(item);
        level.getBlockState(pos).entityInside(level, pos, item, InsideBlockEffectApplier.NOOP, true);
        helper.assertTrue(level.getBlockState(pos).getValue(ArcanePressurePlateBlock.POWERED), "plate not pressed by an item");
        helper.assertTrue(level.getBlockState(doorPos).getValue(DoorBlock.OPEN), "plate did not open the neighbouring door");
        item.discard();
        level.getBlockState(pos).tick(level, pos, level.getRandom());
        helper.assertFalse(level.getBlockState(pos).getValue(ArcanePressurePlateBlock.POWERED), "plate not released");
        helper.assertFalse(level.getBlockState(doorPos).getValue(DoorBlock.OPEN), "plate did not close the neighbouring door");

        level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(ArcanePressurePlateBlock.SETTING, 2));
        ItemEntity second = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, new ItemStack(Items.STICK));
        level.addFreshEntity(second);
        level.getBlockState(pos).entityInside(level, pos, second, InsideBlockEffectApplier.NOOP, true);
        helper.assertFalse(level.getBlockState(pos).getValue(ArcanePressurePlateBlock.POWERED), "players-only plate pressed by an item");
        helper.succeed();
    }

    private static void hungryChestAbsorbs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        clear(level, pos, 2);
        level.setBlockAndUpdate(pos, ModBlocks.HUNGRY_CHEST.get().defaultBlockState());
        HungryChestBlockEntity chest = (HungryChestBlockEntity) level.getBlockEntity(pos);
        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.COBBLESTONE, 10));
        level.addFreshEntity(item);
        level.getBlockState(pos).entityInside(level, pos, item, InsideBlockEffectApplier.NOOP, true);
        helper.assertTrue(chest.getItem(0).is(Items.COBBLESTONE) && chest.getItem(0).getCount() == 10, "chest did not absorb the stack");
        helper.assertTrue(item.isRemoved(), "item entity survived");
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.DIRT, 64));
        }
        ItemEntity rejected = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.COBBLESTONE, 10));
        level.addFreshEntity(rejected);
        level.getBlockState(pos).entityInside(level, pos, rejected, InsideBlockEffectApplier.NOOP, true);
        helper.assertFalse(rejected.isRemoved(), "full chest ate an item");
        helper.assertValueEqual(rejected.getItem().getCount(), 10, "full chest changed the stack");
        helper.succeed();
    }

    private static void brainJarCollectsXp(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockServerPlayer(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        clear(level, pos, 2);
        level.setBlockAndUpdate(pos, ModBlocks.BRAIN_JAR.get().defaultBlockState());
        BrainJarBlockEntity jar = (BrainJarBlockEntity) level.getBlockEntity(pos);
        ExperienceOrb orb = new ExperienceOrb(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 17);
        level.addFreshEntity(orb);
        BrainJarBlockEntity.serverTick(level, pos, level.getBlockState(pos), jar);
        helper.assertValueEqual(jar.getXp(), 17, "jar xp");
        helper.assertTrue(orb.isRemoved(), "orb survived");
        jar.setXp(1995);
        ExperienceOrb second = new ExperienceOrb(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 17);
        level.addFreshEntity(second);
        BrainJarBlockEntity.serverTick(level, pos, level.getBlockState(pos), jar);
        BrainJarBlockEntity.serverTick(level, pos, level.getBlockState(pos), jar);
        helper.assertValueEqual(jar.getXp(), BrainJarBlockEntity.XP_MAX, "jar xp is capped");
        helper.assertValueEqual(jar.getComparatorSignal(), 15, "full comparator signal");
        jar.setXp(500);
        level.getBlockState(pos).useWithoutItem(level, player, hit(pos, Direction.UP));
        helper.assertTrue(jar.getXp() <= 500, "right click added xp");
        int after = jar.getXp();
        ExperienceOrb third = new ExperienceOrb(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 17);
        level.addFreshEntity(third);
        BrainJarBlockEntity.serverTick(level, pos, level.getBlockState(pos), jar);
        helper.assertValueEqual(jar.getXp(), after, "jar ate during the delay");
        helper.assertFalse(third.isRemoved(), "orb eaten during the delay");
        helper.succeed();
    }

    private static void bannerKeepsData(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockServerPlayer(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        clear(level, pos, 3);
        level.setBlockAndUpdate(pos, ModBlocks.BANNER.get().defaultBlockState());
        BannerBlockEntity banner = (BannerBlockEntity) level.getBlockEntity(pos);
        banner.setColor(5);
        banner.setAspect(Aspect.FIRE);
        banner.setFacing(7);
        banner.setOnWall(false);
        CompoundTag tag = banner.saveWithFullMetadata(level.registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(pos, level.getBlockState(pos), tag, level.registryAccess());
        helper.assertTrue(loaded instanceof BannerBlockEntity, "banner did not load");
        BannerBlockEntity copy = (BannerBlockEntity) loaded;
        helper.assertValueEqual(copy.getColor(), 5, "loaded color");
        helper.assertTrue(copy.getAspect() == Aspect.FIRE, "loaded aspect");
        helper.assertValueEqual(copy.getFacing(), 7, "loaded facing");

        ItemStack drop = BannerBlock.createDrop(ModBlocks.BANNER.get(), banner);
        helper.assertValueEqual(WardedKeys.BANNER_COLOR.get(drop), 5, "dropped color");
        helper.assertTrue(WardedKeys.BANNER_ASPECT.get(drop) == Aspect.FIRE, "dropped aspect");
        Component name = drop.getItem().getName(drop);
        helper.assertTrue(name.getContents() instanceof TranslatableContents contents && contents.getKey().equals("block.thaumcraft.banner.lime"), "colored banner name key");
        helper.assertTrue(ModBlocks.BANNER_ITEM.get().getName(new ItemStack(ModBlocks.BANNER_ITEM.get())).getContents() instanceof TranslatableContents plain && plain.getKey().equals("block.thaumcraft.banner"), "crimson banner name key");

        BlockPos floor = pos.east(2);
        clear(level, floor.above(), 3);
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, drop);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0.0, 0.5, 0.0), Direction.UP, floor, false);
        ModBlocks.BANNER_ITEM.get().place(new BlockPlaceContext(new UseOnContext(level, player, InteractionHand.MAIN_HAND, drop, hit)));
        BlockState placed = level.getBlockState(floor.above());
        helper.assertTrue(placed.is(ModBlocks.BANNER.get()), "banner not placed");
        BannerBlockEntity placedBanner = (BannerBlockEntity) level.getBlockEntity(floor.above());
        helper.assertValueEqual(placedBanner.getColor(), 5, "placed color");
        helper.assertTrue(placedBanner.getAspect() == Aspect.FIRE, "placed aspect");
        helper.assertFalse(placedBanner.isOnWall(), "banner on the floor is a wall banner");
        helper.succeed();
    }

    private static void wardedGlassWandPickup(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = mockServerPlayer(helper);
        ServerPlayer stranger = mockServerPlayer(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        clear(level, pos, 2);
        level.setBlockAndUpdate(pos, ModBlocks.WARDED_GLASS.get().defaultBlockState());
        ModBlocks.WARDED_GLASS.get().setPlacedBy(level, pos, level.getBlockState(pos), owner, new ItemStack(ModBlocks.WARDED_GLASS.get()));
        helper.assertTrue(OwnedBlockEntity.at(level, pos).isOwner(owner.getUUID()), "glass owner");
        ModBlocks.WARDED_GLASS.get().onWandUseOn(level, pos, stranger, ItemStack.EMPTY, Direction.UP);
        helper.assertTrue(level.getBlockState(pos).is(ModBlocks.WARDED_GLASS.get()), "stranger picked the glass up");
        CompoundTag tag = OwnedBlockEntity.at(level, pos).saveWithFullMetadata(level.registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(pos, level.getBlockState(pos), tag, level.registryAccess());
        helper.assertTrue(loaded instanceof OwnedBlockEntity restored && restored.isOwner(owner.getUUID()), "owner lost on reload");
        ModBlocks.WARDED_GLASS.get().onWandUseOn(level, pos, owner, ItemStack.EMPTY, Direction.UP);
        helper.assertTrue(level.getBlockState(pos).isAir(), "owner could not pick the glass up");
        helper.succeed();
    }
}
