package com.mcraft.ui;

import com.mcraft.render.Shader;
import com.mcraft.render.TextureAtlas;
import com.mcraft.world.Block;

public class ChestScreen extends Screen2D {

    private static final int TITLE_H = 14;
    private static final int SEP_H   = 6;

    private Inventory chestInv;            
    private final Inventory playerInv;

    public ChestScreen(int sw, int sh, Inventory playerInv,
                        Shader shader, TextureAtlas atlas, float[] ortho) {
        super(sw, sh, shader, atlas, ortho);
        this.playerInv = playerInv;
    }

    public void openFor(Inventory chestInventory) {
        this.chestInv = chestInventory;
        heldId  = 0;
        heldQty = 0;
    }

    private int panelW() { return BORDER * 2 + 9 * S - PAD; }

    private int panelH() {
        return BORDER * 2
             + TITLE_H + 3 * S
             + SEP_H
             + TITLE_H + 3 * S + PAD + SLOT_PX;
    }

    private int panelX() { return (sw - panelW()) / 2; }
    private int panelY() { return (sh - panelH()) / 2; }

    private int chestSlotX(int idx) {
        return panelX() + BORDER + (idx % 9) * S;
    }

    private int chestSlotY(int idx) {
        int row = idx / 9;
        return panelY() + BORDER + TITLE_H + row * S;
    }

    private int invSectionY() {
        return panelY() + BORDER + TITLE_H + 3 * S + SEP_H;
    }

    private int playerSlotX(int idx) {
        int col = (idx < 9) ? idx : (idx - 9) % 9;
        return panelX() + BORDER + col * S;
    }

    private int playerSlotY(int idx) {
        int secY = invSectionY();
        if (idx < 9) {
            return secY + TITLE_H + 3 * S + PAD;
        }
        int row = (idx - 9) / 9;
        return secY + TITLE_H + row * S;
    }

    @Override
    public void render() {
        if (chestInv == null) return;

        beginRender();

        beginBatch();
        drawPanel();
        drawChestSlots();
        drawPlayerSlots();
        drawHeldItem();
        flushBatch(false);

        atlas.bind(0);
        beginBatch();
        drawChestIcons();
        drawPlayerIcons();
        drawHeldItemIcon();
        flushBatch(true);

        endRender();
    }

    private void drawPanel() {
        addRect(panelX(), panelY(), panelW(), panelH(),
                0.12f, 0.12f, 0.12f, 0.93f);

        int sepY = invSectionY() - 3;
        drawSeparator(panelX() + BORDER, sepY, panelW() - BORDER * 2);

        int hotSepY = playerSlotY(0) - 3;
        drawSeparator(panelX() + BORDER, hotSepY, panelW() - BORDER * 2);
    }

    private void drawChestSlots() {
        for (int i = 0; i < 27; i++) {
            drawSlotBg(chestSlotX(i), chestSlotY(i), false);
        }
    }

    private void drawChestIcons() {
        for (int i = 0; i < 27; i++) {
            int id = chestInv.getItemId(i);
            if (id == 0) continue;
            drawBlockIcon(Block.fromId(id), chestSlotX(i)+2, chestSlotY(i)+2, SLOT_PX-4);
            drawSlotQty(chestInv, i, chestSlotX(i), chestSlotY(i));
        }
    }

    private void drawPlayerSlots() {
        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            boolean sel = (i < Inventory.HOTBAR_SIZE)
                       && (i == playerInv.getSelectedSlot());
            drawSlotBg(playerSlotX(i), playerSlotY(i), sel);
        }
    }

    private void drawPlayerIcons() {
        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            int id = playerInv.getItemId(i);
            if (id == 0) continue;
            drawBlockIcon(Block.fromId(id), playerSlotX(i)+2, playerSlotY(i)+2, SLOT_PX-4);
            drawSlotQty(playerInv, i, playerSlotX(i), playerSlotY(i));
        }
    }

    @Override
    public boolean onClick(int mx, int my) {
       return onClick(mx, my, false);
    }
    
    public boolean onClick(int mx, int my, boolean isRightClick) {
        for (int i = 0; i < 27; i++) {
            if (hit(mx, my, chestSlotX(i), chestSlotY(i), SLOT_PX, SLOT_PX)) {
                int slotId  = chestInv.getItemId(i);
                int slotQty = chestInv.getItemQty(i);
                int slotDur = chestInv.getItemDurability(i);

                if (isRightClick) {
                    if (heldId == 0) {
                        if (slotId != 0 && !Inventory.isTool(slotId)) {
                            int takeQty = (slotQty + 1) / 2;
                            heldId  = slotId;
                            heldQty = takeQty;
                            heldDur = slotDur;
                            
                            int remaining = slotQty - takeQty;
                            if (remaining <= 0) chestInv.clearSlot(i);
                            else chestInv.setSlotFull(i, slotId, remaining, slotDur);
                        } else if (slotId != 0) { 
                            heldId = slotId; heldQty = 1; heldDur = slotDur;
                            chestInv.clearSlot(i);
                        }
                    } else {
                        if (slotId == 0) {
                            chestInv.setSlotFull(i, heldId, 1, heldDur);
                            heldQty--;
                            if (heldQty <= 0) { heldId = 0; heldQty = 0; heldDur = -1; }
                        } else if (slotId == heldId && slotQty < 64 && !Inventory.isTool(heldId)) {
                            chestInv.setSlot(i, slotId, slotQty + 1);
                            heldQty--;
                            if (heldQty <= 0) { heldId = 0; heldQty = 0; heldDur = -1; }
                        }
                    }
                } else {
                    handleInvSlotClick(chestInv, i);
                }
                return true;
            }
        }

        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            if (hit(mx, my, playerSlotX(i), playerSlotY(i), SLOT_PX, SLOT_PX)) {
                int slotId  = playerInv.getItemId(i);
                int slotQty = playerInv.getItemQty(i);
                int slotDur = playerInv.getItemDurability(i);

                if (isRightClick) {
                    if (heldId == 0) {
                        if (slotId != 0 && !Inventory.isTool(slotId)) {
                            int takeQty = (slotQty + 1) / 2;
                            heldId  = slotId;
                            heldQty = takeQty;
                            heldDur = slotDur;
                            
                            int remaining = slotQty - takeQty;
                            if (remaining <= 0) playerInv.clearSlot(i);
                            else playerInv.setSlotFull(i, slotId, remaining, slotDur);
                        } else if (slotId != 0) {
                            heldId = slotId; heldQty = 1; heldDur = slotDur;
                            playerInv.clearSlot(i);
                        }
                    } else {
                        if (slotId == 0) {
                            playerInv.setSlotFull(i, heldId, 1, heldDur);
                            heldQty--;
                            if (heldQty <= 0) { heldId = 0; heldQty = 0; heldDur = -1; }
                        } else if (slotId == heldId && slotQty < 64 && !Inventory.isTool(heldId)) {
                            playerInv.setSlot(i, slotId, slotQty + 1);
                            heldQty--;
                            if (heldQty <= 0) { heldId = 0; heldQty = 0; heldDur = -1; }
                        }
                    }
                } else {
                    handleInvSlotClick(playerInv, i);
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
        returnHeldItem(playerInv);
    }
}