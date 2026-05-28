package com.mcraft.world;

public enum Block {

    AIR            ( 0, "air",              0,   0,  false,  0f   ),
    GRASS          ( 1, "grass",            0,   0,  true,   0.90f),
    DIRT           ( 2, "dirt",             2,   0,  true,   0.75f),
    STONE          ( 3, "stone",            1,   0,  true,   3.00f),
    SAND           ( 4, "sand",             2,   1,  true,   0.75f),
    WOOD_LOG       ( 5, "wood_log",         4,   1,  true,   2.00f),
    LEAVES         ( 6, "leaves",           4,   3,  true,   0.35f),
    WATER          ( 7, "water",           13,  12,  false,  0f   ),
    BEDROCK        ( 8, "bedrock",          1,   1,  true,   0f   ),
    PLANKS         ( 9, "planks",           4,   0,  true,   1.50f),
    CRAFTING_TABLE (10, "crafting_table",  11,   2,  true,   1.50f),
    COAL_ORE       (11, "coal_ore",     0,   2,  true,  3.0f),
    IRON_ORE       (12, "iron_ore",     1,   2,  true,  4.5f),
    GOLD_ORE       (13, "gold_ore",     2,   2,  true,  4.5f),
    DIAMOND_ORE    (14, "diamond_ore",  3,   2,  true,  6.0f),
    FEATHER        (15, "feather",      5, 2, false, 0f),
    LEATHER        (16, "leather",      6, 2, false, 0f),
    RAW_BEEF       (17, "raw_beef",     7, 2, false, 0f),
    ROTTEN_FLESH   (18, "rotten_flesh", 8, 2, false, 0f),
    GUNPOWDER      (19, "gunpowder",    9, 2, false, 0f),
    SNOW           (20, "snow", 2, 4, true, 0.2f),
    CACTUS         (21, "cactus", 6, 4, true, 0.5f);

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
                case 0 -> { col = 0;  row = 0; } 
                case 1 -> { col = 2;  row = 0; } 
                default ->  { col = 3; row = 0; }  
            }
        }

        if (this == CRAFTING_TABLE) {
            switch (face) {
                case 0 -> { col = 11; row = 2; } 
                case 2, 5 -> { col = 12; row = 2; }
                default -> { col = 13; row = 2; } 
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