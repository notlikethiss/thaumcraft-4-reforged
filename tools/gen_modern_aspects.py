import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import aspect_scanner

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "tools" / "modern_aspects"
ASPECT_JAVA = ROOT / "src" / "main" / "java" / "thaumcraft" / "aspect" / "Aspect.java"
OUTPUT = ROOT / "src" / "main" / "java" / "thaumcraft" / "aspect" / "ModernAspects.java"
TARGET = re.compile(r"^#?[a-z0-9_.-]+:[a-z0-9_./-]+$")


def aspect_names():
    return set(aspect_scanner.aspect_names(ASPECT_JAVA.read_text(encoding="utf-8")))


def load_entries():
    items = []
    entities = []
    for path in sorted(SOURCE.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        items.extend((path.name, entry) for entry in data.get("items", []))
        entities.extend((path.name, entry) for entry in data.get("entities", []))
    return items, entities


def aspects_expression(aspects, valid, where):
    parts = ["tags()"]
    for name, amount in aspects.items():
        if name not in valid:
            raise SystemExit(f"{where}: unknown aspect {name}")
        if not isinstance(amount, int) or amount <= 0:
            raise SystemExit(f"{where}: bad amount {amount} for {name}")
        parts.append(f".add(Aspect.{name}, {amount})")
    return "".join(parts)


def check_target(target, where):
    if not TARGET.match(target):
        raise SystemExit(f"{where}: bad target {target}")


def item_lines(items, valid):
    plain = []
    copies = []
    seen = set()
    for file_name, entry in items:
        target = entry["target"]
        where = f"{file_name}: {target}"
        check_target(target, where)
        if target in seen:
            raise SystemExit(f"{where}: duplicate target")
        seen.add(target)
        aspects = entry.get("aspects", {})
        if "copy" in entry:
            check_target(entry["copy"], where)
            expression = f'copy("{entry["copy"]}")'
            if aspects:
                expression += f".add({aspects_expression(aspects, valid, where)})"
            copies.append(f'        fill("{target}", {expression});')
        else:
            if not aspects:
                raise SystemExit(f"{where}: no aspects")
            plain.append(f'        fill("{target}", {aspects_expression(aspects, valid, where)});')
    return plain, copies


def entity_lines(entities, valid):
    lines = []
    seen = set()
    for file_name, entry in entities:
        target = entry["target"]
        where = f"{file_name}: {target}"
        check_target(target, where)
        if target in seen:
            raise SystemExit(f"{where}: duplicate target")
        seen.add(target)
        lines.append(f'        entity("{target}", {aspects_expression(entry["aspects"], valid, where)});')
    return lines


def main():
    valid = aspect_names()
    items, entities = load_entries()
    plain, copies = item_lines(items, valid)
    body_plain = "\n".join(plain)
    body_copies = "\n".join(copies)
    body_entities = "\n".join(entity_lines(entities, valid))
    java = f"""package thaumcraft.aspect;

public final class ModernAspects extends AspectRegistrar {{
    public ModernAspects(AspectRegistry registry) {{
        super(registry);
    }}

    public void registerBase() {{
{body_plain}
    }}

    public void registerDerived() {{
        registerCopies();
        registerEntityAspects();
    }}

    private void registerCopies() {{
{body_copies}
    }}

    private void registerEntityAspects() {{
{body_entities}
    }}
}}
"""
    OUTPUT.write_text(java, encoding="utf-8")
    print(f"{len(items)} items, {len(entities)} entities -> {OUTPUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
