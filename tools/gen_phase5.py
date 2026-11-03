import json
import shutil
from pathlib import Path

NS = "thaumcraft"
ROOT = Path(__file__).resolve().parent.parent
RECIPES = ROOT / "tools" / "tc_recipes.json"
RESEARCH_TINT = {"type": f"{NS}:research_note"}
WHITE = -1
DIRECTIONS = {"east": 0, "south": 90, "west": 180, "north": 270}


def mc_box(origin, size, uv, texture_size, texture="#t"):
    rx, ry, rz = origin
    w, h, d = size
    u, v = uv
    tw, th = texture_size
    scale_u = 16 / tw
    scale_v = 16 / th

    def rect(x1, y1, x2, y2):
        return [round(x1 * scale_u, 4), round(y1 * scale_v, 4), round(x2 * scale_u, 4), round(y2 * scale_v, 4)]

    def face(*coordinates):
        return {"uv": rect(*coordinates), "texture": texture}

    return {
        "from": [8 + rx, 16 - ry - h, 8 - rz - d],
        "to": [8 + rx + w, 16 - ry, 8 - rz],
        "faces": {
            "up": face(u + d, v, u + d + w, v + d),
            "down": face(u + d + w, v, u + d + 2 * w, v + d),
            "west": face(u, v + d, u + d, v + d + h),
            "east": face(u + d + w, v + d, u + 2 * d + w, v + d + h),
            "south": face(u + d, v + d, u + d + w, v + d + h),
            "north": face(u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
        },
    }


def copy_texture(ctx, source, name):
    target = ctx.ASSETS / "textures" / "block" / f"{name}.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(ctx.SOURCE_TEXTURES / "model" / f"{source}.png", target)


def tables(ctx):
    copy_texture(ctx, "table", "table_model")
    copy_texture(ctx, "restable", "research_table_model")
    ctx.block_model("table", {
        "parent": "minecraft:block/block",
        "textures": {"particle": ctx.ref("table_model"), "t": ctx.ref("table_model")},
        "elements": [
            mc_box((-8, 0, -8), (16, 4, 16), (0, 0), (64, 32)),
            mc_box((2, 4, -2), (4, 8, 4), (0, 20), (64, 32)),
            mc_box((-6, 4, -2), (4, 8, 4), (0, 20), (64, 32)),
            mc_box((-8, 12, -4), (16, 4, 8), (16, 20), (64, 32)),
        ],
    })
    ctx.blockstate("table", {"variants": {
        "axis=x": {"model": ctx.ref("table")},
        "axis=z": {"model": ctx.ref("table"), "y": 90},
    }})
    ctx.block_item("table")
    ctx.self_drop("table")

    ctx.block_model("research_table", {
        "parent": "minecraft:block/block",
        "textures": {"particle": ctx.ref("research_table_model"), "t": ctx.ref("research_table_model")},
        "elements": [
            mc_box((-8, 0, -8), (32, 4, 16), (0, 0), (128, 64)),
            mc_box((-6, 4, -6), (4, 12, 4), (0, 24), (128, 64)),
            mc_box((-6, 4, 2), (4, 12, 4), (0, 24), (128, 64)),
            mc_box((18, 4, -6), (4, 12, 4), (0, 24), (128, 64)),
            mc_box((18, 4, 2), (4, 12, 4), (0, 24), (128, 64)),
            mc_box((-4, 10, -2), (24, 4, 4), (24, 24), (128, 64)),
        ],
    })
    ctx.block_model("research_table_part", {
        "parent": "minecraft:block/block",
        "textures": {"particle": ctx.ref("research_table_model")},
    })
    ctx.blockstate("research_table", {"variants": {
        f"facing={name}": {"model": ctx.ref("research_table"), "y": rotation} if rotation else {"model": ctx.ref("research_table")}
        for name, rotation in DIRECTIONS.items()
    }})
    ctx.blockstate("research_table_part", {"variants": {
        f"facing={name}": {"model": ctx.ref("research_table_part")} for name in DIRECTIONS
    }})
    for name in ["research_table", "research_table_part"]:
        ctx.write_loot(name, [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"{NS}:table"}],
            "condition": {"type": "minecraft:survives_explosion"},
        }])
    ctx.mineable("axe", "table", "research_table", "research_table_part")


def items(ctx):
    ctx.generated_item("scribing_tools", "inkwell")
    white = ctx.tint(0xFFFFFF)
    ctx.generated_item("research_notes", ["researchnotes", "researchnotesoverlay"], [white, RESEARCH_TINT])
    ctx.generated_item("discovery", ["discovery", "discoveryoverlay"], [white, RESEARCH_TINT])
    ctx.generated_item("thaumonomicon", "thaumonomicon")
    ctx.generated_item("thaumonomicon_cheat", "thaumonomiconcheat")


def result_of(content):
    result = content.get("result")
    if isinstance(result, dict):
        return result.get("id") or result.get("item"), result.get("count", 1)
    if isinstance(result, str):
        return result, 1
    return None, 1


def existing_results(ctx):
    folder = ctx.DATA / NS / ctx.profile.recipe_folder
    found = {}
    if folder.exists():
        for path in sorted(folder.glob("*.json")):
            identifier, count = result_of(json.loads(path.read_text(encoding="utf-8")))
            if identifier:
                found.setdefault((identifier, count), []).append(path.stem)
    return found


def defined_item(ctx, identifier):
    namespace, _, name = identifier.partition(":")
    if namespace != NS:
        return True
    return (ctx.ASSETS / "items" / f"{name}.json").exists()


def ingredient_ids(value):
    if value is None:
        return []
    return value if isinstance(value, list) else [value]


def ingredient(ctx, value):
    if isinstance(value, list):
        return [ctx.profile.recipe_ingredient(option) for option in value] if len(value) > 1 else ctx.profile.recipe_ingredient(value[0])
    return ctx.profile.recipe_ingredient(value)


def components_of(entry):
    result = entry["result"]
    if result.get("aspects"):
        return False, None
    data = result.get("data")
    if not data:
        return True, None
    if set(data) == {"cap", "rod"}:
        return True, {f"{NS}:wand_parts": {"cap": data["cap"], "rod": data["rod"]}}
    return False, None


def recipes(ctx):
    ctx.recipe("jar_label_filter", {"type": f"{NS}:jar_label"}, [f"{NS}:jar_label", f"{NS}:essentia_phial"])
    entries = json.loads(RECIPES.read_text(encoding="utf-8"))
    existing = existing_results(ctx)
    written = 0
    skipped = []
    problems = []
    for entry in entries:
        if entry.get("custom") or entry.get("incomplete"):
            continue
        result = entry["result"]
        supported, components = components_of(entry)
        referenced = [result["id"]]
        for value in (entry["ingredients"].values() if isinstance(entry["ingredients"], dict) else entry["ingredients"]):
            referenced.extend(option for option in ingredient_ids(value) if not option.startswith("#"))
        if not supported or not all(defined_item(ctx, identifier) for identifier in referenced):
            skipped.append(entry["id"])
            continue
        duplicates = existing.get((result["id"], result["count"]))
        if entry.get("alias"):
            if entry["id"] not in (duplicates or []):
                problems.append(f"alias {entry['key']} -> {entry['id']} does not exist")
            continue
        if duplicates:
            if entry["key"]:
                problems.append(f"{entry['key']} duplicates {duplicates}; add an alias in tools/tc_recipes.json")
            continue
        name = entry["id"]
        if (ctx.DATA / NS / ctx.profile.recipe_folder / f"{name}.json").exists():
            problems.append(f"recipe file {name} already exists")
            continue
        category = "misc"
        if entry["kind"] == "vanilla_shaped":
            content = {"type": "minecraft:crafting_shaped"}
            ctx.profile.recipe_category(content, category)
            content["key"] = {symbol: ingredient(ctx, value) for symbol, value in entry["ingredients"].items()}
            content["pattern"] = entry["pattern"]
            content["result"] = ctx.profile.recipe_result(result["id"], result["count"], components)
            flat = [option for value in entry["ingredients"].values() for option in ingredient_ids(value)]
        else:
            content = {"type": "minecraft:crafting_shapeless"}
            ctx.profile.recipe_category(content, category)
            content["ingredients"] = [ingredient(ctx, value) for value in entry["ingredients"]]
            content["result"] = ctx.profile.recipe_result(result["id"], result["count"], components)
            flat = [option for value in entry["ingredients"] for option in ingredient_ids(value)]
        ctx.recipe(name, content, [result["id"], *flat])
        written += 1
    if problems:
        raise SystemExit("phase 5 recipes:\n  " + "\n  ".join(problems))
    print(f"phase 5 recipes: {written} written, {len(skipped)} waiting for items")


def generate(ctx):
    tables(ctx)
    items(ctx)
