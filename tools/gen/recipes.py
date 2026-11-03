import json

from .blocks import COLORS
from .common import NS, TOOLS

TC_RECIPES = TOOLS / "data" / "tc_recipes.json"


def not_tag_empty(tag):
    return [{"type": "neoforge:not", "value": {"type": "neoforge:tag_empty", "tag": tag}}]


def recipes(ctx):
    tc = lambda name: f"{NS}:{name}"
    for source, nugget, ingot in [
        ("thaumium_nugget", "thaumium_nugget", "thaumium_ingot"),
        ("void_metal_nugget", "void_metal_nugget", "void_metal_ingot"),
        ("quicksilver_drop", "quicksilver_drop", "quicksilver"),
    ]:
        ctx.shapeless(f"{nugget}_from_{ingot}", tc(nugget), 9, [tc(ingot)])
        ctx.shaped(f"{ingot}_from_{nugget}", tc(ingot), 1, ["###", "###", "###"], {"#": tc(nugget)})
    for metal in ["tin", "silver", "lead"]:
        ctx.shapeless(
            f"{metal}_nugget_from_ingot", tc(f"{metal}_nugget"), 9, [f"#c:ingots/{metal}"],
            conditions=not_tag_empty(f"c:ingots/{metal}"),
        )
    ctx.shapeless("quicksilver_from_shimmerleaf", tc("quicksilver"), 1, [tc("shimmerleaf")])
    ctx.shapeless("blaze_powder_from_cinderpearl", "minecraft:blaze_powder", 1, [tc("cinderpearl")])

    for kind in ["greatwood", "silverwood"]:
        ctx.shapeless(f"{kind}_planks", tc(f"{kind}_planks"), 4, [tc(f"{kind}_log")], "building")
        ctx.shaped(f"{kind}_stairs", tc(f"{kind}_stairs"), 4, ["K  ", "KK ", "KKK"], {"K": tc(f"{kind}_planks")}, "building")
        ctx.shaped(f"{kind}_slab", tc(f"{kind}_slab"), 6, ["KKK"], {"K": tc(f"{kind}_planks")}, "building")

    ctx.shaped("thaumium_block", tc("thaumium_block"), 1, ["KKK", "KKK", "KKK"], {"K": "#c:ingots/thaumium"}, "building")
    ctx.shapeless("thaumium_ingot_from_block", tc("thaumium_ingot"), 9, [tc("thaumium_block")])
    ctx.shaped("tallow_block", tc("tallow_block"), 1, ["KKK", "KKK", "KKK"], {"K": tc("magic_tallow")}, "building")
    ctx.shapeless("magic_tallow_from_block", tc("magic_tallow"), 9, [tc("tallow_block")])
    ctx.shaped("amber_block", tc("amber_block"), 1, ["##", "##"], {"#": tc("amber")}, "building")
    ctx.shaped("amber_bricks", tc("amber_bricks"), 4, ["##", "##"], {"#": tc("amber_block")}, "building")
    ctx.shapeless("amber_from_block", tc("amber"), 4, [tc("amber_block")])
    ctx.shapeless("amber_from_bricks", tc("amber"), 4, [tc("amber_bricks")])
    ctx.shaped("obsidian_tile", tc("obsidian_tile"), 4, ["##", "##"], {"#": "minecraft:obsidian"}, "building")
    ctx.shaped("arcane_stone_bricks", tc("arcane_stone_bricks"), 4, ["SS", "SS"], {"S": tc("arcane_stone")}, "building")
    ctx.shaped("arcane_stone_stairs", tc("arcane_stone_stairs"), 4, ["K  ", "KK ", "KKK"], {"K": tc("arcane_stone_bricks")}, "building")
    ctx.shaped("arcane_stone_slab", tc("arcane_stone_slab"), 6, ["KKK"], {"K": tc("arcane_stone_bricks")}, "building")

    ctx.shaped("white_tallow_candle", tc("white_tallow_candle"), 3, [" S ", " T ", " T "], {"S": "minecraft:string", "T": tc("magic_tallow")})
    for color in COLORS[1:]:
        ctx.shapeless(f"{color}_tallow_candle", tc(f"{color}_tallow_candle"), 1, [f"#c:dyes/{color}", tc("white_tallow_candle")])
    ctx.shapeless("white_tallow_candle_from_bone_meal", tc("white_tallow_candle"), 1, ["minecraft:bone_meal", f"#{NS}:tallow_candles"])

    ctx.shaped("glass_phial", tc("glass_phial"), 8, [" C ", "G G", " G "], {"G": "minecraft:glass", "C": "minecraft:clay_ball"})
    for index, nuggets in enumerate([
        ["beef_nugget", "chicken_nugget", "pork_nugget"],
        ["beef_nugget", "chicken_nugget", "fish_nugget"],
        ["beef_nugget", "fish_nugget", "pork_nugget"],
        ["fish_nugget", "chicken_nugget", "pork_nugget"],
    ]):
        ctx.shapeless(f"triple_meat_treat_{index}", tc("triple_meat_treat"), 1, ["minecraft:sugar", *[tc(nugget) for nugget in nuggets]])

    ctx.smelting("quicksilver_from_cinnabar_ore", tc("cinnabar_ore"), tc("quicksilver"), 1, 1.0)
    ctx.smelting("amber_from_amber_ore", tc("amber_ore"), tc("amber"), 1, 1.0)
    ctx.smelting("charcoal_from_greatwood_log", tc("greatwood_log"), "minecraft:charcoal", 1, 0.5)
    ctx.smelting("charcoal_from_silverwood_log", tc("silverwood_log"), "minecraft:charcoal", 1, 0.5)
    ctx.smelting("iron_ingot_from_cluster", tc("native_iron_cluster"), "minecraft:iron_ingot", 2, 1.0)
    ctx.smelting("quicksilver_from_cluster", tc("native_cinnabar_cluster"), tc("quicksilver"), 2, 1.0)
    ctx.smelting("gold_ingot_from_cluster", tc("native_gold_cluster"), "minecraft:gold_ingot", 2, 1.0)
    ctx.smelting("copper_ingot_from_cluster", tc("native_copper_cluster"), "minecraft:copper_ingot", 2, 1.0)
    ctx.smelting("salis_mundus_from_balanced_shard", tc("balanced_shard"), tc("salis_mundus"), 1, 1.0)
    ctx.smelting("gold_nugget_from_coin", tc("gold_coin"), "minecraft:gold_nugget", 1, 0.0)


def result_of(content):
    result = content.get("result")
    if isinstance(result, dict):
        return result.get("id") or result.get("item"), result.get("count", 1)
    if isinstance(result, str):
        return result, 1
    return None, 1


def existing_results(ctx):
    found = {}
    for path in sorted((ctx.DATA / NS / ctx.profile.recipe_folder).glob("*.json")):
        identifier, count = result_of(json.loads(path.read_text(encoding="utf-8")))
        if identifier:
            found.setdefault((identifier, count), []).append(path.stem)
    return found


def defined_item(ctx, identifier):
    namespace, _, name = identifier.partition(":")
    return namespace != NS or name in ctx.ITEM_IDS


def ingredient_ids(value):
    return value if isinstance(value, list) else [value]


def ingredient(ctx, value):
    if isinstance(value, list):
        return [ctx.profile.recipe_ingredient(option) for option in value] if len(value) > 1 else ctx.profile.recipe_ingredient(value[0])
    return ctx.profile.recipe_ingredient(value)


def components_of(result):
    data = result.get("data")
    if not data:
        return None
    return {f"{NS}:wand_parts": {"cap": data["cap"], "rod": data["rod"]}}


def tc_recipes(ctx):
    entries = json.loads(TC_RECIPES.read_text(encoding="utf-8"))
    existing = existing_results(ctx)
    problems = []
    for entry in entries:
        result = entry["result"]
        values = entry["ingredients"].values() if isinstance(entry["ingredients"], dict) else entry["ingredients"]
        flat = [option for value in values for option in ingredient_ids(value)]
        undefined = [identifier for identifier in [result["id"], *flat] if not identifier.startswith("#") and not defined_item(ctx, identifier)]
        if undefined:
            problems.append(f"{entry['id']} uses undefined items {undefined}")
            continue
        duplicates = existing.get((result["id"], result["count"]), [])
        if entry.get("alias"):
            if entry["id"] not in duplicates:
                problems.append(f"alias {entry['key']} -> {entry['id']} does not exist")
            continue
        if duplicates:
            problems.append(f"{entry['id']} duplicates {duplicates}; add an alias in tools/data/tc_recipes.json")
            continue
        if (ctx.DATA / NS / ctx.profile.recipe_folder / f"{entry['id']}.json").exists():
            problems.append(f"recipe file {entry['id']} already exists")
            continue
        if entry["kind"] == "vanilla_shaped":
            content = {"type": "minecraft:crafting_shaped"}
            ctx.profile.recipe_category(content, "misc")
            content["key"] = {symbol: ingredient(ctx, value) for symbol, value in entry["ingredients"].items()}
            content["pattern"] = entry["pattern"]
        else:
            content = {"type": "minecraft:crafting_shapeless"}
            ctx.profile.recipe_category(content, "misc")
            content["ingredients"] = [ingredient(ctx, value) for value in entry["ingredients"]]
        content["result"] = ctx.profile.recipe_result(result["id"], result["count"], components_of(result))
        ctx.recipe(entry["id"], content, [result["id"], *flat])
    if problems:
        raise SystemExit("tc recipes:\n  " + "\n  ".join(problems))
