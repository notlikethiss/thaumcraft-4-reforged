package thaumcraft.test;

import java.util.function.Consumer;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.util.RandomSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.effect.TaintPoisonEvents;
import thaumcraft.entity.TaintedMob;
import thaumcraft.entity.monster.taint.TaintMobFactory;
import thaumcraft.entity.monster.taint.Taintacle;
import thaumcraft.entity.monster.taint.TaintacleTiny;
import thaumcraft.entity.monster.taint.TaintSpore;
import thaumcraft.entity.monster.taint.TaintSwarmer;
import thaumcraft.entity.monster.taint.TaintSwarm;
import thaumcraft.entity.monster.taint.TaintedCreeper;
import thaumcraft.entity.monster.taint.TaintedCow;
import thaumcraft.entity.monster.taint.TaintedVillager;
import thaumcraft.entity.monster.taint.TaintedVillagerIndoorGoal;
import thaumcraft.entity.monster.taint.TaintedVillagerRestrictDoorGoal;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModEffects;
import thaumcraft.registry.ModEntities;
import thaumcraft.taint.TaintBiomes;
import thaumcraft.taint.TaintSpread;
import thaumcraft.blockentity.EtherealBloomBlockEntity;
import thaumcraft.platform.BiomeEdit;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class TaintGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final String[] TESTS = {
        "taint_fibres_spread_to_replaceable_ground",
        "taint_flux_seeds_fibres_and_biome",
        "taint_bloom_restores_original_biome",
        "taint_bloom_server_tick_purifies_nearby_columns",
        "taint_tentacle_stays_stationary",
        "taint_poison_replacement_preserves_position",
        "taint_poison_conversion_removes_original",
        "taint_poison_ignores_tainted_mobs",
        "taint_spore_grows_after_1201_ticks",
        "taint_spore_starves_outside_taint_biome",
        "taint_spore_detachment_releases_spiders",
        "taint_spore_burst_allows_zero_spiders",
        "taint_tiny_tentacle_expires_after_200_ticks",
        "taint_swarmer_spawns_for_vulnerable_players_without_starving",
        "taint_swarmer_first_death_spawns_one_swarm",
        "taint_creeper_fuse_infects_without_block_damage",
        "taint_entity_saved_data_round_trips",
        "tainted_villager_enters_and_restricts_inside_house",
        "taint_trees_reject_water_over_dirt",
        "taint_trees_reject_water_over_stone",
        "taint_trees_grow_on_dry_soil"
    };

    static {
        FUNCTIONS.register(TESTS[0], () -> TaintGameTests::fibresSpreadToReplaceableGround);
        FUNCTIONS.register(TESTS[1], () -> TaintGameTests::fluxSeedsFibresAndBiome);
        FUNCTIONS.register(TESTS[2], () -> TaintGameTests::bloomRestoresOriginalBiome);
        FUNCTIONS.register(TESTS[3], () -> TaintGameTests::bloomServerTickPurifiesNearbyColumns);
        FUNCTIONS.register(TESTS[4], () -> TaintGameTests::tentacleStaysStationary);
        FUNCTIONS.register(TESTS[5], () -> TaintGameTests::poisonReplacementPreservesPosition);
        FUNCTIONS.register(TESTS[6], () -> TaintGameTests::poisonConversionRemovesOriginal);
        FUNCTIONS.register(TESTS[7], () -> TaintGameTests::poisonIgnoresTaintedMobs);
        FUNCTIONS.register(TESTS[8], () -> TaintGameTests::sporeGrowsAfter1201Ticks);
        FUNCTIONS.register(TESTS[9], () -> TaintGameTests::sporeStarvesOutsideTaintBiome);
        FUNCTIONS.register(TESTS[10], () -> TaintGameTests::sporeDetachmentReleasesSpiders);
        FUNCTIONS.register(TESTS[11], () -> TaintGameTests::sporeBurstAllowsZeroSpiders);
        FUNCTIONS.register(TESTS[12], () -> TaintGameTests::tinyTentacleExpiresAfter200Ticks);
        FUNCTIONS.register(TESTS[13], () -> TaintGameTests::swarmerSpawnsForVulnerablePlayersWithoutStarving);
        FUNCTIONS.register(TESTS[14], () -> TaintGameTests::swarmerFirstDeathSpawnsOneSwarm);
        FUNCTIONS.register(TESTS[15], () -> TaintGameTests::creeperFuseInfectsWithoutBlockDamage);
        FUNCTIONS.register(TESTS[16], () -> TaintGameTests::entitySavedDataRoundTrips);
        FUNCTIONS.register(TESTS[17], () -> TaintGameTests::taintedVillagerEntersAndRestrictsInsideHouse);
        FUNCTIONS.register(TESTS[18], () -> TaintGameTests::treesRejectWaterOverDirt);
        FUNCTIONS.register(TESTS[19], () -> TaintGameTests::treesRejectWaterOverStone);
        FUNCTIONS.register(TESTS[20], () -> TaintGameTests::treesGrowOnDrySoil);
    }

    private TaintGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Thaumcraft.id("taint_default"), new TestEnvironmentDefinition.AllOf());
        var growthEnvironment = event.registerEnvironment(Thaumcraft.id("taint_spore_growth"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name));
            int timeout = name.equals("taint_spore_grows_after_1201_ticks") ? 1250
                : name.equals("taint_swarmer_spawns_for_vulnerable_players_without_starving") ? 550
                : name.equals("taint_tiny_tentacle_expires_after_200_ticks") ? 220
                : name.equals("taint_bloom_server_tick_purifies_nearby_columns") ? 300 : 100;
            event.registerTest(
                Thaumcraft.id(name),
                new FunctionGameTestInstance(function, new TestData<>(name.equals("taint_spore_grows_after_1201_ticks") ? growthEnvironment : environment, Thaumcraft.id("test/platform"), timeout, 0, true))
            );
        }
    }

    private static void fibresSpreadToReplaceableGround(GameTestHelper helper) {
        BlockPos target = new BlockPos(3, 2, 3);
        helper.setBlock(target.below(), Blocks.STONE);
        helper.setBlock(target.east(), ModBlocks.FIBROUS_TAINT.get());
        BlockPos worldTarget = helper.absolutePos(target);
        helper.assertTrue(TaintSpread.spreadFibres(helper.getLevel(), worldTarget), "fibres rejected replaceable air next to solid support");
        helper.assertTrue(TaintSpread.isTaintBlock(helper.getBlockState(target)), "fibres spread did not place a taint block");
        helper.succeed();
    }

    private static void fluxSeedsFibresAndBiome(GameTestHelper helper) {
        BlockPos target = new BlockPos(3, 2, 3);
        helper.setBlock(target.below(), Blocks.STONE);
        BlockPos worldTarget = helper.absolutePos(target);
        helper.assertTrue(TaintSpread.taintFromFlux(helper.getLevel(), worldTarget), "flux failed to seed taint");
        helper.assertTrue(helper.getBlockState(target).is(ModBlocks.FIBROUS_TAINT.get()), "flux did not place fibres on supported air");
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(TaintBiomes.isTainted(helper.getLevel(), worldTarget), "flux did not taint the biome column");
            helper.succeed();
        });
    }

    private static void bloomRestoresOriginalBiome(GameTestHelper helper) {
        BlockPos target = new BlockPos(3, 2, 3);
        BlockPos worldTarget = helper.absolutePos(target);
        var original = BiomeEdit.getColumnBiome(helper.getLevel(), worldTarget);
        helper.assertTrue(TaintBiomes.set(helper.getLevel(), worldTarget, TaintBiomes.TAINTED_LAND), "test setup failed to taint biome column");
        helper.assertTrue(TaintBiomes.isTainted(helper.getLevel(), worldTarget), "test setup did not taint the edited quart column");
        helper.assertTrue(TaintBiomes.purify(helper.getLevel(), worldTarget), "purification did not restore a biome column");
        var expected = original.is(TaintBiomes.TAINTED_LAND)
            ? helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS)
            : original;
        helper.assertValueEqual(BiomeEdit.getColumnBiome(helper.getLevel(), worldTarget), expected, "purification did not restore the original biome source");
        helper.succeed();
    }

    private static void bloomServerTickPurifiesNearbyColumns(GameTestHelper helper) {
        BlockPos bloomPos = helper.absolutePos(new BlockPos(8, 3, 8));
        BlockPos outsideRadius = bloomPos.offset(12, 0, 0);
        for (int x = -12; x <= 12; x += 4) {
            for (int z = -12; z <= 12; z += 4) {
                BlockPos column = bloomPos.offset(x, 0, z);
                TaintBiomes.set(helper.getLevel(), column, TaintBiomes.TAINTED_LAND);
            }
        }
        TaintBiomes.set(helper.getLevel(), outsideRadius, TaintBiomes.TAINTED_LAND);
        helper.setBlock(new BlockPos(8, 2, 8), Blocks.STONE);
        helper.setBlock(new BlockPos(8, 3, 8), ModBlocks.ETHEREAL_BLOOM.get());
        helper.assertTrue(helper.getBlockEntity(new BlockPos(8, 3, 8), EtherealBloomBlockEntity.class) != null, "bloom block did not create its block entity");
        helper.runAfterDelay(250, () -> {
            boolean purified = false;
            for (int x = -12; x <= 12; x += 4) {
                for (int z = -12; z <= 12; z += 4) {
                    if (!TaintBiomes.isTainted(helper.getLevel(), bloomPos.offset(x, 0, z))) {
                        purified = true;
                    }
                }
            }
            helper.assertTrue(purified, "bloom server ticks did not purify a nearby tainted biome column");
            helper.assertTrue(TaintBiomes.isTainted(helper.getLevel(), outsideRadius), "bloom purified a biome column outside its radius");
            helper.succeed();
        });
    }

    private static void tentacleStaysStationary(GameTestHelper helper) {
        Taintacle tentacle = ModEntities.TAINTACLE.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(tentacle != null, "failed to create taintacle");
        BlockPos worldPos = helper.absolutePos(new BlockPos(3, 2, 3));
        tentacle.absSnapTo(worldPos.getX() + 0.5, worldPos.getY(), worldPos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(tentacle);
        tentacle.move(MoverType.SELF, new Vec3(2.0, 2.0, -3.0));
        helper.assertValueEqual(tentacle.getX(), worldPos.getX() + 0.5, "taintacle accepted horizontal movement");
        helper.assertValueEqual(tentacle.getZ(), worldPos.getZ() + 0.5, "taintacle accepted horizontal movement");
        helper.assertValueEqual(tentacle.getY(), (double) worldPos.getY(), "taintacle accepted upward movement");
        helper.succeed();
    }

    private static void poisonReplacementPreservesPosition(GameTestHelper helper) {
        Cow original = EntityType.COW.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(original != null, "failed to create test cow");
        BlockPos worldPos = helper.absolutePos(new BlockPos(2, 2, 4));
        original.absSnapTo(worldPos.getX() + 0.25, worldPos.getY(), worldPos.getZ() + 0.75, 47.0F, 0.0F);
        Entity replacement = TaintMobFactory.createReplacement(original);
        helper.assertTrue(replacement instanceof TaintedCow, "cow replacement did not create tainted cow");
        helper.assertTrue(replacement instanceof TaintedMob, "replacement does not implement tainted mob contract");
        helper.assertValueEqual(replacement.getX(), original.getX(), "replacement changed source x coordinate");
        helper.assertValueEqual(replacement.getY(), original.getY(), "replacement changed source y coordinate");
        helper.assertValueEqual(replacement.getZ(), original.getZ(), "replacement changed source z coordinate");
        helper.assertValueEqual(replacement.getYRot(), original.getYRot(), "replacement changed source yaw");
        helper.succeed();
    }

    private static void poisonConversionRemovesOriginal(GameTestHelper helper) {
        Cow original = EntityType.COW.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(original != null, "failed to create test cow");
        BlockPos worldPos = helper.absolutePos(new BlockPos(2, 2, 4));
        double x = worldPos.getX() + 0.25;
        double y = worldPos.getY();
        double z = worldPos.getZ() + 0.75;
        original.absSnapTo(x, y, z, 47.0F, 16.0F);
        helper.getLevel().addFreshEntity(original);
        original.addEffect(new MobEffectInstance(ModEffects.TAINT_POISON, 100, 0));
        original.hurtServer(helper.getLevel(), helper.getLevel().damageSources().genericKill(), original.getMaxHealth() + 1.0F);
        helper.assertTrue(original.isRemoved(), "original entity was not removed after replacement");
        var replacements = helper.getLevel().getEntitiesOfClass(TaintedCow.class, original.getBoundingBox().inflate(2.0));
        helper.assertValueEqual(replacements.size(), 1, "poison conversion should add exactly one tainted cow");
        TaintedCow replacement = replacements.getFirst();
        helper.assertValueEqual(replacement.getX(), x, "replacement changed source x coordinate");
        helper.assertValueEqual(replacement.getY(), y, "replacement changed source y coordinate");
        helper.assertValueEqual(replacement.getZ(), z, "replacement changed source z coordinate");
        helper.assertValueEqual(replacement.getYRot(), 47.0F, "replacement changed source yaw");
        helper.assertValueEqual(replacement.getXRot(), 0.0F, "replacement did not reset source pitch");
        helper.succeed();
    }

    private static void poisonIgnoresTaintedMobs(GameTestHelper helper) {
        TaintedCow tainted = ModEntities.TAINTED_COW.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(tainted != null, "failed to create tainted cow");
        BlockPos worldPos = helper.absolutePos(new BlockPos(3, 2, 3));
        tainted.absSnapTo(worldPos.getX() + 0.5, worldPos.getY(), worldPos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(tainted);
        tainted.addEffect(new MobEffectInstance(ModEffects.TAINT_POISON, 100, 0));
        TaintPoisonEvents.onLivingDeath(new LivingDeathEvent(tainted, helper.getLevel().damageSources().generic()));
        helper.assertTrue(!tainted.isRemoved(), "tainted mob should be immune to poison death replacement");
        helper.assertValueEqual(
            helper.getLevel().getEntitiesOfClass(TaintedCow.class, tainted.getBoundingBox().inflate(2.0)).size(),
            1,
            "tainted mob should not create another replacement"
        );
        helper.succeed();
    }

    private static void sporeGrowsAfter1201Ticks(GameTestHelper helper) {
        BlockPos stalkPos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlockAndUpdate(stalkPos.below(), Blocks.BEDROCK.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(stalkPos, ModBlocks.MATURE_SPORE_STALK.get().defaultBlockState());
        TaintBiomes.set(helper.getLevel(), stalkPos, TaintBiomes.TAINTED_LAND);
        TaintSpore spore = TaintSpore.create(helper.getLevel(), stalkPos);
        helper.assertTrue(spore != null, "failed to create attached spore");
        spore.setPersistenceRequired();
        helper.getLevel().addFreshEntity(spore);
        awaitSporeGrowth(helper, spore, stalkPos);
    }

    private static void awaitSporeGrowth(GameTestHelper helper, TaintSpore spore, BlockPos stalkPos) {
        if (spore.isRemoved() || !spore.isAlive()) {
            helper.fail("attached spore died before growth: tickCount=" + spore.tickCount + ", pos=" + spore.blockPosition()
                + ", stalk=" + helper.getLevel().getBlockState(stalkPos) + ", tainted=" + TaintBiomes.isTainted(helper.getLevel(), spore.blockPosition()));
            return;
        }
        if (spore.tickCount < 1201) {
            helper.runAfterDelay(1, () -> awaitSporeGrowth(helper, spore, stalkPos));
            return;
        }
        helper.assertValueEqual(spore.getSporeSize(), 3, "spore did not grow after its 1201st update: tickCount=" + spore.tickCount
            + ", pos=" + spore.blockPosition() + ", stalk=" + helper.getLevel().getBlockState(stalkPos)
            + ", tainted=" + TaintBiomes.isTainted(helper.getLevel(), spore.blockPosition()));
        helper.succeed();
    }

    private static void sporeStarvesOutsideTaintBiome(GameTestHelper helper) {
        TaintSpore spore = ModEntities.TAINT_SPORE.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(spore != null, "failed to create spore");
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        TaintBiomes.set(helper.getLevel(), pos, Biomes.PLAINS);
        spore.absSnapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(spore);
        helper.runAfterDelay(22, () -> {
            helper.assertTrue(spore.isRemoved(), "spore did not starve outside a tainted biome");
            helper.succeed();
        });
    }

    private static void sporeDetachmentReleasesSpiders(GameTestHelper helper) {
        BlockPos stalkPos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlockAndUpdate(stalkPos, ModBlocks.MATURE_SPORE_STALK.get().defaultBlockState());
        TaintSpore spore = TaintSpore.create(helper.getLevel(), stalkPos);
        helper.assertTrue(spore != null, "failed to create attached spore");
        helper.getLevel().addFreshEntity(spore);
        helper.getLevel().setBlockAndUpdate(stalkPos, Blocks.AIR.defaultBlockState());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(spore.isRemoved(), "detached spore did not burst");
            int spiders = helper.getLevel().getEntitiesOfClass(
                thaumcraft.entity.monster.taint.TaintSpider.class, spore.getBoundingBox().inflate(4.0)
            ).size();
            helper.assertTrue(spiders >= 0 && spiders <= 1, "size-2 detached spore released an invalid spider count: " + spiders);
            helper.succeed();
        });
    }

    private static void sporeBurstAllowsZeroSpiders(GameTestHelper helper) {
        TaintSpore spore = ModEntities.TAINT_SPORE.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(spore != null, "failed to create spore for zero-spider burst test");
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        spore.absSnapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(spore);
        long seed = 0;
        while (RandomSource.create(seed).nextInt(2) != 0) {
            seed += 0x9E3779B97F4A7C15L;
        }
        helper.getLevel().getRandom().setSeed(seed);
        spore.hurt(helper.getLevel().damageSources().generic(), 100.0F);
        helper.assertTrue(spore.isRemoved(), "spore did not die to trigger its burst");
        int spiders = helper.getLevel().getEntitiesOfClass(thaumcraft.entity.monster.taint.TaintSpider.class, spore.getBoundingBox().inflate(4.0)).size();
        helper.assertValueEqual(spiders, 0, "size-2 spore did not use zero-spider burst result");
        helper.succeed();
    }

    private static void tinyTentacleExpiresAfter200Ticks(GameTestHelper helper) {
        TaintacleTiny tentacle = ModEntities.TAINTACLE_TINY.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(tentacle != null, "failed to create tiny tentacle");
        tentacle.setPersistenceRequired();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        TaintBiomes.set((net.minecraft.server.level.ServerLevel) helper.getLevel(), pos, TaintBiomes.TAINTED_LAND);
        tentacle.absSnapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(tentacle);
        helper.runAfterDelay(198, () -> {
            helper.assertTrue(tentacle.isAlive(), "tiny tentacle expired before its 200-tick lifetime");
            helper.runAfterDelay(4, () -> {
                helper.assertTrue(!tentacle.isAlive(), "tiny tentacle survived beyond its 200-tick lifetime");
                helper.succeed();
            });
        });
    }

    private static void swarmerSpawnsForVulnerablePlayersWithoutStarving(GameTestHelper helper) {
        TaintSwarmer swarmer = ModEntities.TAINT_SPORE_SWARMER.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(swarmer != null, "failed to create spore swarmer");
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        swarmer.absSnapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        float initialHealth = swarmer.getHealth();
        helper.getLevel().addFreshEntity(swarmer);
        ServerPlayer player = TestPlayers.mockServerPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(player.getGameProfile(), false);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, cookie) {
            @Override
            public void send(Packet<?> packet) {
            }
        };
        helper.getLevel().addNewPlayer(player);
        player.connection.markClientLoaded();
        player.absSnapTo(pos.getX() + 1.0, pos.getY(), pos.getZ() + 1.0, 0.0F, 0.0F);
        helper.assertTrue(helper.getLevel().getNearestPlayer(player.getX(), player.getY(), player.getZ(), 16.0, entity -> entity == player) == player,
            "mock player was not visible to level nearest-player queries");
        helper.runAfterDelay(499, () -> {
            int earlySwarms = helper.getLevel().getEntitiesOfClass(TaintSwarm.class, swarmer.getBoundingBox().inflate(3.0)).size();
            helper.assertValueEqual(earlySwarms, 0, "swarmer released a swarm before its 500-tick interval");
            helper.runAfterDelay(3, () -> {
                int swarms = helper.getLevel().getEntitiesOfClass(TaintSwarm.class, swarmer.getBoundingBox().inflate(3.0)).size();
                helper.assertValueEqual(swarms, 1, "swarmer did not release one swarm at its 500-tick interval");
                helper.assertTrue(swarmer.isAlive(), "swarmer unexpectedly died");
                helper.assertValueEqual(swarmer.getHealth(), initialHealth, "swarmer inherited spore starvation damage");
                helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
                helper.succeed();
            });
        });
    }

    private static void swarmerFirstDeathSpawnsOneSwarm(GameTestHelper helper) {
        TaintSwarmer swarmer = ModEntities.TAINT_SPORE_SWARMER.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(swarmer != null, "failed to create spore swarmer");
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        swarmer.absSnapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(swarmer);

        helper.assertTrue(swarmer.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1000.0F), "lethal damage was rejected");
        helper.assertTrue(swarmer.isDeadOrDying(), "lethal damage did not begin the swarmer death sequence");
        int swarms = helper.getLevel().getEntitiesOfClass(TaintSwarm.class, swarmer.getBoundingBox().inflate(4.0)).size();
        helper.assertValueEqual(swarms, 1, "first death must spawn exactly one swarm entity");
        helper.succeed();
    }

    private static void creeperFuseInfectsWithoutBlockDamage(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos protectedBlock = pos.east(8);
        helper.getLevel().setBlockAndUpdate(protectedBlock, Blocks.STONE.defaultBlockState());
        Cow victim = EntityType.COW.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(victim != null, "failed to create infection target");
        victim.absSnapTo(pos.getX() + 1.0, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(victim);
        TaintedCreeper creeper = ModEntities.TAINTED_CREEPER.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(creeper != null, "failed to create taint creeper");
        creeper.absSnapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(creeper);
        creeper.setTarget(victim);
        creeper.setSwellDirection(1);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(creeper.isRemoved(), "custom creeper fuse did not explode after 30 ticks");
            helper.assertTrue(helper.getLevel().getBlockState(protectedBlock).is(Blocks.STONE), "taint creeper explosion damaged blocks");
            helper.assertTrue(victim.hasEffect(ModEffects.TAINT_POISON), "taint creeper explosion did not infect a nearby living target");
            helper.succeed();
        });
    }

    private static void entitySavedDataRoundTrips(GameTestHelper helper) {
        var registryAccess = helper.getLevel().registryAccess();
        TaintSpore spore = ModEntities.TAINT_SPORE.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(spore != null, "failed to create spore for save test");
        spore.setSporeSize(7);
        TagValueOutput sporeOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registryAccess);
        spore.saveWithoutId(sporeOutput);
        TaintSpore loadedSpore = ModEntities.TAINT_SPORE.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(loadedSpore != null, "failed to create spore for load test");
        loadedSpore.load(TagValueInput.create(ProblemReporter.DISCARDING, registryAccess, sporeOutput.buildResult()));
        helper.assertValueEqual(loadedSpore.getSporeSize(), 7, "spore size did not survive entity save/load");

        TaintSwarm swarm = ModEntities.TAINT_SWARM.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(swarm != null, "failed to create swarm for save test");
        swarm.setSummoned(true);
        swarm.setDamageBonus(4);
        TagValueOutput swarmOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registryAccess);
        swarm.saveWithoutId(swarmOutput);
        TaintSwarm loadedSwarm = ModEntities.TAINT_SWARM.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(loadedSwarm != null, "failed to create swarm for load test");
        loadedSwarm.load(TagValueInput.create(ProblemReporter.DISCARDING, registryAccess, swarmOutput.buildResult()));
        helper.assertTrue(loadedSwarm.isSummoned(), "swarm summoned flag did not survive entity save/load");
        helper.assertValueEqual(loadedSwarm.getAttributeValue(Attributes.ATTACK_DAMAGE), 6.0, "swarm damage bonus did not survive entity save/load");
        helper.succeed();
    }

    private static void taintedVillagerEntersAndRestrictsInsideHouse(GameTestHelper helper) {
        var level = helper.getLevel();
        level.getServer().getCommands().performPrefixedCommand(
            level.getServer().createCommandSourceStack().withLevel(level), "time set night"
        );
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                level.setBlockAndUpdate(origin.offset(x, 1, z), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(x, 4, z), Blocks.STONE.defaultBlockState());
                for (int y = 2; y <= 3; y++) {
                    boolean wall = x == 2 || x == 6 || z == 2 || z == 6;
                    boolean doorway = x == 4 && z == 6;
                    if (wall && !doorway) {
                        level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }
        BlockPos door = origin.offset(4, 2, 6);
        var lowerDoor = Blocks.OAK_DOOR.defaultBlockState()
            .setValue(DoorBlock.FACING, Direction.NORTH)
            .setValue(DoorBlock.OPEN, true)
            .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        var upperDoor = lowerDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        level.setBlockAndUpdate(door, lowerDoor);
        level.setBlockAndUpdate(door.above(), upperDoor);
        level.setBlockAndUpdate(origin.offset(3, 2, 3), Blocks.BELL.defaultBlockState());

        helper.runAfterDelay(2, () -> {
            level.getPoiManager().ensureLoadedAndValid(level, origin.offset(4, 2, 6), 14);
            TaintedVillager villager = ModEntities.TAINTED_VILLAGER.get().create(level, EntitySpawnReason.TRIGGERED);
            helper.assertTrue(villager != null, "failed to create tainted villager");
            villager.absSnapTo(origin.getX() + 4.5, origin.getY() + 2.0, origin.getZ() + 7.5, 0.0F, 0.0F);
            level.addFreshEntity(villager);
            assertVillagerIndoorBehavior(helper, level, door, villager);
        });
    }

    private static void assertVillagerIndoorBehavior(GameTestHelper helper, net.minecraft.server.level.ServerLevel level, BlockPos door, TaintedVillager villager) {
        TaintedVillagerIndoorGoal.VillageDoorInfo doorInfo = TaintedVillagerIndoorGoal.nearestVillageDoor(level, villager.blockPosition(), 14);
        helper.assertTrue(doorInfo != null, "indoor goal did not locate the village door");
        helper.assertValueEqual(doorInfo.insidePos().getX(), door.getX(), "inside target changed door x position");
        helper.assertValueEqual(doorInfo.insidePos().getZ(), door.getZ() - 2, "inside target did not use the sheltered side");

        TaintedVillagerIndoorGoal indoorGoal = new TaintedVillagerIndoorGoal(villager);
        boolean selected = false;
        for (int attempt = 0; attempt < 200 && !selected; attempt++) {
            selected = indoorGoal.canUse();
        }
        helper.assertTrue(selected, "night indoor goal did not select a nearby village door");
        indoorGoal.start();

        villager.absSnapTo(doorInfo.insidePos().getX() + 0.5, doorInfo.insidePos().getY(), doorInfo.insidePos().getZ() + 0.5, 0.0F, 0.0F);
        TaintedVillagerRestrictDoorGoal restrictGoal = new TaintedVillagerRestrictDoorGoal(villager);
        helper.assertTrue(restrictGoal.canUse(), "door restriction did not start from the inside position");
        restrictGoal.start();
        helper.assertTrue(restrictGoal.canContinueToUse(), "door restriction did not hold while villager remained inside");
        villager.absSnapTo(door.getX() + 0.5, door.getY(), door.getZ() + 1.5, 0.0F, 0.0F);
        helper.assertTrue(!restrictGoal.canContinueToUse(), "door restriction continued after villager crossed outside");
        restrictGoal.stop();
        helper.succeed();
    }

    private static void treesRejectWaterOverDirt(GameTestHelper helper) {
        treesRejectWater(helper, Blocks.DIRT);
    }

    private static void treesRejectWaterOverStone(GameTestHelper helper) {
        treesRejectWater(helper, Blocks.STONE);
    }

    private static void treesRejectWater(GameTestHelper helper, net.minecraft.world.level.block.Block ground) {
        for (boolean big : new boolean[]{false, true}) {
            TaintTreeTestWorld world = new TaintTreeTestWorld(helper, ground, true);
            TreeRandom random = new TreeRandom(big);
            thaumcraft.world.biome.TaintDecorationHook.decorate(world.level, random, BlockPos.ZERO, helper.getLevel().getChunkSource().getGenerator());
            helper.assertValueEqual(random.treeChoices, 2, "decorator changed or retried tree attempts");
            world.assertWaterUnchanged(helper);
        }
        helper.succeed();
    }

    private static void treesGrowOnDrySoil(GameTestHelper helper) {
        for (boolean big : new boolean[]{false, true}) {
            TaintTreeTestWorld world = new TaintTreeTestWorld(helper, Blocks.GRASS_BLOCK, false);
            TreeRandom random = new TreeRandom(big);
            thaumcraft.world.biome.TaintDecorationHook.decorate(world.level, random, BlockPos.ZERO, helper.getLevel().getChunkSource().getGenerator());
            helper.assertValueEqual(random.treeChoices, 2, "decorator changed tree attempts");
            helper.assertTrue(world.logs() > 0, (big ? "big" : "ordinary") + " tainted trees failed to grow on dry soil");
        }
        helper.succeed();
    }

    private static final class TreeRandom extends net.minecraft.world.level.levelgen.LegacyRandomSource {
        private final boolean big;
        int treeChoices;

        TreeRandom(boolean big) {
            super(37L);
            this.big = big;
        }

        @Override
        public int nextInt(int bound) {
            if (bound == 10) return 1;
            if (bound == 8) {
                treeChoices++;
                return big ? 0 : 1;
            }
            return super.nextInt(bound);
        }
    }
}
