package thaumcraft.test;

import java.util.function.Consumer;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.entity.monster.ChampionModifier;
import thaumcraft.entity.AspectOrb;
import thaumcraft.node.NodeHooks;
import thaumcraft.aspect.Aspect;
import thaumcraft.registry.ModEntities;
import thaumcraft.platform.OuterLandsData;
import thaumcraft.world.dim.Cell;
import thaumcraft.world.dim.CellLoc;
import thaumcraft.world.dim.OuterLands;
import thaumcraft.wand.Wands;
import thaumcraft.world.gen.HalloweenFirebatSpawnModifier;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ChampionGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final String[] TESTS = {"champion_modifiers_apply_once", "champion_combat_and_rewards", "aspect_orb_persistence_pickup_and_node", "champion_spawn_roll_boundaries"};

    static {
        FUNCTIONS.register(TESTS[0], () -> ChampionGameTests::modifiersApplyOnce);
        FUNCTIONS.register(TESTS[1], () -> ChampionGameTests::championCombatAndRewards);
        FUNCTIONS.register(TESTS[2], () -> ChampionGameTests::aspectOrbPersistencePickupAndNode);
        FUNCTIONS.register(TESTS[3], () -> ChampionGameTests::championSpawnRollBoundaries);
    }

    private ChampionGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Thaumcraft.id("champion_modifiers"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name));
            event.registerTest(Thaumcraft.id(name), new FunctionGameTestInstance(function, new TestData<>(environment, Thaumcraft.id("test/platform"), 200, 0, true)));
        }
    }

    private static void modifiersApplyOnce(GameTestHelper helper) {
        helper.assertValueEqual(ChampionModifier.count(), 13, "champion modifier count");
        helper.assertValueEqual(ChampionModifier.names().getFirst(), "bold", "champion modifier id 0");
        helper.assertValueEqual(ChampionModifier.names().get(12), "infested", "champion modifier id 12");
        helper.assertTrue(HalloweenFirebatSpawnModifier.isHalloween(10, 31), "Halloween seasonal spawn date");
        helper.assertFalse(HalloweenFirebatSpawnModifier.isHalloween(10, 30), "seasonal spawn must not include October 30");
        helper.assertFalse(HalloweenFirebatSpawnModifier.isHalloween(11, 1), "seasonal spawn must not include November 1");

        var zombieType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("zombie"));
        for (int id = 0; id < ChampionModifier.count(); id++) {
            Mob mob = (Mob) zombieType.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            helper.assertTrue(mob != null, "zombie factory returned null for champion id " + id);
            mob.snapTo(helper.absolutePos(new BlockPos(2 + id % 4, 2, 2 + id / 4)), 0.0F, 0.0F);
            double baseDamage = mob.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
            ChampionModifier.assign(mob, id, false);
            ChampionModifier.assign(mob, id, false);
            helper.assertValueEqual(ChampionModifier.get(mob), id, "champion attachment id " + id);
            helper.assertValueEqual(mob.getAttributeValue(Attributes.MAX_HEALTH), mob.getAttributeBaseValue(Attributes.MAX_HEALTH) + 30.0, "champion health bonus id " + id);
            double expectedDamage = baseDamage * (id == 3 ? 12.0 : 3.0);
            helper.assertValueEqual(mob.getAttributeValue(Attributes.ATTACK_DAMAGE), expectedDamage, "champion attack modifier id " + id);
            if (id == 0) {
                helper.assertValueEqual(mob.getAttributeValue(Attributes.MOVEMENT_SPEED), mob.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * 1.3, "bold speed bonus");
            }
            helper.getLevel().addFreshEntity(mob);
        }
        Mob saved = (Mob) zombieType.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        ChampionModifier.assign(saved, 10, true);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        saved.saveWithoutId(output);
        Mob loaded = (Mob) zombieType.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        helper.assertValueEqual(ChampionModifier.get(loaded), 10, "champion modifier survives entity save and load");
        helper.succeed();
    }

    private static void championSpawnRollBoundaries(GameTestHelper helper) {
        var level = helper.getLevel();
        var zombieType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("zombie"));
        Mob normal = (Mob) zombieType.create(level, EntitySpawnReason.TRIGGERED);
        ChampionModifier.processSpawn(normal, 0);
        helper.assertTrue(ChampionModifier.get(normal) >= 0, "whitelist level zero must preserve the successful zero roll");

        var outer = level.getServer().getLevel(OuterLands.OUTER_LANDS);
        helper.assertTrue(outer != null, "Outer Lands level is unavailable");
        if (outer == null) {
            return;
        }
        Cell dangerous = new Cell();
        dangerous.feature = 6;
        CellLoc location = new CellLoc(1, 0);
        OuterLandsData data = outer.getDataStorage().computeIfAbsent(OuterLandsData.TYPE);
        data.putMaze("champion_test_" + location.x() + "_" + location.z(), new OuterLandsData.MazeState(
            17L, location.x(), location.z(), 1, 1,
            java.util.List.of(new OuterLandsData.CellData(location, (short) dangerous.pack())), java.util.List.of()
        ));
        Mob dangerousMob = (Mob) zombieType.create(outer, EntitySpawnReason.TRIGGERED);
        dangerousMob.setPos(15.5, 64.0, 0.5);
        ChampionModifier.processSpawn(dangerousMob, 10);
        helper.assertTrue(ChampionModifier.get(dangerousMob) >= 0, "Outer Lands and dangerous-cell adjustments must make roll ten pass at ceil-coordinate chunk edge");
        data.putMaze("champion_test_" + location.x() + "_" + location.z(), new OuterLandsData.MazeState(
            17L, location.x(), location.z(), 1, 1, java.util.List.of(), java.util.List.of()
        ));
        thaumcraft.world.dim.MazeHandler.publishSnapshot(outer);
        helper.succeed();
    }

    private static void championCombatAndRewards(GameTestHelper helper) {
        var level = helper.getLevel();
        var zombieType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("zombie"));
        var sheepType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("sheep"));
        Mob attacker = (Mob) zombieType.create(level, EntitySpawnReason.TRIGGERED);
        Mob victim = (Mob) sheepType.create(level, EntitySpawnReason.TRIGGERED);
        Mob undeadVictim = (Mob) zombieType.create(level, EntitySpawnReason.TRIGGERED);
        Vec3 attackerPos = Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 2, 3)));
        Vec3 victimPos = Vec3.atCenterOf(helper.absolutePos(new BlockPos(5, 2, 3)));
        attacker.setPos(attackerPos.x, attackerPos.y, attackerPos.z);
        victim.setPos(victimPos.x, victimPos.y, victimPos.z);
        undeadVictim.setPos(victimPos.x, victimPos.y, victimPos.z + 1.0);
        ChampionModifier.assign(attacker, 4, false);
        ChampionModifier.applyAttackEffect(4, attacker, victim, 4.0F, 0.39F, 0);
        helper.assertTrue(victim.hasEffect(net.minecraft.world.effect.MobEffects.WITHER), "grim applies wither below the 40 percent threshold");
        victim.removeEffect(net.minecraft.world.effect.MobEffects.WITHER);
        ChampionModifier.applyAttackEffect(4, attacker, victim, 4.0F, 0.40F, 0);
        helper.assertFalse(victim.hasEffect(net.minecraft.world.effect.MobEffects.WITHER), "grim does not apply wither at the threshold");
        ChampionModifier.applyAttackEffect(9, attacker, victim, 4.0F, 0.1F, 0);
        helper.assertTrue(victim.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER), "sickly applies hunger");
        ChampionModifier.applyAttackEffect(10, attacker, victim, 4.0F, 0.1F, 0);
        helper.assertTrue(victim.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "venomous applies poison");
        ChampionModifier.applyAttackEffect(10, attacker, undeadVictim, 4.0F, 0.1F, 0);
        helper.assertFalse(undeadVictim.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "venomous respects undead poison immunity");
        attacker.setHealth(1.0F);
        ChampionModifier.applyAttackEffect(11, attacker, victim, 4.0F, 0.99F, 0);
        helper.assertValueEqual(attacker.getHealth(), 3.0F, "vampiric heals by half of damage with a two point minimum");

        java.util.List<ItemEntity> drops = new java.util.ArrayList<>();
        int xpBefore = level.getEntitiesOfClass(ExperienceOrb.class, attacker.getBoundingBox().inflate(4.0)).stream().mapToInt(orb -> orb.getValue() * orb.count).sum();
        ChampionModifier.awardChampionRewards(level, attacker, drops, 0, 1, 0);
        helper.assertValueEqual(drops.size(), 1, "champion awards one loot bag");
        helper.assertTrue(drops.getFirst().getItem().is(thaumcraft.registry.ModItems.COMMON_LOOT_BAG.get()), "fixed low roll awards common bag");
        int xp = level.getEntitiesOfClass(ExperienceOrb.class, attacker.getBoundingBox().inflate(4.0)).stream().mapToInt(orb -> orb.getValue() * orb.count).sum();
        helper.assertValueEqual(xp - xpBefore, 6, "champion awards fixed XP amount for deterministic roll");
        helper.succeed();
    }

    private static void aspectOrbPersistencePickupAndNode(GameTestHelper helper) {
        var level = helper.getLevel();
        AspectOrb orb = AspectOrb.create(level, helper.absolutePos(new BlockPos(3, 2, 3)).getX() + 0.5, helper.absolutePos(new BlockPos(3, 2, 3)).getY() + 0.5, helper.absolutePos(new BlockPos(3, 2, 3)).getZ() + 0.5, Aspect.FIRE, 1);
        helper.assertTrue(orb != null, "aspect orb factory creates an entity");
        orb.tick();
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        orb.saveWithoutId(output);
        AspectOrb loaded = ModEntities.ASPECT_ORB.get().create(level, EntitySpawnReason.TRIGGERED);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
        helper.assertTrue(loaded.getAspect() == Aspect.FIRE, "aspect orb saves and restores its aspect");
        helper.assertValueEqual(loaded.getAspectValue(), 1, "aspect orb saves and restores its amount");
        helper.assertValueEqual(loaded.getAge(), 1, "aspect orb saves and restores lifetime");

        ServerPlayer player = mockServerPlayer(helper);
        ItemStack wand = Wands.create(thaumcraft.wand.WandRegistry.cap("iron"), thaumcraft.wand.WandRegistry.rod("greatwood"), false);
        player.getInventory().setItem(0, wand);
        loaded.setPos(player.getX(), player.getY(), player.getZ());
        loaded.playerTouch(player);
        helper.assertTrue(loaded.isRemoved(), "touching a primal orb with wand space consumes it");
        helper.assertValueEqual(Wands.getVis(wand, Aspect.FIRE), 100, "one vis picked up is stored as 100 centivis");

        Wands.storeVis(wand, Aspect.FIRE, Wands.getMaxVis(wand) - 200);
        AspectOrb partial = AspectOrb.create(level, player.getX(), player.getY(), player.getZ(), Aspect.FIRE, 7);
        partial.playerTouch(player);
        helper.assertFalse(partial.isRemoved(), "recent XP pickup cooldown blocks an immediate second orb pickup");
        ServerPlayer partialPlayer = mockServerPlayer(helper);
        ItemStack partialWand = Wands.create(thaumcraft.wand.WandRegistry.cap("iron"), thaumcraft.wand.WandRegistry.rod("greatwood"), false);
        partialPlayer.getInventory().setItem(0, partialWand);
        Wands.storeVis(partialWand, Aspect.FIRE, Wands.getMaxVis(partialWand) - 200);
        AspectOrb partialCapacity = AspectOrb.create(level, partialPlayer.getX(), partialPlayer.getY(), partialPlayer.getZ(), Aspect.FIRE, 7);
        partialCapacity.playerTouch(partialPlayer);
        helper.assertTrue(partialCapacity.isRemoved(), "partial wand capacity still attracts and consumes the orb");
        helper.assertValueEqual(Wands.getVis(partialWand, Aspect.FIRE), Wands.getMaxVis(partialWand), "partial pickup caps the wand at capacity");

        BlockPos nodePos = helper.absolutePos(new BlockPos(2, 2, 5));
        helper.assertTrue(NodeHooks.spawnAspectOrb(level, nodePos, Aspect.AIR, 3), "node hook successfully spawns aspect orb");
        helper.assertTrue(!level.getEntitiesOfClass(AspectOrb.class, new net.minecraft.world.phys.AABB(nodePos).inflate(2.0)).isEmpty(), "node orb is present near node");
        helper.succeed();
    }

    private static ServerPlayer mockServerPlayer(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.mockServerPlayer(helper, GameType.SURVIVAL);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(player.getGameProfile(), false);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, cookie) {
            @Override public void send(Packet<?> packet) {}
        };
        player.connection.markClientLoaded();
        return player;
    }
}
