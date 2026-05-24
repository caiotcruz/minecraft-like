package com.mcraft.world;

import java.util.Random;

public class WorldGen {

    private final int[] perm = new int[512];
    private final int[] permTemp; 
    private final int[] permHumid;

    private static final int[][] GRAD2 = {
        {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
        {1, 0}, {-1, 0}, { 0, 1}, { 0, -1}
    };

    public WorldGen(long seed) {
        Random rng = new Random(seed);
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;

        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = p[i]; p[i] = p[j]; p[j] = tmp;
        }
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
        permTemp  = buildPerm(new Random(seed ^ 0xDEADBEEFL));
        permHumid = buildPerm(new Random(seed ^ 0xCAFEBABEL));

    }


    private double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }

    private double lerp(double t, double a, double b) { return a + t * (b - a); }

    private double grad(int hash, double x, double y) {
        int[] g = GRAD2[hash & 7];
        return g[0] * x + g[1] * y;
    }

    public double noise(double x, double y) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;
        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);
        double u  = fade(xf);
        double v  = fade(yf);

        int aa = perm[perm[xi    ] + yi    ];
        int ab = perm[perm[xi    ] + yi + 1];
        int ba = perm[perm[xi + 1] + yi    ];
        int bb = perm[perm[xi + 1] + yi + 1];

        return lerp(v,
            lerp(u, grad(aa, xf,     yf    ), grad(ba, xf - 1, yf    )),
            lerp(u, grad(ab, xf,     yf - 1), grad(bb, xf - 1, yf - 1))
        );
    }

    public double fbm(double x, double z, int octaves, double scale, double persistence) {
        double total     = 0;
        double amplitude = 1.0;
        double frequency = 1.0 / scale;
        double maxVal    = 0;

        for (int i = 0; i < octaves; i++) {
            total    += noise(x * frequency, z * frequency) * amplitude;
            maxVal   += amplitude;
            amplitude *= persistence;
            frequency *= 2.0;
        }
        return total / maxVal;
    }

    public byte[] generateChunk(int chunkX, int chunkZ,
                                  int size, int height) {
        byte[] blocks = new byte[size * height * size];

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                double wx = chunkX * size + x;
                double wz = chunkZ * size + z;

                Biome biome = getBiome(wx, wz);

                double n = fbm(wx, wz, 5, 120.0, 0.5);
                int surfaceY = biome.baseHeight + (int)(n * biome.heightVar);
                surfaceY = Math.max(1, Math.min(height - 2, surfaceY));

                int seaLevel = biome.seaLevel;

                for (int y = 0; y < height; y++) {
                    int idx = y * size * size + z * size + x;

                    if (y == 0) {
                        blocks[idx] = (byte) Block.BEDROCK.id;
                    } else if (y < surfaceY - 4) {
                        blocks[idx] = (byte) Block.STONE.id;
                    } else if (y < surfaceY) {
                        blocks[idx] = (byte) biome.subsoilBlock.id;
                    } else if (y == surfaceY) {
                        blocks[idx] = (y <= seaLevel)
                            ? (byte) Block.SAND.id
                            : (byte) biome.surfaceBlock.id;
                    } else {
                        blocks[idx] = (y <= seaLevel)
                            ? (byte) Block.WATER.id
                            : (byte) Block.AIR.id;
                    }
                }
            }
        }

       generateTreesForBiome(blocks, chunkX, chunkZ, size, height);

        return blocks;
    }

    private void generateTreesForBiome(byte[] blocks, int chunkX, int chunkZ,
                                     int size, int height) {
        Random rng = new Random(
            (long) chunkX * 341_873_128_712L + (long) chunkZ * 132_897_987_541L);

        double cx = (chunkX + 0.5) * size, cz = (chunkZ + 0.5) * size;
        Biome biome = getBiome(cx, cz);
        int count = (int)(biome.treesPerChunk * (0.5 + rng.nextDouble() * 0.5));
    

        for (int t = 0; t < count; t++) {
            int tx = rng.nextInt(size - 4) + 2;
            int tz = rng.nextInt(size - 4) + 2;

            int ty = height - 1;
            while (ty > 0) {
                int idx = ty * size * size + tz * size + tx;
                if (blocks[idx] != Block.AIR.id) break;
                ty--;
            }

            int surfIdx = ty * size * size + tz * size + tx;
            if (ty <= 0 || ty >= height - 8) continue;
            if (blocks[surfIdx] != (byte) Block.GRASS.id) continue;

            int trunkH = 4 + rng.nextInt(2); 

            for (int dy = 1; dy <= trunkH; dy++) {
                int idx = (ty + dy) * size * size + tz * size + tx;
                if ((ty + dy) < height) blocks[idx] = (byte) Block.WOOD_LOG.id;
            }

            int leafBase = ty + trunkH;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = -1; dy <= 2; dy++) {
                        int lx = tx + dx, ly = leafBase + dy, lz = tz + dz;
                        if (lx < 0 || lx >= size || lz < 0 || lz >= size) continue;
                        if (ly < 0 || ly >= height) continue;

                        if (Math.max(Math.abs(dx), Math.max(Math.abs(dz), Math.abs(dy))) <= 2) {
                            int idx = ly * size * size + lz * size + lx;
                            if (blocks[idx] == Block.AIR.id) {
                                blocks[idx] = (byte) Block.LEAVES.id;
                            }
                        }
                    }
                }
            }
        }
    }

    private int[] buildPerm(Random rng) {
        int[] p = new int[512];
        int[] base = new int[256];
        for (int i = 0; i < 256; i++) base[i] = i;
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int t = base[i]; base[i] = base[j]; base[j] = t;
        }
        for (int i = 0; i < 512; i++) p[i] = base[i & 255];
        return p;
    }

    public double noiseWith(double x, double y, int[] p) {
        int xi = (int) Math.floor(x) & 255, yi = (int) Math.floor(y) & 255;
        double xf = x - Math.floor(x), yf = y - Math.floor(y);
        double u  = fade(xf), v = fade(yf);
        int aa = p[p[xi]+yi], ab = p[p[xi]+yi+1],
            ba = p[p[xi+1]+yi], bb = p[p[xi+1]+yi+1];
        return lerp(v,
            lerp(u, grad(aa, xf, yf),     grad(ba, xf-1, yf)),
            lerp(u, grad(ab, xf, yf-1),   grad(bb, xf-1, yf-1)));
    }

    public Biome getBiome(double wx, double wz) {
        double temp     = (noiseWith(wx / 512.0, wz / 512.0, permTemp)  + 1) * 0.5;
        double humidity = (noiseWith(wx / 512.0, wz / 512.0, permHumid) + 1) * 0.5;
        return Biome.fromClimate(temp, humidity);
    }
}