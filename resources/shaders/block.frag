#version 330 core

in vec2  vUV;
in float vLight;
in float vFogDist;

uniform sampler2D uTexture;

out vec4 FragColor;

uniform float uAmbientLight;
uniform vec3  uFogColor;

void main() {
    vec4 texColor = texture(uTexture, vUV);
    if (texColor.a < 0.1) discard;

    vec3 lit = texColor.rgb * vLight * uAmbientLight;

    float fogFactor = clamp((80.0 - vFogDist) / 30.0, 0.0, 1.0);
    FragColor = vec4(mix(uFogColor, lit, fogFactor), texColor.a);
}