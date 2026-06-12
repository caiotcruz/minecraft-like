package com.mcraft.render;

import com.mcraft.world.Chunk;

public class Frustum2D {

    private static final float CHUNK_RADIUS =
        (float)(Chunk.SIZE * Math.sqrt(2.0)) * 0.5f;

    private static final float MARGIN = CHUNK_RADIUS + 2f;

    private float camX, camZ;
    private float forwardX, forwardZ; 
    private float halfFovCos;

    public void update(float camX, float camZ, float yaw, float fovDegrees) {
        this.camX = camX;
        this.camZ = camZ;
        this.forwardX = (float)Math.cos(yaw);
        this.forwardZ = (float)Math.sin(yaw);
        float halfFovRad = (float) Math.toRadians(fovDegrees * 0.5f + 15f);
        this.halfFovCos  = (float) Math.cos(halfFovRad);
    }

    public boolean isChunkVisible(int chunkX, int chunkZ) {
        float cx = chunkX * Chunk.SIZE + Chunk.SIZE * 0.5f;
        float cz = chunkZ * Chunk.SIZE + Chunk.SIZE * 0.5f;

        float dx = cx - camX;
        float dz = cz - camZ;
        float dist = (float) Math.sqrt(dx*dx + dz*dz);

        if (dist < MARGIN) return true;

        float dot = (dx * forwardX + dz * forwardZ) / dist;

        float chunkAngularSize = MARGIN / dist;
        float threshold = halfFovCos - chunkAngularSize;

        return dot >= threshold;
    }
}