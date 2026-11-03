from .common import FACES, NS

LINK_COMPONENT = f"{NS}:mirror_link"
MIRRORS = [("magic_mirror", "mirrorframe"), ("essentia_mirror", "mirrorframe2")]


def mirror_item(ctx, name, frame, pane):
    ctx.item_model(name, {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": ctx.ref(frame), "layer1": ctx.ref(pane)},
    })


def mirrors(ctx):
    for name, frame in MIRRORS:
        ctx.particle_model(name, frame)
        ctx.blockstate(name, {"variants": {
            f"facing={facing}": {"model": ctx.ref(name)} for facing in FACES
        }})
        mirror_item(ctx, name, frame, "mirrorpane")
        mirror_item(ctx, f"{name}_linked", frame, "mirrorpaneopen")
        ctx.write(ctx.ASSETS / "items" / f"{name}.json", {
            "model": {
                "type": "minecraft:condition",
                "property": "minecraft:has_component",
                "component": LINK_COMPONENT,
                "on_true": {"type": "minecraft:model", "model": ctx.ref(f"{name}_linked", "item")},
                "on_false": {"type": "minecraft:model", "model": ctx.ref(name, "item")},
            },
        })
        ctx.self_drop(name)


def items(ctx):
    ctx.generated_item("hand_mirror", "mirrorhand")
    ctx.generated_item("essentia_resonator", "resonator")
    for name, texture in [("sinister_lodestone_active", "sinister_stone_active"), ("sinister_lodestone_inactive", "sinister_stone")]:
        ctx.item_model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": ctx.ref(texture, "item")}})
    ctx.write(ctx.ASSETS / "items" / "sinister_lodestone.json", {
        "model": {
            "type": "minecraft:condition",
            "property": f"{NS}:sinister_lodestone_active",
            "on_true": {"type": "minecraft:model", "model": ctx.ref("sinister_lodestone_active", "item")},
            "on_false": {"type": "minecraft:model", "model": ctx.ref("sinister_lodestone_inactive", "item")},
        },
    })


def generate(ctx):
    mirrors(ctx)
    items(ctx)
