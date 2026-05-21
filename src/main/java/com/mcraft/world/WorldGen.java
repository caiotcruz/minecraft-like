package com.mcraft.world;

import java.util.Random;

public class WorldGen {

    private final int[] permutation = new int[512];

    private static final int[][] GRAD2 = {
        {1,1}, {-1,1}, {1,-1}, {-1,-1},
        {1,0}, {-1,0}, {0,1},  {0,-1}
    };

    public WorldGen(long seed) {
        Random rng = new Random(seed);
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;

        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = p[i]; p[i] = p[j]; p[j] = tmp;
        }

        for (int i = 0; i < 512; i++) {
            permutation[i] = p[i & 255];
        }
    }

    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private double grad(int hash, double x, double y) {
        int[] g = GRAD2[hash & 7];
        return g[0] * x + g[1] * y;
    }

    public double noise(double x, double y) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;

        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);

        double u = fade(xf);
        double v = fade(yf);

        int aa = permutation[permutation[xi    ] + yi    ];
        int ab = permutation[permutation[xi    ] + yi + 1];
        int ba = permutation[permutation[xi + 1] + yi    ];
        int bb = permutation[permutation[xi + 1] + yi + 1];

        return lerp(v,
            lerp(u, grad(aa, xf,   yf  ), grad(ba, xf-1, yf  )),
            lerp(u, grad(ab, xf,   yf-1), grad(bb, xf-1, yf-1))
        );
    }

    /**
     * @param octaves   
     * @param scale     
     * @param persistence 
     */
    public double fbm(double x, double z, int octaves, double scale, double persistence) {
        double total = 0;
        double amplitude = 1.0;
        double frequency = 1.0 / scale;
        double maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }

        return total / maxValue; 
    }

    public byte[] generateChunk(int chunkX, int chunkZ,
                                  int chunkSize, int chunkHeight) {
        byte[] blocks = new byte[chunkSize * chunkHeight * chunkSize];

        for (int x = 0; x < chunkSize; x++) {
            for (int z = 0; z < chunkSize; z++) {

                double worldX = chunkX * chunkSize + x;
                double worldZ = chunkZ * chunkSize + z;

                double noiseVal = fbm(worldX, worldZ, 5, 150.0, 0.5);
                int surfaceY = (int) (64 + noiseVal * 32); 

                for (int y = 0; y < chunkHeight; y++) {
                    int idx = x + (y * chunkSize) + (z * chunkSize * chunkHeight);

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

        generateTrees(blocks, chunkX, chunkZ, chunkSize, chunkHeight);

        return blocks;
    }

    private void generateTrees(byte[] blocks, int chunkX, int chunkZ,
                                int chunkSize, int chunkHeight) {
        Random rng = new Random((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L);

        int treeCount = rng.nextInt(4) + 1; 
        for (int t = 0; t < treeCount; t++) {
            int tx = rng.nextInt(chunkSize - 4) + 2;
            int tz = rng.nextInt(chunkSize - 4) + 2;

            int ty = chunkHeight - 1;
            while (ty > 0 && blocks[tx + (ty * chunkSize) + (tz * chunkSize * chunkHeight)] == Block.AIR.id) {
                ty--;
            }

            if (ty <= 0 || ty >= chunkHeight - 8) continue;
            if (blocks[tx + (ty * chunkSize) + (tz * chunkSize * chunkHeight)] != Block.GRASS.id) continue;

            int trunkHeight = 4 + rng.nextInt(2);

            for (int dy = 1; dy <= trunkHeight; dy++) {
                int idx = tx + ((ty + dy) * chunkSize) + (tz * chunkSize * chunkHeight);
                blocks[idx] = (byte) Block.WOOD_LOG.id;
            }

            int leafTop = ty + trunkHeight;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = -1; dy <= 2; dy++) {
                        int lx = tx + dx;
                        int ly = leafTop + dy;
                        int lz = tz + dz;

                        if (lx < 0 || lx >= chunkSize || lz < 0 || lz >= chunkSize) continue;
                        if (ly < 0 || ly >= chunkHeight) continue;

                        if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy) <= 3) {
                            int idx = lx + (ly * chunkSize) + (lz * chunkSize * chunkHeight);
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