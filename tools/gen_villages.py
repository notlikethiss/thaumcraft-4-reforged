import json
from pathlib import Path

import gen_structures

PRIMALS = ["air", "fire", "water", "earth", "order", "entropy"]
APPRENTICE_RINGS = ["air", "earth", "fire", "water", "order", "entropy"]
MAX_USES = 7
XP = 5
TOWER_CHEST_CONTENTS = [
    ("minecraft:glowstone_dust", 1, 3, 3),
    ("minecraft:glass_bottle", 1, 5, 10),
    ("minecraft:gold_nugget", 1, 3, 5),
    ("minecraft:fire_charge", 1, 1, 5),
    ("minecraft:skeleton_skull", 1, 1, 3),
    ("thaumcraft:knowledge_fragment", 1, 3, 20),
    ("thaumcraft:alumentum", 1, 1, 5),
    ("thaumcraft:nitor", 1, 1, 5),
    ("thaumcraft:thaumium_ingot", 1, 2, 5),
    ("thaumcraft:thaumonomicon", 1, 1, 20),
]


def uniform(minimum, maximum):
    if minimum == maximum:
        return minimum
    return {"type": "minecraft:uniform", "min": minimum, "max": maximum}


def stack(item, minimum, maximum):
    if minimum == maximum:
        return {"id": item, "count": minimum} if minimum > 1 else {"id": item}
    return {"id": item, "count": uniform(minimum, maximum)}


def generate(ctx):
    ns = ctx.NS
    available = {path.stem for path in (ctx.ASSETS / "items").glob("*.json")}

    def exists(item):
        namespace, name = item.split(":")
        return namespace == "minecraft" or name in available

    def tc(name):
        return f"{ns}:{name}"

    def trades_for(profession, definitions):
        names = []
        for name, wants, gives in definitions:
            if not exists(wants[0]) or not exists(gives[0]):
                continue
            content = {
                "wants": stack(*wants),
                "gives": stack(gives[0], 1, 1),
                "max_uses": MAX_USES,
                "xp": XP,
                "reputation_discount": 0.05,
            }
            if gives[1] != 1 or gives[2] != 1:
                content["given_item_modifier"] = {"type": "minecraft:set_count", "count": uniform(gives[1], gives[2])}
            ctx.write(ctx.DATA / ns / "villager_trade" / profession / f"{name}.json", ctx.profile.villager_trade(content))
            names.append(f"{ns}:{profession}/{name}")
        ctx.write(ctx.DATA / ns / "tags" / "villager_trade" / profession / "pool.json", {"values": names})
        for level in range(1, 6):
            ctx.write(ctx.DATA / ns / "trade_set" / profession / f"level_{level}.json", {
                "amount": min(2, len(names)),
                "random_sequence": f"{ns}:trade_set/{profession}/level_{level}",
                "trades": f"#{ns}:{profession}/pool",
            })

    emerald = "minecraft:emerald"
    wizard = [
        ("gold_coin_emerald", (tc("gold_coin"), 20, 22), (emerald, 1, 1)),
        ("emerald_knowledge_fragment", (emerald, 1, 1), (tc("knowledge_fragment"), 1, 1)),
        ("quicksilver_emerald", (tc("quicksilver"), 4, 6), (emerald, 1, 1)),
        ("emerald_alumentum", (emerald, 1, 1), (tc("alumentum"), 1, 1)),
        ("amber_emerald", (tc("amber"), 4, 6), (emerald, 1, 1)),
        ("emerald_nitor", (emerald, 1, 1), (tc("nitor"), 1, 1)),
        ("chicken_nugget_emerald", (tc("chicken_nugget"), 24, 31), (emerald, 1, 1)),
        ("book_knowledge_fragment", ("minecraft:book", 4, 6), (tc("knowledge_fragment"), 1, 1)),
        ("beef_nugget_emerald", (tc("beef_nugget"), 24, 31), (emerald, 1, 1)),
    ]
    wizard += [(f"emerald_{element}_shard", (emerald, 1, 1), (tc(f"{element}_shard"), 2, 3)) for element in PRIMALS]
    wizard += [
        ("emerald_mana_bean", (emerald, 1, 1), (tc("mana_bean"), 1, 2)),
        ("emerald_bath_salts", (emerald, 5, 7), (tc("bath_salts"), 1, 1)),
        ("emerald_protection_ring", (emerald, 5, 7), (tc("protection_ring"), 1, 1)),
        ("emerald_vis_stone", (emerald, 5, 7), (tc("vis_stone"), 1, 1)),
    ]
    wizard += [(f"emerald_apprentice_ring_{element}", (emerald, 5, 7), (tc(f"apprentice_ring_{element}"), 1, 1)) for element in APPRENTICE_RINGS]
    trades_for("wizard", wizard)

    coin = tc("gold_coin")
    banker = [
        ("gold_coin_emerald", (coin, 20, 22), (emerald, 1, 1)),
        ("gold_coin_arrow", (coin, 2, 3), ("minecraft:arrow", 1, 1)),
        ("gold_coin_white_wool", (coin, 6, 8), ("minecraft:white_wool", 1, 1)),
        ("gold_coin_paper", (coin, 3, 4), ("minecraft:paper", 1, 1)),
        ("gold_coin_book", (coin, 7, 9), ("minecraft:book", 1, 1)),
        ("gold_coin_experience_bottle", (coin, 16, 20), ("minecraft:experience_bottle", 1, 1)),
        ("gold_coin_glowstone", (coin, 9, 12), ("minecraft:glowstone", 1, 1)),
        ("gold_coin_coal", (coin, 2, 3), ("minecraft:coal", 1, 1)),
        ("gold_coin_diamond", (coin, 22, 24), ("minecraft:diamond", 1, 1)),
        ("gold_coin_iron_ingot", (coin, 6, 8), ("minecraft:iron_ingot", 1, 1)),
        ("gold_coin_thaumium_ingot", (coin, 10, 12), (tc("thaumium_ingot"), 1, 1)),
        ("gold_coin_saddle", (coin, 25, 32), ("minecraft:saddle", 1, 1)),
    ]
    trades_for("banker", banker)

    entries = []
    for item, minimum, maximum, weight in TOWER_CHEST_CONTENTS:
        if not exists(item):
            continue
        entry = {"type": "minecraft:item", "name": item, "weight": weight}
        if maximum > minimum:
            entry["modifier"] = {"type": "minecraft:set_count", "count": uniform(minimum, maximum)}
        entries.append(entry)
    ctx.write(ctx.DATA / ns / ctx.profile.loot_folder / "chests" / "wizard_tower.json", ctx.profile.loot_table({
        "type": "minecraft:chest",
        "pools": [{"rolls": uniform(4, 9), "entries": entries}],
        "random_sequence": f"{ns}:chests/wizard_tower",
    }))

    gen_structures.generate(ctx.DATA)
