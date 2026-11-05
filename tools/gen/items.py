from .blocks import INFUSED_COLORS, INFUSED_ELEMENTS
from .common import NS

ASPECT_TINT = {"type": f"{NS}:aspect"}

ITEMS = {
    "alumentum": "alumentum",
    "nitor": "nitor",
    "thaumium_ingot": "thaumiumingot",
    "quicksilver": "quicksilver",
    "magic_tallow": "tallow",
    "amber": "amber",
    "enchanted_fabric": "cloth",
    "vis_filter": "filter",
    "knowledge_fragment": "knowledgefragment",
    "mirrored_glass": "mirrorglass",
    "tainted_goo": "taint_slime",
    "taint_tendril": "taint_tendril",
    "jar_label": "label",
    "salis_mundus": "dust",
    "primal_charm": "charm",
    "void_metal_ingot": "voidingot",
    "void_seed": "voidseed",
    "gold_coin": "coin",
    "copper_nugget": "nuggetcopper",
    "tin_nugget": "nuggettin",
    "silver_nugget": "nuggetsilver",
    "lead_nugget": "nuggetlead",
    "quicksilver_drop": "nuggetquicksilver",
    "thaumium_nugget": "nuggetthaumium",
    "void_metal_nugget": "nuggetvoid",
    "native_iron_cluster": "clusteriron",
    "native_copper_cluster": "clustercopper",
    "native_tin_cluster": "clustertin",
    "native_silver_cluster": "clustersilver",
    "native_lead_cluster": "clusterlead",
    "native_cinnabar_cluster": "clustercinnabar",
    "native_gold_cluster": "clustergold",
    "chicken_nugget": "nuggetchicken",
    "beef_nugget": "nuggetbeef",
    "pork_nugget": "nuggetpork",
    "fish_nugget": "nuggetfish",
    "triple_meat_treat": "tripletreat",
    "glass_phial": "phial",
    "zombie_brain": "brain",
    "common_loot_bag": "lootbag",
    "uncommon_loot_bag": "lootbagunc",
    "rare_loot_bag": "lootbagrare",
    "balanced_shard": "shard_balanced",
}


def generate(ctx):
    for name, texture in ITEMS.items():
        ctx.generated_item(name, texture)
    for element, color in zip(INFUSED_ELEMENTS, INFUSED_COLORS):
        ctx.generated_item(f"{element}_shard", "shard", [ctx.tint(color)])
        ctx.item_tags[f"{NS}:shards"].add(f"{NS}:{element}_shard")
    ctx.item_tags[f"{NS}:shards"].add(f"{NS}:balanced_shard")
    ctx.generated_item("essentia_phial", ["phial", "essence"], [ctx.tint(0xFFFFFF), ASPECT_TINT])
    ctx.generated_item("wisp_essence", "wispessence", [ASPECT_TINT])
    ctx.generated_item("crystallized_essence", "crystalessence", [ASPECT_TINT])
    ctx.generated_item("mana_bean", "mana_bean", [ASPECT_TINT])

    for metal in ["thaumium", "void_metal"]:
        ctx.item_tags[f"c:ingots/{metal}"].add(f"{NS}:{metal}_ingot")
        ctx.item_tags["c:ingots"].add(f"{NS}:{metal}_ingot")
    for metal in ["thaumium", "void_metal", "quicksilver", "copper", "tin", "silver", "lead"]:
        source = "quicksilver_drop" if metal == "quicksilver" else f"{metal}_nugget"
        ctx.item_tags[f"c:nuggets/{metal}"].add(f"{NS}:{source}")
        ctx.item_tags["c:nuggets"].add(f"{NS}:{source}")
    ctx.item_tags["c:gems/amber"].add(f"{NS}:amber")
    ctx.item_tags["c:gems"].add(f"{NS}:amber")
    for metal in ["iron", "copper", "tin", "silver", "lead", "cinnabar", "gold"]:
        ctx.item_tags[f"{NS}:clusters"].add(f"{NS}:native_{metal}_cluster")
