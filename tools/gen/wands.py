from .common import NS

CAPS = {
    "iron": "wand_cap_iron",
    "gold": "wand_cap_gold",
    "thaumium": "wand_cap_thaumium",
    "copper": "wand_cap_copper",
    "silver": "wand_cap_silver",
    "inert_silver": "wand_cap_silver_inert",
    "inert_thaumium": "wand_cap_thaumium_inert",
    "void": "wand_cap_void",
    "inert_void": "wand_cap_void_inert",
}

RODS = {
    "greatwood": "greatwood",
    "obsidian": "obsidian",
    "silverwood": "silverwood",
    "icy": "ice",
    "quartz": "quartz",
    "reed": "reed",
    "blazing": "blaze",
    "bone": "bone",
}


def wands(ctx):
    ctx.legacy_special_item("wand", {"type": f"{NS}:wand"}, "wand_rod_greatwood", f"{NS}:special_owner")
    for cap, texture in CAPS.items():
        ctx.generated_item(f"{cap}_wand_cap", texture)
    for rod, texture in RODS.items():
        ctx.generated_item(f"{rod}_rod", f"wand_rod_{texture}")
        ctx.generated_item(f"{rod}_staff_core", f"staff_rod_{texture}")
    ctx.generated_item("primal_staff_core", "staff_rod_primal")
    ctx.legacy_special_item("thaumometer", {"type": f"{NS}:thaumometer"}, "blank")
    ctx.generated_item("goggles_of_revealing", "gogglesrevealing")
    ctx.write(ctx.ASSETS / "equipment" / "goggles.json", {
        "layers": {"humanoid": [{"texture": f"{NS}:goggles"}]},
    })


def recipes(ctx):
    ctx.shaped("iron_wand_cap", f"{NS}:iron_wand_cap", 1, ["NNN", "N N"], {"N": "#c:nuggets/iron"})
    ctx.shaped(
        "wand_iron_wood", f"{NS}:wand", 1, ["  I", " S ", "I  "],
        {"I": f"{NS}:iron_wand_cap", "S": "#c:rods/wooden"},
        components={f"{NS}:wand_parts": {"cap": "iron", "rod": "wood"}},
    )


def generate(ctx):
    wands(ctx)
    recipes(ctx)
