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

        // Fisher-Yates shuffle
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = p[i]; p[i] = p[j]; p[j] = tmp;
        }
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
    }

    // ── Perlin Noise 2D ──────────────────────────────────────────────────────

    /** Suavização cúbica de Perlin: 6t⁵ − 15t⁴ + 10t³ */
    private double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }

    private double lerp(double t, double a, double b) { return a + t * (b - a); }

    private double grad(int hash, double x, double y) {
        int[] g = GRAD2[hash & 7];
        return g[0] * x + g[1] * y;
    }

    /** Ruído de Perlin 2D normalizado em [−1, 1]. */
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

    /**
     * Fractal Brownian Motion: soma N oitavas de noise com frequência dobrada
     * e amplitude reduzida à metade a cada camada.
     *
     * Resultado em [−1, 1], normalizado pela soma das amplitudes.
     */
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

    // ── Geração de chunk ─────────────────────────────────────────────────────

    /**
     * Gera o array de blocos para um chunk inteiro.
     * Retorna byte[SIZE × HEIGHT × SIZE] com o mesmo layout de Chunk.java:
     * índice = y × SIZE² + z × SIZE + x
     */
    public byte[] generateChunk(int chunkX, int chunkZ,
                                  int size, int height) {
        byte[] blocks = new byte[size * height * size];

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                double wx = chunkX * size + x;  // Coordenada global
                double wz = chunkZ * size + z;

                // Altura da superfície: fBm mapeado para [SEA - 20, SEA + 32]
                double n       = fbm(wx, wz, 5, 120.0, 0.5);
                int surfaceY   = (int) (64 + n * 28);
                surfaceY       = Math.max(1, Math.min(height - 2, surfaceY));

                for (int y = 0; y < height; y++) {
                    int idx = y * size * size + z * size + x; // ← índice corrigido

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
                        // Água preenche vales abaixo do nível do mar
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

    // ── Geração de árvores ───────────────────────────────────────────────────

    private void generateTrees(byte[] blocks, int chunkX, int chunkZ,
                                int size, int height) {
        // Seed determinístico por chunk: mesma seed → mesmas árvores sempre
        Random rng = new Random(
            (long) chunkX * 341_873_128_712L + (long) chunkZ * 132_897_987_541L
        );

        int count = rng.nextInt(4) + 1; // 1–4 árvores por chunk

        for (int t = 0; t < count; t++) {
            // Margem de 2 blocos para que a copa caiba dentro do chunk
            int tx = rng.nextInt(size - 4) + 2;
            int tz = rng.nextInt(size - 4) + 2;

            // Encontra a superfície nessa coluna
            int ty = height - 1;
            while (ty > 0) {
                int idx = ty * size * size + tz * size + tx;
                if (blocks[idx] != Block.AIR.id) break;
                ty--;
            }

            // Planta árvore apenas em grama, longe do teto
            int surfIdx = ty * size * size + tz * size + tx;
            if (ty <= 0 || ty >= height - 8) continue;
            if (blocks[surfIdx] != (byte) Block.GRASS.id) continue;

            int trunkH = 4 + rng.nextInt(2); // 4 ou 5 blocos de tronco

            // Tronco
            for (int dy = 1; dy <= trunkH; dy++) {
                int idx = (ty + dy) * size * size + tz * size + tx;
                if ((ty + dy) < height) blocks[idx] = (byte) Block.WOOD_LOG.id;
            }

            // Copa esférica de folhas (raio 2 blocos)
            int leafBase = ty + trunkH;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = -1; dy <= 2; dy++) {
                        int lx = tx + dx, ly = leafBase + dy, lz = tz + dz;
                        if (lx < 0 || lx >= size || lz < 0 || lz >= size) continue;
                        if (ly < 0 || ly >= height) continue;

                        // Esfera por distância de Chebyshev ≤ 2
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