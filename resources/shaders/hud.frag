#version 330 core

in vec2 vUV;
in vec4 vColor;

uniform sampler2D uTexture;
uniform int       uUseTexture; 

out vec4 FragColor;

void main() {
    if (uUseTexture == 1) {
        vec4 tex = texture(uTexture, vUV);
        if (tex.a < 0.1) discard;
        FragColor = tex * vColor;
    } else {
        FragColor = vColor;
    }
}