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
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.blockentity.WardedBlockEntity;
import thaumcraft.infusion.InfusionKeys;
import thaumcraft.item.gear.RepairEvents;
import thaumcraft.item.gear.RunicShield;
import thaumcraft.item.gear.SelfRepair;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModEnchantments;
import thaumcraft.registry.ModItems;
import thaumcraft.wand.WandRegistry;
import thaumcraft.wand.Wands;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class GearGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final String[] TESTS = {"runic_shield_absorbs_damage", "void_gear_self_repairs", "repair_enchantment_spends_vis", "portable_hole_spends_vis", "warding_focus_wards_and_unwards"};

    static {
        FUNCTIONS.register(TESTS[0], () -> GearGameTests::runicShieldAbsorbs);
        FUNCTIONS.register(TESTS[1], () -> GearGameTests::voidGearSelfRepairs);
        FUNCTIONS.register(TESTS[2], () -> GearGameTests::repairEnchantmentSpendsVis);
        FUNCTIONS.register(TESTS[3], () -> GearGameTests::portableHoleSpendsVis);
        FUNCTIONS.register(TESTS[4], () -> GearGameTests::wardingFocus);
    }

    private GearGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(Thaumcraft.id("gear_default"), new TestEnvironmentDefinition.AllOf());
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

    private static ItemStack filledWand() {
        ItemStack wand = Wands.create(WandRegistry.CAP_GOLD, WandRegistry.ROD_GREATWOOD, false);
        Wands.fill(wand);
        return wand;
    }

    private static void runicShieldAbsorbs(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        ItemStack chest = new ItemStack(ModItems.VOID_CHESTPLATE.get());
        InfusionKeys.HARDEN.set(chest, 2);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        ItemStack wand = filledWand();
        player.getInventory().setItem(0, wand);
        int air = Wands.getVis(wand, Aspect.AIR);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertValueEqual(RunicShield.getCharge(player), 1, "charge after first tick");
        helper.assertTrue(Wands.getVis(player.getInventory().getItem(0), Aspect.AIR) < air, "recharge spent no air vis");
        float health = player.getHealth();
        ServerLevel level = helper.getLevel();
        player.hurtServer(level, level.damageSources().genericKill(), 1.0F);
        helper.assertValueEqual(player.getHealth(), health, "health after absorbed hit");
        helper.assertValueEqual(RunicShield.getCharge(player), 0, "charge after hit");
        player.hurtServer(level, level.damageSources().genericKill(), 1.0F);
        helper.assertTrue(player.getHealth() < health, "unshielded hit was absorbed");
        helper.succeed();
    }

    private static void voidGearSelfRepairs(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        ItemStack sword = new ItemStack(ModItems.VOID_SWORD.get());
        sword.setDamageValue(10);
        SelfRepair.tick(sword, player);
        helper.assertValueEqual(sword.getDamageValue(), 9, "void sword damage");
        helper.succeed();
    }

    private static void repairEnchantmentSpendsVis(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        ItemStack sword = new ItemStack(ModItems.THAUMIUM_SWORD.get());
        sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.REPAIR), 1);
        sword.setDamageValue(10);
        ItemStack wand = filledWand();
        player.getInventory().setItem(1, wand);
        int total = Wands.getAllVis(wand).visSize();
        RepairEvents.doRepair(sword, player);
        helper.assertValueEqual(sword.getDamageValue(), 9, "repaired damage");
        helper.assertTrue(Wands.getAllVis(player.getInventory().getItem(1)).visSize() < total, "repair spent no vis");
        helper.succeed();
    }

    private static void portableHoleSpendsVis(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        BlockPos bottom = helper.absolutePos(new BlockPos(1, 2, 1));
        ServerLevel level = helper.getLevel();
        for (int index = 0; index < 3; index++) {
            level.setBlockAndUpdate(bottom.above(index), Blocks.STONE.defaultBlockState());
        }
        level.setBlockAndUpdate(bottom.above(3), Blocks.AIR.defaultBlockState());
        ItemStack wand = filledWand();
        Wands.setFocus(wand, new ItemStack(ModItems.FOCUS_PORTABLE_HOLE.get()));
        int entropy = Wands.getVis(wand, Aspect.ENTROPY);
        BlockPos top = bottom.above(2);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(top).add(0.0, 0.5, 0.0), Direction.UP, top, false);
        ModItems.FOCUS_PORTABLE_HOLE.get().onFocusRightClick(wand, level, player, hit);
        helper.assertTrue(level.getBlockState(top).is(ModBlocks.HOLE.get()), "no hole at clicked block");
        helper.assertTrue(Wands.getVis(wand, Aspect.ENTROPY) < entropy, "hole spent no entropy vis");
        helper.succeed();
    }

    private static void wardingFocus(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        ItemStack wand = filledWand();
        Wands.setFocus(wand, new ItemStack(ModItems.FOCUS_WARDING.get()));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0.0, 0.5, 0.0), Direction.UP, pos, false);
        int order = Wands.getVis(wand, Aspect.ORDER);
        ModItems.FOCUS_WARDING.get().onFocusRightClick(wand, level, player, hit);
        helper.assertTrue(level.getBlockState(pos).is(ModBlocks.WARDED_BLOCK.get()), "block not warded");
        helper.assertTrue(level.getBlockEntity(pos) instanceof WardedBlockEntity warded && warded.getDisguise().is(Blocks.STONE), "disguise is not stone");
        helper.assertTrue(Wands.getVis(wand, Aspect.ORDER) < order, "warding spent no order vis");
        try {
            Thread.sleep(550L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        ModItems.FOCUS_WARDING.get().onFocusRightClick(wand, level, player, hit);
        helper.assertTrue(level.getBlockState(pos).is(Blocks.STONE), "block not restored");
        helper.succeed();
    }
}
