import argparse
import gzip
import hashlib
import io
import json
import re
import struct
import subprocess
import sys
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import mcformat

ROOT = Path(__file__).resolve().parent.parent
CACHE = ROOT / "tools" / "cache"
MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
JAVA_HOMES = {"26.3": "openjdk@25", "26.1.2": "openjdk@25", "1.21.1": "openjdk@21", "1.19.2": "openjdk@17"}
ASPECTS = ROOT / "src" / "main" / "java" / "thaumcraft" / "aspect" / "ConfigAspects.java"
ID_PATTERN = re.compile(r'"(#?minecraft:[a-z0-9_./]+)"')
LOOT_TYPES = ["loot_condition_type", "loot_function_type", "loot_pool_entry_type", "loot_number_provider_type", "enchantment_level_based_value_type"]
BONUS_FORMULAS = {"minecraft:uniform_bonus_count", "minecraft:binomial_with_bonus_count", "minecraft:ore_drops"}
BUILTIN_MODELS = {"builtin/entity", "builtin/generated"}
ITEM_MODEL_TYPES = {
    "minecraft:empty", "minecraft:model", "minecraft:range_dispatch", "minecraft:special",
    "minecraft:composite", "minecraft:bundle/selected_item", "minecraft:select",
    "minecraft:condition",
}
ITEM_MODEL_PROPERTIES = {
    "minecraft:custom_model_data", "minecraft:bundle/fullness", "minecraft:damage", "minecraft:cooldown",
    "minecraft:time", "minecraft:compass", "minecraft:crossbow/pull", "minecraft:use_cycle",
    "minecraft:use_duration", "minecraft:count", "minecraft:main_hand", "minecraft:charge_type",
    "minecraft:trim_material", "minecraft:block_state", "minecraft:display_context", "minecraft:local_time",
    "minecraft:context_entity_type", "minecraft:context_dimension", "minecraft:component", "minecraft:using_item",
    "minecraft:broken", "minecraft:damaged", "minecraft:fishing_rod/cast", "minecraft:has_component",
    "minecraft:bundle/has_selected_item", "minecraft:selected", "minecraft:carried", "minecraft:extended_view",
    "minecraft:keybind_down", "minecraft:view_entity",
}
LOOT_NUMBER_PROVIDER_TYPES = {"minecraft:constant", "minecraft:uniform", "minecraft:binomial", "minecraft:score", "minecraft:storage"}
ITEM_MODEL_TINT_TYPES = {"minecraft:constant", "minecraft:dye"}


def curl(url):
    return subprocess.run(["curl", "-fsSL", url], check=True, capture_output=True).stdout


def download(url, target, sha1):
    data = curl(url)
    if hashlib.sha1(data).hexdigest() != sha1:
        raise RuntimeError(f"sha1 mismatch for {url}")
    target.write_bytes(data)


def fetch(version):
    directory = CACHE / version
    server = directory / "server.jar"
    client = directory / "client.jar"
    if server.exists() and client.exists():
        return directory
    directory.mkdir(parents=True, exist_ok=True)
    manifest = json.loads(curl(MANIFEST_URL))
    entry = next(entry for entry in manifest["versions"] if entry["id"] == version)
    package = json.loads(curl(entry["url"]))
    for kind, target in [("server", server), ("client", client)]:
        download(package["downloads"][kind]["url"], target, package["downloads"][kind]["sha1"])
    return directory


def run_reports(directory, version, reports_directory=None):
    reports = reports_directory / "reports" if reports_directory and not (reports_directory / "registries.json").exists() else reports_directory or directory / "reports"
    registries = reports / ("registries.json" if reports_directory else "reports/registries.json")
    if not registries.exists():
        reports.mkdir(exist_ok=True)
        command = [f"/opt/homebrew/opt/{JAVA_HOMES[version]}/bin/java", "-DbundlerMainClass=net.minecraft.data.Main", "-jar", "server.jar", "--reports", "--output", "reports"]
        result = subprocess.run(command, cwd=directory, capture_output=True, text=True)
        if result.returncode != 0 or not registries.exists():
            print(result.stdout[-2000:])
            print(result.stderr[-2000:])
            raise SystemExit("vanilla data generator failed")
    return json.loads(registries.read_text(encoding="utf-8"))


def jar_names(path):
    with zipfile.ZipFile(path) as archive:
        names = set(archive.namelist())
        for name in list(names):
            if name.startswith("META-INF/versions/") and name.endswith(".jar"):
                with zipfile.ZipFile(io.BytesIO(archive.read(name))) as nested:
                    names |= set(nested.namelist())
    return names


def datapack_ids(names, folders):
    result = set()
    for folder in folders:
        prefix = f"data/minecraft/{folder}/"
        result |= {"minecraft:" + name[len(prefix):-5] for name in names if name.startswith(prefix) and name.endswith(".json")}
    return result


def read_nbt(data):
    stream = io.BytesIO(gzip.decompress(data))

    def read(fmt):
        size = struct.calcsize(fmt)
        return struct.unpack(fmt, stream.read(size))[0]

    def read_string():
        return stream.read(read(">H")).decode("utf-8")

    def payload(kind):
        if kind == 1:
            return read(">b")
        if kind == 2:
            return read(">h")
        if kind == 3:
            return read(">i")
        if kind == 4:
            return read(">q")
        if kind == 5:
            return read(">f")
        if kind == 6:
            return read(">d")
        if kind == 7:
            return [read(">b") for _ in range(read(">i"))]
        if kind == 8:
            return read_string()
        if kind == 9:
            item_kind = read(">b")
            return [payload(item_kind) for _ in range(read(">i"))]
        if kind == 10:
            result = {}
            while True:
                child = read(">b")
                if child == 0:
                    return result
                name = read_string()
                result[name] = payload(child)
        if kind == 11:
            return [read(">i") for _ in range(read(">i"))]
        if kind == 12:
            return [read(">q") for _ in range(read(">i"))]
        raise ValueError(kind)

    stream.read(1)
    read_string()
    return payload(10)


class Validator:
    def __init__(self, version, out, reports_directory=None, server_jar=None, client_jar=None):
        self.version = version
        self.out = out
        self.problems = []
        directory = None if reports_directory and server_jar and client_jar else fetch(version)
        registries = run_reports(directory, version, Path(reports_directory) if reports_directory else None)
        self.registry = {
            name.split(":", 1)[1]: set(entries if isinstance(entries, list) else entries.keys())
            for name, content in registries.items()
            for entries in [content["entries"]]
        }
        server = jar_names(Path(server_jar) if server_jar else directory / "server.jar")
        client = jar_names(Path(client_jar) if client_jar else directory / "client.jar")
        self.models = {name[len("assets/minecraft/models/"):-5] for name in client if name.startswith("assets/minecraft/models/") and name.endswith(".json")}
        self.textures = {name[len("assets/minecraft/textures/"):-4] for name in client if name.startswith("assets/minecraft/textures/") and name.endswith(".png")}
        self.fonts = {"minecraft:" + name[len("assets/minecraft/font/"):-5] for name in client if name.startswith("assets/minecraft/font/") and name.endswith(".json")}
        self.fonts |= self.asset_ids(out, "font", ".json")
        self.models = {"minecraft:" + model for model in self.models}
        self.textures = {"minecraft:" + texture for texture in self.textures}
        self.models |= self.asset_ids(out, "models", ".json")
        self.textures |= self.asset_ids(out, "textures", ".png")
        self.shaders = set()
        for archive_names in (server, client):
            self.shaders |= {
                name[len("assets/"):-len(suffix)].replace("/shaders/", ":", 1)
                for name in archive_names
                for suffix in (".vsh", ".fsh", ".json")
                if name.startswith("assets/") and "/shaders/" in name and name.endswith(suffix)
            }
        self.shaders |= self.asset_ids(out, "shaders", ".vsh")
        self.shaders |= self.asset_ids(out, "shaders", ".fsh")
        self.shaders |= self.asset_ids(out, "shaders", ".json")
        profile = mcformat.PROFILES[version]
        self.loot_tables = datapack_ids(server, [profile.loot_folder])
        self.enchantments = datapack_ids(server, ["enchantment"]) | self.registry.get("enchantment", set())
        self.tags = {}
        for kind, folder in [("block", profile.block_tag_folder), ("item", profile.item_tag_folder), ("biome", "tags/worldgen/biome")]:
            self.tags[kind] = {"#" + tag for tag in datapack_ids(server, [folder])}
        for kind, folder in [("block", profile.block_tag_folder), ("item", profile.item_tag_folder)]:
            minecraft = out / "data" / "minecraft" / folder
            if minecraft.exists():
                for path in minecraft.rglob("*.json"):
                    tag = "#minecraft:" + path.relative_to(minecraft).with_suffix("").as_posix()
                    self.tags[kind].add(tag)
        self.loot_registry = set()
        for name in LOOT_TYPES:
            self.loot_registry |= self.registry.get(name, set())
        self.loot_condition_literals = set()
        self.loot_table_types = set()
        with zipfile.ZipFile(Path(server_jar) if server_jar else directory / "server.jar") as archive:
            for name in archive.namelist():
                if name.startswith("META-INF/versions/") and name.endswith(".jar"):
                    with zipfile.ZipFile(io.BytesIO(archive.read(name))) as nested:
                        self.collect_loot_condition_literals(nested, self.loot_condition_literals)
                        self.collect_loot_table_types(nested, self.loot_table_types)
            self.collect_loot_condition_literals(archive, self.loot_condition_literals)
            self.collect_loot_table_types(archive, self.loot_table_types)
        self.flat = set()
        for entries in self.registry.values():
            self.flat |= entries
        reports_root = Path(reports_directory) if reports_directory else directory / "reports"
        if not (reports_root / "datapack.json").exists():
            reports_root = reports_root / "reports"
        datapack_report = reports_root / "datapack.json"
        if datapack_report.exists():
            for registry, metadata in json.loads(datapack_report.read_text(encoding="utf-8")).get("registries", {}).items():
                if metadata.get("elements"):
                    folder = registry.split(":", 1)[-1]
                    prefix = f"data/minecraft/{folder}/"
                    self.flat |= {
                        "minecraft:" + name[len(prefix):-5]
                        for name in server
                        if name.startswith(prefix) and name.endswith(".json")
                    }
        self.flat |= self.loot_tables | self.enchantments
        self.block_states = {}
        if reports_directory:
            reports_root = Path(reports_directory)
            blocks_file = reports_root / "blocks.json"
            if not blocks_file.exists():
                blocks_file = reports_root / "reports/blocks.json"
            if blocks_file.exists():
                self.block_states = json.loads(blocks_file.read_text(encoding="utf-8"))

    def report(self, source, value, expected):
        self.problems.append((source, value, expected))

    @staticmethod
    def asset_ids(directory, folder, suffix):
        result = set()
        for path in directory.glob(f"assets/*/{folder}/**/*{suffix}"):
            parts = path.relative_to(directory).parts
            result.add(parts[1] + ":" + Path(*parts[3:]).with_suffix("").as_posix())
        return result

    @staticmethod
    def collect_loot_condition_literals(archive, result):
        for name in archive.namelist():
            if not (name.startswith("data/minecraft/loot_table/") and name.endswith(".json")):
                continue
            try:
                root = json.loads(archive.read(name))
            except (KeyError, json.JSONDecodeError):
                continue

            def visit(value):
                if isinstance(value, dict):
                    condition = value.get("condition")
                    if isinstance(condition, str):
                        result.add(condition)
                    for child in value.values():
                        visit(child)
                elif isinstance(value, list):
                    for child in value:
                        visit(child)

            visit(root)

    @staticmethod
    def collect_loot_table_types(archive, result):
        for name in archive.namelist():
            if not (name.startswith("data/minecraft/loot_table/") and name.endswith(".json")):
                continue
            try:
                root = json.loads(archive.read(name))
            except (KeyError, json.JSONDecodeError):
                continue
            loot_type = root.get("type")
            if isinstance(loot_type, str):
                result.add(loot_type)

    def check(self, source, value, kind):
        if kind == "item":
            known = value in self.registry["item"]
        elif kind == "block":
            known = value in self.registry["block"]
        elif kind == "entity":
            known = value in self.registry["entity_type"]
        elif kind == "block_entity":
            known = value in self.registry["block_entity_type"]
        elif kind == "loot_table":
            known = value in self.loot_tables
        elif kind == "enchantment":
            known = value in self.enchantments
        elif kind == "model":
            known = value in self.models or value.split(":", 1)[1] in BUILTIN_MODELS
        elif kind == "shader":
            known = value in self.shaders
        elif kind == "font":
            known = value in self.fonts
        elif kind == "font_texture":
            known = value.removesuffix(".png") in self.textures
        elif kind == "texture":
            known = value in self.textures
        elif kind == "skip":
            known = True
        elif kind == "loot_registry":
            known = value in self.loot_registry or value in self.loot_condition_literals
        elif kind == "loot_number_provider":
            known = value in LOOT_NUMBER_PROVIDER_TYPES
        elif kind == "item_model_type":
            known = value in ITEM_MODEL_TYPES
        elif kind == "item_model_property":
            known = value in ITEM_MODEL_PROPERTIES
        elif kind == "item_model_tint":
            known = value in ITEM_MODEL_TINT_TYPES
        elif kind == "recipe_serializer":
            known = value in self.registry.get("recipe_serializer", set())
        elif kind == "loot_table_type":
            known = value in self.loot_table_types
        elif kind.startswith("tag:"):
            known = value in self.tags[kind[4:]]
        else:
            known = value in self.flat
        if not known:
            self.report(source, value, kind)

    def walk(self, source, value, keys, file_kind):
        if isinstance(value, str):
            if keys and keys[-1] == "tag" and file_kind in ("recipe", "other"):
                value = "#" + value
            if value.startswith("minecraft:") or value.startswith("#minecraft:"):
                self.check(source, value, self.classify(value, keys, file_kind))
            return
        if isinstance(value, list):
            for entry in value:
                self.walk(source, entry, keys, file_kind)
        elif isinstance(value, dict):
            for key, entry in value.items():
                if key == "tag" and value.get("type") == "minecraft:matching_block_tag" and isinstance(entry, str):
                    tag = entry if entry.startswith("#") else "#" + entry
                    self.check(source, tag, "tag:block")
                    continue
                if key == "name" and value.get("type") == "minecraft:loot_table":
                    key = "value"
                self.walk(source, entry, keys + [key], file_kind)

    def classify(self, value, keys, file_kind):
        key = keys[-1] if keys else ""
        if file_kind == "post_effect":
            if key in ("vertex_shader", "fragment_shader"):
                return "shader"
            if key in ("target", "output") and value == "minecraft:main":
                return "skip"
        if value.startswith("#"):
            if key == "infiniburn":
                return "tag:block"
            if "matching_block_tag" in keys:
                return "tag:block"
            if file_kind == "block_tag" or key in ("blocks", "block"):
                return "tag:block"
            if "biomes" in keys:
                return "tag:biome"
            return "tag:item"
        if file_kind == "recipe":
            return ("recipe_serializer" if value.startswith("minecraft:") else "skip") if key == "type" else "item"
        if file_kind == "item_tag":
            return "item"
        if file_kind == "block_tag":
            return "block"
        if file_kind == "font":
            return "font" if key == "id" else "font_texture" if key == "file" else "skip"
        if file_kind == "model":
            if "textures" in keys:
                return "texture"
            if key in ("parent", "model"):
                return "model"
            return "flat"
        if file_kind == "item_model":
            if key == "type":
                if "tints" in keys:
                    return "item_model_tint"
                if value in ITEM_MODEL_TINT_TYPES:
                    return "item_model_tint"
                return "item_model_type"
            if key == "property":
                return "item_model_property"
            if key in ("model", "base") and ":" in value:
                return "model"
        if file_kind == "loot":
            if key == "type" and len(keys) == 1:
                return "loot_table_type"
            if key == "name":
                return "item"
            if key == "value":
                return "loot_table"
            if key in ("condition", "function"):
                return "loot_registry"
            if key == "enchantment":
                return "enchantment"
            if key in ("blocks", "block"):
                return "block"
            if key == "formula":
                return "skip" if value in BONUS_FORMULAS else "flat"
            if key == "type" and len(keys) > 1:
                if value in LOOT_NUMBER_PROVIDER_TYPES:
                    return "loot_number_provider"
                return "loot_registry"
            if key == "type":
                return "skip"
            return "flat"
        if key in ("enchantment", "exclusive_set") and file_kind != "recipe":
            return "enchantment"
        if file_kind == "blockstate" and key == "model":
            return "model"
        return "flat"

    def check_json(self, path):
        relative = path.relative_to(self.out).as_posix()
        profile = mcformat.PROFILES[self.version]
        if "/" + profile.recipe_folder + "/" in "/" + relative:
            file_kind = "recipe"
        elif "/" + profile.block_tag_folder + "/" in "/" + relative:
            file_kind = "block_tag"
        elif "/" + profile.item_tag_folder + "/" in "/" + relative:
            file_kind = "item_tag"
        elif "/" + profile.loot_folder + "/" in "/" + relative:
            file_kind = "loot"
        elif "/blockstates/" in "/" + relative:
            file_kind = "blockstate"
        elif relative.startswith("assets/") and "/models/" in relative:
            file_kind = "model"
        elif relative.startswith("assets/") and "/items/" in relative:
            file_kind = "item_model"
        elif relative.startswith("assets/") and "/font/" in relative:
            file_kind = "font"
        elif relative.startswith("assets/") and "/post_effect/" in relative:
            file_kind = "post_effect"
        else:
            file_kind = "other"
        content = json.loads(path.read_text(encoding="utf-8"))
        self.walk(relative, content, [], file_kind)
        if file_kind == "blockstate":
            self.check_blockstate(relative, content)

    def check_blockstate(self, relative, content):
        asset_path = relative.split("/blockstates/", 1)[1]
        namespace = relative.split("/", 2)[1]
        block_id = f"{namespace}:{Path(asset_path).with_suffix('').as_posix()}"
        report = self.block_states.get(block_id)
        if report is None:
            return
        properties = report.get("properties", {})
        for selector in content.get("variants", {}):
            self.check_state_selector(relative, block_id, selector, properties)
        for part in content.get("multipart", []):
            when = part.get("when", {})
            self.check_state_condition(relative, block_id, when, properties)

    def check_state_selector(self, source, block_id, selector, properties):
        if not selector:
            return
        values = {}
        for property_value in selector.split(","):
            name, separator, value = property_value.partition("=")
            if not separator:
                self.report(source, selector, f"valid {block_id} block-state selector")
                return
            values[name] = value.split("|")
        self.check_state_properties(source, block_id, values, properties)

    def check_state_properties(self, source, block_id, values, properties):
        for name, expected_values in values.items():
            if name not in properties:
                self.report(source, f"{block_id}[{name}]", "block-state property")
                continue
            candidates = expected_values.split("|") if isinstance(expected_values, str) else expected_values
            for value in candidates:
                if value not in properties[name]:
                    self.report(source, f"{block_id}[{name}={value}]", "block-state property value")

    def check_state_condition(self, source, block_id, condition, properties):
        if isinstance(condition, list):
            for part in condition:
                self.check_state_condition(source, block_id, part, properties)
            return
        if not isinstance(condition, dict):
            return
        for operator in ("AND", "OR"):
            if operator in condition:
                self.check_state_condition(source, block_id, condition[operator], properties)
                return
        self.check_state_properties(source, block_id, condition, properties)

    def check_structure(self, path):
        relative = path.relative_to(self.out).as_posix()
        root = read_nbt(path.read_bytes())
        for entry in root["palette"]:
            name = entry[mcformat.PROFILES[self.version].block_state_keys[0]]
            if name.startswith("minecraft:"):
                self.check(relative, name, "block")
        for entry in root["blocks"]:
            nbt = entry.get("nbt")
            if nbt and nbt["id"].startswith("minecraft:"):
                self.check(relative, nbt["id"], "block_entity")
        for entry in root["entities"]:
            nbt = entry["nbt"]
            self.check(relative, nbt["id"], "entity")
            villager_type = nbt.get("VillagerData", {}).get("type")
            if villager_type and villager_type not in self.registry["villager_type"]:
                self.report(relative, villager_type, "villager_type")
        if root["DataVersion"] != mcformat.PROFILES[self.version].data_version:
            self.report(relative, str(root["DataVersion"]), "DataVersion")

    def check_aspects(self, path):
        text = path.read_text(encoding="utf-8")
        for match in ID_PATTERN.finditer(text):
            value = match.group(1)
            line = text[:match.start()].rsplit("\n", 1)[-1]
            if re.search(r"\bentity\s*\(\s*$", line):
                kind = "entity"
            else:
                kind = "tag:item" if value.startswith("#") else "item"
            self.check(path.name, value, kind)

    def run(self, aspects):
        for path in sorted(self.out.rglob("*.json")):
            self.check_json(path)
        for path in sorted(self.out.rglob("*.nbt")):
            self.check_structure(path)
        if aspects:
            self.check_aspects(aspects)
        return self.problems


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--aspects", help="ConfigAspects.java to check (default: the committed one)")
    parser.add_argument("--no-aspects", action="store_true")
    parser.add_argument("--fetch-only", action="store_true")
    parser.add_argument("--reports-dir", help="Minecraft data-generator reports directory")
    parser.add_argument("--server-jar", help="Vanilla server jar including its bundled server")
    parser.add_argument("--client-jar", help="Vanilla client jar for model and texture IDs")
    profile = mcformat.init(parser=parser)
    args, _ = parser.parse_known_args()
    if profile.modern and not (args.reports_dir and args.server_jar and args.client_jar):
        raise SystemExit("validation needs a vanilla server jar, use --mc 1.21.1 or --mc 1.19.2")
    if args.fetch_only:
        print(fetch(profile.name))
        return
    aspects = None if args.no_aspects else Path(args.aspects) if args.aspects else ASPECTS
    problems = Validator(profile.name, mcformat.out_root, args.reports_dir, args.server_jar, args.client_jar).run(aspects)
    for source, value, expected in problems:
        print(f"{source}: {value} (expected {expected})")
    print(f"{profile.name}: {len(problems)} problem(s)")
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
