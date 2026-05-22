package com.mcraft.world;

import java.util.Random;

public class WorldGen {

    private final int[] perm = new int[512];

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

                double n       = fbm(wx, wz, 5, 120.0, 0.5);
                int surfaceY   = (int) (64 + n * 28);
                surfaceY       = Math.max(1, Math.min(height - 2, surfaceY));

                for (int y = 0; y < height; y++) {
                    int idx = y * size * size + z * size + x; 

                    if (y == 0) {
                        blocks[idx] = (byte) Block.BEDROCK.id;
                    } else if (y < surfaceY - 4) {
                        blocks[idx] = (byte) Block.STONE.id;
                    } else if (y < surfaceY) {
                        blocks[idx] = (byte) Block.DIRT.id;
                    } else if (y == surfaceY) {
                        blocks[idx] = (y <= 64)
                            ? (byte) Block.SAND.id
                            : (byte) Block.GRASS.id;
                    } else {
                        blocks[idx] = (y <= 64)
                            ? (byte) Block.WATER.id
                            : (byte) Block.AIR.id;
                    }
                }
            }
        }

        generateTrees(blocks, chunkX, chunkZ, size, height);

        return blocks;
    }

    private void generateTrees(byte[] blocks, int chunkX, int chunkZ,
                                int size, int height) {
        Random rng = new Random(
            (long) chunkX * 341_873_128_712L + (long) chunkZ * 132_897_987_541L
        );

        int count = rng.nextInt(4) + 1; 

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
}