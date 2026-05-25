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

public class InventoryScreen {

    private static final int SLOT_PX = 36;         
    private static final int PAD     = 4;            
    private static final int BORDER  = 10;           
    private static final int S       = SLOT_PX + PAD; 

    private final int          sw, sh;
    private final Inventory    inventory;
    private final CraftingGrid craft   = new CraftingGrid(2);
    private final Shader       shader;
    private final TextureAtlas atlas;
    private final float[]      ortho;

    private int heldId  = 0;
    private int heldQty = 0;
    private int mouseX;
    private int mouseY;

    private static final int MAX_QUADS   = 256;
    private static final int VERT_FLOATS = 8;   

    private int vao, vbo, ebo;
    private final FloatBuffer vBuf =
            BufferUtils.createFloatBuffer(MAX_QUADS * 4 * VERT_FLOATS);
    private final IntBuffer   iBuf =
            BufferUtils.createIntBuffer(MAX_QUADS * 6);
    private int quadCount;

    public InventoryScreen(int sw, int sh, Inventory inventory,
                            Shader shader, TextureAtlas atlas, float[] ortho) {
        this.sw        = sw;
        this.sh        = sh;
        this.inventory = inventory;
        this.shader    = shader;
        this.atlas     = atlas;
        this.ortho     = ortho;
        this.mouseX    = sw / 2;
        this.mouseY    = sh / 2;
        initGPU();
    }

    private int panelW() {
        return BORDER * 2 + 9 * S - PAD;
    }

    private int panelH() {
        return BORDER * 2 + 2 * S + 8 + 3 * S + PAD + SLOT_PX;
    }

    private int panelX() { return (sw - panelW()) / 2; }
    private int panelY() { return (sh - panelH()) / 2; }


    private int craftX(int col) { return panelX() + BORDER + col * S; }

    private int craftY(int row) { return panelY() + BORDER + row * S; }

    private int arrowX() { return craftX(2) + 4; }

    private int arrowY() { return craftY(0) + SLOT_PX / 2 - 4; }

    private int resultX() { return arrowX() + 30; }

    private int resultY() { return craftY(0) + (2 * S - PAD - SLOT_PX) / 2; }

    private int invAreaY() {
        return craftY(0) + 2 * S - PAD + 8;  
    }

    private int slotX(int idx) {
        int col = (idx < 9) ? idx : (idx - 9) % 9;
        return panelX() + BORDER + col * S;
    }

    private int slotY(int idx) {
        if (idx < 9) {
            return invAreaY() + 3 * S - PAD + PAD + 4;
        } else {
            int row = (idx - 9) / 9;  
            return invAreaY() + row * S;
        }
    }

    public void render() {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader.use();
        shader.setMatrix4("uOrtho", ortho);
        shader.setInt("uTexture", 0);

        beginBatch();
        drawPanel();
        drawCraftSection();
        drawInventorySlots();
        if (heldId != 0) {
            drawRect(mouseX - SLOT_PX / 2, mouseY - SLOT_PX / 2,
                    SLOT_PX, SLOT_PX, 0.5f, 0.5f, 0.5f, 0.75f);
        }
        flushBatch(false);

        atlas.bind(0);
        beginBatch();
        drawInventoryIcons();
        drawCraftIcons();
        if (heldId != 0) {
            drawBlockIcon(Block.fromId(heldId),
                    mouseX - SLOT_PX / 2 + 2,
                    mouseY - SLOT_PX / 2 + 2,
                    SLOT_PX - 4);
        }
        flushBatch(true);

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }


    private void drawPanel() {
        drawRect(panelX(), panelY(), panelW(), panelH(),
                0.12f, 0.12f, 0.12f, 0.93f);

        int sepY = invAreaY() - 5;
        drawRect(panelX() + BORDER, sepY, panelW() - BORDER * 2, 2,
                0.55f, 0.55f, 0.55f, 0.6f);

        int hotSepY = slotY(0) - 5;
        drawRect(panelX() + BORDER, hotSepY, panelW() - BORDER * 2, 2,
                0.55f, 0.55f, 0.55f, 0.6f);
    }

    private void drawCraftSection() {
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                drawSlotBg(craftX(c), craftY(r), false);
            }
        }

        int ax = arrowX(), ay = arrowY();
        drawRect(ax,      ay + 3, 20, 4,  0.85f, 0.85f, 0.1f, 1f);
        drawRect(ax + 14, ay,     6,  10, 0.85f, 0.85f, 0.1f, 1f);  

        int[] result = craft.getResult();
        drawSlotBg(resultX(), resultY(), false);
        if (result != null) {
            drawRect(resultX(), resultY(), SLOT_PX, SLOT_PX,
                    0.3f, 0.7f, 0.3f, 0.35f);
        }

    }


    private void drawInventorySlots() {
        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            boolean isSelected = (i < Inventory.HOTBAR_SIZE)
                                 && (i == inventory.getSelectedSlot());
            drawSlotBg(slotX(i), slotY(i), isSelected);
        }
    }

    private void drawSlotBg(int x, int y, boolean selected) {
        float bg = selected ? 0.62f : 0.32f;
        drawRect(x, y, SLOT_PX, SLOT_PX, bg, bg, bg, 1.0f);
        float br = selected ? 1.0f : 0.55f;
        drawRect(x,              y,              SLOT_PX, 1,      br, br, br, 1f);
        drawRect(x,              y + SLOT_PX - 1, SLOT_PX, 1,    br, br, br, 1f);
        drawRect(x,              y,              1,       SLOT_PX, br, br, br, 1f);
        drawRect(x + SLOT_PX - 1, y,             1,       SLOT_PX, br, br, br, 1f);
    }

    private void drawInventoryIcons() {
        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            int id = inventory.getItemId(i);
            if (id == 0) continue;  
            drawBlockIcon(Block.fromId(id), slotX(i) + 2, slotY(i) + 2, SLOT_PX - 4);
        }
    }

    private void drawCraftIcons() {
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                int id = craft.getSlot(r, c);
                if (id != 0) {
                    drawBlockIcon(Block.fromId(id),
                            craftX(c) + 2, craftY(r) + 2, SLOT_PX - 4);
                }
            }
        }

        int[] result = craft.getResult();
        if (result != null && result[0] != 0) {
            drawBlockIcon(Block.fromId(result[0]),
                    resultX() + 2, resultY() + 2, SLOT_PX - 4);
        }
    }

    public void updateMouse(int mx, int my) {
        this.mouseX = mx;
        this.mouseY = my;
    }

    public boolean onClick(int mx, int my) {
        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            if (hitTest(mx, my, slotX(i), slotY(i), SLOT_PX, SLOT_PX)) {
                handleInvSlotClick(i);
                return true;
            }
        }

        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                if (hitTest(mx, my, craftX(c), craftY(r), SLOT_PX, SLOT_PX)) {
                    handleCraftSlotClick(r, c);
                    return true;
                }
            }
        }

        if (hitTest(mx, my, resultX(), resultY(), SLOT_PX, SLOT_PX)) {
            handleResultClick();
            return true;
        }

        if (hitTest(mx, my, panelX(), panelY(), panelW(), panelH())) {
            return true;
        }

        return false;
    }

    private void handleInvSlotClick(int idx) {
        int slotId  = inventory.getItemId (idx);
        int slotQty = inventory.getItemQty(idx);

        if (heldId == 0) {
            if (slotId != 0) {
                heldId  = slotId;
                heldQty = slotQty;
                inventory.clearSlot(idx);    
            }
        } else {
            if (slotId == 0) {
                inventory.setSlot(idx, heldId, heldQty);
                heldId = 0;
                heldQty = 0;
            } else if (slotId == heldId && slotQty < 64) {
                int canAdd = Math.min(heldQty, 64 - slotQty);
                inventory.setSlot(idx, slotId, slotQty + canAdd);
                heldQty -= canAdd;
                if (heldQty <= 0) { heldId = 0; heldQty = 0; }
            } else {
                inventory.setSlot(idx, heldId, heldQty);
                heldId  = slotId;
                heldQty = slotQty;
            }
        }
    }

    private void handleCraftSlotClick(int row, int col) {
        int prev = craft.getSlot(row, col);

        if (heldId == 0) {
            if (prev != 0) {
                heldId  = prev;
                heldQty = 1;
                craft.setSlot(row, col, 0);
            }
        } else {
            if (prev == 0 || prev == heldId) {
                craft.setSlot(row, col, heldId);
                heldQty--;
                if (heldQty <= 0) { heldId = 0; heldQty = 0; }
            } else {
                craft.setSlot(row, col, heldId);
                heldId  = prev;
                heldQty = 1; 
            }
        }
    }

    private void handleResultClick() {
        int[] result = craft.getResult();
        if (result == null) return;

        int resultId  = result[0];
        int resultQty = result[1];

        boolean canTake = (heldId == 0)
                       || (heldId == resultId && heldQty + resultQty <= 64);
        if (!canTake) return;

        heldId  = resultId;
        heldQty = (heldId == resultId ? heldQty : 0) + resultQty;

        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                if (craft.getSlot(r, c) != 0) {
                    craft.setSlot(r, c, 0);
                }
            }
        }
    }

    public void onClose() {
        if (heldId != 0) {
            inventory.addItem(heldId, heldQty);
            heldId = 0;
            heldQty = 0;
        }
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                int id = craft.getSlot(r, c);
                if (id != 0) {
                    inventory.addItem(id, 1);
                    craft.setSlot(r, c, 0);
                }
            }
        }
    }

    private void drawRect(int x, int y, int w, int h,
                           float r, float g, float b, float a) {
        addQuad(x, y, x + w, y + h, 0, 0, 1, 1, r, g, b, a);
    }

    private void drawBlockIcon(Block block, int x, int y, int size) {
        if (block == null || block == Block.AIR) return;
        float ts = 1f / 16f;
        float u0 = block.texCol * ts, v0 = block.texRow * ts;
        addQuad(x, y, x + size, y + size, u0, v0, u0 + ts, v0 + ts, 1, 1, 1, 1);
    }

    private void addQuad(float x0, float y0, float x1, float y1,
                          float u0, float v0, float u1, float v1,
                          float r, float g, float b, float a) {
        if (quadCount >= MAX_QUADS) return;

        vBuf.put(new float[]{
            x0, y1,  u0, v1,  r, g, b, a,
            x1, y1,  u1, v1,  r, g, b, a,
            x1, y0,  u1, v0,  r, g, b, a,
            x0, y0,  u0, v0,  r, g, b, a
        });

        int base = quadCount * 4;
        iBuf.put(new int[]{ base, base+1, base+2,  base+2, base+3, base });
        quadCount++;
    }

    private void initGPU() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER,
                (long) MAX_QUADS * 4 * VERT_FLOATS * Float.BYTES, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER,
                (long) MAX_QUADS * 6 * Integer.BYTES, GL_DYNAMIC_DRAW);

        int stride = VERT_FLOATS * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 8L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 4, GL_FLOAT, false, stride, 16L);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
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

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vBuf);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, iBuf);

        shader.setInt("uUseTexture", useTexture ? 1 : 0);
        glDrawElements(GL_TRIANGLES, quadCount * 6, GL_UNSIGNED_INT, 0L);

        glBindVertexArray(0);
    }

    private static boolean hitTest(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}