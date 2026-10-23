package thaumcraft.test;

import io.netty.channel.embedded.EmbeddedChannel;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.block.LiquidDeathBlock;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModFluids;
import thaumcraft.registry.ModEffects;
import thaumcraft.damage.ModDamageTypes;
import thaumcraft.research.Warp;
import thaumcraft.research.WarpType;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class FluidGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final String[] TESTS = {
        "liquid_death_horizontal_quanta",
        "liquid_death_vertical_quanta",
        "purifying_fluid_no_source_regeneration",
        "sanitizing_soap_release_threshold",
        "purifying_fluid_ward_consumes_source",
        "bath_salts_expire_in_water",
        "purifying_bucket_round_trip",
        "liquid_death_bucket_round_trip",
        "liquid_death_yields_essence",
        "sanitizing_soap_full_use_once",
        "sanitizing_soap_full_lifecycle_once"
    };

    static {
        FUNCTIONS.register(TESTS[0], () -> FluidGameTests::liquidDeathHorizontalQuanta);
        FUNCTIONS.register(TESTS[1], () -> FluidGameTests::liquidDeathVerticalQuanta);
        FUNCTIONS.register(TESTS[2], () -> FluidGameTests::purifyingFluidNoSourceRegeneration);
        FUNCTIONS.register(TESTS[3], () -> FluidGameTests::sanitizingSoapReleaseThreshold);
        FUNCTIONS.register(TESTS[4], () -> FluidGameTests::purifyingFluidWardConsumesSource);
        FUNCTIONS.register(TESTS[5], () -> FluidGameTests::bathSaltsExpireInWater);
        FUNCTIONS.register(TESTS[6], () -> FluidGameTests::purifyingBucketRoundTrip);
        FUNCTIONS.register(TESTS[7], () -> FluidGameTests::liquidDeathBucketRoundTrip);
        FUNCTIONS.register(TESTS[8], () -> FluidGameTests::liquidDeathYieldsEssence);
        FUNCTIONS.register(TESTS[9], () -> FluidGameTests::sanitizingSoapFullUseOnce);
        FUNCTIONS.register(TESTS[10], () -> FluidGameTests::sanitizingSoapFullLifecycleOnce);
    }

    private FluidGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Thaumcraft.id("fluid_default"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name));
            event.registerTest(Thaumcraft.id(name), new FunctionGameTestInstance(
                function,
                new TestData<>(environment, Thaumcraft.id("test/platform"), 240, 0, true)
            ));
        }
    }

    private static void liquidDeathHorizontalQuanta(GameTestHelper helper) {
        BlockPos source = new BlockPos(2, 2, 2);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                helper.setBlock(source.offset(x, -1, z), Blocks.STONE);
            }
        }
        helper.setBlock(source, ModBlocks.LIQUID_DEATH.get());
        helper.runAfterDelay(8, () -> {
            boolean spread = false;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    spread |= (x != 0 || z != 0) && quanta(helper.getBlockState(source.offset(x, 0, z))) > 0;
                }
            }
            helper.assertValueEqual(totalQuanta(helper, source, -1, 1, -1, 1, -1, 1), LiquidDeathBlock.QUANTA_PER_BLOCK, "scheduled horizontal flow changed liquid death quanta");
            helper.assertTrue(spread, "scheduled horizontal tick did not move liquid death");
            helper.runAfterDelay(15, () -> {
                helper.assertValueEqual(totalQuanta(helper, source, -2, 2, -1, 1, -2, 2), LiquidDeathBlock.QUANTA_PER_BLOCK, "repeated scheduled horizontal ticks duplicated or lost liquid death quanta");
                helper.succeed();
            });
        });
    }

    private static void liquidDeathVerticalQuanta(GameTestHelper helper) {
        BlockPos source = new BlockPos(2, 3, 2);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                helper.setBlock(source.offset(x, -2, z), Blocks.STONE);
            }
        }
        helper.setBlock(source, ModBlocks.LIQUID_DEATH.get());
        helper.runAfterDelay(8, () -> {
            helper.assertValueEqual(totalQuanta(helper, source, -1, 1, -2, 0, -1, 1), LiquidDeathBlock.QUANTA_PER_BLOCK, "scheduled vertical flow changed liquid death quanta");
            helper.assertValueEqual(quanta(helper.getBlockState(source.below())), LiquidDeathBlock.QUANTA_PER_BLOCK, "scheduled vertical tick did not move all liquid death quanta");
            helper.runAfterDelay(15, () -> {
                helper.assertValueEqual(totalQuanta(helper, source, -2, 2, -2, 0, -2, 2), LiquidDeathBlock.QUANTA_PER_BLOCK, "repeated scheduled vertical ticks duplicated or lost liquid death quanta");
                helper.succeed();
            });
        });
    }

    private static void purifyingFluidNoSourceRegeneration(GameTestHelper helper) {
        BlockPos left = new BlockPos(1, 3, 2);
        BlockPos center = new BlockPos(2, 3, 2);
        BlockPos right = new BlockPos(3, 3, 2);
        helper.setBlock(left.below(), Blocks.STONE);
        helper.setBlock(center.below(), Blocks.STONE);
        helper.setBlock(right.below(), Blocks.STONE);
        helper.setBlock(left, ModBlocks.PURIFYING_FLUID.get());
        helper.setBlock(center, ModBlocks.PURIFYING_FLUID.get());
        helper.setBlock(right, ModBlocks.PURIFYING_FLUID.get());
        helper.setBlock(center, Blocks.AIR);
        helper.runAfterDelay(8, () -> {
            BlockState state = helper.getBlockState(center);
            helper.assertFalse(state.getFluidState().isSource(), "purifying fluid recreated a consumed source between adjacent sources");
            helper.assertTrue(state.getFluidState().isEmpty() || state.getFluidState().getType() == ModFluids.FLOWING_PURIFYING_FLUID.get(), "consumed purifying source became an unrelated fluid");
            helper.succeed();
        });
    }

    private static void sanitizingSoapReleaseThreshold(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        Warp.set(player, 0, WarpType.STICKY);
        Warp.set(player, 4, WarpType.TEMPORARY);
        ItemStack soap = new ItemStack(ModItems.SANITIZING_SOAP.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, soap);
        player.startUsingItem(InteractionHand.MAIN_HAND);
        ModItems.SANITIZING_SOAP.get().onUseTick(helper.getLevel(), player, soap, 5);
        helper.assertValueEqual(soap.getCount(), 2, "soap was consumed before 196 use ticks");
        helper.assertValueEqual(Warp.get(player, WarpType.TEMPORARY), 4, "soap cleared temporary warp before 196 use ticks");
        ModItems.SANITIZING_SOAP.get().onUseTick(helper.getLevel(), player, soap, 4);
        helper.assertValueEqual(soap.getCount(), 1, "soap was not consumed at 196 use ticks");
        helper.assertValueEqual(Warp.get(player, WarpType.TEMPORARY), 0, "soap did not clear temporary warp at 196 use ticks");
        helper.succeed();
    }

    private static void purifyingFluidWardConsumesSource(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, ModBlocks.PURIFYING_FLUID.get().defaultBlockState());
        ServerPlayer player = mockServerPlayer(helper);
        player.snapTo(Vec3.atCenterOf(pos), 0.0F, 0.0F);
        Warp.set(player, 9, WarpType.PERMANENT);
        level.getBlockState(pos).entityInside(level, pos, player, net.minecraft.world.entity.InsideBlockEffectApplier.NOOP, true);
        helper.assertTrue(player.hasEffect(ModEffects.WARP_WARD), "purifying source did not apply ward");
        helper.assertTrue(level.getBlockState(pos).isAir(), "purifying source was not consumed by ward application");
        helper.succeed();
    }

    private static void bathSaltsExpireInWater(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        ItemEntity salts = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModItems.BATH_SALTS.get()));
        level.addFreshEntity(salts);
        salts.makeFakeItem();
        salts.tick();
        helper.assertTrue(level.getBlockState(pos).is(ModBlocks.PURIFYING_FLUID.get()), "expired bath salts did not transform source water");
        helper.succeed();
    }

    private static void purifyingBucketRoundTrip(GameTestHelper helper) {
        bucketRoundTrip(helper, ModItems.PURIFYING_FLUID_BUCKET.get(), ModBlocks.PURIFYING_FLUID.get());
    }

    private static void liquidDeathBucketRoundTrip(GameTestHelper helper) {
        bucketRoundTrip(helper, ModItems.LIQUID_DEATH_BUCKET.get(), ModBlocks.LIQUID_DEATH.get());
    }

    private static void bucketRoundTrip(GameTestHelper helper, BucketItem fluidBucket, net.minecraft.world.level.block.Block fluidBlock) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        ServerPlayer player = mockServerPlayer(helper);
        player.snapTo(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 0.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(fluidBucket));
        InteractionResult placed = fluidBucket.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(placed.consumesAction(), "fluid bucket did not place its fluid");
        helper.assertTrue(level.getBlockState(pos).is(fluidBlock), "fluid bucket placed an unexpected block");
        helper.assertTrue(level.getFluidState(pos).isSource(), "placed fluid bucket did not create a source");
        player.snapTo(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 0.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        InteractionResult pickedUp = Items.BUCKET.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(pickedUp.consumesAction(), "empty bucket did not pick up the source");
        helper.assertTrue(level.getBlockState(pos).isAir(), "bucket pickup did not remove the source block");
        helper.assertTrue(
            pickedUp instanceof InteractionResult.Success success
                && success.heldItemTransformedTo() != null
                && success.heldItemTransformedTo().is(fluidBucket),
            "bucket pickup returned the wrong fluid bucket"
        );
        helper.succeed();
    }

    private static void liquidDeathYieldsEssence(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos relativePos = new BlockPos(2, 2, 2);
        BlockPos pos = helper.absolutePos(relativePos);
        Pig pig = new Pig(EntityType.PIG, level);
        pig.getRandom().setSeed(0L);
        pig.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(pig);
        pig.hurtServer(level, level.damageSources().source(ModDamageTypes.DISSOLVE), 100.0F);
        helper.assertItemEntityPresent(ModItems.CRYSTALLIZED_ESSENCE.get(), relativePos, 6.0);
        helper.assertItemEntityPresent(Items.PORKCHOP, relativePos, 6.0);
        helper.assertTrue(pig.isDeadOrDying(), "dissolving liquid death damage did not kill the mob");
        helper.succeed();
    }

    private static void sanitizingSoapFullUseOnce(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        Warp.set(player, 0, WarpType.STICKY);
        Warp.set(player, 4, WarpType.TEMPORARY);
        ItemStack soap = new ItemStack(ModItems.SANITIZING_SOAP.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, soap);
        ModItems.SANITIZING_SOAP.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        for (int elapsed = 1; elapsed < 196; elapsed++) {
            ModItems.SANITIZING_SOAP.get().onUseTick(helper.getLevel(), player, soap, 200 - elapsed);
        }
        helper.assertValueEqual(soap.getCount(), 2, "full soap use consumed before its completion threshold");
        ModItems.SANITIZING_SOAP.get().onUseTick(helper.getLevel(), player, soap, 4);
        helper.assertValueEqual(soap.getCount(), 1, "completed soap use did not consume exactly one item");
        helper.assertValueEqual(Warp.get(player, WarpType.TEMPORARY), 0, "completed soap use did not clear temporary warp");
        helper.assertFalse(player.isUsingItem(), "soap use did not stop at its completion threshold");
        helper.succeed();
    }

    private static void sanitizingSoapFullLifecycleOnce(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        Warp.set(player, 0, WarpType.STICKY);
        Warp.set(player, 4, WarpType.TEMPORARY);
        ItemStack soap = new ItemStack(ModItems.SANITIZING_SOAP.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, soap);
        ModItems.SANITIZING_SOAP.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        boolean[] reachedPreCompletionTick = {false};
        helper.onEachTick(() -> {
            if (player.isUsingItem()) {
                player.doTick();
                if (player.isUsingItem() && player.getUseItemRemainingTicks() == 4) {
                    helper.assertValueEqual(soap.getCount(), 2, "soap lifecycle consumed the item before its completion update");
                    reachedPreCompletionTick[0] = true;
                }
                return;
            }
            if (reachedPreCompletionTick[0]) {
                helper.assertValueEqual(soap.getCount(), 1, "soap lifecycle did not consume exactly one item at completion");
                helper.assertValueEqual(Warp.get(player, WarpType.TEMPORARY), 0, "soap lifecycle did not clear temporary warp");
                player.releaseUsingItem();
                helper.assertValueEqual(soap.getCount(), 1, "soap release or finish consumed the item twice");
                helper.succeed();
            }
        });
    }

    private static int quanta(BlockState state) {
        if (!(state.getBlock() instanceof LiquidDeathBlock)) {
            return 0;
        }
        int level = state.getValue(LiquidBlock.LEVEL);
        return level == 0 ? LiquidDeathBlock.QUANTA_PER_BLOCK : Math.max(0, LiquidDeathBlock.QUANTA_PER_BLOCK - level);
    }

    private static int totalQuanta(GameTestHelper helper, BlockPos origin, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        int total = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    total += quanta(helper.getBlockState(origin.offset(x, y, z)));
                }
            }
        }
        return total;
    }

    private static ServerPlayer mockServerPlayer(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
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
}
