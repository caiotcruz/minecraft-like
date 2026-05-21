package com.mcraft.world;

public enum Block {

    //           id   nome          UV-coluna  UV-linha  sólido
    AIR         (0,  "air",         0,         0,        false),
    GRASS       (1,  "grass",       0,         0,        true),
    DIRT        (2,  "dirt",        2,         0,        true),
    STONE       (3,  "stone",       1,         0,        true),
    SAND        (4,  "sand",        2,         1,        true),
    WOOD_LOG    (5,  "wood_log",    4,         1,        true),
    LEAVES      (6,  "leaves",      4,         3,        true),
    WATER       (7,  "water",       13,        12,       false),
    BEDROCK     (8,  "bedrock",     1,         1,        true);

    public final int id;
    public final String name;
    public final int texCol;   // Coluna no atlas de texturas
    public final int texRow;   // Linha no atlas de texturas
    public final boolean solid;

    // Mapa de ID → Block para lookup rápido O(1)
    private static final Block[] BY_ID = new Block[256];

    static {
        for (Block b : values()) {
            BY_ID[b.id] = b;
        }
    }

    Block(int id, String name, int texCol, int texRow, boolean solid) {
        this.id = id;
        this.name = name;
        this.texCol = texCol;
        this.texRow = texRow;
        this.solid = solid;
    }

    public static Block fromId(int id) {
        if (id < 0 || id >= BY_ID.length) return AIR;
        Block b = BY_ID[id];
        return b != null ? b : AIR;
    }

    /**
     * Retorna as coordenadas UV (0.0–1.0) para uma dada face.
     * O atlas tem 16x16 tiles, cada tile = 1/16 do total.
     *
     * Face: 0=top, 1=bottom, 2=front, 3=back, 4=left, 5=right
     */
    public float[] getUVs(int face) {
        int col = texCol;
        int row = texRow;

        // Grass tem top diferente (grama verde) e sides especiais
        if (this == GRASS) {
            if (face == 0) { col = 0; row = 0; }       // top: grama
            else if (face == 1) { col = 2; row = 0; }  // bottom: dirt
            else { col = 3; row = 0; }                  // sides: grass-side
        }

        float tileSize = 1.0f / 16.0f;
        float u0 = col * tileSize;
        float v0 = row * tileSize;
        float u1 = u0 + tileSize;
        float v1 = v0 + tileSize;

        // Retorna 4 pares UV para os 4 vértices do quad
        return new float[] {
            u0, v1,  // bottom-left
            u1, v1,  // bottom-right
            u1, v0,  // top-right
            u0, v0   // top-left
        };
    }
}