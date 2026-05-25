#version 330 core

in float vLight;
in vec3  vColor;
in float vFogDist;

uniform float uAmbientLight;
uniform vec3  uFogColor;

out vec4 FragColor;

void main() {
    vec3 lit = vColor * vLight * uAmbientLight;
    float fogFactor = clamp((80.0 - vFogDist) / 30.0, 0.0, 1.0);
    FragColor = vec4(mix(uFogColor, lit, fogFactor), 1.0);
}