package thaumcraft.test;

import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.RobeArmorDyeRecipe;
import thaumcraft.item.armor.Hover;
import thaumcraft.item.armor.ArmorEvents;
import thaumcraft.item.armor.RobeColor;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModEnchantments;
import thaumcraft.research.Warp;
import thaumcraft.wand.WandManager;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ArmorGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final String[] TESTS = {"armor_vis_discount_sums", "armor_warp_from_gear", "robe_dye_mixes_color", "hover_harness_burns_fuel", "traveller_boots_elytra_no_thrust"};

    static {
        FUNCTIONS.register(TESTS[0], () -> ArmorGameTests::visDiscountSums);
        FUNCTIONS.register(TESTS[1], () -> ArmorGameTests::warpFromGear);
        FUNCTIONS.register(TESTS[2], () -> ArmorGameTests::robeDyeMixesColor);
        FUNCTIONS.register(TESTS[3], () -> ArmorGameTests::hoverBurnsFuel);
        FUNCTIONS.register(TESTS[4], () -> ArmorGameTests::travellerElytra);
    }

    private ArmorGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(Thaumcraft.id("armor_default"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name));
            event.registerTest(
                Thaumcraft.id(name),
                new FunctionGameTestInstance(function, new TestData<>(environment, Thaumcraft.id("test/platform"), 100, 0, true))
            );
        }
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

    private static void visDiscountSums(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.ROBE_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.ROBE_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.ROBE_BOOTS.get()));
        helper.assertValueEqual(Math.round(WandManager.getTotalVisDiscount(player, null) * 100.0F), 5, "robe discount");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.GOGGLES_OF_REVEALING.get()));
        helper.assertValueEqual(Math.round(WandManager.getTotalVisDiscount(player, null) * 100.0F), 10, "robe and goggles discount");
        helper.succeed();
    }

    private static void warpFromGear(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        helper.assertValueEqual(Warp.getWarpFromGear(player), 0, "bare warp");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.VOID_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.VOID_ROBE_CHESTPLATE.get()));
        helper.assertValueEqual(Warp.getWarpFromGear(player), 3, "void gear warp");
        helper.succeed();
    }

    private static void robeDyeMixesColor(GameTestHelper helper) {
        ItemStack robe = new ItemStack(ModItems.ROBE_CHESTPLATE.get());
        CraftingInput input = CraftingInput.of(2, 1, List.of(robe, new ItemStack(Items.RED_DYE)));
        helper.assertTrue(RobeArmorDyeRecipe.INSTANCE.matches(input, helper.getLevel()), "recipe does not match");
        ItemStack result = RobeArmorDyeRecipe.INSTANCE.assemble(input);
        helper.assertTrue(result.has(DataComponents.DYED_COLOR), "no color applied");
        int color = RobeColor.get(result);
        int red = color >> 16 & 0xFF;
        int blue = color & 0xFF;
        helper.assertTrue(red > 0x6A && blue < 0x80, "color not reddened");
        CraftingInput invalid = CraftingInput.of(2, 1, List.of(new ItemStack(ModItems.VOID_ROBE_CHESTPLATE.get()), new ItemStack(Items.RED_DYE)));
        helper.assertFalse(RobeArmorDyeRecipe.INSTANCE.matches(invalid, helper.getLevel()), "void robe matched robe recipe");
        helper.succeed();
    }

    private static void hoverBurnsFuel(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        ItemStack armor = new ItemStack(ModItems.HOVER_HARNESS.get());
        ItemStack jar = new ItemStack(ModBlocks.FILLED_JAR.get());
        AspectKeys.ASPECTS.set(jar, new AspectList().add(Aspect.ENERGY, 2));
        helper.assertTrue(Hover.isFuel(jar), "jar is not fuel");
        Hover.setJar(armor, jar);
        player.setItemSlot(EquipmentSlot.CHEST, armor);
        armor = player.getItemBySlot(EquipmentSlot.CHEST);
        helper.assertValueEqual(Hover.getHarnessFuel(armor), 2, "fuel");
        for (int tick = 0; tick < Hover.EFFICIENCY; tick++) {
            helper.assertTrue(Hover.expendCharge(player, armor), "charge ended early");
        }
        helper.assertValueEqual(Hover.getHarnessFuel(armor), 2, "fuel before burn");
        helper.assertTrue(Hover.expendCharge(player, armor), "no fuel left after burn");
        helper.assertValueEqual(Hover.getHarnessFuel(armor), 1, "fuel after burn");
        Hover.toggle(player);
        helper.assertTrue(Hover.isHovering(armor), "hover not toggled");
        helper.succeed();
    }

    private static void travellerElytra(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        player.setLastClientInput(new Input(true, false, false, false, false, false, false));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.BOOTS_TRAVELLER.get()));
        var step = player.getAttribute(Attributes.STEP_HEIGHT);
        double originalStep = step.getValue();
        player.setOnGround(true);
        player.setDeltaMovement(Vec3.ZERO);
        ArmorEvents.onPlayerTick(new PlayerTickEvent.Pre(player));
        helper.assertTrue(player.getDeltaMovement().horizontalDistance() > 0.05, "boots must still accelerate walking");
        helper.assertTrue(step.getValue() > originalStep, "boots must still increase step height");
        ArmorEvents.onJump(new LivingEvent.LivingJumpEvent(player));
        helper.assertTrue(player.getDeltaMovement().y > 0.27, "boots must still boost jumping");
        player.setOnGround(false);
        player.startFallFlying();
        Vec3 gliding = new Vec3(0.4, -0.05, 0.8);
        for (var boots : List.of(Items.LEATHER_BOOTS, ModItems.BOOTS_TRAVELLER.get())) {
            for (int haste : new int[]{0, 3}) {
                ItemStack stack = new ItemStack(boots);
                if (haste > 0) {
                    stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.HASTE), haste);
                }
                player.setItemSlot(EquipmentSlot.FEET, stack);
                player.setDeltaMovement(gliding);
                for (int tick = 0; tick < 80; tick++) {
                    ArmorEvents.onPlayerTick(new PlayerTickEvent.Pre(player));
                }
                helper.assertTrue(player.getDeltaMovement().equals(gliding), "boots/haste added elytra thrust: " + boots + " haste=" + haste);
                helper.assertTrue(Math.abs(step.getValue() - originalStep) < 1.0E-6, "gliding must remove the step modifier");
            }
        }
        player.stopFallFlying();
        player.fallDistance = 3;
        ArmorEvents.onPlayerTick(new PlayerTickEvent.Pre(player));
        helper.assertTrue(player.fallDistance < 3, "boots must still reduce fall distance");
        helper.succeed();
    }
}
