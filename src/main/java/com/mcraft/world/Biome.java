package com.mcraft.world;

public enum Biome {

    //             nome        superfície   subsolo      marNível  altBase  varAltura árvores
    PLAINS   ("Plains",   Block.GRASS, Block.DIRT,   64,  63,  22, 1),
    DESERT   ("Desert",   Block.SAND,  Block.SAND,   58,  60,  14, 0),
    FOREST   ("Forest",   Block.GRASS, Block.DIRT,   64,  64,  24, 6),
    MOUNTAINS("Mounts",   Block.STONE, Block.STONE,  64,  82,  38, 2),
    TAIGA    ("Taiga",    Block.GRASS, Block.DIRT,   64,  64,  26, 5),
    TUNDRA   ("Tundra",   Block.DIRT,  Block.DIRT,   64,  60,  12, 0),
    SWAMP    ("Swamp",    Block.GRASS, Block.DIRT,   66,  58,  10, 3),
    OCEAN    ("Ocean",    Block.SAND,  Block.SAND,   72,  36,  10, 0);

    public final String name;
    public final Block  surfaceBlock, subsoilBlock;
    public final int    seaLevel, baseHeight, heightVar, treesPerChunk;

    Biome(String n, Block s, Block sub, int sea, int base, int var, int t) {
        name = n; surfaceBlock = s; subsoilBlock = sub;
        seaLevel = sea; baseHeight = base; heightVar = var; treesPerChunk = t;
    }

    public static Biome fromClimate(double temp, double humidity) {
        if (temp < 0.28) {
            if (humidity < 0.42) return TUNDRA;
            return TAIGA;
        }

        if (temp < 0.52) {
            if (humidity < 0.28) return PLAINS;
            if (humidity < 0.66) return MOUNTAINS;
            return FOREST;
        }

        if (temp < 0.72) {
            if (humidity < 0.22) return PLAINS;
            if (humidity < 0.52) return FOREST;
            if (humidity < 0.72) return FOREST;
            return SWAMP;
        }

        if (humidity < 0.32) return DESERT;
        if (humidity < 0.58) return PLAINS;
        return SWAMP;
    }
}