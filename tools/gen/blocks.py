import random

from .common import FACES, NS

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


def world_blocks(ctx):
    for name, texture in [("cinnabar_ore", "cinnibar"), ("amber_ore", "amberore")]:
        ctx.cube_all(name, texture)
        ctx.simple_state(name)
        ctx.block_item(name)
    ctx.self_drop("cinnabar_ore")
    ctx.bonus_drop("amber_ore", f"{NS}:amber")
    ctx.mineable("pickaxe", "cinnabar_ore", "amber_ore")
    ctx.block_tags["minecraft:needs_iron_tool"].update({f"{NS}:cinnabar_ore", f"{NS}:amber_ore"})
    ctx.tag_both("c:ores", "cinnabar_ore", "amber_ore")
    ctx.tag_both("c:ores/cinnabar", "cinnabar_ore")
    ctx.tag_both("c:ores/amber", "amber_ore")

    for element, color in zip(INFUSED_ELEMENTS, INFUSED_COLORS):
        name = f"{element}_infused_stone"
        ctx.block_model(name, {
            "parent": "minecraft:block/block",
            "textures": {"particle": ctx.ref("infusedorestone"), "stone": ctx.ref("infusedorestone"), "ore": ctx.ref("infusedore")},
            "elements": [
                {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                    face: {"texture": "#stone", "cullface": face} for face in FACES
                }},
                {"from": [0, 0, 0], "to": [16, 16, 16], "light_emission": 10, "faces": {
                    face: {"texture": "#ore", "cullface": face, "tintindex": 0} for face in FACES
                }},
            ],
        })
        ctx.simple_state(name)
        ctx.block_item(name, [ctx.tint(color)])
        ctx.write_loot(name, [{
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
        ctx.mineable("pickaxe", name)
        ctx.tag_both("c:ores", name)


def wood(ctx):
    for kind in ["greatwood", "silverwood"]:
        log = f"{kind}_log"
        ctx.cube_column(log, f"{kind}top", f"{kind}side")
        ctx.block_model(f"{log}_horizontal", {
            "parent": "minecraft:block/cube_column_horizontal",
            "textures": {"end": ctx.ref(f"{kind}top"), "side": ctx.ref(f"{kind}side")},
        })
        ctx.blockstate(log, {"variants": {
            "axis=x": {"model": ctx.ref(f"{log}_horizontal"), "x": 90, "y": 90},
            "axis=y": {"model": ctx.ref(log)},
            "axis=z": {"model": ctx.ref(f"{log}_horizontal"), "x": 90},
        }})
        ctx.block_item(log)
        ctx.self_drop(log)
        ctx.mineable("axe", log)
        ctx.tag_both("minecraft:logs", log)
        ctx.tag_both("minecraft:logs_that_burn", log)

        leaves = f"{kind}_leaves"
        ctx.block_model(leaves, {"parent": "minecraft:block/leaves", "textures": {"all": ctx.ref(f"{kind}leaves")}})
        ctx.simple_state(leaves)
        ctx.block_item(leaves, [ctx.tint(FOLIAGE_DEFAULT if kind == "greatwood" else SILVERWOOD_COLOR)])
        ctx.shear_only(leaves)
        ctx.mineable("hoe", leaves)
        ctx.tag_both("minecraft:leaves", leaves)

        sapling = f"{kind}_sapling"
        ctx.block_model(sapling, {"parent": "minecraft:block/cross", "textures": {"cross": ctx.ref(f"{kind}sapling")}})
        ctx.simple_state(sapling)
        ctx.generated_item(sapling, f"{kind}sapling", texture_kind="block")
        ctx.self_drop(sapling)
        ctx.tag_both("minecraft:saplings", sapling)

        planks = f"{kind}_planks"
        ctx.cube_all(planks, f"planks_{kind}")
        ctx.simple_state(planks)
        ctx.block_item(planks)
        ctx.self_drop(planks)
        ctx.slab(f"{kind}_slab", f"planks_{kind}")
        ctx.stairs(f"{kind}_stairs", f"planks_{kind}")
        ctx.mineable("axe", planks, f"{kind}_slab", f"{kind}_stairs")
        ctx.tag_both("minecraft:planks", planks)
        ctx.tag_both("minecraft:wooden_slabs", f"{kind}_slab")
        ctx.tag_both("minecraft:wooden_stairs", f"{kind}_stairs")
        ctx.tag_both("minecraft:slabs", f"{kind}_slab")
        ctx.tag_both("minecraft:stairs", f"{kind}_stairs")


def plants(ctx):
    for name, texture in [("shimmerleaf", "shimmerleaf"), ("cinderpearl", "cinderpearl"), ("vishroom", "manashroom"), ("ethereal_bloom", "purifier_seed")]:
        ctx.block_model(name, {"parent": "minecraft:block/cross", "textures": {"cross": ctx.ref(texture)}})
        ctx.simple_state(name)
        ctx.generated_item(name, texture, texture_kind="block")
        ctx.self_drop(name)

    for age in range(8):
        stem = 0 if age < 2 else (1 if age < 5 else 2)
        ctx.block_model(f"mana_pod_{age}", {"parent": "minecraft:block/cross", "textures": {"cross": ctx.ref(f"manapod_stem_{stem}")}})
    ctx.blockstate("mana_pod", {"variants": {f"age={age}": {"model": ctx.ref(f"mana_pod_{age}")} for age in range(8)}})
    ctx.write_loot("mana_pod", [])


def cosmetic(ctx):
    totem_variants = {}
    for part, model, side in [("base", "obsidian_totem", "obsidiantotembase"), ("shaded", "obsidian_totem_shaded", "obsidiantotembaseshaded")]:
        ctx.cube_column(model, "obsidiantile", side)
        for carving in range(4):
            totem_variants[f"carving={carving},part={part}"] = {"model": ctx.ref(model)}
    for carving in range(4):
        model = f"obsidian_totem_carved_{carving}"
        textures = {"particle": ctx.ref("obsidiantile"), "down": ctx.ref("obsidiantile"), "up": ctx.ref("obsidiantile")}
        for face, side in {"north": 2, "south": 3, "west": 4, "east": 5}.items():
            textures[face] = ctx.ref(f"obsidiantotem{(side + carving) % 4 + 1}")
        ctx.block_model(model, {"parent": "minecraft:block/cube", "textures": textures})
        totem_variants[f"carving={carving},part=carved"] = {"model": ctx.ref(model)}
    ctx.blockstate("obsidian_totem", {"variants": totem_variants})
    ctx.cube_all("obsidian_tile", "obsidiantile")
    ctx.cube_all("travel_paving_stone", "paving_stone_travel")
    ctx.cube_all("warding_paving_stone", "paving_stone_warding")
    ctx.cube_all("thaumium_block", "thaumiumblock")
    ctx.cube_column("tallow_block", "tallowblock_top", "tallowblock")
    ctx.cube_all("arcane_stone", "pedestal_top")
    ctx.cube_all("arcane_stone_bricks", "arcane_stone")
    ctx.cube_all("crusted_stone", "crust")
    ctx.block_model("ancient_stone_pedestal", {
        "parent": "minecraft:block/cube_column",
        "textures": {"end": ctx.ref("es_1"), "side": ctx.ref("es_p")},
    })
    ctx.cube_column("amber_block", "amberblock_top", "amberblock")
    ctx.cube_all("amber_bricks", "amberbrick")
    for name in ["obsidian_tile", "travel_paving_stone", "warding_paving_stone", "thaumium_block", "tallow_block", "arcane_stone",
                 "arcane_stone_bricks", "crusted_stone", "ancient_stone_pedestal", "amber_block", "amber_bricks"]:
        ctx.simple_state(name)

    for index in range(1, 5):
        ctx.cube_all(f"ancient_stone_{index}", f"es_{index}")
        ctx.cube_all(f"ancient_rock_{index}", f"er_{index}")
    for name, prefix in [("ancient_stone", "ancient_stone"), ("ancient_stone_no_spawn", "ancient_stone"), ("ancient_rock", "ancient_rock")]:
        ctx.blockstate(name, {"variants": {"": [{"model": ctx.ref(f"{prefix}_{index}")} for index in range(1, 5)]}})
    ctx.block_item("ancient_stone", model=ctx.ref("ancient_stone_1"))
    ctx.block_item("ancient_stone_no_spawn", model=ctx.ref("ancient_stone_1"))
    ctx.block_item("ancient_rock", model=ctx.ref("ancient_rock_1"))

    for state, top in (("", "golem_stone_top"), ("_active", "golem_stone_top_active")):
        ctx.block_model(f"golem_fetter{state}", {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {"bottom": ctx.ref("arcane_stone"), "top": ctx.ref(top), "side": ctx.ref("golem_stone_side")},
        })
    ctx.blockstate("golem_fetter", {"variants": {
        "active=false": {"model": ctx.ref("golem_fetter")},
        "active=true": {"model": ctx.ref("golem_fetter_active")},
    }})

    items = [
        "obsidian_totem", "obsidian_tile", "travel_paving_stone", "warding_paving_stone", "thaumium_block", "tallow_block",
        "arcane_stone", "arcane_stone_bricks", "golem_fetter", "crusted_stone", "ancient_stone_pedestal", "amber_block", "amber_bricks",
    ]
    for name in items:
        ctx.block_item(name)
    for name in items + ["ancient_stone", "ancient_stone_no_spawn", "ancient_rock"]:
        ctx.self_drop(name)
    ctx.mineable("pickaxe", *items, "ancient_stone", "ancient_stone_no_spawn", "ancient_rock")
    beacon = [
        "obsidian_totem", "obsidian_tile", "travel_paving_stone", "warding_paving_stone", "thaumium_block", "tallow_block",
        "arcane_stone", "arcane_stone_bricks", "golem_fetter", "ancient_stone", "ancient_rock", "ancient_stone_no_spawn",
        "crusted_stone", "ancient_stone_pedestal",
    ]
    for name in beacon:
        ctx.block_tags["minecraft:beacon_base_blocks"].add(f"{NS}:{name}")
    ctx.tag_both("c:storage_blocks/thaumium", "thaumium_block")
    ctx.tag_both("c:storage_blocks", "thaumium_block")

    ctx.slab("arcane_stone_slab", "arcane_stone")
    ctx.slab("ancient_stone_slab", "es_1")
    ctx.stairs("arcane_stone_stairs", "arcane_stone")
    ctx.stairs("ancient_stone_stairs", "es_1")
    ctx.mineable("pickaxe", "arcane_stone_slab", "ancient_stone_slab", "arcane_stone_stairs", "ancient_stone_stairs")
    ctx.tag_both("minecraft:slabs", "arcane_stone_slab", "ancient_stone_slab")
    ctx.tag_both("minecraft:stairs", "arcane_stone_stairs", "ancient_stone_stairs")


def candles(ctx):
    for index, color in enumerate(COLORS):
        name = f"{color}_tallow_candle"
        elements = [{
            "from": [6, 0, 6], "to": [10, 8, 10],
            "faces": {direction: {"texture": "#candle", "tintindex": 0} for direction in FACES},
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
                "faces": {direction: {"texture": "#candle", "tintindex": 0} for direction in FACES},
            })
        elements.append({
            "from": [7.6, 8, 7.6], "to": [8.4, 10, 8.4],
            "faces": {direction: {"texture": "#stub"} for direction in FACES if direction != "down"},
        })
        ctx.block_model(name, {
            "parent": "minecraft:block/block",
            "textures": {"particle": ctx.ref("candle"), "candle": ctx.ref("candle"), "stub": ctx.ref("candlestub")},
            "elements": elements,
        })
        ctx.simple_state(name)
        ctx.block_item(name, [ctx.tint(CANDLE_COLORS[index])])
        ctx.self_drop(name)
        ctx.tag_both(f"{NS}:tallow_candles", name)


def generate(ctx):
    world_blocks(ctx)
    wood(ctx)
    plants(ctx)
    cosmetic(ctx)
    candles(ctx)
