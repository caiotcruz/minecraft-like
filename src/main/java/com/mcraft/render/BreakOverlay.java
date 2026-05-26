package com.mcraft.render;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class BreakOverlay {

    private final Shader crackShader;
    private int vao, vbo, ebo;

   
    private static final float[] VERTS = {
        0,1,0, 0,1,   0,1,1, 0,0,   1,1,1, 1,0,   1,1,0, 1,1,
        0,0,1, 0,0,   0,0,0, 0,1,   1,0,0, 1,1,   1,0,1, 1,0,
        0,0,1, 0,0,   1,0,1, 1,0,   1,1,1, 1,1,   0,1,1, 0,1,
        1,0,0, 0,0,   0,0,0, 1,0,   0,1,0, 1,1,   1,1,0, 0,1,
        0,0,0, 0,0,   0,0,1, 1,0,   0,1,1, 1,1,   0,1,0, 0,1,
        1,0,1, 0,0,   1,0,0, 1,0,   1,1,0, 1,1,   1,1,1, 0,1,
    };

    public BreakOverlay() {
        crackShader = new Shader("crack.vert", "crack.frag");
        initGPU();
    }

    public void render(int bx, int by, int bz, float progress,
                        float[] proj, float[] view) {
        if (progress < 0.01f) return;

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);       
        glEnable(GL_POLYGON_OFFSET_FILL); 
        glPolygonOffset(-1f, -1f);

        crackShader.use();
        crackShader.setMatrix4("uProjection", proj);
        crackShader.setMatrix4("uView",       view);
        crackShader.setFloat  ("uProgress",   progress);

        float s = 1.003f, o = -0.0015f; 
        float[] model = {
            s, 0, 0, 0,
            0, s, 0, 0,
            0, 0, s, 0,
            bx + o, by + o, bz + o, 1f
        };
        crackShader.setMatrix4("uModel", model);

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);

        glDisable(GL_POLYGON_OFFSET_FILL);
        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);
    }

    public void delete() {
        crackShader.delete();
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }


    private void initGPU() {
        int[] indices = new int[36];
        for (int f = 0; f < 6; f++) {
            int base = f * 4;
            int i    = f * 6;
            indices[i]   = base;   indices[i+1] = base+1; indices[i+2] = base+2;
            indices[i+3] = base+2; indices[i+4] = base+3; indices[i+5] = base;
        }

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        FloatBuffer vb = BufferUtils.createFloatBuffer(VERTS.length);
        vb.put(VERTS).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vb, GL_STATIC_DRAW);

        IntBuffer ib = BufferUtils.createIntBuffer(36);
        ib.put(indices).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        int stride = 5 * Float.BYTES; 
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 12L);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }
}