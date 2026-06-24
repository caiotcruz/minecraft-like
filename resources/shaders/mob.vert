#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in float aLightDir;
layout (location = 2) in vec3  aColor;
layout (location = 3) in float aSkyLight;
layout (location = 4) in float aBlockLight;

uniform mat4 uProjection;
uniform mat4 uView;

out float vLightDir;
out vec3  vColor;
out float vSkyLight;
out float vBlockLight;
out float vFogDist;

void main() {
    vec4 viewPos = uView * vec4(aPos, 1.0);
    gl_Position  = uProjection * viewPos;
    vLightDir    = aLightDir;
    vColor       = aColor;
    vSkyLight    = aSkyLight;
    vBlockLight  = aBlockLight;
    vFogDist     = length(viewPos.xyz);
}