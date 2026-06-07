package com.mcraft.ui;

import com.mcraft.render.Shader;
import com.mcraft.render.TextureAtlas;
import com.mcraft.world.Block;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public abstract class Screen2D {

    protected static final int SLOT_PX = 36;
    protected static final int PAD     = 4;
    protected static final int BORDER  = 10;
    protected static final int S       = SLOT_PX + PAD;

    protected final int          sw, sh;
    protected final Shader       shader;
    protected final TextureAtlas atlas;
    protected final float[]      ortho;

    protected int heldId  = 0;
    protected int heldQty = 0;
    protected int heldDur = -1;
    protected int mouseX, mouseY;

    private static final int MAX_QUADS   = 512;
    private static final int VERT_FLOATS = 8;

    private int vao, vbo, ebo;
    private final FloatBuffer vBuf =
            BufferUtils.createFloatBuffer(MAX_QUADS * 4 * VERT_FLOATS);
    private final IntBuffer iBuf =
            BufferUtils.createIntBuffer(MAX_QUADS * 6);
    private int quadCount;

    protected Screen2D(int sw, int sh, Shader shader,
                        TextureAtlas atlas, float[] ortho) {
        this.sw     = sw;
        this.sh     = sh;
        this.shader = shader;
        this.atlas  = atlas;
        this.ortho  = ortho;
        this.mouseX = sw / 2;
        this.mouseY = sh / 2;
        initGPU();
    }

    public abstract void render();

    public abstract boolean onClick(int mx, int my);

    public abstract void onClose();

    public void updateMouse(int mx, int my) { 
        mouseX = mx; 
        mouseY = my; 
    }

    protected void beginBatch() {
        vBuf.clear();
        iBuf.clear();
        quadCount = 0;
    }

    protected void flushBatch(boolean useTexture) {
        if (quadCount == 0) return;
        vBuf.flip(); iBuf.flip();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vBuf);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, iBuf);

        shader.setInt("uUseTexture", useTexture ? 1 : 0);
        glDrawElements(GL_TRIANGLES, quadCount * 6, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    protected void addRect(int x, int y, int w, int h,
                            float r, float g, float b, float a) {
        addQuad(x, y, x+w, y+h, 0, 0, 1, 1, r, g, b, a);
    }

    protected void drawBlockIcon(Block block, int x, int y, int size) {
        if (block == null || block == Block.AIR) return;
        float ts = 1f / 16f;
        float u0 = block.texCol * ts, v0 = block.texRow * ts;
        addQuad(x, y, x+size, y+size, u0, v0, u0+ts, v0+ts, 1, 1, 1, 1);
    }

    protected void drawSlotBg(int x, int y, boolean selected) {
        float bg = selected ? 0.62f : 0.32f;
        float br = selected ? 1.0f  : 0.55f;

        addRect(x, y, SLOT_PX, SLOT_PX, bg, bg, bg, 1f);
        addRect(x,           y,           SLOT_PX, 1, br, br, br, 1f);
        addRect(x,           y+SLOT_PX-1, SLOT_PX, 1, br, br, br, 1f);
        addRect(x,           y,           1, SLOT_PX, br, br, br, 1f);
        addRect(x+SLOT_PX-1, y,           1, SLOT_PX, br, br, br, 1f);
    }

    protected void drawSeparator(int x, int y, int width) {
        addRect(x, y, width, 2, 0.55f, 0.55f, 0.55f, 0.6f);
    }

    protected void drawSlotQty(Inventory inv, int slotIdx, int slotX, int slotY) {
        int qty = inv.getItemQty(slotIdx);
        if (qty > 1) {
            int nw = PixelFont.measureWidth(qty) * 2 + 2;
            PixelFont.drawIntShadow(this::addRect,
                slotX + SLOT_PX - nw - 2,
                slotY + SLOT_PX - 12,
                2, qty, 1f, 1f, 1f);
        }
    }

    protected void drawHeldItem() {
        if (heldId == 0) return;
        int hx = mouseX - SLOT_PX/2, hy = mouseY - SLOT_PX/2;
        addRect(hx, hy, SLOT_PX, SLOT_PX, 0.4f, 0.4f, 0.4f, 0.7f);
    }

    protected void drawHeldItemIcon() {
        if (heldId == 0) return;
        atlas.bind(0);
        int hx = mouseX - SLOT_PX/2, hy = mouseY - SLOT_PX/2;
        drawBlockIcon(Block.fromId(heldId), hx+2, hy+2, SLOT_PX-4);
    }

    protected void handleInvSlotClick(Inventory inv, int slotIdx) {
        int slotId  = inv.getItemId (slotIdx);
        int slotQty = inv.getItemQty(slotIdx);
        int slotDur = inv.getItemDurability(slotIdx);

        if (heldId == 0) {
            if (slotId != 0) {
                heldId  = slotId;
                heldQty = slotQty;
                heldDur = slotDur;
                inv.clearSlot(slotIdx);
            }
        } else {
            if (slotId == 0) {
                inv.setSlotFull(slotIdx, heldId, heldQty, heldDur);
                heldId = 0; heldQty = 0; heldDur = -1;
            } else if (slotId == heldId && slotQty < 64 && !Inventory.isTool(heldId)) {
                int add = Math.min(heldQty, 64 - slotQty);
                inv.setSlot(slotIdx, slotId, slotQty + add);
                heldQty -= add;
                if (heldQty <= 0) { heldId = 0; heldQty = 0; }
            } else {
                inv.setSlotFull(slotIdx, heldId, heldQty, heldDur);
                heldId = slotId; heldQty = slotQty; heldDur = slotDur;
            }
        }
    }

    protected void returnHeldItem(Inventory inv) {
        if (heldId != 0) {
            if (Inventory.isTool(heldId)) {
                inv.addToolWithDurability(heldId, heldDur);
            } else {
                inv.addItem(heldId, heldQty);
            }
            heldId = 0; heldQty = 0; heldDur = -1;
        }
    }

    protected static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x+w && my >= y && my < y+h;
    }

    protected void beginRender() {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader.use();
        shader.setMatrix4("uOrtho", ortho);
        shader.setInt("uTexture", 0);
    }

    protected void endRender() {
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    private void addQuad(float x0, float y0, float x1, float y1,
                          float u0, float v0, float u1, float v1,
                          float r, float g, float b, float a) {
        if (quadCount >= MAX_QUADS) return;
        vBuf.put(new float[]{
            x0,y1, u0,v1, r,g,b,a,
            x1,y1, u1,v1, r,g,b,a,
            x1,y0, u1,v0, r,g,b,a,
            x0,y0, u0,v0, r,g,b,a
        });
        int base = quadCount * 4;
        iBuf.put(new int[]{ base, base+1, base+2, base+2, base+3, base });
        quadCount++;
    }

    private void initGPU() {
        vao = glGenVertexArrays(); vbo = glGenBuffers(); ebo = glGenBuffers();
        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER,
            (long)MAX_QUADS * 4 * VERT_FLOATS * Float.BYTES, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER,
            (long)MAX_QUADS * 6 * Integer.BYTES, GL_DYNAMIC_DRAW);

        int stride = VERT_FLOATS * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 8L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 4, GL_FLOAT, false, stride, 16L);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
    }

    public void delete() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }
}