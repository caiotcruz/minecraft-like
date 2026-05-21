package com.mcraft.world;

public class Chunk {

    public static final int SIZE   = 16;
    public static final int HEIGHT = 256;

    private final int chunkX;
    private final int chunkZ;

    private final byte[] blocks;

    public Chunk(int chunkX, int chunkZ) {

        this.chunkX = chunkX;
        this.chunkZ = chunkZ;

        this.blocks = new byte[SIZE * HEIGHT * SIZE];
    }

    public Chunk(int chunkX, int chunkZ, byte[] blocks) {

        this.chunkX = chunkX;
        this.chunkZ = chunkZ;

        if (blocks == null || blocks.length != SIZE * HEIGHT * SIZE) {
            throw new IllegalArgumentException(
                    "Array de blocos inválido para chunk."
            );
        }

        this.blocks = blocks;
    }

    private int index(int x, int y, int z) {

        return x
                + (y * SIZE)
                + (z * SIZE * HEIGHT);
    }

    public boolean isInside(int x, int y, int z) {

        return x >= 0 && x < SIZE
                && y >= 0 && y < HEIGHT
                && z >= 0 && z < SIZE;
    }

    public Block getBlock(int x, int y, int z) {

        if (!isInside(x, y, z)) {
            return Block.AIR;
        }

        int idx = index(x, y, z);

        return Block.fromId(blocks[idx] & 0xFF);
    }

    public Block getBlockSafe(int x, int y, int z) {

        if (!isInside(x, y, z)) {
            return Block.AIR;
        }

        return getBlock(x, y, z);
    }

    public void setBlock(int x, int y, int z, int blockId) {

        if (!isInside(x, y, z)) {
            return;
        }

        blocks[index(x, y, z)] = (byte) blockId;
    }

    public byte[] getBlocks() {

        return blocks;
    }

    public int getChunkX() {

        return chunkX;
    }

    public int getChunkZ() {

        return chunkZ;
    }

    public int getWorldX() {

        return chunkX * SIZE;
    }

    public int getWorldZ() {

        return chunkZ * SIZE;
    }
}