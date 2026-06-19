package com.mcraft.world;

public enum Block {

    AIR            ( 0, "air",              0,  0, false, 0f, 0),
    WATER          ( 7, "block/water",            13, 12, false, 0f, 0),
    
    // ROW 0 — BLOCOS BÁSICOS
    GRASS          ( 1, "block/grass",            0,  0, true,  0.90f, 0),
    STONE          ( 3, "block/stone",            1,  0, true,  3.00f, 0),
    DIRT           ( 2, "block/dirt",             2,  0, true,  0.75f, 0),
    PLANKS         ( 9, "block/planks",           3,  0, true,  1.50f, 0),
    BEDROCK        ( 8, "block/bedrock",          4,  0, true,  0f, 0),
    SPRUCE_PLANKS  ( 72,"block/spruce_planks",    5,  0, true,  1.50f, 0),
    JUNGLE_PLANKS  ( 73,"block/jungle_planks",    6,  0, true,  1.50f, 0),

    // ROW 1 — NATUREZA
    SAND           ( 4, "block/sand",             0,  1, true,  0.75f, 0),
    WOOD_LOG       ( 5, "block/wood_log",         1,  1, true,  2.00f, 0),
    LEAVES         ( 6, "block/leaves",           2,  1, true,  0.35f, 0),
    CACTUS         (21, "block/cactus",           3,  1, true,  0.50f, 0),
    DENSE_GRASS    (32, "block/dense_grass",      4,  1, true,  0.9f, 0),
    SPRUCE_LOG     (68, "block/spruce_log",       5,  1, true,  2.0f, 0),
    SPRUCE_LEAVES  (69, "block/spruce_leaves",    6,  1, true,  0.35f, 0),
    JUNGLE_LOG     (70, "block/jungle_log",       7,  1, true,  2.0f, 0),
    JUNGLE_LEAVES  (71, "block/jungle_leaves",    8,  1, true,  0.35f, 0),

    // ROW 2 — MINÉRIOS
    COAL_ORE       (11, "block/coal_ore",         0,  2, true,  3.0f, 0),
    IRON_ORE       (12, "block/iron_ore",         1,  2, true,  4.5f, 0),
    GOLD_ORE       (13, "block/gold_ore",         2,  2, true,  4.5f, 0),
    DIAMOND_ORE    (14, "block/diamond_ore",      3,  2, true,  6.0f, 0),

    // ROW 3 — BLOCOS UTILITÁRIOS
    CRAFTING_TABLE (10, "block/crafting_table",   0,  3, true,  1.50f, 0),
    CHEST          (31, "block/chest",            1,  3, true,  1.50f, 0),
    BED            (24, "block/bed",              2,  3, true,  0.50f, 0),
    FURNACE        (33, "block/furnace",          3,  3, true,  1.50f, 0),

    // ROW 4 — FERRAMENTAS E ARMAS
    WOODEN_PICKAXE (26, "tools/wooden_pickaxe",   0,  4, false, 0f, 0),
    WOODEN_AXE     (27, "tools/wooden_axe",       1,  4, false, 0f, 0),
    WOODEN_SHOVEL  (28, "tools/wooden_shovel",    2,  4, false, 0f, 0),
    WOODEN_SWORD   (29, "tools/wooden_sword",     3,  4, false, 0f, 0),
    STONE_PICKAXE  (40, "tools/stone_pickaxe",    4,  4, false, 0f, 0),
    STONE_AXE      (41, "tools/stone_axe",        5,  4, false, 0f, 0),
    STONE_SHOVEL   (42, "tools/stone_shovel",     6,  4, false, 0f, 0),
    STONE_SWORD    (43, "tools/stone_sword",      7,  4, false, 0f, 0),
    IRON_PICKAXE   (44, "tools/iron_pickaxe",     8,  4, false, 0f, 0),
    IRON_AXE       (45, "tools/iron_axe",         9,  4, false, 0f, 0),
    IRON_SHOVEL    (46, "tools/iron_shovel",      10, 4, false, 0f, 0),
    IRON_SWORD     (47, "tools/iron_sword",       11, 4, false, 0f, 0),
    DIAMOND_PICKAXE(48, "tools/diamond_pickaxe",  12, 4, false, 0f, 0),
    DIAMOND_AXE    (49, "tools/diamond_axe",      13, 4, false, 0f, 0),
    DIAMOND_SHOVEL (50, "tools/diamond_shovel",   14, 4, false, 0f, 0),
    DIAMOND_SWORD  (51, "tools/diamond_sword",    15, 4, false, 0f, 0),

    // ROW 5 — DROPS E RECURSOS
    FEATHER        (15, "materiais/feather",          0,  5, false, 0f, 0),
    LEATHER        (16, "materiais/leather",          1,  5, false, 0f, 0),
    RAW_BEEF       (17, "materiais/raw_beef",         2,  5, false, 0f, 0),
    COOKED_BEEF    (38, "materiais/cooked_beef",      6,  5, false, 0f, 0),
    ROTTEN_FLESH   (18, "materiais/rotten_flesh",     3,  5, false, 0f, 0),
    GUNPOWDER      (19, "materiais/gunpowder",        4,  5, false, 0f, 0),
    WOOL           (25, "materiais/wool",             5,  5, true,  0.8f, 0),

    // ROW 6 — BIOMAS FRIOS
    SNOW           (20, "block/snow",             0,  6, true,  0.2f, 0),
    SNOWY_GRASS    (22, "block/snowy_grass",      1,  6, true,  0.9f, 0),
    ICE            (23, "block/ice",              2,  6, true,  0.2f, 0),

    // ROW 7 - MATERIAIS
    STICK          (30, "materiais/stick",            0,  7, false, 0f, 0),
    COAL           (34, "materiais/coal",             1,  7, false, 0f, 0),
    IRON_INGOT     (35, "materiais/iron_ingot",       2,  7, false, 0f, 0),
    GOLD_INGOT     (36, "materiais/gold_ingot",       3,  7, false, 0f, 0),
    DIAMOND        (37, "materiais/diamond",          4,  7, false, 0f, 0),

    // ROW 8 - ILUMINAÇÃO
    TORCH          (39, "iluminacao/torch",            0,  8, true, 0.05f, 14),

    // ROW 9 - ARMADURAS

    LEATHER_HELMET    (52, "armor/leather_helmet",    0, 9, false, 0f, 0),
    LEATHER_CHESTPLATE(53, "armor/leather_chestplate",1, 9, false, 0f, 0),
    LEATHER_LEGGINGS  (54, "armor/leather_leggings",  2, 9, false, 0f,0 ),
    LEATHER_BOOTS     (55, "armor/leather_boots",     3, 9, false, 0f, 0),
    IRON_HELMET       (56, "armor/iron_helmet",       4, 9, false, 0f, 0),
    IRON_CHESTPLATE   (57, "armor/iron_chestplate",   5, 9, false, 0f, 0),
    IRON_LEGGINGS     (58, "armor/iron_leggings",     6, 9, false, 0f, 0),
    IRON_BOOTS        (59, "armor/iron_boots",        7, 9, false, 0f, 0),
    GOLD_HELMET       (60, "armor/golden_helmet",       8, 9, false, 0f, 0),
    GOLD_CHESTPLATE   (61, "armor/golden_chestplate",   9, 9, false, 0f, 0),
    GOLD_LEGGINGS     (62, "armor/golden_leggings",     10, 9, false, 0f, 0),
    GOLD_BOOTS        (63, "armor/golden_boots",        11, 9, false, 0f, 0),
    DIAMOND_HELMET    (64, "armor/diamond_helmet",    12, 9, false, 0f, 0),
    DIAMOND_CHESTPLATE(65, "armor/diamond_chestplate",13, 9, false, 0f, 0),
    DIAMOND_LEGGINGS  (66, "armor/diamond_leggings",  14, 9, false, 0f, 0),
    DIAMOND_BOOTS     (67, "armor/diamond_boots",     15, 9, false, 0f, 0);

    /**
    * ROW 15 - Faces Auxiliaras
    * GRASS_SIDE                0 15
    * FURNACE_FRONT             1 15
    * CRAFTING_TABLE_SIDE       2 15
    * FURNACE_SIDE              3 15
    * CHEST_SIDE                4 15
    * CHEST_BOTTOM              5 15
    * SNOWY_GRASS_SIDE          6 15
    * WOOD_LOG_SIDE             7 15
    * CACTUS_SIDE               8 15
    * SPRUCE_LOG_SIDE           9 15
    * JUNGLE_LOG_SIDE           10 15
    */

    public final int     id;
    public final String  name;
    public final int     texCol;
    public final int     texRow;  
    public final boolean solid;
    public final float breakTime;
    public final int lightEmission;

    private static final Block[] BY_ID = new Block[256];

    static {
        for (Block b : values()) {
            BY_ID[b.id] = b;
        }
    }

    Block(int id, String name, int texCol, int texRow, boolean solid, float breakTime, int lightEmission) {
        this.id     = id;
        this.name   = name;
        this.texCol = texCol;
        this.texRow = texRow;
        this.solid  = solid;
        this.breakTime = breakTime;
        this.lightEmission = lightEmission;
    }

    public static Block fromId(int id) {
        if (id < 0 || id >= BY_ID.length) return AIR;
        Block b = BY_ID[id];
        return (b != null) ? b : AIR;
    }

    public int getDropId() {
        return switch (this) {
            case COAL_ORE -> Block.COAL.id;
            case DIAMOND_ORE -> Block.DIAMOND.id;
            default       -> this.id;
        };
    }

    public int getToolMaxDurability() {
        String n = this.name();
        
        if (n.contains("_PICKAXE") || n.contains("_AXE") || n.contains("_SHOVEL") || n.contains("_SWORD")) {
            if (n.startsWith("WOODEN"))  return 60;
            if (n.startsWith("STONE"))   return 90;
            if (n.startsWith("IRON"))    return 120;
            if (n.startsWith("DIAMOND")) return 180;
        }
        return -1;
    }

    public float getToolMaterialMultiplier() {
        String n = this.name();
        if (n.startsWith("WOODEN"))  return 2.5f;
        if (n.startsWith("STONE"))   return 3.0f;
        if (n.startsWith("IRON"))    return 3.5f;
        if (n.startsWith("DIAMOND")) return 5.0f;
        return 1.0f;
    }

    public boolean isStoneType() {
        return switch (this) {
            case STONE, COAL_ORE, IRON_ORE, GOLD_ORE, DIAMOND_ORE -> true;
            default -> false;
        };
    }

    public boolean isWoodType() {
        return switch (this) {
            case WOOD_LOG, SPRUCE_LOG, JUNGLE_LOG, PLANKS, CRAFTING_TABLE, CHEST -> true;
            default -> false;
        };
    }

    public boolean isEarthType() {
        return switch (this) {
            case DIRT, GRASS, SAND, SNOW, SNOWY_GRASS, DENSE_GRASS -> true;
            default -> false;
        };
    }

    public boolean isLeaves() {
        return switch (this) {
            case LEAVES, SPRUCE_LEAVES, JUNGLE_LEAVES -> true;
            default -> false;
        };
    }

    public int getArmorSlot() {
        return switch (this) {
            case LEATHER_HELMET, IRON_HELMET, GOLD_HELMET, DIAMOND_HELMET         -> 0;
            case LEATHER_CHESTPLATE, IRON_CHESTPLATE, GOLD_CHESTPLATE, DIAMOND_CHESTPLATE -> 1;
            case LEATHER_LEGGINGS, IRON_LEGGINGS, GOLD_LEGGINGS, DIAMOND_LEGGINGS -> 2;
            case LEATHER_BOOTS, IRON_BOOTS, GOLD_BOOTS, DIAMOND_BOOTS             -> 3;
            default -> -1;
        };
    }

    public float getArmorProtection() {
        return switch (this) {

            case LEATHER_HELMET, LEATHER_BOOTS              -> 0.04f;
            case LEATHER_CHESTPLATE                          -> 0.10f;
            case LEATHER_LEGGINGS                            -> 0.06f;

            case IRON_HELMET, IRON_BOOTS                    -> 0.08f;
            case IRON_CHESTPLATE                             -> 0.20f;
            case IRON_LEGGINGS                               -> 0.12f;

            case GOLD_HELMET, GOLD_BOOTS                    -> 0.15f;
            case GOLD_CHESTPLATE                             -> 0.40f;
            case GOLD_LEGGINGS                               -> 0.20f;

            case DIAMOND_HELMET, DIAMOND_BOOTS              -> 0.12f;
            case DIAMOND_CHESTPLATE                          -> 0.30f;
            case DIAMOND_LEGGINGS                            -> 0.18f;
            default -> 0f;
        };
    }

    public int getArmorMaxDurability() {
        if (getArmorSlot() < 0) return -1;
        String n = name();
        if (n.startsWith("LEATHER"))  return 60;
        if (n.startsWith("IRON"))     return 140;
        if (n.startsWith("GOLD"))     return 40;
        if (n.startsWith("DIAMOND"))  return 200;
        return -1;
    }

    public boolean isArmor() {
        return getArmorSlot() != -1;
    }

    public boolean isTool() {
        return getToolMaxDurability() != -1;
    }


    public float[] getUVs(int face) {
        int col = texCol;
        int row = texRow;

        switch (this) {
            case GRASS -> {
                switch (face) {
                    case 0 -> { col = 0; row = 0; }
                    case 1 -> { col = 2; row = 0; }
                    default -> { col = 0; row = 15; }
                }
            }
            case SNOWY_GRASS -> {
                switch (face) {
                    case 0 -> { col = 0; row = 6; }
                    case 1 -> { col = 2; row = 0; }
                    default -> { col = 6; row = 15; }
                }
            }
            case WOOD_LOG -> {
                switch (face) {
                    case 0, 1 -> { col = 1; row = 1; }
                    default -> { col = 7; row = 15; }
                }
            }
            case SPRUCE_LOG -> {
                switch (face){
                    case 0, 1 -> { col = 5; row = 1; }
                    default -> { col = 9; row = 15; }
                }
            }
            case JUNGLE_LOG -> {
                switch (face){
                    case 0, 1 -> { col = 7; row = 1; }
                    default -> { col = 10; row = 15; }
                }
            }
            case CRAFTING_TABLE -> {
                switch (face) {
                    case 0 -> { col = 0; row = 3; }
                    case 1 -> { col = 3; row = 0; }
                    default -> { col = 2; row = 15; }
                }
            }
            case FURNACE -> {
                switch (face) {
                    case 0 -> { col = 3; row = 3; }
                    case 1 -> { col = 3; row = 3; }
                    case 3 -> { col =1; row = 15;}
                    default -> { col = 3; row = 15; } 
                }
            }
            case CHEST -> {
                switch (face) {
                    case 0 -> {col = 1; row = 3;}
                    case 1 -> { col = 5; row = 15; }
                    default -> { col = 4; row = 15; }
                }
            }
            case BED -> {
                switch (face){
                    case 0 -> {col = 2; row = 3;}
                    default -> {col = 3; row = 0; }
                }
            }
            case CACTUS -> {
                switch (face){
                    case 0 -> {col = 3; row = 1;}
                    case 1 -> {col = 3; row = 1;}
                    default -> {col = 8; row = 15; }
                }
            }
            default -> {
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