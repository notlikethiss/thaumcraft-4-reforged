package thaumcraft.test;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;
import thaumcraft.block.LiquidDeathBlock;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModFluids;
import thaumcraft.registry.ModEffects;
import thaumcraft.damage.ModDamageTypes;
import thaumcraft.research.Warp;
import thaumcraft.research.WarpType;

@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public final class FluidGameTests {
    private static BlockState fluidState(int kind, boolean partial) {
        if (kind == 0) {
            return Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, partial ? 4 : 0);
        }
        if (kind == 1) {
            return ModBlocks.PURIFYING_FLUID.get().defaultBlockState().setValue(LiquidBlock.LEVEL, partial ? 4 : 0);
        }
        return ModBlocks.LIQUID_DEATH.get().defaultBlockState().setValue(LiquidBlock.LEVEL, partial ? 2 : 0);
    }

    private static void motionPool(GameTestHelper helper, int kind, boolean partial) {
        for (int x = 3; x <= 11; x++) {
            for (int z = 3; z <= 11; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 3, z), fluidState(kind, partial));
            }
        }
    }

    private static LivingEntity swimmer(GameTestHelper helper, boolean player) {
        LivingEntity entity;
        if (player) {
            entity = TestSupport.mockServerPlayer(helper);
            entity.addEffect(new MobEffectInstance(ModEffects.WARP_WARD, 10000));
        } else {
            Pig pig = new Pig(EntityType.PIG, helper.getLevel());
            entity = pig;
        }
        entity.setInvulnerable(true);
        setTestPosition(entity, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(7, 3, 7))).add(0, 0.05, 0));
        return entity;
    }

    @GameTest(template = "test/eldritch_regression", timeoutTicks = 400)
    public static void fluidPhysicsMovement(GameTestHelper helper) {
        for (boolean player : new boolean[]{false, true}) {
            for (boolean partial : new boolean[]{false, true}) {
                double reference = 0;
                for (int kind = 0; kind < 3; kind++) {
                    motionPool(helper, kind, partial);
                    LivingEntity entity = swimmer(helper, player);
                    Vec3 resting = entity.position();
                    for (int tick = 0; tick < 5; tick++) {
                        entity.baseTick();
                        entity.travel(Vec3.ZERO);
                    }
                    helper.assertTrue(entity.position().subtract(resting).horizontalDistance() < 0.001, "still fluid pushed swimmer, kind=" + kind);
                    double startZ = entity.getZ();
                    for (int tick = 0; tick < 12; tick++) {
                        entity.baseTick();
                        entity.travel(new Vec3(0, 0, 1));
                    }
                    double moved = entity.getZ() - startZ;
                    String label = "kind=" + kind + ", player=" + player + ", partial=" + partial;
                    helper.assertTrue(moved > 0.2, "swimmer stuck: " + label + ", moved=" + moved);
                    if (kind == 0) {
                        reference = moved;
                    } else {
                        helper.assertTrue(Math.abs(moved - reference) < 0.05, "movement differs from water: " + label + ", moved=" + moved + ", water=" + reference + ", velocity=" + entity.getDeltaMovement());
                    }
                    entity.setDeltaMovement(Vec3.ZERO);
                    entity.jumpInFluid(fluidState(kind, partial).getFluidState().getFluidType());
                    double startY = entity.getY();
                    entity.travel(Vec3.ZERO);
                    helper.assertTrue(entity.getY() > startY, "swimmer cannot rise: " + label);
                    for (int tick = 0; tick < 100; tick++) {
                        entity.baseTick();
                        entity.travel(new Vec3(0, 0, 1));
                    }
                    helper.assertTrue(entity.getZ() > helper.absolutePos(new BlockPos(7, 3, 12)).getZ(), "swimmer cannot leave pool: " + label);
                    entity.discard();
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test/eldritch_regression", timeoutTicks = 400)
    public static void fluidPhysicsCurrents(GameTestHelper helper) {
        for (int kind = 0; kind < 3; kind++) {
            for (boolean partial : new boolean[]{false, true}) {
                motionPool(helper, kind, partial);
                Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(7, 3, 7))).add(0, 0.05, 0);
                ItemEntity item = new ItemEntity(helper.getLevel(), start.x, start.y, start.z, new ItemStack(Items.STONE));
                item.setDeltaMovement(Vec3.ZERO);
                for (int tick = 0; tick < 12; tick++) {
                    item.tick();
                }
                helper.assertTrue(item.position().subtract(start).horizontalDistance() < 0.001, "still fluid pushed item, kind=" + kind);
                helper.assertTrue(item.getDeltaMovement().length() < 0.5, "still fluid launched item");
                item.discard();
            }
            for (boolean falling : new boolean[]{false, true}) {
                motionPool(helper, kind, false);
                for (int z = 3; z <= 11; z++) {
                    helper.setBlock(new BlockPos(8, 3, z), fluidState(kind, true));
                    if (falling) {
                        helper.setBlock(new BlockPos(8, 2, z), fluidState(kind, true));
                        helper.setBlock(new BlockPos(8, 3, z), Blocks.AIR);
                    }
                }
                for (int type = 0; type < 3; type++) {
                    Entity entity = type < 2 ? swimmer(helper, type == 1)
                        : new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(Items.STONE));
                    setTestPosition(entity, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(7, 3, 7))).add(0.45, 0.05, 0));
                    entity.setDeltaMovement(Vec3.ZERO);
                    double startX = entity.getX();
                    double startZ = entity.getZ();
                    for (int tick = 0; tick < 12; tick++) {
                        if (entity instanceof LivingEntity living) {
                            living.baseTick();
                            living.travel(Vec3.ZERO);
                        } else {
                            entity.tick();
                        }
                        helper.assertTrue(entity.getDeltaMovement().length() < 0.5, "fluid current accumulated excessive speed, kind=" + kind + ", entity=" + type);
                    }
                    helper.assertTrue(entity.getX() > startX, "fluid current points away from descent, kind=" + kind + ", entity=" + type + ", dx=" + (entity.getX() - startX) + ", velocity=" + entity.getDeltaMovement());
                    helper.assertTrue(Math.abs(entity.getZ() - startZ) < 0.01, "fluid current pushed sideways, kind=" + kind + ", entity=" + type + ", falling=" + falling + ", dz=" + (entity.getZ() - startZ));
                    entity.discard();
                }
            }
        }
        helper.succeed();
    }

    private static void drainBasin(GameTestHelper helper, BlockPos center) {
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                helper.setBlock(center.offset(x, -2, z), Blocks.STONE);
                helper.setBlock(center.offset(x, -1, z), Blocks.STONE);
                helper.setBlock(center.offset(x, 0, z), Math.abs(x) == 6 || Math.abs(z) == 6 ? Blocks.STONE : Blocks.AIR);
                helper.setBlock(center.offset(x, 1, z), Blocks.AIR);
            }
        }
    }

    @GameTest(template = "test/eldritch_regression", timeoutTicks = 400)
    public static void purifyingFluidWaterPaths(GameTestHelper helper) {
        purifyingFluidWaterPaths(helper, 0);
    }

    private static void purifyingFluidWaterPaths(GameTestHelper helper, int scenario) {
        BlockPos water = new BlockPos(8, 3, 8);
        BlockPos purifying = new BlockPos(23, 3, 8);
        for (BlockPos center : new BlockPos[]{water, purifying}) {
            drainBasin(helper, center);
            int distance = scenario == 0 ? 1 : 3;
            helper.setBlock(center.offset(distance, -1, 0), Blocks.AIR);
            if (scenario == 2) {
                helper.setBlock(center.offset(1, 0, 0), Blocks.STONE);
            }
            if (scenario == 3) {
                helper.setBlock(center.offset(-distance, -1, 0), Blocks.AIR);
            }
        }
        helper.setBlock(water, Blocks.WATER);
        helper.setBlock(purifying, ModBlocks.PURIFYING_FLUID.get());
        helper.runAfterDelay(45, () -> {
            for (int x = -6; x <= 6; x++) {
                for (int z = -6; z <= 6; z++) {
                    for (int y = -1; y <= 0; y++) {
                        var expected = helper.getBlockState(water.offset(x, y, z)).getFluidState();
                        var actual = helper.getBlockState(purifying.offset(x, y, z)).getFluidState();
                        helper.assertTrue(expected.isEmpty() == actual.isEmpty() && expected.getAmount() == actual.getAmount()
                            && expected.isSource() == actual.isSource(), "purifying flow differs from water, scenario=" + scenario + ", offset=" + x + "," + y + "," + z);
                    }
                }
            }
            if (scenario < 3) {
                purifyingFluidWaterPaths(helper, scenario + 1);
            } else {
                helper.succeed();
            }
        });
    }

    @GameTest(template = "test/eldritch_regression", timeoutTicks = 400)
    public static void liquidDeathDrainPaths(GameTestHelper helper) {
        liquidDeathDrainPaths(helper, 0, 4);
    }

    @GameTest(template = "test/eldritch_regression", timeoutTicks = 400)
    public static void liquidDeathSingleQuantumDrain(GameTestHelper helper) {
        liquidDeathDrainPaths(helper, 1, 1);
    }

    private static void liquidDeathDrainPaths(GameTestHelper helper, int scenario, int amount) {
        BlockPos center = new BlockPos(8, 3, 8);
        drainBasin(helper, center);
        int distance = scenario == 0 ? 1 : scenario == 1 ? 4 : 2;
        BlockPos hole = center.offset(distance, -1, 0);
        helper.setBlock(hole, Blocks.AIR);
        if (scenario == 2) {
            helper.setBlock(center.offset(1, 0, 0), Blocks.STONE);
        }
        if (scenario == 3) {
            helper.setBlock(center.offset(-distance, -1, 0), Blocks.AIR);
        }
        helper.setBlock(center, ModBlocks.LIQUID_DEATH.get().defaultBlockState().setValue(LiquidBlock.LEVEL, amount == 4 ? 0 : 4 - amount));
        helper.runAfterDelay(70, () -> {
            helper.assertValueEqual(totalQuanta(helper, center, -6, 6, -1, 0, -6, 6), amount, "drain routing duplicated or lost liquid death");
            int drained = quanta(helper.getBlockState(hole));
            if (scenario == 3) {
                int other = quanta(helper.getBlockState(center.offset(-distance, -1, 0)));
                helper.assertTrue(Math.abs(drained - other) <= 1, "equidistant drains received unequal volumes");
                drained += other;
            }
            helper.assertValueEqual(drained, amount, "liquid death did not reach drain, scenario=" + scenario);
            helper.runAfterDelay(14, () -> {
                helper.assertValueEqual(totalQuanta(helper, center, -6, 6, -1, 0, -6, 6), amount, "settled liquid death changed volume");
                if (amount == 4 && scenario < 3) {
                    liquidDeathDrainPaths(helper, scenario + 1, amount);
                } else {
                    helper.succeed();
                }
            });
        });
    }

    @GameTest(template = "test/eldritch_regression", timeoutTicks = 400)
    public static void liquidDeathBlockedDrain(GameTestHelper helper) {
        BlockPos center = new BlockPos(8, 3, 8);
        drainBasin(helper, center);
        helper.setBlock(center.offset(2, -1, 0), Blocks.AIR);
        for (int z = -5; z <= 5; z++) {
            helper.setBlock(center.offset(1, 0, z), Blocks.STONE);
        }
        helper.setBlock(center.offset(-1, -1, 0), Blocks.DIRT);
        helper.setBlock(center.offset(-1, 0, 0), Blocks.SHORT_GRASS);
        helper.setBlock(center, ModBlocks.LIQUID_DEATH.get().defaultBlockState().setValue(LiquidBlock.LEVEL, 3));
        helper.runAfterDelay(35, () -> {
            helper.assertTrue(helper.getBlockState(center.offset(-1, 0, 0)).is(Blocks.SHORT_GRASS), "route search destroyed a block without transferring fluid");
            helper.assertValueEqual(quanta(helper.getBlockState(center.offset(2, -1, 0))), 0, "liquid death crossed a solid wall");
            helper.assertValueEqual(totalQuanta(helper, center, -6, 6, -1, 0, -6, 6), 1, "blocked liquid death changed volume");
            helper.succeed();
        });
    }

    @GameTest(template = "test/eldritch_regression", timeoutTicks = 400)
    public static void liquidDeathDamageMotion(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(8, 3, 8));
        ServerPlayer player = TestSupport.mockServerPlayer(helper);
        for (int tick = 0; tick < 61; tick++) {
            player.tick();
        }
        setTestPosition(player, Vec3.atBottomCenterOf(pos));
        player.setDeltaMovement(Vec3.ZERO);
        helper.assertTrue(player.hurt(helper.getLevel().damageSources().source(ModDamageTypes.DISSOLVE), 4), "liquid death damage did not apply");
        helper.assertValueEqual(player.getHealth(), 16.0F, "liquid death damage changed");
        helper.assertTrue(player.getDeltaMovement().length() < 0.5, "dissolve damage launched player");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "test/eldritch_regression", timeoutTicks = 400)
    public static void liquidDeathDrainCapacity(GameTestHelper helper) {
        BlockPos center = new BlockPos(8, 3, 8);
        drainBasin(helper, center);
        BlockState partial = ModBlocks.LIQUID_DEATH.get().defaultBlockState().setValue(LiquidBlock.LEVEL, 1);
        helper.setBlock(center.offset(1, -1, 0), partial);
        helper.setBlock(center.offset(1, 0, 0), partial);
        helper.setBlock(center, ModBlocks.LIQUID_DEATH.get());
        helper.runAfterDelay(70, () -> {
            helper.assertValueEqual(totalQuanta(helper, center, -6, 6, -1, 0, -6, 6), 10, "partially filled drain duplicated or lost liquid death");
            helper.assertValueEqual(quanta(helper.getBlockState(center.offset(1, -1, 0))), 4, "drain capacity was not filled");
            helper.runAfterDelay(35, () -> {
                helper.assertValueEqual(totalQuanta(helper, center, -6, 6, -1, 0, -6, 6), 10, "repeated partial drain ticks changed volume");
                helper.succeed();
            });
        });
    }

    private static void setTestPosition(Entity entity, Vec3 pos) {
        entity.moveTo(pos.x, pos.y, pos.z, 0, 0);
    }

    @GameTest(template = "test/platform", timeoutTicks = 240)
    public static void liquidDeathHorizontalQuanta(GameTestHelper helper) {
        BlockPos source = new BlockPos(2, 3, 2);
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
    @GameTest(template = "test/platform", timeoutTicks = 240)
    public static void liquidDeathVerticalQuanta(GameTestHelper helper) {
        BlockPos source = new BlockPos(2, 4, 2);
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
    @GameTest(template = "test/platform", timeoutTicks = 240)
    public static void purifyingFluidNoSourceRegeneration(GameTestHelper helper) {
        BlockPos left = new BlockPos(1, 4, 2);
        BlockPos center = new BlockPos(2, 4, 2);
        BlockPos right = new BlockPos(3, 4, 2);
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
    @GameTest(template = "test/platform", timeoutTicks = 240)
    public static void sanitizingSoapReleaseThreshold(GameTestHelper helper) {
        ServerPlayer player = TestSupport.mockServerPlayer(helper);
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
    @GameTest(template = "test/platform", timeoutTicks = 240)
    public static void purifyingFluidWardConsumesSource(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos, ModBlocks.PURIFYING_FLUID.get().defaultBlockState());
        ServerPlayer player = TestSupport.mockServerPlayer(helper);
        player.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0F, 0.0F);
        Warp.set(player, 9, WarpType.PERMANENT);
        level.getBlockState(pos).entityInside(level, pos, player);
        helper.assertTrue(player.hasEffect(ModEffects.WARP_WARD), "purifying source did not apply ward");
        helper.assertTrue(level.getBlockState(pos).isAir(), "purifying source was not consumed by ward application");
        helper.succeed();
    }
    @GameTest(template = "test/platform", timeoutTicks = 240)
    public static void bathSaltsExpireInWater(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        ItemEntity salts = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModItems.BATH_SALTS.get()));
        level.addFreshEntity(salts);
        salts.makeFakeItem();
        salts.tick();
        helper.assertTrue(level.getBlockState(pos).is(ModBlocks.PURIFYING_FLUID.get()), "expired bath salts did not transform source water");
        helper.succeed();
    }
    @GameTest(template = "test/platform", timeoutTicks = 240)
    public static void purifyingBucketRoundTrip(GameTestHelper helper) {
        bucketRoundTrip(helper, ModItems.PURIFYING_FLUID_BUCKET.get(), ModBlocks.PURIFYING_FLUID.get());
    }
    @GameTest(template = "test/platform", timeoutTicks = 240)
    public static void liquidDeathBucketRoundTrip(GameTestHelper helper) {
        bucketRoundTrip(helper, ModItems.LIQUID_DEATH_BUCKET.get(), ModBlocks.LIQUID_DEATH.get());
    }

    private static void bucketRoundTrip(GameTestHelper helper, BucketItem fluidBucket, net.minecraft.world.level.block.Block fluidBlock) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        ServerPlayer player = TestSupport.mockServerPlayer(helper);
        player.moveTo(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 0.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(fluidBucket));
        var placed = fluidBucket.use(level, player, InteractionHand.MAIN_HAND).getResult();
        helper.assertTrue(placed.consumesAction(), "fluid bucket did not place its fluid");
        helper.assertTrue(level.getBlockState(pos).is(fluidBlock), "fluid bucket placed an unexpected block");
        helper.assertTrue(level.getFluidState(pos).isSource(), "placed fluid bucket did not create a source");
        player.moveTo(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 0.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        var pickup = Items.BUCKET.use(level, player, InteractionHand.MAIN_HAND);
        var pickedUp = pickup.getResult();
        helper.assertTrue(pickedUp.consumesAction(), "empty bucket did not pick up the source");
        helper.assertTrue(level.getBlockState(pos).isAir(), "bucket pickup did not remove the source block");
        helper.assertTrue(
            pickup.getObject().is(fluidBucket),
            "bucket pickup returned the wrong fluid bucket"
        );
        helper.succeed();
    }
    @GameTest(template = "test/platform", timeoutTicks = 240)
    public static void liquidDeathYieldsEssence(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos relativePos = new BlockPos(2, 3, 2);
        BlockPos pos = helper.absolutePos(relativePos);
        Pig pig = new Pig(EntityType.PIG, level);
        pig.getRandom().setSeed(0L);
        pig.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(pig);
        pig.hurt(level.damageSources().source(ModDamageTypes.DISSOLVE), 100.0F);
        helper.assertItemEntityPresent(ModItems.CRYSTALLIZED_ESSENCE.get(), relativePos, 6.0);
        helper.assertItemEntityPresent(Items.PORKCHOP, relativePos, 6.0);
        helper.assertTrue(pig.isDeadOrDying(), "dissolving liquid death damage did not kill the mob");
        helper.succeed();
    }
    @GameTest(template = "test/platform", timeoutTicks = 240)
    public static void sanitizingSoapFullUseOnce(GameTestHelper helper) {
        ServerPlayer player = TestSupport.mockServerPlayer(helper);
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
    @GameTest(template = "test/platform", timeoutTicks = 240)
    public static void sanitizingSoapFullLifecycleOnce(GameTestHelper helper) {
        ServerPlayer player = TestSupport.mockServerPlayer(helper);
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

}
