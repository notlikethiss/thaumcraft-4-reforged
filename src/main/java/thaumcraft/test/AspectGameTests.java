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
        Aspect addon = new Aspect(
            "testaspect",
            0x123456,
            new Aspect[]{Aspect.AIR, Aspect.FIRE},
            Thaumcraft.id("textures/aspects/testaspect.png"),
            1
        );

        helper.assertTrue(Aspect.getAspect("testaspect") == addon, "registered aspect was not found by tag");
        helper.assertValueEqual(Aspect.AIR.ordinal(), 0, "built-in aspect ordinal changed");
        helper.assertValueEqual(Aspect.TRAP.ordinal(), 47, "built-in aspect ordinal changed");
        helper.assertValueEqual(addon.ordinal(), 48, "add-on aspect did not follow built-in aspects");
        helper.assertTrue(addon.getComponents() != null && addon.getComponents().length == 2, "add-on aspect is not compound");
        helper.assertTrue(addon.getImage().equals(Thaumcraft.id("textures/aspects/testaspect.png")), "add-on aspect image changed");

        AspectList aspects = new AspectList().add(addon, 7).add(Aspect.AIR, 2);
        var encoded = AspectList.CODEC.encodeStart(JsonOps.INSTANCE, aspects).result().orElseThrow();
        AspectList decoded = AspectList.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        helper.assertTrue(decoded.equals(aspects), "aspect list codec lost the add-on aspect");
        ByteBuf buffer = Unpooled.buffer();
        AspectList.STREAM_CODEC.encode(buffer, aspects);
        helper.assertTrue(AspectList.STREAM_CODEC.decode(buffer).equals(aspects), "aspect list stream codec lost the add-on aspect");

        AspectRegistry registry = new AspectRegistry();
        registry.registerEntity(EntityTypes.CREEPER, new AspectList().add(Aspect.FIRE, 2), "powered", 1);
        registry.mergeEntity(EntityTypes.CREEPER, new AspectList().add(addon, 3).add(Aspect.FIRE, 1), "powered", 1);
        List<EntityAspects> merged = registry.exportEntities().get(EntityTypes.CREEPER);
        helper.assertValueEqual(merged.size(), 1, "matching entity tags were not merged");
        helper.assertValueEqual(merged.getFirst().aspects().getAmount(addon), 3, "merged entity tag lost the add-on aspect");
        helper.assertValueEqual(merged.getFirst().aspects().getAmount(Aspect.FIRE), 3, "merged entity tag did not sum aspect amounts");

        registry.mergeEntity(EntityTypes.CREEPER, new AspectList().add(addon, 4), "powered", 0);
        helper.assertValueEqual(registry.exportEntities().get(EntityTypes.CREEPER).size(), 2, "different entity conditions were merged");

        try {
            new Aspect(
                "testaspect",
                0x654321,
                new Aspect[]{Aspect.AIR, Aspect.FIRE},
                Thaumcraft.id("textures/aspects/testaspect.png"),
                1
            );
            helper.fail("duplicate aspect tag was accepted");
        } catch (IllegalArgumentException expected) {
        }

        try {
            new Aspect(
                "invalidaspect",
                0x654321,
                new Aspect[]{Aspect.AIR},
                Thaumcraft.id("textures/aspects/invalidaspect.png"),
                1
            );
            helper.fail("add-on aspect with invalid components was accepted");
        } catch (IllegalArgumentException expected) {
        }

        helper.succeed();
    }
}
