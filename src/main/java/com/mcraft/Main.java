package com.mcraft;

import com.mcraft.core.*;
import com.mcraft.render.*;
import com.mcraft.ui.MenuScreen;

import static org.lwjgl.opengl.GL11.*;

public class Main {
    public static void main(String[] args) {
        Window window = new Window(1920, 1000, "MCraft");
        window.init();
        Input input = new Input(window.getHandle());

        Shader      hudShader = new Shader("hud.vert", "hud.frag");
        TextureAtlas atlas    = TextureAtlas.generateProcedural();
        float[]     ortho2D   = Camera.ortho(window.getWidth(), window.getHeight());

        GameSettings settings = GameSettings.loadOrDefault();

        MenuScreen menu = new MenuScreen(window.getWidth(), window.getHeight(), hudShader, atlas, ortho2D, settings);

        MenuScreen.PlayRequest request = null;
        while (!window.shouldClose() && request == null) {
            input.prepareFrame();
            window.swapAndPoll();
            menu.update(input);
            double[] mx = new double[1];
            double[] my = new double[1];
            org.lwjgl.glfw.GLFW.glfwGetCursorPos(window.getHandle(), mx, my);
            menu.updateMouse((int) mx[0], (int) my[0]); 
            if (input.isMouseJustPressed(0)) { 
                menu.onClick((int) mx[0], (int) my[0]);
            }
            input.endFrame();
            input.updateKeyEdges();

            glClearColor(0.08f, 0.08f, 0.12f, 1f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            menu.render();

            request = menu.consumePlayRequest();
        }

        menu.delete();

        if (request != null && !window.shouldClose()) {
            GameLoop game = new GameLoop(window, input, hudShader, atlas, ortho2D, settings, request.worldName(), request.seed(), request.isNewWorld());
            game.run();
        }

        window.destroy();
    }
}