from .common import FACES, NS, SIDES, box, flat, uniform_faces

ELEMENTS = ["air", "fire", "water", "earth", "order", "entropy"]
CRYSTALS = [
    ("air_crystal_cluster", 0),
    ("fire_crystal_cluster", 1),
    ("water_crystal_cluster", 2),
    ("earth_crystal_cluster", 3),
    ("order_crystal_cluster", 4),
    ("entropy_crystal_cluster", 5),
    ("mixed_crystal_cluster", 6),
    ("strange_crystals", 7),
]
LEVITATOR_TOP_GLOW = 0x00A000
LEVITATOR_SIDE_GLOW = 0xDD11FF
LEVITATOR_INNER_GLOW = 0xEECCFF
LEVITATOR_ACTIVE_LIGHT = 11


def airy(ctx):
    for name in ["nitor_light", "arcane_lamp_light", "warding_aura"]:
        ctx.particle_model(name, "blank")
        ctx.simple_state(name)
    ctx.write_loot("nitor_light", [{
        "rolls": 1,
        "entries": [{"type": "minecraft:item", "name": f"{NS}:nitor"}],
        "condition": {"type": "minecraft:survives_explosion"},
    }])
    ctx.block_tags["minecraft:replaceable_by_trees"].update([f"{NS}:arcane_lamp_light", f"{NS}:warding_aura"])


def lamps(ctx):
    for name, top, side in [
        ("arcane_lamp", "lamp_top", "lamp_side"),
        ("lamp_of_growth_lit", "lamp_grow_top", "lamp_grow_side"),
        ("lamp_of_growth_unlit", "lamp_grow_top_off", "lamp_grow_side_off"),
        ("lamp_of_fertility_lit", "lamp_fert_top", "lamp_fert_side"),
        ("lamp_of_fertility_unlit", "lamp_fert_top_off", "lamp_fert_side_off"),
    ]:
        ctx.block_model(name, {
            "parent": "minecraft:block/block",
            "textures": {"particle": ctx.ref(side), "top": ctx.ref(top), "side": ctx.ref(side)},
            "elements": [box([4, 2, 4], [12, 14, 12], {
                "down": {"texture": "#top"}, "up": {"texture": "#top"},
                "north": {"texture": "#side"}, "south": {"texture": "#side"},
                "west": {"texture": "#side"}, "east": {"texture": "#side"},
            })],
        })
    ctx.blockstate("arcane_lamp", {"variants": {f"facing={facing}": {"model": ctx.ref("arcane_lamp")} for facing in FACES}})
    for name in ["lamp_of_growth", "lamp_of_fertility"]:
        ctx.blockstate(name, {"variants": {
            f"facing={facing},lit={lit}": {"model": ctx.ref(f"{name}_{'lit' if lit == 'true' else 'unlit'}")}
            for facing in FACES
            for lit in ["false", "true"]
        }})
    ctx.block_item("arcane_lamp")
    ctx.block_item("lamp_of_growth", model=ctx.ref("lamp_of_growth_lit"))
    ctx.block_item("lamp_of_fertility", model=ctx.ref("lamp_of_fertility_lit"))
    for name in ["arcane_lamp", "lamp_of_growth", "lamp_of_fertility"]:
        ctx.self_drop(name)
        ctx.mineable("pickaxe", name)


def grate(ctx):
    planes = []
    for position, face in [(2.968, "east"), (13.032, "west"), (8.968, "east"), (7.032, "west")]:
        planes.append(flat(face, [position, 13, 0], [position, 16, 16], "#bars"))
    for position, face in [(2.968, "south"), (13.032, "north"), (8.968, "south"), (7.032, "north")]:
        planes.append(flat(face, [0, 13, position], [16, 16, position], "#bars"))
    base = box([0, 13, 0], [16, 16, 16], uniform_faces("#grate"))
    textures = {"particle": ctx.ref("grate"), "grate": ctx.ref("grate"), "bars": ctx.ref("metalbase"), "hatch": ctx.ref("grate_hatch")}
    ctx.block_model("item_grate_open", {"parent": "minecraft:block/block", "textures": textures, "elements": [base, *planes]})
    ctx.block_model("item_grate_closed", {
        "parent": "minecraft:block/block",
        "textures": textures,
        "elements": [base, *planes, box([1, 14, 1], [15, 15, 15], uniform_faces("#hatch"))],
    })
    ctx.blockstate("item_grate", {"variants": {
        "closed=false": {"model": ctx.ref("item_grate_open")},
        "closed=true": {"model": ctx.ref("item_grate_closed")},
    }})
    ctx.block_item("item_grate", model=ctx.ref("item_grate_closed"))
    ctx.self_drop("item_grate")
    ctx.mineable("pickaxe", "item_grate")


def ear(ctx):
    def part(start, end, top, bottom, side):
        faces = {"down": {"texture": bottom}, "up": {"texture": top}}
        faces.update({face: {"texture": side} for face in SIDES})
        return box(start, end, faces)

    for name, top in [("arcane_ear_off", "arcaneeartopoff"), ("arcane_ear_on", "arcaneeartopon")]:
        elements = [
            part([0, 0, 0], [16, 3, 16], "#top", "#bottom", "#side"),
            part([4, 3, 4], [12, 16, 12], "#top", "#bottom", "#side"),
        ]
        for start, end in [
            ([4, 8, 1], [12, 16, 3]),
            ([5, 8, 3], [11, 15, 4]),
            ([1, 8, 4], [3, 16, 12]),
            ([3, 8, 5], [4, 15, 11]),
            ([4, 8, 13], [12, 16, 15]),
            ([5, 8, 12], [11, 15, 13]),
            ([13, 8, 4], [15, 16, 12]),
            ([12, 8, 5], [13, 15, 11]),
        ]:
            elements.append(part(start, end, "#bell_top", "#bell_top", "#bell_side"))
        ctx.block_model(name, {
            "parent": "minecraft:block/block",
            "textures": {
                "particle": ctx.ref("arcaneearsideon"),
                "top": ctx.ref(top),
                "bottom": ctx.ref("arcaneearbottom"),
                "side": ctx.ref("arcaneearsideon"),
                "bell_top": ctx.ref("arcaneearbelltop"),
                "bell_side": ctx.ref("arcaneearbellside"),
            },
            "elements": elements,
        })
    ctx.blockstate("arcane_ear", {"variants": {
        "powered=false": {"model": ctx.ref("arcane_ear_off")},
        "powered=true": {"model": ctx.ref("arcane_ear_on")},
    }})
    ctx.block_item("arcane_ear", model=ctx.ref("arcane_ear_on"))
    ctx.self_drop("arcane_ear")
    ctx.mineable("axe", "arcane_ear")


def levitator(ctx):
    def cube(glow_light):
        elements = [box([0, 0, 0], [16, 16, 16], {
            "down": {"texture": "#bottom", "cullface": "down"},
            "up": {"texture": "#top", "cullface": "up"},
            **{face: {"texture": "#side", "cullface": face} for face in SIDES},
        })]
        elements.append(flat("up", [0, 15.84, 0], [16, 15.84, 16], "#glow", tintindex=0, emission=glow_light))
        elements.append(flat("east", [15.84, 0, 0], [15.84, 16, 16], "#glow", tintindex=1, emission=glow_light))
        elements.append(flat("west", [0.16, 0, 0], [0.16, 16, 16], "#glow", tintindex=1, emission=glow_light))
        elements.append(flat("south", [0, 0, 15.84], [16, 16, 15.84], "#glow", tintindex=1, emission=glow_light))
        elements.append(flat("north", [0, 0, 0.16], [16, 16, 0.16], "#glow", tintindex=1, emission=glow_light))
        return elements

    textures = {
        "particle": ctx.ref("lifterside"),
        "bottom": ctx.ref("arcaneearbottom"),
        "top": ctx.ref("liftertop"),
        "side": ctx.ref("lifterside"),
        "glow": ctx.ref("animatedglow"),
    }
    ctx.block_model("arcane_levitator_active", {"parent": "minecraft:block/block", "textures": textures, "elements": cube(LEVITATOR_ACTIVE_LIGHT)})
    ctx.block_model("arcane_levitator_idle", {"parent": "minecraft:block/block", "textures": textures, "elements": cube(0)})
    ctx.blockstate("arcane_levitator", {"variants": {
        "powered=false": {"model": ctx.ref("arcane_levitator_active")},
        "powered=true": {"model": ctx.ref("arcane_levitator_idle")},
    }})
    ctx.item_model("arcane_levitator", {
        "parent": "minecraft:block/block",
        "textures": textures,
        "elements": [
            box([0, 0, 0], [16, 16, 16], {
                "down": {"texture": "#bottom"}, "up": {"texture": "#top"},
                **{face: {"texture": "#side"} for face in SIDES},
            }),
            box([0.16, 14.4, 0.16], [15.84, 15.84, 15.84], uniform_faces("#glow", tintindex=0)),
            box([0.16, 1.6, 0.16], [15.84, 14.4, 15.84], uniform_faces("#glow", tintindex=1)),
        ],
    })
    ctx.item_definition("arcane_levitator", ctx.ref("arcane_levitator", "item"), [ctx.tint(LEVITATOR_TOP_GLOW), ctx.tint(LEVITATOR_INNER_GLOW)])
    ctx.self_drop("arcane_levitator")
    ctx.mineable("axe", "arcane_levitator")


def crystals(ctx):
    for name, kind in CRYSTALS:
        ctx.particle_model(name, "crystal")
        ctx.blockstate(name, {"variants": {f"facing={facing}": {"model": ctx.ref(name)} for facing in FACES}})
        ctx.item_base_particle(name, "crystal")
        ctx.special_item(name, {"type": f"{NS}:crystal_cluster", "kind": kind})
        if kind < 6:
            pools = [{
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": f"{NS}:{ELEMENTS[kind]}_shard", "modifier": [{"type": "minecraft:set_count", "count": 6}]}],
                "condition": {"type": "minecraft:survives_explosion"},
            }]
        elif kind == 6:
            pools = [{
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": f"{NS}:{element}_shard"}],
                "condition": {"type": "minecraft:survives_explosion"},
            } for element in ELEMENTS[:6]]
        else:
            pools = [{
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": f"{NS}:balanced_shard"}],
                "condition": {"type": "minecraft:survives_explosion"},
            }]
        ctx.write_loot(name, pools)


def tags(ctx):
    ctx.block_tags[f"{NS}:lamp_blacklist"]


def generate(ctx):
    airy(ctx)
    lamps(ctx)
    grate(ctx)
    ear(ctx)
    levitator(ctx)
    crystals(ctx)
    tags(ctx)
