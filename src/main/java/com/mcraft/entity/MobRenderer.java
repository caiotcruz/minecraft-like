package com.mcraft.entity;

import com.mcraft.render.Shader;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class MobRenderer {

    private static final int VERT_FLOATS = 7; 
    private static final int MAX_VERTS   = 8_000;

    private static final float L_TOP   = 1.00f;
    private static final float L_FRONT = 0.80f;
    private static final float L_BACK  = 0.70f;
    private static final float L_SIDE  = 0.65f;
    private static final float L_BOT   = 0.50f;

    private int vao, vbo, ebo;
    private final FloatBuffer vBuf = BufferUtils.createFloatBuffer(MAX_VERTS * VERT_FLOATS);
    private final IntBuffer   iBuf = BufferUtils.createIntBuffer(MAX_VERTS * 3 / 2);
    private int vCount, indexCount;

    public MobRenderer() { initGPU(); }

    public void renderAll(List<Mob> mobs, Shader shader, float[] proj, float[] view, float ambient, float[] fogColor) {
        if (mobs.isEmpty()) return;

        shader.use();
        shader.setMatrix4("uProjection", proj);
        shader.setMatrix4("uView",       view);
        shader.setFloat  ("uAmbientLight", ambient);
        shader.setVec3   ("uFogColor", fogColor[0], fogColor[1], fogColor[2]);

        vBuf.clear(); iBuf.clear(); vCount = 0; indexCount = 0;

        for (Mob mob : mobs) renderMob(mob);

        if (indexCount == 0) return;

        vBuf.flip(); iBuf.flip();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vBuf);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, iBuf);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    private void renderMob(Mob mob) {
        float x = mob.getX(), y = mob.getY(), z = mob.getZ();
        float h = mob.isHurt()
            ? mob.getHurtTimer() / mob.getHurtDuration()
            : 0f;

        switch (mob.getType()) {
            case CHICKEN -> renderChicken(x, y, z, h);
            case COW     -> renderCow    (x, y, z, h);
            case ZOMBIE  -> renderZombie (x, y, z, h);
            case CREEPER -> renderCreeper(x, y, z, h);
        }
    }

    private void renderChicken(float x, float y, float z, float h) {
        float[] body = hurt(0.95f, 0.95f, 0.95f, h);  // branco → vermelho
        float[] leg  = hurt(1.0f,  0.62f, 0.12f, h);  // laranja → vermelho

        box(x-.175f, y+.15f, z-.125f, x+.175f, y+.45f, z+.125f, body[0],body[1],body[2]);
        box(x-.110f, y+.45f, z-.130f, x+.110f, y+.67f, z+.090f, body[0],body[1],body[2]);
        box(x-.038f, y+.53f, z-.190f, x+.038f, y+.58f, z-.130f, leg[0],leg[1],leg[2]);
        box(x-.080f, y,      z-.040f, x-.040f, y+.18f, z+.040f, leg[0],leg[1],leg[2]);
        box(x+.040f, y,      z-.040f, x+.080f, y+.18f, z+.040f, leg[0],leg[1],leg[2]);
    }

    private void renderCow(float x, float y, float z, float h) {
        float[] body  = hurt(.45f, .28f, .10f, h);
        float[] white = hurt(.92f, .92f, .92f, h);
        float[] snout = hurt(.35f, .20f, .07f, h);
        float[] udder = hurt(.78f, .62f, .42f, h);
        float[] teats = hurt(.28f, .10f, .10f, h);
        float[] legs  = hurt(.30f, .18f, .05f, h);

        box(x-.45f, y+.50f, z-.22f,  x+.45f, y+1.00f, z+.22f,  body[0],  body[1],  body[2]);
        box(x-.20f, y+.99f, z-.10f,  x+.10f, y+1.01f, z+.15f,  white[0], white[1], white[2]); 
        box(x-.38f, y+.99f, z-.05f,  x-.15f, y+1.01f, z+.18f,  white[0], white[1], white[2]);
        box(x-.22f, y+.40f, z-.38f,  x+.22f, y+.75f,  z-.22f,  snout[0], snout[1], snout[2]);
        box(x-.12f, y+.42f, z-.44f,  x+.12f, y+.60f,  z-.37f,  udder[0], udder[1], udder[2]); 
        box(x-.08f, y+.46f, z-.445f, x-.03f, y+.52f,  z-.435f, teats[0], teats[1], teats[2]);
        box(x+.03f, y+.46f, z-.445f, x+.08f, y+.52f,  z-.435f, teats[0], teats[1], teats[2]);
        
        box(x-.38f, y, z-.18f, x-.22f, y+.55f, z-.04f, legs[0], legs[1], legs[2]);
        box(x+.22f, y, z-.18f, x+.38f, y+.55f, z-.04f, legs[0], legs[1], legs[2]); 
        box(x-.38f, y, z+.04f, x-.22f, y+.55f, z+.18f, legs[0], legs[1], legs[2]); 
        box(x+.22f, y, z+.04f, x+.38f, y+.55f, z+.18f, legs[0], legs[1], legs[2]);
    }

    private void renderZombie(float x, float y, float z, float h) {
        float[] shirt = hurt(.20f, .38f, .55f, h);
        float[] skin  = hurt(.40f, .55f, .35f, h);
        float[] eyes  = hurt(.55f, .05f, .05f, h);
        float[] pants = hurt(.12f, .15f, .30f, h);
        float[] shoes = hurt(.18f, .18f, .18f, h);

        box(x-.30f, y+.70f,  z-.15f,  x+.30f, y+1.35f, z+.15f,  shirt[0], shirt[1], shirt[2]); 
        box(x-.25f, y+1.35f, z-.25f,  x+.25f, y+1.85f, z+.25f,  skin[0],  skin[1],  skin[2]);
        box(x-.15f, y+1.62f, z-.26f,  x-.05f, y+1.72f, z-.24f,  eyes[0],  eyes[1],  eyes[2]); 
        box(x+.05f, y+1.62f, z-.26f,  x+.15f, y+1.72f, z-.24f,  eyes[0],  eyes[1],  eyes[2]); 
        box(x-.25f, y+.25f,  z-.13f,  x-.05f, y+.75f,  z+.13f,  pants[0], pants[1], pants[2]); 
        box(x+.05f, y+.25f,  z-.13f,  x+.25f, y+.75f,  z+.13f,  pants[0], pants[1], pants[2]);
        box(x-.25f, y,       z-.14f,  x-.05f, y+.25f,  z+.14f,  shoes[0], shoes[1], shoes[2]); 
        box(x+.05f, y,       z-.14f,  x+.25f, y+.25f,  z+.14f,  shoes[0], shoes[1], shoes[2]); 
        box(x-.45f, y+.85f,  z-.38f,  x-.30f, y+1.30f, z+.05f,  skin[0],  skin[1],  skin[2]); 
        box(x+.30f, y+.85f,  z-.38f,  x+.45f, y+1.30f, z+.05f,  skin[0],  skin[1],  skin[2]); 
    }

    private void renderCreeper(float x, float y, float z, float h) {
        float[] body = hurt(.16f, .70f, .16f, h);
        float[] head = hurt(.20f, .76f, .20f, h);
        float[] face = hurt(.02f, .08f, .02f, h);
        float[] lip  = hurt(.02f, .06f, .02f, h);
        float[] legs = hurt(.12f, .58f, .12f, h);

        box(x-.30f, y+.55f,  z-.20f,  x+.30f, y+1.20f, z+.20f,  body[0], body[1], body[2]); 
        box(x-.30f, y+1.20f, z-.30f,  x+.30f, y+1.78f, z+.30f,  head[0], head[1], head[2]); 
        box(x-.20f, y+1.52f, z-.31f,  x-.06f, y+1.66f, z-.28f,  face[0], face[1], face[2]);
        box(x+.06f, y+1.52f, z-.31f,  x+.20f, y+1.66f, z-.28f,  face[0], face[1], face[2]);
        box(x-.16f, y+1.30f, z-.31f,  x-.06f, y+1.44f, z-.28f,  lip[0],  lip[1],  lip[2]);
        box(x+.06f, y+1.30f, z-.31f,  x+.16f, y+1.44f, z-.28f,  lip[0],  lip[1],  lip[2]);
        box(x-.16f, y+1.34f, z-.31f,  x+.16f, y+1.44f, z-.28f,  lip[0],  lip[1],  lip[2]);
        
        box(x-.25f, y, z-.15f, x-.05f, y+.55f, z-.01f, legs[0], legs[1], legs[2]);
        box(x+.05f, y, z-.15f, x+.25f, y+.55f, z-.01f, legs[0], legs[1], legs[2]);
        box(x-.25f, y, z+.01f, x-.05f, y+.55f, z+.15f, legs[0], legs[1], legs[2]);
        box(x+.05f, y, z+.01f, x+.25f, y+.55f, z+.15f, legs[0], legs[1], legs[2]);
    }

    private void box(float x0, float y0, float z0,
                     float x1, float y1, float z1,
                     float r,  float g,  float b) {
        face(x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, r,g,b, L_TOP);   
        face(x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1, r,g,b, L_BOT);   
        face(x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, r,g,b, L_FRONT); 
        face(x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, r,g,b, L_BACK);  
        face(x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, r,g,b, L_SIDE);  
        face(x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, r,g,b, L_SIDE); 
    }

    private void face(float x0, float y0, float z0,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float x3, float y3, float z3,
                      float r, float g, float b, float light) {
        if (vCount + 4 > MAX_VERTS) return;
        vBuf.put(x0); vBuf.put(y0); vBuf.put(z0); vBuf.put(light); vBuf.put(r); vBuf.put(g); vBuf.put(b);
        vBuf.put(x1); vBuf.put(y1); vBuf.put(z1); vBuf.put(light); vBuf.put(r); vBuf.put(g); vBuf.put(b);
        vBuf.put(x2); vBuf.put(y2); vBuf.put(z2); vBuf.put(light); vBuf.put(r); vBuf.put(g); vBuf.put(b);
        vBuf.put(x3); vBuf.put(y3); vBuf.put(z3); vBuf.put(light); vBuf.put(r); vBuf.put(g); vBuf.put(b);
        int base = vCount;
        iBuf.put(base); iBuf.put(base+1); iBuf.put(base+2);
        iBuf.put(base+2); iBuf.put(base+3); iBuf.put(base);
        vCount += 4; indexCount += 6;
    }

    private static float[] hurt(float r, float g, float b, float hurt) {
        return new float[]{
            r + (1.0f - r) * hurt * 0.90f,   
            g * (1.0f - hurt * 0.80f),        
            b * (1.0f - hurt * 0.80f)         
        };
    }

    private void initGPU() {
        vao = glGenVertexArrays(); vbo = glGenBuffers(); ebo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long)MAX_VERTS * VERT_FLOATS * Float.BYTES, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, (long)MAX_VERTS * 3 / 2 * Integer.BYTES, GL_DYNAMIC_DRAW);
        int stride = VERT_FLOATS * Float.BYTES; 
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);  
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 1, GL_FLOAT, false, stride, 12L); 
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 16L); 
        glEnableVertexAttribArray(2);
        glBindVertexArray(0);
    }

    public void delete() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }
}