package com.mcraft.core;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;

public class Input {

    private final long window;

    private double lastX, lastY;
    private float  mouseDX, mouseDY;
    private boolean firstMouse = true; 

    private final boolean[] mouseJustPressed  = new boolean[8];
    private final boolean[] mousePrevPressed  = new boolean[8];

    public Input(long window) {
        this.window = window;

        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            if (firstMouse) {
                lastX = xpos;
                lastY = ypos;
                firstMouse = false;
            }
            mouseDX += (float)(xpos - lastX);
            mouseDY += (float)(lastY - ypos);
            lastX = xpos;
            lastY = ypos;
        });
    }

    public void prepareFrame() {
        for (int i = 0; i < mouseJustPressed.length; i++) {
            mousePrevPressed[i] = glfwGetMouseButton(window, i) == GLFW_PRESS;
        }
    }

    public void endFrame() {
        for (int i = 0; i < mouseJustPressed.length; i++) {
            boolean now = glfwGetMouseButton(window, i) == GLFW_PRESS;
            mouseJustPressed[i] = now && !mousePrevPressed[i];
        }
    }

    public boolean isKeyDown(int key) {
        return glfwGetKey(window, key) == GLFW_PRESS;
    }

    public boolean isMouseDown(int button) {
        return glfwGetMouseButton(window, button) == GLFW_PRESS;
    }

    public boolean isMouseJustPressed(int button) {
        return mouseJustPressed[button];
    }

    public float consumeMouseDX() {
        float v = mouseDX;
        mouseDX = 0;
        return v;
    }

    public float consumeMouseDY() {
        float v = mouseDY;
        mouseDY = 0;
        return v;
    }

    public void resetFirstMouse() {
        firstMouse = true;
        mouseDX    = 0f;
        mouseDY    = 0f;
    }
}