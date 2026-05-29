package com.mcraft.ui;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;

import com.mcraft.render.Camera;
import com.mcraft.render.Shader;
import com.mcraft.render.TextureAtlas;
import com.mcraft.world.Block;
import com.mcraft.player.Player;

public class HUD {

    private static final int SLOT_SIZE = 40;
    private static final int PADDING   = 4;

    private static final int MAX_QUADS = 8192;
    private static final int VERT_FLOATS = 8;

    private float breakProgress = 0f;

    private boolean underwater    = false;
    private float   underwaterWave = 0f;

    private final int     screenW, screenH;
    private final Inventory inventory;
    private final Shader    hudShader;
    private final TextureAtlas atlas;
    private final float[]   ortho;

    private final FloatBuffer vBuf = BufferUtils.createFloatBuffer(MAX_QUADS * 4 * VERT_FLOATS);
    private final IntBuffer   iBuf = BufferUtils.createIntBuffer (MAX_QUADS * 6);
    private int vao2d, vbo2d, ebo2d;
    private int quadCount;

    private float deathAlpha      = 0f;
    private final Player player;
    private float damageFlash    = 0f; 
    private int   lastHealth     = -1;

    private int   currentDay     = 1;
    private String notifMessage  = "";
    private float  notifTimer    = 0f;

    public HUD (int screenW, int screenH, Inventory inventory, Shader hudShader, TextureAtlas atlas, Player player) {
        this.screenW   = screenW;
        this.screenH   = screenH;
        this.inventory = inventory;
        this.hudShader = hudShader;
        this.player = player;
        this.atlas     = atlas;
        this.ortho     = Camera.ortho(screenW, screenH);

        initGPUBuffers();
    }

    private void initGPUBuffers() {
        vao2d = glGenVertexArrays();
        vbo2d = glGenBuffers();
        ebo2d = glGenBuffers();

        glBindVertexArray(vao2d);

        glBindBuffer(GL_ARRAY_BUFFER, vbo2d);
        glBufferData(GL_ARRAY_BUFFER, (long) MAX_QUADS * 4 * VERT_FLOATS * Float.BYTES,
                GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo2d);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, (long) MAX_QUADS * 6 * Integer.BYTES,
                GL_DYNAMIC_DRAW);

        int stride = VERT_FLOATS * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0L);      
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 8L);       
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 4, GL_FLOAT, false, stride, 16L);      
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
    }


    public void render(float dt) {

        quadCount = 0;

        vBuf.clear();
        iBuf.clear();

        if (vBuf.remaining() < 32 || iBuf.remaining() < 6)
            return;

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        hudShader.use();
        hudShader.setMatrix4("uOrtho", ortho);
        hudShader.setInt("uTexture", 0);

        beginBatch();
        drawCrosshair();
        drawHotbar();
        flushBatch(false);

        beginBatch();
        drawHotbarIcons();
        flushBatch(true);
        
        beginBatch();
        drawHotbarCounts();
        flushBatch(false);

        beginBatch();
        int curHealth = player.getHealth();
        if (lastHealth > 0 && curHealth < lastHealth) {
            damageFlash = 1.0f;
        }
        lastHealth = curHealth;
        if (damageFlash > 0) damageFlash -= dt * 4f; 
        damageFlash = Math.max(0f, damageFlash);

        drawHearts(curHealth, player.getMaxHealth());
        if (damageFlash > 0.01f)   drawDamageFlash();
        flushBatch(false);

        beginBatch();
        if (underwater) {
            underwaterWave += dt * 1.8f; 
            drawUnderwaterOverlay();
        }
        flushBatch(false);

        beginBatch();
        drawDayCounter();
        if (notifTimer > 0) { drawNotification(dt); }
        flushBatch(false);

        beginBatch();
        if (deathAlpha > 0.01f) drawDeathOverlay();
        flushBatch(false);

        glDisable(GL_BLEND);

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }

    private void drawCrosshair() {
        int cx = screenW / 2, cy = screenH / 2;
        int arm = 9, thick = 1;

        drawRect(cx - arm, cy - thick, arm * 2, thick * 2, 1, 1, 1, 0.8f);
        drawRect(cx - thick, cy - arm, thick * 2, arm * 2, 1, 1, 1, 0.8f);
    }

    private void drawHotbar() {
        int total  = Inventory.HOTBAR_SIZE * (SLOT_SIZE + PADDING) - PADDING;
        int startX = (screenW - total) / 2;
        int startY = screenH - SLOT_SIZE - 10;

        for (int i = 0; i < Inventory.HOTBAR_SIZE; i++) {
            int sx = startX + i * (SLOT_SIZE + PADDING);
            boolean sel = (i == inventory.getSelectedSlot());

            float bg = sel ? 0.9f : 0.3f;
            drawRect(sx, startY, SLOT_SIZE, SLOT_SIZE, bg, bg, bg, 0.75f);

            if (sel) {
                int b = 2;
                drawRect(sx - b, startY - b, SLOT_SIZE + b*2, b, 1, 1, 1, 1); 
                drawRect(sx - b, startY + SLOT_SIZE, SLOT_SIZE + b*2, b, 1, 1, 1, 1);
                drawRect(sx - b, startY, b, SLOT_SIZE, 1, 1, 1, 1); 
                drawRect(sx + SLOT_SIZE, startY, b, SLOT_SIZE, 1, 1, 1, 1); 
            }
        }
    }

    private void drawHotbarIcons() {
        int total  = Inventory.HOTBAR_SIZE * (SLOT_SIZE + PADDING) - PADDING;
        int startX = (screenW - total) / 2;
        int startY = screenH - SLOT_SIZE - 10;
        int pad    = 6;

        atlas.bind(0);

        for (int i = 0; i < Inventory.HOTBAR_SIZE; i++) {
            int blockId = inventory.getItemId(i);
            if (blockId == 0) continue;

            Block block = Block.fromId(blockId);
            float[] uvs = block.getUVs(0);

            int sx = startX + i * (SLOT_SIZE + PADDING) + pad;
            int sy = startY + pad;
            int sz = SLOT_SIZE - pad * 2;

            drawTexRect(sx, sy, sz, sz, uvs[0], uvs[1], uvs[4], uvs[5]);
        }
    }

    private void drawHotbarCounts() {
        int total  = Inventory.HOTBAR_SIZE * (SLOT_SIZE + PADDING) - PADDING;
        int startX = (screenW - total) / 2;
        int startY = screenH - SLOT_SIZE - 10;

        for (int i = 0; i < Inventory.HOTBAR_SIZE; i++) {

            int qty = inventory.getItemQty(i);

            if (qty <= 1) continue;

            int sx = startX + i * (SLOT_SIZE + PADDING);

            int numW = PixelFont.measureWidth(qty) * 2 + 2;

            int qx = sx + SLOT_SIZE - numW - 3;
            int qy = startY + SLOT_SIZE - 13;

            PixelFont.drawIntShadow(
                this::addRect,
                qx,
                qy,
                2,
                qty,
                1f, 1f, 1f
            );
        }
    }

    private void drawRect(int x, int y, int w, int h,
                           float r, float g, float b, float a) {
        float x0 = x, y0 = y, x1 = x + w, y1 = y + h;
        float[] uv = {0, 0, 1, 0, 1, 1, 0, 1};

        addQuad(x0, y0, x1, y1, uv[0], uv[1], uv[4], uv[5], r, g, b, a);
    }

    private void drawTexRect(int x, int y, int w, int h,
                              float u0, float v0, float u1, float v1) {
        addQuad(x, y, x + w, y + h, u0, v0, u1, v1, 1, 1, 1, 1);
    }

    private void drawHearts(int health, int maxHealth) {
        int totalHearts = maxHealth / 2;
        int fullHearts  = health    / 2;
        boolean hasHalf = (health % 2) == 1;

        int HW = 18, HH = 16, GAP = 2; 
        int totalW = totalHearts * (HW + GAP) - GAP;

        int hotbarW = Inventory.HOTBAR_SIZE * (SLOT_SIZE + PADDING) - PADDING;
        int hotbarX = (screenW - hotbarW) / 2;
        int hotbarY = screenH - SLOT_SIZE - 10;
        int startX  = hotbarX + (hotbarW - totalW) / 2;
        int startY  = hotbarY - HH - 4;

        for (int i = 0; i < totalHearts; i++) {
            int hx = startX + i * (HW + GAP);

            drawPixelHeart(hx, startY, HW, HH, 0.28f, 0.28f, 0.28f, 0.8f);

            if (i < fullHearts) {
                float red  = 0.85f + damageFlash * 0.15f;
                float green= 0.10f - damageFlash * 0.10f;
                drawPixelHeart(hx, startY, HW, HH, red, green, 0.10f, 1.0f);
            } else if (i == fullHearts && hasHalf) {
                drawPixelHeartHalf(hx, startY, HW, HH, 0.85f, 0.10f, 0.10f);
            }
        }
    }

    private void drawPixelHeart(int x, int y, int w, int h, float r, float g, float b, float a) {
        int s = w / 9; 

        addRect(x + s,   y,       s*3, s*2, r, g, b, a);
        addRect(x + s*5, y,       s*3, s*2, r, g, b, a);
        addRect(x,       y + s*2, s*9, s*2, r, g, b, a);
        addRect(x + s,   y + s*4, s*7, s*2, r, g, b, a);
        addRect(x + s*2, y + s*6, s*5, s,   r, g, b, a);
        addRect(x + s*3, y + s*7, s*3, s,   r, g, b, a);
        addRect(x + s*4, y + s*8, s,   s,   r, g, b, a);
    }

    private void drawPixelHeartHalf(int x, int y, int w, int h,
                                    float r, float g, float b) {
        int s   = w / 9;
        int mid = x + w / 2;

        addRect(x + s,   y,       s*3, s*2, r, g, b, 1f);
        addRect(x,       y + s*2, mid-x, s*2, r, g, b, 1f);
        addRect(x + s,   y + s*4, mid-x-s, s*2, r, g, b, 1f);
        addRect(x + s*2, y + s*6, mid-x-s*2, s, r, g, b, 1f);
    }

    private void drawDeathOverlay() {
        addRect(0, 0, screenW, screenH, 0.6f, 0.0f, 0.0f, deathAlpha * 0.65f);

        if (deathAlpha > 0.5f) {
            String msg = "YOU DIED";
            int pixSize = 4;
            int totalW = 8 * (PixelFont.measureWidth(8) + 1) * pixSize;
            int tx = (screenW - totalW) / 2;
            int ty = (screenH / 2) - 20;

            for (int i = 0; i < msg.length(); i++) {
                char c = msg.charAt(i);
                if (c >= '0' && c <= '9') {
                    PixelFont.drawIntShadow(this::addRect,
                        tx + i * (PixelFont.measureWidth(c-'0')+1)*pixSize,
                        ty, pixSize, c - '0', 1f, 1f, 1f);
                }
            }
        }
    }

    private void drawDamageFlash() {
        float a = damageFlash * 0.45f;
        addRect(0, 0, screenW, screenH, 0.75f, 0f, 0f, a);
    }

    private void drawUnderwaterOverlay() {
        float pulse = 0.22f +
        (float)Math.sin(underwaterWave) * 0.04f;

        addRect(
            0, 0,
            screenW, screenH,
            0.04f, 0.16f, 0.42f,
            pulse
        );

        float edge = pulse * 1.35f;

        addRect(0, 0, screenW, 70,
            0.05f, 0.18f, 0.55f, edge);

        addRect(0, screenH - 70, screenW, 70,
            0.05f, 0.18f, 0.55f, edge);

        addRect(0, 70, 70, screenH - 140,
            0.05f, 0.18f, 0.55f, edge * 0.8f);

        addRect(screenW - 70, 70, 70, screenH - 140,
            0.05f, 0.18f, 0.55f, edge * 0.8f);
    }

    private void drawDayCounter() {
        int px  = screenW - 80, py = 6;
        int day = currentDay;
        int ps  = 2; 

        PixelFont.drawIntShadow(this::addRect, px, py, ps, day, 1f, 1f, 0.8f);

        float t = 0.5f;
        boolean isNight = (t < 0.22f || t > 0.72f);
        float iconColor = isNight ? 0.7f : 1.0f;
        addRect(px - 18, py, 14, 14, iconColor, iconColor, isNight ? 0.5f : 0.1f, 0.85f);
    }

    private void drawNotification(float dt) {

        notifTimer -= dt;

        if (notifTimer <= 0f) {
            notifMessage = "";
            return;
        }

        float alpha = Math.min(1.0f, notifTimer);
        int pixelSize = 2;
        int msgW = PixelFont.measureWidth(notifMessage) * pixelSize;
        int mx = (screenW - msgW) / 2;
        int my = screenH / 2 - 60;

        addRect(
            mx - 10,
            my - 4,
            msgW + 20,
            18,
            0f, 0f, 0f,
            alpha * 0.65f
        );

        PixelFont.drawStringShadow(
            this::addRect,
            mx,
            my,
            pixelSize,
            notifMessage,
            1f, 1f, 1f
        );
    }

    private void addQuad(float x0, float y0, float x1, float y1,
                     float u0, float v0, float u1, float v1,
                     float r, float g, float b, float a) {

        if (vBuf.remaining() < 32 || iBuf.remaining() < 6) {
            return;
        }

        vBuf.put(new float[]{
            x0, y1,  u0, v1,  r, g, b, a,
            x1, y1,  u1, v1,  r, g, b, a,
            x1, y0,  u1, v0,  r, g, b, a,
            x0, y0,  u0, v0,  r, g, b, a
        });

        int base = quadCount * 4;

        iBuf.put(new int[]{
            base, base+1, base+2,
            base+2, base+3, base
        });

        quadCount++;
    }

    private void addRect(int x, int y, int w, int h, float r, float g, float b, float a) {
        addQuad(x, y, x + w, y + h, 0, 0, 1, 1, r, g, b, a);
    }

    public void showNotification(String msg, float duration) {
        this.notifMessage = msg;
        this.notifTimer   = duration;
    }

    private void beginBatch() {
        vBuf.clear();
        iBuf.clear();
        quadCount = 0;
    }

    private void flushBatch(boolean useTexture) {
        if (quadCount == 0) return;

        vBuf.flip();
        iBuf.flip();

        glBindVertexArray(vao2d);

        glBindBuffer(GL_ARRAY_BUFFER, vbo2d);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vBuf);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo2d);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, iBuf);

        if (useTexture) {
            atlas.bind(0);
            hudShader.setInt("uTexture", 0);
        }

        hudShader.setInt("uUseTexture", useTexture ? 1 : 0);

        glDrawElements(GL_TRIANGLES, quadCount * 6, GL_UNSIGNED_INT, 0L);

        glBindVertexArray(0);
    }

    public void setBreakProgress(float p) {
        this.breakProgress = Math.max(0f, Math.min(1f, p));
    }
    public float getBreakProgress(){
        return this.breakProgress;
    }

    public void setDeathAlpha(float a) { this.deathAlpha = a; }
    public float getDeathAlpha() {return this.deathAlpha;}

    public void setUnderwater(boolean u) { this.underwater = u; }
    public boolean getUnderwater() { return this.underwater;}

    public void setDay(int day)   { this.currentDay = day; }

}