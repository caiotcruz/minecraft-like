package com.mcraft;

import com.mcraft.core.Window;

import static org.lwjgl.opengl.GL11.*;

public class Main {

    public static void main(String[] args) {

        Window window = new Window(
                854,
                480,
                "Minecraft Clássico - Etapa 2"
        );

        window.init();

        while (!window.shouldClose()) {

            // Limpa a tela
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Atualiza janela/input
            window.update();
        }
    }
}