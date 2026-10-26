import json
import shutil
from pathlib import Path
import gen_phase10_11

ROOT = Path(__file__).resolve().parent.parent
NS = "thaumcraft"

BLOCKS = {
    "eldritch_altar": ("obsidiantile", "tile.blockEldritch.0.name"),
    "eldritch_obelisk": ("obsidiantile", "tile.blockEldritch.1.name"),
    "eldritch_capstone": ("obsidiantile", "tile.blockEldritch.3.name"),
    "glowing_crusted_stone": ("es_i_1", "tile.blockEldritch.4.name"),
    "glyphed_stone": ("es_i_2", "tile.blockEldritch.5.name"),
    "ancient_doorway": ("deco_3", "tile.blockEldritch.7.name"),
    "ancient_locking_mechanism": ("deco_2", "tile.blockEldritch.8.name"),
    "crusted_opening": ("crust", "tile.blockEldritch.9.name"),
    "runed_stone": ("es_5", "tile.blockEldritch.10.name"),
    "eldritch_portal": ("item/blank", "tile.blockPortalEldritch.name"),
    "eldritch_nothing": ("item/blank", None),
}

ITEMS = {
    "eldritch_eye": ("eldritch_object", "item.ItemEldritchObject.0.name"),
    "crimson_rites": ("crimson_rites", "item.ItemEldritchObject.1.name"),
    "runed_tablet": ("eldritch_object_2", "item.ItemEldritchObject.2.name"),
    "primordial_pearl": ("eldritch_object_3", "item.ItemEldritchObject.3.name"),
    "eldritch_obelisk_placer": ("ob_placer", "item.ItemEldritchObject.4.name"),
}

SPAWN_EGGS = {
    "eldritch_warden_spawn_egg": (0x552222, 0x404040),
    "eldritch_golem_spawn_egg": (0x555555, 0x404040),
    "cultist_leader_spawn_egg": (0xFF5055, 0x505050),
    "cultist_portal_spawn_egg": (0xFF5055, 0xFF50FF),
    "taintacle_giant_spawn_egg": (0xFFC0FF, 0x808080),
}


def read_legacy_language(path):
    values = {}
    if not path.exists():
        return values
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def modern_key(kind, name):
    return f"{kind}.{NS}.{name}"


def copy_legacy_texture(ctx, name, folder):
    source = ROOT / "reference" / "decompiled" / "assets" / NS / "textures" / folder / f"{name}.png"
    target = ctx.ASSETS / "textures" / ("block" if folder == "blocks" else "item") / f"{name}.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, target)


def generate(ctx):
    assets = ctx.ASSETS
    data = ctx.DATA
    for texture in {texture.removeprefix("item/") for texture, _ in BLOCKS.values()}:
        if texture != "blank" and not (ctx.SOURCE_TEXTURES / "block" / f"{texture}.png").exists():
            copy_legacy_texture(ctx, texture, "blocks")
    for texture in {texture for texture, _ in ITEMS.values()}:
        if not (ctx.SOURCE_TEXTURES / "item" / f"{texture}.png").exists():
            copy_legacy_texture(ctx, texture, "items")
    for name, (texture, _) in BLOCKS.items():
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
                        side: {"texture": "#all"} for side in ("down", "up", "north", "south", "west", "east")
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
    for name, texture in {
        "eldritch_eye": "eldritch_object",
        "crimson_rites": "crimson_rites",
        "runed_tablet": "eldritch_object_2",
        "primordial_pearl": "eldritch_object_3",
        "eldritch_obelisk_placer": "ob_placer",
    }.items():
        ctx.generated_item(name, texture)
    for name, (base_color, spot_color) in SPAWN_EGGS.items():
        gen_phase10_11.write_spawn_egg(ctx, name, base_color, spot_color)

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
    dimension_type["attributes"]["minecraft:gameplay/bed_rule"] = ctx.mcformat.profile.bed_rule(dimension_type["attributes"]["minecraft:gameplay/bed_rule"])
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

    biome_path = data / NS / "worldgen" / "biome" / "eldritch.json"
    biome = json.loads(biome_path.read_text(encoding="utf-8"))
    if ctx.mcformat.profile.attribute_spawns:
        biome["attributes"]["minecraft:gameplay/natural_mob_spawns"]["argument"]["spawns_by_category"]["monster"] = [
            {"type": f"{NS}:{name}", "count": 1, "weight": 1} for name in ["inhabited_zombie", "eldritch_guardian"]
        ]
    else:
        biome["spawners"]["monster"] = [
            {"type": f"{NS}:{name}", "minCount": 1, "maxCount": 1, "weight": 1} for name in ["inhabited_zombie", "eldritch_guardian"]
        ]
    ctx.write(biome_path, biome)

    locale_files = sorted((ctx.SOURCE_TEXTURES.parent / "lang").glob("*.json"))
    legacy_lang_root = ROOT / "reference" / "decompiled" / "assets" / NS / "lang"
    for locale_path in locale_files:
        locale = locale_path.stem
        legacy_locale = locale.split("_")[0] + "_" + locale.split("_")[1].upper()
        legacy = read_legacy_language(legacy_lang_root / f"{legacy_locale}.lang")
        translations = json.loads(locale_path.read_text(encoding="utf-8"))
        for name, (_, old_key) in BLOCKS.items():
            if old_key and old_key in legacy:
                translations[modern_key("block", name)] = legacy[old_key]
        for name, (_, old_key) in ITEMS.items():
            if old_key in legacy:
                translations[modern_key("item", name)] = legacy[old_key]
        locale_path.write_text(json.dumps(translations, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
