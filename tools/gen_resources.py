import json
import random
import shutil
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(Path(__file__).resolve().parent))
import mcformat
import gen_biomes
import gen_loot
import gen_phase4
import gen_phase5
import gen_phase6
import gen_phase8
import gen_phase9_devices
import gen_phase9_warded
import gen_phase9_machines
import gen_phase9_mirrors
import gen_phase12
import gen_phase13
import gen_phase10_11
import gen_warp_shaders
import gen_node_shaders
import gen_villages
import gen_world

profile = mcformat.init()
OUT = mcformat.out_root
ASSETS = OUT / "assets" / "thaumcraft"
DATA = OUT / "data"
NS = "thaumcraft"
SOURCE_TEXTURES = ROOT / "src" / "main" / "resources" / "assets" / NS / "textures"

INFUSED_ELEMENTS = ["air", "fire", "water", "earth", "order", "entropy"]
INFUSED_COLORS = [0xFFFF7E, 0xFF3C01, 0x0090FF, 0x00A000, 0xEECCFF, 0x555577]
FOLIAGE_DEFAULT = 0x48B518
SILVERWOOD_COLOR = 0x8899AA
COLORS = [
    "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
    "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black",
]
CANDLE_COLORS = [
    15790320, 15435844, 12801229, 6719955, 14602026, 4312372, 14188952, 4408131,
    10526880, 2651799, 8073150, 2437522, 5320730, 3887386, 11743532, 1973019,
]
DIRECTIONS = ["down", "up", "north", "south", "west", "east"]
FUELS = {"alumentum": 6400, "magical_log": 400}
FUEL_ITEMS = {"alumentum": ["alumentum"], "magical_log": ["greatwood_log", "silverwood_log"]}
ASPECT_TINT = {"type": f"{NS}:aspect"}

block_tags = defaultdict(set)
item_tags = defaultdict(set)
BLOCK_MODELS = {}
TEXTURE_REFERENCES = set()


def write(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(content, indent=2) + "\n", encoding="utf-8")


def ref(name, kind="block"):
    return f"{NS}:{kind}/{name}"


def tint(color):
    value = color | 0xFF000000
    if value >= 0x80000000:
        value -= 0x100000000
    return {"type": "minecraft:constant", "value": value}


def block_model(name, content):
    BLOCK_MODELS[name] = content
    write(ASSETS / "models" / "block" / f"{name}.json", content)


def item_model(name, content):
    write(ASSETS / "models" / "item" / f"{name}.json", content)


def item_definition(name, model, tints=None):
    definition = {"type": "minecraft:model", "model": model}
    if tints:
        definition["tints"] = tints
    write(ASSETS / "items" / f"{name}.json", {"model": definition})


def blockstate(name, content):
    write(ASSETS / "blockstates" / f"{name}.json", content)


def simple_state(name, model=None):
    blockstate(name, {"variants": {"": {"model": model or ref(name)}}})


def cube_all(name, texture):
    block_model(name, {"parent": "minecraft:block/cube_all", "textures": {"all": ref(texture)}})


def cube_column(name, end, side):
    block_model(name, {"parent": "minecraft:block/cube_column", "textures": {"end": ref(end), "side": ref(side)}})


def generated_item(name, layers, tints=None, texture_kind="item"):
    if isinstance(layers, str):
        layers = [layers]
    item_model(name, {
        "parent": "minecraft:item/generated",
        "textures": {f"layer{index}": ref(layer, texture_kind) for index, layer in enumerate(layers)},
    })
    item_definition(name, ref(name, "item"), tints)


def block_item(name, tints=None, model=None):
    item_definition(name, model or ref(name), tints)


def write_loot(name, pools):
    write(DATA / NS / profile.loot_folder / "blocks" / f"{name}.json", profile.loot_table({
        "type": "minecraft:block",
        "pools": pools,
        "random_sequence": f"{NS}:blocks/{name}",
    }))


def self_drop(name):
    write_loot(name, [{
        "rolls": 1,
        "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
        "condition": {"type": "minecraft:survives_explosion"},
    }])


def slab_drop(name):
    write_loot(name, [{
        "rolls": 1,
        "entries": [{
            "type": "minecraft:item",
            "name": f"{NS}:{name}",
            "modifier": [
                {
                    "type": "minecraft:set_count",
                    "count": 2,
                    "add": False,
                    "condition": {"type": "minecraft:match_block", "blocks": f"{NS}:{name}", "state": {"type": "double"}},
                },
                {"type": "minecraft:explosion_decay"},
            ],
        }],
    }])


def bonus_drop(block, item, base_max=1):
    modifiers = []
    if base_max > 1:
        modifiers.append({"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": base_max}})
    modifiers.append({
        "type": "minecraft:apply_bonus",
        "enchantment": "minecraft:fortune",
        "formula": "minecraft:uniform_bonus_count",
        "parameters": {"bonusMultiplier": 1},
    })
    modifiers.append({"type": "minecraft:explosion_decay"})
    write_loot(block, [{
        "rolls": 1,
        "entries": [{
            "type": "minecraft:alternatives",
            "children": [
                {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": f"{NS}:{block}"},
                {"type": "minecraft:item", "name": item, "modifier": modifiers},
            ],
        }],
    }])


def shear_only(name):
    write_loot(name, [{
        "rolls": 1,
        "entries": [{
            "type": "minecraft:item",
            "condition": {"type": "minecraft:any_of", "terms": ["minecraft:tool/can_shear", "minecraft:tool/can_silk_touch"]},
            "name": f"{NS}:{name}",
        }],
    }])


def mineable(tool, *blocks):
    for name in blocks:
        block_tags[f"minecraft:mineable/{tool}"].add(f"{NS}:{name}")


def tag_both(tag, *names):
    for name in names:
        block_tags[tag].add(f"{NS}:{name}")
        item_tags[tag].add(f"{NS}:{name}")


def stairs_state(name):
    variants = {}
    table = {
        "straight": ("stairs", {"east": 0, "west": 180, "south": 90, "north": 270}, {"east": 0, "west": 180, "south": 90, "north": 270}),
        "outer_right": ("stairs_outer", {"east": 0, "west": 180, "south": 90, "north": 270}, {"east": 90, "west": 270, "south": 180, "north": 0}),
        "outer_left": ("stairs_outer", {"east": 270, "west": 90, "south": 0, "north": 180}, {"east": 0, "west": 180, "south": 90, "north": 270}),
        "inner_right": ("stairs_inner", {"east": 0, "west": 180, "south": 90, "north": 270}, {"east": 90, "west": 270, "south": 180, "north": 0}),
        "inner_left": ("stairs_inner", {"east": 270, "west": 90, "south": 0, "north": 180}, {"east": 0, "west": 180, "south": 90, "north": 270}),
    }
    suffix = {"stairs": "", "stairs_outer": "_outer", "stairs_inner": "_inner"}
    for shape, (kind, bottom, top) in table.items():
        for facing in ["east", "west", "south", "north"]:
            for half, rotations in (("bottom", bottom), ("top", top)):
                variant = {"model": ref(name + suffix[kind]), "y": rotations[facing]}
                if half == "top":
                    variant["x"] = 180
                if variant["y"] or half == "top":
                    variant["uvlock"] = True
                if not variant["y"]:
                    del variant["y"]
                variants[f"facing={facing},half={half},shape={shape}"] = variant
    blockstate(name, {"variants": variants})


def stairs(name, texture):
    textures = {"bottom": ref(texture), "top": ref(texture), "side": ref(texture)}
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        block_model(name + suffix, {"parent": f"minecraft:block/{parent}", "textures": textures})
    stairs_state(name)
    block_item(name)
    self_drop(name)


def slab(name, texture, double_texture=None):
    textures = {"bottom": ref(texture), "top": ref(texture), "side": ref(texture)}
    block_model(name, {"parent": "minecraft:block/slab", "textures": textures})
    block_model(f"{name}_top", {"parent": "minecraft:block/slab_top", "textures": textures})
    block_model(f"{name}_double", {"parent": "minecraft:block/cube_all", "textures": {"all": ref(texture)}})
    blockstate(name, {"variants": {
        "type=bottom": {"model": ref(name)},
        "type=top": {"model": ref(f"{name}_top")},
        "type=double": {"model": ref(f"{name}_double")},
    }})
    block_item(name)
    slab_drop(name)


def world_blocks():
    for name, texture in [("cinnabar_ore", "cinnibar"), ("amber_ore", "amberore")]:
        cube_all(name, texture)
        simple_state(name)
        block_item(name)
    self_drop("cinnabar_ore")
    bonus_drop("amber_ore", f"{NS}:amber")
    mineable("pickaxe", "cinnabar_ore", "amber_ore")
    block_tags["minecraft:needs_iron_tool"].update({f"{NS}:cinnabar_ore", f"{NS}:amber_ore"})
    tag_both("c:ores", "cinnabar_ore", "amber_ore")
    tag_both("c:ores/cinnabar", "cinnabar_ore")
    tag_both("c:ores/amber", "amber_ore")

    for element, color in zip(INFUSED_ELEMENTS, INFUSED_COLORS):
        name = f"{element}_infused_stone"
        block_model(name, {
            "parent": "minecraft:block/block",
            "textures": {"particle": ref("infusedorestone"), "stone": ref("infusedorestone"), "ore": ref("infusedore")},
            "elements": [
                {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                    face: {"texture": "#stone", "cullface": face} for face in DIRECTIONS
                }},
                {"from": [0, 0, 0], "to": [16, 16, 16], "light_emission": 10, "faces": {
                    face: {"texture": "#ore", "cullface": face, "tintindex": 0} for face in DIRECTIONS
                }},
            ],
        })
        simple_state(name)
        block_item(name, [tint(color)])
        write_loot(name, [{
            "rolls": 1,
            "entries": [{
                "type": "minecraft:alternatives",
                "children": [
                    {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": f"{NS}:{name}"},
                    {
                        "type": "minecraft:item",
                        "name": f"{NS}:{element}_shard",
                        "modifier": [
                            {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                            {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:uniform_bonus_count", "parameters": {"bonusMultiplier": 1}},
                            {"type": "minecraft:explosion_decay"},
                        ],
                    },
                ],
            }],
        }])
        mineable("pickaxe", name)
        tag_both("c:ores", name)


def wood():
    for kind in ["greatwood", "silverwood"]:
        log = f"{kind}_log"
        cube_column(log, f"{kind}top", f"{kind}side")
        block_model(f"{log}_horizontal", {
            "parent": "minecraft:block/cube_column_horizontal",
            "textures": {"end": ref(f"{kind}top"), "side": ref(f"{kind}side")},
        })
        blockstate(log, {"variants": {
            "axis=x": {"model": ref(f"{log}_horizontal"), "x": 90, "y": 90},
            "axis=y": {"model": ref(log)},
            "axis=z": {"model": ref(f"{log}_horizontal"), "x": 90},
        }})
        block_item(log)
        self_drop(log)
        mineable("axe", log)
        tag_both("minecraft:logs", log)
        tag_both("minecraft:logs_that_burn", log)

        leaves = f"{kind}_leaves"
        block_model(leaves, {"parent": "minecraft:block/leaves", "textures": {"all": ref(f"{kind}leaves")}})
        simple_state(leaves)
        block_item(leaves, [tint(FOLIAGE_DEFAULT if kind == "greatwood" else SILVERWOOD_COLOR)])
        shear_only(leaves)
        mineable("hoe", leaves)
        tag_both("minecraft:leaves", leaves)

        sapling = f"{kind}_sapling"
        block_model(sapling, {"parent": "minecraft:block/cross", "textures": {"cross": ref(f"{kind}sapling")}})
        simple_state(sapling)
        generated_item(sapling, f"{kind}sapling", texture_kind="block")
        self_drop(sapling)
        tag_both("minecraft:saplings", sapling)

        planks = f"{kind}_planks"
        cube_all(planks, f"planks_{kind}")
        simple_state(planks)
        block_item(planks)
        self_drop(planks)
        slab(f"{kind}_slab", f"planks_{kind}")
        stairs(f"{kind}_stairs", f"planks_{kind}")
        mineable("axe", planks, f"{kind}_slab", f"{kind}_stairs")
        tag_both("minecraft:planks", planks)
        tag_both("minecraft:wooden_slabs", f"{kind}_slab")
        tag_both("minecraft:wooden_stairs", f"{kind}_stairs")
        tag_both("minecraft:slabs", f"{kind}_slab")
        tag_both("minecraft:stairs", f"{kind}_stairs")


def plants():
    for name, texture in [("shimmerleaf", "shimmerleaf"), ("cinderpearl", "cinderpearl"), ("vishroom", "manashroom"), ("ethereal_bloom", "purifier_seed")]:
        block_model(name, {"parent": "minecraft:block/cross", "textures": {"cross": ref(texture)}})
        simple_state(name)
        generated_item(name, texture, texture_kind="block")
        self_drop(name)

    for age in range(8):
        stem = 0 if age < 2 else (1 if age < 5 else 2)
        block_model(f"mana_pod_{age}", {"parent": "minecraft:block/cross", "textures": {"cross": ref(f"manapod_stem_{stem}")}})
    blockstate("mana_pod", {"variants": {f"age={age}": {"model": ref(f"mana_pod_{age}")} for age in range(8)}})
    write_loot("mana_pod", [])


def cosmetic():
    totem_variants = {}
    for part, model, side in [("base", "obsidian_totem", "obsidiantotembase"), ("shaded", "obsidian_totem_shaded", "obsidiantotembaseshaded")]:
        cube_column(model, "obsidiantile", side)
        for carving in range(4):
            totem_variants[f"carving={carving},part={part}"] = {"model": ref(model)}
    for carving in range(4):
        model = f"obsidian_totem_carved_{carving}"
        textures = {"particle": ref("obsidiantile"), "down": ref("obsidiantile"), "up": ref("obsidiantile")}
        for face, side in {"north": 2, "south": 3, "west": 4, "east": 5}.items():
            textures[face] = ref(f"obsidiantotem{(side + carving) % 4 + 1}")
        block_model(model, {"parent": "minecraft:block/cube", "textures": textures})
        totem_variants[f"carving={carving},part=carved"] = {"model": ref(model)}
    blockstate("obsidian_totem", {"variants": totem_variants})
    cube_all("obsidian_tile", "obsidiantile")
    cube_all("travel_paving_stone", "paving_stone_travel")
    cube_all("warding_paving_stone", "paving_stone_warding")
    cube_all("thaumium_block", "thaumiumblock")
    cube_column("tallow_block", "tallowblock_top", "tallowblock")
    cube_all("arcane_stone", "pedestal_top")
    cube_all("arcane_stone_bricks", "arcane_stone")
    cube_all("crusted_stone", "crust")
    block_model("ancient_stone_pedestal", {
        "parent": "minecraft:block/cube_column",
        "textures": {"end": ref("es_1"), "side": ref("es_p")},
    })
    cube_column("amber_block", "amberblock_top", "amberblock")
    cube_all("amber_bricks", "amberbrick")
    for name in ["obsidian_tile", "travel_paving_stone", "warding_paving_stone", "thaumium_block", "tallow_block", "arcane_stone",
                 "arcane_stone_bricks", "crusted_stone", "ancient_stone_pedestal", "amber_block", "amber_bricks"]:
        simple_state(name)

    for index in range(1, 5):
        cube_all(f"ancient_stone_{index}", f"es_{index}")
        cube_all(f"ancient_rock_{index}", f"er_{index}")
    for name, prefix in [("ancient_stone", "ancient_stone"), ("ancient_stone_no_spawn", "ancient_stone"), ("ancient_rock", "ancient_rock")]:
        blockstate(name, {"variants": {"": [{"model": ref(f"{prefix}_{index}")} for index in range(1, 5)]}})
    block_item("ancient_stone", model=ref("ancient_stone_1"))
    block_item("ancient_stone_no_spawn", model=ref("ancient_stone_1"))
    block_item("ancient_rock", model=ref("ancient_rock_1"))

    for state, top in (("", "golem_stone_top"), ("_active", "golem_stone_top_active")):
        block_model(f"golem_fetter{state}", {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {"bottom": ref("arcane_stone"), "top": ref(top), "side": ref("golem_stone_side")},
        })
    blockstate("golem_fetter", {"variants": {
        "active=false": {"model": ref("golem_fetter")},
        "active=true": {"model": ref("golem_fetter_active")},
    }})

    items = [
        "obsidian_totem", "obsidian_tile", "travel_paving_stone", "warding_paving_stone", "thaumium_block", "tallow_block",
        "arcane_stone", "arcane_stone_bricks", "golem_fetter", "crusted_stone", "ancient_stone_pedestal", "amber_block", "amber_bricks",
    ]
    for name in items:
        block_item(name)
    for name in items + ["ancient_stone", "ancient_stone_no_spawn", "ancient_rock"]:
        self_drop(name)
    mineable("pickaxe", *items, "ancient_stone", "ancient_stone_no_spawn", "ancient_rock")
    beacon = [
        "obsidian_totem", "obsidian_tile", "travel_paving_stone", "warding_paving_stone", "thaumium_block", "tallow_block",
        "arcane_stone", "arcane_stone_bricks", "golem_fetter", "ancient_stone", "ancient_rock", "ancient_stone_no_spawn",
        "crusted_stone", "ancient_stone_pedestal",
    ]
    for name in beacon:
        block_tags["minecraft:beacon_base_blocks"].add(f"{NS}:{name}")
    tag_both("c:storage_blocks/thaumium", "thaumium_block")
    tag_both("c:storage_blocks", "thaumium_block")

    slab("arcane_stone_slab", "arcane_stone")
    slab("ancient_stone_slab", "es_1")
    stairs("arcane_stone_stairs", "arcane_stone")
    stairs("ancient_stone_stairs", "es_1")
    mineable("pickaxe", "arcane_stone_slab", "ancient_stone_slab", "arcane_stone_stairs", "ancient_stone_stairs")
    tag_both("minecraft:slabs", "arcane_stone_slab", "ancient_stone_slab")
    tag_both("minecraft:stairs", "arcane_stone_stairs", "ancient_stone_stairs")


def candles():
    for index, color in enumerate(COLORS):
        name = f"{color}_tallow_candle"
        elements = [{
            "from": [6, 0, 6], "to": [10, 8, 10],
            "faces": {direction: {"texture": "#candle", "tintindex": 0} for direction in DIRECTIONS},
        }]
        rng = random.Random(index + 1)
        for drip in range(1 + rng.randrange(5)):
            side = rng.random() < 0.5
            location = 2 + rng.randrange(2)
            height = 1 + rng.randrange(3)
            if drip % 2 == 0:
                start = [5 + location, 0, 5 if side else 10]
                end = [6 + location, height, 6 if side else 11]
            else:
                start = [5 if side else 10, 0, 5 + location]
                end = [6 if side else 11, height, 6 + location]
            elements.append({
                "from": start, "to": end,
                "faces": {direction: {"texture": "#candle", "tintindex": 0} for direction in DIRECTIONS},
            })
        elements.append({
            "from": [7.6, 8, 7.6], "to": [8.4, 10, 8.4],
            "faces": {direction: {"texture": "#stub"} for direction in DIRECTIONS if direction != "down"},
        })
        block_model(name, {
            "parent": "minecraft:block/block",
            "textures": {"particle": ref("candle"), "candle": ref("candle"), "stub": ref("candlestub")},
            "elements": elements,
        })
        simple_state(name)
        block_item(name, [tint(CANDLE_COLORS[index])])
        self_drop(name)
        tag_both(f"{NS}:tallow_candles", name)


ITEMS = {
    "alumentum": "alumentum",
    "nitor": "nitor",
    "thaumium_ingot": "thaumiumingot",
    "quicksilver": "quicksilver",
    "magic_tallow": "tallow",
    "amber": "amber",
    "enchanted_fabric": "cloth",
    "vis_filter": "filter",
    "knowledge_fragment": "knowledgefragment",
    "mirrored_glass": "mirrorglass",
    "tainted_goo": "taint_slime",
    "taint_tendril": "taint_tendril",
    "jar_label": "label",
    "salis_mundus": "dust",
    "primal_charm": "charm",
    "void_metal_ingot": "voidingot",
    "void_seed": "voidseed",
    "gold_coin": "coin",
    "copper_nugget": "nuggetcopper",
    "tin_nugget": "nuggettin",
    "silver_nugget": "nuggetsilver",
    "lead_nugget": "nuggetlead",
    "quicksilver_drop": "nuggetquicksilver",
    "thaumium_nugget": "nuggetthaumium",
    "void_metal_nugget": "nuggetvoid",
    "native_iron_cluster": "clusteriron",
    "native_copper_cluster": "clustercopper",
    "native_tin_cluster": "clustertin",
    "native_silver_cluster": "clustersilver",
    "native_lead_cluster": "clusterlead",
    "native_cinnabar_cluster": "clustercinnabar",
    "native_gold_cluster": "clustergold",
    "chicken_nugget": "nuggetchicken",
    "beef_nugget": "nuggetbeef",
    "pork_nugget": "nuggetpork",
    "fish_nugget": "nuggetfish",
    "triple_meat_treat": "tripletreat",
    "glass_phial": "phial",
    "zombie_brain": "brain",
    "common_loot_bag": "lootbag",
    "uncommon_loot_bag": "lootbagunc",
    "rare_loot_bag": "lootbagrare",
    "balanced_shard": "shard_balanced",
}


def items():
    for name, texture in ITEMS.items():
        generated_item(name, texture)
    for element, color in zip(INFUSED_ELEMENTS, INFUSED_COLORS):
        generated_item(f"{element}_shard", "shard", [tint(color)])
        item_tags[f"{NS}:shards"].add(f"{NS}:{element}_shard")
    item_tags[f"{NS}:shards"].add(f"{NS}:balanced_shard")
    generated_item("essentia_phial", ["phial", "essence"], [tint(0xFFFFFF), ASPECT_TINT])
    generated_item("wisp_essence", "wispessence", [ASPECT_TINT])
    generated_item("crystallized_essence", "crystalessence", [ASPECT_TINT])
    generated_item("mana_bean", "mana_bean", [ASPECT_TINT])

    for metal in ["thaumium", "void_metal"]:
        item_tags[f"c:ingots/{metal}"].add(f"{NS}:{metal}_ingot")
        item_tags["c:ingots"].add(f"{NS}:{metal}_ingot")
    for metal in ["thaumium", "void_metal", "quicksilver", "copper", "tin", "silver", "lead"]:
        source = "quicksilver_drop" if metal == "quicksilver" else f"{metal}_nugget"
        item_tags[f"c:nuggets/{metal}"].add(f"{NS}:{source}")
        item_tags["c:nuggets"].add(f"{NS}:{source}")
    item_tags["c:gems/amber"].add(f"{NS}:amber")
    item_tags["c:gems"].add(f"{NS}:amber")
    for metal in ["iron", "copper", "tin", "silver", "lead", "cinnabar", "gold"]:
        item_tags[f"{NS}:clusters"].add(f"{NS}:native_{metal}_cluster")


def recipe(name, content, referenced, extra_conditions=None):
    conditions = profile.recipe_conditions(sorted(value for value in referenced if isinstance(value, str) and value.startswith(f"{NS}:")))
    if extra_conditions:
        existing = conditions.get("neoforge:conditions", [])
        conditions = {"neoforge:conditions": existing + extra_conditions}
    if conditions:
        content = {**conditions, **content}
    write(DATA / NS / profile.recipe_folder / f"{name}.json", content)


def shaped(name, result, count, pattern, key, category="misc", conditions=None, components=None):
    content = {"type": "minecraft:crafting_shaped"}
    profile.recipe_category(content, category)
    content["key"] = {symbol: profile.recipe_ingredient(ingredient) for symbol, ingredient in key.items()}
    content["pattern"] = pattern
    content["result"] = profile.recipe_result(result, count, components)
    recipe(name, content, [result, *key.values()], conditions)


def shapeless(name, result, count, ingredients, category="misc", conditions=None):
    content = {"type": "minecraft:crafting_shapeless"}
    profile.recipe_category(content, category)
    content["ingredients"] = [profile.recipe_ingredient(ingredient) for ingredient in ingredients]
    content["result"] = profile.recipe_result(result, count, None)
    recipe(name, content, [result, *ingredients], conditions)


def smelting(name, ingredient, result, count, experience):
    content = {
        "type": "minecraft:smelting",
        "cookingtime": 200,
        "experience": experience,
        "ingredient": profile.recipe_ingredient(ingredient),
        "result": profile.smelting_result(result, count),
    }
    recipe(name, content, [ingredient, result])


def not_tag_empty(tag):
    return [{"type": "neoforge:not", "value": {"type": "neoforge:tag_empty", "tag": tag}}]


def recipes():
    tc = lambda name: f"{NS}:{name}"
    for source, nugget, ingot in [
        ("thaumium_nugget", "thaumium_nugget", "thaumium_ingot"),
        ("void_metal_nugget", "void_metal_nugget", "void_metal_ingot"),
        ("quicksilver_drop", "quicksilver_drop", "quicksilver"),
    ]:
        shapeless(f"{nugget}_from_{ingot}", tc(nugget), 9, [tc(ingot)])
        shaped(f"{ingot}_from_{nugget}", tc(ingot), 1, ["###", "###", "###"], {"#": tc(nugget)})
    for metal in ["tin", "silver", "lead"]:
        shapeless(
            f"{metal}_nugget_from_ingot", tc(f"{metal}_nugget"), 9, [f"#c:ingots/{metal}"],
            conditions=not_tag_empty(f"c:ingots/{metal}"),
        )
    shapeless("quicksilver_from_shimmerleaf", tc("quicksilver"), 1, [tc("shimmerleaf")])
    shapeless("blaze_powder_from_cinderpearl", "minecraft:blaze_powder", 1, [tc("cinderpearl")])

    for kind in ["greatwood", "silverwood"]:
        shapeless(f"{kind}_planks", tc(f"{kind}_planks"), 4, [tc(f"{kind}_log")], "building")
        shaped(f"{kind}_stairs", tc(f"{kind}_stairs"), 4, ["K  ", "KK ", "KKK"], {"K": tc(f"{kind}_planks")}, "building")
        shaped(f"{kind}_slab", tc(f"{kind}_slab"), 6, ["KKK"], {"K": tc(f"{kind}_planks")}, "building")

    shaped("thaumium_block", tc("thaumium_block"), 1, ["KKK", "KKK", "KKK"], {"K": "#c:ingots/thaumium"}, "building")
    shapeless("thaumium_ingot_from_block", tc("thaumium_ingot"), 9, [tc("thaumium_block")])
    shaped("tallow_block", tc("tallow_block"), 1, ["KKK", "KKK", "KKK"], {"K": tc("magic_tallow")}, "building")
    shapeless("magic_tallow_from_block", tc("magic_tallow"), 9, [tc("tallow_block")])
    shaped("amber_block", tc("amber_block"), 1, ["##", "##"], {"#": tc("amber")}, "building")
    shaped("amber_bricks", tc("amber_bricks"), 4, ["##", "##"], {"#": tc("amber_block")}, "building")
    shapeless("amber_from_block", tc("amber"), 4, [tc("amber_block")])
    shapeless("amber_from_bricks", tc("amber"), 4, [tc("amber_bricks")])
    shaped("obsidian_tile", tc("obsidian_tile"), 4, ["##", "##"], {"#": "minecraft:obsidian"}, "building")
    shaped("arcane_stone_bricks", tc("arcane_stone_bricks"), 4, ["SS", "SS"], {"S": tc("arcane_stone")}, "building")
    shaped("arcane_stone_stairs", tc("arcane_stone_stairs"), 4, ["K  ", "KK ", "KKK"], {"K": tc("arcane_stone_bricks")}, "building")
    shaped("arcane_stone_slab", tc("arcane_stone_slab"), 6, ["KKK"], {"K": tc("arcane_stone_bricks")}, "building")

    shaped("white_tallow_candle", tc("white_tallow_candle"), 3, [" S ", " T ", " T "], {"S": "minecraft:string", "T": tc("magic_tallow")})
    for color in COLORS[1:]:
        shapeless(f"{color}_tallow_candle", tc(f"{color}_tallow_candle"), 1, [f"#c:dyes/{color}", tc("white_tallow_candle")])
    shapeless("white_tallow_candle_from_bone_meal", tc("white_tallow_candle"), 1, ["minecraft:bone_meal", f"#{NS}:tallow_candles"])

    shaped("glass_phial", tc("glass_phial"), 8, [" C ", "G G", " G "], {"G": "minecraft:glass", "C": "minecraft:clay_ball"})
    for index, nuggets in enumerate([
        ["beef_nugget", "chicken_nugget", "pork_nugget"],
        ["beef_nugget", "chicken_nugget", "fish_nugget"],
        ["beef_nugget", "fish_nugget", "pork_nugget"],
        ["fish_nugget", "chicken_nugget", "pork_nugget"],
    ]):
        shapeless(f"triple_meat_treat_{index}", tc("triple_meat_treat"), 1, ["minecraft:sugar", *[tc(nugget) for nugget in nuggets]])

    smelting("quicksilver_from_cinnabar_ore", tc("cinnabar_ore"), tc("quicksilver"), 1, 1.0)
    smelting("amber_from_amber_ore", tc("amber_ore"), tc("amber"), 1, 1.0)
    smelting("charcoal_from_greatwood_log", tc("greatwood_log"), "minecraft:charcoal", 1, 0.5)
    smelting("charcoal_from_silverwood_log", tc("silverwood_log"), "minecraft:charcoal", 1, 0.5)
    smelting("iron_ingot_from_cluster", tc("native_iron_cluster"), "minecraft:iron_ingot", 2, 1.0)
    smelting("quicksilver_from_cluster", tc("native_cinnabar_cluster"), tc("quicksilver"), 2, 1.0)
    smelting("gold_ingot_from_cluster", tc("native_gold_cluster"), "minecraft:gold_ingot", 2, 1.0)
    smelting("copper_ingot_from_cluster", tc("native_copper_cluster"), "minecraft:copper_ingot", 2, 1.0)
    smelting("salis_mundus_from_balanced_shard", tc("balanced_shard"), tc("salis_mundus"), 1, 1.0)
    smelting("gold_nugget_from_coin", tc("gold_coin"), "minecraft:gold_nugget", 1, 0.0)


def tag_entries(values):
    return [{"id": value, "required": False} if value.startswith(f"{NS}:") else value for value in sorted(values)]


def write_tags():
    if profile.name == "26.3":
        block_tags["minecraft:washed_away_by_fluids"].update({f"{NS}:purifying_fluid", f"{NS}:liquid_death"})
    for tag, values in block_tags.items():
        namespace, path = profile.tag_name(tag)
        write(DATA / namespace / profile.block_tag_folder / f"{path}.json", {"replace": False, "values": tag_entries(values)})
    for tag, values in item_tags.items():
        namespace, path = profile.tag_name(tag)
        write(DATA / namespace / profile.item_tag_folder / f"{path}.json", {"replace": False, "values": tag_entries(values)})


def collect_textures(value):
    if isinstance(value, dict):
        for key, entry in value.items():
            if key == "textures" and isinstance(entry, dict):
                TEXTURE_REFERENCES.update(texture for texture in entry.values() if isinstance(texture, str) and not texture.startswith("#"))
            else:
                collect_textures(entry)
    elif isinstance(value, list):
        for entry in value:
            collect_textures(entry)


def validate_textures():
    for path in (ASSETS / "models").rglob("*.json"):
        collect_textures(json.loads(path.read_text(encoding="utf-8")))
    missing = []
    for texture in sorted(TEXTURE_REFERENCES):
        namespace, _, location = texture.partition(":")
        if namespace != NS:
            continue
        if not (SOURCE_TEXTURES / f"{location}.png").exists() and not (ASSETS / "textures" / f"{location}.png").exists():
            missing.append(texture)
    if missing:
        raise SystemExit("missing textures:\n  " + "\n  ".join(missing))


def item_definition_models(definition):
    if definition["type"] == "minecraft:empty":
        return
    if definition["type"] == "minecraft:select":
        for case in definition["cases"]:
            yield from item_definition_models(case["model"])
        if "fallback" in definition:
            yield from item_definition_models(definition["fallback"])
    elif definition["type"] in ("minecraft:special", f"{NS}:special_owner"):
        yield definition["base"]
    elif definition["type"] == "minecraft:condition":
        yield from item_definition_models(definition["on_true"])
        yield from item_definition_models(definition["on_false"])
    elif definition["type"] == "minecraft:range_dispatch":
        for entry in definition["entries"]:
            yield from item_definition_models(entry["model"])
        if "fallback" in definition:
            yield from item_definition_models(definition["fallback"])
    elif definition["type"] == "minecraft:composite":
        for model in definition["models"]:
            yield from item_definition_models(model)
    else:
        yield definition["model"]


def blockstate_models(value):
    if isinstance(value, dict):
        model = value.get("model")
        if isinstance(model, str):
            yield model
        for key, entry in value.items():
            if key != "model":
                yield from blockstate_models(entry)
    elif isinstance(value, list):
        for entry in value:
            yield from blockstate_models(entry)


def validate_models():
    missing = []
    for path in (ASSETS / "items").glob("*.json"):
        for model in item_definition_models(json.loads(path.read_text(encoding="utf-8"))["model"]):
            namespace, _, location = model.partition(":")
            if namespace == NS and not (ASSETS / "models" / f"{location}.json").exists():
                missing.append(f"{path.name}: {model}")
    for path in (ASSETS / "blockstates").glob("*.json"):
        for model in blockstate_models(json.loads(path.read_text(encoding="utf-8"))):
            namespace, _, location = model.partition(":")
            if namespace == NS and not (ASSETS / "models" / f"{location}.json").exists():
                missing.append(f"{path.name}: {model}")
    if missing:
        raise SystemExit("missing models:\n  " + "\n  ".join(missing))


GALACTIC_LETTERS = {
    "А": "A", "Б": "B", "В": "V", "Г": "G", "Д": "D", "Е": "E", "Ё": "E", "Ж": "X", "З": "Z", "И": "I", "Й": "J",
    "К": "K", "Л": "L", "М": "M", "Н": "N", "О": "O", "П": "P", "Р": "R", "С": "S", "Т": "T", "У": "U", "Ф": "F",
    "Х": "H", "Ц": "C", "Ч": "C", "Ш": "W", "Щ": "W", "Ъ": "Q", "Ы": "Y", "Ь": "Q", "Э": "E", "Ю": "U", "Я": "Y",
}


def galactic_font():
    layers = []
    for cyrillic, latin in GALACTIC_LETTERS.items():
        for source, target in ((cyrillic, latin), (cyrillic.lower(), latin.lower())):
            code = ord(target)
            layer = next((grid for grid in layers if code not in grid), None)
            if layer is None:
                layer = {}
                layers.append(layer)
            layer[code] = source
    providers = [{"type": "reference", "id": "minecraft:alt"}]
    for layer in layers:
        rows = ["".join(layer.get(row * 16 + column, "\u0000") for column in range(16)) for row in range(16)]
        providers.append({"type": "bitmap", "file": "minecraft:font/ascii_sga.png", "ascent": 7, "chars": rows})
    write(ASSETS / "font" / "galactic.json", {"providers": providers})


def write_fuels():
    if not profile.fuel_data_map:
        return
    values = {f"{NS}:{item}": ticks for name, ticks in FUELS.items() for item in FUEL_ITEMS[name]}
    write(DATA / "neoforge" / "data_maps" / "item" / "furnace_fuels.json", {"values": {key: {"burn_time": ticks} for key, ticks in values.items()}})


def main():
    if OUT.exists():
        if mcformat.out_overridden and OUT == ROOT:
            raise SystemExit("refusing to clear the repository root")
        shutil.rmtree(OUT)
    world_blocks()
    wood()
    plants()
    cosmetic()
    candles()
    items()
    recipes()
    galactic_font()
    ctx = sys.modules[__name__]
    gen_world.generate(ctx)
    gen_biomes.generate(ctx)
    gen_loot.generate(ctx)
    gen_villages.generate(ctx)
    gen_phase4.generate(ctx)
    gen_phase8.generate(ctx)
    gen_phase5.generate(ctx)
    gen_phase6.generate(ctx)
    gen_phase9_devices.generate(ctx)
    gen_phase9_warded.generate(ctx)
    gen_phase9_machines.generate(ctx)
    gen_phase9_mirrors.generate(ctx)
    gen_phase10_11.generate(ctx)
    gen_phase12.generate(ctx)
    gen_phase13.generate(ctx)
    gen_warp_shaders.generate(mcformat.profile.name, OUT)
    gen_node_shaders.generate(mcformat.profile.name, OUT)
    gen_phase5.recipes(ctx)
    write_fuels()
    write_tags()
    validate_textures()
    validate_models()
    count = sum(1 for _ in OUT.rglob("*.json"))
    print(f"generated {count} files")


if __name__ == "__main__":
    main()
