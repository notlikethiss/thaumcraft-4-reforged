package thaumcraft.entity.projectile;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.registry.ModEffects;
import thaumcraft.registry.ModEntities;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ProjectileGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Thaumcraft.MODID);
    private static final String[] TESTS = {
        "alumentum_projectile_explodes_and_respects_mob_griefing",
        "bottle_taint_poisons_only_non_undead",
        "dart_projectile_deals_one_arrow_damage",
        "eldritch_orb_damages_and_weakens_non_undead",
        "golem_orb_damage_matches_red_variant",
    };

    static {
        FUNCTIONS.register(TESTS[0], () -> ProjectileGameTests::alumentumExplodes);
        FUNCTIONS.register(TESTS[1], () -> ProjectileGameTests::bottleTaintPoisons);
        FUNCTIONS.register(TESTS[2], () -> ProjectileGameTests::dartDealsOneDamage);
        FUNCTIONS.register(TESTS[3], () -> ProjectileGameTests::eldritchOrbWeakens);
        FUNCTIONS.register(TESTS[4], () -> ProjectileGameTests::golemOrbDamageScaling);
    }

    private ProjectileGameTests() {
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Thaumcraft.id("projectile_default"), new TestEnvironmentDefinition.AllOf());
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, Thaumcraft.id(name));
            event.registerTest(Thaumcraft.id(name), new FunctionGameTestInstance(
                function,
                new TestData<>(environment, Thaumcraft.id("test/platform"), 100, 0, true)
            ));
        }
    }

    private static void alumentumExplodes(GameTestHelper helper) {
        BlockPos targetPos = helper.absolutePos(new BlockPos(3, 2, 3));
        LivingEntity target = (LivingEntity) EntityType.COW.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(target != null, "cow factory returned null");
        target.snapTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().setBlock(targetPos.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().addFreshEntity(target);
        float health = target.getHealth();
        helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, false, helper.getLevel().getServer());
        AlumentumProjectile projectile = new AlumentumProjectile(ModEntities.ALUMENTUM_PROJECTILE.get(), helper.getLevel());
        projectile.snapTo(target.getX(), target.getY() + 0.2, target.getZ(), 0.0F, 0.0F);
        projectile.onHit(new BlockHitResult(target.position(), Direction.UP, target.blockPosition(), false));
        helper.assertTrue(target.getHealth() < health, "alumentum impact did not damage a nearby entity");
        helper.assertTrue(helper.getLevel().getBlockState(targetPos.below()).is(Blocks.STONE), "alumentum damaged blocks while mob griefing was disabled");
        helper.assertTrue(projectile.isRemoved(), "alumentum projectile survived impact");
        helper.succeed();
    }

    private static void bottleTaintPoisons(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        LivingEntity cow = (LivingEntity) EntityType.COW.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        LivingEntity zombie = (LivingEntity) EntityType.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(cow != null && zombie != null, "test mob factory returned null");
        cow.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        zombie.snapTo(pos.getX() + 1.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(cow);
        helper.getLevel().addFreshEntity(zombie);
        BottleTaint bottle = new BottleTaint(ModEntities.BOTTLE_TAINT.get(), helper.getLevel());
        bottle.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        bottle.onHit(new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        helper.assertTrue(cow.hasEffect(ModEffects.TAINT_POISON), "bottle taint did not poison a living non-undead target");
        helper.assertFalse(zombie.hasEffect(ModEffects.TAINT_POISON), "bottle taint poisoned an undead target");
        helper.assertTrue(bottle.isRemoved(), "bottle taint survived impact");
        helper.succeed();
    }

    private static void dartDealsOneDamage(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        LivingEntity target = (LivingEntity) EntityType.COW.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(target != null, "cow factory returned null");
        target.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(target);
        float health = target.getHealth();
        ExposedDart dart = new ExposedDart(ModEntities.DART.get(), helper.getLevel());
        dart.setDeltaMovement(1.0, 0.0, 0.0);
        dart.hit(new EntityHitResult(target));
        helper.assertValueEqual(target.getHealth(), health - 1.0F, "dart did not deal one point at unit velocity");
        helper.succeed();
    }

    private static void eldritchOrbWeakens(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        Monster owner = (Monster) EntityType.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        LivingEntity target = (LivingEntity) EntityType.COW.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(owner != null && target != null, "test mob factory returned null");
        owner.snapTo(pos.getX() - 1.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        target.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(owner);
        helper.getLevel().addFreshEntity(target);
        float health = target.getHealth();
        EldritchOrb orb = new EldritchOrb(ModEntities.ELDRITCH_ORB.get(), helper.getLevel());
        orb.setOwner(owner);
        orb.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        orb.onHit(new EntityHitResult(target));
        helper.assertTrue(target.getHealth() < health, "eldritch orb did not damage the non-undead target");
        helper.assertTrue(target.hasEffect(MobEffects.WEAKNESS), "eldritch orb did not apply weakness");
        helper.assertValueEqual(target.getEffect(MobEffects.WEAKNESS).getDuration(), 160, "eldritch orb weakness duration");
        helper.succeed();
    }

    private static void golemOrbDamageScaling(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        Monster owner = (Monster) EntityType.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        LivingEntity normalTarget = (LivingEntity) EntityType.COW.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        LivingEntity redTarget = (LivingEntity) EntityType.COW.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(owner != null && normalTarget != null && redTarget != null, "cow factory returned null");
        owner.snapTo(pos.getX() - 2.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        normalTarget.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        redTarget.snapTo(pos.getX() + 1.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(owner);
        helper.getLevel().addFreshEntity(normalTarget);
        helper.getLevel().addFreshEntity(redTarget);
        float normalHealth = normalTarget.getHealth();
        float redHealth = redTarget.getHealth();
        GolemOrb normal = new GolemOrb(ModEntities.GOLEM_ORB.get(), helper.getLevel());
        normal.setOwner(owner);
        normal.setRed(false);
        normal.snapTo(normalTarget.position(), 0.0F, 0.0F);
        normal.onHit(new EntityHitResult(normalTarget));
        GolemOrb red = new GolemOrb(ModEntities.GOLEM_ORB.get(), helper.getLevel());
        red.setOwner(owner);
        red.setRed(true);
        red.snapTo(redTarget.position(), 0.0F, 0.0F);
        red.onHit(new EntityHitResult(redTarget));
        helper.assertTrue(normalTarget.getHealth() < normalHealth, "normal golem orb did not damage its hit target");
        helper.assertTrue(redTarget.getHealth() < redHealth, "red golem orb did not damage its hit target");
        helper.assertTrue(redHealth - redTarget.getHealth() > normalHealth - normalTarget.getHealth(), "red golem orb did not deal more damage");
        helper.succeed();
    }

    private static final class ExposedDart extends DartProjectile {
        private ExposedDart(net.minecraft.world.entity.EntityType<? extends DartProjectile> type, net.minecraft.world.level.Level level) {
            super(type, level);
        }

        private void hit(EntityHitResult result) {
            super.onHitEntity(result);
        }
    }
}
