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
        return BORDER * 2 + 9 * S - PAD + S;
    }

    protected int panelH() {
        int craftSectionH = getTopRows() * S + 8;
        int invSectionH = 3 * S + PAD + SLOT_PX;
        return BORDER * 2 + craftSectionH + invSectionH;
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

    private int armorSlotX() { 
        return panelX() + BORDER + 9 * S; 
    }

    private int armorSlotY(int slot) {
        return invAreaY() + slot * (SLOT_PX + PAD);
    }

    public void render() {
        beginRender();

        beginBatch();

        drawPanel(); 
        drawInventorySlots();
    
        drawArmorSlots(); 

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
        
        drawArmorIcons(); 

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
            
            int ax = slotX(i);
            int ay = slotY(i);
            
            drawSlotBg(ax, ay, isSelected);

            int id = inventory.getItemId(i);
            if (id != 0) {
                int durVal = inventory.getItemDurability(i);
                int maxDur = Inventory.getMaxDurability(id);

                if (maxDur > 0 && durVal < maxDur) {
                    float pct  = (float) durVal / maxDur;
                    
                    float r    = pct < 0.5f ? 1f : 1f - (pct - 0.5f) * 2;
                    float g    = pct > 0.5f ? 1f : pct * 2;
                    
                    addRect(ax + 2, ay + SLOT_PX - 4, SLOT_PX - 4, 2, 0f, 0f, 0f, 0.8f);
                    addRect(ax + 2, ay + SLOT_PX - 4, (int)((SLOT_PX - 4) * pct), 2, r, g, 0f, 1f);
                }
            }
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

    private void drawArmorSlots() {
        for (int i = 0; i < 4; i++) {
            int ax = armorSlotX(), ay = armorSlotY(i);
            
            drawSlotBg(ax, ay, false);
            
            if (inventory.getArmorId(i) == 0) {
                float alpha = 0.25f;
                float c = 0.55f;
                
                switch (i) {
                    case 0 -> {
                        addRect(ax + 13, ay + 11, 10, 8,  c, c, c, alpha);
                        addRect(ax + 11, ay + 14, 2,  8,  c, c, c, alpha);
                        addRect(ax + 23, ay + 14, 2,  8,  c, c, c, alpha);
                        addRect(ax + 15, ay + 19, 6,  3,  c, c, c, alpha);
                    }
                    case 1 -> {
                        addRect(ax + 11, ay + 10, 4,  4,  c, c, c, alpha);
                        addRect(ax + 21, ay + 10, 4,  4,  c, c, c, alpha);
                        addRect(ax + 11, ay + 14, 14, 10, c, c, c, alpha);
                        addRect(ax + 9,  ay + 14, 2,  6,  c, c, c, alpha);
                        addRect(ax + 25, ay + 14, 2,  6,  c, c, c, alpha);
                    }
                    case 2 -> {
                        addRect(ax + 11, ay + 10, 14, 4,  c, c, c, alpha);
                        addRect(ax + 11, ay + 14, 5,  12, c, c, c, alpha);
                        addRect(ax + 20, ay + 14, 5,  12, c, c, c, alpha); 
                    }
                    case 3 -> {
                        addRect(ax + 10, ay + 13, 5,  7,  c, c, c, alpha);
                        addRect(ax + 21, ay + 13, 5,  7,  c, c, c, alpha);
                        addRect(ax + 8,  ay + 18, 7,  4,  c, c, c, alpha);
                        addRect(ax + 21, ay + 18, 7,  4,  c, c, c, alpha);
                    }
                }
            }
            
            int durVal = inventory.getArmorDur(i);
            int arId   = inventory.getArmorId(i);
            if (arId != 0) {
                int maxDur = Block.fromId(arId).getArmorMaxDurability();
                if (durVal < maxDur) {
                    float pct  = (float)durVal / maxDur;
                    float r    = pct < 0.5f ? 1f : 1f - (pct-0.5f)*2;
                    float g    = pct > 0.5f ? 1f : pct*2;
                    
                    addRect(ax + 2, ay + SLOT_PX - 4, SLOT_PX - 4, 2, 0f, 0f, 0f, 0.8f);
                    addRect(ax + 2, ay + SLOT_PX - 4, (int)((SLOT_PX - 4) * pct), 2, r, g, 0f, 1f);
                }
            }
        }
    }

    private void drawArmorIcons() {
        for (int i = 0; i < 4; i++) {
            int id = inventory.getArmorId(i);
            if (id != 0) {
                drawBlockIcon(Block.fromId(id),
                    armorSlotX()+2, armorSlotY(i)+2, SLOT_PX-4);
            }
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

        for (int i = 0; i < 4; i++) {
            if (hit(mx, my, armorSlotX(), armorSlotY(i), SLOT_PX, SLOT_PX)) {
                handleArmorSlotClick(i);
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

    private void handleArmorSlotClick(int slotIdx) {
        int equippedId  = inventory.getArmorId(slotIdx);
        int equippedDur = inventory.getArmorDur(slotIdx);

        if (heldId == 0) {
            if (equippedId != 0) {
                heldId  = equippedId;
                heldQty = 1;
                heldDur = equippedDur;
                inventory.unequipArmor(slotIdx);
            }
        } else {
            Block heldBlock = Block.fromId(heldId);
            if (heldBlock.getArmorSlot() == slotIdx) {
                if (equippedId != 0) {
                    if (heldQty == 1) {
                        int tempId  = equippedId;
                        int tempDur = equippedDur;
                        inventory.equipArmor(heldId, heldDur);
                        heldId  = tempId;
                        heldQty = 1;
                        heldDur = tempDur;
                    }
                } else {
                    inventory.equipArmor(heldId, heldDur);
                    
                    if (heldQty > 1) {
                        heldQty--;
                    } else {
                        heldId = 0; heldQty = 0; heldDur = -1;
                    }
                }
            }
        }
    }

    @Override
    public void onClose() {
        returnHeldItem(inventory);
        craft.returnToInventory(inventory);
    }

}