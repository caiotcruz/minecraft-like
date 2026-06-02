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

    private static final int VERT_FLOATS = 6; 

    private int vao, vbo, ebo;
    private int indexCount;

    private int wVao, wVbo, wEbo;
    private int wIndexCount;

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

        int maxQ = cs * ch * cs * 6;
        FloatBuffer vBuf = BufferUtils.createFloatBuffer(maxQ * 4 * VERT_FLOATS);
        IntBuffer   iBuf = BufferUtils.createIntBuffer (maxQ * 6);
        FloatBuffer wBuf = BufferUtils.createFloatBuffer(maxQ * 4 * VERT_FLOATS);
        IntBuffer   wIdx = BufferUtils.createIntBuffer (maxQ * 6);

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
                        Block neighbor = getNeighborFast(
                            chunk, nN, nS, nW, nE,
                            x + dir[0], y + dir[1], z + dir[2]
                        );

                        if (block.solid && neighbor.solid) continue;
                        if (isWater && (neighbor == Block.WATER || neighbor.solid)) continue;

                        float[] uvs   = block.getUVs(face);
                        float   light = FACE_LIGHT[face];
                        float[][] vv  = FACE_VERTS[face];

                        if (block == Block.BED){
                            vv = (block == Block.BED) ? BED_VERTS[face] : FACE_VERTS[face];
                        }

                        if (isWater) {

                            int nx = x + dir[0], ny = y + dir[1], nz = z + dir[2];

                            boolean crossBorder = (nx < 0 || nx >= cs || nz < 0 || nz >= cs);

                            if (crossBorder) {
                                boolean neighborLoaded =
                                    (nx < 0      && nN != null) ||
                                    (nx >= cs    && nS != null) || 
                                    (nz < 0      && nW != null) ||
                                    (nz >= cs    && nE != null);

                                if (!neighborLoaded) continue; 
                            }

                            neighbor = getNeighborFast(chunk, nN, nS, nW, nE, nx, ny, nz);
                            if (neighbor == Block.WATER || neighbor.solid) continue;


                            for (int v = 0; v < 4; v++) {
                                float wl = (face == 0) ? light * 1.1f : light * 0.85f;
                                wBuf.put(x+vv[v][0]).put(y+vv[v][1]).put(z+vv[v][2])
                                    .put(uvs[v*2]).put(uvs[v*2+1]).put(wl);
                            }
                            int base = wQuadCount * 4;
                            wIdx.put(base).put(base+1).put(base+2)
                                .put(base+2).put(base+3).put(base);
                            wQuadCount++;
                        } else {
                            for (int v = 0; v < 4; v++) {
                                vBuf.put(x+vv[v][0]).put(y+vv[v][1]).put(z+vv[v][2])
                                    .put(uvs[v*2]).put(uvs[v*2+1]).put(light);
                            }
                            int base = quadCount * 4;
                            iBuf.put(base).put(base+1).put(base+2)
                                .put(base+2).put(base+3).put(base);
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