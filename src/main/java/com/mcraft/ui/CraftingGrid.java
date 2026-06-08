package com.mcraft.ui;

import com.mcraft.world.Block;
import java.util.HashMap;
import java.util.Map;

public class CraftingGrid {

    public final int size;
    private final int[][] grid;

    private static final Map<String, int[]> RECIPES_2x2 = new HashMap<>();
    private static final Map<String, int[]> RECIPES_3x3 = new HashMap<>();

    static {
        // Receitas 2×2 (inventário)

        //Planks
        reg2(new int[][]{
            {5,0},
            {0,0}
        }, Block.PLANKS.id, 4);
        
        //bancada
        reg2(new int[][]{
            {9,9},
            {9,9}
        }, Block.CRAFTING_TABLE.id, 1);

        //Stick
        reg2(new int[][]{
            {9,0},
            {9,0}
        }, Block.STICK.id , 4);
        reg2(new int[][]{
            {0,9},
            {0,9}
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
    }

    public CraftingGrid(int size) {
        this.size = size;
        this.grid = new int[size][size];
    }

    public void setSlot(int row, int col, int id) {
        if (row>=0&&row<size&&col>=0&&col<size) grid[row][col] = id;
    }
    public int  getSlot(int row, int col) {
        return (row>=0&&row<size&&col>=0&&col<size) ? grid[row][col] : 0;
    }
    public void clearSlot(int row, int col) { setSlot(row, col, 0); }
    public void clear()  { for (int r=0;r<size;r++) for(int c=0;c<size;c++) grid[r][c]=0; }

    public int[] getResult() {
        Map<String,int[]> recipes = (size == 3) ? RECIPES_3x3 : RECIPES_2x2;
        String key = gridKey(grid, size);
        if (recipes.containsKey(key)) return recipes.get(key);
        String mKey = gridKey(mirror(grid, size), size);
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
}