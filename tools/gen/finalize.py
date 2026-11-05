import json

from .common import NS

FUELS = {"alumentum": 6400, "magical_log": 400}
FUEL_ITEMS = {"alumentum": ["alumentum"], "magical_log": ["greatwood_log", "silverwood_log"]}


def tag_entries(values):
    return [{"id": value, "required": False} if value.startswith(f"{NS}:") else value for value in sorted(values)]


def write_tags(ctx):
    if ctx.profile.name == "26.3":
        ctx.block_tags["minecraft:washed_away_by_fluids"].update({f"{NS}:purifying_fluid", f"{NS}:liquid_death"})
    for tag, values in ctx.block_tags.items():
        namespace, path = ctx.profile.tag_name(tag)
        ctx.write(ctx.DATA / namespace / ctx.profile.block_tag_folder / f"{path}.json", {"replace": False, "values": tag_entries(values)})
    for tag, values in ctx.item_tags.items():
        namespace, path = ctx.profile.tag_name(tag)
        ctx.write(ctx.DATA / namespace / ctx.profile.item_tag_folder / f"{path}.json", {"replace": False, "values": tag_entries(values)})


def collect_textures(value, found):
    if isinstance(value, dict):
        for key, entry in value.items():
            if key == "textures" and isinstance(entry, dict):
                found.update(texture for texture in entry.values() if isinstance(texture, str) and not texture.startswith("#"))
            else:
                collect_textures(entry, found)
    elif isinstance(value, list):
        for entry in value:
            collect_textures(entry, found)


def validate_textures(ctx):
    found = set()
    for path in (ctx.ASSETS / "models").rglob("*.json"):
        collect_textures(json.loads(path.read_text(encoding="utf-8")), found)
    missing = []
    for texture in sorted(found):
        namespace, _, location = texture.partition(":")
        if namespace != NS:
            continue
        if not (ctx.SOURCE_TEXTURES / f"{location}.png").exists() and not (ctx.ASSETS / "textures" / f"{location}.png").exists():
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


def validate_models(ctx):
    missing = []
    for path in (ctx.ASSETS / "items").glob("*.json"):
        for model in item_definition_models(json.loads(path.read_text(encoding="utf-8"))["model"]):
            namespace, _, location = model.partition(":")
            if namespace == NS and not (ctx.ASSETS / "models" / f"{location}.json").exists():
                missing.append(f"{path.name}: {model}")
    for path in (ctx.ASSETS / "blockstates").glob("*.json"):
        for model in blockstate_models(json.loads(path.read_text(encoding="utf-8"))):
            namespace, _, location = model.partition(":")
            if namespace == NS and not (ctx.ASSETS / "models" / f"{location}.json").exists():
                missing.append(f"{path.name}: {model}")
    if missing:
        raise SystemExit("missing models:\n  " + "\n  ".join(missing))


def write_fuels(ctx):
    if not ctx.profile.fuel_data_map:
        return
    values = {f"{NS}:{item}": ticks for name, ticks in FUELS.items() for item in FUEL_ITEMS[name]}
    ctx.write(ctx.DATA / "neoforge" / "data_maps" / "item" / "furnace_fuels.json", {"values": {key: {"burn_time": ticks} for key, ticks in values.items()}})
