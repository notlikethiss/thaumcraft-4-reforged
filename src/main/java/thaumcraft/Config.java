package thaumcraft;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ALLOW_MIRRORS = BUILDER
        .translation("thaumcraft.configuration.allow_mirrors")
        .define("general.allow_mirrors", true);
    public static final ModConfigSpec.BooleanValue HARD_NODE = BUILDER
        .translation("thaumcraft.configuration.hard_mode_nodes")
        .define("general.hard_mode_nodes", true);
    public static final ModConfigSpec.BooleanValue WUSS = BUILDER
        .translation("thaumcraft.configuration.wuss_mode")
        .define("general.wuss_mode", false);
    public static final ModConfigSpec.BooleanValue WARDED_STONE = BUILDER
        .translation("thaumcraft.configuration.allow_warded_stone")
        .define("general.allow_warded_stone", true);
    public static final ModConfigSpec.IntValue GOLEM_DELAY = BUILDER
        .translation("thaumcraft.configuration.golem_delay")
        .defineInRange("general.golem_delay", 5, 1, 1200);
    public static final ModConfigSpec.IntValue NODE_RARITY = BUILDER
        .translation("thaumcraft.configuration.node_rarity")
        .defineInRange("general.node_rarity", 36, 1, 10000);
    public static final ModConfigSpec.IntValue SPECIAL_NODE_RARITY = BUILDER
        .translation("thaumcraft.configuration.special_node_rarity")
        .defineInRange("general.special_node_rarity", 18, 3, 10000);
    public static final ModConfigSpec.BooleanValue TAINT_FROM_FLUX = BUILDER
        .translation("thaumcraft.configuration.biome_taint_from_flux")
        .define("general.biome_taint_from_flux", true);
    public static final ModConfigSpec.IntValue TAINT_SPREAD_RATE = BUILDER
        .translation("thaumcraft.configuration.biome_taint_spread")
        .defineInRange("general.biome_taint_spread", 200, 0, 1000000);

    public static final ModConfigSpec.IntValue RESEARCH_DIFFICULTY = BUILDER
        .translation("thaumcraft.configuration.research_difficulty")
        .defineInRange("research.research_difficulty", 0, -1, 1);
    public static final ModConfigSpec.IntValue ASPECT_TOTAL_CAP = BUILDER
        .translation("thaumcraft.configuration.aspect_total_cap")
        .defineInRange("research.aspect_total_cap", 100, 1, 100000);

    public static final ModConfigSpec.BooleanValue GEN_AURA = BUILDER
        .translation("thaumcraft.configuration.generate_aura_nodes")
        .define("world_generation.generate_aura_nodes", true);
    public static final ModConfigSpec.BooleanValue GEN_STRUCTURE = BUILDER
        .translation("thaumcraft.configuration.generate_structures")
        .define("world_generation.generate_structures", true);
    public static final ModConfigSpec.BooleanValue GEN_CINNABAR = BUILDER
        .translation("thaumcraft.configuration.generate_cinnibar_ore")
        .define("world_generation.generate_cinnibar_ore", true);
    public static final ModConfigSpec.BooleanValue GEN_AMBER = BUILDER
        .translation("thaumcraft.configuration.generate_amber_ore")
        .define("world_generation.generate_amber_ore", true);
    public static final ModConfigSpec.BooleanValue GEN_INFUSED_STONE = BUILDER
        .translation("thaumcraft.configuration.generate_infused_stone")
        .define("world_generation.generate_infused_stone", true);
    public static final ModConfigSpec.BooleanValue GEN_TREES = BUILDER
        .translation("thaumcraft.configuration.generate_trees")
        .define("world_generation.generate_trees", true);

    public static final ModConfigSpec.BooleanValue SPAWN_ANGRY_ZOMBIE = BUILDER
        .translation("thaumcraft.configuration.spawn_angry_zombies")
        .define("monster_spawning.spawn_angry_zombies", true);
    public static final ModConfigSpec.BooleanValue SPAWN_FIRE_BAT = BUILDER
        .translation("thaumcraft.configuration.spawn_fire_bats")
        .define("monster_spawning.spawn_fire_bats", true);
    public static final ModConfigSpec.BooleanValue SPAWN_WISP = BUILDER
        .translation("thaumcraft.configuration.spawn_wisps")
        .define("monster_spawning.spawn_wisps", true);
    public static final ModConfigSpec.BooleanValue SPAWN_TAINTACLE = BUILDER
        .translation("thaumcraft.configuration.spawn_taintacles")
        .define("monster_spawning.spawn_taintacles", true);
    public static final ModConfigSpec.BooleanValue SPAWN_TAINT_SPORE = BUILDER
        .translation("thaumcraft.configuration.spawn_taint_spores")
        .define("monster_spawning.spawn_taint_spores", true);
    public static final ModConfigSpec.BooleanValue SPAWN_PECH = BUILDER
        .translation("thaumcraft.configuration.spawn_pechs")
        .define("monster_spawning.spawn_pechs", true);
    public static final ModConfigSpec.BooleanValue SPAWN_ELDER = BUILDER
        .translation("thaumcraft.configuration.spawn_eldercreatures")
        .define("monster_spawning.spawn_eldercreatures", true);
    public static final ModConfigSpec.BooleanValue CHAMPION_MOBS = BUILDER
        .translation("thaumcraft.configuration.champion_mobs")
        .define("monster_spawning.champion_mobs", true);

    public static final ModConfigSpec.IntValue SHIELD_RECHARGE = BUILDER
        .translation("thaumcraft.configuration.runic_recharge_speed")
        .defineInRange("runic_shielding.runic_recharge_speed", 2000, 500, 1000000);
    public static final ModConfigSpec.IntValue SHIELD_WAIT = BUILDER
        .translation("thaumcraft.configuration.runic_recharge_delay")
        .defineInRange("runic_shielding.runic_recharge_delay", 80, 0, 1000000);
    public static final ModConfigSpec.IntValue SHIELD_COST = BUILDER
        .translation("thaumcraft.configuration.runic_cost")
        .defineInRange("runic_shielding.runic_cost", 50, 0, 1000000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    private static boolean flag(ModConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() ? value.getAsBoolean() : value.getDefault();
    }

    private static int number(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.getAsInt() : value.getDefault();
    }

    public static boolean allowMirrors() {
        return flag(ALLOW_MIRRORS);
    }

    public static boolean hardNode() {
        return flag(HARD_NODE);
    }

    public static boolean wuss() {
        return flag(WUSS);
    }

    public static boolean wardedStone() {
        return flag(WARDED_STONE);
    }

    public static int golemDelay() {
        return number(GOLEM_DELAY);
    }

    public static int nodeRarity() {
        return number(NODE_RARITY);
    }

    public static int specialNodeRarity() {
        return number(SPECIAL_NODE_RARITY);
    }

    public static boolean taintFromFlux() {
        return flag(TAINT_FROM_FLUX);
    }

    public static int taintSpreadRate() {
        return number(TAINT_SPREAD_RATE);
    }

    public static int researchDifficulty() {
        return number(RESEARCH_DIFFICULTY);
    }

    public static int aspectTotalCap() {
        return number(ASPECT_TOTAL_CAP);
    }

    public static boolean genAura() {
        return flag(GEN_AURA);
    }

    public static boolean genStructure() {
        return flag(GEN_STRUCTURE);
    }

    public static boolean genCinnabar() {
        return flag(GEN_CINNABAR);
    }

    public static boolean genAmber() {
        return flag(GEN_AMBER);
    }

    public static boolean genInfusedStone() {
        return flag(GEN_INFUSED_STONE);
    }

    public static boolean genTrees() {
        return flag(GEN_TREES);
    }

    public static boolean spawnAngryZombie() {
        return flag(SPAWN_ANGRY_ZOMBIE);
    }

    public static boolean spawnFireBat() {
        return flag(SPAWN_FIRE_BAT);
    }

    public static boolean spawnWisp() {
        return flag(SPAWN_WISP);
    }

    public static boolean spawnTaintacle() {
        return flag(SPAWN_TAINTACLE);
    }

    public static boolean spawnTaintSpore() {
        return flag(SPAWN_TAINT_SPORE);
    }

    public static boolean spawnPech() {
        return flag(SPAWN_PECH);
    }

    public static boolean spawnElder() {
        return flag(SPAWN_ELDER);
    }

    public static boolean championMobs() {
        return flag(CHAMPION_MOBS);
    }

    public static int shieldRecharge() {
        return number(SHIELD_RECHARGE);
    }

    public static int shieldWait() {
        return number(SHIELD_WAIT);
    }

    public static int shieldCost() {
        return number(SHIELD_COST);
    }
}
