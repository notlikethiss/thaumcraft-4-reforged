import json
from pathlib import Path


MODERN = """#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec2 texCoord0;
layout(location = 3) in vec4 vertexColor;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a <= 1.0 / 255.0) {
        discard;
    }
    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
        #ifdef OIT_ACCUMULATE
        color = sampleColorForAccumulation(color);
        vec4 fogColor = vec4(FogColor.rgb * color.a, FogColor.a);
        #else
        vec4 fogColor = FogColor;
        #endif
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, fogColor);
    #endif
}
"""

MODERN_26_1 = """#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a <= 1.0 / 255.0) {
        discard;
    }
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
"""

LEGACY = """#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a <= 1.0 / 255.0) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
"""


def generate(mc, out):
    root = Path(out) / "assets/thaumcraft/shaders/core"
    root.mkdir(parents=True, exist_ok=True)
    (root / "node.fsh").write_text({"26.3": MODERN, "26.1.2": MODERN_26_1}.get(mc, LEGACY), encoding="utf-8")
    if mc in ("26.3", "26.1.2"):
        return
    identity = [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]
    uniforms = [
        ("ModelViewMat", "matrix4x4", identity),
        ("ProjMat", "matrix4x4", identity),
        ("ColorModulator", "float", [1, 1, 1, 1]),
        ("FogStart", "float", [0]),
        ("FogEnd", "float", [1]),
        ("FogColor", "float", [0, 0, 0, 0]),
        ("FogShape", "int", [0]),
    ]
    program = {
        "vertex": "minecraft:particle",
        "fragment": "thaumcraft:node",
        "samplers": [{"name": "Sampler0"}, {"name": "Sampler2"}],
        "uniforms": [
            {"name": name, "type": kind, "count": len(values), "values": values}
            for name, kind, values in uniforms
        ],
    }
    (root / "node.json").write_text(json.dumps(program, indent=2) + "\n", encoding="utf-8")
