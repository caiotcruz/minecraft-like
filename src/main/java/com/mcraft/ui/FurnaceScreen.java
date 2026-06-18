package com.mcraft.ui;

import com.mcraft.render.Shader;
import com.mcraft.render.TextureAtlas;
import com.mcraft.world.Block;
import com.mcraft.world.FurnaceState;

public class FurnaceScreen extends Screen2D {

    private FurnaceState state;
    private final Inventory playerInv;

    private static final int FURNACE_AREA_W = 176;
    private static final int FURNACE_AREA_H = 72;
    private static final int TITLE_H  = 14;
    private static final int SEP_H    = 6;

    private static final int INV_ROWS = 3;

    public FurnaceScreen(int sw, int sh, Inventory playerInv,
                          Shader shader, TextureAtlas atlas, float[] ortho) {
        super(sw, sh, shader, atlas, ortho);
        this.playerInv = playerInv;
    }

    public void openFor(FurnaceState furnaceState) {
        this.state = furnaceState;
        heldId = 0; heldQty = 0; heldDur = -1;
    }

    private int panelW() { return BORDER * 2 + 9 * S - PAD; }
    private int panelH() {
        return BORDER * 2
             + TITLE_H + FURNACE_AREA_H 
             + SEP_H
             + TITLE_H + INV_ROWS * S + PAD + SLOT_PX;
    }
    private int panelX() { return (sw - panelW()) / 2; }
    private int panelY() { return (sh - panelH()) / 2; }
    private int furnAreaX() { return panelX() + BORDER; }
    private int furnAreaY() { return panelY() + BORDER + TITLE_H; }

    private int inputSlotX()  { return furnAreaX(); }
    private int inputSlotY()  { return furnAreaY(); }
    private int fuelSlotX()   { return furnAreaX(); }
    private int fuelSlotY()   { return furnAreaY() + SLOT_PX + 4; }
    private int outputSlotX() { return furnAreaX() + FURNACE_AREA_W - SLOT_PX; }
    private int outputSlotY() { return furnAreaY() + SLOT_PX / 2 - SLOT_PX / 2; }

    private int progressX()   { return furnAreaX() + SLOT_PX + 8; }
    private int progressY()   { return furnAreaY() + 8; }
    private int progressW()   { return FURNACE_AREA_W - SLOT_PX * 2 - 20; }
    private int progressH()   { return 20; }

    private int flameX()      { return furnAreaX() + SLOT_PX + 8; }
    private int flameY()      { return furnAreaY() + SLOT_PX + 4; }

    private int invSectionY() { return furnAreaY() + FURNACE_AREA_H + SEP_H; }
    private int playerSlotX(int idx) {
        int col = (idx < 9) ? idx : (idx - 9) % 9;
        return panelX() + BORDER + col * S;
    }
    private int playerSlotY(int idx) {
        int secY = invSectionY() + TITLE_H;
        if (idx < 9) return secY + INV_ROWS * S + PAD;
        return secY + ((idx - 9) / 9) * S;
    }

    @Override
    public void render() {
        if (state == null) return;
        beginRender();

        beginBatch();
        drawPanel();
        drawFurnaceSlots();
        drawProgressBar();
        drawFlameIcon();
        drawPlayerSlots();
        drawHeldItem();
        flushBatch(false);

        atlas.bind(0);
        beginBatch();
        drawFurnaceIcons();
        drawPlayerIcons();
        drawHeldItemIcon();
        flushBatch(true);

        beginBatch();
        drawFurnaceCounts();
        drawPlayerCounts();
        flushBatch(false);

        endRender();
    }


    private void drawFurnaceCounts() {
        if (state.inputId != 0 && state.inputQty > 1) {
            int nw = PixelFont.measureWidth(state.inputQty) * 2 + 2;
            PixelFont.drawIntShadow(this::addRect, inputSlotX() + SLOT_PX - nw - 2, inputSlotY() + SLOT_PX - 12, 2, state.inputQty, 1f, 1f, 1f);
        }
        if (state.fuelId != 0 && state.fuelQty > 1) {
            int nw = PixelFont.measureWidth(state.fuelQty) * 2 + 2;
            PixelFont.drawIntShadow(this::addRect, fuelSlotX() + SLOT_PX - nw - 2, fuelSlotY() + SLOT_PX - 12, 2, state.fuelQty, 1f, 1f, 1f);
        }
        if (state.outputId != 0 && state.outputQty > 1) {
            int nw = PixelFont.measureWidth(state.outputQty) * 2 + 2;
            PixelFont.drawIntShadow(this::addRect, outputSlotX() + SLOT_PX - nw - 2, outputSlotY() + SLOT_PX - 12, 2, state.outputQty, 1f, 1f, 1f);
        }
    }

    private void drawPlayerCounts() {
        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            int qty = playerInv.getItemQty(i);
            if (qty > 1) {
                drawSlotQty(playerInv, i, playerSlotX(i), playerSlotY(i));
            }
        }
    }

    private void drawPanel() {
        addRect(panelX(), panelY(), panelW(), panelH(), 0.12f, 0.12f, 0.12f, 0.93f);

        int sepY = invSectionY() - 3;
        drawSeparator(panelX() + BORDER, sepY, panelW() - BORDER * 2);

        drawSeparator(panelX() + BORDER, playerSlotY(0) - 3, panelW() - BORDER * 2);
    }

    private void drawFurnaceSlots() {
        drawSlotBg(inputSlotX(),  inputSlotY(),  false);
        drawSlotBg(fuelSlotX(),   fuelSlotY(),   false);
        drawSlotBg(outputSlotX(), outputSlotY(), false);
    }

    private void drawFurnaceIcons() {
        if (state.inputId  != 0){
            drawBlockIcon(Block.fromId(state.inputId), inputSlotX()+2,  inputSlotY()+2,  SLOT_PX-4);
        } 
        if (state.fuelId   != 0){
            drawBlockIcon(Block.fromId(state.fuelId), fuelSlotX()+2,   fuelSlotY()+2,   SLOT_PX-4);
        }
        if (state.outputId != 0) drawBlockIcon(Block.fromId(state.outputId), outputSlotX()+2, outputSlotY()+2, SLOT_PX-4);
    }

    private void drawProgressBar() {
        int pw = progressW(), ph = progressH();
        int px = progressX(), py = progressY();

        addRect(px, py, pw, ph, 0.25f, 0.25f, 0.25f, 0.85f);

        int fill = (int)(pw * Math.min(1f, state.smeltProgress));
        if (fill > 0) {
            addRect(px, py, fill, ph, 0.85f, 0.55f, 0.10f, 1f);
        }

        int arrowMid = py + ph / 2;
        addRect(px + pw - 6, arrowMid - 5, 6, 10, 0.65f, 0.65f, 0.65f, 0.6f);
        addRect(px + pw - 3, arrowMid - 8, 3, 16, 0.65f, 0.65f, 0.65f, 0.6f);
    }

    private void drawFlameIcon() {
        int fx = flameX(), fy = flameY();
        int fw = 14, fh = 14;

        addRect(fx, fy, fw, fh, 0.15f, 0.15f, 0.15f, 0.8f);

        if (state.isLit()) {
            float fuelMax = 80f;
            float ratio = Math.min(1f, state.fuelRemaining / fuelMax);
            int flameH = Math.max(2, (int)(fh * ratio));

            addRect(fx + 2, fy + fh - flameH, fw - 4, flameH, 1.0f, 0.5f, 0.0f, 1f);
            addRect(fx + 4, fy + fh - flameH, fw - 8, flameH / 2, 1.0f, 0.9f, 0.0f, 1f);
        }
    }

    private void drawPlayerSlots() {
        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            boolean sel = (i < Inventory.HOTBAR_SIZE) && (i == playerInv.getSelectedSlot());
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
        if (hit(mx, my, inputSlotX(), inputSlotY(), SLOT_PX, SLOT_PX)) {
            handleFurnaceInputClick(isRightClick);
            return true;
        }

        if (hit(mx, my, fuelSlotX(), fuelSlotY(), SLOT_PX, SLOT_PX)) {
            handleFurnaceFuelClick(isRightClick);
            return true;
        }

        if (hit(mx, my, outputSlotX(), outputSlotY(), SLOT_PX, SLOT_PX)) {
            if (!isRightClick) {
                handleFurnaceOutputClick();
            }
            return true;
        }

        for (int i = 0; i < Inventory.TOTAL_SLOTS; i++) {
            if (hit(mx, my, playerSlotX(i), playerSlotY(i), SLOT_PX, SLOT_PX)) {
                if (isRightClick && heldId == 0 && playerInv.getItemId(i) != 0 && !Inventory.isTool(playerInv.getItemId(i))) {
                    int currentQty = playerInv.getItemQty(i);
                    int takeQty = (currentQty + 1) / 2;
                    heldId = playerInv.getItemId(i);
                    heldQty = takeQty;
                    heldDur = playerInv.getItemDurability(i);
                    
                    int remaining = currentQty - takeQty;
                    if (remaining <= 0) {
                        playerInv.clearSlot(i);
                    } else {
                        playerInv.setSlot(i, heldId, remaining);
                    }
                } else {
                    handleInvSlotClick(playerInv, i);
                }
                return true;
            }
        }

        if (hit(mx, my, panelX(), panelY(), panelW(), panelH())) return true;

        return false;
    }

    private void handleFurnaceInputClick(boolean isRightClick) {
        if (heldId == 0) {
            if (state.inputId != 0) {
                state.smeltProgress = 0f;

                if (isRightClick) {
                    int takeQty = (state.inputQty + 1) / 2; 
                    heldId = state.inputId;
                    heldQty = takeQty;
                    heldDur = -1;
                    
                    state.inputQty -= takeQty;
                    if (state.inputQty <= 0) {
                        state.inputId = 0;
                        state.inputQty = 0;
                    }
                } else {
                    heldId = state.inputId; 
                    heldQty = state.inputQty; 
                    heldDur = -1;
                    state.inputId = 0; 
                    state.inputQty = 0;
                }
            }
        } 
        else {
            if (state.inputId == 0) {
                if (isRightClick) {
                    state.inputId = heldId;
                    state.inputQty = 1;
                    heldQty--;
                    if (heldQty <= 0) { heldId = 0; heldQty = 0; heldDur = -1; }
                } else {
                    state.inputId  = heldId;
                    state.inputQty = heldQty;
                    heldId = 0; heldQty = 0; heldDur = -1;
                }
            } 
            else if (state.inputId == heldId && state.inputQty < 64) {
                if (isRightClick) {
                    state.inputQty += 1;
                    heldQty--;
                    if (heldQty <= 0) { heldId = 0; heldQty = 0; heldDur = -1; }
                } else {
                    int add = Math.min(heldQty, 64 - state.inputQty);
                    state.inputQty += add;
                    heldQty -= add;
                    if (heldQty <= 0) { heldId = 0; heldQty = 0; heldDur = -1; }
                }
            }
        }
    }

    private void handleFurnaceFuelClick(boolean isRightClick) {
        if (heldId == 0 && state.fuelId != 0) {
            if (isRightClick) {
                int takeQty = (state.fuelQty + 1) / 2; 
                heldId = state.fuelId;
                heldQty = takeQty;
                heldDur = -1;
                
                state.fuelQty -= takeQty;
                if (state.fuelQty <= 0) {
                    state.fuelId = 0;
                    state.fuelQty = 0;
                }
            } else {
                heldId = state.fuelId; 
                heldQty = state.fuelQty; 
                heldDur = -1;
                state.fuelId = 0; 
                state.fuelQty = 0;
            } 
        } else {
            if (FurnaceState.getFuelValue(heldId) > 0) {
                
                if (state.fuelId == 0) {
                    if (isRightClick) {
                        state.fuelId = heldId;
                        state.fuelQty = 1;
                        heldQty--;
                        if (heldQty <= 0) { heldId = 0; heldQty = 0; heldDur = -1; }
                    } else {
                        state.fuelId  = heldId;
                        state.fuelQty = heldQty;
                        heldId = 0; heldQty = 0; heldDur = -1;
                    }
                } 
                else if (state.fuelId == heldId && state.fuelQty < 64) {
                    if (isRightClick) {
                        state.fuelQty += 1;
                        heldQty--;
                        if (heldQty <= 0) { heldId = 0; heldQty = 0; heldDur = -1; }
                    } else {
                        int add = Math.min(heldQty, 64 - state.fuelQty);
                        state.fuelQty += add;
                        heldQty -= add;
                        if (heldQty <= 0) { heldId = 0; heldQty = 0; heldDur = -1; }
                    }
                }
            }
        }
    }

    private void handleFurnaceOutputClick() {
        if (state.outputId == 0) return;

        if (heldId == 0) {
            heldId  = state.outputId;
            heldQty = state.outputQty;
            heldDur = -1;
            state.outputId = 0; state.outputQty = 0;
        } else if (heldId == state.outputId && heldQty + state.outputQty <= 64) {
            heldQty += state.outputQty;
            state.outputId = 0; state.outputQty = 0;
        }
    }

    @Override
    public void onClose() {
        returnHeldItem(playerInv);
    }
}