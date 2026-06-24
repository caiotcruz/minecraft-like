#version 330 core

in float vLightDir;
in vec3  vColor;
in float vSkyLight;
in float vBlockLight;
in float vFogDist;

uniform float uAmbientLight;
uniform vec3  uFogColor;
uniform float uUnderwaterFog;

out vec4 FragColor;

void main() {
    float skyContrib = vSkyLight * uAmbientLight;
    float blockContrib = vBlockLight;
    
    float effectiveLight = max(skyContrib, blockContrib);

    effectiveLight = pow(effectiveLight, 2.5);

    effectiveLight = max(effectiveLight, 0.025); 

    vec3 lit = vColor * vLightDir * effectiveLight;

    float fogFar  = mix(80.0, 18.0, uUnderwaterFog);
    float fogNear = mix(50.0,  6.0, uUnderwaterFog);
    float fogFactor = clamp((fogFar - vFogDist) / (fogFar - fogNear), 0.0, 1.0);

    FragColor = vec4(mix(uFogColor, lit, fogFactor), 1.0);
}