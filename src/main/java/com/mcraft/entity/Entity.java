package com.mcraft.entity;

import com.mcraft.world.Block;
import com.mcraft.world.World;

public abstract class Entity {

    protected float x, y, z;
    protected float velX, velY, velZ;
    protected boolean onGround = false;

    protected final float width, height;

    protected static final float GRAVITY = -25f;

    protected Entity(float x, float y, float z, float width, float height) {
        this.x = x; this.y = y; this.z = z;
        this.width = width; this.height = height;
    }

    public void applyPhysics(float dt, World world) {
        if (!onGround) velY += GRAVITY * dt;
        velY = Math.max(velY, -50f);

        x += velX * dt;
        if (checkCollision(world)) { x -= velX * dt; velX = 0; }

        onGround = false;
        y += velY * dt;
        if (checkCollision(world)) {
            if (velY < 0) onGround = true;
            y -= velY * dt;
            velY = 0;
        }

        z += velZ * dt;
        if (checkCollision(world)) { z -= velZ * dt; velZ = 0; }
    }

    protected boolean checkCollision(World world) {
        float hw = width / 2f;
        int minX = (int)Math.floor(x - hw), maxX = (int)Math.floor(x + hw);
        int minY = (int)Math.floor(y),       maxY = (int)Math.floor(y + height);
        int minZ = (int)Math.floor(z - hw), maxZ = (int)Math.floor(z + hw);

        for (int bx = minX; bx <= maxX; bx++)
            for (int by = minY; by <= maxY; by++)
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (!world.getBlock(bx, by, bz).solid) continue;
                    boolean ox = (x-hw) < (bx+1) && (x+hw) > bx;
                    boolean oy = y      < (by+1) && (y+height) > by;
                    boolean oz = (z-hw) < (bz+1) && (z+hw) > bz;
                    if (ox && oy && oz) return true;
                }
        return false;
    }

    public abstract void update(float dt, World world,
                                 float playerX, float playerY, float playerZ);

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getWidth()  { return width; }
    public float getHeight() { return height; }
}