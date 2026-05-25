package com.mcraft.entity;

import com.mcraft.render.Shader;
import com.mcraft.world.Chunk;
import com.mcraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MobManager {

    private final List<Mob> mobs = new ArrayList<>();
    private final MobRenderer renderer = new MobRenderer();
    private final Random rng = new Random();

    private static final int MAX_MOBS = 64;
    private static final float SPAWN_INTERVAL = 10f; 
    private float spawnTimer = 0;

    public void update(float dt, World world, float px, float py, float pz, boolean isNight) {
        mobs.removeIf(m -> {
            m.update(dt, world, px, py, pz);
            float dx = m.getX()-px, dz = m.getZ()-pz;
            return (dx*dx + dz*dz) > 128*128;
        });

        spawnTimer -= dt;
        if (spawnTimer <= 0 && mobs.size() < MAX_MOBS) {
            trySpawn(world, px, py, pz, isNight);
            spawnTimer = SPAWN_INTERVAL;
        }
    }

    private void trySpawn(World world, float px, float py, float pz, boolean isNight) {
        float angle = rng.nextFloat() * (float)(Math.PI * 2);
        float dist  = 20 + rng.nextFloat() * 20;
        int sx = (int)(px + Math.cos(angle) * dist);
        int sz = (int)(pz + Math.sin(angle) * dist);

        int sy = Chunk.HEIGHT - 1;
        while (sy > 1 && !world.getBlock(sx, sy, sz).solid) sy--;

        if (sy <= 0 || sy >= Chunk.HEIGHT - 3) return;
        if (!world.getBlock(sx, sy, sz).solid) return;
        if (world.getBlock(sx, sy+1, sz).solid) return;

        Mob.Type type;

        if (isNight) {
            type = switch (rng.nextInt(4)) {
                case 0 -> Mob.Type.ZOMBIE;
                case 1 -> Mob.Type.CREEPER;
                case 2 -> Mob.Type.CHICKEN;
                default -> Mob.Type.COW;
            };
        } else {
            type = rng.nextBoolean()
                ? Mob.Type.CHICKEN
                : Mob.Type.COW;
        }

        mobs.add(new Mob(type, sx + 0.5f, sy + 1, sz + 0.5f));
    }

    public void render(Shader mobShader, float[] proj, float[] view, float ambient, float[] fogColor) {
        renderer.renderAll(mobs, mobShader, proj, view, ambient, fogColor);
    }

    public List<Mob> getMobs() { return mobs; }
}