package com.mcraft.ui;

public class Inventory {

    public static final int HOTBAR_SIZE = 9;
    public static final int TOTAL_SLOTS = 36; // 4 linhas × 9 colunas + hotbar

    private final int[] itemId  = new int[TOTAL_SLOTS];
    private final int[] itemQty = new int[TOTAL_SLOTS];
    private int selectedSlot = 0;

    /** Tenta empilhar o item em um slot existente; depois ocupa slot vazio. */
    public void addItem(int blockId, int qty) {
        // 1. Tenta empilhar
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (itemId[i] == blockId && itemQty[i] < 64) {
                int add = Math.min(qty, 64 - itemQty[i]);
                itemQty[i] += add;
                qty -= add;
                if (qty <= 0) return;
            }
        }
        // 2. Slot vazio
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (itemId[i] == 0) {
                itemId[i]  = blockId;
                itemQty[i] = Math.min(qty, 64);
                return;
            }
        }
        // Inventário cheio — descarta o excesso
    }

    public void removeItem(int blockId, int qty) {
        for (int i = 0; i < TOTAL_SLOTS && qty > 0; i++) {
            if (itemId[i] == blockId) {
                int remove = Math.min(qty, itemQty[i]);
                itemQty[i] -= remove;
                qty -= remove;
                if (itemQty[i] <= 0) itemId[i] = 0;
            }
        }
    }

    public int getSelectedBlockId() { return itemId[selectedSlot]; }

    public void consumeSelected(int qty) {
        itemQty[selectedSlot] -= qty;
        if (itemQty[selectedSlot] <= 0) {
            itemQty[selectedSlot] = 0;
            itemId[selectedSlot]  = 0;
        }
    }

    public void scrollHotbar(int delta) {
        selectedSlot = ((selectedSlot + delta) % HOTBAR_SIZE + HOTBAR_SIZE) % HOTBAR_SIZE;
    }

    public int getItemId (int slot) { return itemId [slot]; }
    public int getItemQty(int slot) { return itemQty[slot]; }
    public int getSelectedSlot()    { return selectedSlot; }
    public void setSelectedSlot(int s) {
        if (s >= 0 && s < HOTBAR_SIZE) selectedSlot = s;
    }
}