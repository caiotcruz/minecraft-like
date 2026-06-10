package com.mcraft.ui;

import com.mcraft.render.Shader;
import com.mcraft.render.TextureAtlas;
import com.mcraft.world.Block;

public class InventoryScreen extends Screen2D {

    protected final Inventory inventory;
    private CraftingGrid craft;

    public InventoryScreen(int sw, int sh, Inventory inventory,
                            Shader shader, TextureAtlas atlas, float[] ortho) {
        super(sw, sh, shader, atlas, ortho);
        this.inventory = inventory;
        this.craft     = new CraftingGrid(2);
    }

    protected int panelW() {
        return BORDER * 2 + 9 * S - PAD;
    }

    protected int panelH() {
        return BORDER * 2 + getTopRows() * S + 8 + 3 * S + PAD + SLOT_PX;
    }

    protected int panelX() { return (sw - panelW()) / 2; }
    protected int panelY() { return (sh - panelH()) / 2; }

    protected int getTopRows() {
        return 2;
    }

    private int craftX(int col) { return panelX() + BORDER + col * S; }
    private int craftY(int row) { return panelY() + BORDER + row * S; }

    private int arrowX() { return craftX(2) + 4; }
    private int arrowY() { return craftY(0) + SLOT_PX / 2 - 4; }

    private int resultX() { return arrowX() + 30; }
    private int resultY() { return craftY(0) + (2 * S - PAD - SLOT_PX) / 2; }

    private int invAreaY() {
        return panelY() + BORDER + getTopRows() * S - PAD + 8;  
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

    @Override
    public void render() {
        beginRender();

        beginBatch();

        drawPanel(); 
        drawInventorySlots();
        if (heldId != 0) {
            addRect(mouseX - SLOT_PX / 2, mouseY - SLOT_PX / 2,
                    SLOT_PX, SLOT_PX, 0.5f, 0.5f, 0.5f, 0.75f);
        }
        drawTopSectionGeometry(); 
        drawHeldItem();
        flushBatch(false);

        atlas.bind(0);
        beginBatch();
        drawInventoryIcons();
        drawTopSectionIcons();
        drawHeldItemIcon();
        drawSlotCounts();
        flushBatch(true);

        endRender();
    }

    private void drawPanel() {
        addRect(panelX(), panelY(), panelW(), panelH(),
                0.12f, 0.12f, 0.12f, 0.93f);

        int sepY = invAreaY() - 5;
        addRect(panelX() + BORDER, sepY, panelW() - BORDER * 2, 2,
                0.55f, 0.55f, 0.55f, 0.6f);

        int hotSepY = slotY(0) - 5;
        addRect(panelX() + BORDER, hotSepY, panelW() - BORDER * 2, 2,
                0.55f, 0.55f, 0.55f, 0.6f);
    }

    protected void drawTopSectionGeometry() {
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                drawSlotBg(craftX(c), craftY(r), false);
            }
        }

        int ax = arrowX(), ay = arrowY();
        addRect(ax,      ay + 3, 20, 4,  0.85f, 0.85f, 0.1f, 1f);
        addRect(ax + 14, ay,     6,  10, 0.85f, 0.85f, 0.1f, 1f);  

        int[] result = craft.getResult();
        drawSlotBg(resultX(), resultY(), false);
        if (result != null) {
            addRect(resultX(), resultY(), SLOT_PX, SLOT_PX,
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

    private void drawInventoryIcons() {
        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            int id = inventory.getItemId(i);
            if (id == 0) continue;  
            
            drawBlockIcon(Block.fromId(id), slotX(i) + 2, slotY(i) + 2, SLOT_PX - 4);
        }
    }

    protected void drawTopSectionIcons() {
        for (int i = 0; i < craft.size * craft.size; i++) {
            int id  = craft.getSlotId(i);
            int qty = craft.getSlotQty(i);
            if (id == 0) continue;

            int row = i / craft.size;
            int col = i % craft.size;
            int cx = craftX(col);
            int cy = craftY(row);

            drawBlockIcon(Block.fromId(id), cx + 2, cy + 2, SLOT_PX - 4);
            
            if (qty > 1) {
                int nw = PixelFont.measureWidth(qty) * 2 + 2;
                PixelFont.drawIntShadow(this::addRect, cx + SLOT_PX - nw - 2, cy + SLOT_PX - 12, 2, qty, 1f, 1f, 1f);
            }
        }

        int[] result = craft.getResult();
        if (result != null && result[0] != 0) {
            drawBlockIcon(Block.fromId(result[0]), resultX() + 2, resultY() + 2, SLOT_PX - 4);
            int resultQty = craft.getResultQty();
            if (resultQty > 1) {
                int nw = PixelFont.measureWidth(resultQty) * 2 + 2;
                PixelFont.drawIntShadow(this::addRect, resultX() + SLOT_PX - nw - 2, resultY() + SLOT_PX - 12, 2, resultQty, 1f, 1f, 1f);
            }
        }
    }

    private void drawSlotCounts() {
        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            int qty = inventory.getItemQty(i);
            if (qty <= 1) continue;

            int qx = slotX(i) + SLOT_PX - (
                PixelFont.measureWidth(qty) * 2 + 2
            ) - 3;

            int qy = slotY(i) + SLOT_PX - 13;

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

    protected boolean isDefaultCraftActive() {
        return true; 
    }

    @Override
    public boolean onClick(int mx, int my) {
        return onClick(mx, my, false);
    }

    public boolean onClick(int mx, int my, boolean isRightClick) {
        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            if (hit(mx, my, slotX(i), slotY(i), SLOT_PX, SLOT_PX)) {
                if (isRightClick && heldId == 0 && inventory.getItemId(i) != 0 && !Inventory.isTool(inventory.getItemId(i))) {
                    int currentQty = inventory.getItemQty(i);
                    int takeQty = (currentQty + 1) / 2;
                    heldId = inventory.getItemId(i);
                    heldQty = takeQty;
                    heldDur = inventory.getItemDurability(i);
                    
                    int remaining = currentQty - takeQty;
                    if (remaining <= 0) {
                        inventory.clearSlot(i);
                    } else {
                        inventory.setSlot(i, heldId, remaining);
                    }
                } else {
                    handleInvSlotClick(inventory, i);
                }
                return true;
            }
        }

        if (isDefaultCraftActive()) {
            for (int i = 0; i < craft.size * craft.size; i++) {
                int row = i / craft.size;
                int col = i % craft.size;
                if (hit(mx, my, craftX(col), craftY(row), SLOT_PX, SLOT_PX)) {
                    handleCraftSlotClick(craft, i, isRightClick);
                    return true;
                }
            }

            if (hit(mx, my, resultX(), resultY(), SLOT_PX, SLOT_PX)) {
                if (!isRightClick) {
                    handleResultSlotClick(craft, inventory);
                }
                return true;
            }
        }

        if (hit(mx, my, panelX(), panelY(), panelW(), panelH())) {
        return true;
        }

        return false;
    }

    @Override
    public void onClose() {
        returnHeldItem(inventory);
        craft.returnToInventory(inventory);
    }

}