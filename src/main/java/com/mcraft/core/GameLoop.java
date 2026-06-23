package com.mcraft.core;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F2;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F3;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F4;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F5;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F6;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F7;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

import java.util.List;

import com.mcraft.audio.SoundEvent;
import com.mcraft.audio.SoundManager;
import com.mcraft.entity.Mob;
import com.mcraft.entity.MobManager;
import com.mcraft.player.Player;
import com.mcraft.player.Raycast;
import com.mcraft.render.BreakOverlay;
import com.mcraft.render.Camera;
import com.mcraft.render.LightScheduler;
import com.mcraft.render.Shader;
import com.mcraft.render.SkyRenderer;
import com.mcraft.render.TextureAtlas;
import com.mcraft.ui.ChestScreen;
import com.mcraft.ui.CraftingScreen;
import com.mcraft.ui.FurnaceScreen;
import com.mcraft.ui.Inventory;
import com.mcraft.ui.InventoryScreen;
import com.mcraft.world.Biome;
import com.mcraft.world.Block;
import com.mcraft.world.Chunk;
import com.mcraft.world.DayNightCycle;
import com.mcraft.world.FurnaceState;
import com.mcraft.world.WeatherSystem;
import com.mcraft.world.WeatherType;
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

    private Biome currentBiome = Biome.PLAINS;

    private InventoryScreen inventoryScreen;
    private boolean         inventoryOpen   = false;

    private CraftingScreen craftingScreen;
    private boolean craftingOpen = false;

    private ChestScreen chestScreen;
    private boolean     chestOpen = false;

    private FurnaceScreen furnaceScreen;
    private boolean       furnaceOpen = false;

    private boolean         prevEKeyDown    = false;

    private boolean         leftWasDown     = false;
    
    private boolean         rightWasDown    = false;
    private float[]         ortho2D;

    private int   breakX = -1, breakY = -1, breakZ = -1;
    private float breakElapsed  = 0f;
    private float breakDuration = 0f;

    private float deathTimer = 0f;
    private static final float RESPAWN_DELAY = 3.0f;

    @SuppressWarnings("unused")
    private int musicSource = -1;

    private float stepTimer = 0f;
    private static final float STEP_INTERVAL = 0.45f;

    private final DayNightCycle dayNight = new DayNightCycle();
    private final WeatherSystem weather = new WeatherSystem();

    private final MobManager mobs;

    private LightScheduler lightScheduler;

    private long lastProfile = 0;
    private long lightTime = 0;
    private long renderTime = 0;
    private int  profileFrames = 0;

    public GameLoop( Window window, WorldIO worldIO, long seed, float spawnX, float spawnY, float spawnZ) {
        this.window = window;
        ortho2D = Camera.ortho(window.getWidth(), window.getHeight());

        input = new Input(window.getHandle());

        glfwSetInputMode(window.getHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        
        world  = new World(seed, worldIO);
        if (spawnY == 90){
            spawnY = world.getSurfaceY(spawnX, spawnZ);
        }

        lightScheduler = new LightScheduler(world);

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
            player.getInventory(), hudShader, atlas, player
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

        craftingScreen = new CraftingScreen(
            window.getWidth(), window.getHeight(),
            player.getInventory(), hudShader, atlas, ortho2D
        );

        chestScreen = new ChestScreen(
            window.getWidth(), window.getHeight(),
            player.getInventory(), hudShader, atlas, ortho2D
        );

        furnaceScreen = new FurnaceScreen(
            window.getWidth(), window.getHeight(),
            player.getInventory(), hudShader, atlas, ortho2D
        );        

        glfwSetMouseButtonCallback(window.getHandle(), (win, button, action, mods) -> {

            if (!inventoryOpen && !craftingOpen && !chestOpen && !furnaceOpen) return;

            if (action == GLFW_PRESS) {

                boolean isRightClick = (button == GLFW_MOUSE_BUTTON_RIGHT);
                boolean isLeftClick  = (button == GLFW_MOUSE_BUTTON_LEFT);

                if (!isLeftClick && !isRightClick) return;

                double[] cx = new double[1];
                double[] cy = new double[1];

                glfwGetCursorPos(win, cx, cy);

                boolean consumed = false;

                if (craftingOpen) {
                    consumed = craftingScreen.onClick(
                        (int) cx[0],
                        (int) cy[0],
                        isRightClick
                    );
                } else if (inventoryOpen) {
                    consumed = inventoryScreen.onClick(
                        (int) cx[0],
                        (int) cy[0],
                        isRightClick
                    );
                } else if (chestOpen) {
                    consumed = chestScreen.onClick(
                        (int) cx[0],
                        (int) cy[0],
                        isRightClick
                    );
                } else if (furnaceOpen) {
                    consumed = furnaceScreen.onClick(
                        (int) cx[0],
                        (int) cy[0],
                        isRightClick
                    );
                }

                if (!consumed) {
                    closeInventory();
                    closeCrafting();
                    closeChest();
                    closeFurnace();
                }
            }
        });
        lastTime = glfwGetTime();
        sound.init();
        musicSource = sound.playLoop(SoundEvent.MUSIC_DAY, 0.75f);
    }

    private void openInventory() {
        craftingOpen = false;
        chestOpen = false;
        furnaceOpen = false;
        inventoryOpen = true;
        glfwSetInputMode(window.getHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        int cx = window.getWidth()  / 2;
        int cy = window.getHeight() / 2;
        glfwSetCursorPos(window.getHandle(), cx, cy);
        inventoryScreen.updateMouse(cx, cy);
    }

    private void openCrafting(){
        inventoryOpen = false;
        chestOpen = false;
        furnaceOpen = false;
        craftingOpen = true;

        glfwSetInputMode(window.getHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        int cx = window.getWidth()  / 2;
        int cy = window.getHeight() / 2;
        glfwSetCursorPos(window.getHandle(), cx, cy);
        craftingScreen.updateMouse(cx, cy);
    }

    private void openChest() {
        inventoryOpen = false;
        craftingOpen = false;
        furnaceOpen = false;
        chestOpen = true;

        glfwSetInputMode(window.getHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        int cx = window.getWidth()/2;
        int cy = window.getHeight()/2;
        glfwSetCursorPos(window.getHandle(), cx, cy);
        chestScreen.updateMouse(cx, cy);
    }

    private void openFurnace() {
        inventoryOpen = false;
        chestOpen = false;
        craftingOpen = false;
        furnaceOpen = true;

        glfwSetInputMode(window.getHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        int cx = window.getWidth()  / 2;
        int cy = window.getHeight() / 2;
        glfwSetCursorPos(window.getHandle(), cx, cy);
        furnaceScreen.updateMouse(cx, cy);
    }

    private void closeInventory() {
        inventoryOpen = false;
        inventoryScreen.onClose();
        glfwSetInputMode(window.getHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        input.resetFirstMouse();
    }

    private void closeCrafting(){
        craftingOpen = false;
        craftingScreen.onClose();
        glfwSetInputMode(window.getHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        input.resetFirstMouse();
    }

    private void closeChest() {
        chestOpen = false;
        chestScreen.onClose();
        glfwSetInputMode(window.getHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        input.resetFirstMouse();
    }

    private void closeFurnace() {
        furnaceOpen = false;
        furnaceScreen.onClose();
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
            int eyeX = (int) Math.floor(camera.getX());
            int eyeY = (int) Math.floor(camera.getY());
            int eyeZ = (int) Math.floor(camera.getZ());
            boolean underwater = (world.getBlock(eyeX, eyeY, eyeZ).id == Block.WATER.id);
            hud.setUnderwater(underwater);
            player.setHeadInWater(underwater);
            player.setInWater(underwater);
            hud.render(dt);

            if (craftingOpen){
                craftingScreen.render();
            } 
            if (inventoryOpen) {
                inventoryScreen.render();
            }
            if (chestOpen){
                chestScreen.render();
            }
            if (furnaceOpen){
                furnaceScreen.render();
            }
        }

        cleanup();
    }

    private void gameTick(float dt) {
        
        if (player.isDead()) {
            deathTimer += dt;
            float alpha = Math.min(1.0f, deathTimer / 1.5f);
            hud.setDeathAlpha(alpha);

            if (deathTimer >= 1.5f && input.isKeyDown(GLFW_KEY_R)) {
                doRespawn();
            }
            if (deathTimer >= RESPAWN_DELAY) {
                doRespawn();
            }
            return; 
        }

        lightScheduler.flushResults(4);

        hud.setDeathAlpha(Math.max(0f, hud.getDeathAlpha() - dt * 2f));

        dayNight.update(dt);
        skyRenderer.update(dt);

        weather.update(TICK_STEP, player.getX(), player.getY(), player.getZ(), currentBiome);
        if (weather.getCurrent() == WeatherType.RAIN) {
            hud.setRainIntensity(weather.getIntensity());
        } else {
            hud.setRainIntensity(0f);
        }

        hud.setGameTime(dayNight.getTime());
        hud.setDay(dayNight.getDay());

        if (player.getHunger() >= player.getMaxHunger()/2){
            player.tickRegen(TICK_STEP);
        }

        mobs.update(dt, world, player.getX(), player.getY(), player.getZ(), dayNight);
        applyMobDamage(dt);
        
        currentBiome = world.getWorldGen().getBiome(player.getX(), player.getZ());

        worldGenTimer += dt;
        if (worldGenTimer >= WORLD_GEN_INTERVAL) {
            worldGenTimer = 0f;
            world.generateAround(player.getX(), player.getZ());
        }

        world.integrateReady();

        float playerCX = player.getX() / Chunk.SIZE;
        float playerCZ = player.getZ() / Chunk.SIZE;

        int budget = 4;
        int maxDistance = World.RENDER_DISTANCE + 2;
        float maxDistanceSq = maxDistance * maxDistance;

        List<Chunk> activeChunks = world.getLoadedChunksList();

        for (int i = 0; i < activeChunks.size(); i++) {
            if (budget <= 0) break;

            Chunk chunk = activeChunks.get(i);
            
            if (!chunk.isLightDirty() || chunk.isLightPending()) continue;

            float dx = chunk.getChunkX() - playerCX;
            float dz = chunk.getChunkZ() - playerCZ;
            float distSq = dx * dx + dz * dz;

            if (distSq > maxDistanceSq) continue;

            chunk.setLightDirty(false);
            lightScheduler.submit(chunk);
            budget--;
        }

        unloadTimer += dt;
        if (unloadTimer >= UNLOAD_INTERVAL) {
            unloadTimer = 0f;
            world.unloadDistant(player.getX(), player.getZ(), world.getWorldIO());
        }

        handlePlayerInteraction(dt);

        for (FurnaceState fs : world.getFurnaceStates().values()) {
            fs.tick(TICK_STEP);
        }

        for (int k = 0; k < 9; k++) {
            if (input.isKeyDown(GLFW_KEY_1 + k)) {
                player.getInventory().setSelectedSlot(k);
            }
        }

        if (input.isKeyDown(GLFW_KEY_ESCAPE)) {
            if (furnaceOpen)   { closeFurnace();       return; }
            if (chestOpen)     { closeChest();         return; }
            if (craftingOpen)  { closeCrafting();      return; }
            if (inventoryOpen) { closeInventory();     return; }
            glfwSetWindowShouldClose(window.getHandle(), true);
        }
    }

    private void render3D() {
        float[] sky = dayNight.getSkyColor();
        float[] fog = dayNight.getFogColor();
        
        float rainIntensity =
        (weather.getCurrent() == WeatherType.RAIN ||
        weather.getIntensity() > 0.05f)
            ? weather.getIntensity()
            : 0f;

        float grayBlend = rainIntensity * 0.68f;
        float grayLevel = 0.38f;

        float finalSkyR = sky[0] * (1f - grayBlend) + grayLevel * grayBlend;
        float finalSkyG = sky[1] * (1f - grayBlend) + grayLevel * grayBlend;
        float finalSkyB = sky[2] * (1f - grayBlend) + (grayLevel + 0.04f) * grayBlend;

        glClearColor(finalSkyR, finalSkyG, finalSkyB, 1f);

        float finalFogR = fog[0] * (1f - grayBlend) + grayLevel * grayBlend;
        float finalFogG = fog[1] * (1f - grayBlend) + grayLevel * grayBlend;
        float finalFogB = fog[2] * (1f - grayBlend) + (grayLevel + 0.05f) * grayBlend;

        float[] proj = Camera.perspective(70f,
            (float)window.getWidth() / window.getHeight(), 0.05f, 900f);
        float[] view = camera.getViewMatrix();

        skyRenderer.render(camera, dayNight, skyShader, proj, view);
        weather.render(camera, skyShader, proj, view);

        long t0 = System.nanoTime();
        if (lightScheduler != null) {
            lightScheduler.flushResults(4);
        }
        lightTime += System.nanoTime() - t0;

        blockShader.use();
        blockShader.setMatrix4("uProjection", proj);
        blockShader.setMatrix4("uView",       view);
        blockShader.setFloat  ("uAmbientLight", dayNight.getAmbientLight());
        blockShader.setVec3   ("uFogColor", finalFogR, finalFogG, finalFogB);
        atlas.bind(0);
        blockShader.setInt("uTexture", 0);

        long t1 = System.nanoTime();
        world.render(blockShader, camera, proj, view);
        if (breakX != -1 && hud.getBreakProgress() > 0.01f) {
            breakOverlay.render(breakX, breakY, breakZ, hud.getBreakProgress(), proj, view);
        }
        renderTime += System.nanoTime() - t1;

        mobs.render( mobShader, proj, view, dayNight.getAmbientLight(), fog);

        profileFrames++;
        long now = System.currentTimeMillis();
        
        if (now - lastProfile > 5000) {
            if (profileFrames > 0) {
                double avgLightMs  = (lightTime  / 1e6) / profileFrames;
                double avgRenderMs = (renderTime / 1e6) / profileFrames;
                
                System.out.printf("[Perf] Frames: %d | Light Flush: %.3fms | World Render: %.3fms%n",
                    profileFrames, avgLightMs, avgRenderMs);
            }
            
            lightTime = 0;
            renderTime = 0;
            profileFrames = 0;
            lastProfile = now;
        }
    }

    private void updatePhysics(float dt) {

        if (player.isDead()) return;

        handleInventoryToggle();

        if (inventoryOpen) {
            double[] cx = new double[1];
            double[] cy = new double[1];

            glfwGetCursorPos(window.getHandle(), cx, cy);

            inventoryScreen.updateMouse((int) cx[0], (int) cy[0]);
        }

        if (craftingOpen) {
            double[] cx = new double[1];
            double[] cy = new double[1];

            glfwGetCursorPos(window.getHandle(), cx, cy);

            craftingScreen.updateMouse((int) cx[0], (int) cy[0]);
        }

        if (chestOpen) {
            double[] cx = new double[1];
            double[] cy = new double[1];

            glfwGetCursorPos(window.getHandle(), cx, cy);

            chestScreen.updateMouse((int) cx[0], (int) cy[0]);

        }

        if (furnaceOpen) {
            double[] cx = new double[1];
            double[] cy = new double[1];

            glfwGetCursorPos(window.getHandle(), cx, cy);

            furnaceScreen.updateMouse((int) cx[0], (int) cy[0]);
        }

        float dx = 0f;
        float dz = 0f;
        boolean jump = false;
        boolean dive = false;

        if (!inventoryOpen && !craftingOpen && !chestOpen && !furnaceOpen) {

            if (input.isKeyDown(GLFW_KEY_W)) dz -= 1;
            if (input.isKeyDown(GLFW_KEY_S)) dz += 1;
            if (input.isKeyDown(GLFW_KEY_A)) dx -= 1;
            if (input.isKeyDown(GLFW_KEY_D)) dx += 1;

            jump = input.isKeyDown(GLFW_KEY_SPACE);
            dive = !inventoryOpen && input.isKeyDown(GLFW_KEY_LEFT_SHIFT) && player.isInWater();

            //Colocar Noite
            if (input.isKeyDown(GLFW_KEY_F2)) {
                dayNight.setTime(0.8f);
            }
            //Colocar Dia
            if (input.isKeyDown(GLFW_KEY_F7)) {
                dayNight.setTime(0.4f);
            }

            //Spawnar Zumbi
            if (input.isKeyDown(GLFW_KEY_F3)) {

                float[] front = camera.getFront();

                float spawnX = player.getX() + front[0] * 3f;
                float spawnY = player.getY();
                float spawnZ = player.getZ() + front[2] * 3f;

                mobs.getMobs().add(
                    new Mob(
                        Mob.Type.ZOMBIE,
                        spawnX,
                        spawnY,
                        spawnZ
                    )
                );
            }

            //spawn Cow
            if (input.isKeyDown(GLFW_KEY_F6)) {

                float[] front = camera.getFront();

                float spawnX = player.getX() + front[0] * 3f;
                float spawnY = player.getY();
                float spawnZ = player.getZ() + front[2] * 3f;

                mobs.getMobs().add(
                    new Mob(
                        Mob.Type.COW,
                        spawnX,
                        spawnY,
                        spawnZ
                    )
                );
            }

            // Alterar Weather
            if (input.isKeyDown(GLFW_KEY_F4)) {
                WeatherType biomeWeather = WeatherType.forBiome(currentBiome);

                if (weather.getCurrent() == biomeWeather) {
                    weather.setCurrent(WeatherType.CLEAR);
                } else {
                    weather.setCurrent(biomeWeather);
                }
            }

            //Diminuir a fome
            if (input.isKeyDown(GLFW_KEY_F5)) {
                player.setHunger(player.getMaxHunger()/4);
            }
        }

        boolean ctrlDown   = input.isKeyDown(GLFW_KEY_LEFT_CONTROL) && !inventoryOpen;
        boolean isMoving = dx != 0 || dz != 0;

        boolean canSprint = ctrlDown && isMoving && player.getHunger() > 7f;

        player.setSprinting( !inventoryOpen && !craftingOpen && !chestOpen && !furnaceOpen && canSprint );

        player.update(dx, dz, jump, dive, dt);

        boolean moving = dx != 0 || dz != 0;

        if (moving && !inventoryOpen && !craftingOpen && !chestOpen) {

            stepTimer -= dt;

            if (stepTimer <= 0f) {

                int bx = (int) Math.floor(player.getX());
                int by = (int) Math.floor(player.getY() - 1);
                int bz = (int) Math.floor(player.getZ());

                Block ground = world.getBlock(bx, by, bz);

                if (player.isGrounded()){
                    sound.playRandom(
                        sound.stepSound(ground),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        0.15f
                    );

                    stepTimer = STEP_INTERVAL;

                } else if (player.isInWater()){
                    sound.playRandom(
                        sound.stepSound(Block.WATER),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        0.15f
                    );
                    stepTimer = STEP_INTERVAL + 1;
                }
            }
        }
    }   

    private void handleCameraInput() {

        if (inventoryOpen || craftingOpen || chestOpen || furnaceOpen) {

            double[] cx = new double[1];
            double[] cy = new double[1];

            glfwGetCursorPos(window.getHandle(), cx, cy);

            inventoryScreen.updateMouse((int) cx[0], (int) cy[0]);

            input.consumeMouseDX();
            input.consumeMouseDY();

            return;
        }

        float mdx = input.consumeMouseDX();
        float mdy = input.consumeMouseDY();

        camera.rotate(mdx * MOUSE_SENS, mdy * MOUSE_SENS);

        float[] front = camera.getFront();
        float[] up    = camera.getUp();

        sound.updateListener(
            camera.getX(),
            camera.getY(),
            camera.getZ(),
            front[0], front[1], front[2],
            up[0], up[1], up[2]
        );
    }

    private float rayAABB(float ox, float oy, float oz, float dx, float dy, float dz, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        float tmin = 0f, tmax = Float.MAX_VALUE;

        if (Math.abs(dx) > 1e-6f) {
            float t1 = (minX - ox) / dx, t2 = (maxX - ox) / dx;
            if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1); tmax = Math.min(tmax, t2);
        } else if (ox < minX || ox > maxX) return -1f;

        if (Math.abs(dy) > 1e-6f) {
            float t1 = (minY - oy) / dy, t2 = (maxY - oy) / dy;
            if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1); tmax = Math.min(tmax, t2);
        } else if (oy < minY || oy > maxY) return -1f;

        if (Math.abs(dz) > 1e-6f) {
            float t1 = (minZ - oz) / dz, t2 = (maxZ - oz) / dz;
            if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1); tmax = Math.min(tmax, t2);
        } else if (oz < minZ || oz > maxZ) return -1f;

        return (tmax >= tmin) ? tmin : -1f;
    }

    private Mob getMobInSight(float reach) {
        float ox = camera.getX(), oy = camera.getY(), oz = camera.getZ();
        float[] front = camera.getFront();
        float dx = front[0], dy = front[1], dz = front[2];

        Mob   closest = null;
        float closestT = reach;

        for (Mob mob : mobs.getMobs()) {
            float hw = mob.getWidth() / 2f;
            float t = rayAABB(ox, oy, oz, dx, dy, dz,
                mob.getX() - hw, mob.getY(),                mob.getZ() - hw,
                mob.getX() + hw, mob.getY() + mob.getHeight(), mob.getZ() + hw);

            if (t >= 0 && t < closestT) {
                closestT = t;
                closest  = mob;
            }
        }
        return closest;
    }

    private void handlePlayerInteraction(float dt) {

        if (inventoryOpen || craftingOpen || chestOpen || furnaceOpen) {

            breakElapsed  = 0f;
            breakDuration = 0f;

            hitSoundTimer = 0f;

            breakX = breakY = breakZ = -1;

            hud.setBreakProgress(0f);

            leftWasDown  = false;
            rightWasDown = false;

            return;
        }

        float ox = camera.getX();
        float oy = camera.getY();
        float oz = camera.getZ();

        float[] front = camera.getFront();

        Raycast.HitResult hit = Raycast.cast(
            ox, oy, oz,
            front[0], front[1], front[2],
            REACH,
            world
        );

        float blockDist = hit.hit
            ? (float) Math.sqrt(
                Math.pow(hit.blockX + 0.5 - ox, 2) +
                Math.pow(hit.blockY + 0.5 - oy, 2) +
                Math.pow(hit.blockZ + 0.5 - oz, 2))
            : Float.MAX_VALUE;

        Mob targetMob = getMobInSight(3.5f);

        boolean leftDown        = input.isMouseDown(GLFW_MOUSE_BUTTON_LEFT);
        boolean rightDown       = input.isMouseDown(GLFW_MOUSE_BUTTON_RIGHT);
        boolean leftJustPressed = leftDown && !leftWasDown;

        if (leftJustPressed && targetMob != null) {

            float mdx = targetMob.getX() - ox;
            float mdy = targetMob.getY() - oy;
            float mdz = targetMob.getZ() - oz;

            float mobDist = (float) Math.sqrt(mdx * mdx + mdy * mdy + mdz * mdz);

            if (mobDist < blockDist) {

                int toolSlot = player.getInventory().getSelectedSlot();
                int toolId = player.getInventory().getSelectedBlockId();
                Block tool = Block.fromId(toolId); 
                int damage = 2;

                if (tool != null) {
                    switch (tool) {
                        case WOODEN_SWORD: damage += 5; break;
                        case WOODEN_PICKAXE: damage += 1; break;
                        case WOODEN_AXE: damage += 3; break; 
                        case WOODEN_SHOVEL: damage -= 1.9f; break;
                        
                        case STONE_SWORD: damage += 7; break;
                        case STONE_PICKAXE: damage += 1; break;
                        case STONE_AXE: damage += 4; break; 
                        case STONE_SHOVEL: damage -= 1.5f; break;

                        case IRON_SWORD: damage += 10; break;
                        case IRON_PICKAXE: damage += 2; break;
                        case IRON_AXE: damage += 6; break; 
                        case IRON_SHOVEL: damage -= 1.3f; break;

                        case DIAMOND_SWORD: damage += 15; break;
                        case DIAMOND_PICKAXE: damage += 2; break;
                        case DIAMOND_AXE: damage += 7; break; 
                        case DIAMOND_SHOVEL: damage -= 1; break;

                        default: break;
                    }
                }
                targetMob.damage(damage);

                float kx = targetMob.getX() - camera.getX();
                float kz = targetMob.getZ() - camera.getZ();
                float klen = (float) Math.sqrt(kx*kx + kz*kz);
                if (klen > 0.001f) {
                    targetMob.applyKnockback(kx/klen, kz/klen, 9.0f);
                }

                if (Inventory.getMaxDurability(toolId) > 0) {
                    player.getInventory().damageTool(toolSlot);
                }

                leftWasDown  = leftDown;
                rightWasDown = rightDown;

                return;
            }
        }

        boolean rightJustPressed = rightDown && !rightWasDown;
        if (rightJustPressed) {
            int selId = player.getInventory().getSelectedBlockId();
            if (Player.isFoodItem(selId)) {
                boolean ate = player.eatFood(selId);
                if (ate) {
                    player.getInventory().consumeSelected(1);
                    rightWasDown = rightDown;
                    leftWasDown = leftDown;
                    return;
                }
            }
        }

        if (hit.hit) {

            Block target = world.getBlock(hit.blockX, hit.blockY, hit.blockZ);

            if (leftDown && target.breakTime > 0f) {

                int   toolId = player.getInventory().getSelectedBlockId();
                float mult   = getToolMultiplier(toolId, target);

                if (hit.blockX != breakX
                || hit.blockY != breakY
                || hit.blockZ != breakZ) {

                    breakX = hit.blockX;
                    breakY = hit.blockY;
                    breakZ = hit.blockZ;

                    breakElapsed  = 0f;
                    breakDuration = target.breakTime/mult;

                    sound.playRandom(
                        sound.hitSound(target),
                        breakX + 0.5f,
                        breakY + 0.5f,
                        breakZ + 0.5f,
                        0.4f
                    );

                    hitSoundTimer = 0f;
                }

                breakElapsed += dt;

                if (breakElapsed >= breakDuration) {

                    world.setBlock(breakX, breakY, breakZ, 0);
                    int dropId = target.getDropId();

                    int toolSlot = player.getInventory().getSelectedSlot();

                    if (mult > 1.0f) {
                        boolean broke = player.getInventory().damageTool(toolSlot);
                        if (broke) {
                            sound.play(SoundEvent.TOOL_BREAKING, player.getX(), player.getY(), player.getZ(), 1f, 1f);
                        }
                    }

                    if (dropId != 0) {
                        player.getInventory().addItem(dropId, 1);
                    }

                    if (target == Block.CHEST) {
                        dumpChestContents(breakX, breakY, breakZ);
                    }

                    sound.playRandom(
                        sound.breakSound(target),
                        breakX + 0.5f,
                        breakY + 0.5f,
                        breakZ + 0.5f,
                        1f
                    );

                    breakElapsed  = 0f;
                    breakDuration = 0f;

                    breakX = breakY = breakZ = -1;
                }

                hitSoundTimer += dt;

                if (hitSoundTimer >= HIT_SOUND_INTERVAL) {

                    hitSoundTimer = 0f;

                    sound.playRandom(
                        sound.hitSound(target),
                        breakX + 0.5f,
                        breakY + 0.5f,
                        breakZ + 0.5f,
                        0.4f
                    );
                }

            } else {

                breakElapsed  = 0f;
                breakDuration = 0f;

                hitSoundTimer = 0f;

                if (!leftDown) {
                    breakX = breakY = breakZ = -1;
                }
            }

            if (rightDown && !rightWasDown) {

                Block hitBlock = world.getBlock(hit.blockX, hit.blockY, hit.blockZ);
                
                if (hitBlock == Block.BED) {
                    player.setSpawnPoint(
                        hit.blockX + 0.5f,
                        hit.blockY + 1f,
                        hit.blockZ + 0.5f
                    );
                    hud.showNotification("Ponto de spawn definido!", 3.0f);

                    if (dayNight.isNight()) {
                        dayNight.skipToMorning();
                        hud.showNotification("Passando para a manhã...", 2.0f);
                    }
                } else if (hitBlock == Block.DOOR_CLOSED) {
                    world.setBlock(hit.blockX, hit.blockY, hit.blockZ, Block.DOOR_OPEN.id);
                } else if (hitBlock == Block.DOOR_OPEN) {
                    world.setBlock(hit.blockX, hit.blockY, hit.blockZ, Block.DOOR_CLOSED.id);
                } else if (hitBlock == Block.CRAFTING_TABLE) {
                    openCrafting();
                    return; 
                } else if (hitBlock == Block.CHEST) {
                    Inventory ci = world.getChestInventory(hit.blockX, hit.blockY, hit.blockZ);
                    chestScreen.openFor(ci);
                    openChest();
                    return;
                } else if (hitBlock == Block.FURNACE){
                    FurnaceState fs = world.getFurnaceState(hit.blockX, hit.blockY, hit.blockZ);
                    furnaceScreen.openFor(fs);
                    openFurnace();
                    return;
                }  else{
                    int blockId = player.getInventory().getSelectedBlockId();

                    if (blockId != 0) {

                        world.setBlock(
                            hit.prevX,
                            hit.prevY,
                            hit.prevZ,
                            blockId
                        );

                        player.getInventory().consumeSelected(1);

                        sound.playRandom(
                            sound.placeSound(target),
                            hit.prevX + 0.5f,
                            hit.prevY + 0.5f,
                            hit.prevZ + 0.5f,
                            0.6f
                        );
                    }
                }
            }

            rightWasDown = rightDown;
            leftWasDown  = leftDown;

        } else {

            breakElapsed  = 0f;
            breakDuration = 0f;

            breakX = breakY = breakZ = -1;

            leftWasDown  = false;
            rightWasDown = false;
        }

        float breakProgress = (breakDuration > 0f)
            ? Math.min(1f, breakElapsed / breakDuration)
            : 0f;

        hud.setBreakProgress(breakProgress);

        leftWasDown  = leftDown;
        rightWasDown = rightDown;
    }

    private void handleInventoryToggle() {
        boolean eDown = input.isKeyDown(GLFW_KEY_E);
        if (eDown && !prevEKeyDown) {
            if (inventoryOpen) {
                closeInventory();
            } else if (craftingOpen){
                closeCrafting();
            } else if (chestOpen){
                closeChest();
            } else if (furnaceOpen){
                closeFurnace();
            }
            else {
                openInventory();
            }
        }
        prevEKeyDown = eDown;
    }

    private float getToolMultiplier(int toolId, Block target) {
        Block tool = Block.fromId(toolId);
        if (tool == null || target == null) return 1.0f;

        String toolName = tool.name();

        if (toolName.contains("_PICKAXE") && target.isStoneType()) {
            return tool.getToolMaterialMultiplier();
        }
        
        if (toolName.contains("_AXE") && target.isWoodType()) {
            return tool.getToolMaterialMultiplier();
        }
        
        if (toolName.contains("_SHOVEL") && target.isEarthType()) {
            return tool.getToolMaterialMultiplier();
        }

        return 1.0f;
    }

    private void doRespawn() {
        player.respawn();
        deathTimer = 0f;
        hud.setDeathAlpha(0f);
        player.getInventory().clear();
    }

    private void applyMobDamage(float dt) {
        float px = player.getX(), pz = player.getZ(), py = player.getY();
        boolean hit = false;

        for (com.mcraft.entity.Mob mob : mobs.getMobs()) {
            if (mob.getType() != com.mcraft.entity.Mob.Type.ZOMBIE
            && mob.getType() != com.mcraft.entity.Mob.Type.CREEPER) continue;
            if (mob.getState() != com.mcraft.entity.Mob.AIState.SEEK) continue;

            float dx = mob.getX() - px;
            float dz = mob.getZ() - pz;
            float dist = (float) Math.sqrt(dx*dx + dz*dz);

            float mobBottom  = mob.getY();
            float mobTop     = mob.getY() + mob.getHeight();
            float playerBottom = py;
            float playerTop    = py + Player.HEIGHT;
            boolean yOverlap = (mobTop > playerBottom - 0.3f) && (mobBottom < playerTop + 0.3f);

            if (dist < 1.2f && yOverlap) {
                hit = player.takeDamageWithArmour(2);
                float kx = player.getX() - mob.getX();
                float kz = player.getZ() - mob.getZ();
                float klen = (float) Math.sqrt(kx*kx + kz*kz);
                if (klen > 0.001f) {
                    player.applyKnockback(kx/klen, kz/klen, 7.0f);
                }
            }

            if (hit){}
        }
    }

    private void dumpChestContents(int bx, int by, int bz) {
        Inventory chestInv = world.removeChestInventory(bx, by, bz);
        if (chestInv == null) return; 

        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            int id  = chestInv.getItemId(i);
            if (id == 0) continue;

            int qty = chestInv.getItemQty(i);
            int dur = chestInv.getItemDurability(i);

            if (dur >= 0) {
                player.getInventory().addToolWithDurability(id, dur);
            } else {
                player.getInventory().addItem(id, qty);
            }
        }
    }

    private void cleanup() {
        world.shutdown();
        if (inventoryOpen) {
            inventoryScreen.onClose();
        }
        if (craftingOpen){
            craftingScreen.onClose();
        }
        if (chestOpen){
            chestScreen.onClose();
        }
        if (furnaceOpen){
            furnaceScreen.onClose();
        }
        blockShader.delete();
        world.saveAll(world.getWorldIO());
        world.getWorldIO().saveMobs(mobs.getMobs());
        world.deleteAllMeshes();
        hudShader.delete();
        skyRenderer.delete();
        skyShader.delete();
        mobShader.delete();
        breakOverlay.delete();
        atlas.delete();
        sound.cleanup();
        lightScheduler.delete();
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

    public WeatherSystem getWeatherSystem(){
        return this.weather;
    }

    public void setMobs(java.util.List<Mob> loadedMobs) {
        mobs.setMobs(loadedMobs);
    }
}