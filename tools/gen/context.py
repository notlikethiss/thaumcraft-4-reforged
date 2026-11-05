import json
import shutil
from collections import defaultdict

import mcformat

from .common import NS, ROOT

LEGACY_VIEWS = [
    ("inventory", ["gui"]),
    ("equipped", ["thirdperson_lefthand", "thirdperson_righthand"]),
    ("first_person", ["firstperson_lefthand", "firstperson_righthand"]),
]


class Context:
    NS = NS

    def __init__(self, profile, out):
        self.profile = profile
        self.mcformat = mcformat
        self.OUT = out
        self.ASSETS = out / "assets" / NS
        self.DATA = out / "data"
        self.SOURCE_TEXTURES = ROOT / "src" / "main" / "resources" / "assets" / NS / "textures"
        self.block_tags = defaultdict(set)
        self.item_tags = defaultdict(set)

    def write(self, path, content):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(content, indent=2) + "\n", encoding="utf-8")

    def ref(self, name, kind="block"):
        return f"{NS}:{kind}/{name}"

    def tint(self, color):
        value = color | 0xFF000000
        if value >= 0x80000000:
            value -= 0x100000000
        return {"type": "minecraft:constant", "value": value}

    def copy_texture(self, source, name):
        target = self.ASSETS / "textures" / "block" / f"{name}.png"
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(self.SOURCE_TEXTURES / "model" / f"{source}.png", target)

    def block_model(self, name, content):
        self.write(self.ASSETS / "models" / "block" / f"{name}.json", content)

    def item_model(self, name, content):
        self.write(self.ASSETS / "models" / "item" / f"{name}.json", content)

    def item_definition(self, name, model, tints=None):
        definition = {"type": "minecraft:model", "model": model}
        if tints:
            definition["tints"] = tints
        self.write(self.ASSETS / "items" / f"{name}.json", {"model": definition})

    def blockstate(self, name, content):
        self.write(self.ASSETS / "blockstates" / f"{name}.json", content)

    def simple_state(self, name, model=None):
        self.blockstate(name, {"variants": {"": {"model": model or self.ref(name)}}})

    def cube_all(self, name, texture):
        self.block_model(name, {"parent": "minecraft:block/cube_all", "textures": {"all": self.ref(texture)}})

    def cube_column(self, name, end, side):
        self.block_model(name, {"parent": "minecraft:block/cube_column", "textures": {"end": self.ref(end), "side": self.ref(side)}})

    def particle_model(self, name, texture, kind="block"):
        self.block_model(name, {"parent": "minecraft:block/block", "textures": {"particle": self.ref(texture, kind)}})

    def generated_item(self, name, layers, tints=None, texture_kind="item"):
        if isinstance(layers, str):
            layers = [layers]
        self.item_model(name, {
            "parent": "minecraft:item/generated",
            "textures": {f"layer{index}": self.ref(layer, texture_kind) for index, layer in enumerate(layers)},
        })
        self.item_definition(name, self.ref(name, "item"), tints)

    def block_item(self, name, tints=None, model=None):
        self.item_definition(name, model or self.ref(name), tints)

    def item_base_from_block(self, name):
        self.item_model(name, {"parent": self.ref(name)})

    def item_base_particle(self, name, texture, kind="block"):
        self.item_model(name, {"parent": "minecraft:block/block", "textures": {"particle": self.ref(texture, kind)}})

    def special_item(self, name, model, base=None):
        self.write(self.ASSETS / "items" / f"{name}.json", {
            "model": {"type": "minecraft:special", "base": base or self.ref(name, "item"), "model": model},
        })

    def legacy_special_item(self, name, model, particle, kind="minecraft:special"):
        self.item_model(name, {"gui_light": "side", "textures": {"particle": self.ref(particle, "item")}})
        base = self.ref(name, "item")

        def special(view):
            return {"type": kind, "base": base, "model": {**model, "view": view}}

        self.write(self.ASSETS / "items" / f"{name}.json", {
            "model": {
                "type": "minecraft:select",
                "property": "minecraft:display_context",
                "cases": [{"when": contexts, "model": special(view)} for view, contexts in LEGACY_VIEWS],
                "fallback": special("entity"),
            },
        })

    def write_loot(self, name, pools):
        self.write(self.DATA / NS / self.profile.loot_folder / "blocks" / f"{name}.json", self.profile.loot_table({
            "type": "minecraft:block",
            "pools": pools,
            "random_sequence": f"{NS}:blocks/{name}",
        }))

    def self_drop(self, name):
        self.write_loot(name, [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
            "condition": {"type": "minecraft:survives_explosion"},
        }])

    def slab_drop(self, name):
        self.write_loot(name, [{
            "rolls": 1,
            "entries": [{
                "type": "minecraft:item",
                "name": f"{NS}:{name}",
                "modifier": [
                    {
                        "type": "minecraft:set_count",
                        "count": 2,
                        "add": False,
                        "condition": {"type": "minecraft:match_block", "blocks": f"{NS}:{name}", "state": {"type": "double"}},
                    },
                    {"type": "minecraft:explosion_decay"},
                ],
            }],
        }])

    def bonus_drop(self, block, item, base_max=1):
        modifiers = []
        if base_max > 1:
            modifiers.append({"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": base_max}})
        modifiers.append({
            "type": "minecraft:apply_bonus",
            "enchantment": "minecraft:fortune",
            "formula": "minecraft:uniform_bonus_count",
            "parameters": {"bonusMultiplier": 1},
        })
        modifiers.append({"type": "minecraft:explosion_decay"})
        self.write_loot(block, [{
            "rolls": 1,
            "entries": [{
                "type": "minecraft:alternatives",
                "children": [
                    {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": f"{NS}:{block}"},
                    {"type": "minecraft:item", "name": item, "modifier": modifiers},
                ],
            }],
        }])

    def shear_only(self, name):
        self.write_loot(name, [{
            "rolls": 1,
            "entries": [{
                "type": "minecraft:item",
                "condition": {"type": "minecraft:any_of", "terms": ["minecraft:tool/can_shear", "minecraft:tool/can_silk_touch"]},
                "name": f"{NS}:{name}",
            }],
        }])

    def mineable(self, tool, *blocks):
        for name in blocks:
            self.block_tags[f"minecraft:mineable/{tool}"].add(f"{NS}:{name}")

    def tag_both(self, tag, *names):
        for name in names:
            self.block_tags[tag].add(f"{NS}:{name}")
            self.item_tags[tag].add(f"{NS}:{name}")

    def stairs_state(self, name):
        variants = {}
        table = {
            "straight": ("stairs", {"east": 0, "west": 180, "south": 90, "north": 270}, {"east": 0, "west": 180, "south": 90, "north": 270}),
            "outer_right": ("stairs_outer", {"east": 0, "west": 180, "south": 90, "north": 270}, {"east": 90, "west": 270, "south": 180, "north": 0}),
            "outer_left": ("stairs_outer", {"east": 270, "west": 90, "south": 0, "north": 180}, {"east": 0, "west": 180, "south": 90, "north": 270}),
            "inner_right": ("stairs_inner", {"east": 0, "west": 180, "south": 90, "north": 270}, {"east": 90, "west": 270, "south": 180, "north": 0}),
            "inner_left": ("stairs_inner", {"east": 270, "west": 90, "south": 0, "north": 180}, {"east": 0, "west": 180, "south": 90, "north": 270}),
        }
        suffix = {"stairs": "", "stairs_outer": "_outer", "stairs_inner": "_inner"}
        for shape, (kind, bottom, top) in table.items():
            for facing in ["east", "west", "south", "north"]:
                for half, rotations in (("bottom", bottom), ("top", top)):
                    variant = {"model": self.ref(name + suffix[kind]), "y": rotations[facing]}
                    if half == "top":
                        variant["x"] = 180
                    if variant["y"] or half == "top":
                        variant["uvlock"] = True
                    if not variant["y"]:
                        del variant["y"]
                    variants[f"facing={facing},half={half},shape={shape}"] = variant
        self.blockstate(name, {"variants": variants})

    def stairs(self, name, texture):
        textures = {"bottom": self.ref(texture), "top": self.ref(texture), "side": self.ref(texture)}
        for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
            self.block_model(name + suffix, {"parent": f"minecraft:block/{parent}", "textures": textures})
        self.stairs_state(name)
        self.block_item(name)
        self.self_drop(name)

    def slab(self, name, texture):
        textures = {"bottom": self.ref(texture), "top": self.ref(texture), "side": self.ref(texture)}
        self.block_model(name, {"parent": "minecraft:block/slab", "textures": textures})
        self.block_model(f"{name}_top", {"parent": "minecraft:block/slab_top", "textures": textures})
        self.block_model(f"{name}_double", {"parent": "minecraft:block/cube_all", "textures": {"all": self.ref(texture)}})
        self.blockstate(name, {"variants": {
            "type=bottom": {"model": self.ref(name)},
            "type=top": {"model": self.ref(f"{name}_top")},
            "type=double": {"model": self.ref(f"{name}_double")},
        }})
        self.block_item(name)
        self.slab_drop(name)

    def recipe(self, name, content, referenced, extra_conditions=None):
        conditions = self.profile.recipe_conditions(sorted(value for value in referenced if isinstance(value, str) and value.startswith(f"{NS}:")))
        if extra_conditions:
            existing = conditions.get("neoforge:conditions", [])
            conditions = {"neoforge:conditions": existing + extra_conditions}
        if conditions:
            content = {**conditions, **content}
        self.write(self.DATA / NS / self.profile.recipe_folder / f"{name}.json", content)

    def shaped(self, name, result, count, pattern, key, category="misc", conditions=None, components=None):
        content = {"type": "minecraft:crafting_shaped"}
        self.profile.recipe_category(content, category)
        content["key"] = {symbol: self.profile.recipe_ingredient(ingredient) for symbol, ingredient in key.items()}
        content["pattern"] = pattern
        content["result"] = self.profile.recipe_result(result, count, components)
        self.recipe(name, content, [result, *key.values()], conditions)

    def shapeless(self, name, result, count, ingredients, category="misc", conditions=None):
        content = {"type": "minecraft:crafting_shapeless"}
        self.profile.recipe_category(content, category)
        content["ingredients"] = [self.profile.recipe_ingredient(ingredient) for ingredient in ingredients]
        content["result"] = self.profile.recipe_result(result, count, None)
        self.recipe(name, content, [result, *ingredients], conditions)

    def smelting(self, name, ingredient, result, count, experience):
        content = {
            "type": "minecraft:smelting",
            "cookingtime": 200,
            "experience": experience,
            "ingredient": self.profile.recipe_ingredient(ingredient),
            "result": self.profile.smelting_result(result, count),
        }
        self.recipe(name, content, [ingredient, result])
