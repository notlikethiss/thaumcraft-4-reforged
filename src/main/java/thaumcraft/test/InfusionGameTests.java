package thaumcraft.test;

import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.util.List;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.InfusionRunicAugmentRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.essentia.JarBlockEntity;
import thaumcraft.infusion.InfusionMatrixBlockEntity;
import thaumcraft.infusion.InfusionPillarBlockEntity;
import thaumcraft.infusion.InfusionTriggers;
import thaumcraft.infusion.PedestalBlockEntity;
import thaumcraft.registry.ModBlocks;
import thaumcraft.research.ResearchManager;
import thaumcraft.wand.WandRegistry;
import thaumcraft.wand.Wands;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class InfusionGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final String[] TESTS = {
        "infusion_altar_forms", "infusion_stability_symmetry", "infusion_without_essentia", "infusion_with_jars",
        "infusion_zephyr_sword_with_floor_jars", "infusion_layout_9x9", "infusion_layout_11x11", "infusion_layout_13x13",
        "infusion_layout_17x17"
    };
    private static final BlockPos[] RING = {
        new BlockPos(1, 1, 3), new BlockPos(5, 1, 3), new BlockPos(3, 1, 1), new BlockPos(3, 1, 5),
        new BlockPos(1, 1, 1), new BlockPos(5, 1, 5), new BlockPos(1, 1, 5), new BlockPos(5, 1, 1)
    };
    private static final BlockPos[] JARS = {
        new BlockPos(0, 4, 0), new BlockPos(6, 4, 0), new BlockPos(0, 4, 6), new BlockPos(6, 4, 6),
        new BlockPos(0, 5, 3), new BlockPos(6, 5, 3)
    };

    static {
        FUNCTIONS.register(TESTS[0], () -> InfusionGameTests::altarForms);
        FUNCTIONS.register(TESTS[1], () -> InfusionGameTests::stabilitySymmetry);
        FUNCTIONS.register(TESTS[2], () -> InfusionGameTests::withoutEssentia);
        FUNCTIONS.register(TESTS[3], () -> InfusionGameTests::withJars);
        FUNCTIONS.register(TESTS[4], () -> InfusionGameTests::zephyrSwordWithFloorJars);
        FUNCTIONS.register(TESTS[5], () -> helper -> pedestalLayout(helper, 4));
        FUNCTIONS.register(TESTS[6], () -> helper -> pedestalLayout(helper, 5));
        FUNCTIONS.register(TESTS[7], () -> helper -> pedestalLayout(helper, 6));
        FUNCTIONS.register(TESTS[8], () -> helper -> pedestalLayout(helper, 8));
    }

    private InfusionGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        if (TESTS.length == 0) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
            Thaumcraft.id("infusion_default"),
            new TestEnvironmentDefinition.AllOf()
        );
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name));
            event.registerTest(
                Thaumcraft.id(name),
                new FunctionGameTestInstance(function, new TestData<>(environment,
                    Thaumcraft.id(name.startsWith("infusion_layout_") ? "test/eldritch_regression" : "test/platform"), 200, 0, true))
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

    private static void buildFrame(GameTestHelper helper) {
        buildFrame(helper, BlockPos.ZERO);
    }

    private static void buildFrame(GameTestHelper helper, BlockPos offset) {
        helper.setBlock(new BlockPos(3, 1, 3).offset(offset), ModBlocks.ARCANE_PEDESTAL.get());
        helper.setBlock(new BlockPos(3, 3, 3).offset(offset), ModBlocks.RUNIC_MATRIX.get());
        for (int x = 2; x <= 4; x += 2) {
            for (int z = 2; z <= 4; z += 2) {
                helper.setBlock(new BlockPos(x, 1, z).offset(offset), ModBlocks.ARCANE_STONE_BRICKS.get());
                helper.setBlock(new BlockPos(x, 2, z).offset(offset), ModBlocks.ARCANE_STONE.get());
            }
        }
    }

    private static void form(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        InfusionTriggers.replace(helper.getLevel(), origin);
    }

    private static void equipWand(ServerPlayer player) {
        ItemStack wand = Wands.create(WandRegistry.CAP_GOLD, WandRegistry.ROD_SILVERWOOD, false);
        Wands.fill(wand);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
    }

    private static void clickMatrix(GameTestHelper helper, ServerPlayer player) {
        clickMatrix(helper, player, new BlockPos(3, 3, 3));
    }

    private static void clickMatrix(GameTestHelper helper, ServerPlayer player, BlockPos relativePos) {
        BlockPos matrixPos = helper.absolutePos(relativePos);
        helper.assertTrue(player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(matrixPos), Direction.UP, matrixPos, false)).consumesAction(), "wand click was not handled");
    }

    private static void altarForms(GameTestHelper helper) {
        buildFrame(helper);
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.assertTrue(InfusionTriggers.fits(helper.getLevel(), origin), "blueprint does not fit");
        ServerPlayer player = mockServerPlayer(helper);
        ResearchManager.complete(player, "INFUSION");
        equipWand(player);
        clickMatrix(helper, player);
        InfusionMatrixBlockEntity matrix = helper.getBlockEntity(new BlockPos(3, 3, 3), InfusionMatrixBlockEntity.class);
        helper.assertTrue(matrix.isActive(), "matrix not active");
        helper.assertTrue(matrix.validLocation(), "structure invalid");
        helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 2)).is(ModBlocks.INFUSION_PILLAR_TOP.get()), "no pillar top");
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 1, 4)).is(ModBlocks.INFUSION_PILLAR.get()), "no pillar");
        helper.assertValueEqual(helper.getBlockEntity(new BlockPos(4, 1, 4), InfusionPillarBlockEntity.class).getOrientation(), 5, "orientation");
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            helper.assertValueEqual(Wands.getVis(player.getMainHandItem(), aspect), 7500, "formation vis cost for " + aspect);
        }
        helper.succeed();
    }

    private static void stabilitySymmetry(GameTestHelper helper) {
        buildFrame(helper);
        form(helper);
        InfusionMatrixBlockEntity matrix = helper.getBlockEntity(new BlockPos(3, 3, 3), InfusionMatrixBlockEntity.class);
        matrix.recalculateSurroundings();
        helper.assertValueEqual(matrix.getSymmetry(), 0, "empty symmetry");
        helper.setBlock(RING[0], ModBlocks.ARCANE_PEDESTAL.get());
        helper.getBlockEntity(RING[0], PedestalBlockEntity.class).setStack(new ItemStack(Items.STICK));
        matrix.recalculateSurroundings();
        helper.assertValueEqual(matrix.getSymmetry(), 3, "single pedestal symmetry");
        helper.setBlock(RING[1], ModBlocks.ARCANE_PEDESTAL.get());
        helper.getBlockEntity(RING[1], PedestalBlockEntity.class).setStack(new ItemStack(Items.STICK));
        matrix.recalculateSurroundings();
        helper.assertValueEqual(matrix.getSymmetry(), 0, "mirrored pedestal symmetry");
        Block candle = ModBlocks.CANDLES.values().iterator().next().get();
        for (int index = 0; index < 20; index++) {
            helper.setBlock(new BlockPos(0, 1 + index / 7, index % 7), candle);
        }
        matrix.recalculateSurroundings();
        helper.assertValueEqual(matrix.getSymmetry(), 2, "candle symmetry");
        helper.succeed();
    }

    private static @Nullable InfusionRecipe usableRecipe() {
        for (InfusionRecipe recipe : TcRecipes.all(InfusionRecipe.class)) {
            if (InfusionRunicAugmentRecipe.isDisplayRecipe(recipe.key()) || recipe.research().isEmpty()
                || !recipe.central().exists() || recipe.components().size() > RING.length || recipe.central().displayStacks().isEmpty()) {
                continue;
            }
            boolean usable = true;
            for (TcIngredient ingredient : recipe.components()) {
                usable &= ingredient.exists() && !ingredient.displayStacks().isEmpty();
            }
            if (usable && recipe.result().exists() && recipe.aspects().size() <= JARS.length) {
                return recipe;
            }
        }
        return null;
    }

    private static InfusionRecipe prepare(GameTestHelper helper, ServerPlayer player) {
        InfusionRecipe chosen = usableRecipe();
        helper.assertTrue(chosen != null, "no usable infusion recipe");
        prepare(helper, player, chosen);
        return chosen;
    }

    private static void prepare(GameTestHelper helper, ServerPlayer player, InfusionRecipe chosen) {
        ResearchManager.complete(player, chosen.research());
        ResearchManager.complete(player, "INFUSION");
        buildFrame(helper);
        equipWand(player);
        clickMatrix(helper, player);
        helper.assertTrue(helper.getBlockEntity(new BlockPos(3, 3, 3), InfusionMatrixBlockEntity.class).validLocation(), "altar did not form through wand interaction");
        ItemStack central = chosen.central().displayStacks().get(0).copyWithCount(1);
        helper.getBlockEntity(new BlockPos(3, 1, 3), PedestalBlockEntity.class).setStack(central);
        for (int index = 0; index < chosen.components().size(); index++) {
            helper.setBlock(RING[index], ModBlocks.ARCANE_PEDESTAL.get());
            helper.getBlockEntity(RING[index], PedestalBlockEntity.class).setStack(chosen.components().get(index).displayStacks().get(0).copyWithCount(1));
        }
    }

    private static InfusionMatrixBlockEntity start(GameTestHelper helper, ServerPlayer player) {
        InfusionMatrixBlockEntity matrix = helper.getBlockEntity(new BlockPos(3, 3, 3), InfusionMatrixBlockEntity.class);
        matrix.recalculateSurroundings();
        clickMatrix(helper, player);
        helper.assertTrue(matrix.isCrafting(), "crafting did not start");
        return matrix;
    }

    private static void run(GameTestHelper helper, InfusionMatrixBlockEntity matrix, int ticks) {
        BlockPos matrixPos = matrix.getBlockPos();
        for (int tick = 0; tick < ticks && (tick == 0 || matrix.isCrafting()); tick++) {
            InfusionMatrixBlockEntity.tick(helper.getLevel(), matrixPos, matrix.getBlockState(), matrix);
        }
    }

    private static void withoutEssentia(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        InfusionRecipe chosen = prepare(helper, player);
        ItemStack central = chosen.central().displayStacks().get(0);
        InfusionMatrixBlockEntity matrix = start(helper, player);
        run(helper, matrix, 600);
        PedestalBlockEntity center = helper.getBlockEntity(new BlockPos(3, 1, 3), PedestalBlockEntity.class);
        helper.assertTrue(center.getStack().is(central.getItem()), "central item changed without essentia");
        helper.succeed();
    }

    private static void withJars(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        InfusionRecipe chosen = prepare(helper, player);
        craftWithJars(helper, player, chosen, false);
    }

    private static void zephyrSwordWithFloorJars(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        InfusionRecipe chosen = (InfusionRecipe) TcRecipes.get("ElementalSword");
        helper.assertTrue(chosen != null, "Sword of the Zephyr recipe missing");
        prepare(helper, player, chosen);
        craftWithJars(helper, player, chosen, true);
    }

    private static void pedestalLayout(GameTestHelper helper, int radius) {
        ServerPlayer player = mockServerPlayer(helper);
        InfusionRecipe chosen = (InfusionRecipe) TcRecipes.get("ElementalSword");
        helper.assertTrue(chosen != null, "Sword of the Zephyr recipe missing");
        ResearchManager.complete(player, "INFUSION");
        ResearchManager.complete(player, chosen.research());
        equipWand(player);
        buildFrame(helper, new BlockPos(12, 0, 12));
        BlockPos matrixPos = new BlockPos(15, 3, 15);
        BlockPos centerPos = matrixPos.below(2);
        clickMatrix(helper, player, matrixPos);
        InfusionMatrixBlockEntity matrix = helper.getBlockEntity(matrixPos, InfusionMatrixBlockEntity.class);
        helper.assertTrue(matrix.isActive() && matrix.validLocation(), "large altar did not form");
        PedestalBlockEntity center = helper.getBlockEntity(centerPos, PedestalBlockEntity.class);
        center.setStack(chosen.central().displayStacks().getFirst().copyWithCount(1));
        BlockPos[] corners = {
            centerPos.offset(-radius, 0, -radius), centerPos.offset(radius, 0, radius),
            centerPos.offset(-radius, 0, radius), centerPos.offset(radius, 0, -radius)
        };
        for (int index = 0; index < corners.length; index++) {
            helper.setBlock(corners[index], ModBlocks.ARCANE_PEDESTAL.get());
            helper.getBlockEntity(corners[index], PedestalBlockEntity.class)
                .setStack(chosen.components().get(index).displayStacks().getFirst().copyWithCount(1));
        }
        List<Aspect> aspects = chosen.aspects().getAspects();
        for (int index = 0; index < aspects.size(); index++) {
            BlockPos jarPos = centerPos.offset(index - 1, 0, 3);
            helper.setBlock(jarPos, ModBlocks.WARDED_JAR.get());
            JarBlockEntity jar = helper.getBlockEntity(jarPos, JarBlockEntity.class);
            jar.setAspect(aspects.get(index));
            jar.setAmount(chosen.aspects().getAmount(aspects.get(index)));
        }
        matrix.recalculateSurroundings();
        helper.assertValueEqual(matrix.getPedestals().size(), corners.length, "large layout pedestal count");
        for (BlockPos corner : corners) {
            helper.assertTrue(matrix.getPedestals().contains(helper.absolutePos(corner)), "corner pedestal missing: " + corner);
        }
        helper.assertValueEqual(matrix.getSymmetry(), 0, "large layout symmetry");
        clickMatrix(helper, player, matrixPos);
        helper.assertTrue(matrix.isCrafting(), "crafting did not start at radius " + radius);
        suppressInstability(matrix);
        run(helper, matrix, 4000);
        helper.assertFalse(matrix.isCrafting(), "crafting did not finish at radius " + radius);
        helper.assertTrue(chosen.result().matches(center.getStack()), "large layout result missing");
        for (BlockPos corner : corners) {
            helper.assertTrue(helper.getBlockEntity(corner, PedestalBlockEntity.class).getStack().isEmpty(), "corner ingredient not consumed");
        }
        helper.succeed();
    }

    private static void suppressInstability(InfusionMatrixBlockEntity matrix) {
        try {
            Field instability = InfusionMatrixBlockEntity.class.getDeclaredField("instability");
            instability.setAccessible(true);
            instability.setInt(matrix, 0);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void craftWithJars(GameTestHelper helper, ServerPlayer player, InfusionRecipe chosen, boolean floorJars) {
        List<Aspect> aspects = chosen.aspects().getAspects();
        for (int index = 0; index < aspects.size(); index++) {
            BlockPos jarPos = floorJars ? JARS[index].below(3) : JARS[index];
            helper.setBlock(jarPos, ModBlocks.WARDED_JAR.get());
            JarBlockEntity jar = helper.getBlockEntity(jarPos, JarBlockEntity.class);
            jar.setAspect(aspects.get(index));
            jar.setAmount(jar.getMaxAmount());
        }
        InfusionMatrixBlockEntity matrix = start(helper, player);
        suppressInstability(matrix);
        run(helper, matrix, 10);
        helper.assertValueEqual(matrix.getAspects().visSize(), chosen.aspects().visSize() - 1, "matrix did not drain essentia from jars");
        run(helper, matrix, 4000);
        helper.assertFalse(matrix.isCrafting(), "crafting did not finish");
        PedestalBlockEntity center = helper.getBlockEntity(new BlockPos(3, 1, 3), PedestalBlockEntity.class);
        helper.assertTrue(chosen.result().matches(center.getStack()), "central item is not the result: " + center.getStack());
        for (int index = 0; index < chosen.components().size(); index++) {
            helper.assertTrue(helper.getBlockEntity(RING[index], PedestalBlockEntity.class).getStack().isEmpty(), "ingredient not consumed: " + index);
        }
        for (int index = 0; index < aspects.size(); index++) {
            BlockPos jarPos = floorJars ? JARS[index].below(3) : JARS[index];
            JarBlockEntity jar = helper.getBlockEntity(jarPos, JarBlockEntity.class);
            helper.assertValueEqual(jar.getAmount(), jar.getMaxAmount() - chosen.aspects().getAmount(aspects.get(index)), "jar " + aspects.get(index).getTag());
        }
        helper.succeed();
    }
}
