package com.mcraft.world;

public enum Biome {

     //          nome             surf        sub          mar    base     var     trees  snowY     fog(RGB int)
    PLAINS   ("Plains",  Block.GRASS, Block.DIRT,    64, 63, 22, 1,  999, 0xB8D4E8),
    DESERT   ("Desert",  Block.SAND,  Block.SAND,    58, 60, 14, 4,  999, 0xE8D898),
    FOREST   ("Forest",  Block.GRASS, Block.DIRT,    64, 64, 24, 6,  999, 0x98C8A0),
    MOUNTAINS("Mounts",  Block.STONE, Block.STONE,   64, 82, 38, 1,   95, 0xC8D8E8),
    TAIGA    ("Taiga",   Block.GRASS, Block.DIRT,    64, 64, 26, 5,  105, 0xA8C0B8),
    TUNDRA   ("Tundra",  Block.SNOW,  Block.DIRT,    64, 68, 12, 0,   64, 0xC8D8E0),
    SWAMP    ("Swamp",   Block.GRASS, Block.DIRT,    65, 63, 10, 3,  999, 0x78907A),
    OCEAN    ("Ocean",   Block.SAND,  Block.SAND,    72, 36, 10, 0,  999, 0x607898);

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

    public static Biome fromClimate(double temp, double humidity) {
        if (temp < 0.28) {
            if (humidity < 0.42) return TUNDRA;
            return TAIGA;
        }

        if (temp < 0.52) {
            if (humidity < 0.28) return PLAINS;
            if (humidity < 0.66) return FOREST;
            return FOREST;
        }

        if (temp < 0.72) {
            if (humidity < 0.22) return PLAINS;
            if (humidity < 0.52) return FOREST;
            if (humidity < 0.72) return FOREST;
            return SWAMP;
        }

        if (humidity < 0.32) return DESERT;
        if (humidity < 0.58) return MOUNTAINS;
        return SWAMP;
    }
}