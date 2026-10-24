package thaumcraft.test;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.AspectRegistry;
import thaumcraft.aspect.EntityAspects;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class AspectGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "addon_aspect_registration", AspectGameTests::addonAspectRegistration
    );

    static {
        TESTS.forEach((name, test) -> FUNCTIONS.register(name, () -> test));
    }

    private AspectGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Thaumcraft.id("aspect_game_tests"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS.keySet()) {
            event.registerTest(
                Thaumcraft.id(name),
                new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name)), new TestData<>(environment, Thaumcraft.id("test/platform"), 200, 0, true))
            );
        }
    }

    private static void addonAspectRegistration(GameTestHelper helper) {
        Aspect[] components = {Aspect.AIR, Aspect.FIRE};
        Identifier addonImage = Identifier.fromNamespaceAndPath("testaddon", "textures/aspects/testaspect.png");
        Aspect addon = new Aspect(
            "testaspect",
            0x123456,
            components,
            addonImage,
            1
        );

        helper.assertTrue(Aspect.getAspect("testaspect") == addon, "registered aspect was not found by tag");
        helper.assertValueEqual(Aspect.AIR.ordinal(), 0, "built-in aspect ordinal changed");
        helper.assertValueEqual(Aspect.TRAP.ordinal(), 47, "built-in aspect ordinal changed");
        helper.assertValueEqual(addon.ordinal(), 48, "add-on aspect did not follow built-in aspects");
        helper.assertValueEqual(Aspect.values().length, 49, "add-on aspect was not added to the registry");
        helper.assertTrue(Aspect.values()[48] == addon, "add-on aspect did not follow built-in aspects in the registry");
        helper.assertTrue(Aspect.getCompoundAspects().contains(addon), "add-on aspect was not classified as compound");
        helper.assertTrue(!Aspect.getPrimalAspects().contains(addon), "add-on aspect was classified as primal");
        helper.assertTrue(Aspect.getAspect("missing") == null, "unknown aspect tag did not return null");
        helper.assertTrue(addon.getComponents() != null && addon.getComponents().length == 2, "add-on aspect is not compound");
        helper.assertTrue(addon.getComponents()[0] == Aspect.AIR && addon.getComponents()[1] == Aspect.FIRE, "add-on aspect components changed");
        components[0] = Aspect.ENTROPY;
        helper.assertTrue(addon.getComponents()[0] == Aspect.AIR, "add-on aspect retained a mutable component array");
        addon.getComponents()[0] = Aspect.ENTROPY;
        helper.assertTrue(addon.getComponents()[0] == Aspect.AIR, "add-on aspect exposed a mutable component array");
        helper.assertTrue(addon.getImage().equals(addonImage), "add-on aspect image identifier or namespace changed");

        AspectList aspects = new AspectList().add(addon, 7).add(Aspect.AIR, 2);
        var encoded = AspectList.CODEC.encodeStart(JsonOps.INSTANCE, aspects).result().orElseThrow();
        AspectList decoded = AspectList.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        helper.assertTrue(decoded.equals(aspects), "aspect list codec lost the add-on aspect");
        ByteBuf buffer = Unpooled.buffer();
        AspectList.STREAM_CODEC.encode(buffer, aspects);
        helper.assertTrue(AspectList.STREAM_CODEC.decode(buffer).equals(aspects), "aspect list stream codec lost the add-on aspect");

        AspectRegistry registry = new AspectRegistry();
        registry.registerEntity(EntityTypes.CREEPER, new AspectList().add(Aspect.FIRE, 2),
            "powered", 1, "variant", 1);
        registry.mergeEntity(EntityTypes.CREEPER, new AspectList().add(addon, 3).add(Aspect.FIRE, 1),
            "variant", 1, "powered", 1);
        List<EntityAspects> merged = registry.exportEntities().get(EntityTypes.CREEPER);
        helper.assertValueEqual(merged.size(), 1, "matching entity tags with reordered conditions were not merged");
        helper.assertValueEqual(merged.getFirst().aspects().getAmount(addon), 3, "merged entity tag lost the add-on aspect");
        helper.assertValueEqual(merged.getFirst().aspects().getAmount(Aspect.FIRE), 3, "merged entity tag did not sum aspect amounts");
        helper.assertValueEqual(merged.getFirst().conditions().getFirst().name(), "powered",
            "entity conditions were not stored in canonical name order");
        EntityAspects directlyConstructed = new EntityAspects(new AspectList(), List.of(
            new EntityAspects.Condition("variant", 1),
            new EntityAspects.Condition("powered", 1)
        ));
        helper.assertValueEqual(directlyConstructed.conditions().getFirst().name(), "powered",
            "directly constructed entity aspects did not canonicalize conditions");

        registry.mergeEntity(EntityTypes.CREEPER, new AspectList().add(addon, 4), "variant", 0, "powered", 1);
        helper.assertValueEqual(registry.exportEntities().get(EntityTypes.CREEPER).size(), 2,
            "different entity conditions were merged");

        try {
            EntityAspects.of(new AspectList(), "powered", 1, "powered", 0);
            helper.fail("duplicate entity condition names were accepted");
        } catch (IllegalArgumentException expected) {
        }
        try {
            EntityAspects.of(new AspectList(), "powered");
            helper.fail("an incomplete entity condition pair was accepted");
        } catch (IllegalArgumentException expected) {
        }
        try {
            EntityAspects.of(new AspectList(), "powered", null);
            helper.fail("a null entity condition value was accepted");
        } catch (IllegalArgumentException expected) {
        }

        ByteBuf malformedCondition = Unpooled.buffer();
        net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.encode(malformedCondition, "powered");
        malformedCondition.writeByte(99);
        try {
            EntityAspects.Condition.STREAM_CODEC.decode(malformedCondition);
            helper.fail("an unknown entity condition value type was accepted");
        } catch (IllegalArgumentException expected) {
        } finally {
            malformedCondition.release();
        }

        try {
            new Aspect(
                "testaspect",
                0x654321,
                new Aspect[]{Aspect.AIR, Aspect.FIRE},
                addonImage,
                1
            );
            helper.fail("duplicate aspect tag was accepted");
        } catch (IllegalArgumentException expected) {
        }
        helper.assertTrue(Aspect.getAspect("testaspect") == addon, "duplicate aspect tag replaced the registered aspect");

        assertRejectedAspect(helper, "nullcomponents", null, Thaumcraft.id("textures/aspects/nullcomponents.png"), "add-on aspect with null components was accepted");
        assertRejectedAspect(helper, "invalidaspect", new Aspect[]{Aspect.AIR}, Thaumcraft.id("textures/aspects/invalidaspect.png"), "add-on aspect with invalid components was accepted");
        assertRejectedAspect(helper, "nullcomponent", new Aspect[]{Aspect.AIR, null}, Thaumcraft.id("textures/aspects/nullcomponent.png"), "add-on aspect with a null component was accepted");
        assertRejectedAspect(helper, "nullimage", new Aspect[]{Aspect.AIR, Aspect.FIRE}, null, "add-on aspect with a null image was accepted");

        helper.succeed();
    }

    private static void assertRejectedAspect(GameTestHelper helper, String tag, Aspect[] components, Identifier image, String message) {
        try {
            new Aspect(tag, 0x654321, components, image, 1);
            helper.fail(message);
        } catch (IllegalArgumentException expected) {
        }
        helper.assertTrue(Aspect.getAspect(tag) == null, "rejected aspect was registered");
    }
}
