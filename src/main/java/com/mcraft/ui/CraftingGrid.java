package com.mcraft.ui;

import java.util.HashMap;
import java.util.Map;

import com.mcraft.world.Block;

public class CraftingGrid {

    public static final int SIZE = 2; 
    private final int[][] grid = new int[SIZE][SIZE];

    private static final Map<String, int[]> RECIPES = new HashMap<>();

    static {
       
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int[][] pat = new int[SIZE][SIZE];
                pat[r][c] = Block.WOOD_LOG.id;
                register(pat, Block.PLANKS.id, 4);
            }
        }

        register(new int[][]{{Block.PLANKS.id, Block.PLANKS.id},
                             {Block.PLANKS.id, Block.PLANKS.id}},
                Block.CRAFTING_TABLE.id, 1);
    }


    public void setSlot(int row, int col, int blockId) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return;
        grid[row][col] = blockId;
    }

    public int getSlot(int row, int col) { return grid[row][col]; }

    public void clearSlot(int row, int col) { setSlot(row, col, 0); }

    public void clear() {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                grid[r][c] = 0;
    }

    public int[] getResult() {
        String key = gridKey(grid);
        if (RECIPES.containsKey(key)) return RECIPES.get(key);

        String mirrorKey = gridKey(mirrorH(grid));
        if (RECIPES.containsKey(mirrorKey)) return RECIPES.get(mirrorKey);

        return null;
    }

    private static void register(int[][] pattern, int resultId, int qty) {
        RECIPES.put(gridKey(pattern), new int[]{resultId, qty});
    }

    private static String gridKey(int[][] g) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                sb.append(g[r][c]).append(',');
        return sb.toString();
    }

    private static int[][] mirrorH(int[][] g) {
        int[][] m = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                m[r][c] = g[r][SIZE - 1 - c];
        return m;
    }
}