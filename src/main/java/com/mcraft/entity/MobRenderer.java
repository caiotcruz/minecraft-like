package com.mcraft.entity;

import com.mcraft.render.Shader;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class MobRenderer {

    private int vao, vbo, ebo;
    private static final int MAX_VERTS = 4096;

    public MobRenderer() { initGPU(); }

    public void renderAll(List<Mob> mobs, Shader shader,
                           float camX, float camY, float camZ) {
        if (mobs.isEmpty()) return;

        FloatBuffer vBuf = BufferUtils.createFloatBuffer(MAX_VERTS * 6);
        IntBuffer   iBuf = BufferUtils.createIntBuffer(MAX_VERTS * 2);
        int quadCount = 0;

        for (Mob mob : mobs) {
            float mx = mob.getX(), my = mob.getY(), mz = mob.getZ();
            float w  = mob.getWidth() / 2f;
            float h  = mob.getHeight();
            float[] c = mob.getType().color;

            quadCount = addBox(vBuf, iBuf, mx, my, mz, w, h, c, quadCount);
        }

        vBuf.flip(); iBuf.flip();

        float[] identity = {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
        shader.setMatrix4("uModel", identity);

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vBuf);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, iBuf);
        glDrawElements(GL_TRIANGLES, quadCount * 6, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }
    private int addBox(FloatBuffer v, IntBuffer idx,
                        float cx, float cy, float cz,
                        float hw, float h, float[] color,
                        int qc) {
        qc = addFace(v, idx, qc, color[0],
            cx-hw, cy,   cz+hw,
            cx+hw, cy,   cz+hw,
            cx+hw, cy+h, cz+hw,
            cx-hw, cy+h, cz+hw);
        qc = addFace(v, idx, qc, color[1],
            cx+hw, cy,   cz-hw,
            cx-hw, cy,   cz-hw,
            cx-hw, cy+h, cz-hw,
            cx+hw, cy+h, cz-hw);
        qc = addFace(v, idx, qc, color[2],
            cx-hw, cy,   cz-hw,
            cx-hw, cy,   cz+hw,
            cx-hw, cy+h, cz+hw,
            cx-hw, cy+h, cz-hw);
        qc = addFace(v, idx, qc, color[0],
            cx+hw, cy,   cz+hw,
            cx+hw, cy,   cz-hw,
            cx+hw, cy+h, cz-hw,
            cx+hw, cy+h, cz+hw);
        qc = addFace(v, idx, qc, 1.0f,
            cx-hw, cy+h, cz-hw,
            cx-hw, cy+h, cz+hw,
            cx+hw, cy+h, cz+hw,
            cx+hw, cy+h, cz-hw);
        qc = addFace(v, idx, qc, 0.5f,
            cx-hw, cy, cz,
            cx+hw, cy, cz,
            cx+hw, cy, cz,
            cx-hw, cy, cz);
        return qc;
    }

    private int addFace(FloatBuffer v, IntBuffer idx, int qc, float light,
                         float x0, float y0, float z0,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3) {
        float[][] verts = {{x0,y0,z0},{x1,y1,z1},{x2,y2,z2},{x3,y3,z3}};
        float[][] uvs   = {{0,1},{1,1},{1,0},{0,0}};
        for (int i = 0; i < 4; i++) {
            v.put(verts[i]); v.put(uvs[i]); v.put(light);
        }
        int base = qc * 4;
        idx.put(new int[]{ base, base+1, base+2, base+2, base+3, base });
        return qc + 1;
    }

    private void initGPU() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long)MAX_VERTS * 6 * Float.BYTES, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, (long)MAX_VERTS * 2 * Integer.BYTES, GL_DYNAMIC_DRAW);
        int stride = 6 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 12L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 20L);
        glEnableVertexAttribArray(2);
        glBindVertexArray(0);
    }
}