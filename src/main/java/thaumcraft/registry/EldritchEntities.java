package thaumcraft.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.entity.boss.CultistLeader;
import thaumcraft.entity.boss.CultistPortal;
import thaumcraft.entity.boss.EldritchGolem;
import thaumcraft.entity.boss.EldritchWarden;
import thaumcraft.entity.boss.TaintacleGiant;

public final class EldritchEntities {
    private static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Thaumcraft.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<EldritchGolem>> ELDRITCH_GOLEM = register("eldritch_golem", EldritchGolem::new, 1.75F, 3.5F, true);
    public static final DeferredHolder<EntityType<?>, EntityType<EldritchWarden>> ELDRITCH_WARDEN = register("eldritch_warden", EldritchWarden::new, 1.5F, 3.5F, false);
    public static final DeferredHolder<EntityType<?>, EntityType<CultistLeader>> CULTIST_LEADER = register("cultist_leader", CultistLeader::new, 0.75F, 2.25F, false);
    public static final DeferredHolder<EntityType<?>, EntityType<TaintacleGiant>> TAINTACLE_GIANT = register("taintacle_giant", TaintacleGiant::new, 1.1F, 6.0F, false);
    public static final DeferredHolder<EntityType<?>, EntityType<CultistPortal>> CULTIST_PORTAL = register("cultist_portal", CultistPortal::new, 1.5F, 3.0F, true);

    private EldritchEntities() {
    }

    private static <T extends net.minecraft.world.entity.Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String name, EntityType.EntityFactory<T> factory, float width, float height, boolean fireImmune) {
        return ENTITIES.registerEntityType(name, factory, MobCategory.MONSTER, builder -> {
            builder.sized(width, height).clientTrackingRange(10).updateInterval(3).notInPeaceful();
            if (fireImmune) {
                builder.fireImmune();
            }
            return builder;
        });
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(EldritchEntities::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ELDRITCH_GOLEM.get(), EldritchGolem.createAttributes().build());
        event.put(ELDRITCH_WARDEN.get(), EldritchWarden.createAttributes().build());
        event.put(CULTIST_LEADER.get(), CultistLeader.createAttributes().build());
        event.put(TAINTACLE_GIANT.get(), TaintacleGiant.createAttributes().build());
        event.put(CULTIST_PORTAL.get(), CultistPortal.createAttributes().build());
    }

    public static boolean spawnCultistPortal(ServerLevel level, BlockPos pos) {
        CultistPortal portal = CULTIST_PORTAL.get().create(level, EntitySpawnReason.TRIGGERED);
        if (portal == null) {
            return false;
        }
        portal.snapTo(Vec3.atBottomCenterOf(pos), 0.0F, 0.0F);
        portal.setHomeTo(pos, 32);
        return level.addFreshEntity(portal);
    }

    public static boolean spawnBoss(ServerLevel level, BlockPos pos, BossKind kind) {
        return spawnBoss(level, pos, kind, pos);
    }

    public static boolean spawnBoss(ServerLevel level, BlockPos pos, BossKind kind, BlockPos home) {
        var type = switch (kind) {
            case GOLEM -> ELDRITCH_GOLEM.get();
            case WARDEN -> ELDRITCH_WARDEN.get();
            case CULTIST_PORTAL -> CULTIST_PORTAL.get();
            case TAINTACLE_GIANT -> TAINTACLE_GIANT.get();
        };
        if (kind == BossKind.TAINTACLE_GIANT) {
            return spawnTaintEncounter(level, pos);
        }
        var boss = type.create(level, EntitySpawnReason.TRIGGERED);
        if (boss == null) {
            return false;
        }
        boss.snapTo(Vec3.atBottomCenterOf(pos), 0.0F, 0.0F);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.TRIGGERED, null);
        boss.setHomeTo(home, 32);
        return level.addFreshEntity(boss);
    }

    public static boolean spawnTaintEncounter(ServerLevel level, BlockPos center) {
        boolean hard = level.getDifficulty() == net.minecraft.world.Difficulty.HARD;
        boolean secondGiant = level.getRandom().nextBoolean();
        boolean fourthGiant = level.getRandom().nextBoolean();
        boolean result = true;
        result &= spawnTaintacle(level, center, hard, true);
        result &= spawnTaintacle(level, center.offset(3, 0, 3), secondGiant, true);
        result &= spawnTaintacle(level, center.offset(-3, 0, 3), !secondGiant, true);
        result &= spawnTaintacle(level, center.offset(3, 0, -3), fourthGiant, true);
        result &= spawnTaintacle(level, center.offset(-3, 0, -3), !fourthGiant, true);
        return result;
    }

    private static boolean spawnTaintacle(ServerLevel level, BlockPos pos, boolean giant, boolean champion) {
        EntityType<? extends thaumcraft.entity.monster.taint.Taintacle> type = giant
            ? TAINTACLE_GIANT.get()
            : ModEntities.TAINTACLE.get();
        var entity = type.create(level, EntitySpawnReason.TRIGGERED);
        if (entity == null) {
            return false;
        }
        entity.snapTo(Vec3.atBottomCenterOf(pos.above(3)), 0.0F, 0.0F);
        entity.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.TRIGGERED, null);
        if (champion && !giant) {
            thaumcraft.entity.monster.ChampionModifier.assign(entity, level.getRandom().nextInt(13), true);
        }
        entity.setPersistenceRequired();
        return level.addFreshEntity(entity);
    }

    public enum BossKind {
        GOLEM,
        WARDEN,
        CULTIST_PORTAL,
        TAINTACLE_GIANT
    }
}
