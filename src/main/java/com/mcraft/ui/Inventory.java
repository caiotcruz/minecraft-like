package com.mcraft.ui;

import com.mcraft.world.Block;

public class Inventory {

    public static final int HOTBAR_SIZE = 9;
    public static final int TOTAL_SLOTS = 36;

    private final int[] itemId  = new int[TOTAL_SLOTS];
    private final int[] itemQty = new int[TOTAL_SLOTS];
    private final int[] itemDurability = new int[TOTAL_SLOTS];
    private final int[] armorId  = new int[4];
    private final int[] armorDur = new int[4];
    private int selectedSlot = 0;

    public void clearSlot(int index) {
        if (index < 0 || index >= TOTAL_SLOTS) return;
        itemId [index] = 0;
        itemQty[index] = 0;
    }

    public void setSlot(int index, int id, int qty) {
        if (index < 0 || index >= TOTAL_SLOTS) return;
        itemId [index] = id;
        itemQty[index] = qty;
        itemDurability[index] = getMaxDurability(id);
    }

    public void setSlotFull(int index, int id, int qty, int durability) {
        if (index < 0 || index >= TOTAL_SLOTS) return;
        itemId  [index] = id;
        itemQty [index] = qty;
        
        if (durability < 0 && isTool(id)) {
            itemDurability[index] = getMaxDurability(id);
        } else {
            itemDurability[index] = durability;
        }
    }

    public void swapSlots(int a, int b) {
        int tmpId  = itemId [a]; int tmpQty = itemQty[a];
        itemId [a] = itemId [b]; itemQty[a] = itemQty[b];
        itemId [b] = tmpId;      itemQty[b] = tmpQty;
    }

    public int getItemId (int slot) { return itemId [slot]; }
    public int getItemQty(int slot) { return itemQty[slot]; }
    public int getSelectedSlot()    { return selectedSlot; }

    public int  getSelectedBlockId()     { return itemId[selectedSlot]; }
    public void setSelectedSlot(int s)   { if (s >= 0 && s < HOTBAR_SIZE) selectedSlot = s; }
    public void scrollHotbar(int delta)  {
        selectedSlot = ((selectedSlot + delta) % HOTBAR_SIZE + HOTBAR_SIZE) % HOTBAR_SIZE;
    }

    public void consumeSelected(int qty) {
        itemQty[selectedSlot] -= qty;
        if (itemQty[selectedSlot] <= 0) clearSlot(selectedSlot);
    }

    public void addItem(int blockId, int qty) {
        int maxDur = getMaxDurability(blockId);

        if (maxDur >= 0) {
            for (int i = 0; i < TOTAL_SLOTS; i++) {
                if (itemId[i] == 0) {
                    itemId  [i] = blockId;
                    itemQty [i] = 1;
                    itemDurability[i] = maxDur;
                    return;
                }
            }
            return;
        }

        for (int i = 0; i < TOTAL_SLOTS && qty > 0; i++) {
            if (itemId[i] == blockId && itemQty[i] < 64) {
                int add = Math.min(qty, 64 - itemQty[i]);
                itemQty[i] += add; qty -= add;
            }
        }
        for (int i = 0; i < TOTAL_SLOTS && qty > 0; i++) {
            if (itemId[i] == 0) {
                itemId[i] = blockId; itemQty[i] = Math.min(qty, 64); qty = 0;
            }
        }
    }

    public void addToolWithDurability(int blockId, int durability) {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (itemId[i] == 0) {
                itemId         [i] = blockId;
                itemQty        [i] = 1;
                itemDurability [i] = durability;
                return;
            }
        }
    }

    public void removeItem(int blockId, int qty) {
        for (int i = 0; i < TOTAL_SLOTS && qty > 0; i++) {
            if (itemId[i] == blockId) {
                int remove = Math.min(qty, itemQty[i]);
                itemQty[i] -= remove; qty -= remove;
                if (itemQty[i] <= 0) clearSlot(i);
            }
        }
    }

    public void load(int[] items, int[] counts, int[] durability, int selectedSlot, int[] armors, int[] armorsDur) {

        System.arraycopy(items, 0, this.getItems(), 0, items.length);
        System.arraycopy(counts, 0, this.getCounts(), 0, counts.length);
        System.arraycopy(durability, 0, this.getDurabilities(), 0, durability.length);
        System.arraycopy(armors, 0, this.getArmors(), 0, armors.length);
        System.arraycopy(armorsDur, 0, this.getArmorDurabilities(), 0, armorsDur.length);

        this.selectedSlot = selectedSlot;
    }
    
    public void clear() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            clearSlot(i);
        }

        selectedSlot = 0;
    }

    public int[] getItems() {
        return itemId;
    }

    public int[] getCounts() {
        return itemQty;
    }

    public int[] getDurabilities(){
        return itemDurability;
    }

    public int[] getArmors(){
        return armorId;
    }

    public int[] getArmorDurabilities(){
        return armorDur;
    }

    public int  getArmorId (int slot) { return armorId [slot]; }
    public int  getArmorDur(int slot) { return armorDur[slot]; }

    public static int getMaxDurability(int blockId) {
        Block b = Block.fromId(blockId);
        if (b == null) return -1;
        
        if (b.isTool()) return b.getToolMaxDurability();
        
        if (b.isArmor()) return b.getArmorMaxDurability();
        
        return -1;
    }
        
    public int getItemDurability(int slot) {
        if (slot < 0 || slot >= TOTAL_SLOTS) return -1;
        return itemDurability[slot];
    }

    public boolean damageTool(int slot) {
        if (slot < 0 || slot >= TOTAL_SLOTS) return false;
        if (itemDurability[slot] < 0) return false; 

        itemDurability[slot]--;
        if (itemDurability[slot] <= 0) {
            clearSlot(slot);
            return true; 
        }
        return false;
    }

    public static boolean isTool(int blockId) {
        Block b = Block.fromId(blockId);
        if (b == null) return false;
        
        return b.isTool() || b.isArmor();
    }

    public int equipArmor(int armorBlockId, int durability) {
        Block armor = Block.fromId(armorBlockId);
        int slot = armor.getArmorSlot();
        if (slot < 0) return armorBlockId;
        
        int oldArmorId = armorId[slot];

        armorId [slot] = armorBlockId;
        armorDur[slot] = (durability < 0) ? armor.getArmorMaxDurability() : durability;
        
        return oldArmorId;
    }

    public int unequipArmor(int slot) {
        int id = armorId[slot];
        armorId[slot] = 0; armorDur[slot] = 0;
        return id;
    }


    public float getTotalArmorReduction() {
        float total = 0f;
        for (int i = 0; i < 4; i++) {
            if (armorId[i] != 0) {
                total += Block.fromId(armorId[i]).getArmorProtection();
            }
        }
        return Math.min(0.85f, total);
    }

    public boolean damageArmor(int amount) {
        boolean brokeAny = false;
        for (int i = 0; i < 4; i++) {
            if (armorId[i] == 0) continue;
            armorDur[i] -= amount;
            if (armorDur[i] <= 0) {
                armorId[i] = 0;
                armorDur[i] = 0;
                brokeAny = true;
            }
        }
        return brokeAny;
    }
        
}