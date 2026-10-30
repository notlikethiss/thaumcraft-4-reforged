package thaumcraft.registry;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.entity.AspectOrb;
import thaumcraft.entity.monster.FireBat;
import thaumcraft.entity.monster.BrainyZombie;
import thaumcraft.entity.monster.CultistCleric;
import thaumcraft.entity.monster.CultistKnight;
import thaumcraft.entity.monster.GiantBrainyZombie;
import thaumcraft.entity.monster.InhabitedZombie;
import thaumcraft.entity.monster.Pech;
import thaumcraft.entity.monster.ThaumicSlime;
import thaumcraft.entity.monster.Wisp;
import thaumcraft.entity.monster.eldritch.EldritchCrab;
import thaumcraft.entity.monster.eldritch.EldritchGuardian;
import thaumcraft.entity.monster.eldritch.MindSpider;
import thaumcraft.entity.projectile.Ember;
import thaumcraft.entity.projectile.ExplosiveOrb;
import thaumcraft.entity.projectile.EldritchOrb;
import thaumcraft.entity.projectile.GolemOrb;
import thaumcraft.entity.projectile.FrostShard;
import thaumcraft.entity.projectile.PechBlast;
import thaumcraft.entity.projectile.PrimalArrow;
import thaumcraft.entity.projectile.PrimalOrb;
import thaumcraft.entity.projectile.ShockOrb;
import thaumcraft.entity.projectile.BottleTaint;
import thaumcraft.entity.projectile.AlumentumProjectile;
import thaumcraft.entity.projectile.DartProjectile;
import thaumcraft.entity.monster.taint.FallingTaint;
import thaumcraft.entity.monster.taint.TaintSpider;
import thaumcraft.entity.monster.taint.Taintacle;
import thaumcraft.entity.monster.taint.TaintacleTiny;
import thaumcraft.entity.monster.taint.TaintSpore;
import thaumcraft.entity.monster.taint.TaintSwarmer;
import thaumcraft.entity.monster.taint.TaintSwarm;
import thaumcraft.entity.monster.taint.TaintedChicken;
import thaumcraft.entity.monster.taint.TaintedCow;
import thaumcraft.entity.monster.taint.TaintedCreeper;
import thaumcraft.entity.monster.taint.TaintedPig;
import thaumcraft.entity.monster.taint.TaintedSheep;
import thaumcraft.entity.monster.taint.TaintedVillager;

public final class ModEntities {
    private static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Thaumcraft.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<FireBat>> FIREBAT = ENTITIES.registerEntityType(
        "firebat",
        FireBat::new,
        MobCategory.MONSTER,
        builder -> builder.sized(0.5F, 0.9F).eyeHeight(0.45F).fireImmune().clientTrackingRange(4).updateInterval(3).notInPeaceful()
    );
    public static final DeferredHolder<EntityType<?>, EntityType<PrimalOrb>> PRIMAL_ORB = projectile("primal_orb", PrimalOrb::new, 0.25F);
    public static final DeferredHolder<EntityType<?>, EntityType<FrostShard>> FROST_SHARD = projectile("frost_shard", FrostShard::new, 0.25F);
    public static final DeferredHolder<EntityType<?>, EntityType<PrimalArrow>> PRIMAL_ARROW = projectile("primal_arrow", PrimalArrow::new, 0.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<PechBlast>> PECH_BLAST = projectile("pech_blast", PechBlast::new, 0.25F);
    public static final DeferredHolder<EntityType<?>, EntityType<ShockOrb>> SHOCK_ORB = projectile("shock_orb", ShockOrb::new, 0.25F);
    public static final DeferredHolder<EntityType<?>, EntityType<Ember>> EMBER = projectile("ember", Ember::new, 0.25F);
    public static final DeferredHolder<EntityType<?>, EntityType<ExplosiveOrb>> EXPLOSIVE_ORB = projectile("explosive_orb", ExplosiveOrb::new, 0.25F);
    public static final DeferredHolder<EntityType<?>, EntityType<EldritchOrb>> ELDRITCH_ORB = projectile("eldritch_orb", EldritchOrb::new, 0.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<GolemOrb>> GOLEM_ORB = projectile("golem_orb", GolemOrb::new, 0.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<AspectOrb>> ASPECT_ORB = ENTITIES.registerEntityType(
        "aspect_orb",
        AspectOrb::new,
        MobCategory.MISC,
        builder -> builder.sized(0.125F, 0.125F).clientTrackingRange(4).updateInterval(2)
    );
    public static final DeferredHolder<EntityType<?>, EntityType<BottleTaint>> BOTTLE_TAINT = projectile("bottle_taint", BottleTaint::new, 0.25F);
    public static final DeferredHolder<EntityType<?>, EntityType<AlumentumProjectile>> ALUMENTUM_PROJECTILE = trackedProjectile("alumentum_projectile", AlumentumProjectile::new, 0.25F, 64);
    public static final DeferredHolder<EntityType<?>, EntityType<DartProjectile>> DART = trackedProjectile("dart", DartProjectile::new, 0.5F, 64);
    public static final DeferredHolder<EntityType<?>, EntityType<FallingTaint>> FALLING_TAINT = projectile("falling_taint", FallingTaint::new, 0.98F);
    public static final DeferredHolder<EntityType<?>, EntityType<TaintSpider>> TAINT_SPIDER = mob("taint_spider", TaintSpider::new, 0.4F, 0.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<Taintacle>> TAINTACLE = mob("taintacle", Taintacle::new, 0.66F, 3.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<TaintacleTiny>> TAINTACLE_TINY = mob("taintacle_small", TaintacleTiny::new, 0.22F, 1.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<TaintSpore>> TAINT_SPORE = mob("taint_spore", TaintSpore::new, 0.4F, 0.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<TaintSwarmer>> TAINT_SPORE_SWARMER = mob("taint_spore_swarmer", TaintSwarmer::new, 1.0F, 1.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<TaintSwarm>> TAINT_SWARM = mob("taint_swarm", TaintSwarm::new, 2.0F, 2.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<TaintedChicken>> TAINTED_CHICKEN = mob("tainted_chicken", TaintedChicken::new, 0.5F, 0.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<TaintedCow>> TAINTED_COW = mob("tainted_cow", TaintedCow::new, 0.9F, 1.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<TaintedCreeper>> TAINTED_CREEPER = mob("tainted_creeper", TaintedCreeper::new, 0.6F, 1.7F);
    public static final DeferredHolder<EntityType<?>, EntityType<TaintedPig>> TAINTED_PIG = mob("tainted_pig", TaintedPig::new, 0.9F, 0.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<TaintedSheep>> TAINTED_SHEEP = mob("tainted_sheep", TaintedSheep::new, 0.9F, 1.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<TaintedVillager>> TAINTED_VILLAGER = mob("tainted_villager", TaintedVillager::new, 0.6F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<BrainyZombie>> BRAINY_ZOMBIE = mob("brainy_zombie", BrainyZombie::new, 0.6F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<GiantBrainyZombie>> GIANT_BRAINY_ZOMBIE = mob("giant_brainy_zombie", GiantBrainyZombie::new, 0.6F, 1.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<Wisp>> WISP = mob("wisp", Wisp::new, 0.9F, 0.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<Pech>> PECH = mob("pech", Pech::new, 0.6F, 1.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<ThaumicSlime>> THAUMIC_SLIME = mob("thaumic_slime", ThaumicSlime::new, 0.5F, 0.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<InhabitedZombie>> INHABITED_ZOMBIE = mob("inhabited_zombie", InhabitedZombie::new, 0.6F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<CultistKnight>> CULTIST_KNIGHT = mob("cultist_knight", CultistKnight::new, 0.6F, 1.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<CultistCleric>> CULTIST_CLERIC = mob("cultist_cleric", CultistCleric::new, 0.6F, 1.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<EldritchCrab>> ELDRITCH_CRAB = mob("eldritch_crab", EldritchCrab::new, 0.8F, 0.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<MindSpider>> MIND_SPIDER = mob("mind_spider", MindSpider::new, 0.3F, 0.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<EldritchGuardian>> ELDRITCH_GUARDIAN = ENTITIES.registerEntityType(
        "eldritch_guardian",
        EldritchGuardian::new,
        MobCategory.MONSTER,
        builder -> builder.sized(0.8F, 2.25F).eyeHeight(2.1F).clientTrackingRange(8).updateInterval(3).notInPeaceful()
    );

    public static final DeferredHolder<EntityType<?>, EntityType<thaumcraft.entity.golem.GolemBase>> GOLEM = ENTITIES.registerEntityType("golem", thaumcraft.entity.golem.GolemBase::new, MobCategory.MISC, builder -> builder.sized(0.4F, 0.95F).clientTrackingRange(4).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<thaumcraft.entity.golem.TravelingTrunk>> TRAVELING_TRUNK = ENTITIES.registerEntityType("traveling_trunk", thaumcraft.entity.golem.TravelingTrunk::new, MobCategory.MISC, builder -> builder.sized(0.8F, 0.8F).fireImmune().clientTrackingRange(4).updateInterval(3));
    public static final DeferredHolder<EntityType<?>, EntityType<thaumcraft.entity.golem.GolemBobber>> GOLEM_BOBBER = ENTITIES.registerEntityType("golem_bobber", thaumcraft.entity.golem.GolemBobber::new, MobCategory.MISC, builder -> builder.sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(64));

    private ModEntities() {
    }

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> projectile(String name, EntityType.EntityFactory<T> factory, float size) {
        return trackedProjectile(name, factory, size, 4);
    }

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> trackedProjectile(String name, EntityType.EntityFactory<T> factory, float size, int range) {
        return ENTITIES.registerEntityType(name, factory, MobCategory.MISC, builder -> builder.sized(size, size).clientTrackingRange(range).updateInterval(20));
    }

    private static <T extends net.minecraft.world.entity.Mob> DeferredHolder<EntityType<?>, EntityType<T>> mob(String name, EntityType.EntityFactory<T> factory, float width, float height) {
        return ENTITIES.registerEntityType(name, factory, MobCategory.MONSTER, builder -> builder.sized(width, height).clientTrackingRange(8).updateInterval(3).notInPeaceful());
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntities::createAttributes);
        bus.addListener(ModEntities::registerSpawnPlacements);
        thaumcraft.research.WarpEncounterHooks.install(new thaumcraft.research.WarpEncounterHooks.Spawner() {
            @Override
            public boolean spawnGuardianAt(net.minecraft.server.level.ServerLevel level, net.minecraft.server.level.ServerPlayer player, net.minecraft.core.BlockPos pos) {
                return EldritchGuardian.spawnForWarp(level, player, pos);
            }

            @Override
            public boolean spawnMindSpiderAt(net.minecraft.server.level.ServerLevel level, net.minecraft.server.level.ServerPlayer player, net.minecraft.core.BlockPos pos, boolean harmless) {
                return MindSpider.spawnForWarp(level, player, pos, harmless);
            }
        });
    }

    private static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(TRAVELING_TRUNK.get(), thaumcraft.entity.golem.TravelingTrunk.createAttributes().build());
        event.put(GOLEM.get(), thaumcraft.entity.golem.GolemBase.createAttributes().build());
        event.put(FIREBAT.get(), FireBat.createAttributes().build());
        event.put(TAINT_SPIDER.get(), TaintSpider.createAttributes().build());
        event.put(TAINTACLE.get(), Taintacle.createAttributes().build());
        event.put(TAINTACLE_TINY.get(), TaintacleTiny.createAttributes().build());
        event.put(TAINT_SPORE.get(), TaintSpore.createAttributes().build());
        event.put(TAINT_SPORE_SWARMER.get(), TaintSwarmer.createAttributes().build());
        event.put(TAINT_SWARM.get(), TaintSwarm.createAttributes().build());
        event.put(TAINTED_CHICKEN.get(), TaintedChicken.createAttributes().build());
        event.put(TAINTED_COW.get(), TaintedCow.createAttributes().build());
        event.put(TAINTED_CREEPER.get(), TaintedCreeper.createAttributes().build());
        event.put(TAINTED_PIG.get(), TaintedPig.createAttributes().build());
        event.put(TAINTED_SHEEP.get(), TaintedSheep.createAttributes().build());
        event.put(TAINTED_VILLAGER.get(), TaintedVillager.createAttributes().build());
        event.put(BRAINY_ZOMBIE.get(), BrainyZombie.createAttributes().build());
        event.put(GIANT_BRAINY_ZOMBIE.get(), GiantBrainyZombie.createAttributes().build());
        event.put(WISP.get(), Wisp.createAttributes().build());
        event.put(PECH.get(), Pech.createAttributes().build());
        event.put(THAUMIC_SLIME.get(), ThaumicSlime.createAttributes().build());
        event.put(INHABITED_ZOMBIE.get(), InhabitedZombie.createAttributes().build());
        event.put(CULTIST_KNIGHT.get(), CultistKnight.createAttributes().build());
        event.put(CULTIST_CLERIC.get(), CultistCleric.createAttributes().build());
        event.put(ELDRITCH_CRAB.get(), EldritchCrab.createAttributes().build());
        event.put(MIND_SPIDER.get(), MindSpider.createAttributes().build());
        event.put(ELDRITCH_GUARDIAN.get(), EldritchGuardian.createAttributes().build());
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(BRAINY_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            BrainyZombie::checkBrainyZombieSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GIANT_BRAINY_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            BrainyZombie::checkBrainyZombieSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ELDRITCH_GUARDIAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            thaumcraft.entity.monster.EldritchSpawnRules::checkGuardian, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(INHABITED_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            thaumcraft.entity.monster.EldritchSpawnRules::check, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
            TAINTACLE.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Taintacle::canSpawnNaturally,
            RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        event.register(
            PECH.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Pech::checkPechSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }
}
