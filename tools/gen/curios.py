from .common import NS

SLOTS = {
    "necklace": ["mundane_amulet", "runic_shielding_amulet", "emergency_shielding_amulet", "vis_stone", "vis_amulet"],
    "ring": [
        "mundane_ring", "protection_ring", "runic_shielding_ring", "charged_shielding_ring", "revitalizing_shielding_ring",
        "apprentice_ring_air", "apprentice_ring_earth", "apprentice_ring_fire", "apprentice_ring_water",
        "apprentice_ring_order", "apprentice_ring_entropy",
    ],
    "belt": ["mundane_belt", "runic_shielding_girdle", "kinetic_shielding_girdle", "thaumostatic_girdle", "focus_pouch"],
}
EXTRA_RING_SLOTS = 1


def generate(ctx):
    if ctx.profile.modern:
        return
    ctx.write(ctx.DATA / "curios" / "curios" / "slots" / "ring.json", {"size": EXTRA_RING_SLOTS, "operation": "ADD"})
    ctx.write(ctx.DATA / NS / "curios" / "entities" / "player.json", {
        "entities": ["minecraft:player"],
        "slots": list(SLOTS),
    })
    for slot, items in SLOTS.items():
        for item in items:
            ctx.item_tags[f"curios:{slot}"].add(f"{NS}:{item}")
