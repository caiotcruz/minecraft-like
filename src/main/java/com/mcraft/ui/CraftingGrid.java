package com.mcraft.ui;

import com.mcraft.world.Block;
import java.util.HashMap;
import java.util.Map;

public class CraftingGrid {

    public final int size;
    private final int[] gridId;
    private final int[] gridQty;
    private final int[] gridDur;

    private static final Map<String, int[]> RECIPES_2x2 = new HashMap<>();
    private static final Map<String, int[]> RECIPES_3x3 = new HashMap<>();

    static {
        // Receitas 2×2 (inventário)

        //Oak Planks
        reg2(new int[][]{
            {Block.WOOD_LOG.id,0},
            {0,0}
        }, Block.PLANKS.id, 4);

        // Spruce Plank
        reg2(new int[][]{
            {Block.SPRUCE_LOG.id,0},
            {0,0}
        }, Block.SPRUCE_PLANKS.id, 4);

        // Jungle Plank
        reg2(new int[][]{
            {Block.JUNGLE_LOG.id,0},
            {0,0}
        }, Block.JUNGLE_PLANKS.id, 4);

        //bancada
        reg2(new int[][]{
            {Block.PLANKS.id, Block.PLANKS.id},
            {Block.PLANKS.id, Block.PLANKS.id}
        }, Block.CRAFTING_TABLE.id, 1);

        //Stick
        reg2(new int[][]{
            {Block.PLANKS.id,0},
            {Block.PLANKS.id,0}
        }, Block.STICK.id , 4);
        reg2(new int[][]{
            {0,Block.PLANKS.id},
            {0,Block.PLANKS.id}
        }, Block.STICK.id , 4);

        // Receitas 3×3 (bancada)

        // Picareta de madeira
        reg3(new int[][]{
            {Block.PLANKS.id, Block.PLANKS.id, Block.PLANKS.id},
            {     0,          Block.STICK.id,      0          },
            {     0,          Block.STICK.id,      0          }
        }, Block.WOODEN_PICKAXE.id, 1);

        // Machado de madeira
        reg3(new int[][]{
            {Block.PLANKS.id, Block.PLANKS.id, 0},
            {Block.PLANKS.id, Block.STICK.id,  0},
            {0,               Block.STICK.id,  0}
        }, Block.WOODEN_AXE.id, 1);
        reg3(new int[][]{
            {0, Block.PLANKS.id, Block.PLANKS.id},
            {0, Block.STICK.id,  Block.PLANKS.id},
            {0, Block.STICK.id,        0        }
        }, Block.WOODEN_AXE.id, 1);

        // Pá de madeira
        reg3(new int[][]{
            {0, Block.PLANKS.id, 0},
            {0, Block.STICK.id, 0},
            {0, Block.STICK.id, 0}
        }, Block.WOODEN_SHOVEL.id, 1);

        // Espada de madeira
        reg3(new int[][]{
            {0, Block.PLANKS.id, 0},
            {0, Block.PLANKS.id, 0},
            {0, Block.STICK.id,  0} 
        }, Block.WOODEN_SWORD.id, 1);

        // Picareta de Pedra
        reg3(new int[][]{
            {Block.STONE.id, Block.STONE.id, Block.STONE.id},
            {     0,          Block.STICK.id,      0          },
            {     0,          Block.STICK.id,      0          }
        }, Block.STONE_PICKAXE.id, 1);

        // Machado de Pedra
        reg3(new int[][]{
            {Block.STONE.id, Block.STONE.id, 0},
            {Block.STONE.id, Block.STICK.id,  0},
            {0,               Block.STICK.id,  0}
        }, Block.STONE_AXE.id, 1);
        reg3(new int[][]{
            {0, Block.STONE.id, Block.STONE.id},
            {0, Block.STICK.id,  Block.STONE.id},
            {0, Block.STICK.id,        0        }
        }, Block.STONE_AXE.id, 1);

        // Pá de Pedra
        reg3(new int[][]{
            {0, Block.STONE.id, 0},
            {0, Block.STICK.id, 0},
            {0, Block.STICK.id, 0}
        }, Block.STONE_SHOVEL.id, 1);

        // Espada de Pedra
        reg3(new int[][]{
            {0, Block.STONE.id, 0},
            {0, Block.STONE.id, 0},
            {0, Block.STICK.id,  0} 
        }, Block.STONE_SWORD.id, 1);

        // Picareta de Ferro
        reg3(new int[][]{
            {Block.IRON_INGOT.id, Block.IRON_INGOT.id, Block.IRON_INGOT.id},
            {     0,          Block.STICK.id,      0          },
            {     0,          Block.STICK.id,      0          }
        }, Block.IRON_PICKAXE.id, 1);

        // Machado de Ferro
        reg3(new int[][]{
            {Block.IRON_INGOT.id, Block.IRON_INGOT.id, 0},
            {Block.IRON_INGOT.id, Block.STICK.id,  0},
            {0,               Block.STICK.id,  0}
        }, Block.IRON_AXE.id, 1);
        reg3(new int[][]{
            {0, Block.IRON_INGOT.id, Block.IRON_INGOT.id},
            {0, Block.STICK.id,  Block.IRON_INGOT.id},
            {0, Block.STICK.id,        0        }
        }, Block.IRON_AXE.id, 1);

        // Pá de Ferro
        reg3(new int[][]{
            {0, Block.IRON_INGOT.id, 0},
            {0, Block.STICK.id, 0},
            {0, Block.STICK.id, 0}
        }, Block.IRON_SHOVEL.id, 1);

        // Espada de Ferro
        reg3(new int[][]{
            {0, Block.IRON_INGOT.id, 0},
            {0, Block.IRON_INGOT.id, 0},
            {0, Block.STICK.id,  0} 
        }, Block.IRON_SWORD.id, 1);

        // Picareta de Diamante
        reg3(new int[][]{
            {Block.DIAMOND.id, Block.DIAMOND.id, Block.DIAMOND.id},
            {     0,          Block.STICK.id,      0          },
            {     0,          Block.STICK.id,      0          }
        }, Block.DIAMOND_PICKAXE.id, 1);

        // Machado de Diamante
        reg3(new int[][]{
            {Block.DIAMOND.id, Block.DIAMOND.id, 0},
            {Block.DIAMOND.id, Block.STICK.id,  0},
            {0,               Block.STICK.id,  0}
        }, Block.DIAMOND_AXE.id, 1);
        reg3(new int[][]{
            {0, Block.DIAMOND.id, Block.DIAMOND.id},
            {0, Block.STICK.id,  Block.DIAMOND.id},
            {0, Block.STICK.id,        0        }
        }, Block.DIAMOND_AXE.id, 1);

        // Pá de Diamante
        reg3(new int[][]{
            {0, Block.DIAMOND.id, 0},
            {0, Block.STICK.id, 0},
            {0, Block.STICK.id, 0}
        }, Block.DIAMOND_SHOVEL.id, 1);

        // Espada de Diamante
        reg3(new int[][]{
            {0, Block.DIAMOND.id, 0},
            {0, Block.DIAMOND.id, 0},
            {0, Block.STICK.id,  0} 
        }, Block.DIAMOND_SWORD.id, 1);

        // Baú
        reg3(new int[][]{
            {Block.PLANKS.id, Block.PLANKS.id, Block.PLANKS.id},
            {Block.PLANKS.id,         0,       Block.PLANKS.id},
            {Block.PLANKS.id, Block.PLANKS.id, Block.PLANKS.id}
        }, 31, 1); 

        // Cama
        reg3(new int[][]{
            {Block.WOOL.id, Block.WOOL.id, Block.WOOL.id},
            {Block.PLANKS.id, Block.PLANKS.id, Block.PLANKS.id},
            {      0,              0,              0}
        }, Block.BED.id, 1);

        // Fornalha
        reg3 (new int[][]{
            {Block.STONE.id, Block.STONE.id, Block.STONE.id},
            {Block.STONE.id,       0,        Block.STONE.id},
            {Block.STONE.id, Block.STONE.id, Block.STONE.id}
        }, Block.FURNACE.id, 1);

        reg3(new int[][]{
            {Block.LEATHER.id, Block.LEATHER.id, Block.LEATHER.id},
            {Block.LEATHER.id,        0,         Block.LEATHER.id},
            {       0,                0,                0       }
        }, Block.LEATHER_HELMET.id, 1);

        // Peitoral de Couro
        reg3(new int[][]{
            {Block.LEATHER.id,        0,         Block.LEATHER.id},
            {Block.LEATHER.id, Block.LEATHER.id, Block.LEATHER.id},
            {Block.LEATHER.id, Block.LEATHER.id, Block.LEATHER.id}
        }, Block.LEATHER_CHESTPLATE.id, 1);

        // Calça de Couro
        reg3(new int[][]{
            {Block.LEATHER.id, Block.LEATHER.id, Block.LEATHER.id},
            {Block.LEATHER.id,        0,         Block.LEATHER.id},
            {Block.LEATHER.id,        0,         Block.LEATHER.id}
        }, Block.LEATHER_LEGGINGS.id, 1);

        // Botas de Couro
        reg3(new int[][]{
            {Block.LEATHER.id,        0,         Block.LEATHER.id},
            {Block.LEATHER.id,        0,         Block.LEATHER.id},
            {       0,                0,                0       }
        }, Block.LEATHER_BOOTS.id, 1);

        // Elmo de Ferro
        reg3(new int[][]{
            {Block.IRON_INGOT.id, Block.IRON_INGOT.id, Block.IRON_INGOT.id},
            {Block.IRON_INGOT.id,          0,          Block.IRON_INGOT.id},
            {          0,                  0,                   0         }
        }, Block.IRON_HELMET.id, 1);

        // Peitoral de Ferro
        reg3(new int[][]{
            {Block.IRON_INGOT.id,          0,          Block.IRON_INGOT.id},
            {Block.IRON_INGOT.id, Block.IRON_INGOT.id, Block.IRON_INGOT.id},
            {Block.IRON_INGOT.id, Block.IRON_INGOT.id, Block.IRON_INGOT.id}
        }, Block.IRON_CHESTPLATE.id, 1);

        // Calça de Ferro
        reg3(new int[][]{
            {Block.IRON_INGOT.id, Block.IRON_INGOT.id, Block.IRON_INGOT.id},
            {Block.IRON_INGOT.id,          0,          Block.IRON_INGOT.id},
            {Block.IRON_INGOT.id,          0,          Block.IRON_INGOT.id}
        }, Block.IRON_LEGGINGS.id, 1);

        // Botas de Ferro
        reg3(new int[][]{
            {Block.IRON_INGOT.id,          0,          Block.IRON_INGOT.id},
            {Block.IRON_INGOT.id,          0,          Block.IRON_INGOT.id},
            {          0,                  0,                   0         }
        }, Block.IRON_BOOTS.id, 1);

        // Elmo de Ouro
        reg3(new int[][]{
            {Block.GOLD_INGOT.id, Block.GOLD_INGOT.id, Block.GOLD_INGOT.id},
            {Block.GOLD_INGOT.id,          0,          Block.GOLD_INGOT.id},
            {          0,                  0,                   0         }
        }, Block.GOLD_HELMET.id, 1);

        // Peitoral de Ouro
        reg3(new int[][]{
            {Block.GOLD_INGOT.id,          0,          Block.GOLD_INGOT.id},
            {Block.GOLD_INGOT.id, Block.GOLD_INGOT.id, Block.GOLD_INGOT.id},
            {Block.GOLD_INGOT.id, Block.GOLD_INGOT.id, Block.GOLD_INGOT.id}
        }, Block.GOLD_CHESTPLATE.id, 1);

        // Calça de Ouro
        reg3(new int[][]{
            {Block.GOLD_INGOT.id, Block.GOLD_INGOT.id, Block.GOLD_INGOT.id},
            {Block.GOLD_INGOT.id,          0,          Block.GOLD_INGOT.id},
            {Block.GOLD_INGOT.id,          0,          Block.GOLD_INGOT.id}
        }, Block.GOLD_LEGGINGS.id, 1);

        // Botas de Ouro
        reg3(new int[][]{
            {Block.GOLD_INGOT.id,          0,          Block.GOLD_INGOT.id},
            {Block.GOLD_INGOT.id,          0,          Block.GOLD_INGOT.id},
            {          0,                  0,                   0         }
        }, Block.GOLD_BOOTS.id, 1);

        // Elmo de Diamante
        reg3(new int[][]{
            {Block.DIAMOND.id, Block.DIAMOND.id, Block.DIAMOND.id},
            {Block.DIAMOND.id,        0,         Block.DIAMOND.id},
            {       0,                0,                0       }
        }, Block.DIAMOND_HELMET.id, 1);

        // Peitoral de Diamante
        reg3(new int[][]{
            {Block.DIAMOND.id,        0,         Block.DIAMOND.id},
            {Block.DIAMOND.id, Block.DIAMOND.id, Block.DIAMOND.id},
            {Block.DIAMOND.id, Block.DIAMOND.id, Block.DIAMOND.id}
        }, Block.DIAMOND_CHESTPLATE.id, 1);

        // Calça de Diamante
        reg3(new int[][]{
            {Block.DIAMOND.id, Block.DIAMOND.id, Block.DIAMOND.id},
            {Block.DIAMOND.id,        0,         Block.DIAMOND.id},
            {Block.DIAMOND.id,        0,         Block.DIAMOND.id}
        }, Block.DIAMOND_LEGGINGS.id, 1);

        // Botas de Diamante
        reg3(new int[][]{
            {Block.DIAMOND.id,        0,         Block.DIAMOND.id},
            {Block.DIAMOND.id,        0,         Block.DIAMOND.id},
            {       0,                0,                0       }
        }, Block.DIAMOND_BOOTS.id, 1);

        //Porta
        reg3(new int[][]{
            {Block.PLANKS.id, Block.PLANKS.id, 0},
            {Block.PLANKS.id, Block.PLANKS.id, 0},
            {Block.PLANKS.id, Block.PLANKS.id, 0}
        }, Block.DOOR_CLOSED.id, 2);
    }

    public CraftingGrid(int size) {
        this.size = size;

        int slots = size * size;

        this.gridId  = new int[slots];
        this.gridQty = new int[slots];
        this.gridDur = new int[slots];
        java.util.Arrays.fill(gridDur, -1);
    }

    private int idx(int row, int col) {
        return row * size + col;
    }

    public void setSlot(int row, int col, int id) {
        if (row >= 0 && row < size && col >= 0 && col < size) {
            int i = idx(row, col);

            gridId[i] = id;
            gridQty[i] = (id == 0) ? 0 : 1;
        }
    }

    public void setSlot(int row, int col, int id, int qty) {
        setSlot(row, col, id, qty, -1);
    }

    public void setSlot(int row, int col, int id, int qty, int dur) {
        if (row >= 0 && row < size && col >= 0 && col < size) {
            int i = row * size + col;
            gridId[i]  = id;
            gridQty[i] = qty;
            gridDur[i] = dur;
        }
    }

    public int getSlot(int row, int col) {
        if (row < 0 || row >= size || col < 0 || col >= size)
            return 0;

        return gridId[idx(row, col)];
    }

    public int getSlotQty(int row, int col) {
        if (row < 0 || row >= size || col < 0 || col >= size)
            return 0;

        return gridQty[idx(row, col)];
    }

    public int getSlotId(int idx) {
        return (idx >= 0 && idx < gridId.length)
                ? gridId[idx]
                : 0;
    }

    public int getSlotQty(int idx) {
        return (idx >= 0 && idx < gridQty.length)
                ? gridQty[idx]
                : 0;
    }

    public int getSlotDur(int idx) {
        return (idx >= 0 && idx < gridDur.length) ? gridDur[idx] : -1;
    }

    public int getResultQty() {
        int[] result = getResult();
        return (result != null) ? result[1] : 0;
    }

    public void clearSlot(int row, int col) {
        int i = idx(row, col);

        gridId[i] = 0;
        gridQty[i] = 0;
        gridDur[i] = -1;
    }

    public void clear() {
        for (int i = 0; i < gridId.length; i++) {
            gridId[i] = 0;
            gridQty[i] = 0;
        }
    }

    public int[] getResult() {
        Map<String, int[]> recipes =
            (size == 3) ? RECIPES_3x3 : RECIPES_2x2;

        int[][] ids = toIdMatrix();

        String key = gridKey(ids, size);

        if (recipes.containsKey(key))
            return recipes.get(key);

        String mKey = gridKey(mirror(ids, size), size);

        return recipes.getOrDefault(mKey, null);
    }
    private static void reg2(int[][] p, int id, int qty) {
        RECIPES_2x2.put(gridKey(p, 2), new int[]{id, qty});
    }

    private static void reg3(int[][] p, int id, int qty) {
        RECIPES_3x3.put(gridKey(p, 3), new int[]{id, qty});
    }

    private static String gridKey(int[][] g, int s) {
        var sb = new StringBuilder();
        for (int r=0;r<s;r++) for (int c=0;c<s;c++) sb.append(g[r][c]).append(',');
        return sb.toString();
    }

    private static int[][] mirror(int[][] g, int s) {
        int[][] m = new int[s][s];
        for (int r=0;r<s;r++) for (int c=0;c<s;c++) m[r][c]=g[r][s-1-c];
        return m;
    }

    private int[][] toIdMatrix() {
        int[][] ids = new int[size][size];

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                ids[r][c] = gridId[idx(r, c)];
            }
        }

        return ids;
    }

    public void consumeIngredients() {
        for (int i = 0; i < gridId.length; i++) {
            if (gridId[i] != 0) {
                gridQty[i]--;
                if (gridQty[i] <= 0) {
                    gridId[i] = 0;
                    gridQty[i] = 0;
                    gridDur[i] = -1;
                }
            }
        }
    }

    public void returnToInventory(Inventory inv) {
        for (int i = 0; i < gridId.length; i++) {
            if (gridId[i] != 0) {
                if (Inventory.isTool(gridId[i])) {
                    inv.addToolWithDurability(gridId[i], gridDur[i]);
                } else {
                    inv.addItem(gridId[i], gridQty[i]);
                }
                gridId[i] = 0; gridQty[i] = 0; gridDur[i] = -1;
            }
        }
    }
}