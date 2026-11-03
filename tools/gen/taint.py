from .common import NS


def block(ctx, name, texture):
    ctx.cube_all(name, texture)
    ctx.simple_state(name)
    ctx.block_item(name)


def silk_touch_drop(ctx, block, item=None, count=1):
    entries = [{"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": f"{NS}:{block}"}]
    if item is not None:
        drop = {"type": "minecraft:item", "name": item}
        if count > 1:
            drop["functions"] = [{"function": "minecraft:set_count", "count": count}]
        entries.append(drop)
    ctx.write_loot(block, [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": entries}]}])


def cross(ctx, name, texture):
    ctx.block_model(name, {"parent": "minecraft:block/cross", "textures": {"cross": ctx.ref(texture, "block")}})
    ctx.simple_state(name)
    ctx.generated_item(name, texture, texture_kind="block")
    ctx.write_loot(name, [])


def taint_fibres_model(ctx):
    ctx.block_model("taint_fibres_face", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": ctx.ref("taint_fibres", "block")},
    })
    for name, texture in [
        ("blank", "blank"),
        ("over_1", "taint_over_1"),
        ("over_2", "taint_over_2"),
        ("over_3", "taint_over_3"),
    ]:
        ctx.block_model(f"taint_fibres_{name}", {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": ctx.ref(texture, "block")},
        })
    for model, texture in [
        ("taint_fibres_grass", "taintgrass1"),
        ("taint_fibres_plant", "taintgrass2"),
        ("taint_fibres_stalk", "taint_spore_stalk_1"),
        ("taint_fibres_mature_stalk", "taint_spore_stalk_2"),
    ]:
        ctx.block_model(model, {
            "parent": "minecraft:block/cross",
            "textures": {"cross": ctx.ref(texture, "block")},
        })
    model = lambda name: {"model": f"{NS}:block/{name}"}
    overlays = [
        [model(f"taint_fibres_{name}") for name in ("blank", "over_1", "over_2", "over_3")]
        for _ in range(6)
    ]
    blockstate = {"variants": {"": {
        "type": f"{NS}:taint_fibres",
        "faces": [model("taint_fibres_face") for _ in range(6)],
        "overlays": overlays,
        "grass": model("taint_fibres_grass"),
        "plant": model("taint_fibres_plant"),
        "stalk": model("taint_fibres_stalk"),
        "mature_stalk": model("taint_fibres_mature_stalk"),
    }}}
    for name in ("fibrous_taint", "tainted_grass", "tainted_plant", "spore_stalk", "mature_spore_stalk"):
        ctx.write(ctx.ASSETS / "blockstates" / f"{name}.json", blockstate)


def generate(ctx):
    for name, texture in [
        ("crusted_taint", "taint_crust"),
        ("tainted_soil", "taint_soil"),
        ("flesh_block", "fleshblock"),
    ]:
        block(ctx, name, texture)
    silk_touch_drop(ctx, "crusted_taint")
    silk_touch_drop(ctx, "tainted_soil", "minecraft:dirt")
    silk_touch_drop(ctx, "flesh_block", "minecraft:rotten_flesh", 9)

    for name, texture in [
        ("fibrous_taint", "taint_fibres"),
        ("tainted_grass", "taintgrass1"),
        ("tainted_plant", "taintgrass2"),
        ("spore_stalk", "taint_spore_stalk_1"),
        ("mature_spore_stalk", "taint_spore_stalk_2"),
    ]:
        cross(ctx, name, texture)
    taint_fibres_model(ctx)

    for name, texture in [
        ("purifying_fluid_bucket", "bucket_pure"),
        ("liquid_death_bucket", "bucket_death"),
        ("bath_salts", "bath_salts"),
        ("sanitizing_soap", "soap"),
        ("sanity_checker", "sanitychecker"),
        ("bottled_taint", "bottle_taint"),
    ]:
        ctx.generated_item(name, texture)

    for name in ["purifying_fluid", "liquid_death"]:
        ctx.block_model(name, {"parent": "minecraft:block/block", "textures": {"particle": ctx.ref("fluidpure" if name == "purifying_fluid" else "fluiddeath", "block")}})
        ctx.simple_state(name)
