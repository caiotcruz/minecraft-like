package com.mcraft.world;

import java.util.Random;

public class WorldGen {

    private final int[] permutation = new int[512];

    // Gradientes 2D para o noise
    private static final int[][] GRAD2 = {
        {1,1}, {-1,1}, {1,-1}, {-1,-1},
        {1,0}, {-1,0}, {0,1},  {0,-1}
    };

    public WorldGen(long seed) {
        Random rng = new Random(seed);
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;

        // Embaralha usando Fisher-Yates
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = p[i]; p[i] = p[j]; p[j] = tmp;
        }

        // Duplica para evitar overflow de índice
        for (int i = 0; i < 512; i++) {
            permutation[i] = p[i & 255];
        }
    }

    /** Função smoothstep cúbica de Perlin: 6t⁵ - 15t⁴ + 10t³ */
    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    /** Interpolação linear */
    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    /** Produto escalar do gradiente com vetor de distância */
    private double grad(int hash, double x, double y) {
        int[] g = GRAD2[hash & 7];
        return g[0] * x + g[1] * y;
    }

    /** Ruído de Perlin 2D em um único ponto */
    public double noise(double x, double y) {
        // Célula da grade
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;

        // Posição dentro da célula [0, 1]
        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);

        // Curvas de suavização
        double u = fade(xf);
        double v = fade(yf);

        // Hashes dos 4 cantos da célula
        int aa = permutation[permutation[xi    ] + yi    ];
        int ab = permutation[permutation[xi    ] + yi + 1];
        int ba = permutation[permutation[xi + 1] + yi    ];
        int bb = permutation[permutation[xi + 1] + yi + 1];

        // Interpolação bilinear
        return lerp(v,
            lerp(u, grad(aa, xf,   yf  ), grad(ba, xf-1, yf  )),
            lerp(u, grad(ab, xf,   yf-1), grad(bb, xf-1, yf-1))
        );
    }

    /**
     * fBm — soma de oitavas de noise.
     * Cada oitava dobra a frequência e reduz a amplitude pela metade.
     *
     * @param octaves   Número de camadas de detalhe (4–6 é bom)
     * @param scale     Escala base (quanto menor, mais "zoom")
     * @param persistence Quanto a amplitude cai por oitava (0.5 é padrão)
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

        return total / maxValue; // Normaliza para [-1, 1]
    }

    /**
     * Gera o array de blocos para um chunk inteiro.
     * O chunk tem dimensões: CHUNK_SIZE x CHUNK_HEIGHT x CHUNK_SIZE
     */
    public byte[] generateChunk(int chunkX, int chunkZ,
                                  int chunkSize, int chunkHeight) {
        byte[] blocks = new byte[chunkSize * chunkHeight * chunkSize];

        for (int x = 0; x < chunkSize; x++) {
            for (int z = 0; z < chunkSize; z++) {

                // Coordenada global do bloco
                double worldX = chunkX * chunkSize + x;
                double worldZ = chunkZ * chunkSize + z;

                // Altura do terreno: fBm normalizado para [SEA_LEVEL, MAX_HEIGHT]
                double noiseVal = fbm(worldX, worldZ, 5, 150.0, 0.5);
                int surfaceY = (int) (64 + noiseVal * 32); // base 64, variação ±32

                for (int y = 0; y < chunkHeight; y++) {
                    int idx = x + (y * chunkSize) + (z * chunkSize * chunkHeight);

                    if (y == 0) {
                        blocks[idx] = (byte) Block.BEDROCK.id;
                    } else if (y < surfaceY - 4) {
                        blocks[idx] = (byte) Block.STONE.id;
                    } else if (y < surfaceY) {
                        blocks[idx] = (byte) Block.DIRT.id;
                    } else if (y == surfaceY) {
                        // Abaixo do nível do mar vira areia
                        blocks[idx] = (y <= 64)
                            ? (byte) Block.SAND.id
                            : (byte) Block.GRASS.id;
                    } else {
                        // Agua nos vales submersos
                        blocks[idx] = (y <= 64)
                            ? (byte) Block.WATER.id
                            : (byte) Block.AIR.id;
                    }
                }
            }
        }

        // Gera árvores simples em posições determinísticas
        generateTrees(blocks, chunkX, chunkZ, chunkSize, chunkHeight);

        return blocks;
    }

    private void generateTrees(byte[] blocks, int chunkX, int chunkZ,
                                int chunkSize, int chunkHeight) {
        Random rng = new Random((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L);

        int treeCount = rng.nextInt(4) + 1; // 1–4 árvores por chunk

        for (int t = 0; t < treeCount; t++) {
            int tx = rng.nextInt(chunkSize - 4) + 2;
            int tz = rng.nextInt(chunkSize - 4) + 2;

            // Encontra a superfície
            int ty = chunkHeight - 1;
            while (ty > 0 && blocks[tx + (ty * chunkSize) + (tz * chunkSize * chunkHeight)] == Block.AIR.id) {
                ty--;
            }

            if (ty <= 0 || ty >= chunkHeight - 8) continue;
            if (blocks[tx + (ty * chunkSize) + (tz * chunkSize * chunkHeight)] != Block.GRASS.id) continue;

            int trunkHeight = 4 + rng.nextInt(2);

            // Tronco
            for (int dy = 1; dy <= trunkHeight; dy++) {
                int idx = tx + ((ty + dy) * chunkSize) + (tz * chunkSize * chunkHeight);
                blocks[idx] = (byte) Block.WOOD_LOG.id;
            }

            // Copa esférica de folhas (raio 2)
            int leafTop = ty + trunkHeight;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = -1; dy <= 2; dy++) {
                        int lx = tx + dx;
                        int ly = leafTop + dy;
                        int lz = tz + dz;

                        if (lx < 0 || lx >= chunkSize || lz < 0 || lz >= chunkSize) continue;
                        if (ly < 0 || ly >= chunkHeight) continue;

                        // Esfera aproximada: distância Manhattan ≤ 2
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