package com.mcraft.world;

public enum Biome {

    PLAINS          ("Plains",    Block.GRASS,  Block.DIRT,   64,   62,         20,       1),
    DESERT          ("Desert",    Block.SAND,   Block.SAND,   58,   58,         12,       0),
    FOREST          ("Forest",    Block.GRASS,  Block.DIRT,   64,   64,         22,       6),
    MOUNTAINS       ("Mountains", Block.STONE,  Block.STONE,  64,   80,         40,       2),
    TAIGA           ("Taiga",     Block.GRASS,  Block.DIRT,   64,   63,         25,       4),
    TUNDRA          ("Tundra",    Block.DIRT,   Block.DIRT,   64,   60,         10,       0),
    OCEAN           ("Ocean",     Block.SAND,   Block.SAND,   70,   40,         8,        0),
    SWAMP           ("Swamp",     Block.GRASS,  Block.DIRT,   65,   58,         8,        3);

    public final String name;
    public final Block  surfaceBlock;
    public final Block  subsoilBlock;
    public final int    seaLevel;
    public final int    baseHeight;  
    public final int    heightVar;  
    public final int    treesPerChunk;

    Biome(String name, Block s, Block sub, int sea, int base, int var, int trees) {
        this.name           = name;
        this.surfaceBlock   = s;
        this.subsoilBlock   = sub;
        this.seaLevel       = sea;
        this.baseHeight     = base;
        this.heightVar      = var;
        this.treesPerChunk  = trees;
    }

    public static Biome fromClimate(double temp, double humidity) {
        if (temp < 0.2) {
            return humidity < 0.3 ? TUNDRA : TAIGA;
        } else if (temp < 0.5) {
            if (humidity < 0.3)  return PLAINS;
            if (humidity < 0.65) return FOREST;
            return SWAMP;
        } else if (temp < 0.75) {
            if (humidity < 0.25) return DESERT;
            if (humidity < 0.5)  return PLAINS;
            return FOREST;
        } else {
            if (humidity < 0.2)  return DESERT;
            if (humidity < 0.5)  return PLAINS;
            return FOREST;
        }
    }

    public static int blendHeight(Biome a, Biome b, double weight,
                                   double noiseVal) {
        double ha = a.baseHeight + noiseVal * a.heightVar;
        double hb = b.baseHeight + noiseVal * b.heightVar;
        return (int)(ha * (1 - weight) + hb * weight);
    }
}