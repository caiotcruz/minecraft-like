package com.mcraft.world;

public enum Block {

    //              id   nome              col  row  sólido
    AIR            ( 0, "air",              0,   0,  false),
    GRASS          ( 1, "grass",            0,   0,  true),
    DIRT           ( 2, "dirt",             2,   0,  true),
    STONE          ( 3, "stone",            1,   0,  true),
    SAND           ( 4, "sand",             2,   1,  true),
    WOOD_LOG       ( 5, "wood_log",         4,   1,  true),
    LEAVES         ( 6, "leaves",           4,   3,  true),
    WATER          ( 7, "water",           13,  12,  false),
    BEDROCK        ( 8, "bedrock",          1,   1,  true),
    PLANKS         ( 9, "planks",           4,   0,  true),   // ← novo
    CRAFTING_TABLE (10, "crafting_table",   11,  2,  true);   // ← novo

    public final int     id;
    public final String  name;
    public final int     texCol;  // Coluna no atlas 16×16
    public final int     texRow;  // Linha no atlas 16×16
    public final boolean solid;

    // Lookup O(1): id → Block
    private static final Block[] BY_ID = new Block[256];

    static {
        for (Block b : values()) {
            BY_ID[b.id] = b;
        }
    }

    Block(int id, String name, int texCol, int texRow, boolean solid) {
        this.id     = id;
        this.name   = name;
        this.texCol = texCol;
        this.texRow = texRow;
        this.solid  = solid;
    }

    public static Block fromId(int id) {
        if (id < 0 || id >= BY_ID.length) return AIR;
        Block b = BY_ID[id];
        return (b != null) ? b : AIR;
    }

    /**
     * Coordenadas UV para uma das 6 faces do cubo.
     *
     * O atlas tem 16×16 tiles; cada tile ocupa 1/16 do comprimento UV.
     * Face: 0=topo, 1=base, 2=frente(Z+), 3=trás(Z-), 4=esq(X-), 5=dir(X+)
     *
     * @return float[8] — 4 pares (U,V), um por vértice do quad, em ordem:
     *         [bl.u, bl.v,  br.u, br.v,  tr.u, tr.v,  tl.u, tl.v]
     */
    public float[] getUVs(int face) {
        int col = texCol;
        int row = texRow;

        // Grass: topo verde, base dirt, laterais grass-side
        if (this == GRASS) {
            switch (face) {
                case 0 -> { col = 0;  row = 0; }  // topo: grass_top
                case 1 -> { col = 2;  row = 0; }  // base: dirt
                default ->  { col = 3; row = 0; }  // laterais: grass_side
            }
        }

        // Crafting table: topo especial, fronts especiais
        if (this == CRAFTING_TABLE) {
            switch (face) {
                case 0 -> { col = 11; row = 2; } // topo
                case 2, 5 -> { col = 12; row = 2; } // frente/direita
                default -> { col = 13; row = 2; } // lateral/base
            }
        }

        float s  = 1.0f / 16.0f;               // tamanho de um tile
        float u0 = col       * s;
        float v0 = row       * s;
        float u1 = (col + 1) * s;
        float v1 = (row + 1) * s;

        // 4 vértices: bottom-left, bottom-right, top-right, top-left
        return new float[] {
            u0, v1,
            u1, v1,
            u1, v0,
            u0, v0
        };
    }
}