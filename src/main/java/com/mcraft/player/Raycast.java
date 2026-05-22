package com.mcraft.player;

import com.mcraft.world.Block;
import com.mcraft.world.World;

public class Raycast {

    public static class HitResult {
        public boolean hit;
        public int blockX, blockY, blockZ;  
        public int prevX,  prevY,  prevZ;   
    }

    /**
     * @param ox,oy,oz  
     * @param dx,dy,dz  
     * @param maxDist  
     */
    public static HitResult cast(float ox, float oy, float oz,
                                  float dx, float dy, float dz,
                                  float maxDist, World world) {
        HitResult result = new HitResult();

        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 1e-6f) return result;
        dx /= len; dy /= len; dz /= len;

        int bx = (int) Math.floor(ox);
        int by = (int) Math.floor(oy);
        int bz = (int) Math.floor(oz);

        int stepX = dx > 0 ? 1 : -1;
        int stepY = dy > 0 ? 1 : -1;
        int stepZ = dz > 0 ? 1 : -1;

        float tDx = Math.abs(1f / dx);
        float tDy = (Math.abs(dy) > 1e-6f) ? Math.abs(1f / dy) : Float.MAX_VALUE;
        float tDz = Math.abs(1f / dz);

        float tMx = ((dx > 0 ? (bx+1) : bx) - ox) / dx;
        float tMy = (Math.abs(dy) > 1e-6f) ? ((dy > 0 ? (by+1) : by) - oy) / dy : Float.MAX_VALUE;
        float tMz = ((dz > 0 ? (bz+1) : bz) - oz) / dz;

        int prevX = bx, prevY = by, prevZ = bz;
        float t = 0;

        while (t < maxDist) {
            if (tMx < tMy && tMx < tMz) {
                t = tMx; tMx += tDx;
                prevX = bx; prevY = by; prevZ = bz;
                bx += stepX;
            } else if (tMy < tMz) {
                t = tMy; tMy += tDy;
                prevX = bx; prevY = by; prevZ = bz;
                by += stepY;
            } else {
                t = tMz; tMz += tDz;
                prevX = bx; prevY = by; prevZ = bz;
                bz += stepZ;
            }

            Block block = world.getBlock(bx, by, bz);
            if (block.solid) {
                result.hit    = true;
                result.blockX = bx; result.blockY = by; result.blockZ = bz;
                result.prevX  = prevX; result.prevY = prevY; result.prevZ = prevZ;
                return result;
            }
        }

        return result; 
    }
}