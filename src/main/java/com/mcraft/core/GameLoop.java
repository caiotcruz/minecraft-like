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

import com.mcraft.audio.SoundEvent;
import com.mcraft.audio.SoundManager;
import com.mcraft.entity.Mob;
import com.mcraft.entity.MobManager;
import com.mcraft.player.Player;
import com.mcraft.player.Raycast;
import com.mcraft.render.BreakOverlay;
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

    private static final float REACH      = 5.0f;
    private static final float MOUSE_SENS = 0.0015f;

    private static final float PHYSICS_HZ   = 60f;
    private static final float PHYSICS_STEP = 1f / PHYSICS_HZ;

    private static final float TICK_HZ   = 20f;
    private static final float TICK_STEP = 1f / TICK_HZ;

    private float physicsAccumulator = 0f;
    private float tickAccumulator    = 0f;

    private final Window       window;
    private final Input        input;
    private final World        world;
    private final Player       player;
    private final Camera       camera;
    private final Shader       blockShader;
    private final Shader       hudShader;
    private final Shader       mobShader;
    private final Shader      skyShader;
    private final SkyRenderer skyRenderer;
    private BreakOverlay breakOverlay;

    private float hitSoundTimer    = 0f;
    private static final float HIT_SOUND_INTERVAL = 0.30f;

    private final TextureAtlas atlas;
    private final SoundManager sound = new SoundManager();

    private final com.mcraft.ui.HUD hud;

    private double lastTime;

    private float worldGenTimer    = 0f;
    private static final float WORLD_GEN_INTERVAL = 0.35f;

    private float unloadTimer = 0f;
    private static final float UNLOAD_INTERVAL = 8f;

    private InventoryScreen inventoryScreen;
    private boolean         inventoryOpen   = false;
    private boolean         prevEKeyDown    = false;

    @SuppressWarnings("unused")
    private boolean         leftWasDown     = false;
    
     private boolean         rightWasDown    = false;
    private float[]         ortho2D;

    private int   breakX = -1, breakY = -1, breakZ = -1;
    private float breakElapsed  = 0f;
    private float breakDuration = 0f;

    @SuppressWarnings("unused")
    private int musicSource = -1;

    private float stepTimer = 0f;
    private static final float STEP_INTERVAL = 0.45f;

    private final DayNightCycle dayNight = new DayNightCycle();

    private final MobManager mobs;


    public GameLoop( Window window, WorldIO worldIO, long seed, float spawnX, float spawnY, float spawnZ) {
        this.window = window;
        ortho2D = Camera.ortho(window.getWidth(), window.getHeight());

        input = new Input(window.getHandle());

        glfwSetInputMode(window.getHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        
        world  = new World(seed, worldIO);

        player = new Player(
            spawnX,
            spawnY +1,
            spawnZ,
            world
        );

        camera = player.getCamera();

        glfwSetScrollCallback(window.getHandle(), (win, xOff, yOff) ->
            player.getInventory().scrollHotbar((int) -yOff)
        );

        world.generateInitialArea(player.getX(), player.getZ());

        blockShader = new Shader("block.vert", "block.frag");
        hudShader   = new Shader("hud.vert",   "hud.frag");
        skyShader   = new Shader("sky.vert", "sky.frag");
        mobShader = new Shader("mob.vert", "mob.frag");
        skyRenderer = new SkyRenderer();
        breakOverlay = new BreakOverlay();

        atlas = TextureAtlas.generateProcedural();

        hud = new com.mcraft.ui.HUD(
            window.getWidth(), window.getHeight(),
            player.getInventory(), hudShader, atlas
        );

        mobs = new MobManager(drops -> {
            for (int[] drop : drops) {
                if (drop[1] > 0) {
                    player.getInventory().addItem(drop[0], drop[1]);
                }
            }
        });

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

            if (dt > 0.1f) dt = 0.1f;

            input.prepareFrame();
            window.swapAndPoll();
            input.endFrame();

            handleCameraInput();

            physicsAccumulator += dt;
            while (physicsAccumulator >= PHYSICS_STEP) {
                updatePhysics(PHYSICS_STEP);
                physicsAccumulator -= PHYSICS_STEP;
            }

            tickAccumulator += dt;
            while (tickAccumulator >= TICK_STEP) {
                gameTick(TICK_STEP);
                tickAccumulator -= TICK_STEP;
            }

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            render3D();
            hud.render();

            if (inventoryOpen) {
                inventoryScreen.render();
            }
        }

        cleanup();
    }

    private void gameTick(float dt) {
        dayNight.update(dt);
        skyRenderer.update(dt);

        mobs.update(dt, world, player.getX(), player.getY(), player.getZ(), dayNight.isNight());

        worldGenTimer += dt;
        if (worldGenTimer >= WORLD_GEN_INTERVAL) {
            worldGenTimer = 0f;
            world.generateAround(player.getX(), player.getZ());
        }

        world.integrateReady();

        unloadTimer += dt;
        if (unloadTimer >= UNLOAD_INTERVAL) {
            unloadTimer = 0f;
            world.unloadDistant(player.getX(), player.getZ(), world.getWorldIO());
        }

        handleBlockBreaking(dt);

        for (int k = 0; k < 9; k++) {
            if (input.isKeyDown(GLFW_KEY_1 + k)) {
                player.getInventory().setSelectedSlot(k);
            }
        }

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
        world.render(blockShader, camera, proj, view);
        if (breakX != -1 && hud.getBreakProgress() > 0.01f) {
            breakOverlay.render(breakX, breakY, breakZ, hud.getBreakProgress(), proj, view);
        }

        mobs.render( mobShader, proj, view, dayNight.getAmbientLight(), fog);
    }

    private void updatePhysics(float dt) {
        
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

        
        float dx = 0, dz = 0;
        if (input.isKeyDown(GLFW_KEY_W)) dz -= 1;
        if (input.isKeyDown(GLFW_KEY_S)) dz += 1;
        if (input.isKeyDown(GLFW_KEY_A)) dx -= 1;
        if (input.isKeyDown(GLFW_KEY_D)) dx += 1;
        
        boolean jump = input.isKeyDown(GLFW_KEY_SPACE);

        player.update(dx, dz, jump, dt);

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
                    0.15f
                );

                stepTimer = STEP_INTERVAL;
            }
        }

    }

    private void handleCameraInput() {

        if (inventoryOpen) {

            double[] cx = new double[1];
            double[] cy = new double[1];

            glfwGetCursorPos(window.getHandle(), cx, cy);

            inventoryScreen.updateMouse( (int) cx[0], (int) cy[0] );

            return;
        }

        float mdx = input.consumeMouseDX();
        float mdy = input.consumeMouseDY();

        camera.rotate( mdx * MOUSE_SENS, mdy * MOUSE_SENS);

        float[] front = camera.getFront();
        float[] up    = camera.getUp();

        sound.updateListener( camera.getX(), camera.getY(), camera.getZ(), front[0], front[1], front[2], up[0], up[1], up[2] );
    }

    private void handleBlockBreaking(float dt){

        float[] front = camera.getFront();
        boolean leftDown = input.isMouseDown(GLFW_MOUSE_BUTTON_LEFT);
        boolean leftJustPressed = leftDown && !leftWasDown;

        Raycast.HitResult hit = Raycast.cast(
            camera.getX(), camera.getY(), camera.getZ(),
            front[0], front[1], front[2],
            REACH, world
        );

        if (leftJustPressed && !hit.hit) {
            attackNearestMob();
        }

        if (hit.hit) {
            Block target = world.getBlock(hit.blockX, hit.blockY, hit.blockZ);

            if (leftDown && target.breakTime > 0f) {
                if (hit.blockX != breakX || hit.blockY != breakY || hit.blockZ != breakZ) {
                    breakX        = hit.blockX;
                    breakY        = hit.blockY;
                    breakZ        = hit.blockZ;
                    breakElapsed  = 0f;
                    breakDuration = target.breakTime;

                    sound.playRandom(sound.hitSound(target), breakX + 0.5f, breakY + 0.5f, breakZ + 0.5f, 0.4f);
                    hitSoundTimer = 0f;

                }

                breakElapsed += dt;

                if (breakElapsed >= breakDuration) {
                    world.setBlock(breakX, breakY, breakZ, 0);
                    player.getInventory().addItem(target.id, 1);
                    breakElapsed = 0f; breakDuration = 0f;
                    breakX = breakY = breakZ = -1;
                    sound.playRandom(sound.breakSound(target), breakX + 0.5f, breakY + 0.5f, breakZ + 0.5f, 1f);
                }

                hitSoundTimer += dt;
                if (hitSoundTimer >= HIT_SOUND_INTERVAL) {
                    hitSoundTimer = 0f;
                    sound.playRandom(sound.hitSound(target), breakX + 0.5f, breakY + 0.5f, breakZ + 0.5f, 0.4f);
                }

            } else {
                breakElapsed = 0f;
                hitSoundTimer = 0f; 
                if (!leftDown) breakX = breakY = breakZ = -1;
            }

            boolean rightDown = input.isMouseDown(GLFW_MOUSE_BUTTON_RIGHT);
            if (rightDown && !rightWasDown) {
                int blockId = player.getInventory().getSelectedBlockId();
                if (blockId != 0) {
                    world.setBlock(hit.prevX, hit.prevY, hit.prevZ, blockId);
                    player.getInventory().consumeSelected(1);
                    sound.playRandom(sound.placeSound(target), hit.prevX + 0.5f, hit.prevY + 0.5f, hit.prevZ + 0.5f, 0.6f);
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
    }

    private void attackNearestMob() {
        final float REACH = 3.5f;
        float cx = camera.getX(), cy = camera.getY(), cz = camera.getZ();

        Mob nearest = null;
        float nearestDist = Float.MAX_VALUE;

        for (Mob mob : mobs.getMobs()) {
            float dx = mob.getX() - cx;
            float dy = mob.getY() + mob.getHeight()/2f - cy;
            float dz = mob.getZ() - cz;
            float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist < REACH && dist < nearestDist) {
                nearestDist = dist;
                nearest = mob;
            }
        }

        if (nearest != null) {
            nearest.damage(5);
        }
    }

    private void cleanup() {
        world.shutdown();
        if (inventoryOpen) {
            inventoryScreen.onClose();
        }
        blockShader.delete();
        world.saveAll(world.getWorldIO());
        world.deleteAllMeshes();
        hudShader.delete();
        skyRenderer.delete();
        skyShader.delete();
        mobShader.delete();
        breakOverlay.delete();
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