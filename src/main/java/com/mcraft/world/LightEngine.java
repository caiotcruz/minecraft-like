package com.mcraft.world;

import java.util.ArrayDeque;
import java.util.Queue;

public final class LightEngine {

    private static final int[][] DIRS = {
        {1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}
    };

    public static void calculateChunkLight(Chunk chunk, World world) {
        chunk.clearLight();

        int cx = chunk.getChunkX() * Chunk.SIZE;
        int cz = chunk.getChunkZ() * Chunk.SIZE;

        Queue<long[]> queue = new ArrayDeque<>(1024);

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
                    Block b = chunk.getBlock(x, y, z);

                    if (b == Block.WATER) {
                        int above = (y + 1 < Chunk.HEIGHT)
                            ? chunk.getSkyLight(x, y+1, z) : 15;
                        int waterLight = Math.max(0, above - 1);
                        if (waterLight > 0) {
                            chunk.setSkyLight(x, y, z, waterLight);
                            queue.add(pack(cx+x, y, cz+z, waterLight, true));
                        }
                        continue;
                    }

                    if (b.solid) break;

                    chunk.setSkyLight(x, y, z, 15);
                    queue.add(pack(cx+x, y, cz+z, 15, true));
                }
            }
        }

        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                for (int x = 0; x < Chunk.SIZE; x++) {
                    Block b = chunk.getBlock(x, y, z);
                    if (b.lightEmission > 0) {
                        chunk.setBlockLight(x, y, z, b.lightEmission);
                        queue.add(pack(cx+x, y, cz+z, b.lightEmission, false));
                    }
                }
            }
        }

        bfsPropagate(queue, world);

        chunk.setLightDirty(false);
        chunk.markDirty(); 
    }

    public static void onBlockChanged(World world, int bx, int by, int bz) {
        int chunkCX = Math.floorDiv(bx, Chunk.SIZE);
        int chunkCZ = Math.floorDiv(bz, Chunk.SIZE);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Chunk c = world.getChunkIfLoaded(chunkCX+dx, chunkCZ+dz);
                if (c != null) c.setLightDirty(true);
            }
        }
    }

    static void propagateBlockLightFrom(World world,
                                                int bx, int by, int bz,
                                                int level) {
        Queue<long[]> q = new ArrayDeque<>();
        world.setBlockLightAt(bx, by, bz, level);
        q.add(pack(bx, by, bz, level, false));
        bfsPropagate(q, world);
        int cx = Math.floorDiv(bx, Chunk.SIZE);
        int cz = Math.floorDiv(bz, Chunk.SIZE);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Chunk c = world.getChunkIfLoaded(cx+dx, cz+dz);
                if (c != null) c.markDirty();
            }
        }
    }

    private static void bfsPropagate(Queue<long[]> queue, World world) {
        while (!queue.isEmpty()) {
            long[] e   = queue.poll();
            int    px  = (int)e[0];
            int    py  = (int)e[1];
            int    pz  = (int)e[2];
            int    lvl = (int)e[3];
            boolean sky = (e[4] == 1);

            if (lvl <= 1) continue;
            int newLvl = lvl - 1;

            for (int[] dir : DIRS) {
                int nx = px+dir[0], ny = py+dir[1], nz = pz+dir[2];
                if (ny < 0 || ny >= Chunk.HEIGHT) continue;

                Block nb = world.getBlock(nx, ny, nz);
                if (nb.solid && nb.lightEmission == 0 && nb != Block.WATER) continue;

                int curLight = sky ? world.getSkyLightAt  (nx, ny, nz)
                                   : world.getBlockLightAt(nx, ny, nz);
                if (newLvl > curLight) {
                    if (sky) world.setSkyLightAt  (nx, ny, nz, newLvl);
                    else     world.setBlockLightAt(nx, ny, nz, newLvl);
                    queue.add(pack(nx, ny, nz, newLvl, sky));
                }
            }
        }
    }

    private static long[] pack(int x, int y, int z, int lvl, boolean sky) {
        return new long[]{ x, y, z, lvl, sky ? 1 : 0 };
    }
}