package com.mcraft.world;

import java.util.HashMap;
import java.util.Map;

import com.mcraft.render.Camera;
import com.mcraft.render.Shader;

public class World {

    public static final int RENDER_DISTANCE = 4;

    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final WorldGen gen;

    public World(long seed) {
        this.gen = new WorldGen(seed);
    }

    private static long key(int cx, int cz) {
        return ((long) cx & 0xFFFFFFFFL) | (((long) cz & 0xFFFFFFFFL) << 32);
    }

    public Chunk getOrCreate(int cx, int cz) {
        return chunks.computeIfAbsent(key(cx, cz), k -> {
            Chunk c = new Chunk(cx, cz);
            byte[] data = gen.generateChunk(cx, cz, Chunk.SIZE, Chunk.HEIGHT);
            c.setBlocks(data);
            return c;
        });
    }

    public Block getBlock(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) return Block.AIR;

        int cx = Math.floorDiv(x, Chunk.SIZE);
        int cz = Math.floorDiv(z, Chunk.SIZE);
        int lx = Math.floorMod(x, Chunk.SIZE);
        int lz = Math.floorMod(z, Chunk.SIZE);

        Chunk chunk = chunks.get(key(cx, cz));
        return (chunk != null) ? chunk.getBlock(lx, y, lz) : Block.AIR;
    }

    public void setBlock(int x, int y, int z, int blockId) {
        if (y < 0 || y >= Chunk.HEIGHT) return;

        int cx = Math.floorDiv(x, Chunk.SIZE);
        int cz = Math.floorDiv(z, Chunk.SIZE);
        int lx = Math.floorMod(x, Chunk.SIZE);
        int lz = Math.floorMod(z, Chunk.SIZE);

        Chunk chunk = getOrCreate(cx, cz);
        chunk.setBlock(lx, y, lz, blockId);

        if (lx == 0)              markDirty(cx - 1, cz);
        if (lx == Chunk.SIZE - 1) markDirty(cx + 1, cz);
        if (lz == 0)              markDirty(cx, cz - 1);
        if (lz == Chunk.SIZE - 1) markDirty(cx, cz + 1);
    }

    private void markDirty(int cx, int cz) {
        Chunk c = chunks.get(key(cx, cz));
        if (c != null) c.markDirty();
    }

    public void generateAround(float worldX, float worldZ) {
        int cx = Math.floorDiv((int) worldX, Chunk.SIZE);
        int cz = Math.floorDiv((int) worldZ, Chunk.SIZE);

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                getOrCreate(cx + dx, cz + dz);
            }
        }
    }

    public void render(Shader shader, Camera camera) {
        int cx = Math.floorDiv((int) camera.getX(), Chunk.SIZE);
        int cz = Math.floorDiv((int) camera.getZ(), Chunk.SIZE);

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                Chunk chunk = chunks.get(key(cx + dx, cz + dz));
                if (chunk != null) {
                    chunk.render(shader, this);
                }
            }
        }
    }
}