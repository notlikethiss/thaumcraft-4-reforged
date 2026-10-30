package thaumcraft.registry;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
import thaumcraft.Thaumcraft;
import thaumcraft.device.ArcaneKeyItem;
import thaumcraft.device.OwnedBlockEntity;
import thaumcraft.essentia.PhialItem;
import thaumcraft.item.AspectContainerItem;
import thaumcraft.item.AlumentumItem;
import thaumcraft.item.BathSaltsItem;
import thaumcraft.item.BottledTaintItem;
import thaumcraft.item.CrystalEssenceItem;
import thaumcraft.item.GogglesItem;
import thaumcraft.item.KnowledgeFragmentItem;
import thaumcraft.item.LootBagItem;
import thaumcraft.item.PrimalCharmItem;
import thaumcraft.item.ResearchNotesItem;
import thaumcraft.item.ScribingToolsItem;
import thaumcraft.item.SanitizingSoapItem;
import thaumcraft.item.ThaumometerItem;
import thaumcraft.item.ThaumonomiconItem;
import thaumcraft.item.ZombieBrainItem;
import thaumcraft.item.armor.CultistBootsItem;
import thaumcraft.item.armor.CultistLeaderArmorItem;
import thaumcraft.item.armor.CultistPlateArmorItem;
import thaumcraft.item.armor.CultistRobeArmorItem;
import thaumcraft.item.armor.FortressArmorItem;
import thaumcraft.item.armor.HoverHarnessItem;
import thaumcraft.item.armor.RobeArmorItem;
import thaumcraft.item.armor.ThaumiumArmorItem;
import thaumcraft.item.armor.TravellerBootsItem;
import thaumcraft.item.armor.VoidArmorItem;
import thaumcraft.item.armor.VoidRobeArmorItem;
import thaumcraft.item.bauble.BaubleBlankItem;
import thaumcraft.item.bauble.HoverGirdleItem;
import thaumcraft.item.bauble.RunicAmuletItem;
import thaumcraft.item.bauble.RunicGirdleItem;
import thaumcraft.item.bauble.RunicRingItem;
import thaumcraft.item.bauble.VisAmuletItem;
import thaumcraft.item.gear.TcMaterials;
import thaumcraft.item.tool.BoneBowItem;
import thaumcraft.item.tool.CrimsonBladeItem;
import thaumcraft.item.tool.ElementalAxeItem;
import thaumcraft.item.tool.ElementalHoeItem;
import thaumcraft.item.tool.ElementalPickaxeItem;
import thaumcraft.item.tool.ElementalShovelItem;
import thaumcraft.item.tool.ElementalSwordItem;
import thaumcraft.item.tool.PrimalArrowItem;
import thaumcraft.item.tool.PrimalCrusherItem;
import thaumcraft.item.tool.ThaumiumAxeItem;
import thaumcraft.item.tool.ThaumiumHoeItem;
import thaumcraft.item.tool.ThaumiumPickaxeItem;
import thaumcraft.item.tool.ThaumiumShovelItem;
import thaumcraft.item.tool.ThaumiumSwordItem;
import thaumcraft.item.tool.VoidAxeItem;
import thaumcraft.item.tool.VoidHoeItem;
import thaumcraft.item.tool.VoidPickaxeItem;
import thaumcraft.item.tool.VoidShovelItem;
import thaumcraft.item.tool.VoidSwordItem;
import thaumcraft.platform.ArmorSlot;
import thaumcraft.platform.Registration;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.focus.ExcavationFocus;
import thaumcraft.wand.focus.FireFocus;
import thaumcraft.wand.focus.FocusItem;
import thaumcraft.wand.focus.FocusPouchItem;
import thaumcraft.wand.focus.FrostFocus;
import thaumcraft.wand.focus.HellbatFocus;
import thaumcraft.wand.focus.PechFocus;
import thaumcraft.wand.focus.PortableHoleFocus;
import thaumcraft.wand.focus.PrimalFocus;
import thaumcraft.wand.focus.ShockFocus;
import thaumcraft.wand.focus.TradeFocus;
import thaumcraft.wand.focus.WardingFocus;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Thaumcraft.MODID);

    static {
        Registration.armorMaterial(GogglesItem.MATERIAL);
        Registration.armorMaterial(TcMaterials.THAUMIUM_ARMOR);
        Registration.armorMaterial(TcMaterials.ROBE_ARMOR);
        Registration.armorMaterial(TcMaterials.TRAVELLER_ARMOR);
        Registration.armorMaterial(TcMaterials.HARNESS_ARMOR);
        Registration.armorMaterial(TcMaterials.FORTRESS_ARMOR);
        Registration.armorMaterial(TcMaterials.VOID_ARMOR);
        Registration.armorMaterial(TcMaterials.VOID_ROBE_ARMOR);
        Registration.armorMaterial(TcMaterials.CULTIST_ROBE_ARMOR);
        Registration.armorMaterial(TcMaterials.CULTIST_PLATE_ARMOR);
        Registration.armorMaterial(TcMaterials.CULTIST_BOOTS_ARMOR);
        Registration.armorMaterial(TcMaterials.CULTIST_LEADER_ARMOR);
    }

    public static final DeferredItem<Item> ALUMENTUM = RegistryHelper.registerItem(ITEMS, "alumentum", properties -> new AlumentumItem(Registration.fuel(properties, 6400)));
    public static final DeferredItem<Item> NITOR = RegistryHelper.registerItem(ITEMS, "nitor", properties -> new net.minecraft.world.item.BlockItem(ModBlocks.NITOR_LIGHT.get(), properties));
    public static final DeferredItem<Item> THAUMIUM_INGOT = RegistryHelper.registerSimpleItem(ITEMS, "thaumium_ingot");
    public static final DeferredItem<Item> QUICKSILVER = RegistryHelper.registerSimpleItem(ITEMS, "quicksilver");
    public static final DeferredItem<Item> MAGIC_TALLOW = RegistryHelper.registerSimpleItem(ITEMS, "magic_tallow");
    public static final DeferredItem<Item> AMBER = RegistryHelper.registerSimpleItem(ITEMS, "amber");
    public static final DeferredItem<Item> ENCHANTED_FABRIC = RegistryHelper.registerSimpleItem(ITEMS, "enchanted_fabric");
    public static final DeferredItem<Item> VIS_FILTER = RegistryHelper.registerSimpleItem(ITEMS, "vis_filter");
    public static final DeferredItem<Item> KNOWLEDGE_FRAGMENT = RegistryHelper.registerItem(ITEMS, "knowledge_fragment", KnowledgeFragmentItem::new);
    public static final DeferredItem<Item> MIRRORED_GLASS = RegistryHelper.registerSimpleItem(ITEMS, "mirrored_glass");
    public static final DeferredItem<Item> TAINTED_GOO = RegistryHelper.registerSimpleItem(ITEMS, "tainted_goo");
    public static final DeferredItem<Item> TAINT_TENDRIL = RegistryHelper.registerSimpleItem(ITEMS, "taint_tendril");
    public static final DeferredItem<Item> JAR_LABEL = RegistryHelper.registerItem(ITEMS, "jar_label", properties -> new AspectContainerItem(properties, "", false));
    public static final DeferredItem<Item> SALIS_MUNDUS = RegistryHelper.registerSimpleItem(ITEMS, "salis_mundus");
    public static final DeferredItem<Item> PRIMAL_CHARM = RegistryHelper.registerItem(ITEMS, "primal_charm", PrimalCharmItem::new);
    public static final DeferredItem<Item> VOID_METAL_INGOT = RegistryHelper.registerSimpleItem(ITEMS, "void_metal_ingot");
    public static final DeferredItem<Item> VOID_SEED = RegistryHelper.registerSimpleItem(ITEMS, "void_seed");
    public static final DeferredItem<Item> GOLD_COIN = RegistryHelper.registerSimpleItem(ITEMS, "gold_coin");

    public static final DeferredItem<SpawnEggItem> BRAINY_ZOMBIE_SPAWN_EGG = spawnEgg("brainy_zombie_spawn_egg", ModEntities.BRAINY_ZOMBIE, 0xFFC0FF, 0x008000);
    public static final DeferredItem<SpawnEggItem> GIANT_BRAINY_ZOMBIE_SPAWN_EGG = spawnEgg("giant_brainy_zombie_spawn_egg", ModEntities.GIANT_BRAINY_ZOMBIE, 0xFFC0FF, 0x004000);
    public static final DeferredItem<SpawnEggItem> WISP_SPAWN_EGG = spawnEgg("wisp_spawn_egg", ModEntities.WISP, 0xFFC0FF, 0xFFFFFF);
    public static final DeferredItem<SpawnEggItem> FIRE_BAT_SPAWN_EGG = spawnEgg("fire_bat_spawn_egg", ModEntities.FIREBAT, 0xFFC0FF, 0xC00000);
    public static final DeferredItem<SpawnEggItem> PECH_SPAWN_EGG = spawnEgg("pech_spawn_egg", ModEntities.PECH, 0xFFC0FF, 0x400040);
    public static final DeferredItem<SpawnEggItem> MIND_SPIDER_SPAWN_EGG = spawnEgg("mind_spider_spawn_egg", ModEntities.MIND_SPIDER, 0xAAAAAA, 0x404040);
    public static final DeferredItem<SpawnEggItem> ELDRITCH_GUARDIAN_SPAWN_EGG = spawnEgg("eldritch_guardian_spawn_egg", ModEntities.ELDRITCH_GUARDIAN, 0x222222, 0x404040);
    public static final DeferredItem<SpawnEggItem> CULTIST_KNIGHT_SPAWN_EGG = spawnEgg("cultist_knight_spawn_egg", ModEntities.CULTIST_KNIGHT, 0xFF5055, 0x000080);
    public static final DeferredItem<SpawnEggItem> CULTIST_CLERIC_SPAWN_EGG = spawnEgg("cultist_cleric_spawn_egg", ModEntities.CULTIST_CLERIC, 0xFF5055, 0x800000);
    public static final DeferredItem<SpawnEggItem> ELDRITCH_CRAB_SPAWN_EGG = spawnEgg("eldritch_crab_spawn_egg", ModEntities.ELDRITCH_CRAB, 0x555555, 0x550000);
    public static final DeferredItem<SpawnEggItem> INHABITED_ZOMBIE_SPAWN_EGG = spawnEgg("inhabited_zombie_spawn_egg", ModEntities.INHABITED_ZOMBIE, 0x557755, 0x550000);
    public static final DeferredItem<SpawnEggItem> THAUMIC_SLIME_SPAWN_EGG = spawnEgg("thaumic_slime_spawn_egg", ModEntities.THAUMIC_SLIME, 0xFFC0FF, 0xFF80FF);
    public static final DeferredItem<SpawnEggItem> TAINT_SPIDER_SPAWN_EGG = spawnEgg("taint_spider_spawn_egg", ModEntities.TAINT_SPIDER, 0xFFC0FF, 0x404040);
    public static final DeferredItem<SpawnEggItem> TAINTACLE_SPAWN_EGG = spawnEgg("taintacle_spawn_egg", ModEntities.TAINTACLE, 0xFFC0FF, 0x800080);
    public static final DeferredItem<SpawnEggItem> TAINTACLE_SMALL_SPAWN_EGG = spawnEgg("taintacle_small_spawn_egg", ModEntities.TAINTACLE_TINY, 0xFFC0FF, 0x800090);
    public static final DeferredItem<SpawnEggItem> TAINT_SPORE_SPAWN_EGG = spawnEgg("taint_spore_spawn_egg", ModEntities.TAINT_SPORE, 0xFFC0FF, 0x800070);
    public static final DeferredItem<SpawnEggItem> TAINT_SPORE_SWARMER_SPAWN_EGG = spawnEgg("taint_spore_swarmer_spawn_egg", ModEntities.TAINT_SPORE_SWARMER, 0xFFC0FF, 0x800060);
    public static final DeferredItem<SpawnEggItem> TAINT_SWARM_SPAWN_EGG = spawnEgg("taint_swarm_spawn_egg", ModEntities.TAINT_SWARM, 0xFFC0FF, 0x800050);
    public static final DeferredItem<SpawnEggItem> TAINT_CHICKEN_SPAWN_EGG = spawnEgg("taint_chicken_spawn_egg", ModEntities.TAINTED_CHICKEN, 0xFFC0FF, 0xC0C0C0);
    public static final DeferredItem<SpawnEggItem> TAINT_COW_SPAWN_EGG = spawnEgg("taint_cow_spawn_egg", ModEntities.TAINTED_COW, 0xFFC0FF, 0x7E3A3B);
    public static final DeferredItem<SpawnEggItem> TAINT_CREEPER_SPAWN_EGG = spawnEgg("taint_creeper_spawn_egg", ModEntities.TAINTED_CREEPER, 0xFFC0FF, 0x00FF00);
    public static final DeferredItem<SpawnEggItem> TAINT_PIG_SPAWN_EGG = spawnEgg("taint_pig_spawn_egg", ModEntities.TAINTED_PIG, 0xFFC0FF, 0xEF99EF);
    public static final DeferredItem<SpawnEggItem> TAINT_SHEEP_SPAWN_EGG = spawnEgg("taint_sheep_spawn_egg", ModEntities.TAINTED_SHEEP, 0xFFC0FF, 0x808080);
    public static final DeferredItem<SpawnEggItem> TAINT_VILLAGER_SPAWN_EGG = spawnEgg("taint_villager_spawn_egg", ModEntities.TAINTED_VILLAGER, 0xFFC0FF, 0x00FFFF);
    public static final DeferredItem<BucketItem> PURIFYING_FLUID_BUCKET = RegistryHelper.registerItem(ITEMS, "purifying_fluid_bucket", properties -> new BucketItem(ModFluids.PURIFYING_FLUID.get(), properties.stacksTo(1)));
    public static final DeferredItem<BucketItem> LIQUID_DEATH_BUCKET = RegistryHelper.registerItem(ITEMS, "liquid_death_bucket", properties -> new BucketItem(ModFluids.LIQUID_DEATH.get(), properties.stacksTo(1)));
    public static final DeferredItem<BathSaltsItem> BATH_SALTS = RegistryHelper.registerItem(ITEMS, "bath_salts", BathSaltsItem::new);
    public static final DeferredItem<SanitizingSoapItem> SANITIZING_SOAP = RegistryHelper.registerItem(ITEMS, "sanitizing_soap", SanitizingSoapItem::new);
    public static final DeferredItem<Item> SANITY_CHECKER = RegistryHelper.registerItem(ITEMS, "sanity_checker", Item::new, properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BottledTaintItem> BOTTLED_TAINT = RegistryHelper.registerItem(ITEMS, "bottled_taint", BottledTaintItem::new);

    public static final DeferredItem<thaumcraft.eldritch.EldritchObjectItem> CRIMSON_RITES = thaumcraft.registry.EldritchContent.CRIMSON_RITES;
    public static final DeferredItem<thaumcraft.item.golem.TravelingTrunkItem> TRAVELING_TRUNK = RegistryHelper.registerItem(ITEMS, "traveling_trunk", thaumcraft.item.golem.TravelingTrunkItem::new);
    private static final java.util.List<DeferredItem<? extends Item>> GOLEM_ITEMS = registerGolemItems();

    private static java.util.List<DeferredItem<? extends Item>> registerGolemItems() {
        var result = new java.util.ArrayList<DeferredItem<? extends Item>>();
        for (var type : thaumcraft.entity.golem.GolemType.values()) result.add(RegistryHelper.registerItem(ITEMS, type.itemId(), properties -> new thaumcraft.item.golem.GolemPlacerItem(properties, type)));
        result.add(RegistryHelper.registerItem(ITEMS, "golem_core_blank", properties -> new thaumcraft.item.golem.GolemPartItem(properties, thaumcraft.item.golem.GolemPartItem.Kind.CORE, -1)));
        for (int i = 0; i < thaumcraft.item.golem.GolemItems.CORES.length; i++) {
            int index = i;
            result.add(RegistryHelper.registerItem(ITEMS, "golem_core_" + thaumcraft.item.golem.GolemItems.CORES[i], properties -> new thaumcraft.item.golem.GolemPartItem(properties, thaumcraft.item.golem.GolemPartItem.Kind.CORE, index)));
        }
        for (int i = 0; i < 6; i++) {
            int index = i;
            result.add(RegistryHelper.registerItem(ITEMS, "golem_upgrade_" + thaumcraft.item.golem.GolemItems.UPGRADES[i], properties -> new thaumcraft.item.golem.GolemPartItem(properties, thaumcraft.item.golem.GolemPartItem.Kind.UPGRADE, index)));
        }
        for (int i = 0; i < 8; i++) {
            int index = i;
            result.add(RegistryHelper.registerItem(ITEMS, thaumcraft.item.golem.GolemItems.DECORATIONS[i], properties -> new thaumcraft.item.golem.GolemPartItem(properties, thaumcraft.item.golem.GolemPartItem.Kind.DECORATION, index)));
        }
        result.add(RegistryHelper.registerItem(ITEMS, "golem_bell", thaumcraft.item.golem.GolemBellItem::new));
        return java.util.List.copyOf(result);
    }

    public static final DeferredItem<SpawnEggItem> ELDRITCH_WARDEN_SPAWN_EGG = spawnEgg("eldritch_warden_spawn_egg", EldritchEntities.ELDRITCH_WARDEN, 0x552222, 0x404040);
    public static final DeferredItem<SpawnEggItem> ELDRITCH_GOLEM_SPAWN_EGG = spawnEgg("eldritch_golem_spawn_egg", EldritchEntities.ELDRITCH_GOLEM, 0x555555, 0x404040);
    public static final DeferredItem<SpawnEggItem> CULTIST_LEADER_SPAWN_EGG = spawnEgg("cultist_leader_spawn_egg", EldritchEntities.CULTIST_LEADER, 0xFF5055, 0x505050);
    public static final DeferredItem<SpawnEggItem> CULTIST_PORTAL_SPAWN_EGG = spawnEgg("cultist_portal_spawn_egg", EldritchEntities.CULTIST_PORTAL, 0xFF5055, 0xFF50FF);
    public static final DeferredItem<SpawnEggItem> TAINTACLE_GIANT_SPAWN_EGG = spawnEgg("taintacle_giant_spawn_egg", EldritchEntities.TAINTACLE_GIANT, 0xFFC0FF, 0x808080);

    public static final DeferredItem<Item> AIR_SHARD = RegistryHelper.registerSimpleItem(ITEMS, "air_shard");
    public static final DeferredItem<Item> FIRE_SHARD = RegistryHelper.registerSimpleItem(ITEMS, "fire_shard");
    public static final DeferredItem<Item> WATER_SHARD = RegistryHelper.registerSimpleItem(ITEMS, "water_shard");
    public static final DeferredItem<Item> EARTH_SHARD = RegistryHelper.registerSimpleItem(ITEMS, "earth_shard");
    public static final DeferredItem<Item> ORDER_SHARD = RegistryHelper.registerSimpleItem(ITEMS, "order_shard");
    public static final DeferredItem<Item> ENTROPY_SHARD = RegistryHelper.registerSimpleItem(ITEMS, "entropy_shard");
    public static final DeferredItem<Item> BALANCED_SHARD = RegistryHelper.registerSimpleItem(ITEMS, "balanced_shard");

    public static final DeferredItem<Item> COPPER_NUGGET = RegistryHelper.registerSimpleItem(ITEMS, "copper_nugget");
    public static final DeferredItem<Item> TIN_NUGGET = RegistryHelper.registerSimpleItem(ITEMS, "tin_nugget");
    public static final DeferredItem<Item> SILVER_NUGGET = RegistryHelper.registerSimpleItem(ITEMS, "silver_nugget");
    public static final DeferredItem<Item> LEAD_NUGGET = RegistryHelper.registerSimpleItem(ITEMS, "lead_nugget");
    public static final DeferredItem<Item> QUICKSILVER_DROP = RegistryHelper.registerSimpleItem(ITEMS, "quicksilver_drop");
    public static final DeferredItem<Item> THAUMIUM_NUGGET = RegistryHelper.registerSimpleItem(ITEMS, "thaumium_nugget");
    public static final DeferredItem<Item> VOID_METAL_NUGGET = RegistryHelper.registerSimpleItem(ITEMS, "void_metal_nugget");
    public static final DeferredItem<Item> NATIVE_IRON_CLUSTER = RegistryHelper.registerSimpleItem(ITEMS, "native_iron_cluster");
    public static final DeferredItem<Item> NATIVE_COPPER_CLUSTER = RegistryHelper.registerSimpleItem(ITEMS, "native_copper_cluster");
    public static final DeferredItem<Item> NATIVE_TIN_CLUSTER = RegistryHelper.registerSimpleItem(ITEMS, "native_tin_cluster");
    public static final DeferredItem<Item> NATIVE_SILVER_CLUSTER = RegistryHelper.registerSimpleItem(ITEMS, "native_silver_cluster");
    public static final DeferredItem<Item> NATIVE_LEAD_CLUSTER = RegistryHelper.registerSimpleItem(ITEMS, "native_lead_cluster");
    public static final DeferredItem<Item> NATIVE_CINNABAR_CLUSTER = RegistryHelper.registerSimpleItem(ITEMS, "native_cinnabar_cluster");
    public static final DeferredItem<Item> NATIVE_GOLD_CLUSTER = RegistryHelper.registerSimpleItem(ITEMS, "native_gold_cluster");

    public static final DeferredItem<Item> CHICKEN_NUGGET = nugget("chicken_nugget");
    public static final DeferredItem<Item> BEEF_NUGGET = nugget("beef_nugget");
    public static final DeferredItem<Item> PORK_NUGGET = nugget("pork_nugget");
    public static final DeferredItem<Item> FISH_NUGGET = nugget("fish_nugget");
    public static final DeferredItem<Item> TRIPLE_MEAT_TREAT = RegistryHelper.registerSimpleItem(ITEMS,
        "triple_meat_treat",
        properties -> Registration.food(properties, 6, 0.8F, true, 1.6F, new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 0.66F)
    );
    public static final DeferredItem<Item> ZOMBIE_BRAIN = RegistryHelper.registerItem(ITEMS,
        "zombie_brain",
        ZombieBrainItem::new,
        properties -> Registration.food(properties, 4, 0.2F, false, 1.6F, new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8F)
    );

    public static final DeferredItem<Item> GLASS_PHIAL = RegistryHelper.registerItem(ITEMS, "glass_phial", properties -> new PhialItem(false, properties));
    public static final DeferredItem<Item> ESSENTIA_PHIAL = RegistryHelper.registerItem(ITEMS, "essentia_phial", properties -> new PhialItem(true, properties));
    public static final DeferredItem<Item> WISP_ESSENCE = RegistryHelper.registerItem(ITEMS, "wisp_essence", properties -> new AspectContainerItem(properties, " x", true));
    public static final DeferredItem<Item> CRYSTALLIZED_ESSENCE = RegistryHelper.registerItem(ITEMS, "crystallized_essence", CrystalEssenceItem::new);

    public static final DeferredItem<Item> COMMON_LOOT_BAG = RegistryHelper.registerItem(ITEMS, "common_loot_bag", LootBagItem::new);
    public static final DeferredItem<Item> UNCOMMON_LOOT_BAG = RegistryHelper.registerItem(ITEMS, "uncommon_loot_bag", LootBagItem::new, properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> RARE_LOOT_BAG = RegistryHelper.registerItem(ITEMS, "rare_loot_bag", LootBagItem::new, properties -> properties.rarity(Rarity.RARE));

    public static final DeferredItem<WandItem> WAND = RegistryHelper.registerItem(ITEMS, "wand", WandItem::new, WandItem::properties);
    public static final DeferredItem<Item> IRON_WAND_CAP = RegistryHelper.registerSimpleItem(ITEMS, "iron_wand_cap");
    public static final DeferredItem<Item> GOLD_WAND_CAP = RegistryHelper.registerSimpleItem(ITEMS, "gold_wand_cap");
    public static final DeferredItem<Item> THAUMIUM_WAND_CAP = RegistryHelper.registerSimpleItem(ITEMS, "thaumium_wand_cap");
    public static final DeferredItem<Item> COPPER_WAND_CAP = RegistryHelper.registerSimpleItem(ITEMS, "copper_wand_cap");
    public static final DeferredItem<Item> SILVER_WAND_CAP = RegistryHelper.registerSimpleItem(ITEMS, "silver_wand_cap");
    public static final DeferredItem<Item> INERT_SILVER_WAND_CAP = RegistryHelper.registerSimpleItem(ITEMS, "inert_silver_wand_cap");
    public static final DeferredItem<Item> INERT_THAUMIUM_WAND_CAP = RegistryHelper.registerSimpleItem(ITEMS, "inert_thaumium_wand_cap");
    public static final DeferredItem<Item> VOID_WAND_CAP = RegistryHelper.registerSimpleItem(ITEMS, "void_wand_cap");
    public static final DeferredItem<Item> INERT_VOID_WAND_CAP = RegistryHelper.registerSimpleItem(ITEMS, "inert_void_wand_cap");
    public static final DeferredItem<Item> GREATWOOD_ROD = RegistryHelper.registerSimpleItem(ITEMS, "greatwood_rod");
    public static final DeferredItem<Item> OBSIDIAN_ROD = RegistryHelper.registerSimpleItem(ITEMS, "obsidian_rod");
    public static final DeferredItem<Item> SILVERWOOD_ROD = RegistryHelper.registerSimpleItem(ITEMS, "silverwood_rod");
    public static final DeferredItem<Item> ICY_ROD = RegistryHelper.registerSimpleItem(ITEMS, "icy_rod");
    public static final DeferredItem<Item> QUARTZ_ROD = RegistryHelper.registerSimpleItem(ITEMS, "quartz_rod");
    public static final DeferredItem<Item> REED_ROD = RegistryHelper.registerSimpleItem(ITEMS, "reed_rod");
    public static final DeferredItem<Item> BLAZING_ROD = RegistryHelper.registerSimpleItem(ITEMS, "blazing_rod");
    public static final DeferredItem<Item> BONE_ROD = RegistryHelper.registerSimpleItem(ITEMS, "bone_rod");
    public static final DeferredItem<Item> GREATWOOD_STAFF_CORE = RegistryHelper.registerSimpleItem(ITEMS, "greatwood_staff_core");
    public static final DeferredItem<Item> OBSIDIAN_STAFF_CORE = RegistryHelper.registerSimpleItem(ITEMS, "obsidian_staff_core");
    public static final DeferredItem<Item> SILVERWOOD_STAFF_CORE = RegistryHelper.registerSimpleItem(ITEMS, "silverwood_staff_core");
    public static final DeferredItem<Item> ICY_STAFF_CORE = RegistryHelper.registerSimpleItem(ITEMS, "icy_staff_core");
    public static final DeferredItem<Item> QUARTZ_STAFF_CORE = RegistryHelper.registerSimpleItem(ITEMS, "quartz_staff_core");
    public static final DeferredItem<Item> REED_STAFF_CORE = RegistryHelper.registerSimpleItem(ITEMS, "reed_staff_core");
    public static final DeferredItem<Item> BLAZING_STAFF_CORE = RegistryHelper.registerSimpleItem(ITEMS, "blazing_staff_core");
    public static final DeferredItem<Item> BONE_STAFF_CORE = RegistryHelper.registerSimpleItem(ITEMS, "bone_staff_core");
    public static final DeferredItem<Item> PRIMAL_STAFF_CORE = RegistryHelper.registerSimpleItem(ITEMS, "primal_staff_core");
    public static final DeferredItem<ThaumometerItem> THAUMOMETER = RegistryHelper.registerItem(ITEMS, "thaumometer", ThaumometerItem::new, ThaumometerItem::properties);
    public static final DeferredItem<GogglesItem> GOGGLES_OF_REVEALING = RegistryHelper.registerItem(ITEMS, "goggles_of_revealing", GogglesItem::new, GogglesItem::properties);
    public static final DeferredItem<ScribingToolsItem> SCRIBING_TOOLS = RegistryHelper.registerItem(ITEMS, "scribing_tools", ScribingToolsItem::new, ScribingToolsItem::properties);
    public static final DeferredItem<ResearchNotesItem> RESEARCH_NOTES = RegistryHelper.registerItem(ITEMS, "research_notes", properties -> new ResearchNotesItem(properties, false), properties -> properties.rarity(Rarity.RARE));
    public static final DeferredItem<ResearchNotesItem> DISCOVERY = RegistryHelper.registerItem(ITEMS, "discovery", properties -> new ResearchNotesItem(properties, true), properties -> properties.rarity(Rarity.EPIC).stacksTo(64));
    public static final DeferredItem<ThaumonomiconItem> THAUMONOMICON = RegistryHelper.registerItem(ITEMS, "thaumonomicon", properties -> new ThaumonomiconItem(properties, false), ThaumonomiconItem::properties);
    public static final DeferredItem<ThaumonomiconItem> THAUMONOMICON_CHEAT = RegistryHelper.registerItem(ITEMS, "thaumonomicon_cheat", properties -> new ThaumonomiconItem(properties, true), ThaumonomiconItem::properties);

    public static final DeferredItem<FocusPouchItem> FOCUS_POUCH = RegistryHelper.registerItem(ITEMS, "focus_pouch", FocusPouchItem::new, properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<FireFocus> FOCUS_FIRE = RegistryHelper.registerItem(ITEMS, "focus_fire", FireFocus::new, FocusItem::properties);
    public static final DeferredItem<ShockFocus> FOCUS_SHOCK = RegistryHelper.registerItem(ITEMS, "focus_shock", ShockFocus::new, FocusItem::properties);
    public static final DeferredItem<HellbatFocus> FOCUS_NINE_HELLS = RegistryHelper.registerItem(ITEMS, "focus_nine_hells", HellbatFocus::new, FocusItem::properties);
    public static final DeferredItem<FrostFocus> FOCUS_FROST = RegistryHelper.registerItem(ITEMS, "focus_frost", FrostFocus::new, FocusItem::properties);
    public static final DeferredItem<TradeFocus> FOCUS_EQUAL_TRADE = RegistryHelper.registerItem(ITEMS, "focus_equal_trade", TradeFocus::new, FocusItem::properties);
    public static final DeferredItem<ExcavationFocus> FOCUS_EXCAVATION = RegistryHelper.registerItem(ITEMS, "focus_excavation", ExcavationFocus::new, FocusItem::properties);
    public static final DeferredItem<PortableHoleFocus> FOCUS_PORTABLE_HOLE = RegistryHelper.registerItem(ITEMS, "focus_portable_hole", PortableHoleFocus::new, FocusItem::properties);
    public static final DeferredItem<PechFocus> FOCUS_PECH = RegistryHelper.registerItem(ITEMS, "focus_pech", PechFocus::new, FocusItem::properties);
    public static final DeferredItem<WardingFocus> FOCUS_WARDING = RegistryHelper.registerItem(ITEMS, "focus_warding", WardingFocus::new, FocusItem::properties);
    public static final DeferredItem<PrimalFocus> FOCUS_PRIMAL = RegistryHelper.registerItem(ITEMS, "focus_primal", PrimalFocus::new, FocusItem::properties);

    public static final DeferredItem<ThaumiumSwordItem> THAUMIUM_SWORD = RegistryHelper.registerItem(ITEMS, "thaumium_sword", ThaumiumSwordItem::new, properties -> Registration.sword(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_TOOL, 3.0F, -2.4F));
    public static final DeferredItem<ThaumiumPickaxeItem> THAUMIUM_PICKAXE = RegistryHelper.registerItem(ITEMS, "thaumium_pickaxe", ThaumiumPickaxeItem::new, properties -> Registration.pickaxe(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_TOOL, 1.0F, -2.8F));
    public static final DeferredItem<ThaumiumAxeItem> THAUMIUM_AXE = RegistryHelper.registerItem(ITEMS, "thaumium_axe", ThaumiumAxeItem::new, properties -> Registration.axe(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_TOOL, 2.0F, -3.0F));
    public static final DeferredItem<ThaumiumShovelItem> THAUMIUM_SHOVEL = RegistryHelper.registerItem(ITEMS, "thaumium_shovel", ThaumiumShovelItem::new, properties -> Registration.shovel(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_TOOL, 0.0F, -3.0F));
    public static final DeferredItem<ThaumiumHoeItem> THAUMIUM_HOE = RegistryHelper.registerItem(ITEMS, "thaumium_hoe", ThaumiumHoeItem::new, properties -> Registration.hoe(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_TOOL, -2.0F, -1.0F));
    public static final DeferredItem<VoidSwordItem> VOID_SWORD = RegistryHelper.registerItem(ITEMS, "void_sword", VoidSwordItem::new, properties -> Registration.sword(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_TOOL, 3.0F, -2.4F));
    public static final DeferredItem<VoidPickaxeItem> VOID_PICKAXE = RegistryHelper.registerItem(ITEMS, "void_pickaxe", VoidPickaxeItem::new, properties -> Registration.pickaxe(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_TOOL, 1.0F, -2.8F));
    public static final DeferredItem<VoidAxeItem> VOID_AXE = RegistryHelper.registerItem(ITEMS, "void_axe", VoidAxeItem::new, properties -> Registration.axe(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_TOOL, 2.0F, -3.0F));
    public static final DeferredItem<VoidShovelItem> VOID_SHOVEL = RegistryHelper.registerItem(ITEMS, "void_shovel", VoidShovelItem::new, properties -> Registration.shovel(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_TOOL, 0.0F, -3.0F));
    public static final DeferredItem<VoidHoeItem> VOID_HOE = RegistryHelper.registerItem(ITEMS, "void_hoe", VoidHoeItem::new, properties -> Registration.hoe(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_TOOL, -3.0F, -1.0F));
    public static final DeferredItem<ElementalSwordItem> ELEMENTAL_SWORD = RegistryHelper.registerItem(ITEMS, "elemental_sword", ElementalSwordItem::new, properties -> Registration.sword(properties.rarity(Rarity.RARE), TcMaterials.ELEMENTAL_TOOL, 3.0F, -2.4F));
    public static final DeferredItem<ElementalPickaxeItem> ELEMENTAL_PICKAXE = RegistryHelper.registerItem(ITEMS, "elemental_pickaxe", ElementalPickaxeItem::new, properties -> Registration.pickaxe(properties.rarity(Rarity.RARE), TcMaterials.ELEMENTAL_TOOL, 1.0F, -2.8F));
    public static final DeferredItem<ElementalAxeItem> ELEMENTAL_AXE = RegistryHelper.registerItem(ITEMS, "elemental_axe", ElementalAxeItem::new, properties -> Registration.axe(properties.rarity(Rarity.RARE), TcMaterials.ELEMENTAL_TOOL, 2.0F, -3.0F));
    public static final DeferredItem<ElementalShovelItem> ELEMENTAL_SHOVEL = RegistryHelper.registerItem(ITEMS, "elemental_shovel", ElementalShovelItem::new, properties -> Registration.shovel(properties.rarity(Rarity.RARE), TcMaterials.ELEMENTAL_TOOL, 0.0F, -3.0F));
    public static final DeferredItem<ElementalHoeItem> ELEMENTAL_HOE = RegistryHelper.registerItem(ITEMS, "elemental_hoe", ElementalHoeItem::new, properties -> Registration.hoe(properties.rarity(Rarity.RARE), TcMaterials.ELEMENTAL_TOOL, -3.0F, -1.0F));
    public static final DeferredItem<CrimsonBladeItem> CRIMSON_BLADE = RegistryHelper.registerItem(ITEMS, "crimson_blade", CrimsonBladeItem::new, properties -> Registration.sword(properties.rarity(Rarity.RARE), TcMaterials.CRIMSON_VOID_TOOL, 3.0F, -2.4F));
    public static final DeferredItem<PrimalCrusherItem> PRIMAL_CRUSHER = RegistryHelper.registerItem(ITEMS, "primal_crusher", PrimalCrusherItem::new, properties -> Registration.pickaxeAndShovel(properties.rarity(Rarity.EPIC), TcMaterials.PRIMAL_VOID_TOOL, 2.5F, -2.8F));
    public static final DeferredItem<BoneBowItem> BONE_BOW = RegistryHelper.registerItem(ITEMS, "bone_bow", BoneBowItem::new, properties -> properties.durability(512).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<PrimalArrowItem> AIR_ARROW = RegistryHelper.registerItem(ITEMS, "air_arrow", properties -> new PrimalArrowItem(properties, 0));
    public static final DeferredItem<PrimalArrowItem> FIRE_ARROW = RegistryHelper.registerItem(ITEMS, "fire_arrow", properties -> new PrimalArrowItem(properties, 1));
    public static final DeferredItem<PrimalArrowItem> WATER_ARROW = RegistryHelper.registerItem(ITEMS, "water_arrow", properties -> new PrimalArrowItem(properties, 2));
    public static final DeferredItem<PrimalArrowItem> EARTH_ARROW = RegistryHelper.registerItem(ITEMS, "earth_arrow", properties -> new PrimalArrowItem(properties, 3));
    public static final DeferredItem<PrimalArrowItem> ORDER_ARROW = RegistryHelper.registerItem(ITEMS, "order_arrow", properties -> new PrimalArrowItem(properties, 4));
    public static final DeferredItem<PrimalArrowItem> ENTROPY_ARROW = RegistryHelper.registerItem(ITEMS, "entropy_arrow", properties -> new PrimalArrowItem(properties, 5));

    public static final DeferredItem<ThaumiumArmorItem> THAUMIUM_HELMET = RegistryHelper.registerItem(ITEMS, "thaumium_helmet", properties -> new ThaumiumArmorItem(TcMaterials.THAUMIUM_ARMOR, ArmorSlot.HELMET, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_ARMOR, ArmorSlot.HELMET));
    public static final DeferredItem<ThaumiumArmorItem> THAUMIUM_CHESTPLATE = RegistryHelper.registerItem(ITEMS, "thaumium_chestplate", properties -> new ThaumiumArmorItem(TcMaterials.THAUMIUM_ARMOR, ArmorSlot.CHESTPLATE, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_ARMOR, ArmorSlot.CHESTPLATE));
    public static final DeferredItem<ThaumiumArmorItem> THAUMIUM_LEGGINGS = RegistryHelper.registerItem(ITEMS, "thaumium_leggings", properties -> new ThaumiumArmorItem(TcMaterials.THAUMIUM_ARMOR, ArmorSlot.LEGGINGS, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_ARMOR, ArmorSlot.LEGGINGS));
    public static final DeferredItem<ThaumiumArmorItem> THAUMIUM_BOOTS = RegistryHelper.registerItem(ITEMS, "thaumium_boots", properties -> new ThaumiumArmorItem(TcMaterials.THAUMIUM_ARMOR, ArmorSlot.BOOTS, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_ARMOR, ArmorSlot.BOOTS));
    public static final DeferredItem<RobeArmorItem> ROBE_CHESTPLATE = RegistryHelper.registerItem(ITEMS, "robe_chestplate", properties -> new RobeArmorItem(TcMaterials.ROBE_ARMOR, ArmorSlot.CHESTPLATE, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.ROBE_ARMOR, ArmorSlot.CHESTPLATE));
    public static final DeferredItem<RobeArmorItem> ROBE_LEGGINGS = RegistryHelper.registerItem(ITEMS, "robe_leggings", properties -> new RobeArmorItem(TcMaterials.ROBE_ARMOR, ArmorSlot.LEGGINGS, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.ROBE_ARMOR, ArmorSlot.LEGGINGS));
    public static final DeferredItem<RobeArmorItem> ROBE_BOOTS = RegistryHelper.registerItem(ITEMS, "robe_boots", properties -> new RobeArmorItem(TcMaterials.ROBE_ARMOR, ArmorSlot.BOOTS, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.ROBE_ARMOR, ArmorSlot.BOOTS));
    public static final DeferredItem<TravellerBootsItem> BOOTS_TRAVELLER = RegistryHelper.registerItem(ITEMS, "boots_traveller", properties -> new TravellerBootsItem(TcMaterials.TRAVELLER_ARMOR, ArmorSlot.BOOTS, properties), properties -> Registration.armor(properties.rarity(Rarity.RARE), TcMaterials.TRAVELLER_ARMOR, ArmorSlot.BOOTS).durability(350));
    public static final DeferredItem<HoverHarnessItem> HOVER_HARNESS = RegistryHelper.registerItem(ITEMS, "hover_harness", properties -> new HoverHarnessItem(TcMaterials.HARNESS_ARMOR, ArmorSlot.CHESTPLATE, properties), properties -> Registration.armor(properties.rarity(Rarity.EPIC), TcMaterials.HARNESS_ARMOR, ArmorSlot.CHESTPLATE).durability(400));
    public static final DeferredItem<FortressArmorItem> FORTRESS_HELMET = RegistryHelper.registerItem(ITEMS, "fortress_helmet", properties -> new FortressArmorItem(TcMaterials.FORTRESS_ARMOR, ArmorSlot.HELMET, properties), properties -> Registration.armor(properties.rarity(Rarity.RARE), TcMaterials.FORTRESS_ARMOR, ArmorSlot.HELMET));
    public static final DeferredItem<FortressArmorItem> FORTRESS_CHESTPLATE = RegistryHelper.registerItem(ITEMS, "fortress_chestplate", properties -> new FortressArmorItem(TcMaterials.FORTRESS_ARMOR, ArmorSlot.CHESTPLATE, properties), properties -> Registration.armor(properties.rarity(Rarity.RARE), TcMaterials.FORTRESS_ARMOR, ArmorSlot.CHESTPLATE));
    public static final DeferredItem<FortressArmorItem> FORTRESS_LEGGINGS = RegistryHelper.registerItem(ITEMS, "fortress_leggings", properties -> new FortressArmorItem(TcMaterials.FORTRESS_ARMOR, ArmorSlot.LEGGINGS, properties), properties -> Registration.armor(properties.rarity(Rarity.RARE), TcMaterials.FORTRESS_ARMOR, ArmorSlot.LEGGINGS));
    public static final DeferredItem<VoidArmorItem> VOID_HELMET = RegistryHelper.registerItem(ITEMS, "void_helmet", properties -> new VoidArmorItem(TcMaterials.VOID_ARMOR, ArmorSlot.HELMET, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_ARMOR, ArmorSlot.HELMET));
    public static final DeferredItem<VoidArmorItem> VOID_CHESTPLATE = RegistryHelper.registerItem(ITEMS, "void_chestplate", properties -> new VoidArmorItem(TcMaterials.VOID_ARMOR, ArmorSlot.CHESTPLATE, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_ARMOR, ArmorSlot.CHESTPLATE));
    public static final DeferredItem<VoidArmorItem> VOID_LEGGINGS = RegistryHelper.registerItem(ITEMS, "void_leggings", properties -> new VoidArmorItem(TcMaterials.VOID_ARMOR, ArmorSlot.LEGGINGS, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_ARMOR, ArmorSlot.LEGGINGS));
    public static final DeferredItem<VoidArmorItem> VOID_BOOTS = RegistryHelper.registerItem(ITEMS, "void_boots", properties -> new VoidArmorItem(TcMaterials.VOID_ARMOR, ArmorSlot.BOOTS, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_ARMOR, ArmorSlot.BOOTS));
    public static final DeferredItem<VoidRobeArmorItem> VOID_ROBE_HELMET = RegistryHelper.registerItem(ITEMS, "void_robe_helmet", properties -> new VoidRobeArmorItem(TcMaterials.VOID_ROBE_ARMOR, ArmorSlot.HELMET, properties), properties -> Registration.armor(properties.rarity(Rarity.EPIC), TcMaterials.VOID_ROBE_ARMOR, ArmorSlot.HELMET));
    public static final DeferredItem<VoidRobeArmorItem> VOID_ROBE_CHESTPLATE = RegistryHelper.registerItem(ITEMS, "void_robe_chestplate", properties -> new VoidRobeArmorItem(TcMaterials.VOID_ROBE_ARMOR, ArmorSlot.CHESTPLATE, properties), properties -> Registration.armor(properties.rarity(Rarity.EPIC), TcMaterials.VOID_ROBE_ARMOR, ArmorSlot.CHESTPLATE));
    public static final DeferredItem<VoidRobeArmorItem> VOID_ROBE_LEGGINGS = RegistryHelper.registerItem(ITEMS, "void_robe_leggings", properties -> new VoidRobeArmorItem(TcMaterials.VOID_ROBE_ARMOR, ArmorSlot.LEGGINGS, properties), properties -> Registration.armor(properties.rarity(Rarity.EPIC), TcMaterials.VOID_ROBE_ARMOR, ArmorSlot.LEGGINGS));
    public static final DeferredItem<CultistRobeArmorItem> CRIMSON_CULT_HOOD = RegistryHelper.registerItem(ITEMS, "crimson_cult_hood", properties -> new CultistRobeArmorItem(TcMaterials.CULTIST_ROBE_ARMOR, ArmorSlot.HELMET, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.CULTIST_ROBE_ARMOR, ArmorSlot.HELMET));
    public static final DeferredItem<CultistRobeArmorItem> CRIMSON_CULT_ROBE = RegistryHelper.registerItem(ITEMS, "crimson_cult_robe", properties -> new CultistRobeArmorItem(TcMaterials.CULTIST_ROBE_ARMOR, ArmorSlot.CHESTPLATE, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.CULTIST_ROBE_ARMOR, ArmorSlot.CHESTPLATE));
    public static final DeferredItem<CultistRobeArmorItem> CRIMSON_CULT_LEGGINGS = RegistryHelper.registerItem(ITEMS, "crimson_cult_leggings", properties -> new CultistRobeArmorItem(TcMaterials.CULTIST_ROBE_ARMOR, ArmorSlot.LEGGINGS, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.CULTIST_ROBE_ARMOR, ArmorSlot.LEGGINGS));
    public static final DeferredItem<CultistBootsItem> CRIMSON_CULT_BOOTS = RegistryHelper.registerItem(ITEMS, "crimson_cult_boots", properties -> new CultistBootsItem(TcMaterials.CULTIST_BOOTS_ARMOR, ArmorSlot.BOOTS, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.CULTIST_BOOTS_ARMOR, ArmorSlot.BOOTS));
    public static final DeferredItem<CultistPlateArmorItem> CRIMSON_CULT_HELM = RegistryHelper.registerItem(ITEMS, "crimson_cult_helm", properties -> new CultistPlateArmorItem(TcMaterials.CULTIST_PLATE_ARMOR, ArmorSlot.HELMET, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.CULTIST_PLATE_ARMOR, ArmorSlot.HELMET));
    public static final DeferredItem<CultistPlateArmorItem> CRIMSON_CULT_CHESTPLATE = RegistryHelper.registerItem(ITEMS, "crimson_cult_chestplate", properties -> new CultistPlateArmorItem(TcMaterials.CULTIST_PLATE_ARMOR, ArmorSlot.CHESTPLATE, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.CULTIST_PLATE_ARMOR, ArmorSlot.CHESTPLATE));
    public static final DeferredItem<CultistPlateArmorItem> CRIMSON_CULT_GREAVES = RegistryHelper.registerItem(ITEMS, "crimson_cult_greaves", properties -> new CultistPlateArmorItem(TcMaterials.CULTIST_PLATE_ARMOR, ArmorSlot.LEGGINGS, properties), properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.CULTIST_PLATE_ARMOR, ArmorSlot.LEGGINGS));
    public static final DeferredItem<CultistLeaderArmorItem> CRIMSON_PRAETOR_HELM = RegistryHelper.registerItem(ITEMS, "crimson_praetor_helm", properties -> new CultistLeaderArmorItem(TcMaterials.CULTIST_LEADER_ARMOR, ArmorSlot.HELMET, properties), properties -> Registration.armor(properties.rarity(Rarity.RARE), TcMaterials.CULTIST_LEADER_ARMOR, ArmorSlot.HELMET));
    public static final DeferredItem<CultistLeaderArmorItem> CRIMSON_PRAETOR_CHESTPLATE = RegistryHelper.registerItem(ITEMS, "crimson_praetor_chestplate", properties -> new CultistLeaderArmorItem(TcMaterials.CULTIST_LEADER_ARMOR, ArmorSlot.CHESTPLATE, properties), properties -> Registration.armor(properties.rarity(Rarity.RARE), TcMaterials.CULTIST_LEADER_ARMOR, ArmorSlot.CHESTPLATE));
    public static final DeferredItem<CultistLeaderArmorItem> CRIMSON_PRAETOR_GREAVES = RegistryHelper.registerItem(ITEMS, "crimson_praetor_greaves", properties -> new CultistLeaderArmorItem(TcMaterials.CULTIST_LEADER_ARMOR, ArmorSlot.LEGGINGS, properties), properties -> Registration.armor(properties.rarity(Rarity.RARE), TcMaterials.CULTIST_LEADER_ARMOR, ArmorSlot.LEGGINGS));

    public static final DeferredItem<BaubleBlankItem> MUNDANE_AMULET = RegistryHelper.registerItem(ITEMS, "mundane_amulet", properties -> new BaubleBlankItem(properties, 0), properties -> properties.stacksTo(1).rarity(Rarity.COMMON));
    public static final DeferredItem<BaubleBlankItem> MUNDANE_RING = RegistryHelper.registerItem(ITEMS, "mundane_ring", properties -> new BaubleBlankItem(properties, 1), properties -> properties.stacksTo(1).rarity(Rarity.COMMON));
    public static final DeferredItem<BaubleBlankItem> MUNDANE_BELT = RegistryHelper.registerItem(ITEMS, "mundane_belt", properties -> new BaubleBlankItem(properties, 2), properties -> properties.stacksTo(1).rarity(Rarity.COMMON));
    public static final DeferredItem<BaubleBlankItem> APPRENTICE_RING_AIR = RegistryHelper.registerItem(ITEMS, "apprentice_ring_air", properties -> new BaubleBlankItem(properties, 3), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BaubleBlankItem> APPRENTICE_RING_EARTH = RegistryHelper.registerItem(ITEMS, "apprentice_ring_earth", properties -> new BaubleBlankItem(properties, 4), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BaubleBlankItem> APPRENTICE_RING_FIRE = RegistryHelper.registerItem(ITEMS, "apprentice_ring_fire", properties -> new BaubleBlankItem(properties, 5), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BaubleBlankItem> APPRENTICE_RING_WATER = RegistryHelper.registerItem(ITEMS, "apprentice_ring_water", properties -> new BaubleBlankItem(properties, 6), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BaubleBlankItem> APPRENTICE_RING_ORDER = RegistryHelper.registerItem(ITEMS, "apprentice_ring_order", properties -> new BaubleBlankItem(properties, 7), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BaubleBlankItem> APPRENTICE_RING_ENTROPY = RegistryHelper.registerItem(ITEMS, "apprentice_ring_entropy", properties -> new BaubleBlankItem(properties, 8), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<RunicAmuletItem> RUNIC_SHIELDING_AMULET = RegistryHelper.registerItem(ITEMS, "runic_shielding_amulet", properties -> new RunicAmuletItem(properties, 0), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<RunicAmuletItem> EMERGENCY_SHIELDING_AMULET = RegistryHelper.registerItem(ITEMS, "emergency_shielding_amulet", properties -> new RunicAmuletItem(properties, 1), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<RunicRingItem> PROTECTION_RING = RegistryHelper.registerItem(ITEMS, "protection_ring", properties -> new RunicRingItem(properties, 0), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<RunicRingItem> RUNIC_SHIELDING_RING = RegistryHelper.registerItem(ITEMS, "runic_shielding_ring", properties -> new RunicRingItem(properties, 1), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<RunicRingItem> CHARGED_SHIELDING_RING = RegistryHelper.registerItem(ITEMS, "charged_shielding_ring", properties -> new RunicRingItem(properties, 2), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<RunicRingItem> REVITALIZING_SHIELDING_RING = RegistryHelper.registerItem(ITEMS, "revitalizing_shielding_ring", properties -> new RunicRingItem(properties, 3), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<RunicGirdleItem> RUNIC_SHIELDING_GIRDLE = RegistryHelper.registerItem(ITEMS, "runic_shielding_girdle", properties -> new RunicGirdleItem(properties, 0), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<RunicGirdleItem> KINETIC_SHIELDING_GIRDLE = RegistryHelper.registerItem(ITEMS, "kinetic_shielding_girdle", properties -> new RunicGirdleItem(properties, 1), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<VisAmuletItem> VIS_STONE = RegistryHelper.registerItem(ITEMS, "vis_stone", properties -> new VisAmuletItem(properties, true), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<VisAmuletItem> VIS_AMULET = RegistryHelper.registerItem(ITEMS, "vis_amulet", properties -> new VisAmuletItem(properties, false), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<HoverGirdleItem> THAUMOSTATIC_GIRDLE = RegistryHelper.registerItem(ITEMS, "thaumostatic_girdle", HoverGirdleItem::new, properties -> properties.stacksTo(1).rarity(Rarity.RARE));


    public static final DeferredItem<ArcaneKeyItem> IRON_ARCANE_KEY = RegistryHelper.registerItem(ITEMS, "iron_arcane_key", properties -> new ArcaneKeyItem(properties, OwnedBlockEntity.IRON), properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<ArcaneKeyItem> GOLD_ARCANE_KEY = RegistryHelper.registerItem(ITEMS, "gold_arcane_key", properties -> new ArcaneKeyItem(properties, OwnedBlockEntity.GOLD), properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<thaumcraft.device.HandMirrorItem> HAND_MIRROR = RegistryHelper.registerItem(ITEMS, "hand_mirror", thaumcraft.device.HandMirrorItem::new, properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<thaumcraft.device.ResonatorItem> ESSENTIA_RESONATOR = RegistryHelper.registerItem(ITEMS, "essentia_resonator", thaumcraft.device.ResonatorItem::new, properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> SINISTER_LODESTONE = RegistryHelper.registerItem(ITEMS, "sinister_lodestone", Item::new, properties -> properties.stacksTo(1).rarity(Rarity.RARE));


    private ModItems() {
    }

    private static DeferredItem<Item> nugget(String name) {
        return RegistryHelper.registerSimpleItem(ITEMS, name, properties -> Registration.food(properties, 1, 0.3F, false, 0.5F));
    }

    private static <T extends Mob> DeferredItem<SpawnEggItem> spawnEgg(
        String name,
        DeferredHolder<EntityType<?>, EntityType<T>> entityType,
        int backgroundColor,
        int highlightColor
    ) {
        Supplier<? extends EntityType<? extends Mob>> type = entityType::get;
        return RegistryHelper.registerItem(ITEMS, name, properties -> new DeferredSpawnEggItem(type, backgroundColor, highlightColor, properties));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
