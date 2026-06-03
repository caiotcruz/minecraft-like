package com.mcraft.world;

public enum Block {

    AIR            ( 0, "air",              0,  0, false, 0f),
    WATER          ( 7, "water",           13, 12, false, 0f),
    
    // ROW 0 — BLOCOS BÁSICOS
    GRASS          ( 1, "grass",            0,  0, true,  0.90f),
    STONE          ( 3, "stone",            1,  0, true,  3.00f),
    DIRT           ( 2, "dirt",             2,  0, true,  0.75f),
    PLANKS         ( 9, "planks",           3,  0, true,  1.50f),
    BEDROCK        ( 8, "bedrock",          4,  0, true,  0f),

    // ROW 1 — NATUREZA
    SAND           ( 4, "sand",             0,  1, true,  0.75f),
    WOOD_LOG       ( 5, "wood_log",         1,  1, true,  2.00f),
    LEAVES         ( 6, "leaves",           2,  1, true,  0.35f),
    CACTUS         (21, "cactus",           3,  1, true,  0.50f),

    // ROW 2 — MINÉRIOS
    COAL_ORE       (11, "coal_ore",         0,  2, true,  3.0f),
    IRON_ORE       (12, "iron_ore",         1,  2, true,  4.5f),
    GOLD_ORE       (13, "gold_ore",         2,  2, true,  4.5f),
    DIAMOND_ORE    (14, "diamond_ore",      3,  2, true,  6.0f),

    // ROW 3 — BLOCOS UTILITÁRIOS
    CRAFTING_TABLE (10, "crafting_table",   0,  3, true,  1.50f),
    CHEST          (31, "chest",            1,  3, true,  1.50f),
    BED            (24, "bed",              2,  3, true,  0.50f),

    // ROW 4 — FERRAMENTAS E ARMAS
    STICK          (30, "stick",            0,  4, false, 0f),

    WOODEN_PICKAXE (26, "wooden_pickaxe",   1,  4, false, 0f),
    WOODEN_AXE     (27, "wooden_axe",       2,  4, false, 0f),
    WOODEN_SHOVEL  (28, "wooden_shovel",    3,  4, false, 0f),
    WOODEN_SWORD   (29, "wooden_sword",     4,  4, false, 0f),

    // ROW 5 — DROPS E RECURSOS
    FEATHER        (15, "feather",          0,  5, false, 0f),
    LEATHER        (16, "leather",          1,  5, false, 0f),
    RAW_BEEF       (17, "raw_beef",         2,  5, false, 0f),
    ROTTEN_FLESH   (18, "rotten_flesh",     3,  5, false, 0f),
    GUNPOWDER      (19, "gunpowder",        4,  5, false, 0f),
    WOOL           (25, "wool",             5,  5, true,  0.8f),

    // ROW 6 — BIOMAS FRIOS
    SNOW           (20, "snow",             0,  6, true,  0.2f),
    SNOWY_GRASS    (22, "snowy_grass",      1,  6, true,  0.9f),
    ICE            (23, "ice",              2,  6, true,  0.2f);

    /**
    * ROW 15 - Faces Auxiliaras
    * GRASS_SIDE                0 15
    * ---------------------     1 15
    * CRAFTING_TABLE LATERAL    2 15
    * ---------                 3 15
    * CHEST SIDES               4 15
    * BED_SIDE                  5 15
    * SNOWY_GRASS_SIDE          6 15
    */

    public final int     id;
    public final String  name;
    public final int     texCol;
    public final int     texRow;  
    public final boolean solid;
    public final float breakTime;

    private static final Block[] BY_ID = new Block[256];

    static {
        for (Block b : values()) {
            BY_ID[b.id] = b;
        }
    }

    Block(int id, String name, int texCol, int texRow, boolean solid, float breakTime) {
        this.id     = id;
        this.name   = name;
        this.texCol = texCol;
        this.texRow = texRow;
        this.solid  = solid;
        this.breakTime = breakTime;
    }

    public static Block fromId(int id) {
        if (id < 0 || id >= BY_ID.length) return AIR;
        Block b = BY_ID[id];
        return (b != null) ? b : AIR;
    }

    public float[] getUVs(int face) {
        int col = texCol;
        int row = texRow;

        if (this == GRASS) {
            switch (face) {
                case 0 -> { col = 0; row = 0; }
                case 1 -> { col = 2; row = 0; }
                default -> { col = 0; row = 15; }
            }
        }

        if (this == SNOWY_GRASS) {
            switch (face) {
                case 0 -> { col = 1; row = 6; }
                case 1 -> { col = 2; row = 0; }
                default -> { col = 6; row = 15; }
            }
        }

        if (this == CRAFTING_TABLE) {
            switch (face) {
                case 0 -> { col = 0; row = 3; }
                default -> { col = 2; row = 15; }
            }
        }

        if (this == CHEST) {
            switch (face) {
                case 1 -> { col = 3; row = 0; } 
                case 3 -> { col = 1;  row = 3; }
                default -> { col = 4; row = 15; } 
            }
        }

        if (this == BED) {
            switch (face) {
                case 0 -> { col = 2;    row = 3; }
                case 1 -> { col = 3; row = 0; } 
                default -> { col = 5; row = 15; } 
            }
        }

        float s  = 1.0f / 16.0f;              
        float u0 = col       * s;
        float v0 = row       * s;
        float u1 = (col + 1) * s;
        float v1 = (row + 1) * s;

        return new float[] {
            u0, v1,
            u1, v1,
            u1, v0,
            u0, v0
        };
    }
}