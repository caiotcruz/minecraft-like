package com.mcraft.ui;

import java.util.HashMap;
import java.util.Map;

import com.mcraft.world.Block;

public class CraftingGrid {

    private final int size;
    private final int[][] grid;

    private static final Map<String, int[]> RECIPES = new HashMap<>();

    static {

        int[][] singleWood = new int[2][2];
        singleWood[0][0] = Block.WOOD_LOG.id;
        register(singleWood, 2, Block.PLANKS.id, 4);

        register(new int[][]{
                {Block.PLANKS.id, Block.PLANKS.id},
                {Block.PLANKS.id, Block.PLANKS.id}
        }, 2, Block.CRAFTING_TABLE.id, 1);
    }

    public CraftingGrid(int size) {
        this.size = size;
        this.grid = new int[size][size];
    }

    public int getSize() {
        return size;
    }

    public void setSlot(int row, int col, int blockId) {
        if (row < 0 || row >= size || col < 0 || col >= size) return;
        grid[row][col] = blockId;
    }

    public int getSlot(int row, int col) {
        if (row < 0 || row >= size || col < 0 || col >= size) return 0;
        return grid[row][col];
    }

    public void clearSlot(int row, int col) {
        setSlot(row, col, 0);
    }

    public void clear() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                grid[r][c] = 0;
            }
        }
    }

    public int[] getResult() {
        String key = gridKey(grid, size);

        if (RECIPES.containsKey(key)) {
            return RECIPES.get(key);
        }

        String mirrorKey = gridKey(mirrorH(grid, size), size);

        return RECIPES.get(mirrorKey);
    }

    private static void register(int[][] pattern, int size,
                                 int resultId, int qty) {
        RECIPES.put(
            gridKey(pattern, size),
            new int[]{resultId, qty}
        );
    }

    private static String gridKey(int[][] g, int size) {
        StringBuilder sb = new StringBuilder();

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                sb.append(g[r][c]).append(',');
            }
        }

        return sb.toString();
    }

    private static int[][] mirrorH(int[][] g, int size) {
        int[][] m = new int[size][size];

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                m[r][c] = g[r][size - 1 - c];
            }
        }

        return m;
    }
}