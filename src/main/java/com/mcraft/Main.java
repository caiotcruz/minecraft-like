package com.mcraft;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

import com.mcraft.core.Window;

public class Main {

    public static void main(String[] args) {

        Window window = new Window(
                1280,
                720,
                "Minecraft Clássico"
        );

        window.init();

        while (!window.shouldClose()) {

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            /*
             * Próxima etapa:
             * shader.use();
             * renderer.render();
             */

            window.update();
        }
    }
}