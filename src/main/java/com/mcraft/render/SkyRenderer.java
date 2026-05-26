package com.mcraft.render;

import com.mcraft.world.DayNightCycle;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class SkyRenderer {

    private static final float SKY_DIST  = 380f;
    private static final float SUN_SIZE  = 22f;
    private static final float MOON_SIZE = 15f;
    private static final float STAR_SIZE = 1.8f;
    private static final int   STAR_COUNT  = 250;
    private static final int   CLOUD_COUNT = 22;

    private final float[][] starDir = new float[STAR_COUNT][3];
    private final float[]   cloudX  = new float[CLOUD_COUNT];
    private final float[]   cloudZ  = new float[CLOUD_COUNT];
    private final float[]   cloudW  = new float[CLOUD_COUNT];
    private final float[]   cloudD  = new float[CLOUD_COUNT];
    private float windOffX = 0f;

    private static final int MAX_QUADS   = 512;
    private static final int VERT_FLOATS = 7;

    private int vao, vbo, ebo;
    private final FloatBuffer vBuf =
            BufferUtils.createFloatBuffer(MAX_QUADS * 4 * VERT_FLOATS);
    private final IntBuffer iBuf =
            BufferUtils.createIntBuffer(MAX_QUADS * 6);
    private int quadCount;

    public SkyRenderer() {
        Random rng = new Random(77331L);

        for (int i = 0; i < STAR_COUNT; i++) {
            double theta = rng.nextDouble() * Math.PI * 2;
            double phi   = Math.toRadians(5 + rng.nextDouble() * 85);
            starDir[i][0] = (float)(Math.cos(phi) * Math.cos(theta));
            starDir[i][1] = (float)(Math.sin(phi));
            starDir[i][2] = (float)(Math.cos(phi) * Math.sin(theta));
        }

        for (int i = 0; i < CLOUD_COUNT; i++) {
            cloudX[i] = (rng.nextFloat() - 0.5f) * 600f;
            cloudZ[i] = (rng.nextFloat() - 0.5f) * 600f;
            cloudW[i] = 40f + rng.nextFloat() * 70f;
            cloudD[i] = 18f + rng.nextFloat() * 35f;
        }

        initGPU();
    }

    public void update(float dt) {
        windOffX += dt * 4f;
    }

    public void render(Camera camera, DayNightCycle dayNight,
                        Shader skyShader, float[] proj, float[] view) {

        float t       = dayNight.getTime();
        float ambient = dayNight.getAmbientLight();
        float cx = camera.getX(), cy = camera.getY(), cz = camera.getZ();
        float[] right = camera.getRight();
        float[] bUp   = { 0f, 1f, 0f }; 

        glDepthMask(false);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        skyShader.use();
        skyShader.setMatrix4("uProjection", proj);
        skyShader.setMatrix4("uView", view);

        beginBatch();

        float sunAngle = 2f * (float)Math.PI * (t - 0.25f);
        float sunX = cx + (float)Math.cos(sunAngle) * SKY_DIST;
        float sunY = cy + (float)Math.sin(sunAngle) * SKY_DIST;
        float sunZ = cz;

        float sunAlpha = horizonFade(sunY - cy);
        if (sunAlpha > 0.01f) {
            float elevation = (float)Math.sin(sunAngle);
            float warmth    = 1f - Math.max(0f, elevation);
            float sr = 1.0f;
            float sg = 0.92f - warmth * 0.45f;
            float sb = 0.40f - warmth * 0.40f;
            addBillboard(sunX, sunY, sunZ, SUN_SIZE,        right, bUp, sr, sg, sb, sunAlpha);
            addBillboard(sunX, sunY, sunZ, SUN_SIZE * 2.2f, right, bUp, sr, sg * 0.8f, 0.1f, sunAlpha * 0.25f);
        }

        float moonX = cx - (float)Math.cos(sunAngle) * SKY_DIST;
        float moonY = cy - (float)Math.sin(sunAngle) * SKY_DIST;
        float moonZ = cz;

        float moonAlpha = horizonFade(moonY - cy);
        if (moonAlpha > 0.01f) {
            addBillboard(moonX, moonY, moonZ, MOON_SIZE, right, bUp, 0.86f, 0.88f, 0.95f, moonAlpha);
        }

        float starAlpha = Math.max(0f, (0.45f - ambient) / 0.40f);
        if (starAlpha > 0.01f) {
            for (float[] dir : starDir) {
                float sx = cx + dir[0] * (SKY_DIST * 0.92f);
                float sy = cy + dir[1] * SKY_DIST;
                float sz = cz + dir[2] * (SKY_DIST * 0.92f);
                float sz2 = STAR_SIZE * (0.6f + 0.8f * Math.abs((dir[0] * 3.7f + dir[2] * 5.1f) % 1f));
                addBillboard(sx, sy, sz, sz2, right, bUp, 1f, 1f, 0.92f, starAlpha);
            }
        }

        float br = 0.5f + ambient * 0.5f;
        float cloudY = cy + 85f;

        for (int i = 0; i < CLOUD_COUNT; i++) {
            float wx = cx + ((cloudX[i] + windOffX) % 600f + 600f) % 600f - 300f;
            float wz = cz + cloudZ[i];
            float w  = cloudW[i], d = cloudD[i];
            addCloud(wx - w/2f, cloudY, wz - d/2f, wx + w/2f, cloudY + 5f, wz + d/2f, br);
        }

        flushBatch();

        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    private void addBillboard(float cx, float cy, float cz, float size,
                               float[] right, float[] up,
                               float r, float g, float b, float a) {
        float hs = size * 0.5f;
        float rx = right[0], ry = right[1], rz = right[2];
        float ux = up[0],    uy = up[1],    uz = up[2];
        addQuad(
            cx - rx*hs - ux*hs, cy - ry*hs - uy*hs, cz - rz*hs - uz*hs,
            cx + rx*hs - ux*hs, cy + ry*hs - uy*hs, cz + rz*hs - uz*hs,
            cx + rx*hs + ux*hs, cy + ry*hs + uy*hs, cz + rz*hs + uz*hs,
            cx - rx*hs + ux*hs, cy - ry*hs + uy*hs, cz - rz*hs + uz*hs,
            r, g, b, a
        );
    }

    private void addCloud(float x0, float y0, float z0,
                           float x1, float y1, float z1,
                           float br) {
        float a = 0.80f;
        addQuad(x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, br, br, br+0.02f, a);
        addQuad(x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1, br*.78f, br*.78f, br*.80f, a*.70f);
        addQuad(x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, br*.85f, br*.85f, br*.87f, a);
        addQuad(x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, br*.82f, br*.82f, br*.84f, a);
        addQuad(x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, br*.83f, br*.83f, br*.85f, a);
        addQuad(x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, br*.83f, br*.83f, br*.85f, a);
    }

    private float horizonFade(float heightAboveCam) {
        float t = (heightAboveCam + SKY_DIST * 0.12f) / (SKY_DIST * 0.28f);
        return Math.max(0f, Math.min(1f, t));
    }

    private void addQuad(float x0, float y0, float z0,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2,
                          float x3, float y3, float z3,
                          float r, float g, float b, float a) {
        if (quadCount >= MAX_QUADS) return;
        vBuf.put(x0); vBuf.put(y0); vBuf.put(z0); vBuf.put(r); vBuf.put(g); vBuf.put(b); vBuf.put(a);
        vBuf.put(x1); vBuf.put(y1); vBuf.put(z1); vBuf.put(r); vBuf.put(g); vBuf.put(b); vBuf.put(a);
        vBuf.put(x2); vBuf.put(y2); vBuf.put(z2); vBuf.put(r); vBuf.put(g); vBuf.put(b); vBuf.put(a);
        vBuf.put(x3); vBuf.put(y3); vBuf.put(z3); vBuf.put(r); vBuf.put(g); vBuf.put(b); vBuf.put(a);
        int base = quadCount * 4;
        iBuf.put(base); iBuf.put(base+1); iBuf.put(base+2);
        iBuf.put(base+2); iBuf.put(base+3); iBuf.put(base);
        quadCount++;
    }

    private void beginBatch() { vBuf.clear(); iBuf.clear(); quadCount = 0; }

    private void flushBatch() {
        if (quadCount == 0) return;
        vBuf.flip(); iBuf.flip();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vBuf);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, iBuf);
        glDrawElements(GL_TRIANGLES, quadCount * 6, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    private void initGPU() {
        vao = glGenVertexArrays(); vbo = glGenBuffers(); ebo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long)MAX_QUADS * 4 * VERT_FLOATS * Float.BYTES, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, (long)MAX_QUADS * 6 * Integer.BYTES, GL_DYNAMIC_DRAW);
        int stride = VERT_FLOATS * Float.BYTES; 
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 4, GL_FLOAT, false, stride, 12L); 
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    public void delete() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }
}