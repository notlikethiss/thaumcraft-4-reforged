import shutil

from .arcane import table_model
from .common import NS

ROBE_COLOR = 6961280
PRIMAL_COLORS = {
    "air": 0xFFFF7E,
    "earth": 0x56C000,
    "fire": 0xFF5A01,
    "water": 0x3CD4FC,
    "order": 0xD5D4EC,
    "entropy": 0x404040,
}

FOCI = {
    "focus_pouch": "focuspouch",
    "focus_fire": "focus_fire",
    "focus_shock": "focus_shock",
    "focus_frost": "focus_frost",
    "focus_excavation": "focus_excavation",
    "focus_portable_hole": "focus_portablehole",
    "focus_pech": "focus_pech",
    "focus_primal": "focus_primal",
}
ORNAMENT_FOCI = {
    "focus_nine_hells": ("focus_hellbat_orn", "focus_hellbat"),
    "focus_equal_trade": ("focus_trade_orn", "focus_trade"),
    "focus_warding": ("focus_warding_orn", "focus_warding"),
}

TOOLS = {
    "thaumium_sword": "thaumiumsword",
    "thaumium_pickaxe": "thaumiumpick",
    "thaumium_axe": "thaumiumaxe",
    "thaumium_shovel": "thaumiumshovel",
    "thaumium_hoe": "thaumiumhoe",
    "void_sword": "voidsword",
    "void_pickaxe": "voidpick",
    "void_axe": "voidaxe",
    "void_shovel": "voidshovel",
    "void_hoe": "voidhoe",
    "elemental_sword": "elementalsword",
    "elemental_pickaxe": "elementalpick",
    "elemental_axe": "elementalaxe",
    "elemental_shovel": "elementalshovel",
    "elemental_hoe": "elementalhoe",
    "crimson_blade": "crimson_blade",
    "primal_crusher": "primal_crusher",
}

ARROWS = {f"{element}_arrow": f"el_arrow_{element}" for element in PRIMAL_COLORS}

ARMOR_ICONS = {
    "thaumium_helmet": "thaumiumhelm",
    "thaumium_chestplate": "thaumiumchest",
    "thaumium_leggings": "thaumiumlegs",
    "thaumium_boots": "thaumiumboots",
    "boots_traveller": "bootstraveler",
    "hover_harness": "hoverharness",
    "fortress_helmet": "thaumiumfortresshelm",
    "fortress_chestplate": "thaumiumfortresschest",
    "fortress_leggings": "thaumiumfortresslegs",
    "void_helmet": "voidhelm",
    "void_chestplate": "voidchest",
    "void_leggings": "voidlegs",
    "void_boots": "voidboots",
    "crimson_cult_hood": "cultistrobehelm",
    "crimson_cult_robe": "cultistrobechest",
    "crimson_cult_leggings": "cultistrobelegs",
    "crimson_cult_boots": "cultistboots",
    "crimson_cult_helm": "cultistplatehelm",
    "crimson_cult_chestplate": "cultistplatechest",
    "crimson_cult_greaves": "cultistplatelegs",
    "crimson_praetor_helm": "cultistplateleaderhelm",
    "crimson_praetor_chestplate": "cultistplateleaderchest",
    "crimson_praetor_greaves": "cultistplateleaderlegs",
}

DYED_ARMOR_ICONS = {
    "robe_chestplate": ("clothchest", "clothchestover"),
    "robe_leggings": ("clothlegs", "clothlegsover"),
    "robe_boots": ("clothboots", "clothbootsover"),
    "void_robe_helmet": ("voidrobehelm", "blank"),
    "void_robe_chestplate": ("voidrobechestover", "voidrobechest"),
    "void_robe_leggings": ("voidrobelegsover", "voidrobelegs"),
}

BAUBLES = {
    "mundane_amulet": "bauble_amulet",
    "mundane_ring": "bauble_ring",
    "mundane_belt": "bauble_belt",
    "runic_shielding_amulet": "runic_amulet",
    "emergency_shielding_amulet": "runic_amulet_emergency",
    "protection_ring": "runic_ring_lesser",
    "runic_shielding_ring": "runic_ring",
    "charged_shielding_ring": "runic_ring_charged",
    "revitalizing_shielding_ring": "runic_ring_regen",
    "runic_shielding_girdle": "runic_girdle",
    "kinetic_shielding_girdle": "runic_girdle_kinetic",
    "vis_stone": "vis_amulet_lesser",
    "vis_amulet": "vis_amulet",
    "thaumostatic_girdle": "hovergirdle",
}

EQUIPMENT = {
    "thaumium": {"humanoid": "thaumium_1", "humanoid_leggings": "thaumium_2"},
    "void": {"humanoid": "void_1", "humanoid_leggings": "void_2"},
    "robe": {"humanoid": "robes_1", "humanoid_leggings": "robes_2", "overlay": ("robes_1_overlay", "robes_2_overlay")},
    "void_robe": {
        "humanoid": "void_robe_armor_overlay", "humanoid_leggings": "void_robe_armor_overlay",
        "overlay": ("void_robe_armor", "void_robe_armor"),
    },
    "boots_traveller": {"humanoid": "bootstraveler"},
    "hover_harness": {"humanoid": "hoverharness"},
    "fortress": {"humanoid": "fortress_armor", "humanoid_leggings": "fortress_armor"},
    "cultist_robe": {"humanoid": "cultist_robe_armor", "humanoid_leggings": "cultist_robe_armor"},
    "cultist_plate": {"humanoid": "cultist_plate_armor", "humanoid_leggings": "cultist_plate_armor"},
    "cultist_boots": {"humanoid": "cultistboots"},
    "cultist_leader": {"humanoid": "cultist_leader_armor", "humanoid_leggings": "cultist_leader_armor"},
}
DYED_EQUIPMENT = {"robe", "void_robe"}

HEAD = [
    "thaumium_helmet", "fortress_helmet", "void_helmet", "void_robe_helmet", "crimson_cult_hood",
    "crimson_cult_helm", "crimson_praetor_helm", "goggles_of_revealing",
]
CHEST = [
    "thaumium_chestplate", "robe_chestplate", "hover_harness", "fortress_chestplate", "void_chestplate",
    "void_robe_chestplate", "crimson_cult_robe", "crimson_cult_chestplate", "crimson_praetor_chestplate",
]
LEGS = [
    "thaumium_leggings", "robe_leggings", "fortress_leggings", "void_leggings", "void_robe_leggings",
    "crimson_cult_leggings", "crimson_cult_greaves", "crimson_praetor_greaves",
]
FEET = ["thaumium_boots", "robe_boots", "boots_traveller", "void_boots", "crimson_cult_boots"]
ARMOR = HEAD + CHEST + LEGS + FEET

SWORDS = ["thaumium_sword", "void_sword", "elemental_sword", "crimson_blade"]
PICKAXES = ["thaumium_pickaxe", "void_pickaxe", "elemental_pickaxe"]
AXES = ["thaumium_axe", "void_axe", "elemental_axe"]
SHOVELS = ["thaumium_shovel", "void_shovel", "elemental_shovel"]
HOES = ["thaumium_hoe", "void_hoe", "elemental_hoe"]
DIGGERS = PICKAXES + AXES + SHOVELS + HOES + ["primal_crusher"]
TOOL_ITEMS = SWORDS + DIGGERS
REPAIRABLE = TOOL_ITEMS + ["bone_bow"] + ARMOR
ROBES = ["robe_chestplate", "robe_leggings", "robe_boots", "void_robe_helmet", "void_robe_chestplate", "void_robe_leggings"]


def dye_tint():
    return {"type": "minecraft:dye", "default": (ROBE_COLOR | 0xFF000000) - 0x100000000}


def handheld_item(ctx, name, texture):
    ctx.item_model(name, {"parent": "minecraft:item/handheld", "textures": {"layer0": ctx.ref(texture, "item")}})
    ctx.item_definition(name, ctx.ref(name, "item"))


def foci(ctx):
    for name, texture in FOCI.items():
        ctx.generated_item(name, texture)
    for name, layers in ORNAMENT_FOCI.items():
        ctx.generated_item(name, list(layers))


def tools(ctx):
    for name, texture in TOOLS.items():
        handheld_item(ctx, name, texture)
    for name, texture in ARROWS.items():
        ctx.generated_item(name, texture)
        ctx.item_tags["minecraft:arrows"].add(f"{NS}:{name}")


def bow(ctx):
    for stage in range(3):
        ctx.item_model(f"bone_bow_pulling_{stage}", {
            "parent": "minecraft:item/bow",
            "textures": {"layer0": ctx.ref(f"bonebow_pulling_{stage}", "item")},
        })
    ctx.item_model("bone_bow", {"parent": "minecraft:item/bow", "textures": {"layer0": ctx.ref("bonebow", "item")}})
    ctx.ITEM_IDS.add("bone_bow")
    if not ctx.profile.modern:
        ctx.item_model("bone_bow", {**ctx.ITEM_MODELS["bone_bow"], "overrides": [
            {"predicate": {"pulling": 1}, "model": ctx.ref("bone_bow_pulling_0", "item")},
            {"predicate": {"pulling": 1, "pull": 0.4}, "model": ctx.ref("bone_bow_pulling_1", "item")},
            {"predicate": {"pulling": 1, "pull": 0.65}, "model": ctx.ref("bone_bow_pulling_2", "item")},
        ]})
        ctx.MANIFEST["predicates"]["bone_bow"] = {
            "minecraft:pull": {"kind": "bow_pull", "scale": 0.05},
            "minecraft:pulling": {"kind": "using_item"},
        }
        return
    ctx.write(ctx.ASSETS / "items" / "bone_bow.json", {
        "model": {
            "type": "minecraft:condition",
            "property": "minecraft:using_item",
            "on_false": {"type": "minecraft:model", "model": ctx.ref("bone_bow", "item")},
            "on_true": {
                "type": "minecraft:range_dispatch",
                "property": "minecraft:use_duration",
                "scale": 0.05,
                "entries": [
                    {"threshold": 0.4, "model": {"type": "minecraft:model", "model": ctx.ref("bone_bow_pulling_1", "item")}},
                    {"threshold": 0.65, "model": {"type": "minecraft:model", "model": ctx.ref("bone_bow_pulling_2", "item")}},
                ],
                "fallback": {"type": "minecraft:model", "model": ctx.ref("bone_bow_pulling_0", "item")},
            },
        },
    })


def armor_items(ctx):
    for name, texture in ARMOR_ICONS.items():
        ctx.generated_item(name, texture)
    for name, layers in DYED_ARMOR_ICONS.items():
        ctx.generated_item(name, list(layers), [dye_tint()])


def baubles(ctx):
    for name, texture in BAUBLES.items():
        ctx.generated_item(name, texture)
    for element, color in PRIMAL_COLORS.items():
        ctx.generated_item(f"apprentice_ring_{element}", "bauble_ring_iron", [ctx.tint(color)])


def copy_equipment_texture(ctx, layer, source, target):
    destination = ctx.ASSETS / "textures" / "entity" / "equipment" / layer / f"{target}.png"
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(ctx.SOURCE_TEXTURES / "model" / f"{source}.png", destination)


def legacy_equipment(ctx, asset, textures):
    layers = []
    for index, layer in enumerate(["humanoid", "humanoid_leggings"], start=1):
        if layer not in textures:
            continue
        suffixes = [("", textures[layer])]
        if textures.get("overlay"):
            suffixes.append(("_overlay", textures["overlay"][index - 1]))
        for suffix, source in suffixes:
            texture = f"textures/models/armor/{asset}_layer_{index}{suffix}.png"
            destination = ctx.ASSETS / texture
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ctx.SOURCE_TEXTURES / "model" / f"{source}.png", destination)
            layers.append({"layer": index, "suffix": suffix, "texture": f"{NS}:{texture}", "dyeable": asset in DYED_EQUIPMENT and not suffix})
    ctx.MANIFEST["armor_layers"][asset] = {"layer_id": f"{NS}:{asset}", "layers": layers}


def equipment(ctx):
    for asset, textures in EQUIPMENT.items():
        if not ctx.profile.modern:
            legacy_equipment(ctx, asset, textures)
            continue
        dyed = asset in DYED_EQUIPMENT
        overlays = textures.get("overlay")
        layers = {}
        for index, layer in enumerate(["humanoid", "humanoid_leggings"]):
            if layer not in textures:
                continue
            copy_equipment_texture(ctx, layer, textures[layer], asset)
            entries = [{"texture": f"{NS}:{asset}"}]
            if dyed:
                entries[0]["dyeable"] = {"color_when_undyed": ROBE_COLOR}
            if overlays:
                overlay_name = f"{asset}_overlay"
                copy_equipment_texture(ctx, layer, overlays[index], overlay_name)
                entries.append({"texture": f"{NS}:{overlay_name}"})
            layers[layer] = entries
        ctx.write(ctx.ASSETS / "equipment" / f"{asset}.json", {"layers": layers})


def blocks(ctx):
    table_model(ctx, "focal_manipulator", "wandtable")
    ctx.block_tags["minecraft:mineable/axe"].discard(f"{NS}:focal_manipulator")
    ctx.mineable("pickaxe", "focal_manipulator")
    ctx.particle_model("hole", "blank", "item")
    ctx.simple_state("hole")
    ctx.write_loot("hole", [])
    ctx.particle_model("warded_block", "wardedstone")
    if ctx.profile.modern:
        ctx.blockstate("warded_block", {"variants": {"": {"type": f"{NS}:warded", "fallback": {"model": "minecraft:block/stone"}}}})
    else:
        ctx.block_model("warded_block", {"parent": "minecraft:block/stone", "loader": f"{NS}:warded"})
        ctx.simple_state("warded_block")
        ctx.MANIFEST["model_loaders"]["warded_block"] = {"loader": f"{NS}:warded", "fallback": "minecraft:block/stone"}
    ctx.write_loot("warded_block", [])
    ctx.block_tags["thaumcraft:mineable/primal_crusher"].update(
        ["#minecraft:mineable/pickaxe", "#minecraft:mineable/shovel"]
    )


def add_items(ctx, tag, names):
    for name in names:
        ctx.item_tags[tag].add(name if ":" in name or name.startswith("#") else f"{NS}:{name}")


def tags(ctx):
    add_items(ctx, f"{NS}:repairs_thaumium", ["thaumium_ingot"])
    add_items(ctx, f"{NS}:repairs_void_metal", ["void_metal_ingot"])
    add_items(ctx, f"{NS}:repairs_primal_charm", ["primal_charm"])
    add_items(ctx, f"{NS}:repairs_enchanted_fabric", ["enchanted_fabric"])
    ctx.item_tags[f"{NS}:repairs_none"]

    add_items(ctx, "minecraft:swords", SWORDS)
    add_items(ctx, "minecraft:pickaxes", PICKAXES)
    add_items(ctx, "minecraft:axes", AXES)
    add_items(ctx, "minecraft:shovels", SHOVELS)
    add_items(ctx, "minecraft:hoes", HOES)
    add_items(ctx, "minecraft:head_armor", HEAD)
    add_items(ctx, "minecraft:chest_armor", CHEST)
    add_items(ctx, "minecraft:leg_armor", LEGS)
    add_items(ctx, "minecraft:foot_armor", FEET)

    add_items(ctx, "minecraft:enchantable/durability", REPAIRABLE)
    add_items(ctx, "minecraft:enchantable/weapon", SWORDS + AXES)
    add_items(ctx, "minecraft:enchantable/sharp_weapon", SWORDS + AXES)
    add_items(ctx, "minecraft:enchantable/melee_weapon", SWORDS)
    add_items(ctx, "minecraft:enchantable/sweeping", SWORDS)
    add_items(ctx, "minecraft:enchantable/fire_aspect", SWORDS)
    add_items(ctx, "minecraft:enchantable/mining", DIGGERS)
    add_items(ctx, "minecraft:enchantable/mining_loot", DIGGERS)
    add_items(ctx, "minecraft:enchantable/bow", ["bone_bow"])
    add_items(ctx, "minecraft:enchantable/armor", ARMOR)
    add_items(ctx, "minecraft:enchantable/head_armor", HEAD)
    add_items(ctx, "minecraft:enchantable/chest_armor", CHEST)
    add_items(ctx, "minecraft:enchantable/leg_armor", LEGS)
    add_items(ctx, "minecraft:enchantable/foot_armor", FEET)
    add_items(ctx, "minecraft:cauldron_can_remove_dye" if ctx.profile.modern else "minecraft:dyeable", ROBES)

    add_items(ctx, "c:tools", TOOL_ITEMS + ["bone_bow"])
    add_items(ctx, "c:tools/melee_weapon", SWORDS)
    add_items(ctx, "c:tools/mining_tool", DIGGERS)
    add_items(ctx, "c:tools/bow", ["bone_bow"])
    add_items(ctx, "c:tools/ranged_weapon", ["bone_bow"])
    add_items(ctx, "c:armors", ARMOR)
    add_items(ctx, "c:armors/humanoid", ARMOR)

    add_items(ctx, f"{NS}:enchantable/haste", ["#minecraft:enchantable/foot_armor", "hover_harness"])
    add_items(ctx, f"{NS}:enchantable/repair", REPAIRABLE)


def enchantment(ctx, name, max_level, weight, anvil_cost, min_base, min_step, slots, supported, exclusive=None):
    content = {
        "anvil_cost": anvil_cost,
        "description": {"translate": f"enchantment.{NS}.{name}"},
        "effects": {},
        "max_cost": {"base": 61, "per_level_above_first": 10},
        "max_level": max_level,
        "min_cost": {"base": min_base, "per_level_above_first": min_step},
        "slots": slots,
        "supported_items": supported,
        "weight": weight,
    }
    if exclusive:
        content["exclusive_set"] = exclusive
    ctx.write(ctx.DATA / NS / "enchantment" / f"{name}.json", content)


ENCHANTMENT_TAGS = ["in_enchanting_table", "non_treasure", "on_random_loot", "tradeable", "on_mob_spawn_equipment", "on_traded_equipment"]


def enchantments(ctx):
    if not ctx.profile.enchantment_json:
        return
    enchantment(ctx, "haste", 3, 3, 2, 15, 9, ["feet", "chest"], f"#{NS}:enchantable/haste")
    enchantment(ctx, "repair", 2, 2, 4, 20, 10, ["any"], f"#{NS}:enchantable/repair", "minecraft:unbreaking")
    for tag in ENCHANTMENT_TAGS:
        ctx.write(ctx.DATA / "minecraft" / "tags" / "enchantment" / f"{tag}.json", {"replace": False, "values": [f"{NS}:haste", f"{NS}:repair"]})


def recipes(ctx):
    for name in ["robe_armor_dye", "void_robe_armor_dye"]:
        ctx.recipe(name, {"type": f"{NS}:{name}"}, [])


def generate(ctx):
    foci(ctx)
    tools(ctx)
    bow(ctx)
    armor_items(ctx)
    baubles(ctx)
    equipment(ctx)
    blocks(ctx)
    tags(ctx)
    enchantments(ctx)
    recipes(ctx)
