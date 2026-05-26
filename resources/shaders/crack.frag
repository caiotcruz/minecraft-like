#version 330 core

in vec2 vUV;

uniform float uProgress; 

out vec4 FragColor;

void main() {
    vec2 uv    = vUV * 8.0;
    float stage = uProgress * 9.0; // 0–9

    float crack = 0.0;

    if (stage > 1.0) crack += step(0.935, abs(sin(uv.x * 5.13 + uv.y * 3.74)));
    if (stage > 2.5) crack += step(0.915, abs(sin(uv.x * 3.31 - uv.y * 7.22)));
    if (stage > 4.0) crack += step(0.895, abs(sin(uv.x * 8.17 + uv.y * 2.36)));
    if (stage > 5.5) crack += step(0.875, abs(sin(uv.x * 4.78 - uv.y * 6.11)));
    if (stage > 7.0) crack += step(0.855, abs(sin(uv.x * 11.3 + uv.y * 4.89)));

    crack = clamp(crack, 0.0, 1.0);

    float baseDark  = 0.06 + uProgress * 0.45;
    float crackDark = crack * 0.78;

    float finalAlpha = clamp(baseDark + crackDark, 0.0, 0.92);
    FragColor = vec4(0.0, 0.0, 0.0, finalAlpha);
}