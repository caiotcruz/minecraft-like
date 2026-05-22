package com.mcraft.ui;

import java.util.HashMap;
import java.util.Map;

import com.mcraft.world.Block;

public class CraftingGrid {

    public static final int SIZE = 2; // Grade 2×2

    // grade[linha][coluna] = blockId do item colocado
    private final int[][] grid = new int[SIZE][SIZE];

    // Mapa de padrão → {resultId, quantidade}
    // A chave é uma string "id,id,id,id" com os 4 slots da grade 2×2
    private static final Map<String, int[]> RECIPES = new HashMap<>();

    static {
        /*
         * Receitas disponíveis (usando apenas IDs do enum Block):
         *
         *  1× WOOD_LOG (5) em qualquer slot  →  4× PLANKS (9)
         *
         *     [5][ ]        [ ][5]
         *     [ ][ ]   ou   [ ][ ]   etc.  (qualquer posição isolada)
         *
         *  4× PLANKS (9) em 2×2             →  1× CRAFTING_TABLE (10)
         *
         *     [9][9]
         *     [9][9]
         */

        // WOOD_LOG em qualquer slot isolado → PLANKS ×4
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int[][] pat = new int[SIZE][SIZE];
                pat[r][c] = Block.WOOD_LOG.id;
                register(pat, Block.PLANKS.id, 4);
            }
        }

        // 2×2 de Planks → Crafting Table
        register(new int[][]{{Block.PLANKS.id, Block.PLANKS.id},
                             {Block.PLANKS.id, Block.PLANKS.id}},
                Block.CRAFTING_TABLE.id, 1);
    }

    // ── API pública ──────────────────────────────────────────────────────────

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

    /**
     * Verifica se a grade atual corresponde a alguma receita.
     * Testa também o espelho horizontal da grade.
     *
     * @return {resultId, quantidade} ou null se nenhuma receita combinar
     */
    public int[] getResult() {
        String key = gridKey(grid);
        if (RECIPES.containsKey(key)) return RECIPES.get(key);

        String mirrorKey = gridKey(mirrorH(grid));
        if (RECIPES.containsKey(mirrorKey)) return RECIPES.get(mirrorKey);

        return null;
    }

    // ── Utilitários ──────────────────────────────────────────────────────────

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