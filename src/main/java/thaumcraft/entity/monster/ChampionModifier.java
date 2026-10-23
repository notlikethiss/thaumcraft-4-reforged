package thaumcraft.entity.monster;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import thaumcraft.Config;
import thaumcraft.Thaumcraft;
import thaumcraft.network.ShieldFxPayload;
import thaumcraft.platform.ModNetwork;
import thaumcraft.registry.ModEntities;
import thaumcraft.registry.ModSounds;
import thaumcraft.registry.ModItems;
import thaumcraft.entity.monster.eldritch.EldritchGuardian;
import thaumcraft.entity.monster.taint.Taintacle;
import thaumcraft.research.Warp;
import thaumcraft.research.WarpType;
import thaumcraft.world.dim.MazeHandler;
import thaumcraft.world.dim.OuterLands;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ChampionModifier {
    public static final int NORMAL = -1;
    public static final int UNPROCESSED = -2;
    private static final String[] NAMES = {"bold", "spine", "armor", "mighty", "grim", "warded", "warp", "undying", "fiery", "sickly", "venomous", "vampiric", "infested"};
    private static final int[] TYPES = {-1, 2, 2, -1, 1, 0, 1, 0, 1, 1, 1, 1, 2};
    private static final AttributeModifier CHAMPION_HEALTH = modifier("a62bef38-48cc-42a6-ac5e-ef913841c4fd", 30.0, AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier CHAMPION_DAMAGE = modifier("a340d2db-d881-4c25-ac62-f0ad14cd63b0", 2.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final AttributeModifier BOLD_SPEED = modifier("4b1edd33-caa9-47ae-a702-d86c05701037", 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final AttributeModifier MIGHTY_DAMAGE = modifier("7163897f-07f5-49b3-9ce4-b74beb83d2d3", 3.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private ChampionModifier() {
    }

    private static AttributeModifier modifier(String id, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(Thaumcraft.id("champion/" + id), amount, operation);
    }

    public static int get(Mob mob) {
        return ChampionKeys.MODIFIER.get(mob);
    }

    public static void assign(Mob mob, int id, boolean persist) {
        if (id < 0 || id >= NAMES.length || mob.level().isClientSide() || get(mob) != UNPROCESSED) {
            return;
        }
        ChampionKeys.MODIFIER.set(mob, id);
        boolean boss = mob instanceof thaumcraft.entity.boss.ThaumcraftBoss;
        if (!boss) {
            mob.getAttribute(Attributes.MAX_HEALTH).addOrReplacePermanentModifier(CHAMPION_HEALTH);
            mob.getAttribute(Attributes.ATTACK_DAMAGE).addOrReplacePermanentModifier(CHAMPION_DAMAGE);
            mob.heal(25.0F);
        }
        if (id == 0) {
            mob.getAttribute(Attributes.MOVEMENT_SPEED).addOrReplacePermanentModifier(BOLD_SPEED);
        } else if (id == 3) {
            mob.getAttribute(Attributes.ATTACK_DAMAGE).addOrReplacePermanentModifier(MIGHTY_DAMAGE);
        } else if (id == 5) {
            float absorption = mob.getAbsorptionAmount() + (float) (mob.getAttributeBaseValue(Attributes.MAX_HEALTH) / 2.0);
            mob.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(Math.max(mob.getAttributeBaseValue(Attributes.MAX_ABSORPTION), absorption));
            mob.setAbsorptionAmount(absorption);
        }
        if (mob instanceof thaumcraft.entity.boss.ThaumcraftBoss thaumcraftBoss) {
            thaumcraftBoss.generateName();
        } else if (!isVanilla(mob, "ender_dragon")) {
            mob.setCustomName(Component.translatable("champion.mod." + NAMES[id]).append(" ").append(mob.getName()));
        }
        if (persist) {
            mob.setPersistenceRequired();
        }
        ChampionKeys.MODIFIER.sync(mob);
    }

    @SubscribeEvent
    static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        int state = get(mob);
        if (state != UNPROCESSED) {
            return;
        }
        if (whitelistLevel(mob) < 0) {
            ChampionKeys.MODIFIER.set(mob, NORMAL);
            return;
        }
        processSpawn(mob, level.getRandom().nextInt(100));
    }

    public static void processSpawn(Mob mob, int initialRoll) {
        if (!(mob.level() instanceof ServerLevel level) || mob.level().isClientSide() || get(mob) != UNPROCESSED) {
            return;
        }
        int whitelist = whitelistLevel(mob);
        if (whitelist < 0) {
            ChampionKeys.MODIFIER.set(mob, NORMAL);
            return;
        }
        boolean champions = Config.championMobs();
        int roll = initialRoll;
        if (level.getDifficulty() == net.minecraft.world.Difficulty.EASY || !champions) {
            roll += 2;
        }
        if (level.getDifficulty() == net.minecraft.world.Difficulty.HARD && champions) {
            roll -= 2;
        }
        var mobX = (int) Math.ceil(mob.getX());
        var mobY = (int) Math.ceil(mob.getY());
        var mobZ = (int) Math.ceil(mob.getZ());
        var biome = level.getBiome(new net.minecraft.core.BlockPos(mobX, mobY, mobZ));
        if (biome.is(Tags.Biomes.IS_SPOOKY) || biome.is(Tags.Biomes.IS_NETHER) || biome.is(Tags.Biomes.IS_END)) {
            roll -= champions ? 2 : 1;
        }
        if (champions || mob instanceof thaumcraft.entity.boss.ThaumcraftBoss) {
            roll -= Math.max(0, whitelist - 1);
        }
        if (level.dimension().equals(OuterLands.OUTER_LANDS)) {
            roll -= 3;
            var cell = MazeHandler.getCell(level, mobX >> 4, mobZ >> 4);
            if (cell != null && (cell.feature == 6 || cell.feature == 8)) {
                roll -= champions ? 10 : 3;
            }
        }
        if (roll <= 0 && mob.getAttributeBaseValue(Attributes.MAX_HEALTH) >= 10.0) {
            int id = isVanilla(mob, "creeper") ? 0 : level.getRandom().nextInt(NAMES.length);
            assign(mob, id, false);
        } else {
            ChampionKeys.MODIFIER.set(mob, NORMAL);
        }
    }

    private static int whitelistLevel(Mob mob) {
        EntityType<?> type = mob.getType();
        int level = -1;
        if (mob instanceof Zombie || mob instanceof Spider || isVanilla(mob, "blaze") || isVanilla(mob, "enderman") || mob instanceof AbstractSkeleton || type == ModEntities.ELDRITCH_CRAB.get()) level = 0;
        if (mob instanceof Witch || mob instanceof thaumcraft.entity.monster.Pech || mob instanceof thaumcraft.entity.monster.Cultist) level = Math.max(level, 1);
        if (mob instanceof Taintacle) level = Math.max(level, 2);
        if (mob instanceof thaumcraft.entity.monster.InhabitedZombie) level = Math.max(level, 3);
        if (mob instanceof thaumcraft.entity.boss.ThaumcraftBoss) level = 200;
        return level;
    }

    private static boolean isVanilla(Mob mob, String path) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).equals(Identifier.withDefaultNamespace(path));
    }

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide() || mob.isDeadOrDying()) {
            return;
        }
        int id = get(mob);
        if (id == 5 && mob.invulnerableTime <= 0 && mob.tickCount % 25 == 0) {
            float limit = (float) (mob.getAttributeBaseValue(Attributes.MAX_HEALTH) / 2.0);
            if (mob.getAbsorptionAmount() < limit) {
                mob.setAbsorptionAmount(mob.getAbsorptionAmount() + 1.0F);
            }
        }
        if (id == 7 && mob.tickCount % 20 == 0) {
            mob.heal(1.0F);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (event.getAmount() <= 0.0F || !(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker) {
            return;
        }
        if (event.getEntity() instanceof Mob defender) {
            int id = get(defender);
            boolean absorptionBranch = (id == 5 || defender instanceof EldritchGuardian) && defender.getAbsorptionAmount() > 0.0F;
            if (absorptionBranch && defender.level() instanceof ServerLevel serverLevel) {
                int targetId = source.getEntity() == null ? -1 : source.getEntity().getId();
                ModNetwork.sendToNear(serverLevel, null, defender.getX(), defender.getY(), defender.getZ(), 32.0, new ShieldFxPayload(defender.getId(), targetId));
                serverLevel.playSound(null, defender.blockPosition(), ModSounds.RUNIC_SHIELD_EFFECT.get(), SoundSource.HOSTILE, 0.66F, 1.1F + serverLevel.getRandom().nextFloat() * 0.1F);
            } else if (id == 1 && !source.is(DamageTypes.THORNS) && defender.level() instanceof ServerLevel serverLevel) {
                attacker.hurtServer(serverLevel, defender.damageSources().thorns(defender), 1 + defender.getRandom().nextInt(3));
            } else if (id == 2 && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
                event.setAmount(event.getAmount() * 19.0F / 25.0F);
            } else if (id == 12 && defender.level() instanceof ServerLevel serverLevel && serverLevel.getRandom().nextFloat() < 0.4F) {
                var spider = ModEntities.TAINT_SPIDER.get().create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                if (spider != null) {
                    spider.snapTo(defender.getX(), defender.getY() + defender.getBbHeight() / 2.0, defender.getZ(), serverLevel.getRandom().nextFloat() * 360.0F, 0.0F);
                    serverLevel.addFreshEntity(spider);
                    serverLevel.playSound(null, defender.blockPosition(), ModSounds.GORE.get(), SoundSource.HOSTILE, 0.5F, 1.0F);
                }
            }
        }
        if (event.getAmount() <= 0.0F || !(attacker instanceof Mob mob)) {
            return;
        }
        int id = get(mob);
        if (id < 0 || TYPES[id] != 1 || !(event.getEntity() instanceof LivingEntity target) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        applyAttackEffect(id, mob, target, event.getAmount(), level.getRandom().nextFloat(), level.getRandom().nextInt(3));
    }

    public static void applyAttackEffect(int id, Mob attacker, LivingEntity target, float damage, float chanceRoll, int roll) {
        switch (id) {
            case 4 -> { if (chanceRoll < 0.4F) target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200)); }
            case 6 -> { if (target instanceof ServerPlayer player && chanceRoll < 0.33F) Warp.add(player, 1 + roll, WarpType.TEMPORARY); }
            case 8 -> { if (chanceRoll < 0.4F) target.igniteForSeconds(4.0F); }
            case 9 -> { if (chanceRoll < 0.4F) target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 500)); }
            case 10 -> { if (chanceRoll < 0.4F) target.addEffect(new MobEffectInstance(MobEffects.POISON, 100)); }
            case 11 -> attacker.heal(Math.max(2.0F, damage / 2.0F));
            default -> { }
        }
    }

    @SubscribeEvent
    static void onChampionDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level) || get(mob) < 0 || mob instanceof thaumcraft.entity.boss.ThaumcraftBoss
            || !event.isRecentlyHit() || event.getSource().getEntity() instanceof FakePlayer) return;
        int looting = event.getSource().getEntity() instanceof LivingEntity killer
            ? EnchantmentHelper.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), killer)
            : 0;
        awardChampionRewards(level, mob, event.getDrops(), looting, level.getRandom().nextInt(3), level.getRandom().nextInt(9));
    }

    public static void awardChampionRewards(ServerLevel level, Mob mob, java.util.Collection<ItemEntity> drops, int looting, int xpRoll, int bagRoll) {
        ExperienceOrb.award(level, mob.position(), 5 + Math.floorMod(xpRoll, 3));
        int tier = Math.min(2, (Math.floorMod(bagRoll, 9) + Math.max(0, looting)) / 5);
        var bag = new ItemStack(tier == 0 ? ModItems.COMMON_LOOT_BAG.get() : tier == 1 ? ModItems.UNCOMMON_LOOT_BAG.get() : ModItems.RARE_LOOT_BAG.get());
        drops.add(new ItemEntity(level, mob.getX(), mob.getY() + mob.getEyeHeight(), mob.getZ(), bag));
    }

    public static String name(int id) {
        return id >= 0 && id < NAMES.length ? NAMES[id] : "";
    }

    public static int count() {
        return NAMES.length;
    }

    public static List<String> names() {
        return List.of(NAMES);
    }
}
