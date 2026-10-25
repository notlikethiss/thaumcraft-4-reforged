package thaumcraft.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
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

@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public final class TaintGameTests {
    @GameTest(template = "test/platform", timeoutTicks = 400)
    public static void fibresSpreadToReplaceableGround(GameTestHelper helper) {
        BlockPos target = new BlockPos(3, 3, 3);
        helper.setBlock(target.below(), Blocks.STONE);
        helper.setBlock(target.east(), ModBlocks.FIBROUS_TAINT.get());
        BlockPos worldTarget = helper.absolutePos(target);
        helper.assertTrue(TaintSpread.spreadFibres(helper.getLevel(), worldTarget), "fibres rejected replaceable air next to solid support");
        helper.assertTrue(TaintSpread.isTaintBlock(helper.getBlockState(target)), "fibres spread did not place a taint block");
        helper.succeed();
    }
    @GameTest(template = "test/platform", timeoutTicks = 400)
    public static void fluxSeedsFibresAndBiome(GameTestHelper helper) {
        BlockPos target = new BlockPos(3, 3, 3);
        helper.setBlock(target.below(), Blocks.STONE);
        BlockPos worldTarget = helper.absolutePos(target);
        helper.assertTrue(TaintSpread.taintFromFlux(helper.getLevel(), worldTarget), "flux failed to seed taint");
        helper.assertTrue(helper.getBlockState(target).is(ModBlocks.FIBROUS_TAINT.get()), "flux did not place fibres on supported air");
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(TaintBiomes.isTainted(helper.getLevel(), worldTarget), "flux did not taint the biome column");
            helper.succeed();
        });
    }
    @GameTest(template = "test/platform", timeoutTicks = 400)
    public static void bloomRestoresOriginalBiome(GameTestHelper helper) {
        BlockPos target = new BlockPos(3, 3, 3);
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
    @GameTest(template = "test/platform", timeoutTicks = 400)
    public static void bloomServerTickPurifiesNearbyColumns(GameTestHelper helper) {
        BlockPos bloomPos = helper.absolutePos(new BlockPos(8, 4, 8));
        BlockPos outsideRadius = bloomPos.offset(12, 0, 0);
        for (int x = -12; x <= 12; x += 4) {
            for (int z = -12; z <= 12; z += 4) {
                BlockPos column = bloomPos.offset(x, 0, z);
                TaintBiomes.set(helper.getLevel(), column, TaintBiomes.TAINTED_LAND);
            }
        }
        TaintBiomes.set(helper.getLevel(), outsideRadius, TaintBiomes.TAINTED_LAND);
        helper.setBlock(new BlockPos(8, 3, 8), Blocks.STONE);
        helper.setBlock(new BlockPos(8, 4, 8), ModBlocks.ETHEREAL_BLOOM.get());
        helper.assertTrue((EtherealBloomBlockEntity) helper.getBlockEntity(new BlockPos(8, 4, 8)) != null, "bloom block did not create its block entity");
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
    @GameTest(template = "test/platform", timeoutTicks = 400)
    public static void tentacleStaysStationary(GameTestHelper helper) {
        Taintacle tentacle = ModEntities.TAINTACLE.get().create(helper.getLevel());
        helper.assertTrue(tentacle != null, "failed to create taintacle");
        BlockPos worldPos = helper.absolutePos(new BlockPos(3, 3, 3));
        tentacle.moveTo(worldPos.getX() + 0.5, worldPos.getY(), worldPos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(tentacle);
        tentacle.move(MoverType.SELF, new Vec3(2.0, 2.0, -3.0));
        helper.assertValueEqual(tentacle.getX(), worldPos.getX() + 0.5, "taintacle accepted horizontal movement");
        helper.assertValueEqual(tentacle.getZ(), worldPos.getZ() + 0.5, "taintacle accepted horizontal movement");
        helper.assertValueEqual(tentacle.getY(), (double) worldPos.getY(), "taintacle accepted upward movement");
        helper.succeed();
    }
    @GameTest(template = "test/platform", timeoutTicks = 400)
    public static void poisonReplacementPreservesPosition(GameTestHelper helper) {
        Cow original = EntityType.COW.create(helper.getLevel());
        helper.assertTrue(original != null, "failed to create test cow");
        BlockPos worldPos = helper.absolutePos(new BlockPos(2, 3, 4));
        original.moveTo(worldPos.getX() + 0.25, worldPos.getY(), worldPos.getZ() + 0.75, 47.0F, 0.0F);
        Entity replacement = TaintMobFactory.createReplacement(original);
        helper.assertTrue(replacement instanceof TaintedCow, "cow replacement did not create tainted cow");
        helper.assertTrue(replacement instanceof TaintedMob, "replacement does not implement tainted mob contract");
        helper.assertValueEqual(replacement.getX(), original.getX(), "replacement changed source x coordinate");
        helper.assertValueEqual(replacement.getY(), original.getY(), "replacement changed source y coordinate");
        helper.assertValueEqual(replacement.getZ(), original.getZ(), "replacement changed source z coordinate");
        helper.assertValueEqual(replacement.getYRot(), original.getYRot(), "replacement changed source yaw");
        helper.succeed();
    }
    @GameTest(template = "test/platform", timeoutTicks = 400)
    public static void poisonConversionRemovesOriginal(GameTestHelper helper) {
        Cow original = EntityType.COW.create(helper.getLevel());
        helper.assertTrue(original != null, "failed to create test cow");
        BlockPos worldPos = helper.absolutePos(new BlockPos(2, 3, 4));
        double x = worldPos.getX() + 0.25;
        double y = worldPos.getY();
        double z = worldPos.getZ() + 0.75;
        original.moveTo(x, y, z, 47.0F, 16.0F);
        helper.getLevel().addFreshEntity(original);
        original.addEffect(new MobEffectInstance(ModEffects.TAINT_POISON, 100, 0));
        original.hurt(helper.getLevel().damageSources().genericKill(), original.getMaxHealth() + 1.0F);
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
    @GameTest(template = "test/platform", timeoutTicks = 400)
    public static void poisonIgnoresTaintedMobs(GameTestHelper helper) {
        TaintedCow tainted = ModEntities.TAINTED_COW.get().create(helper.getLevel());
        helper.assertTrue(tainted != null, "failed to create tainted cow");
        BlockPos worldPos = helper.absolutePos(new BlockPos(3, 3, 3));
        tainted.moveTo(worldPos.getX() + 0.5, worldPos.getY(), worldPos.getZ() + 0.5, 0.0F, 0.0F);
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
    @GameTest(template = "test/platform", timeoutTicks = 1800)
    public static void sporeGrowsAfter1201Ticks(GameTestHelper helper) {
        BlockPos stalkPos = helper.absolutePos(new BlockPos(3, 3, 3));
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
    @GameTest(template = "test/platform", timeoutTicks = 400)
    public static void sporeStarvesOutsideTaintBiome(GameTestHelper helper) {
        TaintSpore spore = ModEntities.TAINT_SPORE.get().create(helper.getLevel());
        helper.assertTrue(spore != null, "failed to create spore");
        BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 3));
        TaintBiomes.set(helper.getLevel(), pos, Biomes.PLAINS);
        spore.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(spore);
        helper.runAfterDelay(22, () -> {
            helper.assertTrue(spore.isRemoved(), "spore did not starve outside a tainted biome");
            helper.succeed();
        });
    }
    @GameTest(template = "test/platform", timeoutTicks = 400)
    public static void sporeDetachmentReleasesSpiders(GameTestHelper helper) {
        BlockPos stalkPos = helper.absolutePos(new BlockPos(3, 3, 3));
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
    @GameTest(template = "test/platform", timeoutTicks = 400)
    public static void sporeBurstAllowsZeroSpiders(GameTestHelper helper) {
        TaintSpore spore = ModEntities.TAINT_SPORE.get().create(helper.getLevel());
        helper.assertTrue(spore != null, "failed to create spore for zero-spider burst test");
        BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 3));
        spore.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
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
    @GameTest(template = "test/platform", timeoutTicks = 1800)
    public static void tinyTentacleExpiresAfter200Ticks(GameTestHelper helper) {
        TaintacleTiny tentacle = ModEntities.TAINTACLE_TINY.get().create(helper.getLevel());
        helper.assertTrue(tentacle != null, "failed to create tiny tentacle");
        tentacle.setPersistenceRequired();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 3));
        TaintBiomes.set((net.minecraft.server.level.ServerLevel) helper.getLevel(), pos, TaintBiomes.TAINTED_LAND);
        tentacle.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(tentacle);
        helper.runAfterDelay(198, () -> {
            helper.assertTrue(tentacle.isAlive(), "tiny tentacle expired before its 200-tick lifetime");
            helper.runAfterDelay(4, () -> {
                helper.assertTrue(!tentacle.isAlive(), "tiny tentacle survived beyond its 200-tick lifetime");
                helper.succeed();
            });
        });
    }
    @GameTest(template = "test/platform", timeoutTicks = 650)
    public static void swarmerSpawnsForVulnerablePlayersWithoutStarving(GameTestHelper helper) {
        TaintSwarmer swarmer = ModEntities.TAINT_SPORE_SWARMER.get().create(helper.getLevel());
        helper.assertTrue(swarmer != null, "failed to create spore swarmer");
        BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 3));
        swarmer.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        float initialHealth = swarmer.getHealth();
        helper.getLevel().addFreshEntity(swarmer);
        ServerPlayer player = TestSupport.mockServerPlayer(helper);
        helper.getLevel().addNewPlayer(player);
                player.moveTo(pos.getX() + 1.0, pos.getY(), pos.getZ() + 1.0, 0.0F, 0.0F);
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
    @GameTest(template = "test/platform", timeoutTicks = 400)
    public static void swarmerFirstDeathSpawnsOneSwarm(GameTestHelper helper) {
        TaintSwarmer swarmer = ModEntities.TAINT_SPORE_SWARMER.get().create(helper.getLevel());
        helper.assertTrue(swarmer != null, "failed to create spore swarmer");
        BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 3));
        swarmer.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(swarmer);

        helper.assertTrue(swarmer.hurt(helper.getLevel().damageSources().generic(), 1000.0F), "lethal damage was rejected");
        helper.assertTrue(swarmer.isDeadOrDying(), "lethal damage did not begin the swarmer death sequence");
        int swarms = helper.getLevel().getEntitiesOfClass(TaintSwarm.class, swarmer.getBoundingBox().inflate(4.0)).size();
        helper.assertValueEqual(swarms, 1, "first death must spawn exactly one swarm entity");
        helper.succeed();
    }
    @GameTest(template = "test/platform", timeoutTicks = 400)
    public static void creeperFuseInfectsWithoutBlockDamage(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 3));
        BlockPos protectedBlock = pos.east(8);
        helper.getLevel().setBlockAndUpdate(protectedBlock, Blocks.STONE.defaultBlockState());
        Cow victim = EntityType.COW.create(helper.getLevel());
        helper.assertTrue(victim != null, "failed to create infection target");
        victim.moveTo(pos.getX() + 1.0, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(victim);
        TaintedCreeper creeper = ModEntities.TAINTED_CREEPER.get().create(helper.getLevel());
        helper.assertTrue(creeper != null, "failed to create taint creeper");
        creeper.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
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
    @GameTest(template = "test/platform", timeoutTicks = 400)
    public static void entitySavedDataRoundTrips(GameTestHelper helper) {
        var registryAccess = helper.getLevel().registryAccess();
        TaintSpore spore = ModEntities.TAINT_SPORE.get().create(helper.getLevel());
        helper.assertTrue(spore != null, "failed to create spore for save test");
        spore.setSporeSize(7);
        net.minecraft.nbt.CompoundTag sporeOutput = new net.minecraft.nbt.CompoundTag();
        sporeOutput = spore.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        TaintSpore loadedSpore = ModEntities.TAINT_SPORE.get().create(helper.getLevel());
        helper.assertTrue(loadedSpore != null, "failed to create spore for load test");
        loadedSpore.load(sporeOutput);
        helper.assertValueEqual(loadedSpore.getSporeSize(), 7, "spore size did not survive entity save/load");

        TaintSwarm swarm = ModEntities.TAINT_SWARM.get().create(helper.getLevel());
        helper.assertTrue(swarm != null, "failed to create swarm for save test");
        swarm.setSummoned(true);
        swarm.setDamageBonus(4);
        net.minecraft.nbt.CompoundTag swarmOutput = new net.minecraft.nbt.CompoundTag();
        swarmOutput = swarm.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        TaintSwarm loadedSwarm = ModEntities.TAINT_SWARM.get().create(helper.getLevel());
        helper.assertTrue(loadedSwarm != null, "failed to create swarm for load test");
        loadedSwarm.load(swarmOutput);
        helper.assertTrue(loadedSwarm.isSummoned(), "swarm summoned flag did not survive entity save/load");
        helper.assertValueEqual(loadedSwarm.getAttributeValue(Attributes.ATTACK_DAMAGE), 6.0, "swarm damage bonus did not survive entity save/load");
        helper.succeed();
    }
    @GameTest(template = "test/platform", timeoutTicks = 400)
    public static void taintedVillagerEntersAndRestrictsInsideHouse(GameTestHelper helper) {
        var level = helper.getLevel();
        level.setDayTime(18000L);
        level.updateSkyBrightness();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        for (int z = 7; z <= 11; z++) {
            for (int y = origin.getY() + 3; y < level.getMaxBuildHeight(); y++) {
                level.setBlockAndUpdate(new BlockPos(origin.getX() + 4, y, origin.getZ() + z), Blocks.AIR.defaultBlockState());
            }
        }

        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                level.setBlockAndUpdate(origin.offset(x, 2, z), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(x, 5, z), Blocks.STONE.defaultBlockState());
                for (int y = 2; y <= 3; y++) {
                    boolean wall = x == 2 || x == 6 || z == 2 || z == 6;
                    boolean doorway = x == 4 && z == 6;
                    if (wall && !doorway) {
                        level.setBlockAndUpdate(origin.offset(x, y + 1, z), Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }
        BlockPos door = origin.offset(4, 3, 6);
        var lowerDoor = Blocks.OAK_DOOR.defaultBlockState()
            .setValue(DoorBlock.FACING, Direction.NORTH)
            .setValue(DoorBlock.OPEN, true)
            .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        var upperDoor = lowerDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        level.setBlockAndUpdate(door, lowerDoor);
        level.setBlockAndUpdate(door.above(), upperDoor);
        level.setBlockAndUpdate(origin.offset(3, 3, 3), Blocks.BELL.defaultBlockState());

        helper.runAfterDelay(2, () -> {
            level.getPoiManager().ensureLoadedAndValid(level, origin.offset(4, 3, 6), 14);
            TaintedVillager villager = ModEntities.TAINTED_VILLAGER.get().create(level);
            helper.assertTrue(villager != null, "failed to create tainted villager");
            villager.moveTo(origin.getX() + 4.5, origin.getY() + 3.0, origin.getZ() + 7.5, 0.0F, 0.0F);
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

        villager.moveTo(doorInfo.insidePos().getX() + 0.5, doorInfo.insidePos().getY(), doorInfo.insidePos().getZ() + 0.5, 0.0F, 0.0F);
        TaintedVillagerRestrictDoorGoal restrictGoal = new TaintedVillagerRestrictDoorGoal(villager);
        helper.assertTrue(restrictGoal.canUse(), "door restriction did not start from the inside position");
        restrictGoal.start();
        helper.assertTrue(restrictGoal.canContinueToUse(), "door restriction did not hold while villager remained inside");
        villager.moveTo(door.getX() + 0.5, door.getY(), door.getZ() + 1.5, 0.0F, 0.0F);
        helper.assertTrue(!restrictGoal.canContinueToUse(), "door restriction continued after villager crossed outside");
        restrictGoal.stop();
        helper.succeed();
    }
    @GameTest(template = "test/platform", batch = "tainted_trees", timeoutTicks = 100)
    public static void treesRejectWaterOverDirt(GameTestHelper helper) {
        treesRejectWater(helper, Blocks.DIRT);
    }

    @GameTest(template = "test/platform", batch = "tainted_trees", timeoutTicks = 100)
    public static void treesRejectWaterOverStone(GameTestHelper helper) {
        treesRejectWater(helper, Blocks.STONE);
    }

    private static void treesRejectWater(GameTestHelper helper, net.minecraft.world.level.block.Block ground) {
        TaintTreeTestWorld world = new TaintTreeTestWorld(helper, ground, true);
        helper.assertFalse(placeTaintedTrees(helper, world), "tainted tree placement accepted water");
        world.assertWaterUnchanged(helper);
        helper.succeed();
    }

    @GameTest(template = "test/platform", batch = "tainted_trees", timeoutTicks = 100)
    public static void treesGrowOnDrySoil(GameTestHelper helper) {
        TaintTreeTestWorld world = new TaintTreeTestWorld(helper, Blocks.GRASS_BLOCK, false);
        helper.assertTrue(placeTaintedTrees(helper, world), "tainted tree filters rejected dry soil");
        helper.assertTrue(world.logs() > 0, "tainted trees failed to grow on dry soil");
        helper.succeed();
    }

    private static boolean placeTaintedTrees(GameTestHelper helper, TaintTreeTestWorld world) {
        var key = net.minecraft.resources.ResourceKey.create(Registries.PLACED_FEATURE, Thaumcraft.id("tainted_trees"));
        var feature = helper.getLevel().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(key).value();
        return feature.placeWithBiomeCheck(world.level, helper.getLevel().getChunkSource().getGenerator(), RandomSource.create(37L), BlockPos.ZERO);
    }
}
