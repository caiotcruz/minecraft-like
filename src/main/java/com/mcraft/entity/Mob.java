package com.mcraft.entity;

import com.mcraft.world.Block;
import com.mcraft.world.World;
import java.util.Random;

public class Mob extends Entity {

    public enum Type {
        CHICKEN    ( "Galinha", MobCategory.PASSIVE, 0.4f, 0.7f, 2.5f, 8f, new float[]{1f, 1f, 1f, 1f}),
        COW        ( "Vaca", MobCategory.PASSIVE, 0.9f, 1.4f, 2.0f, 10f, new float[]{0.3f, 0.2f, 0.1f, 1f}),
        ZOMBIE     ( "Zumbi", MobCategory.HOSTILE, 0.6f, 1.8f, 2.2f, 16f, new float[]{0.2f, 0.5f, 0.2f, 1f}),
        CREEPER    ( "Creeper", MobCategory.HOSTILE, 0.6f, 1.7f, 2.0f, 12f, new float[]{0.2f, 0.8f, 0.2f, 1f});

        public final String name;
        public final float  w, h, speed, senseRadius;
        public final float[] color;
        public final MobCategory category;

        Type(String n, MobCategory category, float w, float h, float s, float r, float[] c) {
            this.category = category;
            this.name = n; this.w = w; this.h = h;
            this.speed = s; this.senseRadius = r; this.color = c;
        }
    }

    private final Type    type;
    private final Random  rng = new Random();

    private enum AIState { WANDER, SEEK }
    private AIState  state       = AIState.WANDER;
    private float    wanderTimer = 0;
    private float    wanderDirX  = 0;
    private float    wanderDirZ  = 0;

    private int     health;
    private boolean dead   = false;

    public Mob(Type type, float x, float y, float z) {
        super(x, y, z, type.w, type.h);
        this.type = type;
        this.health = switch (type) {
            case CHICKEN -> 4;
            case COW     -> 10;
            case ZOMBIE  -> 20;
            case CREEPER -> 20;
        };
        pickNewWanderDir();
    }


    public void update(float dt, World world,
                        float playerX, float playerY, float playerZ) {
        float dx = playerX - x;
        float dz = playerZ - z;
        float distToPlayer = (float) Math.sqrt(dx*dx + dz*dz);

        state = (distToPlayer < type.senseRadius) ? AIState.SEEK : AIState.WANDER;

        switch (state) {
            case WANDER -> updateWander(dt);
            case SEEK   -> updateSeek(dx, dz, distToPlayer);
        }

        if (onGround && isMoving()) {
            if (hasBlockAhead(world) && hasClearanceAbove(world) &&hasGroundAhead(world)) {
                velY = 7.5f; 
            }
        }

        applyPhysics(dt, world);
    }

    private void updateWander(float dt) {
        wanderTimer -= dt;
        if (wanderTimer <= 0) {
            pickNewWanderDir();
            wanderTimer = (wanderDirX == 0 && wanderDirZ == 0)
                ? 1f + rng.nextFloat() * 2f
                : 2f + rng.nextFloat() * 3f;
        }

        velX = wanderDirX * type.speed * 0.5f;
        velZ = wanderDirZ * type.speed * 0.5f;
    }

    private void updateSeek(float dx, float dz, float dist) {
        if (dist < 0.5f) { velX = velZ = 0; return; }
        float nx = dx / dist;
        float nz = dz / dist;
        velX = nx * type.speed;
        velZ = nz * type.speed;
    }

    private void pickNewWanderDir() {
        if (rng.nextFloat() < 0.25f) {
            wanderDirX = wanderDirZ = 0;
        } else {
            float angle = rng.nextFloat() * (float)(Math.PI * 2);
            wanderDirX = (float) Math.cos(angle);
            wanderDirZ = (float) Math.sin(angle);
        }
    }

    public boolean isHostile() {
        return type.category == MobCategory.HOSTILE;
    }

    public boolean isPassive() {
        return type.category == MobCategory.PASSIVE;
    }

    private boolean hasGroundAhead(World world) {
        float speed = (float)Math.sqrt(velX * velX + velZ * velZ);
        if (speed < 0.5f) return false;

        float nx = velX / speed;
        float nz = velZ / speed;

        float dist = width / 2f + 0.25f;

        int bx = (int)Math.floor(x + nx * dist);
        int bz = (int)Math.floor(z + nz * dist);

        int by = (int)Math.floor(y - 0.2f);

        return world.getBlock(bx, by, bz).solid;
    }

    private boolean hasBlockAhead(World world) {
        float speed = (float) Math.sqrt(velX * velX + velZ * velZ);
        if (speed < 0.5f) return false;

        float nx = velX / speed;
        float nz = velZ / speed;
        float hw = width / 2f + 0.05f; 

        int checkX = (int) Math.floor(x + nx * hw);
        int checkZ = (int) Math.floor(z + nz * hw);
        int checkY = (int) Math.floor(y + 0.1f); 

        return world.getBlock(checkX, checkY,     checkZ).solid
            || world.getBlock(checkX, checkY + 1, checkZ).solid;
    }

    private boolean hasClearanceAbove(World world) {
        int topY = (int) Math.floor(y + height + 0.1f);
        float hw = width / 2f - 0.05f;
        float[] xs = { x - hw, x + hw };
        float[] zs = { z - hw, z + hw};

        for (float cx : xs) {
            for (float cz : zs) {

                int bx = (int)Math.floor(cx);
                int bz = (int)Math.floor(cz);

                if (world.getBlock(bx, topY, bz).solid ||
                    world.getBlock(bx, topY + 1, bz).solid) {
                    return false;
                }
            }
        }

        return true;
    }

    public int[][] getDrops() {
        java.util.Random rng = new java.util.Random();
        return switch (type) {
            case CHICKEN -> new int[][]{
                { Block.FEATHER.id,  1 + rng.nextInt(2) }   
            };
            case COW -> new int[][]{
                { Block.LEATHER.id,  1 + rng.nextInt(3) }, 
                { Block.RAW_BEEF.id, 1 + rng.nextInt(3) }   
            };
            case ZOMBIE -> new int[][]{
                { Block.ROTTEN_FLESH.id, rng.nextInt(3) }   
            };
            case CREEPER -> new int[][]{
                { Block.GUNPOWDER.id, rng.nextInt(2) }  
            };
        };
    }

        public void damage(int amount) {
        if (dead) return;
        health -= amount;
        if (health <= 0) dead = true;
    }


    private boolean isMoving() {
        return (float) Math.sqrt(velX*velX + velZ*velZ) > 0.4f;
    }

    public boolean isDead() { return dead; }
    
    public Type   getType()  { return type; }
    public AIState getState() { return state; }

}