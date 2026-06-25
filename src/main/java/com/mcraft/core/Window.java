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
import static org.lwjgl.glfw.GLFW.*;
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
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

public class Window {

    private long handle;
    private int width, height;
    private final String title;
    private boolean fullscreen = false;
    private int windowedX, windowedY, windowedW, windowedH;

    public Window(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public void init() {
        if (!glfwInit()) {
            throw new IllegalStateException("Falha ao inicializar GLFW");
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        handle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (handle == NULL) {
            throw new RuntimeException("Falha ao criar janela GLFW");
        }

        GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(handle,
            (vid.width()  - width)  / 2,
            (vid.height() - height) / 2);

        glfwMakeContextCurrent(handle);
        GL.createCapabilities();    

        glfwSwapInterval(1);     

        glViewport(0, 0, width, height);
        glClearColor(0.5f, 0.7f, 1.0f, 1.0f);

        glEnable(GL_DEPTH_TEST);    
        glEnable(GL_CULL_FACE);    
        glCullFace(GL_BACK);

        glfwShowWindow(handle);
    }

    public void swapAndPoll() {
        glfwSwapBuffers(handle);  
        glfwPollEvents();          
    }

    public void setFullscreen(boolean wantFullscreen) {
        if (wantFullscreen == fullscreen) return;

        if (wantFullscreen) {
            int[] x = new int[1], y = new int[1], w = new int[1], h = new int[1];
            glfwGetWindowPos (handle, x, y);
            glfwGetWindowSize(handle, w, h);
            windowedX = x[0]; windowedY = y[0];
            windowedW = w[0]; windowedH = h[0];

            long monitor = glfwGetPrimaryMonitor();
            GLFWVidMode mode = glfwGetVideoMode(monitor);

            glfwSetWindowMonitor(handle, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());

            width  = mode.width();
            height = mode.height();
        } else {
            glfwSetWindowMonitor(handle, 0L, windowedX, windowedY, windowedW, windowedH, GLFW_DONT_CARE);
            width  = windowedW;
            height = windowedH;
        }

        fullscreen = wantFullscreen;
        glViewport(0, 0, width, height);
    }

    public void toggleFullscreen() {
        setFullscreen(!fullscreen);
    }

    public void destroy() {
        if (handle != NULL) {
            glfwDestroyWindow(handle);
            handle = NULL;
        }

        glfwTerminate();
    }

    public boolean shouldClose() { return glfwWindowShouldClose(handle); }

    public long getHandle()  { return handle; }
    public int  getWidth()   { return width;  }
    public int  getHeight()  { return height; }
    public boolean isFullscreen() { return fullscreen; }
}