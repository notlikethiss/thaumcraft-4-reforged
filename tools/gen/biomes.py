import colorsys

from .common import NS

FLOWERS = [
    "poppy",
    "blue_orchid",
    "allium",
    "azure_bluet",
    "red_tulip",
    "orange_tulip",
    "white_tulip",
    "pink_tulip",
    "oxeye_daisy",
]

OFFSET = {
    "type": "minecraft:offset",
    "x": {"type": "minecraft:trapezoid", "max": 7, "min": -7, "plateau": 0},
    "y": {"type": "minecraft:trapezoid", "max": 3, "min": -3, "plateau": 0},
    "z": {"type": "minecraft:trapezoid", "max": 7, "min": -7, "plateau": 0},
}
AIR_ONLY = {
    "type": "minecraft:block_predicate_filter",
    "predicate": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
}
BIOME_FILTER = {"type": "minecraft:biome"}
PATCH_FEATURES = {"minecraft:flower_default"}

STEPS = 11
UNDERGROUND = {
    1: ["minecraft:lake_lava_underground", "minecraft:lake_lava_surface"],
    2: ["minecraft:amethyst_geode"],
    3: ["minecraft:monster_room", "minecraft:monster_room_deep"],
    6: [
        "minecraft:ore_dirt",
        "minecraft:ore_gravel",
        "minecraft:ore_granite_upper",
        "minecraft:ore_granite_lower",
        "minecraft:ore_diorite_upper",
        "minecraft:ore_diorite_lower",
        "minecraft:ore_andesite_upper",
        "minecraft:ore_andesite_lower",
        "minecraft:ore_tuff",
        "minecraft:ore_coal_upper",
        "minecraft:ore_coal_lower",
        "minecraft:ore_iron_upper",
        "minecraft:ore_iron_middle",
        "minecraft:ore_iron_small",
        "minecraft:ore_gold",
        "minecraft:ore_gold_lower",
        "minecraft:ore_redstone",
        "minecraft:ore_redstone_lower",
        "minecraft:ore_diamond",
        "minecraft:ore_diamond_medium",
        "minecraft:ore_diamond_large",
        "minecraft:ore_diamond_buried",
        "minecraft:ore_lapis",
        "minecraft:ore_lapis_buried",
        "minecraft:ore_copper",
        "minecraft:underwater_magma",
        "minecraft:disk_sand",
        "minecraft:disk_clay",
        "minecraft:disk_gravel",
    ],
    8: ["minecraft:spring_water", "minecraft:spring_lava"],
    10: ["minecraft:freeze_top_layer"],
}

CATEGORIES = [
    "ambient",
    "axolotls",
    "creature",
    "misc",
    "monster",
    "underground_water_creature",
    "water_ambient",
    "water_creature",
]


def uniform(low, high):
    return {"type": "minecraft:uniform", "min_inclusive": low, "max_inclusive": high}


VANILLA_CREATURES = [("sheep", 12, 4), ("pig", 10, 4), ("chicken", 10, 4), ("cow", 8, 4)]
VANILLA_MONSTERS = [
    ("spider", 100, 4),
    ("zombie", 95, 4),
    ("zombie_villager", 5, 1),
    ("skeleton", 100, 4),
    ("creeper", 100, 4),
    ("slime", 100, 4),
    ("enderman", 10, uniform(1, 4)),
    ("witch", 5, 1),
]
GLOW_SQUID = ("glow_squid", 10, uniform(4, 6))
SQUID = ("squid", 10, 4)


def spawns(categories):
    by_category = {}
    for category in CATEGORIES:
        by_category[category] = [
            {"type": name if ":" in name else f"minecraft:{name}", "count": count, "weight": weight}
            for name, weight, count in categories.get(category, [])
        ]
    return {"argument": {"spawn_costs": {}, "spawns_by_category": by_category}, "modifier": "overlay"}


def sky_for_temperature(temperature):
    value = max(-1.0, min(1.0, temperature / 3.0))
    red, green, blue = colorsys.hsv_to_rgb(0.62222224 - value * 0.05, 0.5 + value * 0.1, 1.0)
    return f"#{int(red * 255):02x}{int(green * 255):02x}{int(blue * 255):02x}"


def hex_color(value):
    return f"#{value:06x}"


def counted(count, *tail):
    return [{"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"}, *tail]


def heightmap(kind):
    return {"type": "minecraft:heightmap", "heightmap": kind}


def patch(count, kind, tries):
    return counted(count, heightmap(kind), BIOME_FILTER, {"type": "minecraft:count", "count": tries}, OFFSET, AIR_ONLY)


def surface_trees(count):
    return counted(
        count,
        {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        heightmap("OCEAN_FLOOR"),
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": "minecraft:oak_sapling"}},
        BIOME_FILTER,
    )


def generate(ctx):
    profile = ctx.profile
    modern = profile.modern

    def worldgen(kind, name, content):
        ctx.write(ctx.DATA / NS / "worldgen" / kind / f"{name}.json", content)

    def legacy_provider(value):
        if isinstance(value, dict):
            if value.get("type") == "minecraft:weighted":
                return {
                    "type": "minecraft:weighted_state_provider",
                    "entries": [{"data": {"Name": entry["data"]}, "weight": entry["weight"]} for entry in value["entries"]],
                }
            return {key: legacy_provider(entry) for key, entry in value.items()}
        return value

    def legacy_placement(modifier):
        predicate = modifier.get("predicate")
        if isinstance(predicate, dict) and predicate.get("type") == "minecraft:would_survive":
            return {**modifier, "predicate": {**predicate, "state": {"Name": predicate["state"]}}}
        return modifier

    def legacy_patch(feature_name, placement):
        if len(placement) < 3 or placement[-2] is not OFFSET or placement[-1] is not AIR_ONLY:
            return feature_name, placement
        outer = placement[:-3]
        if feature_name in PATCH_FEATURES:
            return feature_name, outer
        return {
            "type": "minecraft:random_patch",
            "config": {
                "tries": placement[-3]["count"],
                "xz_spread": OFFSET["x"]["max"],
                "y_spread": OFFSET["y"]["max"],
                "feature": {"feature": feature_name, "placement": [AIR_ONLY]},
            },
        }, outer

    def feature(name, content):
        if not modern:
            content = legacy_provider({"type": content["type"], "config": {key: value for key, value in content.items() if key != "type"}})
        worldgen(profile.feature_folder, name, content)

    def placed(name, feature_name, placement):
        if not modern:
            feature_name, placement = legacy_patch(feature_name, placement)
            placement = [legacy_placement(modifier) for modifier in placement]
        worldgen("placed_feature", name, {"feature": feature_name, "placement": placement})

    feature("magical_forest_decoration", {"type": f"{NS}:magical_forest_decoration"})
    placed("magical_forest_decoration", f"{NS}:magical_forest_decoration", [])

    feature(
        "magical_flowers",
        {
            "type": "minecraft:simple_block",
            "to_place": {"type": "minecraft:weighted", "entries": [{"data": f"minecraft:{flower}", "weight": 1} for flower in FLOWERS]},
        },
    )
    placed("magical_flowers", f"{NS}:magical_flowers", patch(10, "MOTION_BLOCKING", 64))
    feature(
        "magical_grass",
        {
            "type": "minecraft:simple_block",
            "to_place": {
                "type": "minecraft:weighted",
                "entries": [{"data": "minecraft:short_grass", "weight": 3}, {"data": "minecraft:fern", "weight": 1}],
            },
        },
    )
    placed("magical_grass", f"{NS}:magical_grass", patch(12, "WORLD_SURFACE_WG", 32))
    lily = "minecraft:waterlily"
    if not modern:
        feature("single_lily_pad", {"type": "minecraft:simple_block", "to_place": {"type": "minecraft:simple_state_provider", "state": {"Name": "minecraft:lily_pad"}}})
        lily = f"{NS}:single_lily_pad"
    placed("magical_lily_pads", lily, patch(6, "WORLD_SURFACE_WG", 10))

    placed("eerie_trees", "minecraft:oak", surface_trees(2))
    placed("tainted_trees", "minecraft:oak", surface_trees(2))
    placed("eerie_flowers", "minecraft:flower_default", patch(1, "MOTION_BLOCKING", 64))
    placed("eerie_grass", profile.vanilla_feature("minecraft:grass"), patch(2, "WORLD_SURFACE_WG", 32))
    feature("taint_decoration", {"type": f"{NS}:taint_decoration"})
    placed("taint_decoration", f"{NS}:taint_decoration", [])

    def features(vegetal, ground=True):
        steps = [[] for _ in range(STEPS)]
        if ground:
            for index, values in UNDERGROUND.items():
                steps[index] = list(values)
        steps[9] = vegetal
        return steps

    def biome(name, temperature, downfall, attributes, effects, steps, carvers=True, precipitation=True):
        carver_list = ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"] if carvers else []
        if modern:
            worldgen(
                "biome",
                name,
                {
                    "attributes": attributes,
                    "carvers": carver_list,
                    "downfall": downfall,
                    "effects": effects,
                    "features": steps,
                    "has_precipitation": precipitation,
                    "temperature": temperature,
                },
            )
            return
        legacy = {key: int(value[1:], 16) for key, value in effects.items()}
        legacy["sky_color"] = int(attributes["minecraft:visual/sky_color"][1:], 16)
        legacy["fog_color"] = 12638463
        legacy["water_fog_color"] = 329011
        music = attributes.get("minecraft:audio/background_music")
        if music:
            legacy["music"] = {**music["default"], "replace_current_music": False}
        by_category = attributes["minecraft:gameplay/natural_mob_spawns"]["argument"]["spawns_by_category"]
        worldgen(
            "biome",
            name,
            {
                "temperature": temperature,
                "downfall": downfall,
                "has_precipitation": precipitation,
                "effects": legacy,
                "carvers": {"air": carver_list},
                "features": steps,
                "spawners": {
                    category: [{"type": spawn["type"], "weight": spawn["weight"], "minCount": 1, "maxCount": 1} for spawn in entries]
                    for category, entries in by_category.items()
                },
                "spawn_costs": {},
            },
        )

    biome(
        "magical_forest",
        0.7,
        0.6,
        {
            "minecraft:audio/background_music": {
                "default": {"max_delay": 24000, "min_delay": 12000, "sound": "minecraft:music.overworld.forest"}
            },
            "minecraft:gameplay/natural_mob_spawns": spawns(
                {
                    "ambient": [("bat", 10, 8)],
                    "creature": [*VANILLA_CREATURES, ("wolf", 5, 4), ("wolf", 2, uniform(1, 3)), ("horse", 2, uniform(1, 3))],
                    "monster": [*VANILLA_MONSTERS, ("witch", 3, 1), ("enderman", 3, 1)],
                    "underground_water_creature": [GLOW_SQUID],
                    "water_creature": [SQUID],
                }
            ),
            "minecraft:visual/sky_color": sky_for_temperature(0.7),
        },
        {"water_color": hex_color(0x0077EE), "grass_color": hex_color(0x55FF81), "foliage_color": hex_color(0x66FFC5)},
        features(
            [
                "minecraft:glow_lichen",
                f"{NS}:magical_forest_decoration",
                f"{NS}:magical_flowers",
                f"{NS}:magical_grass",
                f"{NS}:magical_lily_pads",
                "minecraft:brown_mushroom_normal",
                "minecraft:red_mushroom_normal",
            ]
        ),
    )

    biome(
        "eerie",
        0.5,
        0.5,
        {
            "minecraft:gameplay/natural_mob_spawns": spawns(
                {
                    "ambient": [("bat", 3, 1)],
                    "monster": [*VANILLA_MONSTERS, ("witch", 8, 1), ("enderman", 4, 1)],
                    "underground_water_creature": [GLOW_SQUID],
                    "water_creature": [SQUID],
                }
            ),
            "minecraft:visual/sky_color": hex_color(0x222299),
        },
        {"water_color": hex_color(0x2E535F), "grass_color": hex_color(0x404840), "foliage_color": hex_color(0x405340)},
        features([f"{NS}:eerie_trees", f"{NS}:eerie_flowers", f"{NS}:eerie_grass"]),
    )

    biome(
        "tainted_land",
        0.5,
        0.5,
        {
            "minecraft:gameplay/natural_mob_spawns": spawns({"monster": [(f"{NS}:taintacle", 1, 1)]}),
            "minecraft:visual/sky_color": hex_color(0x7C44FF),
        },
        {"water_color": hex_color(0xCC1188), "grass_color": hex_color(0x6D4189), "foliage_color": hex_color(0x7C6D87)},
        features([f"{NS}:tainted_trees", f"{NS}:taint_decoration"]),
    )

    biome(
        "eldritch",
        0.5,
        0.5,
        {
            "minecraft:gameplay/natural_mob_spawns": spawns({"monster": [(f"{NS}:inhabited_zombie", 1, 1), (f"{NS}:eldritch_guardian", 1, 1)]}),
            "minecraft:visual/sky_color": hex_color(0x000000),
        },
        {"water_color": "#3f76e4"},
        features([], ground=False),
        carvers=False,
        precipitation=False,
    )

    tags = {
        "c:is_magical": ["magical_forest", "eerie", "tainted_land", "eldritch"],
        "c:is_forest": ["magical_forest"],
        "c:is_spooky": ["eerie", "eldritch"],
        "c:is_wasteland": ["tainted_land"],
        "minecraft:is_overworld": ["magical_forest", "eerie", "tainted_land"],
        "minecraft:is_forest": ["magical_forest"],
    }
    for tag, names in tags.items():
        namespace, path = profile.tag_name(tag)
        ctx.write(
            ctx.DATA / namespace / "tags" / "worldgen" / "biome" / f"{path}.json",
            {"replace": False, "values": [f"{NS}:{name}" for name in names]},
        )
