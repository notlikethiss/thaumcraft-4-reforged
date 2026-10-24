import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import aspect_scanner
import convert_tags as ct

ROOT = Path(__file__).resolve().parent.parent
CONFIG = ROOT / "reference" / "decompiled" / "thaumcraft" / "common" / "config"
SOURCE = CONFIG / "ConfigRecipes.java"
RESEARCH_SOURCE = CONFIG / "ConfigResearch.java"
ASPECT_SOURCE = ROOT / "src" / "main" / "java" / "thaumcraft" / "aspect" / "Aspect.java"
OUTPUT = ROOT / "src" / "main" / "java" / "thaumcraft" / "crafting" / "ConfigRecipes.java"
VANILLA_OUTPUT = ROOT / "tools" / "tc_recipes.json"
WILDCARD = 32767
CHUNK = 12
EXPECTED = {"arcane": 109, "crucible": 54, "infusion": 63, "infusion_enchantment": 24, "compound": 9}

ORE_DICT = dict(ct.ORE_DICT)
ORE_DICT.update({
    "ingotIron": "#c:ingots/iron",
    "ingotThaumium": "#c:ingots/thaumium",
    "ingotVoid": "#c:ingots/void_metal",
    "nuggetThaumium": "#c:nuggets/thaumium",
    "nuggetVoid": "#c:nuggets/void_metal",
    "oreCinnabar": "#c:ores/cinnabar",
    "gemEmerald": "#c:gems/emerald",
})
WILDCARD_TAGS = {
    "ConfigItems.itemShard": "#thaumcraft:shards",
    "ConfigBlocks.blockCandle": "#thaumcraft:tallow_candles",
}
ENCHANTMENTS = {
    "protection": "protection",
    "fireProtection": "fire_protection",
    "featherFalling": "feather_falling",
    "blastProtection": "blast_protection",
    "projectileProtection": "projectile_protection",
    "respiration": "respiration",
    "aquaAffinity": "aqua_affinity",
    "thorns": "thorns",
    "sharpness": "sharpness",
    "smite": "smite",
    "baneOfArthropods": "bane_of_arthropods",
    "knockback": "knockback",
    "fireAspect": "fire_aspect",
    "looting": "looting",
    "efficiency": "efficiency",
    "silkTouch": "silk_touch",
    "unbreaking": "unbreaking",
    "fortune": "fortune",
    "power": "power",
    "punch": "punch",
    "flame": "flame",
    "infinity": "infinity",
}
CUSTOM_ENCHANTMENTS = {"enchantHaste": "thaumcraft:haste", "enchantRepair": "thaumcraft:repair"}
ALIASES = {
    "WandBasic": "wand_iron_wood",
    "WandCapIron": "iron_wand_cap",
    "PlankGreatwood": "greatwood_planks",
    "PlankSilverwood": "silverwood_planks",
    "BlockThaumium": "thaumium_block",
    "BlockTallow": "tallow_block",
    "Phial": "glass_phial",
    "TallowCandle": "white_tallow_candle",
    "ArcaneStone2": "arcane_stone_bricks",
    "ArcaneStone3": "arcane_stone_stairs",
    "ArcaneStone4": "arcane_stone_slab",
}
REPORT = []
DYNAMIC = []


def aspect_names():
    text = ASPECT_SOURCE.read_text()
    return aspect_scanner.aspect_names(text)


ASPECTS = aspect_names()


def craft_costs():
    text = (CONFIG / "ConfigItems.java").read_text()
    caps = {}
    rods = {}
    for tag, cost in re.findall(r'new WandCap\(\s*"(\w+)",[^;]*?(\d+)\s*\);', text):
        caps[tag] = int(cost)
    for kind, tag, cost in re.findall(r'new (WandRod|StaffRod)\("(\w+)",\s*\d+,\s*new ItemStack\([^)]*\),\s*(\d+)', text):
        rods[tag if kind == "WandRod" else tag + "_staff"] = int(cost)
    return caps, rods


CAP_COSTS, ROD_COSTS = craft_costs()


TOKEN = re.compile(
    r'\s*(?:(?P<string>"(?:[^"\\]|\\.)*")|(?P<char>\'(?:[^\'\\]|\\.)\')|(?P<number>\d+(?:\.\d+)?[FfLl]?)'
    r'|(?P<ident>[A-Za-z_$][\w$]*)|(?P<punct>\+\+|--|==|!=|<=|>=|&&|\|\||\+=|-=|[{}()\[\];,.<>=+\-*/?:!]))'
)


def tokenize(text):
    tokens = []
    position = 0
    while True:
        match = TOKEN.match(text, position)
        if not match:
            if text[position:].strip():
                raise SystemExit(f"cannot tokenize at {text[position:position + 40]!r}")
            break
        position = match.end()
        kind = match.lastgroup
        value = match.group(kind)
        if kind == "string":
            tokens.append(("string", bytes(value[1:-1], "utf-8").decode("unicode_escape")))
        elif kind == "char":
            tokens.append(("char", value[1:-1]))
        elif kind == "number":
            tokens.append(("number", float(value.rstrip("FfLl")) if "." in value or value[-1] in "Ff" else int(value.rstrip("Ll"))))
        else:
            tokens.append((kind, value))
    return tokens


class Parser:
    def __init__(self, tokens):
        self.tokens = tokens
        self.position = 0

    def peek(self, offset=0):
        index = self.position + offset
        return self.tokens[index] if index < len(self.tokens) else ("eof", "")

    def next(self):
        token = self.peek()
        self.position += 1
        return token

    def accept(self, value):
        if self.peek()[1] == value and self.peek()[0] in ("punct", "ident"):
            self.position += 1
            return True
        return False

    def expect(self, value):
        if not self.accept(value):
            raise SystemExit(f"expected {value!r} at token {self.position}: {self.tokens[self.position:self.position + 8]}")

    def block(self):
        self.expect("{")
        statements = []
        while not self.accept("}"):
            statements.append(self.statement())
        return statements

    def body(self):
        if self.peek()[1] == "{":
            return self.block()
        return [self.statement()]

    def is_declaration(self):
        offset = 0
        if self.peek(offset)[0] != "ident":
            return False
        offset += 1
        while self.peek(offset)[1] == "." and self.peek(offset + 1)[0] == "ident":
            offset += 2
        if self.peek(offset)[1] == "<":
            depth = 0
            while True:
                value = self.peek(offset)[1]
                if value == "<":
                    depth += 1
                elif value == ">":
                    depth -= 1
                offset += 1
                if depth == 0:
                    break
        while self.peek(offset)[1] == "[" and self.peek(offset + 1)[1] == "]":
            offset += 2
        return self.peek(offset)[0] == "ident" and self.peek(offset + 1)[1] in ("=", ";", ":")

    def statement(self):
        token = self.peek()
        if token[1] == "for" and token[0] == "ident":
            self.next()
            self.expect("(")
            if self.is_declaration() and self.peek(self.declaration_length())[1] == ":":
                self.type_name()
                name = self.next()[1]
                self.expect(":")
                iterable = self.expression()
                self.expect(")")
                return ("foreach", name, iterable, self.body())
            init = self.statement()
            condition = self.expression()
            self.expect(";")
            update = self.expression()
            self.expect(")")
            return ("for", init, condition, update, self.body())
        if token[1] == "if" and token[0] == "ident":
            self.next()
            self.expect("(")
            condition = self.expression()
            self.expect(")")
            then = self.body()
            other = []
            if self.accept("else"):
                other = self.body()
            return ("if", condition, then, other)
        if token[1] == "{" and token[0] == "punct":
            return ("block", self.block())
        if self.is_declaration():
            self.type_name()
            name = self.next()[1]
            value = None
            if self.accept("="):
                value = self.expression()
            self.expect(";")
            return ("local", name, value)
        expression = self.expression()
        self.expect(";")
        return ("expr", expression)

    def declaration_length(self):
        offset = 1
        while self.peek(offset)[1] == "." and self.peek(offset + 1)[0] == "ident":
            offset += 2
        while self.peek(offset)[1] == "[":
            offset += 2
        return offset + 1

    def type_name(self):
        self.next()
        while self.accept("."):
            self.next()
        if self.peek()[1] == "<":
            depth = 0
            while True:
                value = self.next()[1]
                if value == "<":
                    depth += 1
                elif value == ">":
                    depth -= 1
                if depth == 0:
                    break
        while self.accept("["):
            self.expect("]")

    def expression(self):
        left = self.ternary()
        if self.peek()[1] in ("=", "+=", "-=") and self.peek()[0] == "punct":
            operator = self.next()[1]
            return ("assign", operator, left, self.expression())
        return left

    def ternary(self):
        condition = self.binary(0)
        if self.accept("?"):
            first = self.expression()
            self.expect(":")
            return ("ternary", condition, first, self.expression())
        return condition

    LEVELS = [["||"], ["&&"], ["==", "!="], ["<", ">", "<=", ">="], ["+", "-"], ["*", "/"]]

    def binary(self, level):
        if level == len(self.LEVELS):
            return self.unary()
        left = self.binary(level + 1)
        while self.peek()[0] == "punct" and self.peek()[1] in self.LEVELS[level]:
            operator = self.next()[1]
            left = ("bin", operator, left, self.binary(level + 1))
        return left

    def unary(self):
        token = self.peek()
        if token[0] == "punct" and token[1] in ("!", "-"):
            self.next()
            return ("unary", token[1], self.unary())
        if token[1] == "(" and token[0] == "punct" and self.peek(1)[0] == "ident" and self.peek(2)[1] == ")":
            follower = self.peek(3)
            if follower[0] in ("ident", "number", "string") or follower[1] == "(":
                if follower[1] not in ("instanceof",):
                    self.next()
                    type_name = self.next()[1]
                    self.next()
                    return ("cast", type_name, self.unary())
        return self.postfix()

    def postfix(self):
        node = self.primary()
        while True:
            if self.accept("."):
                name = self.next()[1]
                if self.peek()[1] == "(" and self.peek()[0] == "punct":
                    node = ("call", node, name, self.arguments())
                else:
                    node = ("field", node, name)
            elif self.peek()[1] == "[" and self.peek()[0] == "punct":
                self.next()
                index = self.expression()
                self.expect("]")
                node = ("index", node, index)
            elif self.peek()[1] in ("++", "--") and self.peek()[0] == "punct":
                node = ("postfix", self.next()[1], node)
            else:
                return node

    def arguments(self):
        self.expect("(")
        arguments = []
        if not self.accept(")"):
            while True:
                arguments.append(self.expression())
                if self.accept(")"):
                    break
                self.expect(",")
        return arguments

    def primary(self):
        kind, value = self.next()
        if kind in ("string", "number"):
            return ("literal", value)
        if kind == "char":
            return ("char", value)
        if kind == "punct" and value == "(":
            node = self.expression()
            self.expect(")")
            return node
        if kind == "ident":
            if value == "new":
                type_name = self.next()[1]
                while self.accept("."):
                    type_name = self.next()[1]
                if self.peek()[1] == "[":
                    self.next()
                    size = None
                    if self.peek()[1] != "]":
                        size = self.expression()
                    self.expect("]")
                    elements = None
                    if self.peek()[1] == "{":
                        elements = self.initializer()
                    return ("array", type_name, elements, size)
                return ("new", type_name, self.arguments())
            if value in ("true", "false"):
                return ("literal", value == "true")
            if value == "null":
                return ("literal", None)
            if self.peek()[1] == "(" and self.peek()[0] == "punct":
                return ("call", None, value, self.arguments())
            return ("name", value)
        raise SystemExit(f"unexpected token {kind} {value!r} at {self.position}")

    def initializer(self):
        self.expect("{")
        elements = []
        while not self.accept("}"):
            elements.append(self.expression())
            if not self.accept(","):
                self.expect("}")
                break
        return elements


class Char(str):
    pass


class Namespace:
    def __init__(self, name):
        self.name = name


class ItemRef:
    def __init__(self, reference):
        self.reference = reference

    @property
    def is_block(self):
        return self.reference.startswith("Blocks.") or self.reference.startswith("ConfigBlocks.")


class EnchantRef:
    def __init__(self, identifier):
        self.identifier = identifier


class Stack:
    def __init__(self, reference, count=1, meta=0):
        self.reference = reference
        self.count = count
        self.meta = meta
        self.nbt = {}
        self.aspects = None
        self.wand = None

    def copy(self):
        clone = Stack(self.reference, self.count, self.meta)
        clone.nbt = dict(self.nbt)
        clone.aspects = self.aspects
        clone.wand = self.wand
        return clone

    def m_getItem(self, args):
        return ItemHandle(self)

    def m_setTagCompound(self, args):
        return None

    @property
    def stackTagCompound(self):
        return NbtHandle(self)


class NbtHandle:
    def __init__(self, stack):
        self.stack = stack

    def m_setByte(self, args):
        self.stack.nbt[args[0]] = int(args[1])

    def m_setString(self, args):
        self.stack.nbt[args[0]] = args[1]


class ItemHandle:
    def __init__(self, stack):
        self.stack = stack

    def m_setAspects(self, args):
        args[0].aspects = args[1]

    def m_setCap(self, args):
        args[0].wand = dict(args[0].wand or {}, cap=CAP_NAMES[args[1]])

    def m_setRod(self, args):
        args[0].wand = dict(args[0].wand or {}, rod=ROD_NAMES[args[1]])


CAP_NAMES = {"WAND_CAP_IRON": "iron"}
ROD_NAMES = {"WAND_ROD_WOOD": "wood"}


class AspectConst:
    def __init__(self, name):
        self.name = name


class AspectList:
    def __init__(self):
        self.operations = []

    def m_add(self, args):
        self.operations.append(("add", args[0].name, int(args[1])))
        return self

    def m_merge(self, args):
        self.operations.append(("merge", args[0].name, int(args[1])))
        return self

    def java(self):
        return "new AspectList()" + "".join(f".{op}(Aspect.{name}, {amount})" for op, name, amount in self.operations)


class HostMap:
    def __init__(self, values):
        self.values = values

    def m_get(self, args):
        return HostCost(self.values[args[0]])

    def m_values(self, args):
        return list(self.values)


class HostCost:
    def __init__(self, cost):
        self.cost = cost

    def m_getCraftCost(self, args):
        return self.cost


class Opaque:
    pass


class RecipeRegistry:
    def __init__(self, interpreter):
        self.interpreter = interpreter

    def m_put(self, args):
        self.interpreter.put(args[0], args[1])


class Recipe:
    def __init__(self, kind, **fields):
        self.kind = kind
        self.key = None
        self.fields = fields

    def __getattr__(self, name):
        try:
            return self.fields[name]
        except KeyError:
            raise AttributeError(name)


class Interpreter:
    def __init__(self, methods):
        self.methods = methods
        self.recipes = []
        self.counter = 0
        self.basic_wand = Stack("ConfigItems.itemWandCasting")
        self.basic_wand.wand = {"cap": "iron", "rod": "wood"}
        self.scopes = [{"basicWand": self.basic_wand}]

    def lookup(self, name):
        for scope in reversed(self.scopes):
            if name in scope:
                return scope[name]
        return Namespace(name)

    def put(self, key, value):
        if isinstance(value, Recipe):
            value.key = key
        elif isinstance(value, list):
            self.recipes.append(self.compound(key, value))
        else:
            raise SystemExit(f"cannot register {key}: {value!r}")

    def compound(self, key, value):
        aspects, width, height, depth, blocks = value
        recipe = Recipe("compound", research="", aspects=aspects, width=width, height=height, depth=depth, blocks=blocks)
        recipe.key = key
        return recipe

    def run(self, statements):
        for statement in statements:
            self.execute(statement)

    def execute(self, statement):
        kind = statement[0]
        if kind == "expr":
            self.evaluate(statement[1])
        elif kind == "local":
            self.scopes[-1][statement[1]] = self.evaluate(statement[2]) if statement[2] is not None else None
        elif kind == "block":
            self.run(statement[1])
        elif kind == "if":
            self.run(statement[2] if self.evaluate(statement[1]) else statement[3])
        elif kind == "for":
            self.scopes.append({})
            self.execute(statement[1])
            while self.evaluate(statement[2]):
                self.scopes.append({})
                self.run(statement[4])
                self.scopes.pop()
                self.evaluate(statement[3])
            self.scopes.pop()
        elif kind == "foreach":
            for item in self.evaluate(statement[2]):
                self.scopes.append({statement[1]: item})
                self.run(statement[3])
                self.scopes.pop()
        else:
            raise SystemExit(f"unknown statement {kind}")

    def assign(self, name, value):
        for scope in reversed(self.scopes):
            if name in scope:
                scope[name] = value
                return
        raise SystemExit(f"assignment to unknown {name}")

    def evaluate(self, node):
        kind = node[0]
        if kind == "literal":
            return node[1]
        if kind == "char":
            return Char(node[1])
        if kind == "name":
            return self.lookup(node[1])
        if kind == "field":
            return self.field(self.evaluate(node[1]), node[2])
        if kind == "index":
            return self.index(self.evaluate(node[1]), self.evaluate(node[2]))
        if kind == "cast":
            return self.evaluate(node[2])
        if kind == "bin":
            return self.binary(node)
        if kind == "unary":
            value = self.evaluate(node[2])
            return -value if node[1] == "-" else not value
        if kind == "ternary":
            return self.evaluate(node[2] if self.evaluate(node[1]) else node[3])
        if kind == "postfix":
            value = self.evaluate(node[2])
            self.assign(node[2][1], value + (1 if node[1] == "++" else -1))
            return value
        if kind == "assign":
            value = self.evaluate(node[3])
            if node[2][0] != "name":
                raise SystemExit("complex assignment")
            if node[1] == "+=":
                value = self.evaluate(node[2]) + value
            self.assign(node[2][1], value)
            return value
        if kind == "array":
            if node[2] is not None:
                return [self.evaluate(element) for element in node[2]]
            return [None] * int(self.evaluate(node[3]))
        if kind == "new":
            return self.new(node[1], [self.evaluate(argument) for argument in node[2]])
        if kind == "call":
            return self.call(node)
        raise SystemExit(f"unknown expression {kind}")

    def binary(self, node):
        operator = node[1]
        if operator == "&&":
            return self.evaluate(node[2]) and self.evaluate(node[3])
        if operator == "||":
            return self.evaluate(node[2]) or self.evaluate(node[3])
        left = self.evaluate(node[2])
        right = self.evaluate(node[3])
        if operator == "+":
            return (str(left) + str(right)) if isinstance(left, str) or isinstance(right, str) else left + right
        return {
            "-": lambda: left - right,
            "*": lambda: left * right,
            "/": lambda: left // right,
            "<": lambda: left < right,
            ">": lambda: left > right,
            "<=": lambda: left <= right,
            ">=": lambda: left >= right,
            "==": lambda: left == right,
            "!=": lambda: left != right,
        }[operator]()

    def field(self, base, name):
        if isinstance(base, Namespace):
            namespace = base.name
            if namespace in ("Items", "Blocks", "ConfigItems", "ConfigBlocks"):
                if namespace == "ConfigItems" and name in ("WAND_CAP_IRON", "WAND_ROD_WOOD"):
                    return name
                return ItemRef(f"{namespace}.{name}")
            if namespace == "Aspect":
                return [AspectConst(entry) for entry in ASPECTS] if name == "aspects" else AspectConst(name)
            if namespace == "Config":
                return True
            if namespace == "Enchantment":
                if name == "enchantmentsList":
                    return Namespace("Enchantment.enchantmentsList")
                return EnchantRef("minecraft:" + ENCHANTMENTS[name])
            if namespace == "ThaumcraftApi":
                return CUSTOM_ENCHANTMENTS[name]
            if namespace == "ConfigAspects" and name == "dyes":
                return [dye for dye, _ in ct.DYES]
            if namespace == "ConfigResearch" and name == "recipes":
                return RecipeRegistry(self)
            if namespace == "WandCap" and name == "caps":
                return HostMap(CAP_COSTS)
            if namespace == "WandRod" and name == "rods":
                return HostMap(ROD_COSTS)
            if namespace == "Category":
                return Opaque()
            return Namespace(f"{namespace}.{name}")
        if isinstance(base, Stack) and name == "stackTagCompound":
            return base.stackTagCompound
        if isinstance(base, list) and name == "values":
            return base
        raise SystemExit(f"unknown field {name} of {base!r}")

    def index(self, base, index):
        if isinstance(base, Namespace) and base.name == "Enchantment.enchantmentsList":
            return EnchantRef(index)
        return base[index]

    def new(self, type_name, arguments):
        if type_name == "ItemStack":
            item = arguments[0]
            count = arguments[1] if len(arguments) > 1 else 1
            meta = arguments[2] if len(arguments) > 2 else 0
            return Stack(item.reference, count, meta)
        if type_name == "AspectList":
            return AspectList()
        if type_name == "NBTTagCompound":
            return Opaque()
        if type_name in ("NBTTagByte", "NBTTagInt"):
            return int(arguments[0])
        if type_name in ("ArcaneWandRecipe", "ArcaneSceptreRecipe", "InfusionRunicAugmentRecipe", "RecipesRobeArmorDyes", "RecipesVoidRobeArmorDyes"):
            DYNAMIC.append(type_name)
            return Opaque()
        raise SystemExit(f"unknown constructor {type_name}")

    def call(self, node):
        _, target, name, argument_nodes = node
        arguments = [self.evaluate(argument) for argument in argument_nodes]
        if target is None:
            if name in self.methods:
                self.scopes.append({})
                self.run(self.methods[name])
                self.scopes.pop()
                return None
            handler = getattr(self, "s_" + name, None)
            if handler is None:
                raise SystemExit(f"unknown method {name}")
            return handler(arguments)
        path = self.path(target)
        if path is not None:
            handler = getattr(self, "c_" + path.replace(".", "_") + "_" + name, None)
            if handler is not None:
                return handler(arguments)
            if path.split(".")[0] in ("RecipeSorter", "WandTriggerRegistry", "FurnaceRecipes"):
                return None
            if path == "ThaumcraftApi" and name in ("addSmeltingBonus", "getCraftingRecipes"):
                return Opaque()
            if path == "Arrays" and name == "asList":
                return list(arguments)
            if path.startswith("GameRegistry") or path == "CraftingManager":
                return None
        value = self.evaluate(target)
        if isinstance(value, Opaque):
            return value
        if isinstance(value, list) and name == "values":
            return value
        if value is None and name in ("addSmeltingRecipe", "addSmelting"):
            return None
        method = getattr(value, "m_" + name, None)
        if method is None:
            if isinstance(value, Namespace) and value.name in ("CraftingManager", "CraftingManager.getInstance", "FurnaceRecipes.instance"):
                return None
            raise SystemExit(f"unknown call {name} on {value!r}")
        return method(arguments)

    def path(self, node):
        if node[0] == "name":
            if self.lookup(node[1]).__class__ is Namespace:
                return node[1]
            return None
        if node[0] == "field":
            base = self.path(node[1])
            return None if base is None else f"{base}.{node[2]}"
        return None

    def c_Arrays_asList(self, arguments):
        return list(arguments)

    def c_ThaumcraftApi_addArcaneCraftingRecipe(self, arguments):
        research, result, aspects, *params = arguments
        recipe = Recipe("arcane_shaped", research=research, result=result, aspects=aspects, params=params)
        self.recipes.append(recipe)
        return recipe

    def c_ThaumcraftApi_addShapelessArcaneCraftingRecipe(self, arguments):
        research, result, aspects, *params = arguments
        recipe = Recipe("arcane_shapeless", research=research, result=result, aspects=aspects, params=params)
        self.recipes.append(recipe)
        return recipe

    def c_ThaumcraftApi_addCrucibleRecipe(self, arguments):
        research, result, catalyst, aspects = arguments
        recipe = Recipe("crucible", research=research, result=result, catalyst=catalyst, aspects=aspects)
        self.recipes.append(recipe)
        return recipe

    def c_ThaumcraftApi_addInfusionCraftingRecipe(self, arguments):
        research, result, instability, aspects, central, components = arguments
        recipe = Recipe("infusion", research=research, result=result, instability=instability, aspects=aspects, central=central, components=components)
        self.recipes.append(recipe)
        return recipe

    def c_ThaumcraftApi_addInfusionEnchantmentRecipe(self, arguments):
        research, enchantment, instability, aspects, components = arguments
        recipe = Recipe("infusion_enchantment", research=research, enchantment=enchantment, instability=instability, aspects=aspects, components=components)
        self.recipes.append(recipe)
        return recipe

    def vanilla(self, kind, arguments, ore=False):
        result, params = arguments[0], arguments[1]
        if isinstance(params, list):
            pass
        recipe = Recipe(kind, research="", result=result, params=params)
        self.recipes.append(recipe)
        return recipe

    def s_oreDictRecipe(self, arguments):
        return self.vanilla("vanilla_shaped", arguments)

    def s_shapelessOreDictRecipe(self, arguments):
        return self.vanilla("vanilla_shapeless", arguments)

    def s_shapelessNBTOreRecipe(self, arguments):
        return self.vanilla("vanilla_shapeless_nbt", arguments)

    def c_GameRegistry_addShapedRecipe(self, arguments):
        return self.vanilla("vanilla_shaped", arguments)

    def c_GameRegistry_addRecipe(self, arguments):
        return self.vanilla("vanilla_shaped", arguments)

    def c_GameRegistry_addShapelessRecipe(self, arguments):
        return self.vanilla("vanilla_shapeless", arguments)

    def c_GameRegistry_addSmelting(self, arguments):
        return None

    def c_CraftingManager_getInstance(self, arguments):
        return CraftingManagerHandle(self)


class CraftingManagerHandle:
    def __init__(self, interpreter):
        self.interpreter = interpreter

    def m_addRecipe(self, args):
        return self.interpreter.vanilla("vanilla_shaped", [args[0], args[1:]])


def parse_methods(text):
    tokens = tokenize(text)
    parser = Parser(tokens)
    methods = {}
    for index in range(len(tokens) - 4):
        if tokens[index][1] == "static" and tokens[index + 1][1] in ("void", "IRecipe") and tokens[index + 3][1] == "(":
            name = tokens[index + 2][1]
            if name in ("oreDictRecipe", "shapelessOreDictRecipe", "shapelessNBTOreRecipe"):
                continue
            parser.position = index + 4
            if parser.peek()[1] != ")":
                continue
            parser.position += 1
            methods[name] = parser.block()
    return methods


def fix_vanilla_arguments(interpreter_class):
    original = interpreter_class.vanilla

    def vanilla(self, kind, arguments, ore=False):
        result = arguments[0]
        if isinstance(result, Opaque):
            return None
        if len(arguments) == 2 and isinstance(arguments[1], list):
            params = arguments[1]
        else:
            params = list(arguments[1:])
        recipe = Recipe(kind, research="", result=result, params=params)
        self.recipes.append(recipe)
        return recipe

    interpreter_class.vanilla = vanilla


fix_vanilla_arguments(Interpreter)


def resolve_ids(reference, meta, line=""):
    if reference == "ConfigBlocks.blockHole":
        return ["minecraft:air"]
    if reference == "ConfigItems.itemResearchNotes" and meta == 42:
        return ["thaumcraft:research_notes"]
    if reference == "ConfigItems.itemNugget" and meta == 0:
        return ["minecraft:iron_nugget"]
    slabs = {
        ("ConfigBlocks.blockSlabWood", 0): "thaumcraft:greatwood_slab",
        ("ConfigBlocks.blockSlabWood", 1): "thaumcraft:silverwood_slab",
        ("ConfigBlocks.blockSlabStone", 0): "thaumcraft:arcane_stone_slab",
        ("ConfigBlocks.blockSlabStone", 1): "thaumcraft:ancient_stone_slab",
    }
    if (reference, meta) in slabs:
        return [slabs[(reference, meta)]]
    if reference.startswith(("Blocks.", "Items.")):
        ids = ct.vanilla_ids(reference, meta)
        if ids is None:
            return []
        result = []
        for identifier in ids:
            namespace, name = identifier.split(":", 1)
            if namespace == "minecraft" and name not in ct.ITEM_IDS and reference not in ct.NO_ITEM_BLOCKS and reference != "Blocks.lava":
                if "wall_" in name:
                    continue
                REPORT.append(f"{reference} meta {meta}: {identifier} is not an item in {ct.PROFILE}")
                continue
            result.append(identifier)
        return result
    return ct.thaumcraft_ids(reference, meta) or []


class Resolver:
    def __init__(self):
        self.unmapped = set()

    def ids(self, reference, meta):
        ids = resolve_ids(reference, meta)
        if not ids:
            self.unmapped.add(f"{reference}:{meta}")
        return ids

    def stack_spec(self, stack):
        if stack.meta == WILDCARD and stack.reference in WILDCARD_TAGS:
            return ("tag", WILDCARD_TAGS[stack.reference][1:])
        ids = self.ids(stack.reference, stack.meta)
        if not ids:
            return None
        if len(ids) > 1:
            return ("any", [("item", identifier) for identifier in ids])
        extras = {}
        if stack.aspects is not None:
            extras["aspects"] = stack.aspects.java()
        if stack.wand:
            extras["data"] = dict(stack.wand)
        elif stack.nbt:
            extras["data"] = {key: color_name(value) if key == "color" else value for key, value in stack.nbt.items()}
        return ("item", ids[0], extras) if extras else ("item", ids[0])

    def param_spec(self, value):
        if isinstance(value, str):
            if value not in ORE_DICT:
                self.unmapped.add(f"oredict {value}")
                return None
            target = ORE_DICT[value]
            return ("tag", target[1:]) if target.startswith("#") else ("item", target)
        if isinstance(value, Stack):
            return self.stack_spec(value)
        if isinstance(value, ItemRef):
            meta = WILDCARD if value.is_block else 0
            return self.stack_spec(Stack(value.reference, 1, meta))
        if value is None:
            return ("empty",)
        raise SystemExit(f"unknown parameter {value!r}")

    def result_spec(self, stack):
        spec = self.stack_spec(stack)
        if spec is None or spec[0] == "any":
            if spec is not None:
                spec = spec[1][0]
            else:
                return None
        identifier = spec[1]
        extras = spec[2] if len(spec) > 2 else {}
        return {"id": identifier, "count": stack.count, **extras}


def color_name(value):
    return ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"][value]


def java_string(value):
    return json.dumps(value)


def java_stack(spec):
    if spec is None:
        return None
    identifier = spec["id"]
    count = spec["count"]
    if "aspects" in spec:
        return f"TcStack.withAspects({java_string(identifier)}, {count}, {spec['aspects']})"
    if "data" in spec:
        entries = ", ".join(f"{java_string(key)}, {java_string(str(value))}" for key, value in spec["data"].items())
        return f"TcStack.withData({java_string(identifier)}, {count}, Map.of({entries}))"
    if identifier == "minecraft:air":
        return "TcStack.NONE"
    if count == 1:
        return f"TcStack.of({java_string(identifier)})"
    return f"TcStack.of({java_string(identifier)}, {count})"


def java_ingredient(spec):
    kind = spec[0]
    if kind == "empty":
        return "TcIngredient.EMPTY"
    if kind == "tag":
        return f"TcIngredient.tag({java_string(spec[1])})"
    if kind == "any":
        return "TcIngredient.anyOf(" + ", ".join(java_ingredient(option) for option in spec[1]) + ")"
    if len(spec) > 2:
        extras = spec[2]
        stack = {"id": spec[1], "count": 1, **extras}
        return f"TcIngredient.of({java_stack(stack)})"
    return f"TcIngredient.item({java_string(spec[1])})"


def spec_to_json(spec):
    if spec[0] == "empty":
        return None
    if spec[0] == "tag":
        return "#" + spec[1]
    if spec[0] == "any":
        return [option[1] for option in spec[1]]
    return spec[1]


def parse_grid(params):
    rows = []
    index = 0
    if params and isinstance(params[0], bool):
        index = 1
    while index < len(params) and isinstance(params[index], str) and not isinstance(params[index], Char):
        rows.append(params[index])
        index += 1
    mapping = {}
    while index < len(params):
        mapping[str(params[index])] = params[index + 1]
        index += 2
    return rows, mapping


def research_references():
    text = RESEARCH_SOURCE.read_text()
    items = [(match.start(), match.group(1)) for match in re.finditer(r'new ResearchItem\(\s*"(\w+)"', text)]
    references = {}
    for match in re.finditer(r'recipes\.get\("(\w+)"', text):
        owner = ""
        for start, name in items:
            if start < match.start():
                owner = name
            else:
                break
        references.setdefault(match.group(1), owner)
    return references


def snake(name):
    return re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name).replace("__", "_").lower()


def main():
    text = SOURCE.read_text()
    methods = parse_methods(text)
    interpreter = Interpreter(methods)
    interpreter.run(methods["init"])
    references = research_references()
    resolver = Resolver()
    sections = {"normal": [], "arcane": [], "infusion": [], "enchantment": [], "crucible": [], "compound": []}
    counts = {"arcane": 0, "crucible": 0, "infusion": 0, "infusion_enchantment": 0, "compound": 0}
    vanilla = []
    used_keys = set()
    synthetic = {}
    dropped = []

    def make_key(recipe):
        if recipe.key:
            return recipe.key
        base = recipe.fields.get("research") or "recipe"
        synthetic[base] = synthetic.get(base, 0) + 1
        return f"{base}_{synthetic[base]}"

    for recipe in interpreter.recipes:
        recipe.key = make_key(recipe)
        if recipe.key in used_keys:
            raise SystemExit(f"duplicate key {recipe.key}")
        used_keys.add(recipe.key)

    for recipe in interpreter.recipes:
        key = java_string(recipe.key)
        research = java_string(recipe.fields.get("research", ""))
        kind = recipe.kind
        if kind in ("arcane_shaped", "arcane_shapeless"):
            result = java_stack(resolver.result_spec(recipe.result))
            if kind == "arcane_shaped":
                rows, mapping = parse_grid(recipe.params)
                width = max(len(row) for row in rows)
                grid = []
                specs = []
                for row in rows:
                    for column in range(width):
                        symbol = row[column] if column < len(row) else " "
                        if symbol == " " or symbol not in mapping:
                            specs.append(("empty",))
                        else:
                            specs.append(resolver.param_spec(mapping[symbol]))
                if result is None or any(spec is None for spec in specs):
                    dropped.append(recipe.key)
                    continue
                grid = ", ".join(java_ingredient(spec) for spec in specs)
                line = f"TcRecipes.register(new ShapedArcaneRecipe({key}, {research}, {result}, {recipe.aspects.java()}, {width}, {len(rows)}, List.of({grid})));"
            else:
                specs = [resolver.param_spec(param) for param in recipe.params]
                if result is None or any(spec is None for spec in specs):
                    dropped.append(recipe.key)
                    continue
                ingredients = ", ".join(java_ingredient(spec) for spec in specs)
                line = f"TcRecipes.register(new ShapelessArcaneRecipe({key}, {research}, {result}, {recipe.aspects.java()}, List.of({ingredients})));"
            sections["arcane"].append(line)
            counts["arcane"] += 1
        elif kind == "crucible":
            result = java_stack(resolver.result_spec(recipe.result))
            catalyst = resolver.param_spec(recipe.catalyst)
            if result is None or catalyst is None:
                dropped.append(recipe.key)
                continue
            line = f"TcRecipes.register(new CrucibleRecipe({key}, {research}, {result}, {java_ingredient(catalyst)}, {recipe.aspects.java()}));"
            sections["crucible"].append(line)
            counts["crucible"] += 1
        elif kind == "infusion":
            central = resolver.param_spec(recipe.central)
            if isinstance(recipe.result, list):
                if central is None:
                    result = None
                else:
                    first = central[1][0] if central[0] == "any" else central
                    result = java_stack({"id": first[1], "count": 1, "data": {recipe.result[0]: recipe.result[1]}})
            else:
                result = java_stack(resolver.result_spec(recipe.result))
            components = [resolver.param_spec(component) for component in recipe.components]
            if result is None or central is None or any(spec is None for spec in components):
                dropped.append(recipe.key)
                continue
            joined = ", ".join(java_ingredient(spec) for spec in components)
            line = (
                f"TcRecipes.register(new InfusionRecipe({key}, {research}, {result}, {recipe.instability}, {recipe.aspects.java()}, "
                f"{java_ingredient(central)}, List.of({joined})));"
            )
            sections["infusion"].append(line)
            counts["infusion"] += 1
        elif kind == "infusion_enchantment":
            enchantment = recipe.enchantment.identifier if isinstance(recipe.enchantment, EnchantRef) else None
            if enchantment is None:
                dropped.append(recipe.key)
                continue
            components = [resolver.param_spec(component) for component in recipe.components]
            if any(spec is None for spec in components):
                dropped.append(recipe.key)
                continue
            joined = ", ".join(java_ingredient(spec) for spec in components)
            line = (
                f"TcRecipes.register(new InfusionEnchantmentRecipe({key}, {research}, Identifier.parse({java_string(enchantment)}), "
                f"{recipe.instability}, {recipe.aspects.java()}, List.of({joined})));"
            )
            sections["enchantment"].append(line)
            counts["infusion_enchantment"] += 1
        elif kind == "compound":
            aspects, width, height, depth, blocks = recipe.aspects, recipe.width, recipe.height, recipe.depth, recipe.blocks
            stacks = []
            failed = False
            for block in blocks:
                if block is None:
                    stacks.append("TcStack.NONE")
                    continue
                if isinstance(block, str):
                    spec = resolver.param_spec(block)
                    if block == "slabWood":
                        spec = ("item", "minecraft:oak_slab")
                    stacks.append(java_stack({"id": spec[1], "count": 1}) if spec else None)
                    continue
                if block.reference == "Blocks.lava":
                    stacks.append(java_stack({"id": "minecraft:lava_bucket", "count": 1}))
                    continue
                spec = resolver.result_spec(block)
                if spec is None:
                    failed = True
                    break
                stacks.append(java_stack(spec))
            if failed or any(entry is None for entry in stacks):
                dropped.append(recipe.key)
                continue
            line = (
                f"TcRecipes.register(new CompoundRecipe({key}, \"\", {aspects.java()}, {width}, {height}, {depth}, List.of({', '.join(stacks)})));"
            )
            sections["compound"].append(line)
            counts["compound"] += 1
        else:
            result = resolver.result_spec(recipe.result)
            if result is None:
                dropped.append(recipe.key)
                continue
            keyed = recipe.key in references or not recipe.key.startswith("recipe_")
            entry = {
                "key": recipe.key if not recipe.key.startswith("recipe_") else None,
                "research": references.get(recipe.key, ""),
                "kind": kind,
                "result": result,
            }
            if kind == "vanilla_shaped":
                rows, mapping = parse_grid(recipe.params)
                entry["pattern"] = rows
                entry["ingredients"] = {}
                for symbol, value in mapping.items():
                    spec = resolver.param_spec(value)
                    if spec is None:
                        entry["incomplete"] = True
                    else:
                        entry["ingredients"][symbol] = spec_to_json(spec)
            else:
                entry["ingredients"] = []
                for value in recipe.params:
                    spec = resolver.param_spec(value)
                    if spec is None:
                        entry["incomplete"] = True
                    else:
                        entry["ingredients"].append(spec_to_json(spec))
            if kind == "vanilla_shapeless_nbt" or recipe.key == "JarLabelNull" or result["id"] in ("minecraft:iron_ingot", "minecraft:iron_nugget"):
                entry["custom"] = True
            if resolver.unmapped and entry.get("incomplete"):
                pass
            vanilla.append(entry)

    for recipe_entry in vanilla:
        if recipe_entry["result"].get("aspects") or recipe_entry["result"].get("data"):
            recipe_entry["components"] = True
    write_vanilla(vanilla)

    keyed_vanilla = [entry for entry in vanilla if entry["key"] and not entry.get("incomplete")]
    refs = []
    for entry in vanilla:
        if entry["key"] and not entry.get("incomplete"):
            refs.append(entry)
    emit_java(sections, refs)
    summary(counts, vanilla, dropped, resolver, refs)


def write_vanilla(vanilla):
    seen = {}
    for entry in vanilla:
        result = entry["result"]
        base = entry["key"] and snake(entry["key"]) or result["id"].split(":", 1)[1]
        if entry.get("custom") and entry["key"]:
            base = snake(entry["key"])
        name = base
        counter = 1
        while name in seen:
            counter += 1
            name = f"{base}_{counter}"
        seen[name] = True
        entry["id"] = name
        if entry["key"] in ALIASES:
            entry["id"] = ALIASES[entry["key"]]
            entry["alias"] = True
    VANILLA_OUTPUT.write_text(json.dumps(vanilla, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def emit_java(sections, refs):
    order = ["normal", "arcane", "infusion", "enchantment", "crucible", "compound"]
    method_names = []
    bodies = []
    ref_lines = []
    for entry in refs:
        result = java_stack(entry["result"])
        ref_lines.append(
            f"TcRecipes.register(new CraftingRecipeRef({java_string(entry['key'])}, {java_string(entry['research'])}, "
            f"Identifier.parse({java_string('thaumcraft:jar_label_filter' if entry['key'].startswith('JarLabel') and entry['key'] != 'JarLabel' else 'thaumcraft:' + entry['id'])}), {result}));"
        )
    sections["normal"] = ref_lines
    for section in order:
        lines = sections[section]
        for start in range(0, len(lines), CHUNK):
            name = f"{section}{start // CHUNK}"
            method_names.append(name)
            body = "\n".join("        " + line for line in lines[start:start + CHUNK])
            bodies.append(f"    private static void {name}() {{\n{body}\n    }}")
    calls = "\n".join(f"        {name}();" for name in method_names)
    java = f"""package thaumcraft.crafting;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;

public final class ConfigRecipes {{
    private ConfigRecipes() {{
    }}

    public static void init() {{
{calls}
        TcRecipeAspects.register();
    }}

{chr(10).join(chr(10) + body if index else body for index, body in enumerate(bodies)).replace(chr(10) + chr(10) + "    private", chr(10) + chr(10) + "    private")}
}}
"""
    java = re.sub(r"\n    }\n    private", "\n    }\n\n    private", java)
    OUTPUT.write_text(java, encoding="utf-8")


def summary(counts, vanilla, dropped, resolver, refs):
    print("counts:", counts)
    for kind, expected in EXPECTED.items():
        if counts[kind] != expected:
            print(f"MISMATCH {kind}: {counts[kind]} (expected {expected})")
    print(f"vanilla recipes: {len(vanilla)}, keyed refs: {len(refs)}")
    print(f"dynamic recipes (phase 7): {sorted(set(DYNAMIC))} x{len(DYNAMIC)}")
    print(f"dropped: {dropped}")
    print(f"unmapped: {sorted(resolver.unmapped)}")
    for entry in REPORT:
        print("  " + entry)
    if any(counts[kind] != expected for kind, expected in EXPECTED.items()):
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
