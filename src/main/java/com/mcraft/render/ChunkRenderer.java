package com.mcraft.render;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import com.mcraft.world.Block;
import com.mcraft.world.Chunk;

public class ChunkRenderer {

    private static final int VERTEX_SIZE = 6;

    private int vao, vbo;
    private int vertexCount;

    private static final float[][] FACE_NORMALS = {
        { 0,  1,  0}, { 0, -1,  0},
        { 0,  0,  1}, { 0,  0, -1},
        {-1,  0,  0}, { 1,  0,  0}
    };

    private static final float[][][] FACE_VERTICES = {
        {{0,1,0}, {1,1,0}, {1,1,1}, {0,1,1}},  
        {{0,0,1}, {1,0,1}, {1,0,0}, {0,0,0}},
        {{0,0,1}, {1,0,1}, {1,1,1}, {0,1,1}}, 
        {{1,0,0}, {0,0,0}, {0,1,0}, {1,1,0}}, 
        {{0,0,0}, {0,0,1}, {0,1,1}, {0,1,0}},  
        {{1,0,1}, {1,0,0}, {1,1,0}, {1,1,1}}   
    };

    private static final float[] FACE_LIGHT = {1.0f, 0.5f, 0.8f, 0.8f, 0.6f, 0.6f};

    public void buildMesh(Chunk chunk) {
        int cs = Chunk.SIZE;
        int ch = Chunk.HEIGHT;
        float[] buffer = new float[cs * ch * cs * 6 * 4 * VERTEX_SIZE];
        int pos = 0;

        for (int x = 0; x < cs; x++) {
            for (int y = 0; y < ch; y++) {
                for (int z = 0; z < cs; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (!block.solid) continue;

                    for (int face = 0; face < 6; face++) {
                        // Verifica se a face está exposta
                        float[] n = FACE_NORMALS[face];
                        int nx = x + (int)n[0];
                        int ny = y + (int)n[1];
                        int nz = z + (int)n[2];

                        Block neighbor = chunk.getBlockSafe(nx, ny, nz);
                        if (neighbor.solid) continue; // Face oculta — pula

                        float[] uvs = block.getUVs(face);
                        float light = FACE_LIGHT[face];

                        // Adiciona os 4 vértices do quad
                        float[][] verts = FACE_VERTICES[face];
                        int[] uvOrder = {0, 1, 2, 3}; // índices UV para cada vértice

                        for (int v = 0; v < 4; v++) {
                            buffer[pos++] = x + verts[v][0];
                            buffer[pos++] = y + verts[v][1];
                            buffer[pos++] = z + verts[v][2];
                            buffer[pos++] = uvs[uvOrder[v] * 2    ]; // U
                            buffer[pos++] = uvs[uvOrder[v] * 2 + 1]; // V
                            buffer[pos++] = light;
                        }
                    }
                }
            }
        }

        vertexCount = pos / VERTEX_SIZE;
        uploadToGPU(buffer, pos);
    }

    private void uploadToGPU(float[] data, int count) {
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
        }

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        java.nio.FloatBuffer fb = org.lwjgl.BufferUtils.createFloatBuffer(count);
        fb.put(data, 0, count).flip();
        glBufferData(GL_ARRAY_BUFFER, fb, GL_DYNAMIC_DRAW);

        int stride = VERTEX_SIZE * Float.BYTES;

        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
    }

    public void render() {
        if (vertexCount == 0) return;
        glBindVertexArray(vao);
        glDrawArrays(GL_QUADS, 0, vertexCount);
        glBindVertexArray(0);
    }
}