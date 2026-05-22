#version 330 core

in vec2  vUV;
in float vLight;
in float vFogDist;

uniform sampler2D uTexture;

out vec4 FragColor;

void main() {
    vec4 texColor = texture(uTexture, vUV);

    if (texColor.a < 0.1) discard;

    vec3 lit = texColor.rgb * vLight;

    float fogStart  = 60.0;
    float fogEnd    = 100.0;
    float fogFactor = clamp((fogEnd - vFogDist) / (fogEnd - fogStart), 0.0, 1.0);
    vec3  fogColor  = vec3(0.5, 0.7, 1.0); 

    FragColor = vec4(mix(fogColor, lit, fogFactor), texColor.a);
}