import json

import mcformat

from .common import NS
from .finalize import FUEL_ITEMS, FUELS


def texture_file(ctx, reference):
    if ":" not in reference:
        reference = "minecraft:" + reference
    namespace, path = reference.split(":", 1)
    if namespace != NS:
        return None
    for root in (ctx.ASSETS / "textures", ctx.SOURCE_TEXTURES):
        candidate = root / f"{path}.png"
        if candidate.exists():
            return candidate
    return None


def texture_layer(ctx, reference, cache={}):
    if reference not in cache:
        from PIL import Image
        file = texture_file(ctx, reference)
        layer = "solid"
        if file is not None:
            alpha = Image.open(file).convert("RGBA").getchannel("A")
            histogram = alpha.histogram()
            if any(histogram[1:255]):
                layer = "translucent"
            elif histogram[0]:
                layer = "cutout"
        cache[reference] = layer
    return cache[reference]


def model_textures(ctx, name, seen=None):
    seen = seen or set()
    if name in seen or name not in ctx.BLOCK_MODELS:
        return []
    seen.add(name)
    content = ctx.BLOCK_MODELS[name]
    result = [value for value in content.get("textures", {}).values() if not value.startswith("#")]
    parent = content.get("parent", "")
    if parent.startswith(f"{NS}:block/"):
        result += model_textures(ctx, parent[len(f"{NS}:block/"):], seen)
    return result


def apply_render_types(ctx):
    if not ctx.profile.explicit_render_types:
        return
    order = ["solid", "cutout", "translucent"]
    for name in ctx.BLOCK_MODELS:
        if "render_type" in ctx.BLOCK_MODELS[name]:
            continue
        layers = [texture_layer(ctx, texture) for texture in model_textures(ctx, name)]
        if not layers:
            continue
        layer = max(layers, key=order.index)
        if layer == "solid":
            continue
        path = ctx.ASSETS / "models" / "block" / f"{name}.json"
        content = json.loads(path.read_text(encoding="utf-8"))
        content["render_type"] = f"minecraft:{layer}"
        path.write_text(json.dumps(content, indent=2) + "\n", encoding="utf-8")


SECTIONS = {
    "builtin_entity": "item id -> special renderer. The item model json is parent minecraft:builtin/entity (with the display transforms of a block item), so the item is drawn by a BlockEntityWithoutLevelRenderer: dispatch on the item id, create the renderer named in renderer (26.3 special model type id) with params. base_model is the 26.3 base model the special renderer was layered on (its textures are copied into the builtin/entity json for the particle). Entries with views_by_display_context are items whose renderer takes params.view: in renderByItem use views_by_display_context[ItemDisplayContext name] and fall back to fallback_view when the context is not listed (ground, fixed, head, gui of other kinds).",
    "composite": "item id -> list of parts drawn together. The item json is builtin/entity. The BlockEntityWithoutLevelRenderer must draw parts in order: item_model/block_model is a baked json model to load as an additional model (ModelEvent.RegisterAdditional) and render with the item transform, builtin_entity is a special renderer as in builtin_entity.",
    "tints": "item id -> layer index -> colour source. constant is a fixed RGB for ItemColor of that layer (tintindex = layer index; block items use the tintindex of the block model elements). source is a tint source id: thaumcraft:aspect = colour of the first aspect of the stack component thaumcraft:aspects (white when empty); thaumcraft:research_note = colour of the research note component thaumcraft:research_note (0x999999 when absent). minecraft:dye = DyedItemColor of the stack with the recorded default color. Register one ItemColor per item with these layers.",
    "armor_layers": "armor id -> ArmorMaterial.Layer location and the generated texture.",
    "predicates": "item id -> properties required by model overrides; vanilla bow pull and pulling have the same behavior as minecraft:bow.",
    "model_loaders": "block id -> NeoForge geometry loader and fallback baked model.",
}


def write_manifest(name, content):
    path = mcformat.manifest_dir / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(content, indent=2) + "\n", encoding="utf-8")


def write_extras(ctx):
    if ctx.profile.modern:
        return
    if ctx.INJECTIONS:
        ctx.write(ctx.DATA / ctx.profile.loot_modifier_namespace / "loot_modifiers" / "global_loot_modifiers.json", {"replace": False, "entries": ctx.INJECTIONS})
    values = {f"{NS}:{item}": ticks for name, ticks in FUELS.items() for item in FUEL_ITEMS[name]}
    if ctx.profile.data_maps:
        ctx.write(ctx.DATA / "neoforge" / "data_maps" / "item" / "furnace_fuels.json", {"values": {key: {"burn_time": ticks} for key, ticks in values.items()}})
    write_manifest("fuels.json", values)
    if ctx.profile.pack_mcmeta:
        ctx.write(ctx.OUT / "pack.mcmeta", ctx.profile.pack_mcmeta)


def write_manifests(ctx):
    if ctx.profile.modern:
        return
    manifest = {"minecraft_version": ctx.profile.name, "sections": SECTIONS, "nbt_keys": ctx.NBT_KEYS, **ctx.MANIFEST}
    manifest["notes"] = {"model_elements": ctx.NOTES["model_elements"]}
    write_manifest(f"item_models_{ctx.profile.name}.json", manifest)
    write_manifest("trades.json", {
        "meaning": "villager professions without a trade_set registry; build MerchantOffers in VillagerTradesEvent (or a profession level to ItemListing list). Each profession level 1..5 draws offers_picked_per_level distinct offers from the shared pool (seeded random per villager). wants/gives are item stacks (id, count where count is an int or a minecraft:uniform {min,max} range rolled per offer); given_item_modifier set_count rolls the result count. max_uses, xp and reputation_discount (price multiplier) map to MerchantOffer(maxUses, xp, priceMultiplier). Professions have no job site (point_of_interest is null), the wizard and banker villagers are spawned by the village templates with Xp 1.",
        "professions": ctx.TRADES,
    })
    if ctx.NOTES["recipes"]:
        write_manifest(f"recipe_notes_{ctx.profile.name}.json", ctx.NOTES["recipes"])
