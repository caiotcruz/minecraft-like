package com.mcraft.core;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F2;
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

import com.mcraft.audio.SoundEvent;
import com.mcraft.audio.SoundManager;
import com.mcraft.entity.MobManager;
import com.mcraft.player.Player;
import com.mcraft.player.Raycast;
import com.mcraft.render.Camera;
import com.mcraft.render.Shader;
import com.mcraft.render.SkyRenderer;
import com.mcraft.render.TextureAtlas;
import com.mcraft.ui.InventoryScreen;
import com.mcraft.world.Block;
import com.mcraft.world.DayNightCycle;
import com.mcraft.world.World;
import com.mcraft.world.WorldIO;

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
    private final Shader       mobShader;
    private final SkyRenderer skyRenderer;
    private final Shader      skyShader;
    private final TextureAtlas atlas;
    private final SoundManager sound = new SoundManager();

    private final com.mcraft.ui.HUD hud;

    private float  accumulator = 0f;
    private double lastTime;

    private float worldGenTimer    = 0f;
    private static final float WORLD_GEN_INTERVAL = 0.35f;

    private InventoryScreen inventoryScreen;
    private boolean         inventoryOpen   = false;
    private boolean         prevEKeyDown    = false;
    private boolean         leftWasDown     = false;
    private boolean         rightWasDown    = false;
    private float[]         ortho2D;

    private int   breakX = -1, breakY = -1, breakZ = -1;
    private float breakElapsed  = 0f;
    private float breakDuration = 0f;

    private int musicSource = -1;

    private float stepTimer = 0f;
    private static final float STEP_INTERVAL = 0.45f;

    private final DayNightCycle dayNight = new DayNightCycle();

    private final MobManager mobs = new MobManager();


    public GameLoop( Window window, WorldIO worldIO, long seed, float spawnX, float spawnY, float spawnZ) {
        this.window = window;
        ortho2D = Camera.ortho(window.getWidth(), window.getHeight());

        input = new Input(window.getHandle());

        glfwSetInputMode(window.getHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        
        world  = new World(seed, worldIO);

        player = new Player(
            spawnX,
            spawnY,
            spawnZ,
            world
        );

        camera = player.getCamera();

        glfwSetScrollCallback(window.getHandle(), (win, xOff, yOff) ->
            player.getInventory().scrollHotbar((int) -yOff)
        );

        world.generateAround(player.getX(), player.getZ());

        blockShader = new Shader("block.vert", "block.frag");
        hudShader   = new Shader("hud.vert",   "hud.frag");
        skyShader   = new Shader("sky.vert", "sky.frag");
        mobShader = new Shader("mob.vert", "mob.frag");
        skyRenderer = new SkyRenderer();

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
        sound.init();
        musicSource = sound.playLoop(SoundEvent.MUSIC_DAY, 0.75f);
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
        skyRenderer.update(dt);

        float dx = 0, dz = 0;
        if (input.isKeyDown(GLFW_KEY_W)) dz -= 1;
        if (input.isKeyDown(GLFW_KEY_S)) dz += 1;
        if (input.isKeyDown(GLFW_KEY_A)) dx -= 1;
        if (input.isKeyDown(GLFW_KEY_D)) dx += 1;
        boolean jump = input.isKeyDown(GLFW_KEY_SPACE);

        if (input.isKeyDown(GLFW_KEY_F2)) dayNight.setTime(0.8f); ;

        float mdx = input.consumeMouseDX();
        float mdy = input.consumeMouseDY();
        camera.rotate(mdx * MOUSE_SENS, mdy * MOUSE_SENS);

        player.update(dx, dz, jump, dt);

        
        worldGenTimer += dt;
        if (worldGenTimer >= WORLD_GEN_INTERVAL) {
            worldGenTimer = 0f;
            world.generateAround(player.getX(), player.getZ());
        }

        boolean moving = dx != 0 || dz != 0;

        if (moving) {
            stepTimer -= dt;

            if (stepTimer <= 0f) {

                int bx = (int)Math.floor(player.getX());
                int by = (int)Math.floor(player.getY() - 1);
                int bz = (int)Math.floor(player.getZ());

                Block ground = world.getBlock(bx, by, bz);

                sound.playRandom(
                    sound.stepSound(ground),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    0.35f
                );

                stepTimer = STEP_INTERVAL;
            }
        }


        for (int k = 0; k < 9; k++) {
            if (input.isKeyDown(GLFW_KEY_1 + k)) {
                player.getInventory().setSelectedSlot(k);
            }
        }

        float[] front = camera.getFront();
        float[] up    = camera.getUp();

        sound.updateListener(
            camera.getX(), camera.getY(), camera.getZ(),
            front[0], front[1], front[2],
            up[0],    up[1],    up[2]
        );

        Raycast.HitResult hit = Raycast.cast(
            camera.getX(), camera.getY(), camera.getZ(),
            front[0], front[1], front[2],
            REACH, world
        );

        if (hit.hit) {
            Block target = world.getBlock(hit.blockX, hit.blockY, hit.blockZ);
            boolean leftDown = input.isMouseDown(GLFW_MOUSE_BUTTON_LEFT);

            if (leftDown && target.breakTime > 0f) {
                if (hit.blockX != breakX || hit.blockY != breakY || hit.blockZ != breakZ) {
                    breakX        = hit.blockX;
                    breakY        = hit.blockY;
                    breakZ        = hit.blockZ;
                    breakElapsed  = 0f;
                    breakDuration = target.breakTime;
                }

                breakElapsed += dt;

                if (breakElapsed >= breakDuration) {
                    world.setBlock(breakX, breakY, breakZ, 0);
                    player.getInventory().addItem(target.id, 1);
                    breakElapsed = 0f; breakDuration = 0f;
                    breakX = breakY = breakZ = -1;
                }
            } else {
                breakElapsed = 0f;
                if (!leftDown) breakX = breakY = breakZ = -1;
            }

            boolean rightDown = input.isMouseDown(GLFW_MOUSE_BUTTON_RIGHT);
            if (rightDown && !rightWasDown) {
                int blockId = player.getInventory().getSelectedBlockId();
                if (blockId != 0) {
                    world.setBlock(hit.prevX, hit.prevY, hit.prevZ, blockId);
                    player.getInventory().consumeSelected(1);
                }
            }
            rightWasDown = rightDown;
            leftWasDown  = leftDown;
        } else {
            breakElapsed = 0f; breakDuration = 0f;
            breakX = breakY = breakZ = -1;
            leftWasDown = rightWasDown = false;
        }

        float breakProgress = (breakDuration > 0f)
            ? Math.min(1f, breakElapsed / breakDuration)
            : 0f;
        hud.setBreakProgress(breakProgress);

        mobs.update(dt, world, player.getX(), player.getY(), player.getZ(), dayNight.isNight());

        if (input.isKeyDown(GLFW_KEY_ESCAPE)) {
            glfwSetWindowShouldClose(window.getHandle(), true);
        }
    }

    private void render3D() {
        float[] sky = dayNight.getSkyColor();
        float[] fog = dayNight.getFogColor();
        glClearColor(sky[0], sky[1], sky[2], 1f);

        float[] proj = Camera.perspective(70f,
            (float)window.getWidth() / window.getHeight(), 0.05f, 900f);
        float[] view = camera.getViewMatrix();

        skyRenderer.render(camera, dayNight, skyShader, proj, view);

        blockShader.use();
        blockShader.setMatrix4("uProjection", proj);
        blockShader.setMatrix4("uView",       view);
        blockShader.setFloat  ("uAmbientLight", dayNight.getAmbientLight());
        blockShader.setVec3   ("uFogColor", fog[0], fog[1], fog[2]);
        atlas.bind(0);
        blockShader.setInt("uTexture", 0);
        world.render(blockShader, camera);

        mobs.render( mobShader, proj, view, dayNight.getAmbientLight(), fog);
    }

    private void cleanup() {
        if (inventoryOpen) {
            inventoryScreen.onClose();
        }
        blockShader.delete();
        hudShader.delete();
        skyRenderer.delete();
        skyShader.delete();
        mobShader.delete();
        atlas.delete();
        sound.cleanup();
    }

    public World getWorld() {
        return world;
    }

    public Player getPlayer() {
        return player;
    }

    public DayNightCycle getDayNight() {
        return dayNight;
    }
}