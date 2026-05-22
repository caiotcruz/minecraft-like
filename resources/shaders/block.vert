#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aUV;
layout (location = 2) in float aLight;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;       

out vec2  vUV;
out float vLight;
out float vFogDist;         

void main() {
    vec4 worldPos = uModel * vec4(aPos, 1.0);
    vec4 viewPos  = uView  * worldPos;

    gl_Position = uProjection * viewPos;

    vUV      = aUV;
    vLight   = aLight;
    vFogDist = length(viewPos.xyz);  
}