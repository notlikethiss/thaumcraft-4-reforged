package thaumcraft.test;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.registry.ModBlocks;
import thaumcraft.registry.ModEntities;
import thaumcraft.registry.EldritchContent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class EldritchFieldGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "eldritch_sapping_field_effect", EldritchFieldGameTests::sapping,
        "eldritch_sapping_field_immunity", EldritchFieldGameTests::immunity,
        "eldritch_impassable_field_collision", EldritchFieldGameTests::collision,
        "eldritch_field_expires", EldritchFieldGameTests::expiration,
        "eldritch_block_experience", EldritchFieldGameTests::experience,
        "eldritch_obelisk_break", EldritchFieldGameTests::breakObelisk
    );

    static {
        TESTS.forEach((name, test) -> FUNCTIONS.register(name, () -> test));
    }

    private EldritchFieldGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(Thaumcraft.id("eldritch_fields"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            event.registerTest(Thaumcraft.id(name), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name)), new TestData<>(environment, Thaumcraft.id("test/platform"), 100, 0, true)));
        }
    }

    private static void touch(GameTestHelper helper, BlockState state, LivingEntity entity) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        state.entityInside(helper.getLevel(), pos, entity, InsideBlockEffectApplier.NOOP, false);
    }

    private static void sapping(GameTestHelper helper) {
        LivingEntity cow = EntityType.COW.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(cow != null, "cow creation");
        cow.setDeltaMovement(1, 0.2, 1);
        touch(helper, ModBlocks.SAPPING_FIELD.get().defaultBlockState(), cow);
        helper.assertTrue(cow.hasEffect(MobEffects.WEAKNESS), "sapping weakness");
        helper.assertValueEqual(cow.getEffect(MobEffects.WEAKNESS).getAmplifier(), 1, "weakness level");
        helper.assertTrue(Math.abs(cow.getDeltaMovement().x - 0.66) < 0.0001, "horizontal slowing");
        helper.assertTrue(Math.abs(cow.getDeltaMovement().y - 0.2) < 0.0001, "vertical velocity preserved");
        helper.succeed();
    }

    private static void immunity(GameTestHelper helper) {
        LivingEntity guardian = ModEntities.ELDRITCH_GUARDIAN.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(guardian != null, "guardian creation");
        guardian.setDeltaMovement(1, 0.2, 1);
        touch(helper, ModBlocks.SAPPING_FIELD.get().defaultBlockState(), guardian);
        helper.assertFalse(guardian.hasEffect(MobEffects.WEAKNESS), "eldritch immunity");
        helper.assertTrue(guardian.getDeltaMovement().equals(new Vec3(1, 0.2, 1)), "eldritch motion preserved");
        helper.succeed();
    }

    private static void collision(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.assertFalse(ModBlocks.IMPASSABLE_FIELD.get().defaultBlockState().getCollisionShape(helper.getLevel(), pos).isEmpty(), "barrier collision");
        helper.assertTrue(ModBlocks.SAPPING_FIELD.get().defaultBlockState().getCollisionShape(helper.getLevel(), pos).isEmpty(), "sapping is passable");
        helper.assertTrue(ModBlocks.IMPASSABLE_FIELD.get().defaultBlockState().getShape(helper.getLevel(), pos, CollisionContext.empty()).isEmpty(), "barrier is unselectable");
        helper.succeed();
    }

    private static void expiration(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.SAPPING_FIELD.get().defaultBlockState());
        helper.getLevel().scheduleTick(pos, ModBlocks.SAPPING_FIELD.get(), 2);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(helper.getLevel().isEmptyBlock(pos), "scheduled field expiration");
            helper.succeed();
        });
    }

    private static void experience(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        for (var block : new thaumcraft.eldritch.EldritchBlock[]{EldritchContent.GLYPHED_STONE.get(), EldritchContent.RUNED_STONE.get(), EldritchContent.CRUSTED_OPENING.get()}) {
            int xp = block.getExpDrop(block.defaultBlockState(), helper.getLevel(), pos, null, null, ItemStack.EMPTY);
            int minimum = block.kind() == 9 ? 6 : 1;
            int maximum = block.kind() == 9 ? 10 : 4;
            helper.assertTrue(xp >= minimum && xp <= maximum, "original experience range for " + block.kind());
        }
        helper.succeed();
    }

    private static void breakObelisk(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlockAndUpdate(pos, EldritchContent.ELDRITCH_ALTAR.get().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos.above(2), EldritchContent.ELDRITCH_OBELISK.get().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos.above(3), EldritchContent.ELDRITCH_OBELISK.get().defaultBlockState().setValue(thaumcraft.eldritch.EldritchBlock.PART, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        helper.getLevel().setBlockAndUpdate(pos.east(2), EldritchContent.GLOWING_CRUSTED_STONE.get().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().removeBlock(pos, false);
        helper.assertTrue(helper.getLevel().isEmptyBlock(pos.above(2)), "obelisk base removed with altar");
        helper.assertTrue(helper.getLevel().isEmptyBlock(pos.above(3)), "obelisk segment removed transitively");
        helper.assertTrue(helper.getLevel().getBlockState(pos.east(2)).is(EldritchContent.GLOWING_CRUSTED_STONE.get()), "decorations survive structure break");
        helper.assertTrue(helper.getLevel().getBlockState(pos.below()).is(Blocks.STONE), "structure explosion does not destroy terrain");
        helper.succeed();
    }
}
