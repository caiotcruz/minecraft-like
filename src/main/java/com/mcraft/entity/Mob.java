package com.mcraft.entity;

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
    private float    stuckTimer  = 0;

    public Mob(Type type, float x, float y, float z) {
        super(x, y, z, type.w, type.h);
        this.type = type;
        pickNewWanderDir();
    }


    @Override
    public void update(float dt, World world, float playerX, float playerY, float playerZ) {
        float dx = playerX - x;
        float dz = playerZ - z;
        float distToPlayer = (float) Math.sqrt(dx*dx + dz*dz);

        state = (distToPlayer < type.senseRadius) ? AIState.SEEK : AIState.WANDER;

        switch (state) {
            case WANDER -> updateWander(dt);
            case SEEK   -> updateSeek(dx, dz, distToPlayer);
        }

        if (onGround && (Math.abs(velX) < 0.1f && Math.abs(velZ) < 0.1f)
                     && (wanderDirX != 0 || wanderDirZ != 0)) {
            stuckTimer += dt;
            if (stuckTimer > 0.4f) {
                velY = 7f; 
                stuckTimer = 0;
            }
        } else {
            stuckTimer = 0;
        }

        applyPhysics(dt, world);
    }

    private void updateWander(float dt) {
        wanderTimer -= dt;
        if (wanderTimer <= 0) {
            pickNewWanderDir();
            wanderTimer = 3f + rng.nextFloat() * 4f; 
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

    public Type   getType()  { return type; }
    public AIState getState() { return state; }

    public boolean isHostile() {
        return type.category == MobCategory.HOSTILE;
    }

    public boolean isPassive() {
        return type.category == MobCategory.PASSIVE;
    }
}