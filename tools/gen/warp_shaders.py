import json
import re

from .common import TOOLS

SOURCES = TOOLS / "shaders"
GRAY_HUE = """        vec3 adds = vec3(
                          ((green - blue ) / delta),
                2.0 + ((blue  - red  ) / delta),
                4.0 + ((red   - green) / delta)
        );"""
GUARDED_GRAY_HUE = """        vec3 adds = delta > 0.0 ? vec3(
                          ((green - blue ) / delta),
                2.0 + ((blue  - red  ) / delta),
                4.0 + ((red   - green) / delta)
        ) : vec3(0.0);"""
MODERN_COLOR = """#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
layout(location=0) in vec2 texCoord;
layout(std140) uniform WarpColorConfig { vec3 ColorScale; float Saturation; };
layout(location=0) out vec4 fragColor;
const vec3 Gray=vec3(0.3,0.59,0.11);
void main(){ vec4 c=texture(InSampler,texCoord); vec3 s=c.rgb*ColorScale; float l=dot(s,Gray); fragColor=vec4((s-l)*Saturation+l,c.a); }
"""
LEGACY_COLOR = """#version 150
uniform sampler2D DiffuseSampler;
in vec2 texCoord;
uniform vec3 ColorScale;
uniform float Saturation;
out vec4 fragColor;
const vec3 Gray=vec3(0.3,0.59,0.11);
void main(){ vec4 c=texture(DiffuseSampler,texCoord); vec3 s=c.rgb*ColorScale; float l=dot(s,Gray); fragColor=vec4((s-l)*Saturation+l,c.a); }
"""
IDENTITY = [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]


def put_json(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(content, indent=2) + "\n", encoding="utf-8")


def put(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def guard_gray_hue(source):
    return source.replace(GRAY_HUE, GUARDED_GRAY_HUE)


def modernize(path, sampler, output_name):
    source = path.read_text(encoding="utf-8").replace("#version 120", "#version 330\n#extension GL_ARB_separate_shader_objects : require")
    source = source.replace("uniform sampler2D DiffuseSampler;", f"uniform sampler2D {sampler};").replace("uniform vec2 OutSize;", "")
    source = source.replace("varying vec2 texCoord;", "layout(location = 0) in vec2 texCoord;\nlayout(location = 0) out vec4 fragColor;")
    source = source.replace("texture2D(DiffuseSampler", f"texture({sampler}").replace("gl_FragColor", "fragColor")
    if output_name == "warp_bloom_color":
        source = guard_gray_hue(source)
    return source


def legacyize(path):
    source = path.read_text(encoding="utf-8").replace("#version 120", "#version 150")
    source = source.replace("uniform vec2 OutSize;", "").replace("varying vec2 texCoord;", "in vec2 texCoord;\nout vec4 fragColor;")
    source = source.replace("texture2D(", "texture(").replace("gl_FragColor", "fragColor")
    if path.stem == "bloomColor":
        source = guard_gray_hue(source)
    return source


def glsl_26_1(source):
    source = source.replace("#extension GL_ARB_separate_shader_objects : require\n", "").replace("#include <", "#moj_import <")
    return re.sub(r"layout\(\s*location\s*=\s*\d+\s*\)\s*", "", source)


def simple_pass(fragment, source, output, uniforms=None):
    content = {
        "vertex_shader": "minecraft:core/screenquad",
        "fragment_shader": f"thaumcraft:post/{fragment}",
        "inputs": [{"sampler_name": "In", "target": source, "bilinear": True}],
        "output": output,
    }
    if uniforms:
        content["uniforms"] = uniforms
    return content


def blur_pass(direction):
    horizontal = direction[0]
    return {
        "vertex_shader": "minecraft:core/screenquad",
        "fragment_shader": "minecraft:post/box_blur",
        "inputs": [{"sampler_name": "In", "target": "minecraft:main" if horizontal else "swap", "bilinear": True}],
        "output": "swap" if horizontal else "minecraft:main",
        "uniforms": {"BlurConfig": [{"name": "BlurDir", "type": "vec2", "value": direction}, {"name": "Radius", "type": "float", "value": 10.0}]},
    }


def generate_modern(assets):
    swap = {"targets": {"swap": {}}}
    put_json(assets / "post_effect" / "blurtc.json", {**swap, "passes": [blur_pass([1.0, 0.0]), blur_pass([0.0, 1.0])]})
    for name, scale, saturation in [("desaturatetc", [1.0, 1.0, 1.0], 0.8), ("hunger", [1.0, 0.8, 0.8], 1.1)]:
        uniforms = {"WarpColorConfig": [{"name": "ColorScale", "type": "vec3", "value": scale}, {"name": "Saturation", "type": "float", "value": saturation}]}
        put_json(assets / "post_effect" / f"{name}.json", {**swap, "passes": [
            simple_pass("warp_color", "minecraft:main", "swap", uniforms),
            {
                "vertex_shader": "minecraft:core/screenquad",
                "fragment_shader": "minecraft:post/blit",
                "inputs": [{"sampler_name": "In", "target": "swap"}],
                "output": "minecraft:main",
                "uniforms": {"BlitConfig": [{"name": "ColorModulate", "type": "vec4", "value": [1.0, 1.0, 1.0, 1.0]}]},
            },
        ]})
    put_json(assets / "post_effect" / "sunscorned.json", {**swap, "passes": [
        simple_pass("warp_bloom_color", "minecraft:main", "swap"),
        simple_pass("warp_bloom_add", "swap", "minecraft:main"),
    ]})
    put(assets / "shaders" / "post" / "warp_color.fsh", MODERN_COLOR)
    put(assets / "shaders" / "post" / "warp_bloom_color.fsh", modernize(SOURCES / "program" / "bloomColor.fsh", "InSampler", "warp_bloom_color"))
    put(assets / "shaders" / "post" / "warp_bloom_add.fsh", modernize(SOURCES / "program" / "bloom2.fsh", "InSampler", "warp_bloom_add"))


def legacy_program(folder, name, vertex, fragment, uniforms):
    vertex_id = "minecraft:sobel" if vertex == "sobel" else f"thaumcraft:{vertex}"
    put_json(folder / f"{name}.json", {
        "blend": {"func": "add", "srcrgb": "one", "dstrgb": "zero"},
        "vertex": vertex_id,
        "fragment": f"thaumcraft:{fragment}",
        "attributes": ["Position"],
        "samplers": [{"name": "DiffuseSampler"}],
        "uniforms": [
            {"name": "ProjMat", "type": "matrix4x4", "count": 16, "values": IDENTITY},
            {"name": "InSize", "type": "float", "count": 2, "values": [1, 1]},
            {"name": "OutSize", "type": "float", "count": 2, "values": [1, 1]},
            *uniforms,
        ],
    })


def generate_legacy(assets, mc):
    post = assets / "shaders" / "post"
    program = assets / "shaders" / "program"
    renames = {
        "blurtc": [],
        "desaturatetc": ["color_convolve"],
        "hunger": ["thaumcraft:warp_color_convolve"],
        "sunscorned": ["thaumcraft:warp_bloom_color", "thaumcraft:warp_bloom_add"],
    }
    for name, passes in renames.items():
        content = json.loads((SOURCES / "post" / f"{name}.json").read_text())
        if name == "blurtc" and mc == "1.21.1":
            for blur in content["passes"]:
                blur["name"] = "box_blur"
        for index, pass_name in enumerate(passes):
            content["passes"][index]["name"] = pass_name
        put_json(post / f"{name}.json", content)
    vertex = (SOURCES / "program" / "bloomVertexBase.vsh").read_text()
    vertex = vertex.replace("#version 120", "#version 150").replace("attribute vec4 Position;", "in vec4 Position;").replace("varying vec2 texCoord;", "out vec2 texCoord;")
    put(program / "warp_bloom_vertex.vsh", vertex)
    put(program / "warp_color_convolve.fsh", LEGACY_COLOR)
    put(program / "warp_bloom_color.fsh", legacyize(SOURCES / "program" / "bloomColor.fsh"))
    put(program / "warp_bloom_add.fsh", legacyize(SOURCES / "program" / "bloom2.fsh"))
    legacy_program(program, "warp_color_convolve", "sobel", "warp_color_convolve", [
        {"name": "ColorScale", "type": "float", "count": 3, "values": [1.0, 0.8, 0.8]},
        {"name": "Saturation", "type": "float", "count": 1, "values": [1.1]},
    ])
    legacy_program(program, "warp_bloom_color", "warp_bloom_vertex", "warp_bloom_color", [])
    legacy_program(program, "warp_bloom_add", "warp_bloom_vertex", "warp_bloom_add", [])


def generate(ctx):
    mc = ctx.profile.name
    if mc in ("26.3", "26.1.2"):
        generate_modern(ctx.ASSETS)
    else:
        generate_legacy(ctx.ASSETS, mc)
    if mc == "26.1.2":
        for shader in (ctx.ASSETS / "shaders" / "post").glob("*.fsh"):
            shader.write_text(glsl_26_1(shader.read_text(encoding="utf-8")), encoding="utf-8")
