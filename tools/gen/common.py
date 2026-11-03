from pathlib import Path

NS = "thaumcraft"
ROOT = Path(__file__).resolve().parents[2]
TOOLS = ROOT / "tools"
FACES = ["down", "up", "north", "south", "west", "east"]
SIDES = ["north", "south", "west", "east"]


def box(start, end, faces, **extra):
    return {"from": start, "to": end, "faces": faces, **extra}


def uniform_faces(texture, names=FACES, **extra):
    return {face: {"texture": texture, **extra} for face in names}


def flat(face, start, end, texture, emission=0, **extra):
    element = {"from": start, "to": end, "faces": {face: {"texture": texture, **extra}}}
    if emission:
        element["light_emission"] = emission
    return element


def mc_box(origin, size, uv, texture_size, texture="#t"):
    rx, ry, rz = origin
    w, h, d = size
    u, v = uv
    tw, th = texture_size
    scale_u = 16 / tw
    scale_v = 16 / th

    def rect(x1, y1, x2, y2):
        return [round(x1 * scale_u, 4), round(y1 * scale_v, 4), round(x2 * scale_u, 4), round(y2 * scale_v, 4)]

    def face(*coordinates):
        return {"uv": rect(*coordinates), "texture": texture}

    return {
        "from": [8 + rx, 16 - ry - h, 8 - rz - d],
        "to": [8 + rx + w, 16 - ry, 8 - rz],
        "faces": {
            "up": face(u + d, v, u + d + w, v + d),
            "down": face(u + d + w, v, u + d + 2 * w, v + d),
            "west": face(u, v + d, u + d, v + d + h),
            "east": face(u + d + w, v + d, u + 2 * d + w, v + d + h),
            "south": face(u + d, v + d, u + d + w, v + d + h),
            "north": face(u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
        },
    }
