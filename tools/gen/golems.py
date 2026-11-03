TYPES = ["straw", "wood", "tallow", "clay", "flesh", "stone", "iron", "thaumium"]
CORES = {
    "fill": "fill", "empty": "empty", "gather": "gather", "harvest": "harvest", "guard": "guard",
    "decanting": "liquid", "alchemy": "essentia", "chop": "lumber", "use": "use", "butcher": "butcher",
    "sorting": "sorting", "fishing": "fish", "blank": "blank",
}
DECORATIONS = {
    "top_hat": "golemdecotophat", "spectacles": "golemdecoglasses", "bowtie": "golemdecobowtie",
    "fez": "golemdecofez", "dart_launcher": "golemdecodart", "visor": "golemdecovisor",
    "iron_plating": "golemdecoarmor", "mace_arm": "golemdecomace",
}


def icon(ctx, name, texture):
    ctx.item_model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": ctx.ref(texture, "item")}})
    ctx.item_definition(name, ctx.ref(name, "item"))


def condition(property_name, model):
    return {
        "type": "minecraft:condition", "property": f"thaumcraft:{property_name}",
        "on_true": {"type": "minecraft:model", "model": model}, "on_false": {"type": "minecraft:empty"},
    }


def box(start, end, offset, size):
    u, v = offset
    x, y, z = size

    def face(a, b, c, d):
        return {"uv": [a / 4, b / 4, c / 4, d / 4], "texture": "#trunk"}

    return {"from": start, "to": end, "faces": {
        "up": face(u + z, v, u + z + x, v + z),
        "down": face(u + z + x, v, u + z + 2 * x, v + z),
        "north": face(u + z, v + z, u + z + x, v + z + y),
        "south": face(u + 2 * z + x, v + z, u + 2 * z + 2 * x, v + z + y),
        "west": face(u, v + z, u + z, v + z + y),
        "east": face(u + z + x, v + z, u + 2 * z + x, v + z + y),
    }}


def generate(ctx):
    for kind in TYPES:
        name = f"{kind}_golem"
        icon(ctx, name, f"golem_{kind}")
        if ctx.mcformat.profile.name == "26.3":
            ctx.write(ctx.ASSETS / "items" / f"{name}.json", {"model": {
                "type": "minecraft:composite", "models": [
                    {"type": "minecraft:model", "model": ctx.ref(name, "item")},
                    condition("golem_advanced", ctx.ref("golem_advanced_overlay", "item")),
                    condition("golem_has_core", ctx.ref("golem_core_overlay", "item")),
                ],
            }})
    icon(ctx, "golem_advanced_overlay", "golem_over_adv")
    icon(ctx, "golem_core_overlay", "golem_over_core")
    for mode, texture in CORES.items():
        icon(ctx, f"golem_core_{mode}", f"golem_core_{texture}")
    for element in ["air", "earth", "fire", "water", "order", "entropy"]:
        icon(ctx, f"golem_upgrade_{element}", f"golem_upgrade_{element}")
    for name, texture in DECORATIONS.items():
        icon(ctx, name, texture)
    icon(ctx, "golem_bell", "ironbell")
    ctx.item_model("traveling_trunk", {
        "parent": "minecraft:block/block", "gui_light": "side",
        "textures": {"trunk": "thaumcraft:model/trunk", "particle": "thaumcraft:model/trunk"},
        "elements": [
            box([1, 0, 1], [15, 10, 15], (0, 19), (14, 10, 14)),
            box([1, 9, 1], [15, 14, 15], (0, 0), (14, 5, 14)),
            box([7, 7, 0], [9, 11, 1], (0, 0), (2, 4, 1)),
        ],
    })
    ctx.item_definition("traveling_trunk", ctx.ref("traveling_trunk", "item"))
