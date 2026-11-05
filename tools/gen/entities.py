from .common import NS

SPAWN_EGGS = [
    ("brainy_zombie_spawn_egg", 0xFFC0FF, 0x008000),
    ("giant_brainy_zombie_spawn_egg", 0xFFC0FF, 0x004000),
    ("wisp_spawn_egg", 0xFFC0FF, 0xFFFFFF),
    ("fire_bat_spawn_egg", 0xFFC0FF, 0xC00000),
    ("pech_spawn_egg", 0xFFC0FF, 0x400040),
    ("mind_spider_spawn_egg", 0xAAAAAA, 0x404040),
    ("eldritch_guardian_spawn_egg", 0x222222, 0x404040),
    ("cultist_knight_spawn_egg", 0xFF5055, 0x000080),
    ("cultist_cleric_spawn_egg", 0xFF5055, 0x800000),
    ("eldritch_crab_spawn_egg", 0x555555, 0x550000),
    ("inhabited_zombie_spawn_egg", 0x557755, 0x550000),
    ("thaumic_slime_spawn_egg", 0xFFC0FF, 0xFF80FF),
    ("taint_spider_spawn_egg", 0xFFC0FF, 0x404040),
    ("taintacle_spawn_egg", 0xFFC0FF, 0x800080),
    ("taintacle_small_spawn_egg", 0xFFC0FF, 0x800090),
    ("taint_spore_spawn_egg", 0xFFC0FF, 0x800070),
    ("taint_spore_swarmer_spawn_egg", 0xFFC0FF, 0x800060),
    ("taint_swarm_spawn_egg", 0xFFC0FF, 0x800050),
    ("taint_chicken_spawn_egg", 0xFFC0FF, 0xC0C0C0),
    ("taint_cow_spawn_egg", 0xFFC0FF, 0x7E3A3B),
    ("taint_creeper_spawn_egg", 0xFFC0FF, 0x00FF00),
    ("taint_pig_spawn_egg", 0xFFC0FF, 0xEF99EF),
    ("taint_sheep_spawn_egg", 0xFFC0FF, 0x808080),
    ("taint_villager_spawn_egg", 0xFFC0FF, 0x00FFFF),
    ("eldritch_warden_spawn_egg", 0x552222, 0x404040),
    ("eldritch_golem_spawn_egg", 0x555555, 0x404040),
    ("cultist_leader_spawn_egg", 0xFF5055, 0x505050),
    ("cultist_portal_spawn_egg", 0xFF5055, 0xFF50FF),
    ("taintacle_giant_spawn_egg", 0xFFC0FF, 0x808080),
]
PROJECTILE_DAMAGE_TYPES = {
    "airarrow": ["minecraft:is_projectile", "minecraft:bypasses_armor", "minecraft:witch_resistant_to"],
    "firearrow": ["minecraft:is_projectile", "minecraft:is_fire"],
    "orderarrow": ["minecraft:is_projectile", "minecraft:bypasses_armor", "minecraft:witch_resistant_to"],
}
DAMAGE_TYPES = {
    "taint": ["minecraft:bypasses_armor", "neoforge:is_magic"],
    "dissolve": ["minecraft:bypasses_armor"],
    "tentacle": [],
    "swarm": [],
}
ENTITY_TAGS = {
    "arthropod": ["eldritch_crab", "mind_spider", "taint_spider"],
    "undead": ["brainy_zombie", "giant_brainy_zombie", "inhabited_zombie", "eldritch_guardian"],
}


def damage_types(ctx):
    tags = {}

    def add(name, content, names):
        ctx.write(ctx.DATA / NS / "damage_type" / f"{name}.json", content)
        for tag in names:
            tags.setdefault(tag, []).append(f"{NS}:{name}")

    if ctx.profile.enchantment_json:
        for name, names in PROJECTILE_DAMAGE_TYPES.items():
            content = {"exhaustion": 0.1, "message_id": f"{NS}.{name}", "scaling": "when_caused_by_living_non_player"}
            if "minecraft:is_fire" in names:
                content["effects"] = "burning"
            add(name, content, names)
    for name, names in DAMAGE_TYPES.items():
        add(name, {"exhaustion": 0.3, "message_id": f"{NS}.{name}", "scaling": "when_caused_by_living_non_player"}, names)
    for tag, values in tags.items():
        namespace, path = tag.split(":")
        ctx.write(ctx.DATA / namespace / "tags" / "damage_type" / f"{path}.json", {"replace": False, "values": sorted(values)})


def entity_tags(ctx):
    for name, entities in ENTITY_TAGS.items():
        ctx.write(
            ctx.DATA / "minecraft" / "tags" / "entity_type" / f"{name}.json",
            {"replace": False, "values": sorted(f"{NS}:{entity}" for entity in entities)},
        )


def loot(ctx):
    ctx.write(
        ctx.DATA / NS / ctx.profile.loot_folder / "entities" / "inhabited_zombie.json",
        ctx.profile.loot_table({
            "type": "minecraft:entity",
            "pools": [],
            "random_sequence": f"{NS}:entities/inhabited_zombie",
        }),
    )


def spawn_eggs(ctx):
    for name, base_color, spot_color in SPAWN_EGGS:
        ctx.generated_item(name, ["spawn_egg", "spawn_egg_overlay"], [ctx.tint(base_color), ctx.tint(spot_color)])


def spawn_modifiers(ctx):
    profile = ctx.profile
    brainy_biomes = [
        "minecraft:forest",
        "minecraft:flower_forest",
        "minecraft:birch_forest",
        "minecraft:old_growth_birch_forest",
        "minecraft:dark_forest",
        "minecraft:plains",
        "minecraft:sunflower_plains",
        "minecraft:taiga",
        "minecraft:snowy_taiga",
        "minecraft:old_growth_pine_taiga",
        "minecraft:old_growth_spruce_taiga",
        "minecraft:jungle",
        "minecraft:sparse_jungle",
        "minecraft:bamboo_jungle",
    ]
    ctx.write(
        ctx.DATA / NS / "tags" / "worldgen" / "biome" / "brainy_zombie_biomes.json",
        {"replace": False, "values": brainy_biomes},
    )
    modifiers = {
        "firebat": ("#minecraft:is_nether", 10, 1, 2),
        "wisp": ("#minecraft:is_nether", 5),
        "taintacle": (f"{NS}:tainted_land", 1),
    }
    for name, settings in modifiers.items():
        biomes, weight, *counts = settings
        ctx.write(
            ctx.DATA / NS / profile.biome_modifier_namespace / "biome_modifier" / f"{name}_spawns.json",
            profile.spawn_modifier(biomes, f"{NS}:{name}", weight, *(counts or [1, 1])),
        )
    ctx.write(
        ctx.DATA / NS / profile.biome_modifier_namespace / "biome_modifier" / "brainy_zombie_spawns.json",
        {"type": f"{NS}:monster_biome_spawns"},
    )
    for name, mob, biome, weight, minimum, maximum in [
        ("forest_wisp", "wisp", "magical_forest", 10, 1, 2),
        ("eerie_brainy", "brainy_zombie", "eerie", 32, 1, 1),
        ("eerie_giant", "giant_brainy_zombie", "eerie", 8, 1, 1),
        ("eerie_wisp", "wisp", "eerie", 3, 1, 1),
        ("eerie_guardian", "eldritch_guardian", "eerie", 1, 1, 1),
    ]:
        ctx.write(
            ctx.DATA / NS / profile.biome_modifier_namespace / "biome_modifier" / f"{name}_spawns.json",
            profile.spawn_modifier(f"{NS}:{biome}", f"{NS}:{mob}", weight, minimum, maximum),
        )
    ctx.write(
        ctx.DATA / NS / profile.biome_modifier_namespace / "biome_modifier" / "halloween_firebat_spawns.json",
        {"type": f"{NS}:halloween_firebat_spawns", "biomes": f"#{NS}:brainy_zombie_biomes"},
    )


def generate(ctx):
    damage_types(ctx)
    entity_tags(ctx)
    loot(ctx)
    spawn_eggs(ctx)
    spawn_modifiers(ctx)
