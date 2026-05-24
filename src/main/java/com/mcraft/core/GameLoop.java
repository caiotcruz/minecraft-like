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
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;
import static org.lwjgl.opengl.GL11.glClearColor;

import com.mcraft.player.Player;
import com.mcraft.player.Raycast;
import com.mcraft.render.Camera;
import com.mcraft.render.Shader;
import com.mcraft.render.TextureAtlas;
import com.mcraft.ui.InventoryScreen;
import com.mcraft.world.DayNightCycle;
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

    private InventoryScreen inventoryScreen;
    private boolean         inventoryOpen   = false;
    private boolean         prevEKeyDown    = false;
    private boolean         leftWasDown     = false;
    private boolean         rightWasDown    = false;
    private float[]         ortho2D;

    private final DayNightCycle dayNight = new DayNightCycle();

    public GameLoop(Window window) {
        this.window = window;
        ortho2D = Camera.ortho(window.getWidth(), window.getHeight());

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

        atlas = TextureAtlas.generateProcedural();

        hud = new com.mcraft.ui.HUD(
            window.getWidth(), window.getHeight(),
            player.getInventory(), hudShader, atlas
        );

        inventoryScreen = new InventoryScreen(
            window.getWidth(), window.getHeight(),
            player.getInventory(),
            hudShader,    
            atlas,
            ortho2D
        );

        glfwSetMouseButtonCallback(window.getHandle(), (win, button, action, mods) -> {
            if (!inventoryOpen) return;
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                double[] cx = new double[1], cy = new double[1];
                glfwGetCursorPos(win, cx, cy);
                boolean consumed = inventoryScreen.onClick((int) cx[0], (int) cy[0]);
                if (!consumed) {
                    closeInventory();
                }
            }
        });
        lastTime = glfwGetTime();
    }

    private void openInventory() {
        inventoryOpen = true;
        glfwSetInputMode(window.getHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        int cx = window.getWidth()  / 2;
        int cy = window.getHeight() / 2;
        glfwSetCursorPos(window.getHandle(), cx, cy);
        inventoryScreen.updateMouse(cx, cy);
    }

    private void closeInventory() {
        inventoryOpen = false;
        inventoryScreen.onClose();
        glfwSetInputMode(window.getHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        input.resetFirstMouse();
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
            if (inventoryOpen) inventoryScreen.render();
        }

        cleanup();
    }

    private void update(float dt) {

        boolean eDown = input.isKeyDown(GLFW_KEY_E);
        if (eDown && !prevEKeyDown) {             
            if (inventoryOpen) {
                closeInventory();
            } else {
                openInventory();
            }
        }
        
        prevEKeyDown = eDown;

        if (inventoryOpen) {
            double[] cx = new double[1];
            double[] cy = new double[1];
            glfwGetCursorPos(window.getHandle(), cx, cy);

            inventoryScreen.updateMouse((int) cx[0], (int) cy[0]);
            return;
        }

        dayNight.update(dt);
        
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

        float[] fog = dayNight.getFogColor();
        float[] sky = dayNight.getSkyColor();

        glClearColor(sky[0], sky[1], sky[2], 1.0f);

        blockShader.setFloat("uAmbientLight", dayNight.getAmbientLight());
        blockShader.setFloat("uFogColor[0]", fog[0]); 
        blockShader.setFloat("uFogColor[1]", fog[1]);
        blockShader.setFloat("uFogColor[2]", fog[2]);

        blockShader.setVec3("uFogColor", fog[0], fog[1], fog[2]);

        float[] proj = Camera.perspective(70f,
            (float) window.getWidth() / window.getHeight(), 0.05f, 500f);

        blockShader.setMatrix4("uProjection", proj);
        blockShader.setMatrix4("uView",       camera.getViewMatrix());

        atlas.bind(0);
        blockShader.setInt("uTexture", 0);

        world.render(blockShader, camera);
    }

    private void cleanup() {
        if (inventoryOpen) {
            inventoryScreen.onClose();
        }
        blockShader.delete();
        hudShader.delete();
        atlas.delete();
    }
}