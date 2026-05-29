package com.mcraft.player;

import com.mcraft.render.Camera;
import com.mcraft.ui.Inventory;
import com.mcraft.world.Chunk;
import com.mcraft.world.World;

public class Player {

    public static final float WIDTH  = 0.6f;
    public static final float HEIGHT = 1.8f;

    private float x, y, z;
    private float velX, velY, velZ;
    private boolean onGround = false;

    private int   health    = 20;   
    private int   maxHealth = 20;
    private float invincibleTimer = 0f; 
    private static final float INVINCIBLE_TIME = 0.5f;
    private float regenTimer     = 0f;
    private static final float REGEN_INTERVAL  = 5.0f;

    private float fallStart = Float.NaN;
    private boolean wasFalling = false;

     private boolean dead = false;

    private static final float GRAVITY    = -25.0f;
    private static final float JUMP_FORCE =   9.0f;
    private static final float MOVE_SPEED =   5.0f;
    private static final float EYE_HEIGHT =   1.62f;

    private final Camera    camera;
    private final World     world;
    private final com.mcraft.ui.Inventory inventory;

    private final float spawnX, spawnY, spawnZ;

    public Player(float x, float y, float z, World world) {
        this.spawnX = x; this.spawnY = y; this.spawnZ = z;
        this.x = x; this.y = y; this.z = z;
        this.world  = world;
        this.camera = new Camera(x, y + EYE_HEIGHT, z);

        this.inventory = new com.mcraft.ui.Inventory();
        inventory.addItem(com.mcraft.world.Block.GRASS.id, 64);
        inventory.addItem(com.mcraft.world.Block.STONE.id, 64);
        inventory.addItem(com.mcraft.world.Block.DIRT.id,  64);
    }

    public void update(float dx, float dz, boolean jump, float dt) {
        if (dead) return;
        
        if (invincibleTimer > 0) invincibleTimer -= dt;

        float yaw  = camera.getYaw();   
        float cos  = (float) Math.cos(yaw);
        float sin  = (float) Math.sin(yaw);

        float movX = -dx * sin - dz * cos;
        float movZ = dx * cos - dz * sin;

        float len = (float) Math.sqrt(movX * movX + movZ * movZ);
        if (len > 1e-6f) { movX = movX / len * MOVE_SPEED; movZ = movZ / len * MOVE_SPEED; }

        velX = movX;
        velZ = movZ;

        if (!onGround) {
            velY += GRAVITY * dt;
            velY = Math.max(velY, -50f);

            if (!wasFalling) { fallStart = y; wasFalling = true; }
        }

        if (jump && onGround) {
            velY = JUMP_FORCE;
            onGround = false;
        }

        moveAndCollide(dt);

        if (onGround && wasFalling && !Float.isNaN(fallStart)) {
            float dropped = fallStart - y; 
            if (dropped > 3.0f) {
                int dmg = (int)((dropped - 3.0f) * 1.2f); 
                if (dmg > 0) takeDamage(dmg);
            }
            wasFalling = false; fallStart = Float.NaN;
        }
        if (!onGround) wasFalling = true;

        camera.setPosition(x, y + EYE_HEIGHT, z);
    }


    private void moveAndCollide(float dt) {
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

    private boolean checkCollision(World world) {
        float hw = WIDTH / 2f;

        int minX = (int) Math.floor(x - hw),  maxX = (int) Math.floor(x + hw);
        int minY = (int) Math.floor(y),        maxY = (int) Math.floor(y + HEIGHT);
        int minZ = (int) Math.floor(z - hw),  maxZ = (int) Math.floor(z + hw);

        if (minY >= Chunk.HEIGHT || maxY < 0) return false;
        minY = Math.max(0, minY);
        maxY = Math.min(Chunk.HEIGHT - 1, maxY);

        if (minX == maxX && minY == maxY && minZ == maxZ) {
            return world.getBlock(minX, minY, minZ).solid;
        }

        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                if (world.getBlock(bx, minY, bz).solid) return true;
            }
        }

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY + 1; by <= maxY; by++) { 
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (world.getBlock(bx, by, bz).solid) return true;
                }
            }
        }
        return false;
    }

    public boolean takeDamage(int amount) {
        if (dead || invincibleTimer > 0) return false;
        health -= amount;
        invincibleTimer = INVINCIBLE_TIME;
        if (health <= 0) { health = 0; dead = true; }
        return true;
    }

    public void tickRegen(float dt) {
        if (dead || health >= maxHealth) { regenTimer = 0f; return; }

        if (invincibleTimer > 0) { regenTimer = 0f; return; }

        regenTimer += dt;
        if (regenTimer >= REGEN_INTERVAL) {
            regenTimer = 0f;
            heal(1);
        }
    }

    public void heal(int amount) {
        if (dead) return;
        health = Math.min(maxHealth, health + amount);
    }

    public void respawn() {
        x = spawnX; y = spawnY; z = spawnZ;
        velX = velY = velZ = 0;
        health = maxHealth;
        dead = false; wasFalling = false; fallStart = Float.NaN;
        invincibleTimer = 1.0f; 
        camera.setPosition(x, y + EYE_HEIGHT, z);
    }

    public Camera    getCamera()    { return camera; }
    public com.mcraft.ui.Inventory getInventory() { return inventory; }
    public float     getX()         { return x; }
    public float     getY()         { return y; }
    public float     getZ()         { return z; }
    public boolean   isGrounded()   {return onGround;}
    public Inventory geInventory()  {return inventory;}
    public boolean   isDead()       { return dead; }
    public int       getHealth()    { return health; }
    public int       getMaxHealth() { return maxHealth; }
}