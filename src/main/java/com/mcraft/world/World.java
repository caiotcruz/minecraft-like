package com.mcraft.world;

import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.mcraft.render.Camera;
import com.mcraft.render.Frustum;
import com.mcraft.render.Shader;

public class World {

    public static final int RENDER_DISTANCE = 4;
    private static final int UNLOAD_DISTANCE = RENDER_DISTANCE + 3;

    private final Frustum frustum = new Frustum();

    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final WorldGen gen;
    private WorldIO worldIO; 
    private long seed;

    private final Set<Long> pendingGeneration = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<Chunk> readyChunks = new ConcurrentLinkedQueue<>();


    private final ExecutorService chunkGenPool = Executors.newFixedThreadPool(
        Math.max(1, Runtime.getRuntime().availableProcessors() - 1)
    );

    
    public World(long seed, WorldIO worldIO) {
        this.seed = seed;
        this.worldIO = worldIO;
        this.gen = new WorldGen(seed);
    }

    private static long key(int cx, int cz) {
        return ((long) cx & 0xFFFFFFFFL) | (((long) cz & 0xFFFFFFFFL) << 32);
    }

    public Chunk getOrCreate(int cx, int cz) {
        return chunks.computeIfAbsent(key(cx, cz), k -> {
            Chunk c = new Chunk(cx, cz);

            if (worldIO != null) {
                worldIO.loadChunkBlocks(cx, cz).ifPresentOrElse(
                    c::setBlocks,
                    () -> {
                        byte[] data = gen.generateChunk(cx, cz, Chunk.SIZE, Chunk.HEIGHT);
                        c.setBlocks(data);
                    }
                );
            } else {
                byte[] data = gen.generateChunk(cx, cz, Chunk.SIZE, Chunk.HEIGHT);
                c.setBlocks(data);
            }
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
    public void generateInitialArea(float worldX, float worldZ) {
        int cx = Math.floorDiv((int) worldX, Chunk.SIZE);
        int cz = Math.floorDiv((int) worldZ, Chunk.SIZE);

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {

                int tcx = cx + dx;
                int tcz = cz + dz;

                long k = key(tcx, tcz);

                if (chunks.containsKey(k)) continue;

                Chunk c = new Chunk(tcx, tcz);

                if (worldIO != null) {
                    worldIO.loadChunkBlocks(tcx, tcz).ifPresentOrElse(
                        c::setBlocks,
                        () -> c.setBlocks(
                            gen.generateChunk(tcx, tcz, Chunk.SIZE, Chunk.HEIGHT)
                        )
                    );
                } else {
                    c.setBlocks(
                        gen.generateChunk(tcx, tcz, Chunk.SIZE, Chunk.HEIGHT)
                    );
                }

                chunks.put(k, c);
            }
        }
    }

    public void generateAround(float worldX, float worldZ) {
        int cx = Math.floorDiv((int) worldX, Chunk.SIZE);
        int cz = Math.floorDiv((int) worldZ, Chunk.SIZE);

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                int tcx = cx + dx, tcz = cz + dz;
                long k  = key(tcx, tcz);

                if (chunks.containsKey(k) || pendingGeneration.contains(k)) continue;

                pendingGeneration.add(k);
                int fcx = tcx, fcz = tcz; 
                chunkGenPool.submit(() -> {
                    Chunk c = new Chunk(fcx, fcz);

                    if (worldIO != null) {
                        worldIO.loadChunkBlocks(fcx, fcz).ifPresentOrElse(
                            c::setBlocks,
                            () -> c.setBlocks(gen.generateChunk(fcx, fcz, Chunk.SIZE, Chunk.HEIGHT))
                        );
                    } else {
                        c.setBlocks(gen.generateChunk(fcx, fcz, Chunk.SIZE, Chunk.HEIGHT));
                    }

                    readyChunks.add(c); 
                });
            }
        }
    }

    public void integrateReady() {
        Chunk c;
        int maxPerFrame = 4; 
        while (maxPerFrame-- > 0 && (c = readyChunks.poll()) != null) {
            long k = key(c.getChunkX(), c.getChunkZ());
            chunks.put(k, c);
            pendingGeneration.remove(k);
        }
    }

    public void shutdown() {
        chunkGenPool.shutdown();

        try {
            if (!chunkGenPool.awaitTermination(3, TimeUnit.SECONDS)) {
                chunkGenPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            chunkGenPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void render(Shader shader, Camera camera, float[] proj, float[] view) {
        float[] pv = multiply4x4(proj, view);
        frustum.update(pv);

        int cx = Math.floorDiv((int) camera.getX(), Chunk.SIZE);
        int cz = Math.floorDiv((int) camera.getZ(), Chunk.SIZE);

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {

                Chunk chunk = chunks.get(key(cx + dx, cz + dz));
                if (chunk == null) continue;

                float wx0 = chunk.getChunkX() * Chunk.SIZE;
                float wz0 = chunk.getChunkZ() * Chunk.SIZE;

                if (!frustum.isVisible(
                        wx0, 0, wz0,
                        wx0 + Chunk.SIZE, Chunk.HEIGHT,
                        wz0 + Chunk.SIZE)) {
                    continue;
                }

                chunk.render(shader, this);
            }
        }

        glDepthMask(false);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glDisable(GL_CULL_FACE);

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {

                Chunk chunk = chunks.get(key(cx + dx, cz + dz));
                if (chunk == null) continue;

                float wx0 = chunk.getChunkX() * Chunk.SIZE;
                float wz0 = chunk.getChunkZ() * Chunk.SIZE;

                if (!frustum.isVisible(
                        wx0, 0, wz0,
                        wx0 + Chunk.SIZE, Chunk.HEIGHT,
                        wz0 + Chunk.SIZE)) {
                    continue;
                }

                chunk.renderWater(shader);
            }
        }

        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);

        glDepthMask(true);
    }

    private static float[] multiply4x4(float[] a, float[] b) {
        float[] r = new float[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                for (int k = 0; k < 4; k++) {
                    r[col*4+row] += a[k*4+row] * b[col*4+k];
                }
            }
        }
        return r;
    }

    public int unloadDistant(float playerX, float playerZ, WorldIO worldIO) {
        int pcx = (int)Math.floor(playerX / Chunk.SIZE);
        int pcz = (int)Math.floor(playerZ / Chunk.SIZE);

        List<Long> toRemove = new ArrayList<>();

        for (Map.Entry<Long, Chunk> entry : chunks.entrySet()) {
            Chunk c  = entry.getValue();
            int   dx = Math.abs(c.getChunkX() - pcx);
            int   dz = Math.abs(c.getChunkZ() - pcz);

            if (dx > UNLOAD_DISTANCE || dz > UNLOAD_DISTANCE) {
                toRemove.add(entry.getKey());
            }
        }

        for (long key : toRemove) {
            Chunk chunk = chunks.get(key);
            if (chunk == null) continue;

            if (worldIO != null) {
                try {
                    worldIO.saveChunk(chunk);
                } catch (IOException e) {
                    System.err.printf("[Unload] Falha ao salvar chunk %d,%d: %s%n",
                            chunk.getChunkX(), chunk.getChunkZ(), e.getMessage());
                }
            }

            chunk.deleteMesh();

            chunks.remove(key);
        }

        if (!toRemove.isEmpty()) {
            System.out.printf("[Unload] %d chunks descarregados. Total: %d%n",
                    toRemove.size(), chunks.size());
        }

        return toRemove.size();
    }  

    public void saveAll(WorldIO worldIO) {
        for (Chunk chunk : chunks.values()) {
            try {
                worldIO.saveChunk(chunk);
            } catch (IOException e) {
                System.err.println("[Save] Falha: " + e.getMessage());
            }
        }
        System.out.println("[Save] " + chunks.size() + " chunks salvos.");
    }

    public void deleteAllMeshes() {
        for (Chunk chunk : chunks.values()) {
            chunk.deleteMesh();
        }
    }

    public Chunk getChunkIfLoaded(int cx, int cz) {
        return chunks.get(key(cx, cz));
    }

    public Map<Long, Chunk> getLoadedChunks() {
        return Collections.unmodifiableMap(chunks);
    }

    public long getSeed() { return seed; }
    public WorldIO getWorldIO() {return worldIO;}
}