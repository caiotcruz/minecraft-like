package com.mcraft.ui;

import com.mcraft.render.Shader;
import com.mcraft.render.TextureAtlas;
import com.mcraft.world.Block;

public class CraftingScreen extends InventoryScreen {

    private final CraftingGrid craft3x3 = new CraftingGrid(3);

    public CraftingScreen(int sw, int sh, Inventory inventory,
                          Shader shader, TextureAtlas atlas, float[] ortho) {
        super(sw, sh, inventory, shader, atlas, ortho);
    }

    @Override
    protected boolean isDefaultCraftActive() {
        return false; 
    }

    @Override
    protected void drawTopSectionGeometry() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                drawSlotBg(craft3X(c), craft3Y(r), false);
            }
        }

        int ax = craft3X(2) + SLOT_PX + 12;
        int ay = craft3Y(1) + SLOT_PX / 2 - 5;

        addRect(ax,      ay + 3, 20, 4,  0.85f, 0.85f, 0.1f, 1f);
        addRect(ax + 14, ay,     6, 10, 0.85f, 0.85f, 0.1f, 1f);

        drawSlotBg(result3X(), result3Y(), false);

        int[] result = craft3x3.getResult();
        if (result != null) {
            addRect(result3X(), result3Y(), SLOT_PX, SLOT_PX, 0.3f, 0.7f, 0.3f, 0.35f);
        }
    }

    @Override
    protected void drawTopSectionIcons() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {

                int id  = craft3x3.getSlot(r, c);
                int qty = craft3x3.getSlotQty(r, c);

                if (id == 0) continue;

                int sx = craft3X(c);
                int sy = craft3Y(r);

                drawBlockIcon( Block.fromId(id), sx + 2, sy + 2, SLOT_PX - 4 );

                if (qty > 1) {
                    int nw = PixelFont.measureWidth(qty) * 2 + 2;
                    PixelFont.drawIntShadow( this::addRect, sx + SLOT_PX - nw - 2, sy + SLOT_PX - 12, 2, qty, 1f, 1f, 1f );
                }
            }
        }

        int[] result = craft3x3.getResult();

        if (result != null && result[0] != 0) {

            drawBlockIcon( Block.fromId(result[0]), result3X() + 2, result3Y() + 2, SLOT_PX - 4 );

            int resultQty = craft3x3.getResultQty();

            if (resultQty > 1) {
                int nw = PixelFont.measureWidth(resultQty) * 2 + 2;
                PixelFont.drawIntShadow( this::addRect, result3X() + SLOT_PX - nw - 2, result3Y() + SLOT_PX - 12, 2, resultQty, 1f, 1f, 1f );
            }
        }
    }

    @Override
    public boolean onClick(int mx, int my) {
        return onClick(mx, my, false);
    }

    public boolean onClick(int mx, int my, boolean isRightClick) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (hit(mx, my, craft3X(c), craft3Y(r), SLOT_PX, SLOT_PX)) {
                    int linearIndex = r * 3 + c;
                    handleCraftSlotClick(craft3x3, linearIndex, isRightClick);
                    return true;
                }
            }
        }

        if (hit(mx, my, result3X(), result3Y(), SLOT_PX, SLOT_PX)) {
            if (!isRightClick) {
                handleResultSlotClick(craft3x3, inventory);
            }
            return true;
        }

        return super.onClick(mx, my, isRightClick);
    }

    @Override
    protected int getTopRows() {
        return 3;
    }

    private int craft3X(int col) { 
        return panelX() + BORDER + col * S; 
    }

    private int craft3Y(int row) { 
        return panelY() + BORDER + row * S; 
    }

    private int result3X() { 
        return craft3X(3) + 40; 
    }

    private int result3Y() { 
        return craft3Y(1); 
    }

    @Override
    public void onClose() {
        super.onClose(); 
        craft3x3.returnToInventory(inventory);
    }
}