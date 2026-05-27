package com.mcraft.world;

import java.util.Random;

public class WorldGen {

    private final int[] perm = new int[512];
    private final int[] permTemp; 
    private final int[] permHumid;
    private final int[] permCont;

    private static final int[][] GRAD2 = {
        {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
        {1, 0}, {-1, 0}, { 0, 1}, { 0, -1}
    };

    private static final int[][] GRAD3 = {
        { 1, 1, 0}, {-1, 1, 0}, { 1,-1, 0}, {-1,-1, 0},
        { 1, 0, 1}, {-1, 0, 1}, { 1, 0,-1}, {-1, 0,-1},
        { 0, 1, 1}, { 0,-1, 1}, { 0, 1,-1}, { 0,-1,-1}
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
        permCont  = buildPerm(new Random(seed ^ 0xFEEDFACEL));

    }

    public double fbmWith(double x, double z, int octaves, double scale, double persistence, int[] p) { double total = 0, amplitude = 1.0, frequency = 1.0 / scale, maxVal = 0;
        for (int i = 0; i < octaves; i++) {
            total    += noiseWith(x * frequency, z * frequency, p) * amplitude;
            maxVal   += amplitude;
            amplitude *= persistence;
            frequency *= 2.0;
        }
        return total / maxVal;
    }

    private double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }

    private double lerp(double t, double a, double b) { return a + t * (b - a); }

    private double grad(int hash, double x, double y) {
        int[] g = GRAD2[hash & 7];
        return g[0] * x + g[1] * y;
    }

    private double grad3(int hash, double x, double y, double z) {
        int[] g = GRAD3[hash & 11];
        return g[0] * x + g[1] * y + g[2] * z;
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

    public double noise3D(double x, double y, double z) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;
        int zi = (int) Math.floor(z) & 255;

        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);
        double zf = z - Math.floor(z);

        double u = fade(xf), v = fade(yf), w = fade(zf);

        int aaa = perm[perm[perm[xi  ] + yi  ] + zi  ];
        int aab = perm[perm[perm[xi  ] + yi  ] + zi+1];
        int aba = perm[perm[perm[xi  ] + yi+1] + zi  ];
        int abb = perm[perm[perm[xi  ] + yi+1] + zi+1];
        int baa = perm[perm[perm[xi+1] + yi  ] + zi  ];
        int bab = perm[perm[perm[xi+1] + yi  ] + zi+1];
        int bba = perm[perm[perm[xi+1] + yi+1] + zi  ];
        int bbb = perm[perm[perm[xi+1] + yi+1] + zi+1];

        return lerp(w,
            lerp(v,
                lerp(u, grad3(aaa, xf,   yf,   zf  ), grad3(baa, xf-1, yf,   zf  )),
                lerp(u, grad3(aba, xf,   yf-1, zf  ), grad3(bba, xf-1, yf-1, zf  ))),
            lerp(v,
                lerp(u, grad3(aab, xf,   yf,   zf-1), grad3(bab, xf-1, yf,   zf-1)),
                lerp(u, grad3(abb, xf,   yf-1, zf-1), grad3(bbb, xf-1, yf-1, zf-1))));
    }

    private boolean shouldCarve(double wx, int y, double wz, int surfaceY) {
        double n1 = noise3D(wx / 20.0,         y / 15.0,          wz / 20.0);
        double n2 = noise3D(wx / 14.0 + 300.0, y / 12.0 + 300.0,  wz / 14.0 + 300.0);
        double caveVal = n1 * n1 + n2 * n2;

        double threshold = 0.065;

        int distToSurface = surfaceY - y;
        if (distToSurface <= 0)  return false; 
        if (distToSurface <= 10) threshold *= distToSurface / 10.0;

        if (y < 25) threshold *= 1.45;

        return caveVal < threshold;
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
                surfaceY = Math.max(2, Math.min(height - 2, surfaceY));

                for (int y = 0; y < height; y++) {
                    int idx = y * size * size + z * size + x;
                    if (y == 0) {
                        blocks[idx] = (byte) Block.BEDROCK.id;
                    } else if (y < surfaceY - 4) {
                        blocks[idx] = (byte) Block.STONE.id;
                    } else if (y < surfaceY) {
                        blocks[idx] = (byte) biome.subsoilBlock.id;
                    } else if (y == surfaceY) {
                        blocks[idx] = (y <= biome.seaLevel)
                            ? (byte) Block.SAND.id
                            : (byte) biome.surfaceBlock.id;
                    } else {
                        blocks[idx] = (y <= biome.seaLevel)
                            ? (byte) Block.WATER.id
                            : (byte) Block.AIR.id;
                    }
                }

                for (int y = 1; y < surfaceY; y++) {
                    int idx = y * size * size + z * size + x;
                    byte current = blocks[idx];

                    if (current != (byte) Block.STONE.id
                    && current != (byte) Block.DIRT.id) continue;

                    if (shouldCarve(wx, y, wz, surfaceY)) {
                        blocks[idx] = (y < 40)
                            ? (byte) Block.WATER.id
                            : (byte) Block.AIR.id;
                    }
                }

                for (int y = 1; y < surfaceY - 1; y++) {
                    int idx = y * size * size + z * size + x;
                    if (blocks[idx] != (byte) Block.STONE.id) continue;

                    byte ore = pickOre(wx, y, wz);
                    if (ore != 0) blocks[idx] = ore;
                }
            }
        }

       generateTreesForBiome(blocks, chunkX, chunkZ, size, height);

        return blocks;
    }

    private void generateTreesForBiome(byte[] blocks, int chunkX, int chunkZ, int size, int height) {
        double cx = (chunkX + 0.5) * size;
        double cz = (chunkZ + 0.5) * size;
        Biome biome = getBiome(cx, cz);

        int maxTrees = biome.treesPerChunk;
        if (maxTrees == 0) return;

        int count = chunkRandInt(chunkX, chunkZ, 0, maxTrees + 1);

        for (int t = 0; t < count; t++) {
            int tx = chunkRandInt(chunkX, chunkZ, t * 4 + 1, size - 4) + 2;
            int tz = chunkRandInt(chunkX, chunkZ, t * 4 + 2, size - 4) + 2;

            int ty = height - 1;
            while (ty > 0 && blocks[ty * size * size + tz * size + tx] == Block.AIR.id) ty--;
            if (ty <= 0 || ty >= height - 8) continue;
            if (blocks[ty * size * size + tz * size + tx] != (byte) Block.GRASS.id) continue;

            int trunkH = 4 + chunkRandInt(chunkX, chunkZ, t * 4 + 3, 2);

            for (int dy = 1; dy <= trunkH; dy++) {
                int y = ty + dy;
                if (y < height) blocks[y * size * size + tz * size + tx] = (byte) Block.WOOD_LOG.id;
            }

            int leafBase = ty + trunkH;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = -1; dy <= 2; dy++) {
                        int lx = tx + dx, ly = leafBase + dy, lz = tz + dz;
                        if (lx < 0 || lx >= size || lz < 0 || lz >= size) continue;
                        if (ly < 0 || ly >= height) continue;
                        if (Math.max(Math.abs(dx), Math.max(Math.abs(dz), Math.abs(dy))) > 2) continue;
                        int idx = ly * size * size + lz * size + lx;
                        if (blocks[idx] == Block.AIR.id) blocks[idx] = (byte) Block.LEAVES.id;
                    }
                }
            }
        }
    }

    private byte pickOre(double wx, int y, double wz) {

        //Diamante
        if (y >= 2 && y <= 16) {
            double nd = Math.abs(
                noise3D(wx / 5.5 + 400,
                        y  / 5.5 + 400,
                        wz / 5.5 + 400)
            );

            if (nd < 0.018)
                return (byte) Block.DIAMOND_ORE.id;
        }

        //Ouro
        if (y >= 2 && y <= 32) {
            double ng = Math.abs(
                noise3D(wx / 6.5 + 800,
                        y  / 6.5 + 800,
                        wz / 6.5 + 800)
            );

            if (ng < 0.024)
                return (byte) Block.GOLD_ORE.id;
        }

        //Ferro
        if (y >= 2 && y <= 64) {
            double ni = Math.abs(
                noise3D(wx / 8.0 + 1200,
                        y  / 8.0 + 1200,
                        wz / 8.0 + 1200)
            );

            if (ni < 0.035)
                return (byte) Block.IRON_ORE.id;
        }

        //Carvão
        if (y >= 2 && y <= 128) {
            double nc = Math.abs(
                noise3D(wx / 10.0 + 1600,
                        y  / 10.0 + 1600,
                        wz / 10.0 + 1600)
            );

            if (nc < 0.050)
                return (byte) Block.COAL_ORE.id;
        }

        return 0;
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

    private static int chunkRandInt(int cx, int cz, int idx, int bound) {
        long h = (long) cx * 341_873_128_712L
            ^ (long) cz * 132_897_987_541L
            ^ (long) idx * 16_764_573L;
        h = h * 2_685_548_017L + 0x5DEECE66DL;
        return (int) (((h >>> 33) & 0x7FFF_FFFFL) % bound);
    }

    public Biome getBiome(double wx, double wz) {
        double continental = fbmWith(wx, wz, 4, 1200.0, 0.55, permCont);
        if (continental < -0.18) return Biome.OCEAN;

        double temp     = (fbmWith(wx, wz, 3, 820.0, 0.58, permTemp)  + 1.0) * 0.5;
        double humidity = (fbmWith(wx, wz, 3, 640.0, 0.58, permHumid) + 1.0) * 0.5;

        return Biome.fromClimate(temp, humidity);
    }
}