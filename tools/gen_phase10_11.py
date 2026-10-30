import json

NS = "thaumcraft"

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
]


def write_damage_types(ctx):
    definitions = {
        "taint": ["minecraft:bypasses_armor", "neoforge:is_magic"],
        "dissolve": ["minecraft:bypasses_armor"],
        "tentacle": [],
        "swarm": [],
    }
    for name, tags in definitions.items():
        ctx.write(
            ctx.DATA / NS / "damage_type" / f"{name}.json",
            {"exhaustion": 0.3, "message_id": f"{NS}.{name}", "scaling": "when_caused_by_living_non_player"},
        )
        for tag in tags:
            namespace, path = tag.split(":", 1)
            tag_path = ctx.DATA / namespace / "tags" / "damage_type" / f"{path}.json"
            current = json.loads(tag_path.read_text(encoding="utf-8")) if tag_path.exists() else {"replace": False, "values": []}
            values = current["values"]
            if f"{NS}:{name}" not in values:
                values.append(f"{NS}:{name}")
            current["values"] = sorted(values, key=lambda entry: entry if isinstance(entry, str) else entry["id"])
            ctx.write(tag_path, current)

    entity_tags = {
        "arthropod": ["eldritch_crab", "mind_spider", "taint_spider"],
        "undead": ["brainy_zombie", "giant_brainy_zombie", "inhabited_zombie", "eldritch_guardian"],
    }
    for name, entities in entity_tags.items():
        path = ctx.DATA / "minecraft" / "tags" / "entity_type" / f"{name}.json"
        current = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"replace": False, "values": []}
        values = current["values"]
        for entity in entities:
            identifier = f"{NS}:{entity}"
            if identifier not in values:
                values.append(identifier)
        current["values"] = sorted(values, key=lambda entry: entry if isinstance(entry, str) else entry["id"])
        ctx.write(path, current)

    ctx.write(
        ctx.DATA / NS / ctx.profile.loot_folder / "entities" / "inhabited_zombie.json",
        ctx.profile.loot_table({
            "type": "minecraft:entity",
            "pools": [],
            "random_sequence": f"{NS}:entities/inhabited_zombie",
        }),
    )


def write_spawn_egg(ctx, name, base_color, spot_color):
    ctx.generated_item(name, ["spawn_egg", "spawn_egg_overlay"], [ctx.tint(base_color), ctx.tint(spot_color)])


def write_spawn_eggs(ctx):
    for name, base_color, spot_color in SPAWN_EGGS:
        write_spawn_egg(ctx, name, base_color, spot_color)


def write_spawn_modifiers(ctx):
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


def block(ctx, name, texture):
    ctx.cube_all(name, texture)
    ctx.simple_state(name)
    ctx.block_item(name)


def silk_touch_drop(ctx, block, item=None, count=1):
    entries = [{"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": f"{NS}:{block}"}]
    if item is not None:
        drop = {"type": "minecraft:item", "name": item}
        if count > 1:
            drop["functions"] = [{"function": "minecraft:set_count", "count": count}]
        entries.append(drop)
    ctx.write_loot(block, [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": entries}]}])


def no_drop(ctx, name):
    ctx.write_loot(name, [])


def cross(ctx, name, texture):
    ctx.block_model(name, {"parent": "minecraft:block/cross", "textures": {"cross": ctx.ref(texture, "block")}})
    ctx.simple_state(name)
    ctx.generated_item(name, texture, texture_kind="block")
    ctx.self_drop(name)


def taint_fibres_model(ctx):
    ctx.block_model("taint_fibres_face", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": ctx.ref("taint_fibres", "block")},
    })
    for name, texture in [
        ("blank", "blank"),
        ("over_1", "taint_over_1"),
        ("over_2", "taint_over_2"),
        ("over_3", "taint_over_3"),
    ]:
        ctx.block_model(f"taint_fibres_{name}", {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": ctx.ref(texture, "block")},
        })
    for model, texture in [
        ("taint_fibres_grass", "taintgrass1"),
        ("taint_fibres_plant", "taintgrass2"),
        ("taint_fibres_stalk", "taint_spore_stalk_1"),
        ("taint_fibres_mature_stalk", "taint_spore_stalk_2"),
    ]:
        ctx.block_model(model, {
            "parent": "minecraft:block/cross",
            "textures": {"cross": ctx.ref(texture, "block")},
        })
    model = lambda name: {"model": f"{NS}:block/{name}"}
    overlays = [
        [model(f"taint_fibres_{name}") for name in ("blank", "over_1", "over_2", "over_3")]
        for _ in range(6)
    ]
    blockstate = {"variants": {"": {
        "type": f"{NS}:taint_fibres",
        "faces": [model("taint_fibres_face") for _ in range(6)],
        "overlays": overlays,
        "grass": model("taint_fibres_grass"),
        "plant": model("taint_fibres_plant"),
        "stalk": model("taint_fibres_stalk"),
        "mature_stalk": model("taint_fibres_mature_stalk"),
    }}}
    for name in ("fibrous_taint", "tainted_grass", "tainted_plant", "spore_stalk", "mature_spore_stalk"):
        ctx.write(ctx.ASSETS / "blockstates" / f"{name}.json", blockstate)


def fluid_item(ctx, item, texture):
    ctx.generated_item(item, texture)


def generate(ctx):
    write_damage_types(ctx)
    write_spawn_modifiers(ctx)
    write_spawn_eggs(ctx)

    for name, texture in [
        ("crusted_taint", "taint_crust"),
        ("tainted_soil", "taint_soil"),
        ("flesh_block", "fleshblock"),
    ]:
        block(ctx, name, texture)
    silk_touch_drop(ctx, "crusted_taint")
    silk_touch_drop(ctx, "tainted_soil", "minecraft:dirt")
    silk_touch_drop(ctx, "flesh_block", "minecraft:rotten_flesh", 9)

    for name, texture in [
        ("fibrous_taint", "taint_fibres"),
        ("tainted_grass", "taintgrass1"),
        ("tainted_plant", "taintgrass2"),
        ("spore_stalk", "taint_spore_stalk_1"),
        ("mature_spore_stalk", "taint_spore_stalk_2"),
    ]:
        cross(ctx, name, texture)
        no_drop(ctx, name)
    taint_fibres_model(ctx)

    for name, texture in [
        ("purifying_fluid_bucket", "bucket_pure"),
        ("liquid_death_bucket", "bucket_death"),
        ("bath_salts", "bath_salts"),
        ("sanitizing_soap", "soap"),
        ("sanity_checker", "sanitychecker"),
        ("bottled_taint", "bottle_taint"),
    ]:
        fluid_item(ctx, name, texture)

    fluid_item(ctx, "crimson_rites", "crimson_rites")

    for name in ["purifying_fluid", "liquid_death"]:
        ctx.block_model(name, {"parent": "minecraft:block/block", "textures": {"particle": ctx.ref("fluidpure" if name == "purifying_fluid" else "fluiddeath", "block")}})
        ctx.simple_state(name)
