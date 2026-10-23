#version 330
uniform sampler2D InSampler;
in vec2 texCoord;
layout(std140) uniform WarpColorConfig { vec3 ColorScale; float Saturation; };
out vec4 fragColor;
const vec3 Gray=vec3(0.3,0.59,0.11);
void main(){ vec4 c=texture(InSampler,texCoord); vec3 s=c.rgb*ColorScale; float l=dot(s,Gray); fragColor=vec4((s-l)*Saturation+l,c.a); }
