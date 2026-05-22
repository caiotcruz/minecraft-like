package com.mcraft.player;

import com.mcraft.render.Camera;
import com.mcraft.ui.Inventory;
import com.mcraft.world.Block;
import com.mcraft.world.World;

public class Player {

    public static final float WIDTH  = 0.6f;
    public static final float HEIGHT = 1.8f;

    private float x, y, z;
    private float velX, velY, velZ;
    private boolean onGround = false;

    private static final float GRAVITY    = -25.0f;
    private static final float JUMP_FORCE =   9.0f;
    private static final float MOVE_SPEED =   5.0f;
    private static final float EYE_HEIGHT =   1.62f;

    private final Camera    camera;
    private final World     world;
    private final Inventory inventory = new Inventory();

    public Player(float x, float y, float z, World world) {
        this.x = x; this.y = y; this.z = z;
        this.world  = world;
        this.camera = new Camera(x, y + EYE_HEIGHT, z);

        inventory.addItem(Block.GRASS.id,   64);
        inventory.addItem(Block.STONE.id,   64);
        inventory.addItem(Block.DIRT.id,    64);
        inventory.addItem(Block.WOOD_LOG.id, 32);
        inventory.addItem(Block.PLANKS.id,  32);
    }

    public void update(float dx, float dz, boolean jump, float dt) {
        float yaw  = camera.getYaw();   
        float cos  = (float) Math.cos(yaw);
        float sin  = (float) Math.sin(yaw);

        float movX = -dx * sin - dz * cos;
        float movZ = dx * cos - dz * sin;

        float len = (float) Math.sqrt(movX * movX + movZ * movZ);
        if (len > 1e-6f) { movX = movX / len * MOVE_SPEED; movZ = movZ / len * MOVE_SPEED; }

        velX = movX;
        velZ = movZ;

        if (!onGround) velY += GRAVITY * dt;
        velY = Math.max(velY, -50f); 

        if (jump && onGround) {
            velY = JUMP_FORCE;
            onGround = false;
        }

        moveAndCollide(dt);

        camera.setPosition(x, y + EYE_HEIGHT, z);
    }


    private void moveAndCollide(float dt) {
        x += velX * dt;
        if (checkCollision()) { x -= velX * dt; velX = 0; }

        boolean wasOnGround = onGround;
        onGround = false;
        y += velY * dt;
        if (checkCollision()) {
            if (velY < 0) onGround = true;
            y -= velY * dt;
            velY = 0;
        }

        z += velZ * dt;
        if (checkCollision()) { z -= velZ * dt; velZ = 0; }
    }

    private boolean checkCollision() {
        float hw = WIDTH / 2f;

        int minX = (int) Math.floor(x - hw);
        int maxX = (int) Math.floor(x + hw);
        int minY = (int) Math.floor(y);
        int maxY = (int) Math.floor(y + HEIGHT);
        int minZ = (int) Math.floor(z - hw);
        int maxZ = (int) Math.floor(z + hw);

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (!world.getBlock(bx, by, bz).solid) continue;

                    boolean ox = (x - hw) < (bx + 1) && (x + hw) > bx;
                    boolean oy = y        < (by + 1) && (y + HEIGHT) > by;
                    boolean oz = (z - hw) < (bz + 1) && (z + hw) > bz;

                    if (ox && oy && oz) return true;
                }
            }
        }
        return false;
    }

    public Camera    getCamera()    { return camera; }
    public Inventory getInventory() { return inventory; } 
    public float     getX()         { return x; }
    public float     getY()         { return y; }
    public float     getZ()         { return z; }
}