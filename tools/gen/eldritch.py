from .common import FACES, NS

BLOCKS = {
    "eldritch_altar": "obsidiantile",
    "eldritch_obelisk": "obsidiantile",
    "eldritch_capstone": "obsidiantile",
    "glowing_crusted_stone": "es_i_1",
    "glyphed_stone": "es_i_2",
    "ancient_doorway": "deco_3",
    "ancient_locking_mechanism": "deco_2",
    "crusted_opening": "crust",
    "runed_stone": "es_5",
    "eldritch_portal": "item/blank",
    "eldritch_nothing": "item/blank",
}

ITEMS = {
    "eldritch_eye": "eldritch_object",
    "crimson_rites": "crimson_rites",
    "runed_tablet": "eldritch_object_2",
    "primordial_pearl": "eldritch_object_3",
    "eldritch_obelisk_placer": "ob_placer",
}


def generate(ctx):
    data = ctx.DATA
    for name, texture in BLOCKS.items():
        model_name = f"{name}_block"
        texture_id = f"{NS}:{texture if "/" in texture else "block/" + texture}"
        if name in {"glowing_crusted_stone", "glyphed_stone"}:
            ctx.block_model(model_name, {
                "textures": {"particle": texture_id, "all": texture_id},
                "elements": [{
                    "from": [2, 2, 2],
                    "to": [14, 14, 14],
                    "faces": {
                        "down": {"texture": "#all", "cullface": "down"},
                        "up": {"texture": "#all", "cullface": "up"},
                        "north": {"texture": "#all", "cullface": "north"},
                        "south": {"texture": "#all", "cullface": "south"},
                        "west": {"texture": "#all", "cullface": "west"},
                        "east": {"texture": "#all", "cullface": "east"},
                    },
                }],
            })
        elif name == "ancient_locking_mechanism":
            ctx.block_model(model_name, {
                "textures": {
                    "particle": texture_id,
                    "up": f"{NS}:block/deco_3",
                    "down": f"{NS}:block/deco_3",
                    "north": f"{NS}:block/deco_2",
                    "south": f"{NS}:block/deco_3",
                    "west": f"{NS}:block/deco_3",
                    "east": f"{NS}:block/deco_3",
                },
                "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                    "down": {"texture": "#down"}, "up": {"texture": "#up"},
                    "north": {"texture": "#north"}, "south": {"texture": "#south"},
                    "west": {"texture": "#west"}, "east": {"texture": "#east"},
                }}],
            })
        else:
            ctx.block_model(model_name, {"parent": "minecraft:block/cube_all", "textures": {"all": texture_id}})
        if name in {"glowing_crusted_stone", "glyphed_stone"}:
            variants = {}
            for mask in range(64):
                connected = lambda side: bool(mask & (1 << side))
                lower = [0 if connected(side) else 2 for side in (4, 0, 2)]
                upper = [16 if connected(side) else 14 for side in (5, 1, 3)]
                variant_name = f"{model_name}_{mask}"
                ctx.block_model(variant_name, {
                    "textures": {"particle": texture_id, "all": texture_id},
                    "elements": [{"from": lower, "to": upper, "faces": {
                        side: {"texture": "#all"} for side in FACES
                    }}],
                })
                variants[f"bounds={mask}"] = {"model": f"{NS}:block/{variant_name}"}
            ctx.blockstate(name, {"variants": variants})
        elif name == "ancient_locking_mechanism":
            variants = {}
            for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270), ("up", 0), ("down", 0)):
                for half in ("lower", "upper"):
                    variant = {"model": f"{NS}:block/{model_name}"}
                    if rotation:
                        variant["y"] = rotation
                    variants[f"facing={facing},part={half}"] = variant
            ctx.blockstate(name, {"variants": variants})
        else:
            ctx.blockstate(name, {"multipart": [{"apply": {"model": f"{NS}:block/{model_name}"}}]})
        if name == "glowing_crusted_stone":
            ctx.block_item(name, model=f"{NS}:block/{model_name}")
        if name == "glowing_crusted_stone":
            ctx.self_drop(name)
        elif name == "glyphed_stone":
            ctx.write_loot(name, [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:knowledge_fragment"}]}])
    for name, texture in ITEMS.items():
        ctx.generated_item(name, texture)

    dimension_type = {
        "ambient_light": 0.0,
        "attributes": {
            "minecraft:gameplay/bed_rule": {"can_set_spawn": "never", "can_sleep": "never", "destroy_on_use": True},
            "minecraft:gameplay/can_start_raid": False,
            "minecraft:gameplay/respawn_anchor_works": False,
            "minecraft:visual/ambient_light_color": "#000000",
            "minecraft:visual/fog_color": "#181318",
            "minecraft:visual/sky_color": "#000000",
            "minecraft:visual/sky_light_color": "#000000",
            "minecraft:visual/sky_light_factor": 0.0,
        },
        "cardinal_light": "default",
        "coordinate_scale": 1.0,
        "has_ceiling": True,
        "has_ender_dragon_fight": False,
        "has_fixed_time": True,
        "has_skylight": False,
        "height": 256,
        "infiniburn": "#minecraft:infiniburn_overworld",
        "logical_height": 256,
        "min_y": 0,
        "monster_spawn_block_light_limit": 0,
        "monster_spawn_light_level": 0,
        "skybox": "none",
    }
    if not ctx.mcformat.profile.modern:
        for field in ["attributes", "cardinal_light", "has_ender_dragon_fight", "has_fixed_time", "skybox"]:
            dimension_type.pop(field)
        dimension_type.update({
            "ultrawarm": False, "natural": False, "piglin_safe": False, "bed_works": False,
            "respawn_anchor_works": False, "has_raids": False, "fixed_time": 18000, "effects": "minecraft:the_end",
        })
    ctx.write(data / NS / "dimension_type" / "outer_lands.json", dimension_type)
    ctx.write(data / NS / "dimension" / "outer_lands.json", {
        "type": f"{NS}:outer_lands",
        "generator": {
            "type": f"{NS}:outer_lands",
            "biome_source": {"type": "minecraft:fixed", "biome": f"{NS}:eldritch"},
        },
    })
