#!/usr/bin/env python3
"""Generate TC4 warp post-effects. Use --mc 26.3|26.1.2|1.21.1|1.19.2 [--out RESOURCE_ROOT]."""
import argparse, json, re
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
PENDING = ROOT / "tools/warp_shader_sources"
DEFAULT_OUT = ROOT / "src/generated/resources"

def put_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")
def put(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
def modernize(path, sampler, output_name):
    s = path.read_text(encoding="utf-8").replace("#version 120", "#version 330\n#extension GL_ARB_separate_shader_objects : require")
    s = s.replace("uniform sampler2D DiffuseSampler;", f"uniform sampler2D {sampler};").replace("uniform vec2 OutSize;", "")
    s = s.replace("varying vec2 texCoord;", "layout(location = 0) in vec2 texCoord;\nlayout(location = 0) out vec4 fragColor;")
    s = s.replace("texture2D(DiffuseSampler", f"texture({sampler}").replace("gl_FragColor", "fragColor")
    if output_name == "warp_bloom_color":
        s = guard_gray_hue(s)
    return s

def guard_gray_hue(source):
    return source.replace("""        vec3 adds = vec3(
                          ((green - blue ) / delta),
                2.0 + ((blue  - red  ) / delta),
                4.0 + ((red   - green) / delta)
        );""", """        vec3 adds = delta > 0.0 ? vec3(
                          ((green - blue ) / delta),
                2.0 + ((blue  - red  ) / delta),
                4.0 + ((red   - green) / delta)
        ) : vec3(0.0);""")
def legacyize(path):
    source = (path.read_text(encoding="utf-8").replace("#version 120", "#version 150")
        .replace("uniform vec2 OutSize;", "").replace("varying vec2 texCoord;", "in vec2 texCoord;\nout vec4 fragColor;")
        .replace("texture2D(", "texture(").replace("gl_FragColor", "fragColor"))
    if path.stem == "bloomColor":
        source = guard_gray_hue(source)
    return source
def generate_modern(root):
    a = root / "assets/thaumcraft"
    swap = {"targets": {"swap": {}}}
    def simple_pass(fragment, source, out, uniforms=None):
        d = {"vertex_shader":"minecraft:core/screenquad", "fragment_shader":f"thaumcraft:post/{fragment}",
             "inputs":[{"sampler_name":"In", "target":source, "bilinear":True}], "output":out}
        if uniforms: d["uniforms"] = uniforms
        return d
    def blur(direction):
        return {"vertex_shader":"minecraft:core/screenquad", "fragment_shader":"minecraft:post/box_blur",
                "inputs":[{"sampler_name":"In", "target":"minecraft:main" if direction[0] else "swap", "bilinear":True}],
                "output":"swap" if direction[0] else "minecraft:main",
                "uniforms":{"BlurConfig":[{"name":"BlurDir","type":"vec2","value":direction},{"name":"Radius","type":"float","value":10.0}]}}
    put_json(a/"post_effect/blurtc.json", {**swap,"passes":[blur([1.0,0.0]),blur([0.0,1.0])]})
    def color_effect(name, scale, sat):
        u={"WarpColorConfig":[{"name":"ColorScale","type":"vec3","value":scale},{"name":"Saturation","type":"float","value":sat}]}
        put_json(a/f"post_effect/{name}.json",{**swap,"passes":[simple_pass("warp_color","minecraft:main","swap",u),
            {"vertex_shader":"minecraft:core/screenquad","fragment_shader":"minecraft:post/blit","inputs":[{"sampler_name":"In","target":"swap"}],"output":"minecraft:main",
             "uniforms":{"BlitConfig":[{"name":"ColorModulate","type":"vec4","value":[1.0,1.0,1.0,1.0]}]}}]})
    color_effect("desaturatetc",[1.0,1.0,1.0],0.8)
    color_effect("hunger",[1.0,0.8,0.8],1.1)
    put_json(a/"post_effect/sunscorned.json",{**swap,"passes":[simple_pass("warp_bloom_color","minecraft:main","swap"),simple_pass("warp_bloom_add","swap","minecraft:main")]})
    put(a/"shaders/post/warp_color.fsh", """#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
layout(location=0) in vec2 texCoord;
layout(std140) uniform WarpColorConfig { vec3 ColorScale; float Saturation; };
layout(location=0) out vec4 fragColor;
const vec3 Gray=vec3(0.3,0.59,0.11);
void main(){ vec4 c=texture(InSampler,texCoord); vec3 s=c.rgb*ColorScale; float l=dot(s,Gray); fragColor=vec4((s-l)*Saturation+l,c.a); }
""")
    put(a/"shaders/post/warp_bloom_color.fsh",modernize(PENDING / "program/bloomColor.fsh","InSampler","warp_bloom_color"))
    put(a/"shaders/post/warp_bloom_add.fsh",modernize(PENDING / "program/bloom2.fsh","InSampler","warp_bloom_add"))
def glsl_26_1(source):
    source = source.replace("#extension GL_ARB_separate_shader_objects : require\n", "").replace("#include <", "#moj_import <")
    return re.sub(r"layout\(\s*location\s*=\s*\d+\s*\)\s*", "", source)
def generate_legacy(root):
    a=root/"assets/thaumcraft/shaders"; post=a/"post"; prog=a/"program"
    p=PENDING / "post"; q=PENDING / "program"
    put_json(post/"blurtc.json",json.loads((p/"blurtc.json").read_text()))
    d=json.loads((p/"desaturatetc.json").read_text()); d["passes"][0]["name"]="color_convolve"; put_json(post/"desaturatetc.json",d)
    h=json.loads((p/"hunger.json").read_text()); h["passes"][0]["name"]="thaumcraft:warp_color_convolve"; put_json(post/"hunger.json",h)
    s=json.loads((p/"sunscorned.json").read_text()); s["passes"][0]["name"]="thaumcraft:warp_bloom_color"; s["passes"][1]["name"]="thaumcraft:warp_bloom_add"; put_json(post/"sunscorned.json",s)
    v=(q/"bloomVertexBase.vsh").read_text().replace("#version 120","#version 150").replace("attribute vec4 Position;","in vec4 Position;").replace("varying vec2 texCoord;","out vec2 texCoord;")
    put(prog/"warp_bloom_vertex.vsh",v)
    put(prog/"warp_color_convolve.fsh", """#version 150
uniform sampler2D DiffuseSampler;
in vec2 texCoord;
uniform vec3 ColorScale;
uniform float Saturation;
out vec4 fragColor;
const vec3 Gray=vec3(0.3,0.59,0.11);
void main(){ vec4 c=texture(DiffuseSampler,texCoord); vec3 s=c.rgb*ColorScale; float l=dot(s,Gray); fragColor=vec4((s-l)*Saturation+l,c.a); }
""")
    put(prog/"warp_bloom_color.fsh",legacyize(q/"bloomColor.fsh")); put(prog/"warp_bloom_add.fsh",legacyize(q/"bloom2.fsh"))
    def program(name, vertex, fragment, uniforms):
        u=[{"name":"ProjMat","type":"matrix4x4","count":16,"values":[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1]},
           {"name":"InSize","type":"float","count":2,"values":[1,1]},{"name":"OutSize","type":"float","count":2,"values":[1,1]},*uniforms]
        vertex_id = "minecraft:sobel" if vertex == "sobel" else f"thaumcraft:{vertex}"
        put_json(prog/f"{name}.json",{"blend":{"func":"add","srcrgb":"one","dstrgb":"zero"},"vertex":vertex_id,"fragment":f"thaumcraft:{fragment}","attributes":["Position"],"samplers":[{"name":"DiffuseSampler"}],"uniforms":u})
    program("warp_color_convolve","sobel","warp_color_convolve",[
        {"name":"ColorScale","type":"float","count":3,"values":[1.0,0.8,0.8]}, {"name":"Saturation","type":"float","count":1,"values":[1.1]}])
    program("warp_bloom_color","warp_bloom_vertex","warp_bloom_color",[]); program("warp_bloom_add","warp_bloom_vertex","warp_bloom_add",[])
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mc",required=True,choices=("26.3","26.1.2","1.21.1","1.19.2"))
    parser.add_argument("--out",type=Path,default=DEFAULT_OUT,help="resource root (defaults to src/generated/resources)")
    args=parser.parse_args(); out=args.out.resolve()
    generate(args.mc, out)
    print(f"Generated warp post effects for {args.mc}: {out}")

def generate(mc, out=DEFAULT_OUT):
    """Generate effects into a resource root; useful to compose with gen_resources.py."""
    output = Path(out).resolve()
    if mc == "26.3":
        generate_modern(output)
    elif mc == "26.1.2":
        generate_modern(output)
        for shader in (output / "assets/thaumcraft/shaders/post").glob("*.fsh"):
            shader.write_text(glsl_26_1(shader.read_text(encoding="utf-8")), encoding="utf-8")
    elif mc in ("1.21.1", "1.19.2"):
        generate_legacy(output)
    else:
        raise ValueError(f"unsupported Minecraft profile: {mc}")
    return output
if __name__=="__main__": main()
