package com.mcraft.world;

import com.mcraft.render.ChunkRenderer;
import com.mcraft.render.Shader;

public class Chunk {

    public static final int SIZE   = 16;
    public static final int HEIGHT = 128;

    private final byte[] blocks = new byte[SIZE * HEIGHT * SIZE];

    private final int chunkX, chunkZ; 
    private boolean dirty = true;       

    private final ChunkRenderer renderer = new ChunkRenderer();

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }


    private static int idx(int x, int y, int z) {
        return y * SIZE * SIZE + z * SIZE + x;
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

    public void markDirty() { dirty = true; }
    public int  getChunkX() { return chunkX; }
    public int  getChunkZ() { return chunkZ; }
    public byte[] getRawBlocks() { return blocks; }

}