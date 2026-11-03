TIERS = ["common", "uncommon", "rare"]

APPRENTICE_ELEMENTS = ["air", "earth", "fire", "water", "order", "entropy"]

COMMON_ITEMS = ["common_loot_bag", "thaumium_ingot", "amber"]
UNCOMMON_ITEMS = ["uncommon_loot_bag", "mundane_amulet", "mundane_ring", "mundane_belt", "knowledge_fragment"]
RARE_ITEMS = [
    "rare_loot_bag", "thaumonomicon", "thaumium_sword", "thaumium_pickaxe", "thaumium_axe", "thaumium_hoe",
    "protection_ring", "vis_stone",
] + [f"apprentice_ring_{element}" for element in APPRENTICE_ELEMENTS]

CHESTS = [
    ("simple_dungeon", "minecraft:chests/simple_dungeon", 8, 8, 120, 5, 4, False),
    ("jungle_temple", "minecraft:chests/jungle_temple", 2, 7, 73, 5, 4, False),
    ("desert_pyramid", "minecraft:chests/desert_pyramid", 2, 7, 73, 5, 4, False),
    ("abandoned_mineshaft", "minecraft:chests/abandoned_mineshaft", 3, 7, 80, 4, 3, False),
    ("stronghold_corridor", "minecraft:chests/stronghold_corridor", 2, 4, 99, 4, 3, False),
    ("stronghold_crossing", "minecraft:chests/stronghold_crossing", 1, 5, 62, 4, 3, False),
    ("stronghold_library", "minecraft:chests/stronghold_library", 1, 5, 44, 4, 3, True),
]

BLACKSMITH_TARGETS = [
    "minecraft:chests/village/village_weaponsmith",
    "minecraft:chests/village/village_toolsmith",
    "minecraft:chests/village/village_armorer",
]


def count_function(minimum, maximum):
    return {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": minimum, "max": maximum}}


def box(from_corner, to_corner, top, side):
    return {
        "from": from_corner,
        "to": to_corner,
        "faces": {
            "down": {"texture": top},
            "up": {"texture": top},
            "north": {"texture": side},
            "south": {"texture": side},
            "west": {"texture": side},
            "east": {"texture": side},
        },
    }


def generate(ctx):
    models(ctx)
    drops(ctx)
    chests(ctx)


def models(ctx):
    for kind, elements in [
        ("urn", [([3, 0, 3], [13, 1, 13]), ([2, 1, 2], [14, 13, 14]), ([4, 13, 4], [12, 16, 12])]),
        ("crate", [([1, 0, 1], [15, 14, 15])]),
    ]:
        name = f"loot_{kind}"
        variants = {}
        for index, tier in enumerate(TIERS):
            model_name = f"{name}_{tier}"
            ctx.block_model(model_name, {
                "textures": {
                    "particle": ctx.ref(f"{kind}_side_{index}"),
                    "top": ctx.ref(f"{kind}_top"),
                    "side": ctx.ref(f"{kind}_side_{index}"),
                },
                "elements": [box(low, high, "#top", "#side") for low, high in elements],
            })
            variants[f"tier={tier}"] = {"model": ctx.ref(model_name)}
        ctx.blockstate(name, {"variants": variants})
        ctx.block_item(name, model=ctx.ref(f"{name}_common"))
    ctx.mineable("shovel", "loot_urn")
    ctx.mineable("axe", "loot_crate")


def drops(ctx):
    ctx.write_loot("loot_urn", [])
    ctx.write_loot("loot_crate", [])


def available(ctx, names):
    existing = ctx.ITEM_IDS
    return [name for name in names if name in existing]


def item_entry(ctx, name, weight, minimum, maximum):
    entry = {"type": "minecraft:item", "name": f"{ctx.NS}:{name}", "weight": weight}
    if (minimum, maximum) != (1, 1):
        entry["functions"] = [count_function(minimum, maximum)]
    return entry


def injection(ctx, name, targets, minimum_rolls, maximum_rolls, vanilla_weight, items):
    present = {}
    missing_weight = 0
    for item_name, weight, minimum, maximum in items:
        if available(ctx, [item_name]):
            present.setdefault(item_name, []).append((weight, minimum, maximum))
        else:
            missing_weight += weight
    entries = []
    for item_name, variants in present.items():
        for weight, minimum, maximum in variants:
            entries.append(item_entry(ctx, item_name, weight, minimum, maximum))
    entries.append({"type": "minecraft:empty", "weight": vanilla_weight + missing_weight})
    rolls = minimum_rolls if minimum_rolls == maximum_rolls else {"type": "minecraft:uniform", "min": minimum_rolls, "max": maximum_rolls}
    ctx.write(ctx.DATA / ctx.NS / ctx.profile.loot_folder / "inject" / f"{name}.json", ctx.profile.loot_table({
        "type": "minecraft:chest",
        "pools": [{"rolls": rolls, "entries": entries}],
    }))
    for index, target in enumerate(targets):
        modifier_name = name if len(targets) == 1 else f"{name}_{index}"
        ctx.INJECTIONS.append(f"{ctx.NS}:{modifier_name}")
        ctx.write(
            ctx.DATA / ctx.NS / "loot_modifiers" / f"{modifier_name}.json",
            ctx.profile.loot_modifier(modifier_name, target, f"{ctx.NS}:inject/{name}"),
        )


def chests(ctx):
    for name, target, minimum_rolls, maximum_rolls, vanilla_weight, common_weight, uncommon_weight, library in CHESTS:
        items = []
        items += [(item, common_weight, 1, 3) for item in COMMON_ITEMS]
        items += [(item, uncommon_weight, 1, 2) for item in UNCOMMON_ITEMS]
        items += [(item, 1, 1, 1) for item in RARE_ITEMS]
        if library:
            items.append(("knowledge_fragment", 20, 3, 6))
        injection(ctx, name, [target], minimum_rolls, maximum_rolls, vanilla_weight, items)
    injection(ctx, "village_blacksmith", BLACKSMITH_TARGETS, 3, 9, 94, [("thaumium_ingot", 10, 1, 3)])
