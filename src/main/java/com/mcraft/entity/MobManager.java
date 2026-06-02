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

    private final java.util.function.Consumer<int[][]> dropCallback;

    public MobManager(java.util.function.Consumer<int[][]> onDrop) {
        this.dropCallback = onDrop;
    }

    public void update(float dt, World world, float px, float py, float pz, boolean isNight) {

        java.util.Iterator<Mob> it = mobs.iterator();

        while (it.hasNext()) {
            Mob m = it.next();

            m.update(dt, world, px, py, pz);

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
        if (world.getBlock(sx, sy + 1, sz).solid) return;

        Mob.Type type;

        if (isNight) {
            type = switch (rng.nextInt(5)) {
                case 0 -> Mob.Type.ZOMBIE;
                case 1 -> Mob.Type.CREEPER;
                case 2 -> Mob.Type.CHICKEN;
                case 3 -> Mob.Type.COW;
                default -> Mob.Type.SHEEP;
            };
        } else {
            type = switch (rng.nextInt(3)) {
                case 0 -> Mob.Type.CHICKEN;
                case 1 -> Mob.Type.COW;
                default -> Mob.Type.SHEEP;
            };
        }

        mobs.add(new Mob(type, sx + 0.5f, sy + 1, sz + 0.5f));
    }

    public void render(Shader mobShader, float[] proj, float[] view, float ambient, float[] fogColor) {
        renderer.renderAll(mobs, mobShader, proj, view, ambient, fogColor);
    }

    public List<Mob> getMobs() { return mobs; }
}