package com.mcraft.core;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

import com.mcraft.player.Player;
import com.mcraft.player.Raycast;
import com.mcraft.render.Camera;
import com.mcraft.render.Shader;
import com.mcraft.render.TextureAtlas;
import com.mcraft.world.World;

public class GameLoop {

    private static final float FIXED_STEP = 1.0f / 60.0f;
    private static final float REACH      = 5.0f;
    private static final float MOUSE_SENS = 0.0015f;

    private final Window       window;
    private final Input        input;
    private final World        world;
    private final Player       player;
    private final Camera       camera;
    private final Shader       blockShader;
    private final Shader       hudShader;
    private final TextureAtlas atlas;
    private final com.mcraft.ui.HUD hud;

    private float  accumulator = 0f;
    private double lastTime;

    private boolean leftWasDown  = false;
    private boolean rightWasDown = false;

    public GameLoop(Window window) {
        this.window = window;

        input = new Input(window.getHandle());

        glfwSetInputMode(window.getHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        world  = new World(42L);
        player = new Player(8, 90, 8, world);
        camera = player.getCamera();

        glfwSetScrollCallback(window.getHandle(), (win, xOff, yOff) ->
            player.getInventory().scrollHotbar((int) -yOff)
        );

        world.generateAround(player.getX(), player.getZ());

        blockShader = new Shader("block.vert", "block.frag");
        hudShader   = new Shader("hud.vert",   "hud.frag");

        atlas = new TextureAtlas("/textures/terrain.png");

        hud = new com.mcraft.ui.HUD(
            window.getWidth(), window.getHeight(),
            player.getInventory(), hudShader, atlas
        );

        lastTime = glfwGetTime();
    }


    public void run() {
        while (!window.shouldClose()) {
            double now = glfwGetTime();
            float  dt  = (float)(now - lastTime);
            lastTime   = now;

            if (dt > 0.25f) dt = 0.25f;

            input.prepareFrame();
            window.swapAndPoll();  
            input.endFrame();

            accumulator += dt;
            while (accumulator >= FIXED_STEP) {
                update(FIXED_STEP);
                accumulator -= FIXED_STEP;
            }

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            render3D();
            hud.render();
        }

        cleanup();
    }

    private void update(float dt) {

        float dx = 0, dz = 0;
        if (input.isKeyDown(GLFW_KEY_W)) dz -= 1;
        if (input.isKeyDown(GLFW_KEY_S)) dz += 1;
        if (input.isKeyDown(GLFW_KEY_A)) dx -= 1;
        if (input.isKeyDown(GLFW_KEY_D)) dx += 1;
        boolean jump = input.isKeyDown(GLFW_KEY_SPACE);

        float mdx = input.consumeMouseDX();
        float mdy = input.consumeMouseDY();
        camera.rotate(mdx * MOUSE_SENS, mdy * MOUSE_SENS);

        player.update(dx, dz, jump, dt);

        for (int k = 0; k < 9; k++) {
            if (input.isKeyDown(GLFW_KEY_1 + k)) {
                player.getInventory().setSelectedSlot(k);
            }
        }

        float[] front = camera.getFront();
        Raycast.HitResult hit = Raycast.cast(
            camera.getX(), camera.getY(), camera.getZ(),
            front[0], front[1], front[2],
            REACH, world
        );

        if (hit.hit) {
            boolean leftDown = input.isMouseDown(GLFW_MOUSE_BUTTON_LEFT);
            if (leftDown && !leftWasDown) {
                world.setBlock(hit.blockX, hit.blockY, hit.blockZ, 0);
            }
            leftWasDown = leftDown;

            boolean rightDown = input.isMouseDown(GLFW_MOUSE_BUTTON_RIGHT);
            if (rightDown && !rightWasDown) {
                int blockId = player.getInventory().getSelectedBlockId();
                if (blockId != 0) {
                    world.setBlock(hit.prevX, hit.prevY, hit.prevZ, blockId);
                    player.getInventory().consumeSelected(1);
                }
            }
            rightWasDown = rightDown;
        } else {
            leftWasDown  = false;
            rightWasDown = false;
        }

        if (input.isKeyDown(GLFW_KEY_ESCAPE)) {
            glfwSetWindowShouldClose(window.getHandle(), true);
        }
    }

    private void render3D() {
        blockShader.use();

        float[] proj = Camera.perspective(70f,
            (float) window.getWidth() / window.getHeight(), 0.05f, 500f);

        blockShader.setMatrix4("uProjection", proj);
        blockShader.setMatrix4("uView",       camera.getViewMatrix());

        atlas.bind(0);
        blockShader.setInt("uTexture", 0);

        world.render(blockShader, camera);
    }

    private void cleanup() {
        blockShader.delete();
        hudShader.delete();
        atlas.delete();
    }
}