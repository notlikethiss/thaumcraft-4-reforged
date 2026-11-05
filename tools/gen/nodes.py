from .common import FACES, NS, box


def nodes(ctx):
    for name in ["aura_node", "energized_aura_node"]:
        ctx.particle_model(name, "blank", "item")
        ctx.simple_state(name)
        ctx.write_loot(name, [])
    ctx.item_base_particle("aura_node", "blank", "item")
    ctx.special_item("aura_node", {"type": f"{NS}:aura_node"})

    ctx.simple_state("charged_obsidian_totem", ctx.ref("obsidian_totem"))
    ctx.block_item("charged_obsidian_totem", model=ctx.ref("obsidian_totem"))
    ctx.self_drop("charged_obsidian_totem")
    ctx.mineable("pickaxe", "charged_obsidian_totem")

    ctx.cube_column("silverwood_knot", "silverwoodknottop", "silverwoodknotside")
    ctx.block_model("silverwood_knot_horizontal", {
        "parent": "minecraft:block/cube_column_horizontal",
        "textures": {"end": ctx.ref("silverwoodknottop"), "side": ctx.ref("silverwoodknotside")},
    })
    ctx.blockstate("silverwood_knot", {"variants": {
        "axis=x": {"model": ctx.ref("silverwood_knot_horizontal"), "x": 90, "y": 90},
        "axis=y": {"model": ctx.ref("silverwood_knot")},
        "axis=z": {"model": ctx.ref("silverwood_knot_horizontal"), "x": 90},
    }})
    ctx.write_loot("silverwood_knot", [{
        "rolls": 1,
        "entries": [{"type": "minecraft:item", "name": f"{NS}:silverwood_log"}],
        "condition": {"type": "minecraft:survives_explosion"},
    }])
    ctx.mineable("axe", "silverwood_knot")
    ctx.tag_both("minecraft:logs", "silverwood_knot")
    ctx.tag_both("minecraft:logs_that_burn", "silverwood_knot")


def devices(ctx):
    for name, model in [
        ("node_stabilizer", {"type": f"{NS}:node_stabilizer", "advanced": False}),
        ("advanced_node_stabilizer", {"type": f"{NS}:node_stabilizer", "advanced": True}),
        ("node_transducer", {"type": f"{NS}:node_transducer"}),
        ("vis_relay", {"type": f"{NS}:vis_relay"}),
    ]:
        texture = "thaumiumblock" if name == "vis_relay" else "pedestal_top"
        ctx.particle_model(name, texture)
        ctx.item_base_particle(name, texture)
        ctx.special_item(name, model)
        ctx.self_drop(name)
        ctx.mineable("pickaxe", name)
    for name in ["node_stabilizer", "advanced_node_stabilizer", "node_transducer"]:
        ctx.simple_state(name)
    ctx.blockstate("vis_relay", {"variants": {
        f"connected={connected},facing={facing}": {"model": ctx.ref("vis_relay")}
        for facing in FACES
        for connected in ["false", "true"]
    }})

    ctx.block_model("wand_recharge_pedestal", {
        "parent": "minecraft:block/block",
        "textures": {"particle": ctx.ref("wandpedestal_side"), "side": ctx.ref("wandpedestal_side"), "top": ctx.ref("wandpedestal_top")},
        "elements": [
            box([0, 0, 0], [16, 4, 16], {
                "down": {"texture": "#top", "cullface": "down"}, "up": {"texture": "#top"},
                "north": {"texture": "#side", "cullface": "north"}, "south": {"texture": "#side", "cullface": "south"},
                "west": {"texture": "#side", "cullface": "west"}, "east": {"texture": "#side", "cullface": "east"},
            }),
            box([2, 4, 2], [14, 8, 14], {
                "up": {"texture": "#top"}, "north": {"texture": "#side"}, "south": {"texture": "#side"},
                "west": {"texture": "#side"}, "east": {"texture": "#side"},
            }),
            box([4, 8, 4], [12, 16, 12], {
                "up": {"texture": "#top", "cullface": "up"}, "north": {"texture": "#side"}, "south": {"texture": "#side"},
                "west": {"texture": "#side"}, "east": {"texture": "#side"},
            }),
        ],
    })
    ctx.block_model("compound_recharge_focus", {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": ctx.ref("wandpedestal_focus_side"),
            "bottom": ctx.ref("wandpedestal_focus_bot"),
            "top": ctx.ref("wandpedestal_focus_top"),
            "side": ctx.ref("wandpedestal_focus_side"),
        },
        "elements": [box(start, end, {
            "down": {"texture": "#bottom"}, "up": {"texture": "#top"},
            "north": {"texture": "#side"}, "south": {"texture": "#side"},
            "west": {"texture": "#side"}, "east": {"texture": "#side"},
        }) for start, end in [
            ([5, 0, 5], [11, 1, 11]),
            ([1, 0, 7], [5, 1, 9]),
            ([11, 0, 7], [15, 1, 9]),
            ([7, 0, 1], [9, 1, 5]),
            ([7, 0, 11], [9, 1, 15]),
            ([1, 1, 7], [3, 7, 9]),
            ([7, 1, 1], [9, 7, 3]),
            ([13, 1, 7], [15, 7, 9]),
            ([7, 1, 13], [9, 7, 15]),
        ]],
    })
    for name in ["wand_recharge_pedestal", "compound_recharge_focus"]:
        ctx.simple_state(name)
        ctx.block_item(name)
        ctx.self_drop(name)
        ctx.mineable("pickaxe", name)


def jar(ctx):
    ctx.block_model("node_jar", {"parent": ctx.ref("warded_jar")})
    ctx.simple_state("node_jar")
    ctx.item_base_from_block("node_jar")
    ctx.special_item("node_jar", {"type": f"{NS}:node_jar"})


def generate(ctx):
    nodes(ctx)
    devices(ctx)
    jar(ctx)
