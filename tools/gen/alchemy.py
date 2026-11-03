from .common import FACES, NS, flat

CENTRIFUGE_ITEM = {"type": f"{NS}:centrifuge"}
CRYSTALLIZER_ITEM = {"type": f"{NS}:essentia_crystallizer"}
RESERVOIR_ITEM = {"type": f"{NS}:essentia_reservoir"}
FILLED_JAR_ITEM = {"type": f"{NS}:filled_jar"}
TUBE_VALVE_ITEM = {"type": f"{NS}:tube_valve"}
ALEMBIC_ITEM = {"type": f"{NS}:alembic"}
FLUX_SCRUBBER_ITEM = {"type": f"{NS}:flux_scrubber"}
BELLOWS_ITEM = {"type": f"{NS}:arcane_bellows"}
HORIZONTAL = {"south": 0, "west": 90, "north": 180, "east": 270}
ARM_DIRECTIONS = {"north": {}, "south": {"y": 180}, "east": {"y": 90}, "west": {"y": 270}, "down": {"x": 90}, "up": {"x": 270}}


def cube_element(start, end, texture="#t", faces=None):
    names = faces or FACES
    return {"from": [start] * 3, "to": [end] * 3, "faces": {face: {"texture": texture} for face in names}}


def arm_element(texture, extended, width=(7, 9)):
    low, high = width
    start = -6 if extended else 0
    end = 7 if low == 7 else low
    faces = {face: {"texture": texture} for face in FACES}
    if extended:
        length = end - start
        faces["down"]["uv"] = [low, 16 - end + start, high, 16]
        faces["up"]["uv"] = [low, 0, high, length]
        faces["west"]["uv"] = [0, 16 - high, length, 16 - low]
        faces["east"]["uv"] = [16 - end + start, 16 - high, 16, 16 - low]
    return {
        "from": [low, low, start],
        "to": [high, high, end],
        "faces": faces,
    }


def tube_models(ctx):
    for name, texture in [("pipe_1", "pipe_1"), ("pipe_buffer", "pipe_buffer")]:
        for extended in [False, True]:
            suffix = "_long" if extended else ""
            ctx.block_model(f"essentia_arm_{name}{suffix}", {
                "parent": "minecraft:block/block",
                "textures": {"particle": ctx.ref(texture), "t": ctx.ref(texture)},
                "elements": [arm_element("#t", extended)],
            })
    cores = {
        "essentia_core_small": ("pipe_3", 6.5, 9.5),
        "essentia_core_big": ("pipe_2", 6, 10),
        "essentia_core_filter": ("pipe_filter", 5.5, 10.5),
        "essentia_core_restrict": ("pipe_restrict", 6.5, 9.5),
        "essentia_core_buffer": ("pipe_buffer", 4, 12),
    }
    for name, (texture, low, high) in cores.items():
        ctx.block_model(name, {
            "parent": "minecraft:block/block",
            "textures": {"particle": ctx.ref(texture), "t": ctx.ref(texture)},
            "elements": [cube_element(low, high)],
        })


def tube_state(ctx, name, arm, core_all=None, core_idle=None, core_arms=None):
    parts = []
    for direction, rotation in ARM_DIRECTIONS.items():
        for kind, suffix in [("normal", ""), ("extended", "_long")]:
            parts.append({"when": {direction: kind}, "apply": {"model": ctx.ref(f"essentia_arm_{arm}{suffix}"), **rotation}})
    if core_all:
        parts.append({"apply": {"model": ctx.ref(core_all)}})
    if core_idle:
        parts.append({"when": {direction: "none" for direction in ARM_DIRECTIONS}, "apply": {"model": ctx.ref(core_idle)}})
    if core_arms:
        parts.append({
            "when": {"OR": [{direction: "normal|extended"} for direction in ARM_DIRECTIONS]},
            "apply": {"model": ctx.ref(core_arms)},
        })
    ctx.blockstate(name, {"multipart": parts})


def tube_item(ctx, name, core, arm_texture="pipe_1", core_texture=None):
    particle = ctx.ref(core_texture or arm_texture)
    ctx.item_model(name, {
        "parent": "minecraft:block/block",
        "textures": {"particle": particle, "t": particle, "a": ctx.ref(arm_texture)},
        "elements": [
            {"from": [7, 0, 7], "to": [9, 16, 9], "faces": {face: {"texture": "#a"} for face in FACES}},
            cube_element(*core),
        ],
    })
    ctx.item_definition(name, ctx.ref(name, "item"))


def essentia(ctx):
    tube_models(ctx)
    tube_state(ctx, "essentia_tube", "pipe_1", core_idle="essentia_core_big", core_arms="essentia_core_small")
    tube_state(ctx, "directional_essentia_tube", "pipe_1", core_idle="essentia_core_big", core_arms="essentia_core_small")
    tube_state(ctx, "essentia_valve", "pipe_1", core_all="essentia_core_big")
    tube_state(ctx, "filtered_essentia_tube", "pipe_1", core_all="essentia_core_filter")
    tube_state(ctx, "restricted_essentia_tube", "pipe_1", core_all="essentia_core_restrict")
    tube_state(ctx, "essentia_buffer", "pipe_buffer", core_all="essentia_core_buffer")
    tube_item(ctx, "essentia_tube", (6.5, 9.5), core_texture="pipe_3")
    tube_item(ctx, "directional_essentia_tube", (6.5, 9.5), core_texture="pipe_3")
    tube_item(ctx, "essentia_valve", (6, 10), core_texture="pipe_2")
    tube_item(ctx, "filtered_essentia_tube", (5.5, 10.5), core_texture="pipe_filter")
    tube_item(ctx, "restricted_essentia_tube", (6.5, 9.5), core_texture="pipe_restrict")
    tube_item(ctx, "essentia_buffer", (4, 12), arm_texture="pipe_buffer")
    ctx.composite_item("essentia_valve", ctx.ref("essentia_valve", "item"), TUBE_VALVE_ITEM)
    for name in ["essentia_tube", "directional_essentia_tube", "essentia_valve", "filtered_essentia_tube", "restricted_essentia_tube", "essentia_buffer"]:
        ctx.self_drop(name)
        ctx.mineable("pickaxe", name)

    ctx.particle_model("alchemical_centrifuge", "thaumiumblock")
    ctx.simple_state("alchemical_centrifuge")
    ctx.item_base_particle("alchemical_centrifuge", "thaumiumblock")
    ctx.special_item("alchemical_centrifuge", CENTRIFUGE_ITEM)
    for name, texture, item in [
        ("essentia_crystallizer", "thaumiumblock", CRYSTALLIZER_ITEM),
        ("essentia_reservoir", "thaumiumblock", RESERVOIR_ITEM),
    ]:
        ctx.particle_model(name, texture)
        ctx.blockstate(name, {"variants": {f"facing={facing}": {"model": ctx.ref(name)} for facing in FACES}})
        ctx.item_base_particle(name, texture)
        ctx.special_item(name, item)
    for name in ["alchemical_centrifuge", "essentia_crystallizer", "essentia_reservoir"]:
        ctx.self_drop(name)
        ctx.mineable("pickaxe", name)

    for name, top, side in [("warded_jar", "jar_top", "jar_side"), ("void_jar", "jar_top_void", "jar_side_void")]:
        def element(start, end):
            return {"from": start, "to": end, "faces": {
                "down": {"texture": "#bottom"}, "up": {"texture": "#top"},
                "north": {"texture": "#side"}, "south": {"texture": "#side"},
                "west": {"texture": "#side"}, "east": {"texture": "#side"},
            }}

        ctx.block_model(name, {
            "parent": "minecraft:block/block",
            "textures": {"particle": ctx.ref(side), "side": ctx.ref(side), "top": ctx.ref(top), "bottom": ctx.ref("jar_bottom")},
            "elements": [element([3, 0, 3], [13, 12, 13]), element([5, 12, 5], [11, 14, 11])],
        })
        ctx.simple_state(name)
        ctx.block_item(name)
    for name in ["filled_jar", "filled_void_jar"]:
        ctx.item_model(name, {"parent": ctx.ref("void_jar" if "void" in name else "warded_jar")})
        ctx.special_item(name, {**FILLED_JAR_ITEM, "void": True} if "void" in name else FILLED_JAR_ITEM)


def crucible(ctx):
    ctx.block_model("crucible", {
        "parent": "minecraft:block/block",
        **({"render_type": "minecraft:cutout"} if ctx.profile.explicit_render_types else {}),
        "textures": {
            "particle": ctx.ref("crucible3"), "side": ctx.ref("crucible3"), "top": ctx.ref("crucible1"),
            "bottom": ctx.ref("crucible2"), "inner": ctx.ref("crucible5"), "floor": ctx.ref("crucible6"),
        },
        "elements": [
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                "down": {"texture": "#bottom", "cullface": "down"}, "up": {"texture": "#top", "cullface": "up"},
                "north": {"texture": "#side", "cullface": "north"}, "south": {"texture": "#side", "cullface": "south"},
                "west": {"texture": "#side", "cullface": "west"}, "east": {"texture": "#side", "cullface": "east"},
            }},
            flat("east", [1.968, 4, 0], [1.968, 16, 16], "#inner"),
            flat("west", [14.032, 4, 0], [14.032, 16, 16], "#inner"),
            flat("south", [0, 4, 1.968], [16, 16, 1.968], "#inner"),
            flat("north", [0, 4, 14.032], [16, 16, 14.032], "#inner"),
            flat("up", [0, 4, 0], [16, 4, 16], "#floor"),
            flat("down", [0, 4, 0], [16, 4, 16], "#floor"),
        ],
    })
    ctx.simple_state("crucible")
    ctx.item_model("crucible", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": ctx.ref("crucible4"), "bottom": ctx.ref("crucible2"), "side": ctx.ref("crucible3")},
    })
    ctx.item_definition("crucible", ctx.ref("crucible", "item"))
    ctx.self_drop("crucible")
    ctx.mineable("pickaxe", "crucible")


def horizontal_state(ctx, name):
    ctx.blockstate(name, {"variants": {
        f"facing={facing}": {"model": ctx.ref(name), "y": rotation} if rotation else {"model": ctx.ref(name)}
        for facing, rotation in HORIZONTAL.items()
    }})


def furnaces(ctx):
    for lit in ["false", "true"]:
        for has_vis in ["false", "true"]:
            name = f"alchemical_furnace_{lit}_{has_vis}"
            ctx.block_model(name, {
                "parent": "minecraft:block/cube",
                "textures": {
                    "particle": ctx.ref("al_furnace_side"),
                    "down": ctx.ref("al_furnace_side"),
                    "up": ctx.ref("al_furnace_top_filled" if has_vis == "true" else "al_furnace_top"),
                    **{side: ctx.ref("al_furnace_front_on" if lit == "true" else "al_furnace_front_off") for side in ["north", "south", "west", "east"]},
                },
            })
    ctx.blockstate("alchemical_furnace", {"variants": {
        f"has_vis={has_vis},lit={lit}": {"model": ctx.ref(f"alchemical_furnace_{lit}_{has_vis}")}
        for lit in ["false", "true"]
        for has_vis in ["false", "true"]
    }})
    ctx.block_item("alchemical_furnace", model=ctx.ref("alchemical_furnace_false_false"))
    ctx.self_drop("alchemical_furnace")
    ctx.mineable("pickaxe", "alchemical_furnace")

    ctx.particle_model("advanced_alchemical_furnace", "alchemyblockadv")
    ctx.blockstate("advanced_alchemical_furnace", {"variants": {
        f"light={light},part={part}": {"model": ctx.ref("advanced_alchemical_furnace")}
        for part in ["core", "nozzle", "frame_alembic", "frame_edge", "frame_corner"]
        for light in range(13)
    }})
    ctx.mineable("pickaxe", "advanced_alchemical_furnace")

    for name, texture in [("alchemical_construct", "alchemyblock"), ("advanced_alchemical_construct", "alchemyblockadv")]:
        ctx.cube_all(name, texture)
        ctx.simple_state(name)
        ctx.block_item(name)
        ctx.self_drop(name)
        ctx.mineable("pickaxe", name)

    for name in ["thaumatorium", "thaumatorium_top"]:
        ctx.particle_model(name, "alchemyblock")
        horizontal_state(ctx, name)
        ctx.mineable("pickaxe", name)

    matrix_textures = {"particle": ctx.ref("brainbox"), "t": ctx.ref("brainbox")}
    ctx.block_model("mnemonic_matrix", {"parent": "minecraft:block/block", "textures": matrix_textures, "elements": [cube_element(3, 13)]})
    ctx.block_model("mnemonic_matrix_connected", {
        "parent": "minecraft:block/block",
        "textures": matrix_textures,
        "elements": [
            cube_element(3, 13),
            {"from": [6, 6, 0], "to": [10, 10, 3], "faces": {face: {"texture": "#t"} for face in ["down", "up", "north", "west", "east"]}},
        ],
    })
    ctx.blockstate("mnemonic_matrix", {"variants": {
        f"facing={facing}": {"model": ctx.ref("mnemonic_matrix_connected"), **ARM_DIRECTIONS[facing]} for facing in FACES
    }})
    ctx.block_item("mnemonic_matrix")
    ctx.self_drop("mnemonic_matrix")
    ctx.mineable("pickaxe", "mnemonic_matrix")


def flux(ctx):
    for name, texture, hanging in [("flux_goo", "fluxgoo", False), ("flux_gas", "fluxgas", True)]:
        variants = {}
        for level in range(1, 9):
            model = f"{name}_{level}"
            height = level * 2
            start, end = (16 - height, 16) if hanging else (0, height)
            ctx.block_model(model, {
                "parent": "minecraft:block/block",
                "textures": {"particle": ctx.ref(texture), "t": ctx.ref(texture)},
                "elements": [{"from": [0, start, 0], "to": [16, end, 16], "faces": {
                    face: {"texture": "#t"} for face in FACES
                }}],
            })
            variants[f"level={level}"] = {"model": ctx.ref(model)}
        ctx.blockstate(name, {"variants": variants})
        ctx.generated_item(name, texture, texture_kind="block")


def alchemy(ctx):
    crucible(ctx)
    furnaces(ctx)
    flux(ctx)

    ctx.particle_model("alembic", "metalbase")
    horizontal_state(ctx, "alembic")
    ctx.item_base_particle("alembic", "metalbase")
    ctx.special_item("alembic", ALEMBIC_ITEM)
    ctx.self_drop("alembic")
    ctx.mineable("pickaxe", "alembic")

    for name, texture, item, tool in [
        ("flux_scrubber", "pedestal_top", FLUX_SCRUBBER_ITEM, "pickaxe"),
        ("arcane_bellows", "planks_greatwood", BELLOWS_ITEM, "axe"),
    ]:
        ctx.particle_model(name, texture)
        ctx.blockstate(name, {"variants": {f"facing={facing}": {"model": ctx.ref(name)} for facing in FACES}})
        ctx.item_base_particle(name, texture)
        ctx.special_item(name, item)
        ctx.self_drop(name)
        ctx.mineable(tool, name)


def generate(ctx):
    essentia(ctx)
    alchemy(ctx)
