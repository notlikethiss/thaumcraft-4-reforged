package thaumcraft.test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import thaumcraft.entity.boss.EldritchWarden;
import thaumcraft.entity.boss.CultistLeader;
import thaumcraft.registry.ModItems;
import thaumcraft.entity.boss.TaintacleGiant;
import thaumcraft.entity.monster.ChampionModifier;
import thaumcraft.registry.EldritchContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.entity.boss.CultistPortal;
import thaumcraft.entity.boss.EldritchGolem;
import thaumcraft.registry.EldritchEntities;
import thaumcraft.entity.monster.Cultist;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class BossGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final String[] TESTS = {
        "eldritch_golem_enters_headless_phase",
        "cultist_portal_starts_minion_wave",
        "eldritch_warden_enters_frenzy_once",
        "taint_boss_encounter_has_five_champions",
        "boss_phase_and_portal_wave_survive_save",
        "cultist_leader_drops_one_tier_two_reward",
        "base_boss_drops_pearl_and_rare_bag",
        "cultist_portal_drops_one_pearl",
        "taintacle_giant_last_death_drops_pearl",
        "boss_spawn_ai_pause_noai_and_resume",
        "warden_frenzy_pause_teleport_and_absorption",
        "cultist_leader_spawn_enchantment",
        "portal_preparation_save_and_leader_home",
        "boss_aggro_preserves_original_scaling",
        "portal_final_leader_home",
        "portal_death_explosion_and_pearl"
    };

    static {
        FUNCTIONS.register(TESTS[0], () -> BossGameTests::golemEntersHeadlessPhase);
        FUNCTIONS.register(TESTS[1], () -> BossGameTests::portalStartsMinionWave);
        FUNCTIONS.register(TESTS[2], () -> BossGameTests::wardenEntersFrenzyOnce);
        FUNCTIONS.register(TESTS[3], () -> BossGameTests::taintEncounterHasFiveChampions);
        FUNCTIONS.register(TESTS[4], () -> BossGameTests::phaseAndWaveSurviveSave);
        FUNCTIONS.register(TESTS[5], () -> BossGameTests::leaderRewardIsUnique);
        FUNCTIONS.register(TESTS[6], () -> BossGameTests::baseBossRewardIsUnique);
        FUNCTIONS.register(TESTS[7], () -> BossGameTests::portalRewardIsUnique);
        FUNCTIONS.register(TESTS[8], () -> BossGameTests::lastGiantRewardIsUnique);
        FUNCTIONS.register(TESTS[9], () -> BossGameTests::bossSpawnAiPauseNoAiAndResume);
        FUNCTIONS.register(TESTS[10], () -> BossGameTests::wardenFrenzyPauseTeleportAndAbsorption);
        FUNCTIONS.register(TESTS[11], () -> BossGameTests::cultistLeaderSpawnEnchantment);
        FUNCTIONS.register(TESTS[12], () -> BossGameTests::portalPreparationSaveAndLeaderHome);
        FUNCTIONS.register(TESTS[13], () -> BossGameTests::bossAggroPreservesOriginalScaling);
        FUNCTIONS.register(TESTS[14], () -> BossGameTests::portalFinalLeaderHome);
        FUNCTIONS.register(TESTS[15], () -> BossGameTests::portalDeathExplosionAndPearl);
    }

    private BossGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Thaumcraft.id("boss_default"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name));
            event.registerTest(Thaumcraft.id(name), new FunctionGameTestInstance(
                function,
                new TestData<>(environment, Thaumcraft.id("test/platform"), 500, 0, true)
            ));
        }
    }

    private static void golemEntersHeadlessPhase(GameTestHelper helper) {
        EldritchGolem golem = EldritchEntities.ELDRITCH_GOLEM.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(golem != null, "golem factory returned null");
        golem.snapTo(helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(golem);
        boolean hit = golem.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 300.0F);
        helper.assertFalse(hit, "first lethal hit should trigger the headless phase");
        helper.assertTrue(golem.isAlive(), "golem died instead of entering its second phase");
        helper.assertTrue(golem.isHeadless(), "golem did not synchronize its headless phase");
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        golem.saveWithoutId(output);
        EldritchGolem loaded = EldritchEntities.ELDRITCH_GOLEM.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        helper.assertTrue(loaded.isHeadless(), "headless phase did not survive entity save/load");
        helper.succeed();
    }

    private static void portalStartsMinionWave(GameTestHelper helper) {
        CultistPortal portal = EldritchEntities.CULTIST_PORTAL.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(portal != null, "portal factory returned null");
        net.minecraft.core.BlockPos pos = helper.absolutePos(new net.minecraft.core.BlockPos(3, 2, 3));
        portal.snapTo(pos, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(portal);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(player.getGameProfile(), false);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, cookie) {
            @Override
            public void send(Packet<?> packet) {
            }
        };
        player.snapTo(pos.getX() + 2.0, pos.getY(), pos.getZ() + 2.0, 0.0F, 0.0F);
        helper.getLevel().addNewPlayer(player);
        player.connection.markClientLoaded();
        for (int tick = 0; tick < 201; tick++) {
            portal.tick();
        }
        helper.assertTrue(portal.getStage() > 0, "portal did not advance after its initial countdown");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(Cultist.class, portal.getBoundingBox().inflate(8.0)).isEmpty(), "portal did not spawn its first cultist");
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        portal.saveWithoutId(output);
        CultistPortal loaded = EldritchEntities.CULTIST_PORTAL.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        helper.assertValueEqual(loaded.getStage(), portal.getStage(), "portal wave stage survives entity save/load");
        helper.succeed();
    }

    private static void wardenEntersFrenzyOnce(GameTestHelper helper) {
        EldritchWarden warden = EldritchEntities.ELDRITCH_WARDEN.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(warden != null, "warden factory returned null");
        warden.snapTo(helper.absolutePos(new BlockPos(2, 2, 2)), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(warden);
        warden.setAbsorptionAmount(0.0F);
        warden.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1.0F);
        helper.assertTrue(warden.getFrenzyTicks() > 0, "warden did not enter frenzy when its absorption shield broke");
        warden.setAbsorptionAmount(10.0F);
        warden.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1.0F);
        helper.assertTrue(warden.getFrenzyTicks() > 0, "warden prematurely left or restarted frenzy");
        helper.succeed();
    }

    private static void taintEncounterHasFiveChampions(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(new BlockPos(8, 2, 8));
        helper.assertTrue(EldritchEntities.spawnTaintEncounter(helper.getLevel(), center), "taint encounter spawn failed");
        var bosses = helper.getLevel().getEntitiesOfClass(thaumcraft.entity.monster.taint.Taintacle.class, new AABB(center).inflate(9.0));
        helper.assertValueEqual(bosses.size(), 5, "taint encounter entity count");
        for (var boss : bosses) {
            helper.assertTrue(ChampionModifier.get(boss) >= 0, "taint encounter member lacks champion modifier");
            boss.discard();
        }
        helper.succeed();
    }

    private static void leaderRewardIsUnique(GameTestHelper helper) {
        CultistLeader leader = EldritchEntities.CULTIST_LEADER.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(leader != null, "leader factory returned null");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        leader.snapTo(pos, 0.0F, 0.0F);
        leader.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(pos), EntitySpawnReason.TRIGGERED, null);
        ChampionModifier.assign(leader, 0, false);
        helper.getLevel().addFreshEntity(leader);
        leader.setHealth(1.0F);
        helper.assertTrue(leader.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 10.0F), "lethal leader hit was rejected");
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4.0));
        long bags = drops.stream().filter(drop -> drop.getItem().is(ModItems.RARE_LOOT_BAG.get())).count();
        long pearls = drops.stream().filter(drop -> drop.getItem().is(EldritchContent.PRIMORDIAL_PEARL.get())).count();
        helper.assertValueEqual(bags, 1L, "leader tier two reward count");
        helper.assertValueEqual(pearls, 0L, "leader must not drop the base boss pearl");
        helper.succeed();
    }

    private static void baseBossRewardIsUnique(GameTestHelper helper) {
        EldritchGolem golem = EldritchEntities.ELDRITCH_GOLEM.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(golem != null, "golem factory returned null");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        golem.snapTo(pos, 0.0F, 0.0F);
        golem.setHeadless(true);
        golem.setSpawnTimer(0);
        golem.setHealth(1.0F);
        ChampionModifier.assign(golem, 0, false);
        helper.assertTrue(helper.getLevel().addFreshEntity(golem), "golem could not be added");
        helper.assertTrue(golem.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 100.0F), "lethal golem hit was rejected");
        AABB bounds = new AABB(pos).inflate(4.0);
        helper.assertValueEqual(countDrops(helper, bounds, EldritchContent.PRIMORDIAL_PEARL.get()), 1L, "base boss pearl count");
        helper.assertValueEqual(countDrops(helper, bounds, ModItems.RARE_LOOT_BAG.get()), 1L, "base boss rare bag count");
        helper.succeed();
    }

    private static void portalRewardIsUnique(GameTestHelper helper) {
        CultistPortal portal = EldritchEntities.CULTIST_PORTAL.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(portal != null, "portal factory returned null");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        portal.snapTo(pos, 0.0F, 0.0F);
        portal.setHealth(1.0F);
        helper.assertTrue(helper.getLevel().addFreshEntity(portal), "portal could not be added");
        helper.assertTrue(portal.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 100.0F), "lethal portal hit was rejected");
        AABB bounds = new AABB(pos).inflate(4.0);
        helper.assertValueEqual(countDrops(helper, bounds, EldritchContent.PRIMORDIAL_PEARL.get()), 1L, "portal pearl count");
        helper.assertValueEqual(countDrops(helper, bounds, ModItems.RARE_LOOT_BAG.get()), 0L, "portal must not drop the base boss rare bag");
        helper.succeed();
    }

    private static void lastGiantRewardIsUnique(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        TaintacleGiant first = EldritchEntities.TAINTACLE_GIANT.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        TaintacleGiant last = EldritchEntities.TAINTACLE_GIANT.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(first != null && last != null, "giant factory returned null");
        first.snapTo(pos, 0.0F, 0.0F);
        last.snapTo(pos.offset(2, 0, 0), 0.0F, 0.0F);
        first.setHealth(1.0F);
        last.setHealth(1.0F);
        helper.assertTrue(helper.getLevel().addFreshEntity(first), "first giant could not be added");
        helper.assertTrue(helper.getLevel().addFreshEntity(last), "last giant could not be added");
        helper.assertTrue(first.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 100.0F), "first giant lethal hit was rejected");
        helper.assertFalse(first.isAlive(), "first giant survived: health=" + first.getHealth());
        AABB bounds = new AABB(pos).inflate(48.0);
        helper.assertValueEqual(countDrops(helper, bounds, EldritchContent.PRIMORDIAL_PEARL.get()), 0L, "non-final giant must not drop a pearl");
        helper.assertTrue(last.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 100.0F), "last giant lethal hit was rejected");
        helper.assertFalse(last.isAlive(), "last giant survived: health=" + last.getHealth());
        helper.assertValueEqual(countDrops(helper, bounds, EldritchContent.PRIMORDIAL_PEARL.get()), 1L, "last giant pearl count");
        helper.succeed();
    }

    private static long countDrops(GameTestHelper helper, AABB bounds, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds).stream()
            .filter(drop -> drop.getItem().is(item))
            .mapToLong(drop -> drop.getItem().getCount())
            .sum();
    }

    private static void bossSpawnAiPauseNoAiAndResume(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        CultistLeader leader = EldritchEntities.CULTIST_LEADER.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(leader != null, "leader factory returned null");
        leader.snapTo(pos, 0.0F, 0.0F);
        leader.setSpawnTimer(3);
        helper.getLevel().addFreshEntity(leader);
        ServerPlayer player = addMockPlayer(helper, pos.offset(1, 0, 1));
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000.0);
        player.setHealth(1000.0F);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(leader.getTarget() == null, "spawn timer allowed target AI to run");
            leader.setNoAi(true);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(leader.getTarget() == null, "NoAI mob acquired a target");
                leader.setNoAi(false);
                leader.getRandom().setSeed(0L);
                helper.runAfterDelay(30, () -> {
                    helper.assertTrue(leader.getTarget() == player, "AI did not resume after spawn timer and NoAI ended");
                    helper.succeed();
                });
            });
        });
    }

    private static void wardenFrenzyPauseTeleportAndAbsorption(GameTestHelper helper) {
        BlockPos home = helper.absolutePos(new BlockPos(8, 2, 8));
        var level = helper.getLevel();
        List<Long> newlyForcedChunks = new ArrayList<>();
        int homeChunkX = home.getX() >> 4;
        int homeChunkZ = home.getZ() >> 4;
        for (int chunkX = homeChunkX - 1; chunkX <= homeChunkX + 1; chunkX++) {
            for (int chunkZ = homeChunkZ - 1; chunkZ <= homeChunkZ + 1; chunkZ++) {
                long chunk = ChunkPos.pack(chunkX, chunkZ);
                if (!level.getForceLoadedChunks().contains(chunk) && level.setChunkForced(chunkX, chunkZ, true)) {
                    newlyForcedChunks.add(chunk);
                }
            }
        }
        BlockPos floor = home.below();
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                helper.getLevel().setBlockAndUpdate(floor.offset(x, 0, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            }
        }
        helper.getLevel().setBlockAndUpdate(home, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        EldritchWarden warden = EldritchEntities.ELDRITCH_WARDEN.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(warden != null, "warden factory returned null");
        warden.snapTo(helper.absolutePos(new BlockPos(2, 2, 2)), 0.0F, 0.0F);
        warden.setHomeTo(home, 32);
        warden.getRandom().setSeed(0L);
        warden.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(warden.blockPosition()), EntitySpawnReason.TRIGGERED, null);
        helper.assertValueEqual(warden.getMaxAbsorption(), 132.0F, "freshly finalized warden did not receive the original absorption cap");
        helper.assertValueEqual(warden.getAbsorptionAmount(), 132.0F, "freshly finalized warden did not receive its absorption shield");
        warden.setHomeTo(home, 32);
        warden.setSpawnTimer(0);
        ChampionModifier.assign(warden, 0, false);
        helper.getLevel().addFreshEntity(warden);
        ServerPlayer player = addMockPlayer(helper, helper.absolutePos(new BlockPos(4, 2, 4)));
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000.0);
        player.setHealth(1000.0F);
        warden.setTarget(player);
        warden.setAbsorptionAmount(0.0F);
        warden.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1.0F);
        helper.assertTrue(warden.getFrenzyTicks() == 150, "warden did not start 150-tick frenzy");
        int frenzyStartTick = warden.tickCount;
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(warden.getTarget() == player, "frenzy unexpectedly cleared its target");
            helper.assertTrue(warden.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) > 0.25, "unsafe home teleport did not select a clear nearby position");
            helper.assertTrue(helper.getLevel().noCollision(warden, warden.getBoundingBox()), "warden teleported into a collision");
            warden.setTarget(null);
            waitForWardenFrenzyAndResume(helper, warden, player, newlyForcedChunks, frenzyStartTick);
        });
    }

    private static void waitForWardenFrenzyAndResume(GameTestHelper helper, EldritchWarden warden, ServerPlayer player, List<Long> newlyForcedChunks, int frenzyStartTick) {
        helper.assertTrue(warden.getTarget() == null, "warden target AI ran before the final frenzy tick");
        if (warden.getFrenzyTicks() > 0) {
            helper.runAfterDelay(1, () -> waitForWardenFrenzyAndResume(helper, warden, player, newlyForcedChunks, frenzyStartTick));
            return;
        }
        helper.assertTrue(warden.tickCount - frenzyStartTick >= 150, "warden frenzy expired before 150 entity ticks: tick=" + warden.tickCount + " start=" + frenzyStartTick);
        helper.assertValueEqual(warden.getSpawnTimer(), 0, "warden spawn timer still paused goals after frenzy");
        helper.assertTrue(movePlayerIntoClearWardenSight(helper, warden, player), "could not place living player in clear Warden line of sight");
        warden.getRandom().setSeed(0L);
        helper.runAfterDelay(30, () -> {
            String targetState = "target=" + warden.getTarget() + " playerAlive=" + player.isAlive() + " playerHealth=" + player.getHealth()
                + " playerPos=" + player.position() + " wardenPos=" + warden.position() + " distanceSqr=" + warden.distanceToSqr(player)
                + " lineOfSight=" + warden.hasLineOfSight(player) + " frenzy=" + warden.getFrenzyTicks() + " spawnTimer=" + warden.getSpawnTimer();
            helper.assertTrue(warden.getTarget() == player, "warden target AI did not resume after frenzy: " + targetState);
            player.discard();
            warden.setTarget(null);
            damageWardenOnRegenBoundary(helper, warden, newlyForcedChunks);
        });
    }

    private static boolean movePlayerIntoClearWardenSight(GameTestHelper helper, EldritchWarden warden, ServerPlayer player) {
        BlockPos center = BlockPos.containing(warden.getX(), warden.getY(), warden.getZ());
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                int distanceSqr = x * x + z * z;
                if (distanceSqr < 9 || distanceSqr > 36) {
                    continue;
                }
                BlockPos target = new BlockPos(center.getX() + x, warden.getBlockY(), center.getZ() + z);
                if (!helper.getLevel().hasChunkAt(target)) {
                    continue;
                }
                player.snapTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.0F, 0.0F);
                if (helper.getLevel().noCollision(player, player.getBoundingBox()) && warden.hasLineOfSight(player)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void damageWardenOnRegenBoundary(GameTestHelper helper, EldritchWarden warden, List<Long> newlyForcedChunks) {
        if (warden.tickCount % 25 != 10) {
            helper.runAfterDelay(1, () -> damageWardenOnRegenBoundary(helper, warden, newlyForcedChunks));
            return;
        }
        warden.setAbsorptionAmount(0.0F);
        helper.assertTrue(warden.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1.0F), "warden did not accept test damage");
        helper.assertTrue(warden.hurtTime > 0 && warden.damageCooldownTime > 10, "test damage did not start hurt animation and damage cooldown");
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(warden.hurtTime == 0 && warden.damageCooldownTime > 0, "test did not reach the post-animation damage cooldown interval");
            helper.assertValueEqual(warden.getAbsorptionAmount(), 0.0F, "absorption regenerated while damage cooldown remained");
            helper.runAfterDelay(25, () -> {
                helper.assertValueEqual(warden.tickCount % 25, 0, "absorption check did not land on the regen tick boundary");
                helper.assertValueEqual(warden.damageCooldownTime, 0, "damage cooldown was not clear at the next regen boundary");
                helper.assertValueEqual(warden.getAbsorptionAmount(), 1.0F, "absorption did not regenerate after damage cooldown ended");
                for (long chunk : newlyForcedChunks) {
                    helper.getLevel().setChunkForced(ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false);
                }
                helper.succeed();
            });
        });
    }

    private static void cultistLeaderSpawnEnchantment(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().getChunkAt(pos).setInhabitedTime(3600000L);
        boolean enchanted = false;
        var allowedEnchantments = java.util.Set.of(
            net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,
            net.minecraft.world.item.enchantment.Enchantments.SMITE,
            net.minecraft.world.item.enchantment.Enchantments.BANE_OF_ARTHROPODS,
            net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK,
            net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT,
            net.minecraft.world.item.enchantment.Enchantments.LOOTING,
            net.minecraft.world.item.enchantment.Enchantments.UNBREAKING,
            thaumcraft.registry.ModEnchantments.REPAIR
        );
        for (long attempt = 0; attempt < 64 && !enchanted; attempt++) {
            CultistLeader leader = EldritchEntities.CULTIST_LEADER.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            helper.assertTrue(leader != null, "leader factory returned null");
            leader.snapTo(pos, 0.0F, 0.0F);
            leader.getRandom().setSeed(1_000_003L * attempt + 1_234_567L);
            leader.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(pos), EntitySpawnReason.TRIGGERED, null);
            var enchantments = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentsForCrafting(leader.getMainHandItem());
            enchanted = !enchantments.isEmpty();
            for (var enchantment : enchantments.keySet()) {
                helper.assertTrue(allowedEnchantments.contains(enchantment.unwrapKey().orElse(null)), "leader received an enchantment outside the original sword pool");
            }
        }
        helper.assertTrue(enchanted, "seeded spawn sequence never produced an enchanted sword");
        helper.succeed();
    }

    private static void portalPreparationSaveAndLeaderHome(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(6, 2, 6));
        CultistPortal portal = EldritchEntities.CULTIST_PORTAL.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(portal != null, "portal factory returned null");
        portal.snapTo(pos, 0.0F, 0.0F);
        BlockPos expectedCenter = new BlockPos((int) portal.getX(), (int) portal.getY(), (int) portal.getZ());
        for (BlockPos banner : new BlockPos[] {
            expectedCenter.offset(0, 0, -6), expectedCenter.offset(0, 0, 6), expectedCenter.offset(-6, 0, 0), expectedCenter.offset(6, 0, 0)
        }) {
            helper.getLevel().setBlockAndUpdate(banner.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        }
        portal.getRandom().setSeed(0L);
        helper.getLevel().addFreshEntity(portal);
        helper.runAfterDelay(10, () -> {
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
            portal.saveWithoutId(output);
            portal.discard();
            CultistPortal restored = EldritchEntities.CULTIST_PORTAL.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
            restored.getRandom().setSeed(0L);
            helper.getLevel().addFreshEntity(restored);
            helper.runAfterDelay(35, () -> {
                BlockPos actualCenter = new BlockPos((int) restored.getX(), (int) restored.getY(), (int) restored.getZ());
                TagValueOutput diagnosticOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
                restored.saveWithoutId(diagnosticOutput);
                int stageCounter = TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), diagnosticOutput.buildResult()).getIntOr("StageCounter", -1);
                helper.assertValueEqual(actualCenter, expectedCenter, "portal center drifted before banner placement");
                helper.assertTrue(stageCounter >= 150 && stageCounter < 160, "unexpected portal preparation counter=" + stageCounter + " tick=" + restored.tickCount + " pos=" + actualCenter);
                assertBanner(helper, actualCenter.offset(0, 0, -6), 8, "north ritual banner");
                assertBanner(helper, actualCenter.offset(0, 0, 6), 0, "south ritual banner");
                assertBanner(helper, actualCenter.offset(-6, 0, 0), 4, "west ritual banner");
                assertBanner(helper, actualCenter.offset(6, 0, 0), 12, "east ritual banner");
                helper.runAfterDelay(140, () -> {
                    boolean crateFound = false;
                    for (int x = -4; x <= 4; x++) {
                        for (int z = -4; z <= 4; z++) {
                            if (helper.getLevel().getBlockState(actualCenter.offset(x, 0, z)).is(thaumcraft.registry.ModBlocks.LOOT_CRATE.get())) {
                                crateFound = true;
                            }
                        }
                    }
                    helper.assertTrue(crateFound, "portal preparation failed to produce a crate");
                    helper.succeed();
                });
            });
        });
    }

    private static void bossAggroPreservesOriginalScaling(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        EldritchWarden warden = EldritchEntities.ELDRITCH_WARDEN.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(warden != null, "warden factory returned null");
        warden.snapTo(pos, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(warden);
        ServerPlayer first = addMockPlayer(helper, pos.offset(1, 0, 0));
        ServerPlayer challenger = addMockPlayer(helper, pos.offset(2, 0, 0));
        warden.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(first), 10.0F);
        warden.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(challenger), 40.0F);
        warden.setTarget(first);
        helper.runAfterDelay(22, () -> {
            helper.assertValueEqual(warden.getMaxHealth(), 200.0F, "original challenger iteration should not add multiplayer scaling for one current player");
            helper.succeed();
        });
    }

    private static ServerPlayer addMockPlayer(GameTestHelper helper, BlockPos pos) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(player.getGameProfile(), false);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, cookie) {
            @Override
            public void send(Packet<?> packet) {
            }
        };
        player.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addNewPlayer(player);
        player.connection.markClientLoaded();
        return player;
    }

    private static void assertBanner(GameTestHelper helper, BlockPos pos, int expectedFacing, String message) {
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(thaumcraft.registry.ModBlocks.BANNER.get()), message + " block missing");
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof thaumcraft.device.BannerBlockEntity, message + " block entity missing");
        thaumcraft.device.BannerBlockEntity banner = (thaumcraft.device.BannerBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertValueEqual(banner.getFacing(), expectedFacing, message + " facing");
    }

    private static void portalFinalLeaderHome(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(5, 2, 5));
        CultistPortal portal = EldritchEntities.CULTIST_PORTAL.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(portal != null, "portal factory returned null");
        portal.snapTo(pos, 0.0F, 0.0F);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        portal.saveWithoutId(output);
        output.putInt("Stage", 12);
        output.putInt("StageCounter", 1);
        output.putBoolean("LeaderSpawned", false);
        CultistPortal restored = EldritchEntities.CULTIST_PORTAL.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        restored.snapTo(pos, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(restored);
        addMockPlayer(helper, pos.offset(2, 0, 2));
        helper.runAfterDelay(3, () -> {
            var leaders = helper.getLevel().getEntitiesOfClass(CultistLeader.class, new AABB(pos).inflate(4.0));
            helper.assertValueEqual(leaders.size(), 1, "portal final leader count");
            CultistLeader leader = leaders.getFirst();
            helper.assertValueEqual(leader.getHomePosition(), pos, "portal leader home center");
            helper.assertValueEqual(leader.getHomeRadius(), 32, "portal leader home radius");
            helper.succeed();
        });
    }

    private static void portalDeathExplosionAndPearl(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(5, 2, 5));
        CultistPortal portal = EldritchEntities.CULTIST_PORTAL.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        net.minecraft.world.entity.monster.zombie.Zombie zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(portal != null && zombie != null, "portal or zombie factory returned null");
        portal.snapTo(pos, 0.0F, 0.0F);
        portal.setHealth(1.0F);
        BlockPos protectedBlock = pos.offset(-3, 0, 0);
        helper.getLevel().setBlockAndUpdate(protectedBlock, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        zombie.snapTo(pos.getX() + 2.0, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(portal);
        helper.getLevel().addFreshEntity(zombie);
        float healthBefore = zombie.getHealth();
        net.minecraft.world.phys.Vec3 velocityBefore = zombie.getDeltaMovement();
        helper.assertTrue(portal.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 2.0F), "lethal portal hit was rejected");
        helper.assertTrue(zombie.getHealth() < healthBefore, "portal death explosion did not damage a nearby entity");
        helper.assertTrue(zombie.getDeltaMovement().subtract(velocityBefore).lengthSqr() > 0.01, "portal death explosion did not knock back a nearby entity");
        helper.assertTrue(helper.getLevel().getBlockState(protectedBlock).is(net.minecraft.world.level.block.Blocks.STONE), "portal explosion damaged blocks");
        helper.assertValueEqual(countDrops(helper, new AABB(pos).inflate(4.0), EldritchContent.PRIMORDIAL_PEARL.get()), 1L, "portal death pearl count");
        helper.succeed();
    }

    private static void phaseAndWaveSurviveSave(GameTestHelper helper) {
        EldritchGolem golem = EldritchEntities.ELDRITCH_GOLEM.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        EldritchGolem loadedGolem = EldritchEntities.ELDRITCH_GOLEM.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        golem.setHeadless(true);
        golem.setBeamCharge(91);
        TagValueOutput golemOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        golem.saveWithoutId(golemOutput);
        loadedGolem.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), golemOutput.buildResult()));
        helper.assertTrue(loadedGolem.isHeadless() && loadedGolem.getBeamCharge() == 91, "golem beam phase not saved");
        CultistPortal portal = EldritchEntities.CULTIST_PORTAL.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        CultistPortal loadedPortal = EldritchEntities.CULTIST_PORTAL.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        TagValueOutput portalOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        portal.saveWithoutId(portalOutput);
        loadedPortal.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), portalOutput.buildResult()));
        helper.assertValueEqual(loadedPortal.getStage(), portal.getStage(), "portal stage is stable across save/load");
        helper.succeed();
    }
}
