#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in vec2  aUV;
layout (location = 2) in float aLightDir;
layout (location = 3) in float aSkyLight;
layout (location = 4) in float aBlockLight;

uniform mat4  uProjection;
uniform mat4  uView;
uniform mat4  uModel;
uniform float uAmbientLight;

out vec2  vUV;
out float vLight;

void main() {
    gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0);
    vUV = aUV;

    float skyContrib = aSkyLight * uAmbientLight;

    float blockContrib = aBlockLight;

    float effectiveLight = max(skyContrib, blockContrib);

    effectiveLight = max(effectiveLight, 0.04);

    vLight = aLightDir * effectiveLight;
}