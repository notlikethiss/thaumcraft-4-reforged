package thaumcraft.test;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectHelper;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;
import thaumcraft.entity.boss.ThaumcraftBoss;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.golem.GolemSnapshot;
import thaumcraft.entity.golem.GolemType;
import thaumcraft.entity.golem.LogisticsGoals;
import thaumcraft.entity.golem.Marker;
import thaumcraft.entity.monster.ChampionModifier;
import thaumcraft.registry.EldritchContent;
import thaumcraft.registry.EldritchEntities;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModEntities;
import thaumcraft.registry.ModItems;
import thaumcraft.research.Warp;
import thaumcraft.research.WarpType;
import thaumcraft.wand.WandRegistry;
import thaumcraft.wand.Wands;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ParityRegressionGameTests {
    private static final net.neoforged.neoforge.registries.DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = net.neoforged.neoforge.registries.DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
        Map.entry("parity_boss_champions_and_melee", ParityRegressionGameTests::bossChampions),
        Map.entry("parity_warded_absorption_and_wisp", ParityRegressionGameTests::wardedAndWisp),
        Map.entry("parity_temporary_champion_warp", ParityRegressionGameTests::championWarp),
        Map.entry("parity_orb_preserves_blocks", ParityRegressionGameTests::orbBlocks),
        Map.entry("parity_warded_pearl_join", ParityRegressionGameTests::wardedPearl),
        Map.entry("parity_construct_returns_pearl", ParityRegressionGameTests::constructPearl),
        Map.entry("parity_jar_label_crafting", ParityRegressionGameTests::jarLabels),
        Map.entry("parity_golem_empty_filters", ParityRegressionGameTests::emptyFilters),
        Map.entry("parity_golem_fill_any_amount", ParityRegressionGameTests::fillAny),
        Map.entry("parity_wand_and_wood_aspects", ParityRegressionGameTests::aspects),
        Map.entry("parity_vis_exhaust_milk", ParityRegressionGameTests::visMilk),
        Map.entry("parity_golem_ranged_stage", ParityRegressionGameTests::golemRangedStage),
        Map.entry("parity_original_tool_damage", ParityRegressionGameTests::toolDamage),
        Map.entry("parity_biome_spawn_lists", ParityRegressionGameTests::biomeSpawns),
        Map.entry("parity_silverwood_knot_scan", ParityRegressionGameTests::silverwoodKnot),
        Map.entry("parity_elemental_axe_does_not_strip", ParityRegressionGameTests::elementalAxeUse),
        Map.entry("parity_hungry_node_eats_items", ParityRegressionGameTests::hungryNodeItems)
    );

    static { TESTS.forEach((name, test) -> FUNCTIONS.register(name, () -> test)); }

    private ParityRegressionGameTests() {
    }

    public static void register(IEventBus bus) { FUNCTIONS.register(bus); }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Thaumcraft.id("parity_regressions"), new TestEnvironmentDefinition.AllOf());
        var isolated = event.registerEnvironment(Thaumcraft.id("parity_hungry_node"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) event.registerTest(Thaumcraft.id(name), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name)), new TestData<>(name.equals("parity_hungry_node_eats_items") ? isolated : environment, Thaumcraft.id("test/platform"), 300, 0, true)));
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.mockServerPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
        };
        player.connection.markClientLoaded();
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new BlockPos(7, 2, 7))));
        return player;
    }

    private static void bossChampions(GameTestHelper helper) {
        var level = helper.getLevel();
        var target = player(helper);
        for (var type : List.of(EldritchEntities.ELDRITCH_WARDEN.get(), EldritchEntities.ELDRITCH_GOLEM.get(), EldritchEntities.CULTIST_LEADER.get())) {
            ThaumcraftBoss boss = (ThaumcraftBoss) type.create(level, EntitySpawnReason.TRIGGERED);
            float health = boss.getMaxHealth();
            double damage = boss.getAttributeValue(Attributes.ATTACK_DAMAGE);
            ChampionModifier.processSpawn(boss, 99);
            helper.assertTrue(ChampionModifier.get(boss) >= 0, "boss did not become a champion");
            helper.assertValueEqual(boss.getMaxHealth(), health, "boss received the ordinary champion health bonus");
            helper.assertValueEqual(boss.getAttributeValue(Attributes.ATTACK_DAMAGE), damage * (ChampionModifier.get(boss) == 3 ? 4.0 : 1.0), "boss received the ordinary champion damage bonus");
            helper.assertTrue(boss.hasCustomName() && !boss.getName().getString().contains("%s"), "boss title was not formatted");
            boss.setPos(target.position().add(1, 0, 0));
            boss.setTarget(target);
            var ranged = new thaumcraft.entity.ai.LongRangeAttackGoal((net.minecraft.world.entity.monster.RangedAttackMob) boss, boss instanceof thaumcraft.entity.boss.CultistLeader ? 16 : 3, 1.0, 5, 5, 24.0F);
            helper.assertFalse(ranged.canUse(), "boss still shoots within melee range");
            boss.setPos(target.position().add(20, 0, 0));
            helper.assertTrue(ranged.canUse(), "boss cannot use ranged attacks at long range");
        }
        helper.succeed();
    }

    private static void wardedAndWisp(GameTestHelper helper) {
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        ChampionModifier.assign(zombie, 5, false);
        helper.assertValueEqual(zombie.getAbsorptionAmount(), 10.0F, "warded absorption was clamped to zero");
        zombie.setAbsorptionAmount(0);
        zombie.invulnerableTime = 10;
        zombie.tickCount = 25;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(zombie));
        helper.assertValueEqual(zombie.getAbsorptionAmount(), 0.0F, "warded absorption regenerated during damage cooldown");
        zombie.invulnerableTime = 0;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(zombie));
        helper.assertValueEqual(zombie.getAbsorptionAmount(), 1.0F, "warded absorption did not resume regeneration");
        var wisp = ModEntities.WISP.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        ChampionModifier.processSpawn(wisp, -100);
        helper.assertValueEqual(ChampionModifier.get(wisp), ChampionModifier.NORMAL, "wisp became a champion");
        var outer = helper.getLevel().getServer().getLevel(thaumcraft.world.dim.OuterLands.OUTER_LANDS);
        var guardian = ModEntities.ELDRITCH_GUARDIAN.get().create(outer, EntitySpawnReason.TRIGGERED);
        guardian.finalizeSpawn(outer, outer.getCurrentDifficultyAt(BlockPos.ZERO), EntitySpawnReason.TRIGGERED, null);
        helper.assertValueEqual(guardian.getAbsorptionAmount(), 25.0F, "Outer Lands guardian lacks absorption");
        helper.succeed();
    }

    private static void championWarp(GameTestHelper helper) {
        var player = player(helper);
        var attacker = net.minecraft.world.entity.EntityType.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        ChampionModifier.applyAttackEffect(6, attacker, player, 2, 0, 2);
        helper.assertValueEqual(Warp.get(player, WarpType.TEMPORARY), 3, "warp champion did not grant temporary warp");
        helper.assertValueEqual(Warp.get(player, WarpType.PERMANENT), 0, "warp champion granted permanent warp");
        for (var item : List.of(ModBlocks.BRAIN_JAR.get().asItem(), ModItems.PRIMAL_STAFF_CORE.get(), net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Thaumcraft.id("flesh_golem")))) {
            helper.assertValueEqual(Warp.getItemWarp(new ItemStack(item)), 1, "missing crafted-item warp");
        }
        helper.succeed();
    }

    private static void orbBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(4, 3, 4));
        level.setBlockAndUpdate(center, Blocks.DIRT.defaultBlockState());
        class Orb extends thaumcraft.entity.projectile.ExplosiveOrb {
            Orb() { super(ModEntities.EXPLOSIVE_ORB.get(), level); }
            void impact() { this.onHit(new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(center), Direction.UP, center, false)); }
        }
        Orb orb = new Orb();
        orb.setPos(net.minecraft.world.phys.Vec3.atCenterOf(center.above()));
        orb.strength = 4;
        orb.impact();
        helper.assertTrue(level.getBlockState(center).is(Blocks.DIRT), "explosive orb destroyed blocks");
        helper.succeed();
    }

    private static void wardedPearl(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos ward = helper.absolutePos(new BlockPos(4, 2, 4));
        level.setBlockAndUpdate(ward, ModBlocks.WARDED_GLASS.get().defaultBlockState());
        var pearl = new net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl(level, player(helper), new ItemStack(Items.ENDER_PEARL));
        pearl.setPos(net.minecraft.world.phys.Vec3.atCenterOf(ward.offset(5, 5, 5)));
        helper.assertFalse(level.addFreshEntity(pearl), "warded object allowed a nearby ender pearl to join");
        helper.assertTrue(pearl.isRemoved(), "warded pearl was not discarded");
        helper.succeed();
    }

    private static void constructPearl(GameTestHelper helper) {
        var player = player(helper);
        thaumcraft.research.Knowledge.get(player).addResearch("ADVALCHEMYFURNACE");
        var grid = new net.minecraft.world.SimpleContainer(11);
        for (int slot : new int[]{0, 2, 6, 8}) grid.setItem(slot, new ItemStack(ModItems.VOID_METAL_INGOT.get()));
        for (int slot : new int[]{1, 3, 5, 7}) grid.setItem(slot, new ItemStack(ModBlocks.ALCHEMICAL_CONSTRUCT.get()));
        grid.setItem(4, new ItemStack(EldritchContent.PRIMORDIAL_PEARL.get()));
        var wand = Wands.create(WandRegistry.CAP_GOLD, WandRegistry.ROD_SILVERWOOD, false);
        Wands.fill(wand);
        grid.setItem(10, wand);
        var result = new ItemStack(ModBlocks.ADVANCED_ALCHEMICAL_CONSTRUCT.get(), 4);
        var output = new net.minecraft.world.inventory.ResultContainer();
        output.setItem(0, result.copy());
        new thaumcraft.menu.SlotCraftingArcaneWorkbench(player, grid, output, 0, 0, 0).onTake(player, result);
        helper.assertValueEqual(grid.getItem(4).getCount(), 1, "primordial pearl was consumed");
        helper.assertTrue(grid.getItem(4).is(EldritchContent.PRIMORDIAL_PEARL.get()), "pearl was replaced with another item");
        helper.assertTrue(grid.getItem(0).isEmpty() && grid.getItem(1).isEmpty(), "construct did not consume other ingredients");
        helper.succeed();
    }

    private static void jarLabels(GameTestHelper helper) {
        var recipe = thaumcraft.crafting.JarLabelRecipe.INSTANCE;
        helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Thaumcraft.id("jar_label_filter"))).isPresent(), "label recipe is missing from datapack");
        for (Aspect aspect : Aspect.values()) {
            var phial = new ItemStack(ModItems.ESSENTIA_PHIAL.get());
            AspectKeys.ASPECTS.set(phial, new AspectList().add(aspect, 8));
            var input = CraftingInput.of(2, 1, List.of(new ItemStack(ModItems.JAR_LABEL.get()), phial));
            helper.assertTrue(recipe.matches(input, helper.getLevel()), "filled phial and label do not match");
            var label = recipe.assemble(input);
            helper.assertTrue(AspectKeys.ASPECTS.get(label).getAspects().equals(List.of(aspect)), "wrong label aspect");
            helper.assertTrue(ItemStack.isSameItemSameComponents(recipe.getRemainingItems(input).get(1), phial), "label recipe consumed the filled phial");
            var clear = CraftingInput.of(1, 1, List.of(label));
            helper.assertTrue(recipe.matches(clear, helper.getLevel()) && AspectKeys.ASPECTS.getOrDefault(recipe.assemble(clear), AspectList.EMPTY).isEmpty(), "label cannot be cleared");
        }
        helper.succeed();
    }

    private static GolemBase golem(GameTestHelper helper, int core, int toggles, List<ItemStack> filters, BlockPos home, BlockPos marker) {
        var golem = new GolemBase(ModEntities.GOLEM.get(), helper.getLevel());
        golem.setPos(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        golem.setHome(home, Direction.EAST);
        golem.configure(GolemType.IRON, new GolemSnapshot(core, false, List.of(), "", List.of(new Marker(GlobalPos.of(helper.getLevel().dimension(), marker), Direction.UP, -1)), ItemContainerContents.fromItems(filters), List.of(), toggles));
        return golem;
    }

    private static void emptyFilters(GameTestHelper helper) {
        var level = helper.getLevel();
        for (int core : new int[]{1, 8}) {
            BlockPos home = helper.absolutePos(new BlockPos(3, 2, core == 1 ? 3 : 8));
            BlockPos source = home.west();
            BlockPos marker = home.east(3);
            level.setBlockAndUpdate(source, ModBlocks.HUNGRY_CHEST.get().defaultBlockState());
            level.setBlockAndUpdate(marker, Blocks.STONE.defaultBlockState());
            var chest = (net.minecraft.world.Container) level.getBlockEntity(source);
            chest.setItem(0, new ItemStack(Items.DIRT, 40));
            var golem = golem(helper, core, 0, List.of(), home, marker);
            var goals = new GoalSelector();
            LogisticsGoals.install(golem, goals);
            goals.tick();
            helper.assertValueEqual(golem.getCarried().getCount(), 32, "empty filter failed to take a full load for core " + core);
            helper.assertValueEqual(chest.getItem(0).getCount(), 8, "empty filter extracted the wrong amount");
        }
        helper.succeed();
    }

    private static void fillAny(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos home = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos source = home.east();
        level.setBlockAndUpdate(home.west(), ModBlocks.HUNGRY_CHEST.get().defaultBlockState());
        level.setBlockAndUpdate(source, ModBlocks.HUNGRY_CHEST.get().defaultBlockState());
        var chest = (net.minecraft.world.Container) level.getBlockEntity(source);
        chest.setItem(0, new ItemStack(Items.WHEAT, 40));
        var golem = golem(helper, 0, 1, List.of(new ItemStack(Items.WHEAT, 1)), home, source);
        var goals = new GoalSelector();
        LogisticsGoals.install(golem, goals);
        goals.tick();
        helper.assertValueEqual(golem.getCarried().getCount(), 32, "Any amount followed the filter count instead of carrying capacity");
        helper.assertValueEqual(chest.getItem(0).getCount(), 8, "Fill extracted an incorrect quantity");
        helper.succeed();
    }

    private static void aspects(GameTestHelper helper) {
        var wand = Wands.create(WandRegistry.CAP_GOLD, WandRegistry.ROD_SILVERWOOD, false);
        var tags = AspectHelper.getObjectTags(wand);
        helper.assertTrue(tags != null && tags.getAmount(Aspect.MAGIC) >= 6 && tags.getAmount(Aspect.TOOL) >= 4, "wand lacks aspects from its parts");
        helper.assertTrue(AspectHelper.getObjectTags(new ItemStack(Items.COOKED_SALMON)).getAmount(Aspect.HUNGER) == 3, "cooked salmon lacks original aspects");
        helper.assertTrue(AspectHelper.getObjectTags(new ItemStack(Items.SPRUCE_DOOR)).getAmount(Aspect.MECHANISM) >= 1, "spruce door lacks mechanism aspect");
        helper.succeed();
    }

    private static void visMilk(GameTestHelper helper) {
        var player = player(helper);
        thaumcraft.effect.EffectHooks.applyVisExhaust(player, 1200, 0);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
        player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        new ItemStack(Items.MILK_BUCKET).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(player.hasEffect(thaumcraft.registry.ModEffects.VIS_EXHAUST), "milk cured vis exhaustion");
        helper.succeed();
    }
    private static void golemRangedStage(GameTestHelper helper) {
        class Probe extends thaumcraft.entity.boss.EldritchGolem {
            Probe() { super(EldritchEntities.ELDRITCH_GOLEM.get(), helper.getLevel()); }
            long rangedGoals() { return this.goalSelector.getAvailableGoals().stream().filter(goal -> goal.getGoal() instanceof thaumcraft.entity.ai.LongRangeAttackGoal).count(); }
        }
        var golem = new Probe();
        helper.assertValueEqual(golem.rangedGoals(), 0L, "golem with a head blocks melee with an inactive ranged goal");
        golem.setHeadless(true);
        golem.setHeadless(true);
        helper.assertValueEqual(golem.rangedGoals(), 1L, "headless golem needs exactly one ranged goal");
        golem.setHeadless(false);
        helper.assertValueEqual(golem.rangedGoals(), 0L, "restoring the head left a ranged goal installed");
        helper.succeed();
    }

    private static void toolDamage(GameTestHelper helper) {
        var player = player(helper);
        for (var expected : Map.of(ModItems.THAUMIUM_SWORD.get(), 7.0, ModItems.THAUMIUM_PICKAXE.get(), 5.0, ModItems.THAUMIUM_AXE.get(), 6.0, ModItems.THAUMIUM_SHOVEL.get(), 4.0, ModItems.THAUMIUM_HOE.get(), 1.0, ModItems.PRIMAL_CRUSHER.get(), 8.5).entrySet()) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(expected.getKey()));
            new ItemStack(expected.getKey()).forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                var instance = player.getAttribute(attribute);
                if (instance != null) instance.addOrUpdateTransientModifier(modifier);
            });
            helper.assertValueEqual(player.getAttributeValue(Attributes.ATTACK_DAMAGE), expected.getValue(), "wrong total damage for " + expected.getKey());
        }
        helper.succeed();
    }

    private static void biomeSpawns(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        for (String biome : List.of("minecraft:desert", "minecraft:swamp", "thaumcraft:magical_forest", "thaumcraft:eerie")) {
            var value = registry.getOrThrow(ResourceKey.create(Registries.BIOME, net.minecraft.resources.Identifier.parse(biome))).value();
            var spawns = value.getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER);
            helper.assertTrue(spawns.unwrap().stream().anyMatch(entry -> entry.value().type() == ModEntities.BRAINY_ZOMBIE.get()), "missing brainy zombie in " + biome);
            if (biome.equals("thaumcraft:magical_forest")) {
                for (var type : List.of(ModEntities.PECH.get(), ModEntities.WISP.get())) helper.assertTrue(spawns.unwrap().stream().anyMatch(entry -> entry.value().type() == type && entry.value().maxCount() == 2), "forest lacks groups of 1-2 for " + type);
            }
            if (biome.equals("thaumcraft:eerie")) {
                for (var type : List.of(ModEntities.GIANT_BRAINY_ZOMBIE.get(), ModEntities.WISP.get(), ModEntities.ELDRITCH_GUARDIAN.get())) helper.assertTrue(spawns.unwrap().stream().anyMatch(entry -> entry.value().type() == type), "Eerie lacks " + type);
            }
        }
        helper.succeed();
    }

    private static void silverwoodKnot(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos, ModBlocks.SILVERWOOD_KNOT.get().defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        thaumcraft.node.NodeGenerator.createRandomNodeAt(level, pos, level.getRandom(), true, false, false);
        var target = thaumcraft.research.ScanManager.resolveBlock(level, pos);
        helper.assertTrue(target != null && target.kind() == thaumcraft.research.ScanKind.PHENOMENON, "knot does not scan as a node");
        helper.assertTrue(target.aspects() != null && target.aspects().getAmount(Aspect.ORDER) > 0, "knot node has no pure aspects");
        var node = (thaumcraft.node.NodeBlockEntity) level.getBlockEntity(pos);
        var client = new thaumcraft.node.NodeBlockEntity(pos, level.getBlockState(pos));
        client.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), node.getUpdateTag(level.registryAccess())));
        helper.assertTrue(client.getNodeType() == thaumcraft.node.NodeType.PURE && client.getAspects().size() == node.getAspects().size(), "knot node does not sync to the client");
        helper.assertTrue(level.getBlockState(pos).getCloneItemStack(level, pos, false).is(ModBlocks.SILVERWOOD_LOG.get().asItem()), "knot does not pick as silverwood log");
        helper.succeed();
    }

    private static void elementalAxeUse(GameTestHelper helper) {
        var player = player(helper);
        var pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, Blocks.OAK_LOG);
        ItemStack axe = new ItemStack(ModItems.ELEMENTAL_AXE.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, axe);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(pos)), Direction.UP, helper.absolutePos(pos), false);
        var result = axe.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(result == net.minecraft.world.InteractionResult.PASS, "elemental axe consumed the block click");
        helper.assertBlockPresent(Blocks.OAK_LOG, pos);
        helper.succeed();
    }

    private static void hungryNodeItems(GameTestHelper helper) {
        helper.assertTrue(thaumcraft.Config.hardNode(), "hard nodes are disabled");
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.OBSIDIAN);
        thaumcraft.node.NodeGenerator.createNodeAt(level, pos, thaumcraft.node.NodeType.HUNGRY, null, new AspectList().add(Aspect.AIR, 20).add(Aspect.EARTH, 20).add(Aspect.FIRE, 20).add(Aspect.WATER, 20).add(Aspect.ORDER, 20).add(Aspect.ENTROPY, 20));
        var node = (thaumcraft.node.NodeBlockEntity) level.getBlockEntity(pos);
        for (Aspect aspect : node.getAspects().getAspects()) {
            node.takeFromContainer(aspect, 10);
        }
        int before = node.getAspects().visSize();
        ItemStack table = new ItemStack(Items.CRAFTING_TABLE);
        var tags = thaumcraft.research.ScanManager.resolveStack(table).aspects();
        helper.assertTrue(tags != null && tags.size() > 0, "crafting table has no aspects");
        var item = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, table);
        item.setNoGravity(true);
        item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        level.addFreshEntity(item);
        helper.succeedWhen(() -> {
            helper.assertTrue(item.isRemoved(), "item not eaten yet");
            helper.assertTrue(node.getAspects().visSize() > before, "hungry node gained nothing from the item");
            level.removeBlock(pos, false);
        });
    }
}
