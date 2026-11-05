from . import (
    alchemy,
    arcane,
    biomes,
    blocks,
    devices,
    eldritch,
    entities,
    equipment,
    finalize,
    font,
    golems,
    items,
    loot,
    machines,
    mirrors,
    node_shaders,
    nodes,
    recipes,
    research,
    taint,
    villages,
    warded,
    wands,
    warp_shaders,
    world,
)
from .context import Context

STEPS = [
    blocks.generate,
    items.generate,
    recipes.recipes,
    font.galactic_font,
    world.generate,
    biomes.generate,
    loot.generate,
    villages.generate,
    nodes.generate,
    wands.generate,
    equipment.generate,
    research.generate,
    arcane.generate,
    alchemy.generate,
    devices.generate,
    warded.generate,
    machines.generate,
    mirrors.generate,
    entities.generate,
    taint.generate,
    golems.generate,
    eldritch.generate,
    warp_shaders.generate,
    node_shaders.generate,
    recipes.tc_recipes,
    finalize.write_fuels,
    finalize.write_tags,
    finalize.validate_textures,
    finalize.validate_models,
]


def build(profile, out):
    ctx = Context(profile, out)
    for step in STEPS:
        step(ctx)
    return ctx
