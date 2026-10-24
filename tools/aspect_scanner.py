import re

ENUM_ASPECT = re.compile(r"public\s+enum\s+Aspect\b")
ENUM_CONSTANT = re.compile(r"(?:^|[{,])\s*([A-Z_]+)\s*\(", re.MULTILINE)
REGISTRY_ASPECT = re.compile(r"(?:^|[;{])\s*public\s+static\s+final\s+Aspect\s+([A-Z_]+)\s*=", re.MULTILINE)


def aspect_names(text):
    enum = ENUM_ASPECT.search(text)
    if enum:
        start = text.index("{", enum.end())
        end = text.index(";", start)
        return ENUM_CONSTANT.findall(text[start:end])
    return REGISTRY_ASPECT.findall(text)
