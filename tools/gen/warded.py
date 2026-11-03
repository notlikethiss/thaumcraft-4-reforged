from .common import NS

HORIZONTAL = ["north", "south", "west", "east"]
DOOR_YAW = {"east": 0, "south": 90, "west": 180, "north": 270}
HUNGRY_CHEST_ITEM = {"type": f"{NS}:hungry_chest"}
BRAIN_JAR_ITEM = {"type": f"{NS}:brain_jar"}
BANNER_ITEM = {"type": f"{NS}:banner"}
GLASS_VARIANTS = 47


def warded_glass(ctx):
    for index in range(1, GLASS_VARIANTS + 1):
        ctx.cube_all(f"warded_glass_{index}", f"warded_glass_{index}")
    models = [ctx.ref(f"warded_glass_{index}") for index in range(1, GLASS_VARIANTS + 1)]
    if ctx.profile.modern:
        ctx.blockstate("warded_glass", {"variants": {"": {
            "type": f"{NS}:warded_glass",
            "models": [{"model": model} for model in models],
        }}})
    else:
        ctx.block_model("warded_glass", {"parent": models[0], "loader": f"{NS}:warded_glass", "models": models})
        ctx.simple_state("warded_glass")
        ctx.MANIFEST["model_loaders"]["warded_glass"] = {"loader": f"{NS}:warded_glass", "models": models}
    ctx.block_item("warded_glass", model=ctx.ref("warded_glass_1"))
    ctx.self_drop("warded_glass")


def door(ctx):
    for half, texture in [("bottom", "adoorbot"), ("top", "adoortop")]:
        for hinge in ["left", "right"]:
            for suffix in ["", "_open"]:
                ctx.block_model(f"arcane_door_{half}_{hinge}{suffix}", {
                    "parent": f"minecraft:block/door_{half}_{hinge}{suffix}",
                    "textures": {"bottom": ctx.ref(texture), "top": ctx.ref(texture)},
                })
    variants = {}
    for facing, yaw in DOOR_YAW.items():
        for half, part in [("lower", "bottom"), ("upper", "top")]:
            for hinge in ["left", "right"]:
                for opened in ["false", "true"]:
                    rotation = yaw
                    if opened == "true":
                        rotation = (yaw + (90 if hinge == "left" else 270)) % 360
                    entry = {"model": ctx.ref(f"arcane_door_{part}_{hinge}" + ("_open" if opened == "true" else ""))}
                    if rotation:
                        entry["y"] = rotation
                    variants[f"facing={facing},half={half},hinge={hinge},open={opened}"] = entry
    ctx.blockstate("arcane_door", {"variants": variants})
    ctx.generated_item("arcane_door", "arcanedoor")
    ctx.write_loot("arcane_door", [])


def pressure_plate(ctx):
    variants = {}
    for setting in range(3):
        for powered, parent in [("false", "pressure_plate_up"), ("true", "pressure_plate_down")]:
            name = f"arcane_pressure_plate_{powered}_{setting}"
            ctx.block_model(name, {"parent": f"minecraft:block/{parent}", "textures": {"texture": ctx.ref(f"applate{setting + 1}")}})
            variants[f"powered={powered},setting={setting}"] = {"model": ctx.ref(name)}
    ctx.blockstate("arcane_pressure_plate", {"variants": variants})
    ctx.block_item("arcane_pressure_plate", model=ctx.ref("arcane_pressure_plate_false_0"))
    ctx.write_loot("arcane_pressure_plate", [])


def hungry_chest(ctx):
    ctx.particle_model("hungry_chest", "woodplain")
    ctx.blockstate("hungry_chest", {"variants": {f"facing={facing}": {"model": ctx.ref("hungry_chest")} for facing in HORIZONTAL}})
    ctx.item_base_particle("hungry_chest", "woodplain")
    ctx.special_item("hungry_chest", HUNGRY_CHEST_ITEM)
    ctx.self_drop("hungry_chest")
    ctx.mineable("axe", "hungry_chest")


def brain_jar(ctx):
    def element(start, end):
        return {"from": start, "to": end, "faces": {
            "down": {"texture": "#bottom"}, "up": {"texture": "#top"},
            "north": {"texture": "#side"}, "south": {"texture": "#side"},
            "west": {"texture": "#side"}, "east": {"texture": "#side"},
        }}

    ctx.block_model("brain_jar", {
        "parent": "minecraft:block/block",
        "textures": {"particle": ctx.ref("jar_side"), "side": ctx.ref("jar_side"), "top": ctx.ref("jar_top"), "bottom": ctx.ref("jar_bottom")},
        "elements": [element([3, 0, 3], [13, 12, 13]), element([5, 12, 5], [11, 14, 11])],
    })
    ctx.simple_state("brain_jar")
    ctx.item_model("brain_jar", {"parent": ctx.ref("brain_jar")})
    ctx.composite_item("brain_jar", ctx.ref("brain_jar", "item"), BRAIN_JAR_ITEM)
    ctx.self_drop("brain_jar")


def banner(ctx):
    ctx.particle_model("banner", "woodplain")
    ctx.simple_state("banner")
    ctx.item_base_particle("banner", "woodplain")
    ctx.special_item("banner", BANNER_ITEM)
    ctx.mineable("axe", "banner")


def generate(ctx):
    warded_glass(ctx)
    door(ctx)
    pressure_plate(ctx)
    hungry_chest(ctx)
    brain_jar(ctx)
    banner(ctx)
    for name in ["iron_arcane_key", "gold_arcane_key"]:
        ctx.generated_item(name, "keyiron" if name.startswith("iron") else "keygold")
