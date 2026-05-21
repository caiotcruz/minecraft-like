package com.mcraft.core;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    private long windowHandle;
    private int width, height;
    private String title;

    public Window(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public void init() {
        // 1. Inicializa a biblioteca GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Falha ao inicializar GLFW");
        }

        // 2. Configura hints — versão OpenGL 3.3 core profile
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        // 3. Cria a janela
        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Falha ao criar janela GLFW");
        }

        // 4. Centraliza na tela
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(
            windowHandle,
            (vidMode.width() - width) / 2,
            (vidMode.height() - height) / 2
        );

        // 5. Torna o contexto OpenGL ativo nesta thread
        glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities(); // Inicializa bindings OpenGL

        // 6. VSync (limita a 60fps)
        glfwSwapInterval(1);

        // 7. Viewport e cor de fundo (céu azul)
        glViewport(0, 0, width, height);
        glClearColor(0.5f, 0.7f, 1.0f, 1.0f);

        // 8. Habilita teste de profundidade (Z-buffer)
        glEnable(GL_DEPTH_TEST);

        // 9. Culling de faces traseiras (otimização essencial)
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        glfwShowWindow(windowHandle);
    }

    public void update() {
        glfwSwapBuffers(windowHandle); // Troca front/back buffer
        glfwPollEvents();              // Processa eventos de input
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    public long getHandle() { return windowHandle; }
    public int getWidth()   { return width; }
    public int getHeight()  { return height; }
}