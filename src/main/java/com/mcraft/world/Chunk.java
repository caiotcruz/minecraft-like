package com.mcraft.world;

import com.mcraft.render.ChunkRenderer;
import com.mcraft.render.Shader;

public class Chunk {

    public static final int SIZE   = 16;
    public static final int HEIGHT = 128;

    private final byte[] blocks = new byte[SIZE * HEIGHT * SIZE];

    private final int chunkX, chunkZ; 
    private boolean dirty = true;       

    private final byte[] lightPacked = new byte[SIZE * HEIGHT * SIZE];
    private final byte[] lightStaging = new byte[SIZE * HEIGHT * SIZE];
    private volatile boolean lightPending      = false;
    private volatile boolean lightStagingReady = false;
    private boolean lightDirty = true;

    private final ChunkRenderer renderer = new ChunkRenderer();

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }


    private int idx(int x, int y, int z) { 
        return y*SIZE*SIZE + z*SIZE + x; 
    }

    public Block getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) {
            return Block.AIR;
        }
        return Block.fromId(blocks[idx(x, y, z)] & 0xFF);
    }

    public void setBlock(int x, int y, int z, int blockId) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) return;
        blocks[idx(x, y, z)] = (byte) blockId;
        dirty = true;
    }

    public void setBlocks(byte[] data) {
        int len = Math.min(data.length, blocks.length);
        System.arraycopy(data, 0, blocks, 0, len);
        dirty = true;
    }

   
    public void render(Shader shader, World world) {
        if (dirty) {
            renderer.buildMesh(this, world);
            dirty = false;
        }

        float[] model = {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            chunkX * (float) SIZE, 0, chunkZ * (float) SIZE, 1  
        };
        shader.setMatrix4("uModel", model);

        renderer.render();
    }

    public void renderWater(Shader shader) {
        float[] model = {
            1,0,0,0, 0,1,0,0, 0,0,1,0,
            chunkX*(float)Chunk.SIZE, 0, chunkZ*(float)Chunk.SIZE, 1
        };
        shader.setMatrix4("uModel", model);
        renderer.renderWater();
    }

    public void deleteMesh() {
        renderer.delete(); 
        dirty = true;     
    }

    public int getSkyLight(int x, int y, int z) {
        if (!inBounds(x,y,z)) return 0;
        return (lightPacked[idx(x,y,z)] >> 4) & 0xF;
    }

    public int getBlockLight(int x, int y, int z) {
        if (!inBounds(x,y,z)) return 0;
        return lightPacked[idx(x,y,z)] & 0xF;
    }

    public int getEffectiveLight(int x, int y, int z) {
        if (!inBounds(x,y,z)) return 0;
        byte packed = lightPacked[idx(x,y,z)];
        return Math.max((packed >> 4) & 0xF, packed & 0xF);
    }

    public void setSkyLight(int x, int y, int z, int v) {
        if (!inBounds(x,y,z)) return;
        int i = idx(x,y,z);
        lightPacked[i] = (byte)((lightPacked[i] & 0x0F) | (clamp15(v) << 4));
    }

    public void setBlockLight(int x, int y, int z, int v) {
        if (!inBounds(x,y,z)) return;
        int i = idx(x,y,z);
        lightPacked[i] = (byte)((lightPacked[i] & 0xF0) | clamp15(v));
    }

    public void clearLight() {
        java.util.Arrays.fill(lightPacked, (byte)0);
    }

    public void markStagingReady() {
        lightStagingReady = true;
        lightPending      = false;
    }

    public void commitLightStaging() {
        System.arraycopy(lightStaging, 0, lightPacked, 0, lightPacked.length);
        lightStagingReady = false;
        dirty             = true;
    }

    public void setLightPending(boolean v) { 
        lightPending = v; 
    }

    public void markDirty() { dirty = true; }
    public int  getChunkX() { return chunkX; }
    public int  getChunkZ() { return chunkZ; }
    public byte[] getRawBlocks() { return blocks; }
    public boolean isLightDirty()      { return lightDirty; }
    public void setLightDirty(boolean d) { lightDirty = d; }
    public byte[] getLightStagingBuffer() { return lightStaging; }
    public boolean isLightPending()      { return lightPending; }
    public boolean isLightStagingReady() { return lightStagingReady; }
    public ChunkRenderer getChunkRenderer() {return renderer;}

    private static int clamp15(int v) { return Math.max(0, Math.min(15, v)); }
    private boolean inBounds(int x, int y, int z) {
        return x>=0 && x<SIZE && y>=0 && y<HEIGHT && z>=0 && z<SIZE;
    }

}