package com.mcraft.render;

import com.mcraft.world.Block;
import com.mcraft.world.Chunk;
import com.mcraft.world.World;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ChunkRenderer {

    private static final int VERT_FLOATS = 8; 

    private int vao, vbo, ebo;
    private int indexCount;

    private int wVao, wVbo, wEbo;
    private int wIndexCount;

    private static int MAX_QUADS = Chunk.SIZE * Chunk.HEIGHT * Chunk.SIZE * 6;
    private static final FloatBuffer OPAQUE_V_BUF = BufferUtils.createFloatBuffer(MAX_QUADS * 4 * 8);
    private static final IntBuffer   OPAQUE_I_BUF = BufferUtils.createIntBuffer(MAX_QUADS * 6);
    private static final FloatBuffer WATER_V_BUF = BufferUtils.createFloatBuffer(MAX_QUADS * 4 * VERT_FLOATS);
    private static final IntBuffer   WATER_I_BUF = BufferUtils.createIntBuffer(MAX_QUADS * 6);
    private static final float[] LIGHT_CACHE = new float[16];
    static {
        for(int i=0; i<16; i++) LIGHT_CACHE[i] = i / 15.0f;
    }

    private static final float BED_TOP_Y = 0.5625f;

    private static final int[][] FACE_DIR = {
        { 0, 1, 0}, { 0,-1, 0}, { 0, 0, 1},
        { 0, 0,-1}, {-1, 0, 0}, { 1, 0, 0}
    };
    private static final float[][][] FACE_VERTS = {
        {{0,1,0},{0,1,1},{1,1,1},{1,1,0}}, 
        {{0,0,1},{0,0,0},{1,0,0},{1,0,1}},
        {{0,0,1},{1,0,1},{1,1,1},{0,1,1}}, 
        {{1,0,0},{0,0,0},{0,1,0},{1,1,0}}, 
        {{0,0,0},{0,0,1},{0,1,1},{0,1,0}}, 
        {{1,0,1},{1,0,0},{1,1,0},{1,1,1}}  
    };

    private static final float[][][] BED_VERTS = {
        {{0,BED_TOP_Y,0},{0,BED_TOP_Y,1},{1,BED_TOP_Y,1},{1,BED_TOP_Y,0}},
        {{0,0,1},{0,0,0},{1,0,0},{1,0,1}},
        {{0,0,1},{1,0,1},{1,BED_TOP_Y,1},{0,BED_TOP_Y,1}},
        {{1,0,0},{0,0,0},{0,BED_TOP_Y,0},{1,BED_TOP_Y,0}},
        {{0,0,0},{0,0,1},{0,BED_TOP_Y,1},{0,BED_TOP_Y,0}},
        {{1,0,1},{1,0,0},{1,BED_TOP_Y,0},{1,BED_TOP_Y,1}}
    };

    private static final float[] FACE_LIGHT = {1.0f,0.5f,0.8f,0.7f,0.65f,0.65f};


    public void buildMesh(Chunk chunk, World world) {
        int cs = Chunk.SIZE, ch = Chunk.HEIGHT;
        int cx = chunk.getChunkX(), cz = chunk.getChunkZ();

        Chunk nN = world.getChunkIfLoaded(cx,   cz-1);
        Chunk nS = world.getChunkIfLoaded(cx,   cz+1);
        Chunk nW = world.getChunkIfLoaded(cx-1, cz  );
        Chunk nE = world.getChunkIfLoaded(cx+1, cz  );

        FloatBuffer vBuf = OPAQUE_V_BUF; vBuf.clear();
        IntBuffer   iBuf = OPAQUE_I_BUF; iBuf.clear();
        FloatBuffer wBuf = WATER_V_BUF; wBuf.clear();
        IntBuffer   wIdx = WATER_I_BUF; wIdx.clear();

        int quadCount  = 0;
        int wQuadCount = 0;

        for (int y = 0; y < ch; y++) {
            for (int z = 0; z < cs; z++) {
                for (int x = 0; x < cs; x++) {
                    Block block = chunk.getBlock(x, y, z);
                    boolean isWater = (block == Block.WATER);

                    if (block == Block.AIR) continue;
                    if (!block.solid && !isWater) continue;

                    for (int face = 0; face < 6; face++) {
                        int[] dir = FACE_DIR[face];
                        int nx = x + dir[0];
                        int ny = y + dir[1];
                        int nz = z + dir[2];

                        Block neighbor = getNeighborFast(chunk, nN, nS, nW, nE, nx, ny, nz);

                        if (block.solid && neighbor.solid) continue;
                        if (isWater && (neighbor == Block.WATER || neighbor.solid)) continue;

                        if (isWater && (nx < 0 || nx >= cs || nz < 0 || nz >= cs)) {
                            boolean neighborLoaded = (nx < 0  && nN != null) ||
                                                    (nx >= cs && nS != null) || 
                                                    (nz < 0  && nW != null) ||
                                                    (nz >= cs && nE != null);
                            if (!neighborLoaded) continue;
                        }

                        float[] uvs   = block.getUVs(face);
                        float[][] vv  = (block == Block.BED) ? BED_VERTS[face] : FACE_VERTS[face];

                        int skyLight, blockLight;
                        int lx = nx;
                        int lz = nz;

                        if (lx >= 0 && lx < cs && lz >= 0 && lz < cs) {
                            skyLight   = chunk.getSkyLight(lx, ny, lz);
                            blockLight = chunk.getBlockLight(lx, ny, lz);
                        } else {
                            int wx = (cx << 4) + nx;
                            int wz = cz * cs + nz;
                            skyLight   = world.getSkyLightAt(wx, ny, wz);
                            blockLight = world.getBlockLightAt(wx, ny, wz);
                        }

                        float skyNorm = LIGHT_CACHE[skyLight];
                        float blockNorm = blockLight / 15.0f;
                        
                        float lightDir  = FACE_LIGHT[face];
                        if (isWater && face == 0) {
                            lightDir *= 1.1f;
                        }

                        FloatBuffer target = isWater ? wBuf : vBuf;

                        for (int v = 0; v < 4; v++) {
                            target.put(x + vv[v][0]).put(y + vv[v][1]).put(z + vv[v][2]);
                            target.put(uvs[v * 2]).put(uvs[v * 2 + 1]);
                            target.put(lightDir);
                            target.put(skyNorm);
                            target.put(blockNorm);
                        }

                        if (isWater) {
                            int base = wQuadCount * 4;
                            wIdx.put(base).put(base + 1).put(base + 2)
                                .put(base + 2).put(base + 3).put(base);
                            wQuadCount++;
                        } else {
                            int base = quadCount * 4;
                            iBuf.put(base).put(base + 1).put(base + 2)
                                .put(base + 2).put(base + 3).put(base);
                            quadCount++;
                        }
                    }
                }
            }
        }

        vBuf.flip(); iBuf.flip(); wBuf.flip(); wIdx.flip();
        indexCount  = quadCount  * 6;
        wIndexCount = wQuadCount * 6;

        upload(vBuf, iBuf, false);  
        upload(wBuf, wIdx, true); 
    }

    public void render() {
        if (indexCount == 0) return;
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    public void renderWater() {
        if (wIndexCount == 0) return;
        glBindVertexArray(wVao);
        glDrawElements(GL_TRIANGLES, wIndexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }


    private void upload(FloatBuffer vb, IntBuffer ib, boolean water) {
        int myVao, myVbo, myEbo;

        if (water) {
            if (wVao != 0) { glDeleteVertexArrays(wVao); glDeleteBuffers(wVbo); glDeleteBuffers(wEbo); }
            wVao = myVao = glGenVertexArrays();
            wVbo = myVbo = glGenBuffers();
            wEbo = myEbo = glGenBuffers();
        } else {
            if (vao != 0) { glDeleteVertexArrays(vao); glDeleteBuffers(vbo); glDeleteBuffers(ebo); }
            vao = myVao = glGenVertexArrays();
            vbo = myVbo = glGenBuffers();
            ebo = myEbo = glGenBuffers();
        }

        glBindVertexArray(myVao);
        glBindBuffer(GL_ARRAY_BUFFER, myVbo);
        glBufferData(GL_ARRAY_BUFFER, vb, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, myEbo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_DYNAMIC_DRAW);

        int stride = VERT_FLOATS * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 12L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 20L);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(3, 1, GL_FLOAT, false, stride, 24L);
        glEnableVertexAttribArray(3);
        glVertexAttribPointer(4, 1, GL_FLOAT, false, stride, 28L);
        glEnableVertexAttribArray(4);
        glBindVertexArray(0);
    }

    public void delete() {
        if (vao  != 0) { glDeleteVertexArrays(vao);  glDeleteBuffers(vbo);  glDeleteBuffers(ebo);  }
        if (wVao != 0) { glDeleteVertexArrays(wVao); glDeleteBuffers(wVbo); glDeleteBuffers(wEbo); }
        vao = vbo = ebo = wVao = wVbo = wEbo = 0;
        indexCount = wIndexCount = 0;
    }

    private Block getNeighborFast(Chunk self,
                                   Chunk nN, Chunk nS, Chunk nW, Chunk nE,
                                   int lx, int ly, int lz) {
        if (ly < 0 || ly >= Chunk.HEIGHT) return Block.AIR;
        if (lx >= 0 && lx < Chunk.SIZE && lz >= 0 && lz < Chunk.SIZE)
            return self.getBlock(lx, ly, lz);
        if (lz < 0)            return (nN != null) ? nN.getBlock(lx, ly, Chunk.SIZE-1) : Block.AIR;
        if (lz >= Chunk.SIZE)  return (nS != null) ? nS.getBlock(lx, ly, 0)           : Block.AIR;
        if (lx < 0)            return (nW != null) ? nW.getBlock(Chunk.SIZE-1, ly, lz) : Block.AIR;
        return                         (nE != null) ? nE.getBlock(0, ly, lz)            : Block.AIR;
    }
}