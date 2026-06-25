package com.mcraft.entity;

import com.mcraft.render.Shader;
import com.mcraft.world.Block;
import com.mcraft.world.Chunk;
import com.mcraft.world.World;
import com.mcraft.world.DayNightCycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MobManager {

    private final List<Mob> mobs = new ArrayList<>();
    private final MobRenderer renderer = new MobRenderer();
    private final Random rng = new Random();

    public static final int MAX_MOBS = 64;
    private static final float SPAWN_INTERVAL = 10f; 
    private float spawnTimer = 0;

    private final java.util.function.Consumer<int[][]> dropCallback;

    public MobManager(java.util.function.Consumer<int[][]> onDrop) {
        this.dropCallback = onDrop;
    }

    public void update(float dt, World world, float px, float py, float pz, DayNightCycle dayNight) {

        java.util.Iterator<Mob> it = mobs.iterator();

        while (it.hasNext()) {
            Mob m = it.next();

            m.update(dt, world, px, py, pz);
            m.updateLightCache(world);

            if (m.isDead()) {
                int[][] drops = m.getDrops();
                if (dropCallback != null && drops != null) {
                    dropCallback.accept(drops);
                }
                it.remove();
                continue;
            }

            float dx = m.getX() - px;
            float dz = m.getZ() - pz;

            if ((dx * dx + dz * dz) > 128f * 128f) {
                it.remove();
            }
        }

        spawnTimer -= dt;

        if (spawnTimer <= 0f && mobs.size() < MAX_MOBS) {
            trySpawn(world, px, py, pz, dayNight);
            spawnTimer = SPAWN_INTERVAL;
        }
    }

    private void trySpawn(World world, float px, float py, float pz, DayNightCycle dayNight) {
        float angle = rng.nextFloat() * (float)(Math.PI * 2);
        float dist  = 16f + rng.nextFloat() * 24f;
        int sx = (int)(px + Math.cos(angle) * dist);
        int sz = (int)(pz + Math.sin(angle) * dist);
        int  sy = world.getSurfaceY(sx, sz) + 1;

        Block atSpawn  = world.getBlock(sx, sy,   sz);
        Block headSpace = world.getBlock(sx, sy+1, sz);

        if (atSpawn.solid || atSpawn == Block.WATER)   return;
        if (headSpace == Block.WATER)                  return;

        while (sy > 1 && !world.getBlock(sx, sy, sz).solid) {
            sy--;
        }

        if (sy <= 0 || sy >= Chunk.HEIGHT - 3) return;
        if (!world.getBlock(sx, sy, sz).solid) return;
        if (world.getBlock(sx, sy + 1, sz).solid) return;
        if (world.getBlock(sx, sy + 2, sz).solid) return;

        int skyLvl   = world.getSkyLightAt(sx, sy + 1, sz);
        int blockLvl = world.getBlockLightAt(sx, sy + 1, sz);
        float ambient = dayNight.getAmbientLight();

        float skyContrib   = skyLvl * ambient;
        float blockContrib = blockLvl / 15.0f;
        
        float effectiveLight = Math.max(skyContrib, blockContrib) * 15f;

        boolean isDark = effectiveLight <= 6f;
        boolean isLit  = effectiveLight >= 9f;

        Mob.Type type;
        if (isDark) {
            type = rng.nextBoolean() ? Mob.Type.ZOMBIE : Mob.Type.CREEPER;
        } else if (isLit) {
            type = switch (rng.nextInt(3)) {
                case 0  -> Mob.Type.CHICKEN;
                case 1  -> Mob.Type.COW;
                default -> Mob.Type.SHEEP;
            };
        } else {
            return;
        }

        float dx = (sx + 0.5f) - px;
        float dz = (sz + 0.5f) - pz;
        if (dx * dx + dz * dz < 8 * 8) return;

        mobs.add(new Mob(type, sx + 0.5f, sy + 1, sz + 0.5f));
    }

    public void render(Shader mobShader, float[] proj, float[] view, float ambient, float[] fogColor) {
        renderer.renderAll(mobs, mobShader, proj, view, ambient, fogColor);
    }

    public List<Mob> getMobs() { return mobs; }

    public void setMobs(List<Mob> loaded) {
        mobs.clear();
        mobs.addAll(loaded);
    }
}