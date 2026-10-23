package thaumcraft.test;

import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
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
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.arcane.ArcaneWorkbenchBlockEntity;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.ArcaneCrafting;
import thaumcraft.crafting.TcStack;
import thaumcraft.menu.ArcaneWorkbenchMenu;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.EldritchContent;
import thaumcraft.research.Knowledge;
import thaumcraft.research.Warp;
import thaumcraft.wand.WandCap;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.WandRod;
import thaumcraft.wand.WandRegistry;
import thaumcraft.wand.WandTriggers;
import thaumcraft.wand.Wands;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ArcaneGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final String[] TESTS = {"arcane_workbench_consumes_vis", "wand_recipe_builds_wand", "table_wand_becomes_worktable", "addon_wand_parts_and_hooks", "arcane_filter_shifted_rows", "arcane_tubes_shifted_inputs", "arcane_vanilla_shifted_remainders", "arcane_insufficient_vis_updates", "arcane_construct_pearl_remainder"};

    static {
        FUNCTIONS.register(TESTS[0], () -> ArcaneGameTests::workbenchConsumesVis);
        FUNCTIONS.register(TESTS[1], () -> ArcaneGameTests::wandRecipeBuildsWand);
        FUNCTIONS.register(TESTS[2], () -> ArcaneGameTests::tableWandBecomesWorktable);
        FUNCTIONS.register(TESTS[3], () -> ArcaneGameTests::addonWandPartsAndHooks);
        FUNCTIONS.register(TESTS[4], () -> ArcaneGameTests::filterShiftedRows);
        FUNCTIONS.register(TESTS[5], () -> ArcaneGameTests::tubesShiftedInputs);
        FUNCTIONS.register(TESTS[6], () -> ArcaneGameTests::vanillaShiftedRemainders);
        FUNCTIONS.register(TESTS[7], () -> ArcaneGameTests::insufficientVisUpdates);
        FUNCTIONS.register(TESTS[8], () -> ArcaneGameTests::constructPearlRemainder);
    }

    private ArcaneGameTests() {
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
            Thaumcraft.id("arcane_default"),
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

    private static void workbenchConsumesVis(GameTestHelper helper) {
        BlockPos tablePos = new BlockPos(1, 1, 1);
        helper.setBlock(tablePos, ModBlocks.ARCANE_WORKTABLE.get());
        ArcaneWorkbenchBlockEntity workbench = helper.getBlockEntity(tablePos, ArcaneWorkbenchBlockEntity.class);
        ServerPlayer player = mockServerPlayer(helper);
        Knowledge.get(player).addResearch("VISPOWER");
        ItemStack wand = Wands.create(WandRegistry.CAP_GOLD, WandRegistry.ROD_GREATWOOD, false);
        Wands.fill(wand);
        int fireBefore = Wands.getVis(wand, Aspect.FIRE);
        workbench.setItem(ArcaneWorkbenchBlockEntity.WAND_SLOT, wand);
        for (int slot : new int[]{1, 3, 5, 7}) {
            workbench.setItem(slot, new ItemStack(Items.IRON_INGOT));
        }
        workbench.setItem(4, new ItemStack(ModItems.BALANCED_SHARD.get()));
        ArcaneWorkbenchMenu menu = new ArcaneWorkbenchMenu(1, player.getInventory(), helper.absolutePos(tablePos));
        ItemStack result = menu.getSlot(ArcaneWorkbenchMenu.RESULT_SLOT).getItem();
        helper.assertTrue(result.is(ModBlocks.VIS_RELAY.get().asItem()) && result.getCount() == 2, "arcane result missing");
        helper.assertValueEqual(menu.getMatchedAspects().getAmount(Aspect.FIRE), 8, "matched fire cost");
        menu.clicked(ArcaneWorkbenchMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(ModBlocks.VIS_RELAY.get().asItem()), "result not taken");
        helper.assertValueEqual(Wands.getVis(workbench.getWand(), Aspect.FIRE), fireBefore - 800, "fire vis left");
        helper.assertValueEqual(Wands.getVis(workbench.getWand(), Aspect.AIR), fireBefore, "air vis untouched");
        for (int slot = 0; slot < ArcaneWorkbenchBlockEntity.GRID_SLOTS; slot++) {
            helper.assertTrue(workbench.getItem(slot).isEmpty(), "grid slot " + slot + " not consumed");
        }
        helper.succeed();
    }

    private static void wandRecipeBuildsWand(GameTestHelper helper) {
        ServerPlayer player = mockServerPlayer(helper);
        List<ItemStack> grid = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            grid.add(ItemStack.EMPTY);
        }
        grid.set(6, new ItemStack(ModItems.GOLD_WAND_CAP.get()));
        grid.set(2, new ItemStack(ModItems.GOLD_WAND_CAP.get()));
        grid.set(4, new ItemStack(ModItems.GREATWOOD_ROD.get()));
        helper.assertTrue(ArcaneCrafting.findResult(grid, player).isEmpty(), "wand crafted without research");
        Knowledge.get(player).addResearch("CAP_gold");
        Knowledge.get(player).addResearch("ROD_greatwood");
        ItemStack result = ArcaneCrafting.findResult(grid, player);
        helper.assertTrue(Wands.isWand(result), "no wand built");
        helper.assertValueEqual(Wands.getCap(result).tag(), "gold", "cap");
        helper.assertValueEqual(Wands.getRod(result).tag(), "greatwood", "rod");
        helper.assertValueEqual(ArcaneCrafting.findAspects(grid, player).getAmount(Aspect.ORDER), 9, "order cost");
        helper.succeed();
    }

    private record Workbench(ArcaneWorkbenchBlockEntity table, ArcaneWorkbenchMenu menu, ServerPlayer player) {
    }

    private static List<ItemStack> emptyGrid() {
        List<ItemStack> grid = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            grid.add(ItemStack.EMPTY);
        }
        return grid;
    }

    private static Workbench prepareWorkbench(GameTestHelper helper, List<ItemStack> grid) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ARCANE_WORKTABLE.get());
        ArcaneWorkbenchBlockEntity table = helper.getBlockEntity(pos, ArcaneWorkbenchBlockEntity.class);
        ServerPlayer player = mockServerPlayer(helper);
        for (String research : new String[]{"DISTILESSENTIA", "TUBES", "TUBEFILTER"}) {
            Knowledge.get(player).addResearch(research);
        }
        for (int slot = 0; slot < 9; slot++) {
            table.setItem(slot, grid.get(slot).copy());
        }
        ItemStack wand = Wands.create(WandRegistry.CAP_GOLD, WandRegistry.ROD_GREATWOOD, false);
        Wands.fill(wand);
        table.setItem(ArcaneWorkbenchBlockEntity.WAND_SLOT, wand);
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
        return new Workbench(table, new ArcaneWorkbenchMenu(1, player.getInventory(), helper.absolutePos(pos)), player);
    }

    private static int inventoryCount(Player player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void assertCraftsTwice(GameTestHelper helper, List<ItemStack> grid, Item output, int count, boolean quickMove) {
        Workbench bench = prepareWorkbench(helper, grid);
        ArcaneWorkbenchMenu menu = bench.menu();
        ItemStack wand = bench.table().getWand();
        AspectList cost = menu.getMatchedAspects().copy();
        helper.assertTrue(menu.getSlot(0).getItem().is(output), "shifted recipe missing");
        helper.assertValueEqual(menu.getSlot(0).getItem().getCount(), count, "recipe output count");
        if (quickMove) {
            menu.clicked(0, 0, ContainerInput.QUICK_MOVE, bench.player());
        } else {
            menu.clicked(0, 0, ContainerInput.PICKUP, bench.player());
            helper.assertTrue(menu.getCarried().is(output), "first output missing");
            helper.assertValueEqual(menu.getCarried().getCount(), count, "first output count");
            for (int slot = 0; slot < 9; slot++) {
                helper.assertValueEqual(bench.table().getItem(slot).getCount(), grid.get(slot).isEmpty() ? 0 : 1, "first consumption at " + slot);
            }
            menu.clicked(0, 0, ContainerInput.PICKUP, bench.player());
        }
        int total = inventoryCount(bench.player(), output) + (menu.getCarried().is(output) ? menu.getCarried().getCount() : 0);
        helper.assertValueEqual(total, count * 2, "two craft output count");
        for (int slot = 0; slot < 9; slot++) {
            helper.assertTrue(bench.table().getItem(slot).isEmpty(), "ingredient left at " + slot);
        }
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "result remains after resources exhausted");
        menu.clicked(0, 0, quickMove ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP, bench.player());
        int after = inventoryCount(bench.player(), output) + (menu.getCarried().is(output) ? menu.getCarried().getCount() : 0);
        helper.assertValueEqual(after, total, "extra output without ingredients");
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            float modifier = ((WandItem) wand.getItem()).getConsumptionModifier(wand, bench.player(), aspect, true);
            int spent = (int)(cost.getAmount(aspect) * 100 * modifier);
            helper.assertValueEqual(Wands.getVis(wand, aspect), Wands.getMaxVis(wand) - spent * 2, "vis consumption " + aspect);
        }
        helper.assertValueEqual(inventoryCount(bench.player(), Items.DIAMOND), 7, "unrelated inventory changed");
        helper.assertTrue(bench.table().getWand() == wand, "wand replaced");
        menu.removed(bench.player());
    }

    private static void filterShiftedRows(GameTestHelper helper) {
        for (int row = 0; row < 3; row++) {
            for (boolean quickMove : new boolean[]{false, true}) {
                List<ItemStack> grid = emptyGrid();
                grid.set(row * 3, new ItemStack(Items.GOLD_INGOT, 2));
                grid.set(row * 3 + 1, new ItemStack(ModBlocks.SILVERWOOD_PLANKS.get(), 2));
                grid.set(row * 3 + 2, new ItemStack(Items.GOLD_INGOT, 2));
                assertCraftsTwice(helper, grid, ModItems.VIS_FILTER.get(), 2, quickMove);
            }
        }
        helper.succeed();
    }

    private static void tubesShiftedInputs(GameTestHelper helper) {
        Item[] additions = {Items.LEVER, ModItems.VIS_FILTER.get(), Items.STONE, Items.BLUE_DYE};
        Item[] outputs = {ModBlocks.ESSENTIA_VALVE.get().asItem(), ModBlocks.FILTERED_ESSENTIA_TUBE.get().asItem(), ModBlocks.RESTRICTED_ESSENTIA_TUBE.get().asItem(), ModBlocks.DIRECTIONAL_ESSENTIA_TUBE.get().asItem()};
        int[][] positions = {{0, 1}, {3, 4}, {6, 7}, {0, 3}, {1, 4}, {2, 5}, {3, 6}, {4, 7}, {5, 8}, {0, 8}};
        for (int recipe = 0; recipe < additions.length; recipe++) {
            for (int[] slots : positions) {
                for (boolean quickMove : new boolean[]{false, true}) {
                    List<ItemStack> grid = emptyGrid();
                    grid.set(slots[0], new ItemStack(ModBlocks.ESSENTIA_TUBE.get(), 2));
                    grid.set(slots[1], new ItemStack(additions[recipe], 2));
                    assertCraftsTwice(helper, grid, outputs[recipe], 1, quickMove);
                }
            }
        }
        helper.succeed();
    }

    private static void vanillaShiftedRemainders(GameTestHelper helper) {
        for (int left = 0; left < 2; left++) {
            for (int top = 0; top < 2; top++) {
                for (boolean quickMove : new boolean[]{false, true}) {
                    List<ItemStack> grid = emptyGrid();
                    for (int x = 0; x < 2; x++) {
                        for (int y = 0; y < 2; y++) {
                            grid.set(left + x + (top + y) * 3, new ItemStack(Items.HONEY_BOTTLE, 2));
                        }
                    }
                    Workbench bench = prepareWorkbench(helper, grid);
                    ArcaneWorkbenchMenu menu = bench.menu();
                    helper.assertTrue(menu.getSlot(0).getItem().is(Items.HONEY_BLOCK), "vanilla recipe missing");
                    menu.clicked(0, 0, quickMove ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP, bench.player());
                    if (!quickMove) {
                        menu.clicked(0, 0, ContainerInput.PICKUP, bench.player());
                    }
                    int resultCount = inventoryCount(bench.player(), Items.HONEY_BLOCK) + (menu.getCarried().is(Items.HONEY_BLOCK) ? menu.getCarried().getCount() : 0);
                    helper.assertValueEqual(resultCount, 2, "vanilla output count");
                    int bottles = inventoryCount(bench.player(), Items.GLASS_BOTTLE);
                    for (int slot = 0; slot < 9; slot++) {
                        ItemStack remaining = bench.table().getItem(slot);
                        if (grid.get(slot).isEmpty()) {
                            helper.assertTrue(remaining.isEmpty(), "remainder outside original pattern");
                        } else {
                            helper.assertTrue(remaining.is(Items.GLASS_BOTTLE), "remainder missing at " + slot);
                            bottles += remaining.getCount();
                        }
                    }
                    helper.assertValueEqual(bottles, 8, "crafting remainder count");
                    helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "vanilla output remains");
                    for (Aspect aspect : Aspect.getPrimalAspects()) {
                        helper.assertValueEqual(Wands.getVis(bench.table().getWand(), aspect), 5000, "vanilla craft used vis");
                    }
                    menu.removed(bench.player());
                }
            }
        }
        helper.succeed();
    }

    private static void insufficientVisUpdates(GameTestHelper helper) {
        List<ItemStack> grid = emptyGrid();
        grid.set(6, new ItemStack(Items.GOLD_INGOT));
        grid.set(7, new ItemStack(ModBlocks.SILVERWOOD_PLANKS.get()));
        grid.set(8, new ItemStack(Items.GOLD_INGOT));
        Workbench bench = prepareWorkbench(helper, grid);
        ItemStack emptyWand = Wands.create(WandRegistry.CAP_GOLD, WandRegistry.ROD_GREATWOOD, false);
        bench.table().setItem(ArcaneWorkbenchBlockEntity.WAND_SLOT, emptyWand);
        helper.assertTrue(bench.menu().getSlot(0).getItem().isEmpty(), "craft allowed without vis");
        helper.assertValueEqual(bench.menu().getMatchedAspects().getAmount(Aspect.ORDER), 5, "insufficient recipe cost missing");
        bench.menu().clicked(0, 0, ContainerInput.PICKUP, bench.player());
        helper.assertTrue(bench.menu().getCarried().isEmpty(), "output taken without vis");
        for (int slot = 0; slot < 9; slot++) {
            helper.assertTrue(ItemStack.matches(bench.table().getItem(slot), grid.get(slot)), "ingredient changed without vis");
        }
        Wands.fill(emptyWand);
        bench.menu().broadcastChanges();
        helper.assertTrue(bench.menu().getSlot(0).getItem().is(ModItems.VIS_FILTER.get()), "result not refreshed after charging");
        Wands.storeVis(emptyWand, Aspect.ORDER, 0);
        bench.menu().broadcastChanges();
        helper.assertTrue(bench.menu().getSlot(0).getItem().isEmpty(), "stale result after vis depleted");
        bench.table().setItem(8, ItemStack.EMPTY);
        helper.assertTrue(bench.menu().getMatchedAspects().isEmpty(), "stale cost after recipe changed");
        bench.menu().removed(bench.player());
        helper.succeed();
    }

    private static void constructPearlRemainder(GameTestHelper helper) {
        for (boolean quickMove : new boolean[]{false, true}) {
            List<ItemStack> grid = emptyGrid();
            for (int slot : new int[]{0, 2, 6, 8}) {
                grid.set(slot, new ItemStack(ModItems.VOID_METAL_INGOT.get(), 2));
            }
            for (int slot : new int[]{1, 3, 5, 7}) {
                grid.set(slot, new ItemStack(ModBlocks.ALCHEMICAL_CONSTRUCT.get(), 2));
            }
            grid.set(4, new ItemStack(EldritchContent.PRIMORDIAL_PEARL.get()));
            Workbench bench = prepareWorkbench(helper, grid);
            Knowledge.get(bench.player()).addResearch("ADVALCHEMYFURNACE");
            ItemStack wand = Wands.create(WandRegistry.CAP_GOLD, WandRegistry.ROD_SILVERWOOD, false);
            Wands.fill(wand);
            bench.table().setItem(ArcaneWorkbenchBlockEntity.WAND_SLOT, wand);
            helper.assertTrue(bench.menu().getSlot(0).getItem().is(ModBlocks.ADVANCED_ALCHEMICAL_CONSTRUCT.get().asItem()), "construct recipe missing");
            bench.menu().clicked(0, 0, quickMove ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP, bench.player());
            if (!quickMove) {
                bench.menu().clicked(0, 0, ContainerInput.PICKUP, bench.player());
            }
            Item output = ModBlocks.ADVANCED_ALCHEMICAL_CONSTRUCT.get().asItem();
            int count = inventoryCount(bench.player(), output) + (bench.menu().getCarried().is(output) ? bench.menu().getCarried().getCount() : 0);
            helper.assertValueEqual(count, 8, "construct output count");
            for (int slot = 0; slot < 9; slot++) {
                if (slot == 4) {
                    helper.assertTrue(bench.table().getItem(slot).is(EldritchContent.PRIMORDIAL_PEARL.get()), "construct consumed pearl");
                    helper.assertValueEqual(bench.table().getItem(slot).getCount(), 1, "pearl duplicated");
                } else {
                    helper.assertTrue(bench.table().getItem(slot).isEmpty(), "construct ingredient not consumed");
                }
            }
            helper.assertValueEqual(Wands.getVis(wand, Aspect.ORDER), 4000, "construct vis consumption");
            helper.assertTrue(bench.menu().getSlot(0).getItem().isEmpty(), "construct result remains without ingredients");
            bench.menu().removed(bench.player());
        }
        helper.succeed();
    }

    private static void tableWandBecomesWorktable(GameTestHelper helper) {
        BlockPos tablePos = new BlockPos(1, 1, 1);
        helper.setBlock(tablePos, ModBlocks.TABLE.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack wand = Wands.create(WandRegistry.CAP_IRON, WandRegistry.ROD_GREATWOOD, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        boolean done = WandTriggers.perform(helper.getLevel(), wand, player, helper.absolutePos(tablePos), Direction.UP);
        helper.assertTrue(done, "trigger did not fire");
        helper.assertBlockPresent(ModBlocks.ARCANE_WORKTABLE.get(), tablePos);
        ArcaneWorkbenchBlockEntity workbench = helper.getBlockEntity(tablePos, ArcaneWorkbenchBlockEntity.class);
        helper.assertTrue(Wands.isWand(workbench.getWand()), "wand not placed in the workbench");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "wand still in hand");
        helper.succeed();
    }

    private static void addonWandPartsAndHooks(GameTestHelper helper) {
        Identifier capTexture = Identifier.fromNamespaceAndPath("addon", "textures/model/cap.png");
        Identifier rodTexture = Identifier.fromNamespaceAndPath("addon", "textures/model/rod.png");
        if (WandRegistry.findCap("addon_test") == null) {
            WandRegistry.registerCap(
                "addon_test", 0.5F, List.of(Aspect.FIRE), 0.25F, Identifier.withDefaultNamespace("amethyst_shard"), 2, null, capTexture
            );
            WandRegistry.registerRod("addon_test", 40, Identifier.withDefaultNamespace("breeze_rod"), 3, null, true, false, false, rodTexture);
        }
        WandCap cap = WandRegistry.cap("addon_test");
        WandRod rod = WandRegistry.rod("addon_test");
        helper.assertValueEqual(cap.texture(), capTexture, "cap texture");
        helper.assertValueEqual(rod.texture(), rodTexture, "rod texture");
        helper.assertValueEqual(WandRegistry.CAP_GOLD.texture(), Thaumcraft.id("textures/model/wand_cap_gold.png"), "builtin cap texture");
        helper.assertValueEqual(WandRegistry.ROD_SILVERWOOD.texture(), Thaumcraft.id("textures/model/wand_rod_silverwood.png"), "builtin rod texture");
        helper.assertValueEqual(WandRegistry.STAFF_PRIMAL.texture(), Thaumcraft.id("textures/model/wand_rod_primal.png"), "builtin staff texture");
        ItemStack wand = Wands.create(cap, rod, false);
        helper.assertValueEqual(Wands.getCap(wand).texture(), capTexture, "wand cap texture");
        helper.assertValueEqual(Wands.getMaxVis(wand), 4000, "max vis");
        helper.assertValueEqual(Wands.getCap(wand).getCostModifier(Aspect.FIRE), 0.25F, "special discount");
        helper.assertValueEqual(Wands.getCap(wand).getCostModifier(Aspect.AIR), 0.5F, "base discount");
        ServerPlayer player = mockServerPlayer(helper);
        List<ItemStack> grid = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            grid.add(ItemStack.EMPTY);
        }
        grid.set(6, new ItemStack(Items.AMETHYST_SHARD));
        grid.set(2, new ItemStack(Items.AMETHYST_SHARD));
        grid.set(4, new ItemStack(Items.BREEZE_ROD));
        helper.assertTrue(ArcaneCrafting.findResult(grid, player).isEmpty(), "addon wand crafted without research");
        Knowledge.get(player).addResearch("CAP_addon_test");
        Knowledge.get(player).addResearch("ROD_addon_test");
        ItemStack crafted = ArcaneCrafting.findResult(grid, player);
        helper.assertTrue(Wands.isWand(crafted), "addon wand not built");
        helper.assertValueEqual(Wands.getCap(crafted).tag(), "addon_test", "crafted cap");
        helper.assertValueEqual(Wands.getRod(crafted).tag(), "addon_test", "crafted rod");
        helper.assertValueEqual(ArcaneCrafting.findAspects(grid, player).getAmount(Aspect.ORDER), 6, "addon order cost");
        Warp.registerItem(Identifier.withDefaultNamespace("amethyst_shard"), 3);
        helper.assertValueEqual(Warp.getItemWarp(new ItemStack(Items.AMETHYST_SHARD)), 3, "item warp");
        int[] applied = {0};
        TcStack.registerDataHandler("addon_test_key", (stack, value) -> applied[0] += Integer.parseInt(value));
        ItemStack created = TcStack.withData("minecraft:stick", 1, java.util.Map.of("addon_test_key", "7")).create();
        helper.assertValueEqual(applied[0], 7, "data handler");
        helper.assertTrue(created.is(Items.STICK), "data stack item");
        helper.succeed();
    }
}
