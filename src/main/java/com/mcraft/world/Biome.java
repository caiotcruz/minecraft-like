package com.mcraft.world;

public enum Biome {

    //          nome           surf         sub             mar       base       var   trees  snow        fog
    PLAINS   ("Plains",  Block.GRASS, Block.DIRT,      62,  65,  16,  1,  999, 0xB8D4E8),
    DESERT   ("Desert",  Block.SAND,  Block.SAND,      52,  66,  12,  2,  999, 0xE8D898),
    FOREST   ("Forest",  Block.GRASS, Block.DIRT,      62,  65,  20,  6,  999, 0x98C8A0),
    MOUNTAINS("Mounts",  Block.STONE, Block.STONE,     62,  82,  44,  1,   95, 0xC8D8E8),
    TAIGA    ("Taiga",   Block.SNOWY_GRASS, Block.DIRT,62,  65,  20,  5,  105, 0xA8C0B8),
    RAINFOREST ("RainForest",Block.DENSE_GRASS, Block.DIRT,  63,  65,  22, 12,  999, 0x48A060),
    TUNDRA   ("Tundra",  Block.SNOW,  Block.DIRT,      62,  63,  10,  0,   64, 0xC8D8E0),
    OCEAN    ("Ocean",   Block.SAND,  Block.SAND,      62,  34,   8,  0,  999, 0x607898);

    public final String name;
    public final Block  surfaceBlock, subsoilBlock;
    public final int    seaLevel, baseHeight, heightVar, treesPerChunk;
    public final int    snowLevel;
    public final int    fogColor;

    Biome(String n, Block s, Block sub, int sea, int base, int var, int t, int snow, int fog) {
        name = n; surfaceBlock = s; subsoilBlock = sub;
        seaLevel = sea; baseHeight = base; heightVar = var; treesPerChunk = t;
        snowLevel = snow; fogColor = fog;
    }

    public float fogR() { return ((fogColor >> 16) & 0xFF) / 255f; }
    public float fogG() { return ((fogColor >>  8) & 0xFF) / 255f; }
    public float fogB() { return ( fogColor        & 0xFF) / 255f; }

    private static double spread(double v) {
        if (v < 0.5) return 0.5 * Math.pow(2 * v, 1.6);
        else         return 1.0 - 0.5 * Math.pow(2 * (1 - v), 1.6);
    }

    public static Biome fromClimate(double rawTemp, double rawHumidity) {
        double temp = spread(rawTemp);
        double hum  = spread(rawHumidity);

        if (temp < 0.28) {
            return (hum < 0.52) ? Biome.TUNDRA : Biome.TAIGA;
        }

        if (temp < 0.50) {
            if (hum < 0.32) return Biome.PLAINS;
            if (hum < 0.68) return Biome.MOUNTAINS;  
            return Biome.TAIGA;
        }

        if (temp < 0.70) {
            if (hum < 0.28) return Biome.PLAINS;
            if (hum < 0.58) return Biome.FOREST;
            return Biome.RAINFOREST;
        }

        if (hum < 0.32) return Biome.DESERT;
        if (hum < 0.60) return Biome.PLAINS;
        return Biome.RAINFOREST;
    }
}