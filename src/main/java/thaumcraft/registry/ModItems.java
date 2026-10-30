package thaumcraft.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
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

    public static final DeferredItem<thaumcraft.item.golem.TravelingTrunkItem> TRAVELING_TRUNK = ITEMS.registerItem("traveling_trunk", thaumcraft.item.golem.TravelingTrunkItem::new);
    private static final java.util.List<DeferredItem<? extends Item>> GOLEM_ITEMS = registerGolemItems();

    private static java.util.List<DeferredItem<? extends Item>> registerGolemItems() {
        var result = new java.util.ArrayList<DeferredItem<? extends Item>>();
        for (var type : thaumcraft.entity.golem.GolemType.values()) result.add(ITEMS.registerItem(type.itemId(), properties -> new thaumcraft.item.golem.GolemPlacerItem(properties, type)));
        result.add(ITEMS.registerItem("golem_core_blank", properties -> new thaumcraft.item.golem.GolemPartItem(properties, thaumcraft.item.golem.GolemPartItem.Kind.CORE, -1)));
        for (int i = 0; i < thaumcraft.item.golem.GolemItems.CORES.length; i++) {
            int index = i;
            result.add(ITEMS.registerItem("golem_core_" + thaumcraft.item.golem.GolemItems.CORES[i], properties -> new thaumcraft.item.golem.GolemPartItem(properties, thaumcraft.item.golem.GolemPartItem.Kind.CORE, index)));
        }
        for (int i = 0; i < 6; i++) {
            int index = i;
            result.add(ITEMS.registerItem("golem_upgrade_" + thaumcraft.item.golem.GolemItems.UPGRADES[i], properties -> new thaumcraft.item.golem.GolemPartItem(properties, thaumcraft.item.golem.GolemPartItem.Kind.UPGRADE, index)));
        }
        for (int i = 0; i < 8; i++) {
            int index = i;
            result.add(ITEMS.registerItem(thaumcraft.item.golem.GolemItems.DECORATIONS[i], properties -> new thaumcraft.item.golem.GolemPartItem(properties, thaumcraft.item.golem.GolemPartItem.Kind.DECORATION, index)));
        }
        result.add(ITEMS.registerItem("golem_bell", thaumcraft.item.golem.GolemBellItem::new));
        return java.util.List.copyOf(result);
    }

    public static final DeferredItem<BucketItem> PURIFYING_FLUID_BUCKET = ITEMS.registerItem("purifying_fluid_bucket", properties -> new BucketItem(ModFluids.PURIFYING_FLUID.get(), properties.stacksTo(1)));
    public static final DeferredItem<BucketItem> LIQUID_DEATH_BUCKET = ITEMS.registerItem("liquid_death_bucket", properties -> new BucketItem(ModFluids.LIQUID_DEATH.get(), properties.stacksTo(1)));
    public static final DeferredItem<BathSaltsItem> BATH_SALTS = ITEMS.registerItem("bath_salts", BathSaltsItem::new);
    public static final DeferredItem<SanitizingSoapItem> SANITIZING_SOAP = ITEMS.registerItem("sanitizing_soap", SanitizingSoapItem::new);
    public static final DeferredItem<Item> SANITY_CHECKER = ITEMS.registerItem("sanity_checker", Item::new, properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BottledTaintItem> BOTTLED_TAINT = ITEMS.registerItem("bottled_taint", BottledTaintItem::new);

    public static final DeferredItem<Item> ALUMENTUM = ITEMS.registerItem("alumentum", properties -> new AlumentumItem(Registration.fuel(properties, 6400)));
    public static final DeferredItem<Item> NITOR = ITEMS.registerItem("nitor", properties -> new net.minecraft.world.item.BlockItem(ModBlocks.NITOR_LIGHT.get(), properties));
    public static final DeferredItem<Item> THAUMIUM_INGOT = ITEMS.registerSimpleItem("thaumium_ingot");
    public static final DeferredItem<Item> QUICKSILVER = ITEMS.registerSimpleItem("quicksilver");
    public static final DeferredItem<Item> MAGIC_TALLOW = ITEMS.registerSimpleItem("magic_tallow");
    public static final DeferredItem<Item> AMBER = ITEMS.registerSimpleItem("amber");
    public static final DeferredItem<Item> ENCHANTED_FABRIC = ITEMS.registerSimpleItem("enchanted_fabric");
    public static final DeferredItem<Item> VIS_FILTER = ITEMS.registerSimpleItem("vis_filter");
    public static final DeferredItem<Item> KNOWLEDGE_FRAGMENT = ITEMS.registerItem("knowledge_fragment", KnowledgeFragmentItem::new);
    public static final DeferredItem<Item> MIRRORED_GLASS = ITEMS.registerSimpleItem("mirrored_glass");
    public static final DeferredItem<Item> TAINTED_GOO = ITEMS.registerSimpleItem("tainted_goo");
    public static final DeferredItem<Item> TAINT_TENDRIL = ITEMS.registerSimpleItem("taint_tendril");
    public static final DeferredItem<Item> JAR_LABEL = ITEMS.registerItem("jar_label", properties -> new AspectContainerItem(properties, "", false));
    public static final DeferredItem<Item> SALIS_MUNDUS = ITEMS.registerSimpleItem("salis_mundus");
    public static final DeferredItem<Item> PRIMAL_CHARM = ITEMS.registerItem("primal_charm", PrimalCharmItem::new);
    public static final DeferredItem<Item> VOID_METAL_INGOT = ITEMS.registerSimpleItem("void_metal_ingot");
    public static final DeferredItem<Item> VOID_SEED = ITEMS.registerSimpleItem("void_seed");
    public static final DeferredItem<Item> GOLD_COIN = ITEMS.registerSimpleItem("gold_coin");
    public static final DeferredItem<thaumcraft.eldritch.EldritchObjectItem> CRIMSON_RITES = thaumcraft.registry.EldritchContent.CRIMSON_RITES;

    public static final DeferredItem<SpawnEggItem> BRAINY_ZOMBIE_SPAWN_EGG = spawnEgg("brainy_zombie_spawn_egg", ModEntities.BRAINY_ZOMBIE);
    public static final DeferredItem<SpawnEggItem> GIANT_BRAINY_ZOMBIE_SPAWN_EGG = spawnEgg("giant_brainy_zombie_spawn_egg", ModEntities.GIANT_BRAINY_ZOMBIE);
    public static final DeferredItem<SpawnEggItem> WISP_SPAWN_EGG = spawnEgg("wisp_spawn_egg", ModEntities.WISP);
    public static final DeferredItem<SpawnEggItem> FIRE_BAT_SPAWN_EGG = spawnEgg("fire_bat_spawn_egg", ModEntities.FIREBAT);
    public static final DeferredItem<SpawnEggItem> PECH_SPAWN_EGG = spawnEgg("pech_spawn_egg", ModEntities.PECH);
    public static final DeferredItem<SpawnEggItem> MIND_SPIDER_SPAWN_EGG = spawnEgg("mind_spider_spawn_egg", ModEntities.MIND_SPIDER);
    public static final DeferredItem<SpawnEggItem> ELDRITCH_GUARDIAN_SPAWN_EGG = spawnEgg("eldritch_guardian_spawn_egg", ModEntities.ELDRITCH_GUARDIAN);
    public static final DeferredItem<SpawnEggItem> CULTIST_KNIGHT_SPAWN_EGG = spawnEgg("cultist_knight_spawn_egg", ModEntities.CULTIST_KNIGHT);
    public static final DeferredItem<SpawnEggItem> CULTIST_CLERIC_SPAWN_EGG = spawnEgg("cultist_cleric_spawn_egg", ModEntities.CULTIST_CLERIC);
    public static final DeferredItem<SpawnEggItem> ELDRITCH_CRAB_SPAWN_EGG = spawnEgg("eldritch_crab_spawn_egg", ModEntities.ELDRITCH_CRAB);
    public static final DeferredItem<SpawnEggItem> ELDRITCH_WARDEN_SPAWN_EGG = spawnEgg("eldritch_warden_spawn_egg", EldritchEntities.ELDRITCH_WARDEN);
    public static final DeferredItem<SpawnEggItem> ELDRITCH_GOLEM_SPAWN_EGG = spawnEgg("eldritch_golem_spawn_egg", EldritchEntities.ELDRITCH_GOLEM);
    public static final DeferredItem<SpawnEggItem> CULTIST_LEADER_SPAWN_EGG = spawnEgg("cultist_leader_spawn_egg", EldritchEntities.CULTIST_LEADER);
    public static final DeferredItem<SpawnEggItem> CULTIST_PORTAL_SPAWN_EGG = spawnEgg("cultist_portal_spawn_egg", EldritchEntities.CULTIST_PORTAL);
    public static final DeferredItem<SpawnEggItem> TAINTACLE_GIANT_SPAWN_EGG = spawnEgg("taintacle_giant_spawn_egg", EldritchEntities.TAINTACLE_GIANT);
    public static final DeferredItem<SpawnEggItem> INHABITED_ZOMBIE_SPAWN_EGG = spawnEgg("inhabited_zombie_spawn_egg", ModEntities.INHABITED_ZOMBIE);
    public static final DeferredItem<SpawnEggItem> THAUMIC_SLIME_SPAWN_EGG = spawnEgg("thaumic_slime_spawn_egg", ModEntities.THAUMIC_SLIME);
    public static final DeferredItem<SpawnEggItem> TAINT_SPIDER_SPAWN_EGG = spawnEgg("taint_spider_spawn_egg", ModEntities.TAINT_SPIDER);
    public static final DeferredItem<SpawnEggItem> TAINTACLE_SPAWN_EGG = spawnEgg("taintacle_spawn_egg", ModEntities.TAINTACLE);
    public static final DeferredItem<SpawnEggItem> TAINTACLE_SMALL_SPAWN_EGG = spawnEgg("taintacle_small_spawn_egg", ModEntities.TAINTACLE_TINY);
    public static final DeferredItem<SpawnEggItem> TAINT_SPORE_SPAWN_EGG = spawnEgg("taint_spore_spawn_egg", ModEntities.TAINT_SPORE);
    public static final DeferredItem<SpawnEggItem> TAINT_SPORE_SWARMER_SPAWN_EGG = spawnEgg("taint_spore_swarmer_spawn_egg", ModEntities.TAINT_SPORE_SWARMER);
    public static final DeferredItem<SpawnEggItem> TAINT_SWARM_SPAWN_EGG = spawnEgg("taint_swarm_spawn_egg", ModEntities.TAINT_SWARM);
    public static final DeferredItem<SpawnEggItem> TAINT_CHICKEN_SPAWN_EGG = spawnEgg("taint_chicken_spawn_egg", ModEntities.TAINTED_CHICKEN);
    public static final DeferredItem<SpawnEggItem> TAINT_COW_SPAWN_EGG = spawnEgg("taint_cow_spawn_egg", ModEntities.TAINTED_COW);
    public static final DeferredItem<SpawnEggItem> TAINT_CREEPER_SPAWN_EGG = spawnEgg("taint_creeper_spawn_egg", ModEntities.TAINTED_CREEPER);
    public static final DeferredItem<SpawnEggItem> TAINT_PIG_SPAWN_EGG = spawnEgg("taint_pig_spawn_egg", ModEntities.TAINTED_PIG);
    public static final DeferredItem<SpawnEggItem> TAINT_SHEEP_SPAWN_EGG = spawnEgg("taint_sheep_spawn_egg", ModEntities.TAINTED_SHEEP);
    public static final DeferredItem<SpawnEggItem> TAINT_VILLAGER_SPAWN_EGG = spawnEgg("taint_villager_spawn_egg", ModEntities.TAINTED_VILLAGER);

    public static final DeferredItem<Item> AIR_SHARD = ITEMS.registerSimpleItem("air_shard");
    public static final DeferredItem<Item> FIRE_SHARD = ITEMS.registerSimpleItem("fire_shard");
    public static final DeferredItem<Item> WATER_SHARD = ITEMS.registerSimpleItem("water_shard");
    public static final DeferredItem<Item> EARTH_SHARD = ITEMS.registerSimpleItem("earth_shard");
    public static final DeferredItem<Item> ORDER_SHARD = ITEMS.registerSimpleItem("order_shard");
    public static final DeferredItem<Item> ENTROPY_SHARD = ITEMS.registerSimpleItem("entropy_shard");
    public static final DeferredItem<Item> BALANCED_SHARD = ITEMS.registerSimpleItem("balanced_shard");

    public static final DeferredItem<Item> COPPER_NUGGET = ITEMS.registerSimpleItem("copper_nugget");
    public static final DeferredItem<Item> TIN_NUGGET = ITEMS.registerSimpleItem("tin_nugget");
    public static final DeferredItem<Item> SILVER_NUGGET = ITEMS.registerSimpleItem("silver_nugget");
    public static final DeferredItem<Item> LEAD_NUGGET = ITEMS.registerSimpleItem("lead_nugget");
    public static final DeferredItem<Item> QUICKSILVER_DROP = ITEMS.registerSimpleItem("quicksilver_drop");
    public static final DeferredItem<Item> THAUMIUM_NUGGET = ITEMS.registerSimpleItem("thaumium_nugget");
    public static final DeferredItem<Item> VOID_METAL_NUGGET = ITEMS.registerSimpleItem("void_metal_nugget");
    public static final DeferredItem<Item> NATIVE_IRON_CLUSTER = ITEMS.registerSimpleItem("native_iron_cluster");
    public static final DeferredItem<Item> NATIVE_COPPER_CLUSTER = ITEMS.registerSimpleItem("native_copper_cluster");
    public static final DeferredItem<Item> NATIVE_TIN_CLUSTER = ITEMS.registerSimpleItem("native_tin_cluster");
    public static final DeferredItem<Item> NATIVE_SILVER_CLUSTER = ITEMS.registerSimpleItem("native_silver_cluster");
    public static final DeferredItem<Item> NATIVE_LEAD_CLUSTER = ITEMS.registerSimpleItem("native_lead_cluster");
    public static final DeferredItem<Item> NATIVE_CINNABAR_CLUSTER = ITEMS.registerSimpleItem("native_cinnabar_cluster");
    public static final DeferredItem<Item> NATIVE_GOLD_CLUSTER = ITEMS.registerSimpleItem("native_gold_cluster");

    public static final DeferredItem<Item> CHICKEN_NUGGET = nugget("chicken_nugget");
    public static final DeferredItem<Item> BEEF_NUGGET = nugget("beef_nugget");
    public static final DeferredItem<Item> PORK_NUGGET = nugget("pork_nugget");
    public static final DeferredItem<Item> FISH_NUGGET = nugget("fish_nugget");
    public static final DeferredItem<Item> TRIPLE_MEAT_TREAT = ITEMS.registerSimpleItem(
        "triple_meat_treat",
        properties -> Registration.food(properties, 6, 0.8F, true, 1.6F, new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 0.66F)
    );
    public static final DeferredItem<Item> ZOMBIE_BRAIN = ITEMS.registerItem(
        "zombie_brain",
        ZombieBrainItem::new,
        properties -> Registration.food(properties, 4, 0.2F, false, 1.6F, new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8F)
    );

    public static final DeferredItem<Item> GLASS_PHIAL = ITEMS.registerItem("glass_phial", properties -> new PhialItem(false, properties));
    public static final DeferredItem<Item> ESSENTIA_PHIAL = ITEMS.registerItem("essentia_phial", properties -> new PhialItem(true, properties));
    public static final DeferredItem<Item> WISP_ESSENCE = ITEMS.registerItem("wisp_essence", properties -> new AspectContainerItem(properties, " x", true));
    public static final DeferredItem<Item> CRYSTALLIZED_ESSENCE = ITEMS.registerItem("crystallized_essence", CrystalEssenceItem::new);

    public static final DeferredItem<Item> COMMON_LOOT_BAG = ITEMS.registerItem("common_loot_bag", LootBagItem::new);
    public static final DeferredItem<Item> UNCOMMON_LOOT_BAG = ITEMS.registerItem("uncommon_loot_bag", LootBagItem::new, properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> RARE_LOOT_BAG = ITEMS.registerItem("rare_loot_bag", LootBagItem::new, properties -> properties.rarity(Rarity.RARE));

    public static final DeferredItem<WandItem> WAND = ITEMS.registerItem("wand", WandItem::new, WandItem::properties);
    public static final DeferredItem<Item> IRON_WAND_CAP = ITEMS.registerSimpleItem("iron_wand_cap");
    public static final DeferredItem<Item> GOLD_WAND_CAP = ITEMS.registerSimpleItem("gold_wand_cap");
    public static final DeferredItem<Item> THAUMIUM_WAND_CAP = ITEMS.registerSimpleItem("thaumium_wand_cap");
    public static final DeferredItem<Item> COPPER_WAND_CAP = ITEMS.registerSimpleItem("copper_wand_cap");
    public static final DeferredItem<Item> SILVER_WAND_CAP = ITEMS.registerSimpleItem("silver_wand_cap");
    public static final DeferredItem<Item> INERT_SILVER_WAND_CAP = ITEMS.registerSimpleItem("inert_silver_wand_cap");
    public static final DeferredItem<Item> INERT_THAUMIUM_WAND_CAP = ITEMS.registerSimpleItem("inert_thaumium_wand_cap");
    public static final DeferredItem<Item> VOID_WAND_CAP = ITEMS.registerSimpleItem("void_wand_cap");
    public static final DeferredItem<Item> INERT_VOID_WAND_CAP = ITEMS.registerSimpleItem("inert_void_wand_cap");
    public static final DeferredItem<Item> GREATWOOD_ROD = ITEMS.registerSimpleItem("greatwood_rod");
    public static final DeferredItem<Item> OBSIDIAN_ROD = ITEMS.registerSimpleItem("obsidian_rod");
    public static final DeferredItem<Item> SILVERWOOD_ROD = ITEMS.registerSimpleItem("silverwood_rod");
    public static final DeferredItem<Item> ICY_ROD = ITEMS.registerSimpleItem("icy_rod");
    public static final DeferredItem<Item> QUARTZ_ROD = ITEMS.registerSimpleItem("quartz_rod");
    public static final DeferredItem<Item> REED_ROD = ITEMS.registerSimpleItem("reed_rod");
    public static final DeferredItem<Item> BLAZING_ROD = ITEMS.registerSimpleItem("blazing_rod");
    public static final DeferredItem<Item> BONE_ROD = ITEMS.registerSimpleItem("bone_rod");
    public static final DeferredItem<Item> GREATWOOD_STAFF_CORE = ITEMS.registerSimpleItem("greatwood_staff_core");
    public static final DeferredItem<Item> OBSIDIAN_STAFF_CORE = ITEMS.registerSimpleItem("obsidian_staff_core");
    public static final DeferredItem<Item> SILVERWOOD_STAFF_CORE = ITEMS.registerSimpleItem("silverwood_staff_core");
    public static final DeferredItem<Item> ICY_STAFF_CORE = ITEMS.registerSimpleItem("icy_staff_core");
    public static final DeferredItem<Item> QUARTZ_STAFF_CORE = ITEMS.registerSimpleItem("quartz_staff_core");
    public static final DeferredItem<Item> REED_STAFF_CORE = ITEMS.registerSimpleItem("reed_staff_core");
    public static final DeferredItem<Item> BLAZING_STAFF_CORE = ITEMS.registerSimpleItem("blazing_staff_core");
    public static final DeferredItem<Item> BONE_STAFF_CORE = ITEMS.registerSimpleItem("bone_staff_core");
    public static final DeferredItem<Item> PRIMAL_STAFF_CORE = ITEMS.registerSimpleItem("primal_staff_core");
    public static final DeferredItem<ThaumometerItem> THAUMOMETER = ITEMS.registerItem("thaumometer", ThaumometerItem::new, ThaumometerItem::properties);
    public static final DeferredItem<GogglesItem> GOGGLES_OF_REVEALING = ITEMS.registerItem("goggles_of_revealing", GogglesItem::new, GogglesItem::properties);
    public static final DeferredItem<ScribingToolsItem> SCRIBING_TOOLS = ITEMS.registerItem("scribing_tools", ScribingToolsItem::new, ScribingToolsItem::properties);
    public static final DeferredItem<ResearchNotesItem> RESEARCH_NOTES = ITEMS.registerItem("research_notes", properties -> new ResearchNotesItem(properties, false), properties -> properties.rarity(Rarity.RARE));
    public static final DeferredItem<ResearchNotesItem> DISCOVERY = ITEMS.registerItem("discovery", properties -> new ResearchNotesItem(properties, true), properties -> properties.rarity(Rarity.EPIC).stacksTo(64));
    public static final DeferredItem<ThaumonomiconItem> THAUMONOMICON = ITEMS.registerItem("thaumonomicon", properties -> new ThaumonomiconItem(properties, false), ThaumonomiconItem::properties);
    public static final DeferredItem<ThaumonomiconItem> THAUMONOMICON_CHEAT = ITEMS.registerItem("thaumonomicon_cheat", properties -> new ThaumonomiconItem(properties, true), ThaumonomiconItem::properties);

    public static final DeferredItem<FocusPouchItem> FOCUS_POUCH = ITEMS.registerItem("focus_pouch", FocusPouchItem::new, properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<FireFocus> FOCUS_FIRE = ITEMS.registerItem("focus_fire", FireFocus::new, FocusItem::properties);
    public static final DeferredItem<ShockFocus> FOCUS_SHOCK = ITEMS.registerItem("focus_shock", ShockFocus::new, FocusItem::properties);
    public static final DeferredItem<HellbatFocus> FOCUS_NINE_HELLS = ITEMS.registerItem("focus_nine_hells", HellbatFocus::new, FocusItem::properties);
    public static final DeferredItem<FrostFocus> FOCUS_FROST = ITEMS.registerItem("focus_frost", FrostFocus::new, FocusItem::properties);
    public static final DeferredItem<TradeFocus> FOCUS_EQUAL_TRADE = ITEMS.registerItem("focus_equal_trade", TradeFocus::new, FocusItem::properties);
    public static final DeferredItem<ExcavationFocus> FOCUS_EXCAVATION = ITEMS.registerItem("focus_excavation", ExcavationFocus::new, FocusItem::properties);
    public static final DeferredItem<PortableHoleFocus> FOCUS_PORTABLE_HOLE = ITEMS.registerItem("focus_portable_hole", PortableHoleFocus::new, FocusItem::properties);
    public static final DeferredItem<PechFocus> FOCUS_PECH = ITEMS.registerItem("focus_pech", PechFocus::new, FocusItem::properties);
    public static final DeferredItem<WardingFocus> FOCUS_WARDING = ITEMS.registerItem("focus_warding", WardingFocus::new, FocusItem::properties);
    public static final DeferredItem<PrimalFocus> FOCUS_PRIMAL = ITEMS.registerItem("focus_primal", PrimalFocus::new, FocusItem::properties);

    public static final DeferredItem<ThaumiumSwordItem> THAUMIUM_SWORD = ITEMS.registerItem("thaumium_sword", ThaumiumSwordItem::new, properties -> Registration.sword(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_TOOL, 4.0F, -2.4F));
    public static final DeferredItem<ThaumiumPickaxeItem> THAUMIUM_PICKAXE = ITEMS.registerItem("thaumium_pickaxe", ThaumiumPickaxeItem::new, properties -> Registration.pickaxe(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_TOOL, 2.0F, -2.8F));
    public static final DeferredItem<ThaumiumAxeItem> THAUMIUM_AXE = ITEMS.registerItem("thaumium_axe", ThaumiumAxeItem::new, properties -> Registration.axe(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_TOOL, 3.0F, -3.0F));
    public static final DeferredItem<ThaumiumShovelItem> THAUMIUM_SHOVEL = ITEMS.registerItem("thaumium_shovel", ThaumiumShovelItem::new, properties -> Registration.shovel(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_TOOL, 1.0F, -3.0F));
    public static final DeferredItem<ThaumiumHoeItem> THAUMIUM_HOE = ITEMS.registerItem("thaumium_hoe", ThaumiumHoeItem::new, properties -> Registration.hoe(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_TOOL, -2.0F, -1.0F).enchantable(5));
    public static final DeferredItem<VoidSwordItem> VOID_SWORD = ITEMS.registerItem("void_sword", VoidSwordItem::new, properties -> Registration.sword(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_TOOL, 4.0F, -2.4F));
    public static final DeferredItem<VoidPickaxeItem> VOID_PICKAXE = ITEMS.registerItem("void_pickaxe", VoidPickaxeItem::new, properties -> Registration.pickaxe(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_TOOL, 2.0F, -2.8F));
    public static final DeferredItem<VoidAxeItem> VOID_AXE = ITEMS.registerItem("void_axe", VoidAxeItem::new, properties -> Registration.axe(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_TOOL, 3.0F, -3.0F));
    public static final DeferredItem<VoidShovelItem> VOID_SHOVEL = ITEMS.registerItem("void_shovel", VoidShovelItem::new, properties -> Registration.shovel(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_TOOL, 1.0F, -3.0F));
    public static final DeferredItem<VoidHoeItem> VOID_HOE = ITEMS.registerItem("void_hoe", VoidHoeItem::new, properties -> Registration.hoe(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_TOOL, -3.0F, -1.0F).enchantable(5));
    public static final DeferredItem<ElementalSwordItem> ELEMENTAL_SWORD = ITEMS.registerItem("elemental_sword", ElementalSwordItem::new, properties -> Registration.sword(properties.rarity(Rarity.RARE), TcMaterials.ELEMENTAL_TOOL, 4.0F, -2.4F));
    public static final DeferredItem<ElementalPickaxeItem> ELEMENTAL_PICKAXE = ITEMS.registerItem("elemental_pickaxe", ElementalPickaxeItem::new, properties -> Registration.pickaxe(properties.rarity(Rarity.RARE), TcMaterials.ELEMENTAL_TOOL, 2.0F, -2.8F));
    public static final DeferredItem<ElementalAxeItem> ELEMENTAL_AXE = ITEMS.registerItem("elemental_axe", ElementalAxeItem::new, properties -> Registration.axe(properties.rarity(Rarity.RARE), TcMaterials.ELEMENTAL_TOOL, 3.0F, -3.0F));
    public static final DeferredItem<ElementalShovelItem> ELEMENTAL_SHOVEL = ITEMS.registerItem("elemental_shovel", ElementalShovelItem::new, properties -> Registration.shovel(properties.rarity(Rarity.RARE), TcMaterials.ELEMENTAL_TOOL, 1.0F, -3.0F));
    public static final DeferredItem<ElementalHoeItem> ELEMENTAL_HOE = ITEMS.registerItem("elemental_hoe", ElementalHoeItem::new, properties -> Registration.hoe(properties.rarity(Rarity.RARE), TcMaterials.ELEMENTAL_TOOL, -3.0F, -1.0F).enchantable(5));
    public static final DeferredItem<CrimsonBladeItem> CRIMSON_BLADE = ITEMS.registerItem("crimson_blade", CrimsonBladeItem::new, properties -> Registration.sword(properties.rarity(Rarity.RARE), TcMaterials.CRIMSON_VOID_TOOL, 4.0F, -2.4F));
    public static final DeferredItem<PrimalCrusherItem> PRIMAL_CRUSHER = ITEMS.registerItem("primal_crusher", PrimalCrusherItem::new, properties -> Registration.pickaxeAndShovel(properties.rarity(Rarity.EPIC), TcMaterials.PRIMAL_VOID_TOOL, 3.5F, -2.8F));
    public static final DeferredItem<BoneBowItem> BONE_BOW = ITEMS.registerItem("bone_bow", BoneBowItem::new, properties -> properties.durability(512).rarity(Rarity.UNCOMMON).enchantable(3));
    public static final DeferredItem<PrimalArrowItem> AIR_ARROW = ITEMS.registerItem("air_arrow", properties -> new PrimalArrowItem(properties, 0));
    public static final DeferredItem<PrimalArrowItem> FIRE_ARROW = ITEMS.registerItem("fire_arrow", properties -> new PrimalArrowItem(properties, 1));
    public static final DeferredItem<PrimalArrowItem> WATER_ARROW = ITEMS.registerItem("water_arrow", properties -> new PrimalArrowItem(properties, 2));
    public static final DeferredItem<PrimalArrowItem> EARTH_ARROW = ITEMS.registerItem("earth_arrow", properties -> new PrimalArrowItem(properties, 3));
    public static final DeferredItem<PrimalArrowItem> ORDER_ARROW = ITEMS.registerItem("order_arrow", properties -> new PrimalArrowItem(properties, 4));
    public static final DeferredItem<PrimalArrowItem> ENTROPY_ARROW = ITEMS.registerItem("entropy_arrow", properties -> new PrimalArrowItem(properties, 5));

    public static final DeferredItem<ThaumiumArmorItem> THAUMIUM_HELMET = ITEMS.registerItem("thaumium_helmet", ThaumiumArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_ARMOR, ArmorSlot.HELMET));
    public static final DeferredItem<ThaumiumArmorItem> THAUMIUM_CHESTPLATE = ITEMS.registerItem("thaumium_chestplate", ThaumiumArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_ARMOR, ArmorSlot.CHESTPLATE));
    public static final DeferredItem<ThaumiumArmorItem> THAUMIUM_LEGGINGS = ITEMS.registerItem("thaumium_leggings", ThaumiumArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_ARMOR, ArmorSlot.LEGGINGS));
    public static final DeferredItem<ThaumiumArmorItem> THAUMIUM_BOOTS = ITEMS.registerItem("thaumium_boots", ThaumiumArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.THAUMIUM_ARMOR, ArmorSlot.BOOTS));
    public static final DeferredItem<RobeArmorItem> ROBE_CHESTPLATE = ITEMS.registerItem("robe_chestplate", RobeArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.ROBE_ARMOR, ArmorSlot.CHESTPLATE));
    public static final DeferredItem<RobeArmorItem> ROBE_LEGGINGS = ITEMS.registerItem("robe_leggings", RobeArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.ROBE_ARMOR, ArmorSlot.LEGGINGS));
    public static final DeferredItem<RobeArmorItem> ROBE_BOOTS = ITEMS.registerItem("robe_boots", RobeArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.ROBE_ARMOR, ArmorSlot.BOOTS));
    public static final DeferredItem<TravellerBootsItem> BOOTS_TRAVELLER = ITEMS.registerItem("boots_traveller", TravellerBootsItem::new, properties -> Registration.armor(properties.rarity(Rarity.RARE), TcMaterials.TRAVELLER_ARMOR, ArmorSlot.BOOTS).durability(350));
    public static final DeferredItem<HoverHarnessItem> HOVER_HARNESS = ITEMS.registerItem("hover_harness", HoverHarnessItem::new, properties -> Registration.armor(properties.rarity(Rarity.EPIC), TcMaterials.HARNESS_ARMOR, ArmorSlot.CHESTPLATE).durability(400));
    public static final DeferredItem<FortressArmorItem> FORTRESS_HELMET = ITEMS.registerItem("fortress_helmet", FortressArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.RARE), TcMaterials.FORTRESS_ARMOR, ArmorSlot.HELMET));
    public static final DeferredItem<FortressArmorItem> FORTRESS_CHESTPLATE = ITEMS.registerItem("fortress_chestplate", FortressArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.RARE), TcMaterials.FORTRESS_ARMOR, ArmorSlot.CHESTPLATE));
    public static final DeferredItem<FortressArmorItem> FORTRESS_LEGGINGS = ITEMS.registerItem("fortress_leggings", FortressArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.RARE), TcMaterials.FORTRESS_ARMOR, ArmorSlot.LEGGINGS));
    public static final DeferredItem<VoidArmorItem> VOID_HELMET = ITEMS.registerItem("void_helmet", VoidArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_ARMOR, ArmorSlot.HELMET));
    public static final DeferredItem<VoidArmorItem> VOID_CHESTPLATE = ITEMS.registerItem("void_chestplate", VoidArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_ARMOR, ArmorSlot.CHESTPLATE));
    public static final DeferredItem<VoidArmorItem> VOID_LEGGINGS = ITEMS.registerItem("void_leggings", VoidArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_ARMOR, ArmorSlot.LEGGINGS));
    public static final DeferredItem<VoidArmorItem> VOID_BOOTS = ITEMS.registerItem("void_boots", VoidArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.VOID_ARMOR, ArmorSlot.BOOTS));
    public static final DeferredItem<VoidRobeArmorItem> VOID_ROBE_HELMET = ITEMS.registerItem("void_robe_helmet", VoidRobeArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.EPIC), TcMaterials.VOID_ROBE_ARMOR, ArmorSlot.HELMET));
    public static final DeferredItem<VoidRobeArmorItem> VOID_ROBE_CHESTPLATE = ITEMS.registerItem("void_robe_chestplate", VoidRobeArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.EPIC), TcMaterials.VOID_ROBE_ARMOR, ArmorSlot.CHESTPLATE));
    public static final DeferredItem<VoidRobeArmorItem> VOID_ROBE_LEGGINGS = ITEMS.registerItem("void_robe_leggings", VoidRobeArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.EPIC), TcMaterials.VOID_ROBE_ARMOR, ArmorSlot.LEGGINGS));
    public static final DeferredItem<CultistRobeArmorItem> CRIMSON_CULT_HOOD = ITEMS.registerItem("crimson_cult_hood", CultistRobeArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.CULTIST_ROBE_ARMOR, ArmorSlot.HELMET));
    public static final DeferredItem<CultistRobeArmorItem> CRIMSON_CULT_ROBE = ITEMS.registerItem("crimson_cult_robe", CultistRobeArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.CULTIST_ROBE_ARMOR, ArmorSlot.CHESTPLATE));
    public static final DeferredItem<CultistRobeArmorItem> CRIMSON_CULT_LEGGINGS = ITEMS.registerItem("crimson_cult_leggings", CultistRobeArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.CULTIST_ROBE_ARMOR, ArmorSlot.LEGGINGS));
    public static final DeferredItem<CultistBootsItem> CRIMSON_CULT_BOOTS = ITEMS.registerItem("crimson_cult_boots", CultistBootsItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.CULTIST_BOOTS_ARMOR, ArmorSlot.BOOTS));
    public static final DeferredItem<CultistPlateArmorItem> CRIMSON_CULT_HELM = ITEMS.registerItem("crimson_cult_helm", CultistPlateArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.CULTIST_PLATE_ARMOR, ArmorSlot.HELMET));
    public static final DeferredItem<CultistPlateArmorItem> CRIMSON_CULT_CHESTPLATE = ITEMS.registerItem("crimson_cult_chestplate", CultistPlateArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.CULTIST_PLATE_ARMOR, ArmorSlot.CHESTPLATE));
    public static final DeferredItem<CultistPlateArmorItem> CRIMSON_CULT_GREAVES = ITEMS.registerItem("crimson_cult_greaves", CultistPlateArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.UNCOMMON), TcMaterials.CULTIST_PLATE_ARMOR, ArmorSlot.LEGGINGS));
    public static final DeferredItem<CultistLeaderArmorItem> CRIMSON_PRAETOR_HELM = ITEMS.registerItem("crimson_praetor_helm", CultistLeaderArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.RARE), TcMaterials.CULTIST_LEADER_ARMOR, ArmorSlot.HELMET));
    public static final DeferredItem<CultistLeaderArmorItem> CRIMSON_PRAETOR_CHESTPLATE = ITEMS.registerItem("crimson_praetor_chestplate", CultistLeaderArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.RARE), TcMaterials.CULTIST_LEADER_ARMOR, ArmorSlot.CHESTPLATE));
    public static final DeferredItem<CultistLeaderArmorItem> CRIMSON_PRAETOR_GREAVES = ITEMS.registerItem("crimson_praetor_greaves", CultistLeaderArmorItem::new, properties -> Registration.armor(properties.rarity(Rarity.RARE), TcMaterials.CULTIST_LEADER_ARMOR, ArmorSlot.LEGGINGS));

    public static final DeferredItem<BaubleBlankItem> MUNDANE_AMULET = ITEMS.registerItem("mundane_amulet", properties -> new BaubleBlankItem(properties, 0), properties -> properties.stacksTo(1).rarity(Rarity.COMMON));
    public static final DeferredItem<BaubleBlankItem> MUNDANE_RING = ITEMS.registerItem("mundane_ring", properties -> new BaubleBlankItem(properties, 1), properties -> properties.stacksTo(1).rarity(Rarity.COMMON));
    public static final DeferredItem<BaubleBlankItem> MUNDANE_BELT = ITEMS.registerItem("mundane_belt", properties -> new BaubleBlankItem(properties, 2), properties -> properties.stacksTo(1).rarity(Rarity.COMMON));
    public static final DeferredItem<BaubleBlankItem> APPRENTICE_RING_AIR = ITEMS.registerItem("apprentice_ring_air", properties -> new BaubleBlankItem(properties, 3), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BaubleBlankItem> APPRENTICE_RING_EARTH = ITEMS.registerItem("apprentice_ring_earth", properties -> new BaubleBlankItem(properties, 4), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BaubleBlankItem> APPRENTICE_RING_FIRE = ITEMS.registerItem("apprentice_ring_fire", properties -> new BaubleBlankItem(properties, 5), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BaubleBlankItem> APPRENTICE_RING_WATER = ITEMS.registerItem("apprentice_ring_water", properties -> new BaubleBlankItem(properties, 6), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BaubleBlankItem> APPRENTICE_RING_ORDER = ITEMS.registerItem("apprentice_ring_order", properties -> new BaubleBlankItem(properties, 7), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BaubleBlankItem> APPRENTICE_RING_ENTROPY = ITEMS.registerItem("apprentice_ring_entropy", properties -> new BaubleBlankItem(properties, 8), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<RunicAmuletItem> RUNIC_SHIELDING_AMULET = ITEMS.registerItem("runic_shielding_amulet", properties -> new RunicAmuletItem(properties, 0), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<RunicAmuletItem> EMERGENCY_SHIELDING_AMULET = ITEMS.registerItem("emergency_shielding_amulet", properties -> new RunicAmuletItem(properties, 1), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<RunicRingItem> PROTECTION_RING = ITEMS.registerItem("protection_ring", properties -> new RunicRingItem(properties, 0), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<RunicRingItem> RUNIC_SHIELDING_RING = ITEMS.registerItem("runic_shielding_ring", properties -> new RunicRingItem(properties, 1), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<RunicRingItem> CHARGED_SHIELDING_RING = ITEMS.registerItem("charged_shielding_ring", properties -> new RunicRingItem(properties, 2), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<RunicRingItem> REVITALIZING_SHIELDING_RING = ITEMS.registerItem("revitalizing_shielding_ring", properties -> new RunicRingItem(properties, 3), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<RunicGirdleItem> RUNIC_SHIELDING_GIRDLE = ITEMS.registerItem("runic_shielding_girdle", properties -> new RunicGirdleItem(properties, 0), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<RunicGirdleItem> KINETIC_SHIELDING_GIRDLE = ITEMS.registerItem("kinetic_shielding_girdle", properties -> new RunicGirdleItem(properties, 1), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<VisAmuletItem> VIS_STONE = ITEMS.registerItem("vis_stone", properties -> new VisAmuletItem(properties, true), properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<VisAmuletItem> VIS_AMULET = ITEMS.registerItem("vis_amulet", properties -> new VisAmuletItem(properties, false), properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<HoverGirdleItem> THAUMOSTATIC_GIRDLE = ITEMS.registerItem("thaumostatic_girdle", HoverGirdleItem::new, properties -> properties.stacksTo(1).rarity(Rarity.RARE));

    public static final DeferredItem<ArcaneKeyItem> IRON_ARCANE_KEY = ITEMS.registerItem("iron_arcane_key", properties -> new ArcaneKeyItem(properties, OwnedBlockEntity.IRON), properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<ArcaneKeyItem> GOLD_ARCANE_KEY = ITEMS.registerItem("gold_arcane_key", properties -> new ArcaneKeyItem(properties, OwnedBlockEntity.GOLD), properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<thaumcraft.device.HandMirrorItem> HAND_MIRROR = ITEMS.registerItem("hand_mirror", thaumcraft.device.HandMirrorItem::new, properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<thaumcraft.device.ResonatorItem> ESSENTIA_RESONATOR = ITEMS.registerItem("essentia_resonator", thaumcraft.device.ResonatorItem::new, properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> SINISTER_LODESTONE = ITEMS.registerItem("sinister_lodestone", Item::new, properties -> properties.stacksTo(1).rarity(Rarity.RARE));

    private ModItems() {
    }

    private static DeferredItem<Item> nugget(String name) {
        return ITEMS.registerSimpleItem(name, properties -> Registration.food(properties, 1, 0.3F, false, 0.5F));
    }

    private static DeferredItem<SpawnEggItem> spawnEgg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<?>> entityType) {
        return ITEMS.registerItem(name, SpawnEggItem::new, properties -> properties.spawnEgg(entityType.get()));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
