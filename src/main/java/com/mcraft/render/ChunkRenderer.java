package com.mcraft.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
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
import com.mcraft.world.World;

public class ChunkRenderer {

    private static final int VERTEX_FLOATS = 6;

    private int vao, vbo, ebo;
    private int indexCount;


    private static final int[][] FACE_DIR = {
        { 0,  1,  0}, { 0, -1,  0},
        { 0,  0,  1}, { 0,  0, -1},
        {-1,  0,  0}, { 1,  0,  0}
    };

    private static final float[][][] FACE_VERTS = {

        {
            {0,1,0},
            {0,1,1},
            {1,1,1},
            {1,1,0}
        },

        {
            {0,0,0},
            {1,0,0},
            {1,0,1},
            {0,0,1}
        },

        {
            {0,0,1},
            {1,0,1},
            {1,1,1},
            {0,1,1}
        },

        {
            {1,0,0},
            {0,0,0},
            {0,1,0},
            {1,1,0}
        },

        {
            {0,0,0},
            {0,0,1},
            {0,1,1},
            {0,1,0}
        },

        {
            {1,0,1},
            {1,0,0},
            {1,1,0},
            {1,1,1}
        }
    };

    private static final float[] FACE_LIGHT = {
        1.0f,   
        0.5f,   
        0.8f,   
        0.8f, 
        0.65f,  
        0.65f   
    };


    public void buildMesh(Chunk chunk, World world) {
        int cx = chunk.getChunkX(), cz = chunk.getChunkZ();
        Chunk nNorth = world.getChunkIfLoaded(cx,     cz - 1); 
        Chunk nSouth = world.getChunkIfLoaded(cx,     cz + 1); 
        Chunk nWest  = world.getChunkIfLoaded(cx - 1, cz    ); 
        Chunk nEast  = world.getChunkIfLoaded(cx + 1, cz    );
        int cs = Chunk.SIZE;
        int ch = Chunk.HEIGHT;

        int maxQuads = cs * ch * cs * 6;
        FloatBuffer vBuf = BufferUtils.createFloatBuffer(maxQuads * 4 * VERTEX_FLOATS);
        IntBuffer   iBuf = BufferUtils.createIntBuffer (maxQuads * 6);

        int quadCount = 0;

        for (int y = 0; y < ch; y++) {
            for (int z = 0; z < cs; z++) {
                for (int x = 0; x < cs; x++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (!block.solid) continue;

                    // Coordenadas globais (para contexto, mas não usadas no lookup)
                    for (int face = 0; face < 6; face++) {
                        int[] dir = FACE_DIR[face];
                        int nx = x + dir[0];
                        int ny = y + dir[1];
                        int nz = z + dir[2];

                        Block neighbor = getNeighborFast(
                            chunk, nNorth, nSouth, nWest, nEast,
                            nx, ny, nz
                        );
                        if (neighbor.solid) continue;

                        float[] uvs   = block.getUVs(face);
                        float   light = FACE_LIGHT[face];

                        float[][] verts = FACE_VERTS[face];
                        for (int v = 0; v < 4; v++) {
                            vBuf.put(x + verts[v][0]); 
                            vBuf.put(y + verts[v][1]);
                            vBuf.put(z + verts[v][2]);
                            vBuf.put(uvs[v * 2    ]);
                            vBuf.put(uvs[v * 2 + 1]); 
                            vBuf.put(light);          
                        }

                        int base = quadCount * 4;
                        iBuf.put(base    ); iBuf.put(base + 1); iBuf.put(base + 2);
                        iBuf.put(base + 2); iBuf.put(base + 3); iBuf.put(base    );

                        quadCount++;
                    }
                }
            }
        }

        vBuf.flip();
        iBuf.flip();
        indexCount = quadCount * 6;

        uploadToGPU(vBuf, iBuf);
    }

    private Block getNeighborFast(Chunk self, Chunk nNorth, Chunk nSouth, Chunk nWest,  Chunk nEast, int lx, int ly, int lz) {

        if (ly < 0 || ly >= Chunk.HEIGHT) return Block.AIR;

        if (lx >= 0 && lx < Chunk.SIZE && lz >= 0 && lz < Chunk.SIZE) {
            return self.getBlock(lx, ly, lz);
        }

        if (lz < 0) { 
            return (nNorth != null) ? nNorth.getBlock(lx, ly, Chunk.SIZE - 1) : Block.AIR;
        }
        if (lz >= Chunk.SIZE) { 
            return (nSouth != null) ? nSouth.getBlock(lx, ly, 0) : Block.AIR;
        }
        if (lx < 0) {
            return (nWest != null) ? nWest.getBlock(Chunk.SIZE - 1, ly, lz) : Block.AIR;
        }
        //  (X+)
        return (nEast != null) ? nEast.getBlock(0, ly, lz) : Block.AIR;
    }

    private void uploadToGPU(FloatBuffer vBuf, IntBuffer iBuf) {
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
            glDeleteBuffers(ebo);
        }

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vBuf, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, iBuf, GL_DYNAMIC_DRAW);

        int stride = VERTEX_FLOATS * Float.BYTES;

        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 12L);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 20L);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
    }


    public void render() {
        if (indexCount == 0) return;
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    public void delete() {

        if (vao != 0) glDeleteVertexArrays(vao);
        if (vbo != 0) glDeleteBuffers(vbo);
        if (ebo != 0) glDeleteBuffers(ebo);

        vao = 0;
        vbo = 0;
        ebo = 0;

        indexCount = 0;
    }
}