package com.mcraft.world;

import com.mcraft.render.Camera;
import com.mcraft.render.Shader;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class WeatherSystem {

    private static final int MAX_PARTICLES = 800;
    private static final float SPAWN_RADIUS = 20f;
    private static final float RAIN_SPEED   = 28f;
    private static final float SNOW_SPEED   = 4f;

    private WeatherType current = WeatherType.CLEAR;
    private float changeTimer   = 60f;
    private float intensity     = 0f;

    private final float[] px  = new float[MAX_PARTICLES];
    private final float[] py  = new float[MAX_PARTICLES];
    private final float[] pz  = new float[MAX_PARTICLES];
    private final Random  rng = new Random();

    private static final int MAX_QUADS = MAX_PARTICLES;
    private int vao, vbo, ebo;
    private final FloatBuffer vBuf =
        BufferUtils.createFloatBuffer(MAX_QUADS * 4 * 7);
    private final IntBuffer   iBuf =
        BufferUtils.createIntBuffer(MAX_QUADS * 6);

    public WeatherSystem() {
        initGPU();
        for (int i = 0; i < MAX_PARTICLES; i++) respawn(i, 0, 0, 0, true);
    }


    public void update(float dt, float cx, float cy, float cz, Biome biome) {
        changeTimer -= dt;
        if (changeTimer <= 0f) {

            changeTimer = 180f + rng.nextFloat() * 240f;

            WeatherType target =
            rng.nextFloat() < 0.75f
                ? WeatherType.CLEAR
                : WeatherType.forBiome(biome);

            current = target;
        }

        float targetIntensity =
        (current != WeatherType.CLEAR)
            ? 1f
            : 0f;
        intensity += (targetIntensity - intensity) * dt * 0.5f;

        if (intensity < 0.01f) return;

        float speed = (current == WeatherType.SNOW) ? SNOW_SPEED : RAIN_SPEED;
        float driftX = (current == WeatherType.SNOW) ? (float)Math.sin(System.currentTimeMillis()*0.0003) * 1.5f : 0;

        for (int i = 0; i < MAX_PARTICLES; i++) {
            py[i] -= speed * dt;
            px[i] += driftX * dt;

            if (py[i] < cy - 10) respawn(i, cx, cy, cz, false);
        }
    }

    private void respawn(int i, float cx, float cy, float cz, boolean randomHeight) {
        px[i] = cx + (rng.nextFloat() - 0.5f) * SPAWN_RADIUS * 2;
        py[i] = randomHeight ? (cy + rng.nextFloat() * 20 - 5) : (cy + 18);
        pz[i] = cz + (rng.nextFloat() - 0.5f) * SPAWN_RADIUS * 2;
    }


    public void render(Camera camera, Shader skyShader, float[] proj, float[] view) {
        if (intensity < 0.01f) return;

        float[] right = camera.getRight();

        float r, g, b, a;
        float pw, ph;

        if (current == WeatherType.RAIN) {

            r = 0.55f;
            g = 0.70f;
            b = 1.00f;
            a = 0.72f * intensity;

            pw = 0.06f;
            ph = 0.55f;

        } else {

            r = 0.92f;
            g = 0.94f;
            b = 1.00f;
            a = 0.70f * intensity;

            pw = 0.12f;
            ph = 0.12f;
        }

        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        skyShader.use();
        skyShader.setMatrix4("uProjection", proj);
        skyShader.setMatrix4("uView", view);

        vBuf.clear();
        iBuf.clear();

        int qc = 0;

        float[] up = {0f, 1f, 0f};

        int particleCount = (int)(MAX_PARTICLES * intensity);

        for (int i = 0; i < particleCount; i++) {

            if (qc >= MAX_QUADS) break;

            float hs = pw * 0.5f;
            float ht = ph * 0.5f;

            float rux = right[0] * hs;
            float ruy = right[1] * hs;
            float ruz = right[2] * hs;

            float uux = up[0] * ht;
            float uuy = up[1] * ht;
            float uuz = up[2] * ht;

            vBuf.put(px[i] - rux - uux).put(py[i] - ruy - uuy).put(pz[i] - ruz - uuz).put(r).put(g).put(b).put(a);
            vBuf.put(px[i] + rux - uux).put(py[i] + ruy - ruz).put(pz[i] + ruz - uuz).put(r).put(g).put(b).put(a);
            vBuf.put(px[i] + rux + uux).put(py[i] + ruy + uuy).put(pz[i] + ruz + uuz).put(r).put(g).put(b).put(a);
            vBuf.put(px[i] - rux + uux).put(py[i] - ruy + uuy).put(pz[i] - ruz + uuz).put(r).put(g).put(b).put(a);

            int base = qc * 4;

            iBuf.put(base);
            iBuf.put(base + 1);
            iBuf.put(base + 2);

            iBuf.put(base + 2);
            iBuf.put(base + 3);
            iBuf.put(base);

            qc++;
        }

        if (qc > 0) {
            vBuf.flip();
            iBuf.flip();

            glBindVertexArray(vao);

            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vBuf);

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, iBuf);

            glDrawElements(GL_TRIANGLES, qc * 6, GL_UNSIGNED_INT, 0L);

            glBindVertexArray(0);
        }

        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public WeatherType getCurrent() { return current; }
    public void setCurrent(WeatherType newWeather){
        this.current = newWeather;
    }

    public void setIntensity(float intensity){
        this.intensity = intensity;
    }
    public float getIntensity()     { return intensity; }

    private void initGPU() {
        vao = glGenVertexArrays(); vbo = glGenBuffers(); ebo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long)MAX_QUADS*4*7*Float.BYTES, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, (long)MAX_QUADS*6*Integer.BYTES, GL_DYNAMIC_DRAW);
        int stride = 7 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);  glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 4, GL_FLOAT, false, stride, 12L); glEnableVertexAttribArray(1);
        glBindVertexArray(0);
    }
}