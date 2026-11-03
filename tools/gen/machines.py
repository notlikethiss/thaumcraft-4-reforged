from .common import FACES, NS

FACING_ROTATION = {"north": 0, "east": 90, "south": 180, "west": 270}
SIDE_COLUMNS = {
    "north": {1: 2, 2: 1, 3: 0},
    "south": {7: 0, 8: 1, 9: 2},
    "west": {1: 0, 4: 1, 7: 2},
    "east": {3: 2, 6: 1, 9: 0},
}
BOTTOM_FLOOR = {2: 16, 4: 17, 6: 26, 8: 25}
LEVELS = {"top": 0, "middle": 9, "bottom": 18}
BORE_BASE_ITEM = {"type": f"{NS}:arcane_bore_base"}
BORE_ITEM = {"type": f"{NS}:arcane_bore"}


def tiles():
    result = [("core", None, None, None), ("bars", None, None, None)]
    for level in ["bottom", "middle", "top"]:
        for cz in range(3):
            for cx in range(3):
                if level == "middle" and cx == 1 and cz in (0, 1):
                    continue
                if level == "top" and cx == 1 and cz == 1:
                    continue
                result.append((f"{level}_{cx}_{cz}", level, cx, cz))
    return result


def face_icon(face, level, cx, cz):
    meta = 1 + cz * 3 + cx
    offset = LEVELS[level]
    alt = 3 if cz == 0 else 0
    if face in SIDE_COLUMNS:
        column = SIDE_COLUMNS[face].get(meta)
        if column is not None:
            return column + offset + (alt if face == "north" else 0)
        return 7 if offset != 9 else 6
    if face == "up":
        if level == "bottom" and meta in BOTTOM_FLOOR:
            return BOTTOM_FLOOR[meta]
    if face == "down" and level == "top" and cx == 1 and cz == 0:
        return 6
    if meta == 5:
        return 10
    return (meta - 1) % 3 + (meta - 1) // 3 * 9


def wall_model(ctx, name, level, cx, cz):
    textures = {face: ctx.ref(f"furnace{face_icon(face, level, cx, cz)}") for face in FACES}
    textures["particle"] = textures["north"]
    ctx.block_model(f"infernal_furnace_{name}", {
        "parent": "minecraft:block/cube",
        "textures": textures,
    })


def bars_model(ctx):
    elements = []
    for depth, texture, top in [(0.375 * 16, "#grate", 16), (0.2 * 16, "#eyes", 16), (0.1 * 16, "#fire", 24)]:
        plane = 16 - depth
        element = {
            "from": [0, 0, plane],
            "to": [16, top, plane],
            "faces": {"north": {"texture": texture, "uv": [0, 0, 16, 16]}},
        }
        elements.append(element)
    ctx.block_model("infernal_furnace_bars", {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": ctx.ref("furnace13"),
            "grate": ctx.ref("furnace13"),
            "eyes": ctx.ref("furnace15"),
            "fire": "minecraft:block/fire_0",
        },
        "elements": elements,
    })


def infernal_furnace(ctx):
    ctx.block_model("infernal_furnace_core", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": "minecraft:block/lava_still"},
    })
    bars_model(ctx)
    variants = {}
    for index, (name, level, cx, cz) in enumerate(tiles()):
        if level is not None:
            wall_model(ctx, name, level, cx, cz)
        for facing, rotation in FACING_ROTATION.items():
            entry = {"model": ctx.ref(f"infernal_furnace_{name}")}
            if rotation and name != "core":
                entry["y"] = rotation
            variants[f"facing={facing},tile={index}"] = entry
    ctx.blockstate("infernal_furnace", {"variants": variants})
    ctx.mineable("pickaxe", "infernal_furnace")


def bore(ctx):
    ctx.particle_model("arcane_bore_base", "planks_greatwood")
    ctx.blockstate("arcane_bore_base", {"variants": {
        f"facing={facing}": {"model": ctx.ref("arcane_bore_base")}
        for facing in FACES
    }})
    ctx.item_base_particle("arcane_bore_base", "planks_greatwood")
    ctx.special_item("arcane_bore_base", BORE_BASE_ITEM)
    ctx.particle_model("arcane_bore", "planks_greatwood")
    ctx.blockstate("arcane_bore", {"variants": {f"base={base}": {"model": ctx.ref("arcane_bore")} for base in ["up", "down"]}})
    ctx.item_base_particle("arcane_bore", "planks_greatwood")
    ctx.special_item("arcane_bore", BORE_ITEM)
    for name in ["arcane_bore_base", "arcane_bore"]:
        ctx.self_drop(name)
        ctx.mineable("axe", name)


def spa(ctx):
    ctx.block_model("arcane_spa", {
        "parent": "minecraft:block/cube",
        "textures": {
            "particle": ctx.ref("spa_side"),
            "down": ctx.ref("pedestal_top"),
            "up": ctx.ref("spa_top"),
            "north": ctx.ref("spa_side"),
            "south": ctx.ref("spa_side"),
            "west": ctx.ref("spa_side"),
            "east": ctx.ref("spa_side"),
        },
    })
    ctx.simple_state("arcane_spa")
    ctx.block_item("arcane_spa")
    ctx.self_drop("arcane_spa")
    ctx.mineable("pickaxe", "arcane_spa")


def generate(ctx):
    infernal_furnace(ctx)
    bore(ctx)
    spa(ctx)
