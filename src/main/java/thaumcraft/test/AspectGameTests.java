package thaumcraft.test;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.aspect.AddonAspects;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.AspectRegistry;
import thaumcraft.aspect.EntityAspects;
import thaumcraft.entity.AspectOrb;

@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public final class AspectGameTests {
    private static final ResourceLocation IMAGE = ResourceLocation.fromNamespaceAndPath("testaddon", "textures/aspects/testaspect.png");

    private AspectGameTests() {
    }

    public static void register(IEventBus bus) {
    }

    @GameTest(template = "test/platform", timeoutTicks = 200)
    public static void addonAspectRegistration(GameTestHelper helper) {
        int initialCount = Aspect.values().length;
        List<Aspect> registered = new ArrayList<>();
        try {
            checkRegistration(helper, registered);
        } finally {
            unregister(registered);
        }
        helper.assertValueEqual(Aspect.values().length, initialCount, "test aspects leaked into the registry");
        helper.assertTrue(Aspect.getAspect("testaspect") == null, "test aspect remained registered");
        helper.succeed();
    }

    private static void checkRegistration(GameTestHelper helper, List<Aspect> registered) {
        AtomicReference<Aspect> fromApi = new AtomicReference<>();
        AddonAspects.addRegistrar(api -> fromApi.set(api.register("zzz_testaspect", 0x345678, new Aspect[]{Aspect.AIR, Aspect.WATER}, 771, IMAGE)));
        Aspect late = fromApi.get();
        helper.assertTrue(late != null && Aspect.getAspect("zzz_testaspect") == late, "registrar added after setup was not applied");
        registered.add(late);
        helper.assertValueEqual(late.getBlend(), 771, "add-on blend");

        Aspect addon = new Aspect("testaspect", 0x123456, new Aspect[]{Aspect.AIR, Aspect.FIRE}, IMAGE, 1);
        registered.add(addon);
        AspectOrb orb = AspectOrb.create(helper.getLevel(), 0, 0, 0, addon, 1);
        Aspect earlier = new Aspect("aaa_testaspect", 0x234567, new Aspect[]{Aspect.AIR, Aspect.WATER}, IMAGE, 1);
        registered.add(earlier);

        helper.assertTrue(orb != null && orb.getAspect() == addon, "aspect orb lost its aspect after reordering");
        helper.assertValueEqual(Aspect.AIR.ordinal(), 0, "built-in ordinal");
        helper.assertValueEqual(Aspect.TRAP.ordinal(), 47, "built-in ordinal");
        helper.assertTrue(earlier.ordinal() > 47 && earlier.ordinal() < addon.ordinal() && addon.ordinal() < late.ordinal(), "add-on aspects are not ordered by tag");
        for (Aspect aspect : registered) {
            helper.assertTrue(Aspect.values()[aspect.ordinal()] == aspect, "values() disagrees with ordinal()");
        }
        helper.assertTrue(Aspect.getCompoundAspects().contains(addon), "add-on aspect must be compound");
        helper.assertTrue(Aspect.getAspect("missing") == null, "unknown tag");

        AspectList aspects = new AspectList().add(addon, 7).add(Aspect.AIR, 2);
        var json = AspectList.CODEC.encodeStart(JsonOps.INSTANCE, aspects).result().orElseThrow();
        helper.assertTrue(AspectList.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow().equals(aspects), "codec round trip");
        helper.assertTrue(Aspect.CODEC.parse(JsonOps.INSTANCE, JsonOps.INSTANCE.createString("missing")).error().isPresent(), "unknown tag must decode to an error");
        ByteBuf buffer = Unpooled.buffer();
        try {
            AspectList.STREAM_CODEC.encode(buffer, aspects);
            helper.assertTrue(AspectList.STREAM_CODEC.decode(buffer).equals(aspects), "stream codec round trip");
        } finally {
            buffer.release();
        }

        AspectRegistry registry = new AspectRegistry();
        registry.registerEntity(EntityType.CREEPER, new AspectList().add(Aspect.FIRE, 2), "powered", 1, "variant", 1);
        registry.mergeEntity(EntityType.CREEPER, new AspectList().add(addon, 3).add(Aspect.FIRE, 1), "variant", 1, "powered", 1);
        List<EntityAspects> merged = registry.exportEntities().get(EntityType.CREEPER);
        helper.assertValueEqual(merged.size(), 1, "matching conditions must merge");
        helper.assertValueEqual(merged.getFirst().aspects().getAmount(addon), 3, "merged add-on amount");
        helper.assertValueEqual(merged.getFirst().aspects().getAmount(Aspect.FIRE), 3, "merged amounts must sum");
        registry.mergeEntity(EntityType.CREEPER, new AspectList().add(addon, 4), "variant", 0, "powered", 1);
        helper.assertValueEqual(registry.exportEntities().get(EntityType.CREEPER).size(), 2, "different conditions must not merge");
        rejects(helper, () -> EntityAspects.of(new AspectList(), "powered", 1, "powered", 0), "duplicate condition");
        rejects(helper, () -> EntityAspects.of(new AspectList(), "powered"), "incomplete condition");

        rejects(helper, () -> new Aspect("testaspect", 0, new Aspect[]{Aspect.AIR, Aspect.FIRE}, IMAGE, 1), "duplicate tag");
        helper.assertTrue(Aspect.getAspect("testaspect") == addon, "duplicate replaced the aspect");
        rejects(helper, () -> new Aspect("testaddon:qualified", 0, new Aspect[]{Aspect.AIR, Aspect.FIRE}, IMAGE, 1), "qualified tag");
        rejects(helper, () -> new Aspect("single_component", 0, new Aspect[]{Aspect.AIR}, IMAGE, 1), "one component");
        rejects(helper, () -> new Aspect("no_image", 0, new Aspect[]{Aspect.AIR, Aspect.FIRE}, null, 1), "missing image");
        helper.assertTrue(Aspect.getAspect("single_component") == null && Aspect.getAspect("no_image") == null, "rejected aspect was registered");
    }

    private static void rejects(GameTestHelper helper, Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        helper.fail(message + " was accepted");
    }

    @SuppressWarnings("unchecked")
    private static void unregister(List<Aspect> aspects) {
        try {
            Field byTag = Aspect.class.getDeclaredField("BY_TAG");
            Field values = Aspect.class.getDeclaredField("VALUES");
            Method reorder = Aspect.class.getDeclaredMethod("reorder");
            byTag.setAccessible(true);
            values.setAccessible(true);
            reorder.setAccessible(true);
            synchronized (Aspect.class) {
                for (Aspect aspect : aspects) {
                    ((Map<String, Aspect>) byTag.get(null)).remove(aspect.getTag(), aspect);
                    ((List<Aspect>) values.get(null)).remove(aspect);
                }
                reorder.invoke(null);
            }
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Failed to remove test aspects", failure);
        }
    }
}
