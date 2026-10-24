import json
import re
import sys
from pathlib import Path

import aspect_scanner
import convert_tags

ROOT = Path(__file__).resolve().parent.parent
CONFIG = ROOT / "reference" / "decompiled" / "thaumcraft" / "common" / "config"
SOURCE = CONFIG / "ConfigResearch.java"
OUTPUT = ROOT / "src" / "main" / "java" / "thaumcraft" / "research" / "ConfigResearch.java"
ASPECT_SOURCE = ROOT / "src" / "main" / "java" / "thaumcraft" / "aspect" / "Aspect.java"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "thaumcraft"
LANG = ASSETS / "lang" / "en_us.json"
WILDCARD = 32767
CHUNK = 12

CATEGORY_METHODS = [
    ("initBasicResearch", "initBasics"),
    ("initThaumaturgyResearch", "initThaumaturgy"),
    ("initAlchemyResearch", "initAlchemy"),
    ("initArtificeResearch", "initArtifice"),
    ("initGolemancyResearch", "initGolemancy"),
    ("initEldritchResearch", "initEldritch"),
]
EXPECTED_CATEGORIES = {"BASICS": 19, "THAUMATURGY": 42, "ALCHEMY": 35, "ARTIFICE": 50, "GOLEMANCY": 39, "ELDRITCH": 16}
EXPECTED_FLAGS = {
    "setConcealed": 109, "setSecondary": 87, "setRound": 32, "setHidden": 27, "setStub": 26,
    "setAutoUnlock": 22, "setAspectTriggers": 17, "setParentsHidden": 16, "setItemTriggers": 11,
    "setSpecial": 9, "setEntityTriggers": 4, "setLost": 2, "setSiblings": 2,
}
PRIMAL = ["AIR", "FIRE", "WATER", "EARTH", "ORDER", "ENTROPY"]
CAST_KINDS = {
    "IArcaneRecipe": "arcane",
    "CrucibleRecipe": "crucible",
    "InfusionRecipe": "infusion",
    "InfusionEnchantmentRecipe": "infusionEnchantment",
    "IRecipe": "crafting",
    "List": "compound",
}
ARRAY_KINDS = {
    "IArcaneRecipe": "arcane",
    "CrucibleRecipe": "crucible",
    "InfusionRecipe": "infusion",
    "IRecipe": "crafting",
}
WAND_PARTS = {
    "WAND_CAP_IRON": ("ConfigItems.itemWandCap", 0, 1),
    "WAND_CAP_GOLD": ("ConfigItems.itemWandCap", 1, 3),
    "WAND_CAP_THAUMIUM": ("ConfigItems.itemWandCap", 2, 6),
    "WAND_ROD_WOOD": ("Items.stick", 0, 1),
    "WAND_ROD_GREATWOOD": ("ConfigItems.itemWandRod", 0, 3),
    "WAND_ROD_SILVERWOOD": ("ConfigItems.itemWandRod", 2, 9),
}
FLAG_METHODS = {
    "setSpecial", "setSecondary", "setRound", "setStub", "setConcealed", "setHidden", "setLost", "setAutoUnlock", "setVirtual",
}
REPORT_UNMAPPED = {}
REPORT_LANG = []
REPORT_NOTES = []

TOKEN = re.compile(
    r"""\s*(?:(?P<comment>//[^\n]*|/\*.*?\*/)|(?P<num>\d+(?:\.\d+)?[FLfl]?)|(?P<id>[A-Za-z_]\w*)|(?P<str>"(?:\\.|[^"\\])*")|(?P<chr>'(?:\\.|[^'\\])')|(?P<op>.))""",
    re.S,
)


def tokenize(text):
    tokens = []
    position = 0
    while position < len(text):
        match = TOKEN.match(text, position)
        if not match:
            break
        position = match.end()
        kind = match.lastgroup
        if kind == "comment" or match.group(kind) is None:
            continue
        tokens.append((kind, match.group(kind)))
    return tokens


class Parser:
    def __init__(self, tokens):
        self.tokens = tokens
        self.position = 0

    def peek(self, offset=0):
        index = self.position + offset
        return self.tokens[index] if index < len(self.tokens) else ("eof", "")

    def take(self):
        token = self.peek()
        self.position += 1
        return token

    def expect(self, text):
        token = self.take()
        if token[1] != text:
            raise SyntaxError(f"expected {text} got {token} at {self.position}")

    def parse_expression(self):
        left = self.parse_term()
        while self.peek()[0] == "op" and self.peek()[1] in "+-":
            operator = self.take()[1]
            left = ("bin", operator, left, self.parse_term())
        return left

    def parse_term(self):
        left = self.parse_unary()
        while self.peek()[0] == "op" and self.peek()[1] in "*/":
            operator = self.take()[1]
            left = ("bin", operator, left, self.parse_unary())
        return left

    def parse_unary(self):
        if self.peek() == ("op", "-"):
            self.take()
            return ("neg", self.parse_unary())
        return self.parse_postfix(self.parse_primary())

    def is_cast(self):
        if self.peek() != ("op", "(") or self.peek(1)[0] != "id" or self.peek(2) != ("op", ")"):
            return False
        name = self.peek(1)[1]
        if not (name[0].isupper() or name in ("int", "byte", "float", "double", "short", "long")):
            return False
        following = self.peek(3)
        return following[0] in ("id", "num", "str") or following == ("op", "(")

    def parse_primary(self):
        kind, text = self.peek()
        if kind == "num":
            self.take()
            return ("num", float(text.rstrip("FfLl")) if re.search(r"[.FfLl]", text) else int(text))
        if kind == "str":
            self.take()
            return ("str", json.loads(text))
        if kind == "chr":
            self.take()
            return ("chr", text[1:-1])
        if text == "(":
            if self.is_cast():
                self.take()
                type_name = self.take()[1]
                self.expect(")")
                return ("cast", type_name, self.parse_unary())
            self.take()
            inner = self.parse_expression()
            self.expect(")")
            return inner
        if text == "new":
            self.take()
            type_name = self.take()[1]
            if self.peek() == ("op", "<"):
                while self.take()[1] != ">":
                    pass
            if self.peek() == ("op", "["):
                self.take()
                size = self.parse_expression()
                self.expect("]")
                return ("newarray", type_name, size)
            return ("new", type_name, self.parse_arguments())
        if kind == "id":
            self.take()
            if self.peek() == ("op", "("):
                return ("call", None, text, self.parse_arguments())
            return ("name", text)
        raise SyntaxError(f"unexpected {kind} {text} at {self.position}")

    def parse_arguments(self):
        self.expect("(")
        arguments = []
        if self.peek() == ("op", ")"):
            self.take()
            return arguments
        while True:
            arguments.append(self.parse_expression())
            token = self.take()
            if token[1] == ")":
                return arguments
            if token[1] != ",":
                raise SyntaxError(f"bad argument list at {self.position}: {token}")

    def parse_postfix(self, node):
        while self.peek() == ("op", "."):
            self.take()
            name = self.take()[1]
            if self.peek() == ("op", "("):
                node = ("call", node, name, self.parse_arguments())
            else:
                node = ("member", node, name)
        return node


class Char(str):
    pass


class ItemRef:
    def __init__(self, reference, count, meta):
        self.reference = reference
        self.count = count
        self.meta = meta


class RecipeRef:
    def __init__(self, key, kind):
        self.key = key
        self.kind = kind


class WandPart:
    def __init__(self, name):
        self.name = name
        self.reference, self.meta, self.cost = WAND_PARTS[name]


class Aspects:
    def __init__(self):
        self.entries = []

    def add(self, name, amount):
        self.entries.append((name, amount))
        return self

    def java(self):
        return "new AspectList()" + "".join(f".add(Aspect.{name}, {amount})" for name, amount in self.entries)


class Research:
    def __init__(self, key, category, tags, column, row, complexity, icon):
        self.key = key
        self.category = category
        self.tags = tags
        self.column = column
        self.row = row
        self.complexity = complexity
        self.icon = icon
        self.chain = []
        self.recipes = []
        self.warp = 0


class Interpreter:
    def __init__(self, aspect_names, aspect_count):
        self.aspect_names = aspect_names
        self.aspect_count = aspect_count
        self.env = {}
        self.researches = []
        self.warps = {}
        self.pending = []
        self.category = None
        self.categories = []
        self.inline_counter = {}

    def evaluate(self, node):
        kind = node[0]
        if kind == "chr":
            return Char(node[1])
        if kind in ("num", "str"):
            return node[1]
        if kind == "neg":
            return -self.evaluate(node[1])
        if kind == "bin":
            left, right = self.evaluate(node[2]), self.evaluate(node[3])
            if node[1] == "+":
                if isinstance(left, str) or isinstance(right, str):
                    return str(left) + str(right)
                return left + right
            if node[1] == "-":
                return left - right
            if node[1] == "*":
                return left * right
            return left / right
        if kind == "name":
            if node[1] in self.env:
                return self.env[node[1]]
            return ("ref", node[1])
        if kind == "member":
            return self.evaluate_member(node)
        if kind == "cast":
            value = self.evaluate(node[2])
            if isinstance(value, RecipeRef):
                if node[1] not in CAST_KINDS:
                    raise ValueError(f"unknown cast {node[1]}")
                return RecipeRef(value.key, CAST_KINDS[node[1]])
            if node[1] == "int":
                return int(value)
            return value
        if kind == "new":
            return self.evaluate_new(node)
        if kind == "call":
            return self.evaluate_call(node)
        raise ValueError(f"cannot evaluate {node}")

    def evaluate_member(self, node):
        base = self.evaluate(node[1])
        name = node[2]
        if isinstance(base, tuple) and base[0] == "ref":
            if base[1] == "Aspect":
                if name == "aspects":
                    return ("aspectmap",)
                if name not in self.aspect_names:
                    raise ValueError(f"unknown aspect {name}")
                return ("aspect", name)
            if base[1] in ("ConfigItems", "ConfigBlocks", "Items", "Blocks"):
                if base[1] == "ConfigItems" and name in WAND_PARTS:
                    return WandPart(name)
                return ("field", f"{base[1]}.{name}")
        raise ValueError(f"cannot evaluate member {node}")

    def evaluate_new(self, node):
        type_name = node[1]
        arguments = [self.evaluate(argument) for argument in node[2]]
        if type_name == "ItemStack":
            reference = arguments[0]
            if isinstance(reference, WandPart):
                reference = ("field", reference.reference)
            count = arguments[1] if len(arguments) > 1 else 1
            meta = arguments[2] if len(arguments) > 2 else 0
            return ItemRef(reference[1], count, meta)
        if type_name == "AspectList":
            if arguments:
                raise ValueError("AspectList copy constructor")
            return Aspects()
        if type_name == "ResourceLocation":
            return ("rl", f"{arguments[0]}:{arguments[1]}")
        if type_name == "ResearchItem":
            if len(arguments) == 2:
                return Research(arguments[0], arguments[1], Aspects(), 0, 0, 0, None)
            return Research(*arguments)
        if type_name == "ResearchPage":
            return self.build_page(arguments)
        if type_name == "ShapedArcaneRecipe":
            return self.build_arcane(arguments)
        if type_name == "InfusionRunicAugmentRecipe":
            return self.build_runic(arguments[0])
        raise ValueError(f"unknown constructor {type_name}")

    def evaluate_call(self, node):
        target_node, name, argument_nodes = node[1], node[2], node[3]
        if target_node is None:
            raise ValueError(f"bare call {name}")
        if target_node == ("name", "recipes") and name == "get":
            return RecipeRef(self.evaluate(argument_nodes[0]), None)
        if target_node == ("name", "ThaumcraftApi"):
            arguments = [self.evaluate(argument) for argument in argument_nodes]
            if name == "addWarpToResearch":
                self.warps[arguments[0]] = arguments[1]
            elif name != "addWarpToItem":
                raise ValueError(f"unknown ThaumcraftApi.{name}")
            return None
        if target_node == ("name", "ResearchCategories") and name == "registerCategory":
            arguments = [self.evaluate(argument) for argument in argument_nodes]
            self.categories.append((arguments[0], arguments[1][1], arguments[2][1]))
            return None
        target = self.evaluate(target_node)
        arguments = [] if name == "toArray" else [self.evaluate(argument) for argument in argument_nodes]
        if isinstance(target, list):
            if name == "add":
                target.append(arguments[0])
                return None
            if name == "toArray":
                element = argument_nodes[0][1]
                return ("array", element, list(target))
        if isinstance(target, Aspects) and name == "add":
            return target.add(arguments[0][1], arguments[1])
        if isinstance(target, WandPart):
            if name == "getItem":
                return ItemRef(target.reference, 1, target.meta)
            if name == "getCraftCost":
                return target.cost
        if isinstance(target, Research):
            if name == "registerResearchItem":
                self.register(target)
            else:
                target.chain.append((name, arguments))
            return target
        if isinstance(target, tuple) and target[0] == "aspectmap" and name == "values":
            return ("aspectvalues",)
        if isinstance(target, tuple) and target[0] == "aspectvalues" and name == "size":
            return self.aspect_count
        raise ValueError(f"unknown call {name} on {target}")

    def register(self, research):
        research.recipes = self.pending
        self.pending = []
        self.researches.append(research)

    def build_page(self, arguments):
        if len(arguments) == 1:
            argument = arguments[0]
            if isinstance(argument, str):
                return ("text", argument)
            if isinstance(argument, RecipeRef):
                return (argument.kind, [argument.key])
            if isinstance(argument, tuple) and argument[0] == "array":
                return (ARRAY_KINDS[argument[1]], [recipe_key(item) for item in argument[2]])
            if isinstance(argument, ItemRef):
                return ("smelting", argument)
            if isinstance(argument, Aspects):
                return ("aspects", argument)
        if len(arguments) == 2:
            if isinstance(arguments[0], str):
                return ("concealed", arguments[0], arguments[1])
            if isinstance(arguments[0], tuple) and arguments[0][0] == "rl":
                return ("image", arguments[0][1], arguments[1])
        raise ValueError(f"unknown page {arguments}")

    def next_inline(self, base):
        self.inline_counter[base] = self.inline_counter.get(base, 0) + 1
        return f"ConfigResearch.{base}{self.inline_counter[base]}"

    def build_arcane(self, arguments):
        research, result, aspects = arguments[0], arguments[1], arguments[2]
        rows = []
        index = 3
        while isinstance(arguments[index], str) and not isinstance(arguments[index], Char):
            rows.append(arguments[index])
            index += 1
        legend = {}
        while index < len(arguments):
            legend[arguments[index]] = arguments[index + 1]
            index += 2
        key = self.next_inline("Sceptre")
        width = max(len(row) for row in rows)
        grid = []
        for row in rows:
            for column in range(width):
                symbol = row[column] if column < len(row) else " "
                grid.append(None if symbol == " " else legend[symbol])
        recipe = InlineRecipe(key, "arcane", research, result, aspects, width, len(rows), grid)
        self.pending.append(recipe)
        return RecipeRef(key, "arcane")

    def build_runic(self, central):
        charge = self.env.get("a", 0)
        key = f"ConfigResearch.RunicAugment{charge}"
        recipe = InlineRecipe(key, "runic", "RUNICAUGMENTATION", central, None, 0, 0, charge)
        self.pending.append(recipe)
        return RecipeRef(key, "infusion")


class InlineRecipe:
    def __init__(self, key, kind, research, result, aspects, width, height, grid):
        self.key = key
        self.kind = kind
        self.research = research
        self.result = result
        self.aspects = aspects
        self.width = width
        self.height = height
        self.grid = grid


def recipe_key(item):
    if isinstance(item, RecipeRef):
        return item.key
    raise ValueError(f"not a recipe {item}")


class Executor:
    def __init__(self, tokens, interpreter):
        self.tokens = tokens
        self.interpreter = interpreter

    def matching(self, start, open_text, close_text):
        depth = 0
        for index in range(start, len(self.tokens)):
            text = self.tokens[index][1]
            if self.tokens[index][0] == "str":
                continue
            if text == open_text:
                depth += 1
            elif text == close_text:
                depth -= 1
                if depth == 0:
                    return index
        raise SyntaxError(f"unbalanced {open_text} at {start}")

    def run(self, start, end):
        index = start
        while index < end:
            kind, text = self.tokens[index]
            if text in ("{", "}"):
                index += 1
            elif kind == "id" and text == "if":
                index = self.matching(index + 1, "(", ")") + 1
            elif kind == "id" and text == "for":
                header_end = self.matching(index + 1, "(", ")")
                body_start = header_end + 1
                if self.tokens[body_start][1] == "{":
                    body_end = self.matching(body_start, "{", "}")
                    body = (body_start + 1, body_end)
                    next_index = body_end + 1
                else:
                    statement_end = self.find_semicolon(body_start)
                    body = (body_start, statement_end + 1)
                    next_index = statement_end + 1
                self.run_for(index + 2, header_end, body)
                index = next_index
            else:
                statement_end = self.find_semicolon(index)
                self.statement(index, statement_end)
                index = statement_end + 1

    def find_semicolon(self, start):
        depth = 0
        for index in range(start, len(self.tokens)):
            kind, text = self.tokens[index]
            if kind == "str":
                continue
            if text in "({[":
                depth += 1
            elif text in ")}]":
                depth -= 1
            elif text == ";" and depth == 0:
                return index
        raise SyntaxError(f"no semicolon from {start}")

    def run_for(self, start, end, body):
        header = [text for _, text in self.tokens[start:end]]
        env = self.interpreter.env
        if ":" in header and header[0] == "Aspect":
            variable = header[1]
            for name in PRIMAL:
                env[variable] = ("aspect", name)
                self.run(*body)
            return
        variable = header[1]
        first = int(header[3])
        comparator = header[6]
        offset = 7
        if header[7] == "=":
            comparator = "<="
            offset = 8
        bound_tokens = self.tokens[start + offset:end]
        semicolon = [i for i, (_, text) in enumerate(bound_tokens) if text == ";"][0]
        bound = Parser(bound_tokens[:semicolon]).parse_expression()
        limit = self.interpreter.evaluate(bound)
        if comparator == "<=":
            limit += 1
        elif comparator != "<":
            raise ValueError(f"unknown comparator {comparator}")
        for value in range(first, limit):
            env[variable] = value
            self.run(*body)
        env.pop(variable, None)

    def statement(self, start, end):
        tokens = self.tokens[start:end]
        texts = [text for _, text in tokens]
        interpreter = self.interpreter
        if texts[0] == "(" and texts[1] == "(":
            return
        if tokens[0][0] == "id" and texts[0] in interpreter.env and len(texts) > 2 and texts[1] == "." and texts[2] in ("setTagInfo", "setTagCompound", "stackTagCompound"):
            return
        if texts[0] in ("initCategories", "initBasicResearch", "initThaumaturgyResearch", "initAlchemyResearch", "initArtificeResearch", "initGolemancyResearch", "initEldritchResearch"):
            return
        position = 0
        if texts[0] != "new" and tokens[0][0] == "id" and len(tokens) > 1 and (tokens[1][0] == "id" or texts[1] == "<"):
            position = 1
            if texts[1] == "<":
                position = texts.index(">") + 1
            name = texts[position]
            parser = Parser(tokens[position + 2:])
            interpreter.env[name] = self.declaration(texts[0], parser.parse_expression())
            return
        if len(texts) > 2 and tokens[0][0] == "id" and texts[1] == "=":
            parser = Parser(tokens[2:])
            interpreter.env[texts[0]] = interpreter.evaluate(parser.parse_expression())
            return
        parser = Parser(tokens)
        node = parser.parse_expression()
        interpreter.evaluate(node)

    def declaration(self, type_name, node):
        if node[0] == "new" and node[1] == "ArrayList":
            return []
        return self.interpreter.evaluate(node)


def read_aspects():
    text = ASPECT_SOURCE.read_text()
    return aspect_scanner.aspect_names(text)


def load_lang():
    return json.loads(LANG.read_text())


def texture_file(identifier):
    namespace, path = identifier.split(":", 1)
    path = re.sub(r"^textures/items/", "textures/item/", path)
    path = re.sub(r"^textures/blocks/", "textures/block/", path)
    file = ASSETS / path
    if namespace != "thaumcraft" or not file.exists():
        raise FileNotFoundError(f"texture {identifier} -> {file}")
    return f"{namespace}:{path}"


def replaced_by_vanilla(reference, meta):
    field = reference.split(".", 1)[1]
    if reference.startswith("ConfigItems."):
        old = convert_tags.ITEM_FIELDS.get(field)
        rows = convert_tags.ID_MAP["items"]
    else:
        old = convert_tags.BLOCK_FIELDS.get(field, field)
        rows = convert_tags.ID_MAP["blocks"]
    for row in rows:
        if row["old"] == old and row["meta"] == meta and row.get("skip"):
            match = re.search(r"replaced by vanilla (minecraft:\w+)", row["skip"])
            if match:
                return [convert_tags.profile_id(match.group(1))]
    return None


def resolve_item(reference, meta, context):
    if reference.startswith("Blocks.") or reference.startswith("Items."):
        ids = convert_tags.vanilla_ids(reference, meta)
        if ids:
            ids = [value for value in ids if value.split(":", 1)[1] in convert_tags.ITEM_IDS or value.split(":", 1)[0] != "minecraft"]
    else:
        ids = convert_tags.thaumcraft_ids(reference, meta)
        if not ids:
            ids = replaced_by_vanilla(reference, meta)
    if not ids:
        REPORT_UNMAPPED.setdefault(f"{reference}:{meta}", []).append(context)
        return None
    return ids


def first_id(item, context):
    ids = resolve_item(item.reference, item.meta, context)
    if ids is None:
        field = item.reference.split(".", 1)[1]
        return f"thaumcraft:unmapped_{field}_{item.meta}"
    return ids[0]


def stack_java(item, context, count=None):
    amount = item.count if count is None else count
    identifier = first_id(item, context)
    if amount > 1:
        return f'TcStack.of("{identifier}", {amount})'
    return f'TcStack.of("{identifier}")'


def string_literal(value):
    return json.dumps(value, ensure_ascii=False)


def smelting_result(item, context):
    text = (CONFIG / "ConfigRecipes.java").read_text()
    start = text.index("private static void initializeSmelting()")
    body = text[start:text.index("static IRecipe oreDictRecipe")]
    pattern = re.compile(r"addSmeltingRecipe\(\s*new ItemStack\(([\w.]+),\s*1,\s*(\d+)\),\s*new ItemStack\(([\w.]+)(?:,\s*(\d+))?(?:,\s*(\d+))?\)")
    for match in pattern.finditer(body):
        if match.group(1) == item.reference and int(match.group(2)) == item.meta:
            count = int(match.group(4) or 1)
            meta = int(match.group(5) or 0)
            return ItemRef(match.group(3), count, meta)
    raise ValueError(f"no smelting result for {item.reference}:{item.meta} in {context}")


def page_java(page, context, lang_keys):
    kind = page[0]
    if kind == "text":
        lang_keys.append((context, page[1]))
        return f"ResearchPage.text({string_literal(page[1])})"
    if kind == "concealed":
        lang_keys.append((context, page[2]))
        return f"ResearchPage.concealed({string_literal(page[1])}, {string_literal(page[2])})"
    if kind == "image":
        return f'ResearchPage.image({string_literal(page[1])}, {string_literal(page[2])})'
    if kind == "aspects":
        return f"ResearchPage.aspects({page[1].java()})"
    if kind == "smelting":
        output = smelting_result(page[1], context)
        return f"ResearchPage.smelting({stack_java(page[1], context)}, {stack_java(output, context)})"
    if kind in ("infusionEnchantment", "compound"):
        return f"ResearchPage.{kind}({string_literal(page[1][0])})"
    keys = ", ".join(string_literal(key) for key in page[1])
    return f"ResearchPage.{kind}({keys})"


def ingredient_java(item, context):
    return f'TcIngredient.item("{first_id(item, context)}")'


def inline_java(recipe, context):
    if recipe.kind == "arcane":
        grid = ", ".join("TcIngredient.EMPTY" if cell is None else ingredient_java(cell, context) for cell in recipe.grid)
        return (
            f"TcRecipes.register(new ShapedArcaneRecipe({string_literal(recipe.key)}, {string_literal(recipe.research)}, "
            f"{stack_java(recipe.result, context)}, {recipe.aspects.java()}, {recipe.width}, {recipe.height}, List.of({grid})));"
        )
    charge = recipe.grid
    vis = 32 * 2 ** charge
    aspects = f"new AspectList().add(Aspect.ARMOR, {vis // 2}).add(Aspect.MAGIC, {vis // 2}).add(Aspect.ENERGY, {vis})"
    components = [ItemRef("Items.diamond", 1, 0), ItemRef("ConfigItems.itemResource", 1, 14)] + [ItemRef("ConfigItems.itemResource", 1, 14)] * charge
    items = ", ".join(ingredient_java(component, context) for component in components)
    return (
        f"TcRecipes.register(new InfusionRecipe({string_literal(recipe.key)}, {string_literal(recipe.research)}, "
        f"{stack_java(recipe.result, context)}, {5 + charge // 2}, {aspects}, {ingredient_java(recipe.result, context)}, List.of({items})));"
    )


def research_java(research, lang_keys):
    context = research.key
    if research.icon is None:
        head = f"new ResearchItem({string_literal(research.key)}, {string_literal(research.category)})"
    else:
        if isinstance(research.icon, ItemRef):
            icon = stack_java(research.icon, context, 1)
        else:
            icon = f'Identifier.parse("{texture_file(research.icon[1])}")'
        head = (
            f"new ResearchItem({string_literal(research.key)}, {string_literal(research.category)}, {research.tags.java()}, "
            f"{research.column}, {research.row}, {research.complexity}, {icon})"
        )
    lines = [head]
    required = {
        "CAP_copper": ["c:ingots/copper"], "CAP_silver": ["c:ingots/silver"],
        **{f"PURE{metal.upper()}": [f"c:ores/{metal}", f"c:ingots/{metal}"] for metal in ("copper", "tin", "silver", "lead")},
        **{f"TRANS{metal.upper()}": [f"c:ingots/{metal}"] for metal in ("copper", "tin", "silver", "lead")},
    }.get(research.key, [])
    if required:
        lines.append(f"            .setRequiredTags({', '.join(string_literal(tag) for tag in required)})")
    for name, arguments in research.chain:
        if name == "setPages":
            pages = [page_java(page, context, lang_keys) for page in arguments]
            body = ",\n".join("                " + page for page in pages)
            lines.append(f"            .setPages(\n{body}\n            )")
        elif name in FLAG_METHODS:
            lines.append(f"            .{name}()")
        elif name == "setComplexity":
            lines.append(f"            .setComplexity({arguments[0]})")
        elif name in ("setParents", "setParentsHidden", "setSiblings"):
            lines.append(f"            .{name}({', '.join(string_literal(value) for value in arguments)})")
        elif name == "setItemTriggers":
            ids = []
            for item in arguments:
                for value in resolve_item(item.reference, item.meta, context) or []:
                    if value not in ids:
                        ids.append(value)
            lines.append(f"            .setItemTriggers({', '.join(string_literal(value) for value in ids)})")
        elif name == "setEntityTriggers":
            ids = []
            for entity in arguments:
                if entity.startswith("Thaumcraft."):
                    identifier = convert_tags.ENTITY_IDS["thaumcraft"].get(entity)
                else:
                    identifier = convert_tags.ENTITY_IDS["vanilla"].get(entity)
                if identifier is None:
                    raise ValueError(f"unmapped entity {entity} in {context}")
                ids.append(identifier)
            lines.append(f"            .setEntityTriggers({', '.join(string_literal(value) for value in ids)})")
        elif name == "setAspectTriggers":
            lines.append(f"            .setAspectTriggers({', '.join('Aspect.' + value[1] for value in arguments)})")
        else:
            raise ValueError(f"unknown research method {name} in {context}")
    if research.warp:
        lines.append(f"            .setWarp({research.warp})")
    lines.append("            .registerResearchItem();")
    pre = [inline_java(recipe, context) for recipe in research.recipes]
    return pre, "\n".join(lines)


def method_range(tokens, name):
    for index in range(len(tokens) - 4):
        if tokens[index][1] == "void" and tokens[index + 1][1] == name and tokens[index + 2][1] == "(":
            start = index + 4
            depth = 0
            for position in range(start, len(tokens)):
                if tokens[position][1] == "{" and tokens[position][0] == "op":
                    depth += 1
                elif tokens[position][1] == "}" and tokens[position][0] == "op":
                    depth -= 1
                    if depth == 0:
                        return start, position
    raise KeyError(name)


def main():
    aspect_names = read_aspects()
    interpreter = Interpreter(set(aspect_names), len(aspect_names))
    tokens = tokenize(SOURCE.read_text())
    executor = Executor(tokens, interpreter)
    first_item = ItemRef("ConfigItems.itemWandCasting", 1, 0)
    interpreter.env["wand"] = first_item
    start, end = method_range(tokens, "initCategories")
    executor.run(start, end)
    category_of_entry = []
    for method, _ in CATEGORY_METHODS:
        start, end = method_range(tokens, method)
        executor.run(start, end)
    researches = interpreter.researches
    for key, warp in interpreter.warps.items():
        matching = [research for research in researches if research.key == key]
        if len(matching) != 1:
            raise ValueError(f"warp target {key}")
        matching[0].warp = warp

    lang = load_lang()
    lang_keys = []
    by_category = {}
    for research in researches:
        pre, text = research_java(research, lang_keys)
        by_category.setdefault(research.category, []).append((pre, text))

    category_names = [name for name, _, _ in interpreter.categories]
    if category_names != list(EXPECTED_CATEGORIES):
        raise ValueError(f"categories {category_names}")
    category_lines = []
    for name, icon, background in interpreter.categories:
        category_lines.append(
            f'        ResearchCategories.registerCategory({string_literal(name)}, Identifier.parse("{texture_file(icon)}"), '
            f'Identifier.parse("{texture_file(background)}"));'
        )

    methods = []
    init_calls = []
    method_names = {"BASICS": "initBasics", "THAUMATURGY": "initThaumaturgy", "ALCHEMY": "initAlchemy", "ARTIFICE": "initArtifice", "GOLEMANCY": "initGolemancy", "ELDRITCH": "initEldritch"}
    for category in EXPECTED_CATEGORIES:
        entries = by_category[category]
        name = method_names[category]
        chunks = [entries[i:i + CHUNK] for i in range(0, len(entries), CHUNK)]
        init_calls.append(f"        {name}();")
        if len(chunks) == 1:
            calls = None
        else:
            calls = "\n".join(f"        {name}{number + 1}();" for number in range(len(chunks)))
            methods.append(f"    private static void {name}() {{\n{calls}\n    }}")
        for number, chunk in enumerate(chunks):
            method_name = name if len(chunks) == 1 else f"{name}{number + 1}"
            body = []
            for pre, text in chunk:
                for statement in pre:
                    body.append("        " + statement)
                body.append("        " + text)
            methods.append(f"    private static void {method_name}() {{\n" + "\n".join(body) + "\n    }")

    java = (
        "package thaumcraft.research;\n\n"
        "import java.util.List;\n"
        "import net.minecraft.resources.Identifier;\n"
        "import thaumcraft.aspect.Aspect;\n"
        "import thaumcraft.aspect.AspectList;\n"
        "import thaumcraft.crafting.InfusionRecipe;\n"
        "import thaumcraft.crafting.ShapedArcaneRecipe;\n"
        "import thaumcraft.crafting.TcIngredient;\n"
        "import thaumcraft.crafting.TcRecipes;\n"
        "import thaumcraft.crafting.TcStack;\n\n"
        "public final class ConfigResearch {\n"
        "    private ConfigResearch() {\n    }\n\n"
        "    public static void init() {\n"
        "        initCategories();\n" + "\n".join(init_calls) + "\n    }\n\n"
        "    private static void initCategories() {\n" + "\n".join(category_lines) + "\n    }\n\n"
        + "\n\n".join(methods) + "\n}\n"
    )
    OUTPUT.write_text(java)

    counts = {}
    for research in researches:
        counts[research.category] = counts.get(research.category, 0) + 1
    flags = {}
    page_total = 0
    inline_keys = []
    for research in researches:
        for name, arguments in research.chain:
            if name == "setPages":
                page_total += len(arguments)
            else:
                flags[name] = flags.get(name, 0) + 1
        inline_keys.extend(recipe.key for recipe in research.recipes)
        if research.warp:
            flags["setWarp"] = flags.get("setWarp", 0) + 1

    print(f"research: {len(researches)}")
    for category in EXPECTED_CATEGORIES:
        print(f"  {category}: {counts.get(category, 0)}")
    print(f"pages: {page_total}")
    for name, expected in EXPECTED_FLAGS.items():
        print(f"  {name}: {flags.get(name, 0)}")
    print(f"  setWarp: {flags.get('setWarp', 0)}")
    print(f"inline recipes: {inline_keys}")

    missing = sorted({(context, key) for context, key in lang_keys if key not in lang})
    print(f"missing lang keys: {len(missing)}")
    for context, key in missing:
        print(f"  {context}: {key}")
    print(f"unmapped ids: {len(REPORT_UNMAPPED)}")
    for reference, contexts in sorted(REPORT_UNMAPPED.items()):
        print(f"  {reference}: {sorted(set(contexts))}")

    keys = [research.key for research in researches]
    assert len(keys) == len(set(keys)), "duplicate research keys"
    assert len(researches) == 201, len(researches)
    assert counts == EXPECTED_CATEGORIES, counts
    for name, expected in EXPECTED_FLAGS.items():
        assert flags.get(name, 0) == expected, (name, flags.get(name, 0), expected)
    assert flags.get("setWarp", 0) == 20, flags.get("setWarp", 0)


if __name__ == "__main__":
    sys.exit(main())
