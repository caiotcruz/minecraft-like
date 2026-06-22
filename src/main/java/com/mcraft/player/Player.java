package com.mcraft.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.mcraft.render.Camera;
import com.mcraft.ui.Inventory;
import com.mcraft.world.Block;
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

    private boolean dead = false;

    private float peakY      = Float.NaN;
    private boolean wasInAir = false;

    private boolean sprinting = false;

    private static final float GRAVITY    = -25.0f;
    private static final float JUMP_FORCE =   9.0f;
    private static final float MOVE_SPEED =   5.0f;
    private static final float SPRINT_SPEED = 8.0f;
    private static final float EYE_HEIGHT =   1.62f;

    private static final float WATER_GRAVITY_MULT   = 0.20f; 
    private static final float WATER_TERMINAL_VEL   = -4.0f; 
    private static final float WATER_SWIM_UP_FORCE  = 4.5f;  
    private static final float WATER_DRAG           = 0.80f;
    private static final float WATER_SPEED_MULT     = 0.65f;

    private static final float MAX_AIR          = 300f;
    private static final float AIR_DEPLETION    = 20f;
    private static final float AIR_REFILL       = 60f;
    private static final float DROWN_INTERVAL   = 1.0f;
    private static final float DROWN_DAMAGE     = 2;

    private float   air            = MAX_AIR;
    private float   drownTimer     = 0f;
    private boolean inWater        = false;
    private boolean headInWater    = false;

    private static final float HUNGER_RATE_IDLE   = 1f / 60f;
    private static final float HUNGER_RATE_WALK   = 1f / 30f;
    private static final float HUNGER_RATE_SPRINT = 1f /  8f;

    private static final float STARVATION_DAMAGE_THRESHOLD = 0f;
    private static final float STARVATION_DAMAGE_INTERVAL  = 4f;

    private static final Map<Integer, Integer> FOOD_VALUES = new HashMap<>();
    static {
        FOOD_VALUES.put(Block.RAW_BEEF.id, 3);
        FOOD_VALUES.put(Block.COOKED_BEEF.id, 9);
        FOOD_VALUES.put(Block.ROTTEN_FLESH.id, 4);
    }

    private static final Set<Integer> POISON_FOODS = Set.of(
        Block.ROTTEN_FLESH.id
    );

    private float hunger              = 20f;
    private static final float MAX_HUNGER = 20f;
    private float hungerDepletionAccum = 0f;  
    private float starvationTimer      = 0f;

    private final Camera    camera;
    private final World     world;
    private final com.mcraft.ui.Inventory inventory;

    private float spawnPointX, spawnPointY, spawnPointZ;

    public Player(float x, float y, float z, World world) {
        this.x = x; this.y = y; this.z = z;
        this.spawnPointX = x; this.spawnPointY = y; this.spawnPointZ = z;
        this.world  = world;
        this.camera = new Camera(x, y + EYE_HEIGHT, z);

        this.inventory = new com.mcraft.ui.Inventory();
        inventory.addItem(com.mcraft.world.Block.GRASS.id, 64);
        inventory.addItem(com.mcraft.world.Block.STONE.id, 64);
        inventory.addItem(com.mcraft.world.Block.DIRT.id,  64);
        inventory.addItem(com.mcraft.world.Block.BED.id,  1);
        inventory.addItem(com.mcraft.world.Block.PLANKS.id, 64);
        inventory.addItem(com.mcraft.world.Block.COAL_ORE.id,  64);
        inventory.addItem(com.mcraft.world.Block.GOLD_ORE.id,  64);
        inventory.addItem(com.mcraft.world.Block.DIAMOND_ORE.id,  64);
        inventory.addItem(com.mcraft.world.Block.IRON_ORE.id,  64);
        inventory.addItem(com.mcraft.world.Block.TORCH.id,  64);
        inventory.addItem(com.mcraft.world.Block.COOKED_BEEF.id, 64);
    }

    public void update(float dx, float dz, boolean jump, boolean dive, float dt) {
        if (dead) return;
        
        if (invincibleTimer > 0) invincibleTimer -= dt;

        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        inWater     = world.getBlock(bx, (int) Math.floor(y + 0.30f), bz) == Block.WATER;
        headInWater = world.getBlock(bx, (int) Math.floor(y + EYE_HEIGHT - 0.1f), bz) == Block.WATER;
        
        float speed = sprinting ? SPRINT_SPEED : MOVE_SPEED;
        if (inWater){
            speed *= WATER_SPEED_MULT;
        }

        float yaw  = camera.getYaw();   
        float cos  = (float) Math.cos(yaw);
        float sin  = (float) Math.sin(yaw);

        float movX = -dx * sin - dz * cos;
        float movZ = dx * cos - dz * sin;

        float len = (float) Math.sqrt(movX * movX + movZ * movZ);
        if (len > 1e-6f) { 
            movX = movX/len * speed; 
            movZ = movZ/len * speed; 
        }

        velX = movX;
        velZ = movZ;



        float movH = (float) Math.sqrt(velX * velX + velZ * velZ);

        float hungerRate;

        if (sprinting && movH > 0.5f) {
            hungerRate = HUNGER_RATE_SPRINT;
        } else if (movH > 0.1f) {
            hungerRate = HUNGER_RATE_WALK;
        } else {
            hungerRate = HUNGER_RATE_IDLE;
        }

        hungerDepletionAccum += hungerRate * dt;

        if (hungerDepletionAccum >= 1f) {
            hunger = Math.max(0f, hunger - 1f);
            hungerDepletionAccum -= 1f;
        }

        if (hunger <= STARVATION_DAMAGE_THRESHOLD) {

            starvationTimer += dt;

            if (starvationTimer >= STARVATION_DAMAGE_INTERVAL) {
                takeDamage(1);
                starvationTimer = 0f;
            }

            sprinting = false;

        } else {
            starvationTimer = 0f;
        }

        if (hunger < 3f) {
            sprinting = false;
        }

        if (inWater) {
            velY += GRAVITY * WATER_GRAVITY_MULT * dt;
            velY  = Math.max(velY, WATER_TERMINAL_VEL);

            if (jump)  velY = WATER_SWIM_UP_FORCE;
            if (dive)  velY = -WATER_SWIM_UP_FORCE;

            velY *= (float) Math.pow(WATER_DRAG, dt * 10);

            peakY    = Float.NaN;
            wasInAir = false;
        }else if (!onGround) {
            velY += GRAVITY * dt;
            velY = Math.max(velY, -50f);

            if (Float.isNaN(peakY)) {
                peakY = y;
            } else {
                peakY = Math.max(peakY, y);
            }
            wasInAir = true;
        }

        if (jump && onGround && !inWater) {
            velY = JUMP_FORCE;
            onGround = false;
        }

        moveAndCollide(dt);

        if (onGround && wasInAir && !Float.isNaN(peakY) && !inWater) {
            float dropped = peakY - y;

            if (dropped > 3.0f) {
                int dmg = (int)((dropped - 3.0f) * 2.0f);
                if (dmg > 0) takeDamage(dmg);
            }

            peakY    = Float.NaN;
            wasInAir = false;
        }
        if (!onGround && !inWater) wasInAir = true;
        else if (onGround) wasInAir = false;

        if (headInWater) {
            air = Math.max(0f, air - AIR_DEPLETION * dt);
            if (air <= 0f) {
                drownTimer += dt;
                if (drownTimer >= DROWN_INTERVAL) {
                    takeDamage((int) DROWN_DAMAGE);
                    drownTimer = 0f;
                }
            }
        } else {
            air = Math.min(MAX_AIR, air + AIR_REFILL * dt);
            drownTimer = 0f;
        }

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

        int minX = (int) Math.floor(x - hw), maxX = (int) Math.floor(x + hw);
        int minY = (int) Math.floor(y),      maxY = (int) Math.floor(y + HEIGHT);
        int minZ = (int) Math.floor(z - hw), maxZ = (int) Math.floor(z + hw);

        if (minY >= Chunk.HEIGHT || maxY < 0) return false;
        minY = Math.max(0, minY);
        maxY = Math.min(Chunk.HEIGHT - 1, maxY);

        float pMinX = x - hw;
        float pMaxX = x + hw;
        float pMinY = y;
        float pMaxY = y + HEIGHT;
        float pMinZ = z - hw;
        float pMaxZ = z + hw;

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    Block block = world.getBlock(bx, by, bz);

                    if (block.solid) return true;

                    if (block == Block.DOOR_CLOSED) {
                        Block westN = world.getBlock(bx - 1, by, bz);
                        Block eastN = world.getBlock(bx + 1, by, bz);
                        boolean wallRunsAlongX = westN.solid || eastN.solid;

                        float doorX0, doorX1, doorZ0, doorZ1;
                        float doorY0 = by;
                        float doorY1 = by + 1.0f;
                        float doorThick = 0.1875f;

                        if (wallRunsAlongX) {
                            doorX0 = bx;        doorX1 = bx + 1.0f;
                            doorZ0 = bz;        doorZ1 = bz + doorThick;
                        } else {
                            doorX0 = bx;        doorX1 = bx + doorThick;
                            doorZ0 = bz;        doorZ1 = bz + 1.0f;
                        }

                        boolean intersectsX = (pMinX < doorX1 && pMaxX > doorX0);
                        boolean intersectsY = (pMinY < doorY1 && pMaxY > doorY0);
                        boolean intersectsZ = (pMinZ < doorZ1 && pMaxZ > doorZ0);

                        if (intersectsX && intersectsY && intersectsZ) {
                            return true;
                        }
                    }
                    if (block == Block.BED) {
                        float bedY0 = by;
                        float bedY1 = by + 0.5625f;

                        boolean intersectsX = (pMinX < (bx + 1.0f) && pMaxX > bx);
                        boolean intersectsY = (pMinY < bedY1         && pMaxY > bedY0);
                        boolean intersectsZ = (pMinZ < (bz + 1.0f) && pMaxZ > bz);

                        if (intersectsX && intersectsY && intersectsZ) {
                            return true;
                        }
                    }
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

    public boolean takeDamageWithArmour (int amount) {
        if (dead || invincibleTimer > 0) return false;

        float reduction = inventory.getTotalArmorReduction();
        int reducedDamage = Math.max(1, (int)(amount * (1.0f - reduction)));

        health -= reducedDamage;
        invincibleTimer = INVINCIBLE_TIME;

        if (reduction > 0) {
            inventory.damageArmor(1);
        }

        if (health <= 0) { health = 0; dead = true; }
        return true;
    }

    public void applyKnockback(float dirX, float dirZ, float force) {
        velX += dirX * force;
        velZ += dirZ * force;
        velY  = Math.max(velY, 3.0f);
        onGround = false; 
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
        this.x = spawnPointX; this.y = spawnPointY; this.z = spawnPointZ;
        velX = velY = velZ = 0;
        health = maxHealth;
        dead = false; wasInAir = false; peakY = Float.NaN;
        invincibleTimer = 1.0f; 
        camera.setPosition(x, y + EYE_HEIGHT, z);
    }

    public boolean eatFood(int blockId) {
        Integer value = FOOD_VALUES.get(blockId);
        if (value == null) return false;
        if (hunger >= MAX_HUNGER) return false; 

        hunger = Math.min(MAX_HUNGER, hunger + value);

        if (POISON_FOODS.contains(blockId)) {
            takeDamage(3);
        }
        return true;
    }

    public void setSpawnPoint(float x, float y, float z) {
        this.spawnPointX = x;
        this.spawnPointY = y;
        this.spawnPointZ = z;
    }

    public void setSprinting(boolean s) { 
        this.sprinting = s; 
    }

    public boolean isSprinting() { 
        return sprinting; 
    }

    public void setHunger(float hunger) {
        this.hunger = hunger;
    } 

    public void setHealth (int health) {
        this.health = health;
    }

    public void setInWater (boolean inWater) {
        this.inWater = inWater;
    }

    public void setHeadInWater(boolean headInWater) {
        this.headInWater = headInWater;
    }

    public void setAir (float air) {
        this.air = air;
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
    public float     getHunger()    { return hunger; }
    public float     getMaxHunger() { return MAX_HUNGER; }
    public boolean   isHungry()   { return hunger < MAX_HUNGER; }
    public static boolean isFoodItem(int blockId) { return FOOD_VALUES.containsKey(blockId); }
    public float   getAir()        { return air; }
    public float   getMaxAir()     { return MAX_AIR; }
    public boolean isInWater()     { return inWater; }
    public boolean isHeadInWater() { return headInWater; }
}