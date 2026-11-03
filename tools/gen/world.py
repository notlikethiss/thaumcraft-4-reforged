def generate(ctx):
    profile = ctx.profile
    namespace = ctx.NS
    ctx.write(
        ctx.DATA / namespace / "worldgen" / profile.feature_folder / "world_generation.json",
        profile.configured_feature({"type": f"{namespace}:world_generation"}),
    )
    ctx.write(
        ctx.DATA / namespace / "worldgen" / "placed_feature" / "world_generation.json",
        {"feature": f"{namespace}:world_generation", "placement": []},
    )
    ctx.write(
        ctx.DATA / namespace / profile.biome_modifier_namespace / "biome_modifier" / "world_generation_overworld.json",
        profile.feature_modifier("#minecraft:is_overworld", f"{namespace}:world_generation", "top_layer_modification"),
    )
