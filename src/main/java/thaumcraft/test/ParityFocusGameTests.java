package thaumcraft.test;

import java.util.List;
import java.util.function.Consumer;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.arcane.FocalManipulatorBlockEntity;
import thaumcraft.block.LootBlock;
import thaumcraft.entity.projectile.PrimalOrb;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModEntities;
import thaumcraft.registry.ModItems;
import thaumcraft.platform.BiomeEdit;
import thaumcraft.taint.TaintBiomes;
import thaumcraft.wand.WandRegistry;
import thaumcraft.wand.Wands;
import thaumcraft.wand.focus.BlockSwapper;
import thaumcraft.wand.focus.ExcavationFocus;
import thaumcraft.wand.focus.FocusKeys;
import thaumcraft.wand.focus.FocusItem;
import thaumcraft.wand.focus.FocusUpgradeType;
import thaumcraft.wand.focus.FocusUtils;
import thaumcraft.wand.focus.TradeFocus;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ParityFocusGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final String[] TESTS = {
        "focus_trade_preserves_rare_loot_tier",
        "focus_excavation_preserves_shared_lazy_cost",
        "focus_manipulator_cancel_and_replacement",
        "primal_orb_taint_splosion_changes_biome_and_places_fibres",
    };

    static {
        FUNCTIONS.register(TESTS[0], () -> ParityFocusGameTests::tradePreservesRareLootTier);
        FUNCTIONS.register(TESTS[1], () -> ParityFocusGameTests::excavationPreservesSharedLazyCost);
        FUNCTIONS.register(TESTS[2], () -> ParityFocusGameTests::manipulatorCancelAndReplacement);
        FUNCTIONS.register(TESTS[3], () -> ParityFocusGameTests::primalOrbTaintSplosion);
    }

    private ParityFocusGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Thaumcraft.id("parity_focus_default"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name));
            event.registerTest(
                Thaumcraft.id(name),
                new FunctionGameTestInstance(function, new TestData<>(environment, Thaumcraft.id("test/platform"), 100, 0, true))
            );
        }
    }

    private static void tradePreservesRareLootTier(GameTestHelper helper) {
        ServerLevel level = (ServerLevel)helper.getLevel();
        BlockPos lootOffset = new BlockPos(2, 2, 2);
        BlockPos targetOffset = new BlockPos(3, 2, 2);
        BlockState rareUrn = ModBlocks.LOOT_URN.get().defaultBlockState().setValue(LootBlock.TIER, LootBlock.Tier.RARE);
        helper.setBlock(lootOffset, rareUrn);
        helper.setBlock(targetOffset, Blocks.STONE);
        ItemStack wand = Wands.create(WandRegistry.CAP_IRON, WandRegistry.ROD_WOOD, false);
        Wands.setFocus(wand, new ItemStack(ModItems.FOCUS_EQUAL_TRADE.get()));
        ServerPlayer player = mockServerPlayer(helper, GameType.CREATIVE);
        BlockPos loot = helper.absolutePos(lootOffset);
        player.setPos(loot.getX() + 0.5, loot.getY() - 1.0, loot.getZ() - 1.0);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.setShiftKeyDown(true);
        TradeFocus tradeFocus = (TradeFocus)ModItems.FOCUS_EQUAL_TRADE.get();
        BlockHitResult pickHit = FocusUtils.getTargetBlock(level, player, false, player.blockInteractionRange());
        helper.assertTrue(pickHit != null, "creative ray did not hit a block");
        helper.assertValueEqual(pickHit.getBlockPos(), loot, "creative ray hit the wrong block");
        helper.assertValueEqual(tradeFocus.onFocusRightClick(wand, level, player, null), InteractionResult.SUCCESS, "creative pick did not complete");
        player.setShiftKeyDown(false);
        ItemStack pickedBeforeSave = TradeFocus.getPickedBlock(wand);
        helper.assertTrue(!pickedBeforeSave.isEmpty(), "creative pick did not store a block stack");
        LootBlock.Tier pickedTier = pickedBeforeSave.getOrDefault(DataComponents.BLOCK_STATE, net.minecraft.world.item.component.BlockItemStateProperties.EMPTY).get(LootBlock.TIER);
        helper.assertTrue(pickedTier != null, "creative pick did not store the loot tier component");
        helper.assertValueEqual(pickedTier, LootBlock.Tier.RARE, "creative pick did not capture rare tier");
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        JsonElement saved = ItemStack.CODEC.encodeStart(ops, wand).getOrThrow();
        ItemStack loaded = ItemStack.CODEC.parse(ops, saved).getOrThrow();
        ItemStack picked = TradeFocus.getPickedBlock(loaded);
        helper.assertTrue(!picked.isEmpty(), "picked stack disappeared after save/load");
        LootBlock.Tier loadedTier = picked.getOrDefault(DataComponents.BLOCK_STATE, net.minecraft.world.item.component.BlockItemStateProperties.EMPTY).get(LootBlock.TIER);
        helper.assertTrue(loadedTier != null, "picked tier component disappeared after save/load");
        helper.assertValueEqual(loadedTier, LootBlock.Tier.RARE, "picked rare tier did not survive stack save/load");
        Wands.fill(loaded);
        player.getInventory().setItem(0, loaded);
        player.getInventory().setSelectedSlot(0);
        BlockPos target = helper.absolutePos(targetOffset);
        BlockSwapper.add(level, target, level.getBlockState(target), picked, 0, player, 0);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(helper.getBlockState(targetOffset).is(ModBlocks.LOOT_URN.get()), "creative swap did not place the urn");
            helper.assertValueEqual(helper.getBlockState(targetOffset).getValue(LootBlock.TIER), LootBlock.Tier.RARE, "creative swap lost the rare tier");
            helper.succeed();
        });
    }

    private static void excavationPreservesSharedLazyCost(GameTestHelper helper) {
        ExcavationFocus focus = ModItems.FOCUS_EXCAVATION.get();
        ItemStack silk = new ItemStack(focus);
        FocusKeys.UPGRADES.set(silk, List.of(FocusUpgradeType.SILKTOUCH.id()));
        ItemStack dowsing = new ItemStack(focus);
        FocusKeys.UPGRADES.set(dowsing, List.of(ExcavationFocus.DOWSING.id()));
        AspectList first = focus.getVisCost(silk);
        AspectList second = focus.getVisCost(dowsing);
        AspectList reverseFirst = focus.getVisCost(dowsing);
        AspectList reverseSecond = focus.getVisCost(silk);
        helper.assertTrue(first == second && first == reverseFirst && first == reverseSecond, "silk touch and dowsing did not share the lazy cost object");
        boolean silkCost = first.getAmount(Aspect.AIR) == 1
            && first.getAmount(Aspect.FIRE) == 1
            && first.getAmount(Aspect.EARTH) == 16
            && first.getAmount(Aspect.WATER) == 1
            && first.getAmount(Aspect.ORDER) == 1
            && first.getAmount(Aspect.ENTROPY) == 1;
        boolean dowsingCost = first.getAmount(Aspect.AIR) == 0
            && first.getAmount(Aspect.FIRE) == 2
            && first.getAmount(Aspect.EARTH) == 15
            && first.getAmount(Aspect.WATER) == 0
            && first.getAmount(Aspect.ORDER) == 2
            && first.getAmount(Aspect.ENTROPY) == 0;
        helper.assertTrue(silkCost || dowsingCost, "first excavation cost was neither original silk nor dowsing cost");
        AspectList base = focus.getVisCost(new ItemStack(focus));
        helper.assertValueEqual(base.getAmount(Aspect.EARTH), 15, "base excavation cost changed");
        helper.assertValueEqual(base.size(), 1, "base excavation cost gained upgraded aspects");
        helper.succeed();
    }

    private static ServerPlayer mockServerPlayer(GameTestHelper helper, GameType gameType) {
        ServerPlayer player = TestPlayers.mockServerPlayer(helper, gameType);
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
        return player;
    }

    private static void manipulatorCancelAndReplacement(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBlocks.FOCAL_MANIPULATOR.get());
        FocalManipulatorBlockEntity table = helper.getBlockEntity(pos, FocalManipulatorBlockEntity.class);
        ItemStack original = new ItemStack(ModItems.FOCUS_EXCAVATION.get());
        table.setItem(0, original);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 100;
        helper.assertTrue(table.startCraft(FocusUpgradeType.FRUGAL.id(), player), "manipulator did not start the test upgrade");
        AspectList pending = table.getAspects().copy();
        ItemStack removed = table.removeItem(0, 1);
        helper.assertTrue(!removed.isEmpty(), "manipulator did not return the removed focus");
        helper.assertTrue(table.getAspects().visSize() > 0, "inventory extraction discarded the pending final drain");
        for (Aspect aspect : pending.getAspects()) {
            helper.assertValueEqual(table.getAspects().getAmount(aspect), pending.getAmount(aspect), "inventory extraction changed pending vis");
        }
        ItemStack replacement = new ItemStack(ModItems.FOCUS_EXCAVATION.get());
        table.setItem(0, replacement);
        for (int tick = 0; tick < 5; tick++) {
            FocalManipulatorBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), table);
        }
        FocusItem focus = (FocusItem)replacement.getItem();
        helper.assertTrue(!focus.isUpgradedWith(replacement, FocusUpgradeType.FRUGAL), "replacement focus received the cancelled upgrade");
        helper.assertValueEqual(table.getSize(), 0, "cancelled replacement craft did not finish");
        helper.succeed();
    }

    private static void primalOrbTaintSplosion(GameTestHelper helper) {
        ServerLevel level = (ServerLevel)helper.getLevel();
        BlockPos orbPos = helper.absolutePos(new BlockPos(8, 3, 8));
        int centerX = (int)(orbPos.getX() + 0.5);
        int centerZ = (int)(orbPos.getZ() + 0.5);
        int[][] heights = new int[6][6];
        var plains = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
        for (int x = 0; x < 6; x++) {
            for (int z = 0; z < 6; z++) {
                int worldX = centerX - 5 + x;
                int worldZ = centerZ - 5 + z;
                BlockPos column = new BlockPos(worldX, 0, worldZ);
                helper.assertTrue(level.hasChunkAt(column), "test taint column is not loaded");
                int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, worldX, worldZ);
                level.setBlock(new BlockPos(worldX, surfaceY - 1, worldZ), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(new BlockPos(worldX, surfaceY, worldZ), Blocks.AIR.defaultBlockState(), 3);
                BiomeEdit.setColumn(level, new BlockPos(worldX, surfaceY, worldZ), plains);
                heights[x][z] = surfaceY;
            }
        }
        PrimalOrb orb = ModEntities.PRIMAL_ORB.get().create(level, EntitySpawnReason.TRIGGERED);
        helper.assertTrue(orb != null, "failed to create primal orb");
        orb.setPos(orbPos.getX() + 0.5, orbPos.getY(), orbPos.getZ() + 0.5);
        long seed = 0L;
        orb.getRandom().setSeed(seed);
        orb.taintSplosion();
        int taintedColumns = 0;
        int fibreBlocks = 0;
        for (int x = 0; x < 6; x++) {
            for (int z = 0; z < 6; z++) {
                BlockPos surface = new BlockPos(centerX - 5 + x, heights[x][z], centerZ - 5 + z);
                if (TaintBiomes.isTainted(level, surface)) {
                    taintedColumns++;
                }
                if (level.getBlockState(surface).is(ModBlocks.FIBROUS_TAINT.get())) {
                    fibreBlocks++;
                }
            }
        }
        helper.assertTrue(taintedColumns > 0, "primal orb did not taint any biome columns");
        helper.assertTrue(fibreBlocks > 0, "primal orb did not place fibres above the solid surface");
        BlockState[][] states = new BlockState[6][6];
        boolean[][] tainted = new boolean[6][6];
        for (int x = 0; x < 6; x++) {
            for (int z = 0; z < 6; z++) {
                BlockPos surface = new BlockPos(centerX - 5 + x, heights[x][z], centerZ - 5 + z);
                states[x][z] = level.getBlockState(surface);
                tainted[x][z] = TaintBiomes.isTainted(level, surface);
            }
        }
        orb.getRandom().setSeed(seed);
        orb.taintSplosion();
        for (int x = 0; x < 6; x++) {
            for (int z = 0; z < 6; z++) {
                BlockPos surface = new BlockPos(centerX - 5 + x, heights[x][z], centerZ - 5 + z);
                helper.assertValueEqual(level.getBlockState(surface), states[x][z], "repeat taint burst changed a previously processed column");
                helper.assertValueEqual(TaintBiomes.isTainted(level, surface), tainted[x][z], "repeat taint burst changed an already tainted biome column");
            }
        }
        helper.succeed();
    }
}
