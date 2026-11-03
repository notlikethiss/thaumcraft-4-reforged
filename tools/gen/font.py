GALACTIC_LETTERS = {
    "А": "A", "Б": "B", "В": "V", "Г": "G", "Д": "D", "Е": "E", "Ё": "E", "Ж": "X", "З": "Z", "И": "I", "Й": "J",
    "К": "K", "Л": "L", "М": "M", "Н": "N", "О": "O", "П": "P", "Р": "R", "С": "S", "Т": "T", "У": "U", "Ф": "F",
    "Х": "H", "Ц": "C", "Ч": "C", "Ш": "W", "Щ": "W", "Ъ": "Q", "Ы": "Y", "Ь": "Q", "Э": "E", "Ю": "U", "Я": "Y",
}


def galactic_font(ctx):
    layers = []
    for cyrillic, latin in GALACTIC_LETTERS.items():
        for source, target in ((cyrillic, latin), (cyrillic.lower(), latin.lower())):
            code = ord(target)
            layer = next((grid for grid in layers if code not in grid), None)
            if layer is None:
                layer = {}
                layers.append(layer)
            layer[code] = source
    providers = [{"type": "reference", "id": "minecraft:alt"}]
    for layer in layers:
        rows = ["".join(layer.get(row * 16 + column, "\u0000") for column in range(16)) for row in range(16)]
        providers.append({"type": "bitmap", "file": "minecraft:font/ascii_sga.png", "ascent": 7, "chars": rows})
    ctx.write(ctx.ASSETS / "font" / "galactic.json", {"providers": providers})
