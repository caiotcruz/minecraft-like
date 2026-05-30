package com.mcraft.world;

public enum WeatherType {
    CLEAR, RAIN, SNOW;

    public static WeatherType forBiome(Biome b) {
        return switch (b) {
            case DESERT, OCEAN  -> CLEAR;
            case TUNDRA, TAIGA  -> SNOW;
            case MOUNTAINS      -> SNOW;
            default             -> RAIN;  
        };
    }
}