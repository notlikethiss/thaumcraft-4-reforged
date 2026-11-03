from .common import NS, mc_box

RESEARCH_TINT = {"type": f"{NS}:research_note"}
DIRECTIONS = {"east": 0, "south": 90, "west": 180, "north": 270}


def tables(ctx):
    ctx.copy_texture("table", "table_model")
    ctx.copy_texture("restable", "research_table_model")
    ctx.block_model("table", {
        "parent": "minecraft:block/block",
        "textures": {"particle": ctx.ref("table_model"), "t": ctx.ref("table_model")},
        "elements": [
            mc_box((-8, 0, -8), (16, 4, 16), (0, 0), (64, 32)),
            mc_box((2, 4, -2), (4, 8, 4), (0, 20), (64, 32)),
            mc_box((-6, 4, -2), (4, 8, 4), (0, 20), (64, 32)),
            mc_box((-8, 12, -4), (16, 4, 8), (16, 20), (64, 32)),
        ],
    })
    ctx.blockstate("table", {"variants": {
        "axis=x": {"model": ctx.ref("table")},
        "axis=z": {"model": ctx.ref("table"), "y": 90},
    }})
    ctx.block_item("table")
    ctx.self_drop("table")

    ctx.block_model("research_table", {
        "parent": "minecraft:block/block",
        **({"render_type": "minecraft:solid"} if ctx.profile.explicit_render_types else {}),
        "textures": {"particle": ctx.ref("research_table_model"), "t": ctx.ref("research_table_model")},
        "elements": [
            mc_box((-8, 0, -8), (32, 4, 16), (0, 0), (128, 64)),
            mc_box((-6, 4, -6), (4, 12, 4), (0, 24), (128, 64)),
            mc_box((-6, 4, 2), (4, 12, 4), (0, 24), (128, 64)),
            mc_box((18, 4, -6), (4, 12, 4), (0, 24), (128, 64)),
            mc_box((18, 4, 2), (4, 12, 4), (0, 24), (128, 64)),
            mc_box((-4, 10, -2), (24, 4, 4), (24, 24), (128, 64)),
        ],
    })
    ctx.block_model("research_table_part", {
        "parent": "minecraft:block/block",
        "textures": {"particle": ctx.ref("research_table_model")},
    })
    ctx.blockstate("research_table", {"variants": {
        f"facing={name}": {"model": ctx.ref("research_table"), "y": rotation} if rotation else {"model": ctx.ref("research_table")}
        for name, rotation in DIRECTIONS.items()
    }})
    ctx.blockstate("research_table_part", {"variants": {
        f"facing={name}": {"model": ctx.ref("research_table_part")} for name in DIRECTIONS
    }})
    for name in ["research_table", "research_table_part"]:
        ctx.write_loot(name, [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"{NS}:table"}],
            "condition": {"type": "minecraft:survives_explosion"},
        }])
    ctx.mineable("axe", "table", "research_table", "research_table_part")


def items(ctx):
    ctx.generated_item("scribing_tools", "inkwell")
    white = ctx.tint(0xFFFFFF)
    ctx.generated_item("research_notes", ["researchnotes", "researchnotesoverlay"], [white, RESEARCH_TINT])
    ctx.generated_item("discovery", ["discovery", "discoveryoverlay"], [white, RESEARCH_TINT])
    ctx.generated_item("thaumonomicon", "thaumonomicon")
    ctx.generated_item("thaumonomicon_cheat", "thaumonomiconcheat")


def generate(ctx):
    tables(ctx)
    items(ctx)
