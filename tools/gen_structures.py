import gzip
import struct
from pathlib import Path

import mcformat

NS = "thaumcraft"
BIOMES = {"plains": "plains", "taiga": "taiga", "savanna": "savanna", "snowy": "snow", "desert": "desert"}
WOODS = {"plains": "oak", "taiga": "spruce", "savanna": "acacia", "snowy": "spruce", "desert": "oak"}

TAG_BYTE, TAG_INT, TAG_DOUBLE, TAG_STRING, TAG_LIST, TAG_COMPOUND = 1, 3, 6, 8, 9, 10


class Byte(int):
    pass


class Int(int):
    pass


class Double(float):
    pass


class TagList(list):
    def __init__(self, items, kind=None):
        super().__init__(items)
        self.kind = kind


def tag_type(value):
    if isinstance(value, Byte):
        return TAG_BYTE
    if isinstance(value, Int):
        return TAG_INT
    if isinstance(value, Double):
        return TAG_DOUBLE
    if isinstance(value, str):
        return TAG_STRING
    if isinstance(value, TagList):
        return TAG_LIST
    if isinstance(value, dict):
        return TAG_COMPOUND
    raise TypeError(type(value))


def write_string(out, value):
    encoded = value.encode("utf-8")
    out.append(struct.pack(">H", len(encoded)))
    out.append(encoded)


def write_payload(out, value):
    kind = tag_type(value)
    if kind == TAG_BYTE:
        out.append(struct.pack(">b", value))
    elif kind == TAG_INT:
        out.append(struct.pack(">i", value))
    elif kind == TAG_DOUBLE:
        out.append(struct.pack(">d", value))
    elif kind == TAG_STRING:
        write_string(out, value)
    elif kind == TAG_LIST:
        item_kind = value.kind if value.kind is not None else (tag_type(value[0]) if value else 0)
        out.append(struct.pack(">bi", item_kind, len(value)))
        for item in value:
            write_payload(out, item)
    else:
        for key, item in value.items():
            out.append(struct.pack(">b", tag_type(item)))
            write_string(out, key)
            write_payload(out, item)
        out.append(b"\x00")


def to_nbt(root):
    out = [struct.pack(">b", TAG_COMPOUND)]
    write_string(out, "")
    write_payload(out, root)
    return gzip.compress(b"".join(out), mtime=0)


def block_state(name, **properties):
    return (name, tuple(sorted(properties.items())))


def stairs(name, facing):
    return block_state(name, facing=facing, half="bottom", shape="straight", waterlogged="false")


def pane(east_west=True):
    return block_state(
        "minecraft:glass_pane",
        east="true" if east_west else "false",
        west="true" if east_west else "false",
        north="false",
        south="false",
        waterlogged="false",
    )


def materials(biome):
    desert = biome == "desert"
    wood = WOODS[biome]
    return {
        "planks": block_state("minecraft:smooth_sandstone" if desert else f"minecraft:{wood}_planks"),
        "log": block_state("minecraft:cut_sandstone" if desert else f"minecraft:{wood}_log", axis="y"),
        "cobble": block_state("minecraft:sandstone" if desert else "minecraft:cobblestone"),
        "wall": "minecraft:sandstone_wall" if desert else "minecraft:cobblestone_wall",
        "stairs": "minecraft:sandstone_stairs" if desert else "minecraft:cobblestone_stairs",
        "door": f"minecraft:{wood}_door",
    }


def door(material, x, y, z, blocks):
    for half, offset in (("lower", 0), ("upper", 1)):
        blocks[(x, y + offset, z)] = block_state(
            material["door"], facing="south", half=half, hinge="left", open="false", powered="false"
        )


def fill_into(blocks, x1, y1, z1, x2, y2, z2, state):
    for x in range(x1, x2 + 1):
        for y in range(y1, y2 + 1):
            for z in range(z1, z2 + 1):
                blocks[(x, y, z)] = state


def jigsaw(material, position, blocks, nbts):
    blocks[position] = block_state("minecraft:jigsaw", orientation="north_up")
    nbts[position] = {
        "id": "minecraft:jigsaw",
        "name": "minecraft:building_entrance",
        "target": "minecraft:building_entrance",
        "pool": "minecraft:empty",
        "joint": "aligned",
        "final_state": state_string(stairs(material["stairs"], "south")),
    }


def tower(biome):
    material = materials(biome)
    blocks = {}
    nbts = {}
    air = block_state("minecraft:air")
    fill_into(blocks, 2, 1, 2, 4, 11, 4, air)
    for y in (0, 5, 10):
        fill_into(blocks, 2, y, 2, 4, y, 4, material["planks"])
    fill_into(blocks, 1, 0, 2, 1, 11, 4, material["cobble"])
    fill_into(blocks, 2, 0, 1, 4, 11, 1, material["cobble"])
    fill_into(blocks, 5, 0, 2, 5, 11, 4, material["cobble"])
    fill_into(blocks, 2, 0, 5, 4, 11, 5, material["cobble"])
    for y in (0, 5, 10):
        for x in (1, 5):
            for z in (1, 5):
                blocks[(x, y, z)] = material["cobble"]
    for position in [(3, 7, 1), (3, 8, 1), (3, 7, 5), (3, 8, 5), (3, 2, 5), (3, 3, 5)]:
        blocks[position] = pane()
    for y in range(1, 10):
        blocks[(4, y, 3)] = block_state("minecraft:ladder", facing="west", waterlogged="false")
    blocks[(4, 10, 3)] = block_state(
        "minecraft:oak_trapdoor", facing="north", half="bottom", open="true", powered="false", waterlogged="false"
    )
    blocks[(3, 5, 3)] = block_state("minecraft:glowstone")
    blocks[(2, 6, 2)] = block_state("minecraft:chest", facing="south", type="single", waterlogged="false")
    nbts[(2, 6, 2)] = {"id": "minecraft:chest", "LootTable": f"{NS}:chests/wizard_tower"}
    door(material, 3, 1, 1, blocks)
    jigsaw(material, (3, 0, 0), blocks, nbts)
    shifted = {(x - 1, y, z): state for (x, y, z), state in blocks.items()}
    shifted_nbts = {(x - 1, y, z): value for (x, y, z), value in nbts.items()}
    return shifted, shifted_nbts, (5, 12, 6), (2.5, 1.0, 3.5), "wizard"


def banker_home(biome):
    material = materials(biome)
    blocks = {}
    nbts = {}
    air = block_state("minecraft:air")
    fill_into(blocks, 1, 1, 1, 3, 5, 4, air)
    fill_into(blocks, 0, 0, 0, 3, 0, 4, material["cobble"])
    fill_into(blocks, 1, 0, 1, 2, 0, 3, block_state("minecraft:dirt"))
    fill_into(blocks, 1, 5, 1, 2, 5, 3, material["log"])
    for x, z in [(1, 0), (2, 0), (1, 4), (2, 4), (0, 1), (0, 2), (0, 3), (3, 1), (3, 2), (3, 3)]:
        blocks[(x, 4, z)] = material["log"]
    for x, z in [(0, 0), (3, 0), (0, 4), (3, 4)]:
        fill_into(blocks, x, 1, z, x, 3, z, material["log"])
    fill_into(blocks, 0, 1, 1, 0, 3, 3, material["planks"])
    fill_into(blocks, 3, 1, 1, 3, 3, 3, material["planks"])
    fill_into(blocks, 1, 1, 0, 2, 3, 0, material["planks"])
    fill_into(blocks, 1, 1, 4, 2, 3, 4, material["planks"])
    bars = block_state("minecraft:iron_bars", east="false", north="true", south="true", west="false", waterlogged="false")
    blocks[(0, 2, 2)] = bars
    blocks[(3, 2, 2)] = bars
    blocks[(2, 1, 3)] = block_state(material["wall"], east="none", north="none", south="none", west="none", up="true", waterlogged="false")
    blocks[(2, 2, 3)] = block_state("minecraft:stone_pressure_plate", powered="false")
    door(material, 1, 1, 0, blocks)
    jigsaw(material, (1, 0, -1), blocks, nbts)
    shifted = {(x, y, z + 1): state for (x, y, z), state in blocks.items()}
    shifted_nbts = {(x, y, z + 1): value for (x, y, z), value in nbts.items()}
    return shifted, shifted_nbts, (4, 6, 6), (1.5, 1.0, 3.5), "banker"


def state_string(state):
    name, properties = state
    if not properties:
        return name
    return name + "[" + ",".join(f"{key}={value}" for key, value in properties) + "]"


def template(biome, builder):
    blocks, nbts, size, villager_position, profession = builder(biome)
    palette = []
    entries = []
    for position in sorted(blocks, key=lambda p: (p[1], p[0], p[2])):
        state = blocks[position]
        if state not in palette:
            palette.append(state)
        entry = {
            "pos": TagList([Int(value) for value in position], TAG_INT),
            "state": Int(palette.index(state)),
        }
        if position in nbts:
            entry["nbt"] = nbts[position]
        entries.append(entry)
    palette_tag = []
    for name, properties in palette:
        name_key, properties_key = mcformat.profile.block_state_keys
        entry = {name_key: name}
        if properties:
            entry[properties_key] = {key: value for key, value in properties}
        palette_tag.append(entry)
    block_position = [int(value) for value in villager_position]
    villager = {
        "pos": TagList([Double(value) for value in villager_position], TAG_DOUBLE),
        "blockPos": TagList([Int(value) for value in block_position], TAG_INT),
        "nbt": {
            "id": "minecraft:villager",
            "VillagerData": {"profession": f"{NS}:{profession}", "level": Int(1), "type": f"minecraft:{BIOMES[biome]}"},
            "Xp": Int(1),
            "PersistenceRequired": Byte(1),
        },
    }
    return {
        "DataVersion": Int(mcformat.profile.data_version),
        "size": TagList([Int(value) for value in size], TAG_INT),
        "palette": TagList(palette_tag, TAG_COMPOUND),
        "blocks": TagList(entries, TAG_COMPOUND),
        "entities": TagList([villager], TAG_COMPOUND),
    }


def test_platform(size=(7, 7, 7)):
    entries = []
    for x in range(size[0]):
        for z in range(size[2]):
            entries.append({"pos": TagList([Int(x), Int(0), Int(z)], TAG_INT), "state": Int(0)})
    return {
        "DataVersion": Int(mcformat.profile.data_version),
        "size": TagList([Int(value) for value in size], TAG_INT),
        "palette": TagList([{mcformat.profile.block_state_keys[0]: "minecraft:stone"}], TAG_COMPOUND),
        "blocks": TagList(entries, TAG_COMPOUND),
        "entities": TagList([], TAG_COMPOUND),
    }


def generate(data_dir):
    path = Path(data_dir) / NS / mcformat.profile.structure_folder / "test" / "platform.nbt"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(to_nbt(test_platform()))
    path.with_name("eldritch_regression.nbt").write_bytes(to_nbt(test_platform((32, 9, 32))))
    for biome in BIOMES:
        for name, builder in (("wizard_tower", tower), ("banker_home", banker_home)):
            path = Path(data_dir) / NS / mcformat.profile.structure_folder / "village" / biome / f"{name}.nbt"
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(to_nbt(template(biome, builder)))
