package com.mcraft.render;

import com.mcraft.world.Block;
import com.mcraft.world.Chunk;
import com.mcraft.world.World;

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

    private boolean wasRecentlyModified = false;
    private int     framesSinceModify   = 0;
    private static final int STABLE_AFTER_FRAMES = 60;

    private static final int MAX_CHUNK_QUADS = 65536;

    private static final float[] LIGHT_CACHE = new float[16];
    static {
        for(int i=0; i<16; i++) LIGHT_CACHE[i] = i / 15.0f;
    }

    private static final float DOOR_THICK = 0.1875f;

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

    private static final float[] FACE_LIGHT = {1.0f,0.5f,0.8f,0.7f,0.65f,0.65f};


    public void buildMesh(Chunk chunk, World world) {
        int cs = Chunk.SIZE, ch = Chunk.HEIGHT;
        int cx = chunk.getChunkX(), cz = chunk.getChunkZ();

        Chunk nN = world.getChunkIfLoaded(cx,   cz - 1);
        Chunk nS = world.getChunkIfLoaded(cx,   cz + 1);
        Chunk nW = world.getChunkIfLoaded(cx - 1, cz);
        Chunk nE = world.getChunkIfLoaded(cx + 1, cz);

        FloatBuffer vBuf = ChunkMeshBufferPool.OPAQUE_V_BUF.get(); vBuf.clear();
        IntBuffer   iBuf = ChunkMeshBufferPool.OPAQUE_I_BUF.get(); iBuf.clear();
        FloatBuffer wBuf = ChunkMeshBufferPool.WATER_V_BUF.get();  wBuf.clear();
        IntBuffer   wIdx = ChunkMeshBufferPool.WATER_I_BUF.get();  wIdx.clear();

        int quadCount  = 0;
        int wQuadCount = 0;

        for (int y = 0; y < ch; y++) {
            for (int z = 0; z < cs; z++) {
                for (int x = 0; x < cs; x++) {
                    Block block = chunk.getBlockUnsafe(x, y, z);
                    boolean isWater = (block == Block.WATER);

                    if (block == Block.AIR) continue;

                    if (block == Block.DOOR_CLOSED || block == Block.DOOR_OPEN) {
                        boolean open = (block == Block.DOOR_OPEN);
                        if (quadCount + 6 < MAX_CHUNK_QUADS) {
                            quadCount += addDoorGeometry( x, y, z, open, chunk, nN, nS, nW, nE, vBuf, iBuf, quadCount, world, cx, cz);
                        }
                        continue;
                    }

                    if (block == Block.TORCH) {
                        if (quadCount + 6 < MAX_CHUNK_QUADS) {
                            quadCount += addTorchGeometry(x, y, z, chunk, vBuf, iBuf, quadCount, world, cx, cz);
                        }
                        continue;
                    }

                    if (block == Block.BED) {
                        if (quadCount + 6 < MAX_CHUNK_QUADS) {
                            quadCount += addBedGeometry(x, y, z, chunk, nN, nS, nW, nE, vBuf, iBuf, quadCount, world, cx, cz);
                        }
                        continue;
                    }

                    if (!block.solid && !isWater) continue;

                    for (int face = 0; face < 6; face++) {
                        int[] dir = FACE_DIR[face];
                        int nx = x + dir[0];
                        int ny = y + dir[1];
                        int nz = z + dir[2];

                        Block neighbor = getNeighborFast(chunk, nN, nS, nW, nE, nx, ny, nz);

                        boolean isLeafFace = false;

                        if (block.solid) {
                            if (neighbor.solid) {
                                if (block.isLeaves() && neighbor.isLeaves()) {
                                    isLeafFace = true;
                                }
                                else if (!block.isLeaves() && neighbor.isLeaves()) {
                                }
                                else {
                                    continue;
                                }
                            }
                        }
                                                
                        if (isWater && (neighbor == Block.WATER || neighbor.solid)) {
                            continue;
                        }

                        if (isWater && (nx < 0 || nx >= cs || nz < 0 || nz >= cs)) {
                            boolean neighborLoaded = (nx < 0  && nN != null) ||
                                                    (nx >= cs && nS != null) || 
                                                    (nz < 0  && nW != null) ||
                                                    (nz >= cs && nE != null);
                            if (!neighborLoaded) continue;
                        }

                        if (vBuf.position() + 32 >= vBuf.capacity() || wBuf.position() + 32 >= wBuf.capacity()) {
                            System.err.println("[Mesh] Overflow de segurança atingido na Thread Pool para o chunk: " + cx + ", " + cz);
                            break;
                        }

                        float[] uvs   = block.getUVs(face);
                        float[][] vv  = FACE_VERTS[face];

                        int skyLight, blockLight;
                        int lx = nx;
                        int lz = nz;

                        if (lx >= 0 && lx < cs && lz >= 0 && lz < cs) {
                            skyLight   = chunk.getSkyLight(lx, ny, lz);
                            blockLight = chunk.getBlockLight(lx, ny, lz);
                        } else {
                            int wx = (cx << 4) + nx;
                            int wz = (cz << 4) + nz;
                            skyLight   = world.getSkyLightAt(wx, ny, wz);
                            blockLight = world.getBlockLightAt(wx, ny, wz);
                        }

                        float skyNorm   = LIGHT_CACHE[skyLight];
                        float blockNorm = LIGHT_CACHE[blockLight];
                        
                        float lightDir  = FACE_LIGHT[face];
                        if (isWater && face == 0) {
                            lightDir *= 1.1f;
                        }

                        boolean isTransparentPass = isWater || isLeafFace;
                        FloatBuffer target = isTransparentPass ? wBuf : vBuf;

                        if (isLeafFace) {
                            lightDir = -lightDir; 
                        }

                        for (int v = 0; v < 4; v++) {
                            target.put(x + vv[v][0]).put(y + vv[v][1]).put(z + vv[v][2]);
                            target.put(uvs[v * 2]).put(uvs[v * 2 + 1]);
                            target.put(lightDir);
                            target.put(skyNorm);
                            target.put(blockNorm);
                        }

                        if (isTransparentPass) {
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

        if (vBuf.hasRemaining()) {
            upload(vBuf, iBuf, false);  
        }
        if (wBuf.hasRemaining()) {
            upload(wBuf, wIdx, true); 
        }
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

    private int addDoorGeometry(int bx, int by, int bz, boolean open, Chunk chunk, Chunk nN, Chunk nS, Chunk nW, Chunk nE, FloatBuffer vBuf, IntBuffer iBuf, int baseQuad, World world, int cx0, int cz0) {
        Block westN = getNeighborFast(chunk, nN, nS, nW, nE, bx-1, by, bz);
        Block eastN = getNeighborFast(chunk, nN, nS, nW, nE, bx+1, by, bz);
        boolean wallRunsAlongX = westN.solid || eastN.solid;

        boolean spanX = open ? !wallRunsAlongX : wallRunsAlongX;

        float x0, x1, z0, z1;
        if (spanX) {
            x0 = bx;      x1 = bx + 1f;
            z0 = bz;      z1 = bz + DOOR_THICK;
        } else {
            x0 = bx;      x1 = bx + DOOR_THICK;
            z0 = bz;      z1 = bz + 1f;
        }
        float y0 = by, y1 = by + 1f;

        int lx = bx - cx0, lz = bz - cz0;
        int skyL, blockL;
        if (lx >= 0 && lx < Chunk.SIZE && lz >= 0 && lz < Chunk.SIZE) {
            skyL   = chunk.getSkyLight  (lx, by, lz);
            blockL = chunk.getBlockLight(lx, by, lz);
        } else {
            skyL   = world.getSkyLightAt  (bx, by, bz);
            blockL = world.getBlockLightAt(bx, by, bz);
        }
        float sn = skyL / 15f, bn = blockL / 15f;

        float ts = 1f/16f;
        float u0 = 12*ts, u1 = 13*ts, v0 = 12*ts, v1 = 13*ts;

        int q = baseQuad;
        q = addBoxFace(vBuf, iBuf, q, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, u0,v0,u1,v1, 1.00f, sn, bn);
        q = addBoxFace(vBuf, iBuf, q, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1, u0,v0,u1,v1, 0.50f, sn, bn);
        q = addBoxFace(vBuf, iBuf, q, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, u0,v0,u1,v1, 0.85f, sn, bn);
        q = addBoxFace(vBuf, iBuf, q, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, u0,v0,u1,v1, 0.75f, sn, bn);
        q = addBoxFace(vBuf, iBuf, q, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, u0,v0,u1,v1, 0.70f, sn, bn);
        q = addBoxFace(vBuf, iBuf, q, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, u0,v0,u1,v1, 0.70f, sn, bn);

        return q - baseQuad;
    }

    private int addTorchGeometry(int bx, int by, int bz, Chunk chunk, FloatBuffer vBuf, IntBuffer iBuf, int baseQuad, World world, int cx0, int cz0) {
        float x0 = bx + 0.4375f; float x1 = bx + 0.5625f;
        float y0 = by;           float y1 = by + 0.625f;
        float z0 = bz + 0.4375f; float z1 = bz + 0.5625f;

        float sn = getSkyLightAt(chunk, world, bx, by, bz, cx0, cz0) / 15f;
        float bn = 1.0f; 

        float[] uvs = Block.TORCH.getUVs(0);
        float uStart = uvs[0];
        float uEnd   = uvs[2];
        float vEnd   = uvs[1]; 
        float vStart = uvs[5]; 

        float texWidth = uEnd - uStart;
        float u0 = uStart + texWidth * 0.4375f;
        float u1 = uStart + texWidth * 0.5625f;
        
        float texHeight = vEnd - vStart;
        float v0 = vEnd - texHeight * 0.625f; 
        float v1 = vEnd;                       

        float pad = 0.001f;
        float ut0 = u0 + pad;
        float ut1 = u1 - pad;
        float vt0 = v0 + pad;

        int q = baseQuad;
        q = addBoxFace(vBuf, iBuf, q, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, ut0,vt0,ut1,vt0, 1.00f, sn, bn);
        q = addBoxFace(vBuf, iBuf, q, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1, u0,v1,u1,v1, 0.50f, sn, bn);
        q = addBoxFace(vBuf, iBuf, q, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, u0,v0,u1,v1, 0.85f, sn, bn); 
        q = addBoxFace(vBuf, iBuf, q, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, u0,v0,u1,v1, 0.75f, sn, bn);
        q = addBoxFace(vBuf, iBuf, q, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, u0,v0,u1,v1, 0.70f, sn, bn); 
        q = addBoxFace(vBuf, iBuf, q, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, u0,v0,u1,v1, 0.70f, sn, bn);

        return q - baseQuad;
    }

    private int addBedGeometry(int bx, int by, int bz, Chunk chunk, Chunk nN, Chunk nS, Chunk nW, Chunk nE, FloatBuffer vBuf, IntBuffer iBuf, int baseQuad, World world, int cx0, int cz0) {
        Block westN = getNeighborFast(chunk, nN, nS, nW, nE, bx-1, by, bz);
        Block eastN = getNeighborFast(chunk, nN, nS, nW, nE, bx+1, by, bz);
        boolean alignAlongX = westN.solid || eastN.solid;

        float x0 = bx, x1 = bx + 1f;
        float y0 = by, y1 = by + 0.5625f;
        float z0 = bz, z1 = bz + 1f;

        if (alignAlongX) {
            z0 = bz + 0.0625f; z1 = bz + 0.9375f;
        } else {
            x0 = bx + 0.0625f; x1 = bx + 0.9375f;
        }

        int lx = bx - cx0, lz = bz - cz0;
        int skyL, blockL;
        
        if (lx >= 0 && lx < Chunk.SIZE && lz >= 0 && lz < Chunk.SIZE) {
            skyL   = chunk.getSkyLight  (lx, by, lz);
            blockL = chunk.getBlockLight(lx, by, lz);
        } else {
            skyL   = world.getSkyLightAt  (bx, by, bz);
            blockL = world.getBlockLightAt(bx, by, bz);
        }

        float sn = skyL / 15f;
        float bn = blockL / 15f;

        int q = baseQuad;
        
        float[] uv0 = Block.BED.getUVs(0);
        float[] uv1 = Block.BED.getUVs(1);
        float[] uv2 = Block.BED.getUVs(2);
        float[] uv3 = Block.BED.getUVs(3);
        float[] uv4 = Block.BED.getUVs(4);
        float[] uv5 = Block.BED.getUVs(5);

        q = addBoxFace(vBuf, iBuf, q, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, uv0[0],uv0[3],uv0[2],uv0[5], 1.00f, sn, bn);
        q = addBoxFace(vBuf, iBuf, q, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1, uv1[0],uv1[3],uv1[2],uv1[5], 0.50f, sn, bn);
        
        float vTop2 = uv2[5] + (uv2[3] - uv2[5]) * (1.0f - 0.5625f);
        float vTop3 = uv3[5] + (uv3[3] - uv3[5]) * (1.0f - 0.5625f);
        float vTop4 = uv4[5] + (uv4[3] - uv4[5]) * (1.0f - 0.5625f);
        float vTop5 = uv5[5] + (uv5[3] - uv5[5]) * (1.0f - 0.5625f);

        q = addBoxFace(vBuf, iBuf, q, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, uv2[0],vTop2,uv2[2],uv2[3], 0.85f, sn, bn);
        q = addBoxFace(vBuf, iBuf, q, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, uv3[0],vTop3,uv3[2],uv3[3], 0.75f, sn, bn);
        q = addBoxFace(vBuf, iBuf, q, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, uv4[0],vTop4,uv4[2],uv4[3], 0.70f, sn, bn);
        q = addBoxFace(vBuf, iBuf, q, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, uv5[0],vTop5,uv5[2],uv5[3], 0.70f, sn, bn);

        return q - baseQuad;
    }

    private int addBoxFace(FloatBuffer vBuf, IntBuffer iBuf, int quadIdx, float x0,float y0,float z0, float x1,float y1,float z1, float x2,float y2,float z2, float x3,float y3,float z3, float u0,float v0,float u1,float v1, float lightDir, float sky, float block) {
        vBuf.put(x0).put(y0).put(z0).put(u0).put(v1).put(lightDir).put(sky).put(block);
        vBuf.put(x1).put(y1).put(z1).put(u1).put(v1).put(lightDir).put(sky).put(block);
        vBuf.put(x2).put(y2).put(z2).put(u1).put(v0).put(lightDir).put(sky).put(block);
        vBuf.put(x3).put(y3).put(z3).put(u0).put(v0).put(lightDir).put(sky).put(block);
        int base = quadIdx * 4;
        iBuf.put(base).put(base+1).put(base+2).put(base+2).put(base+3).put(base);
        return quadIdx + 1;
    }

    private int getSkyLightAt(Chunk chunk, World world, int bx, int by, int bz, int cx0, int cz0) {
        int lx = bx - cx0, lz = bz - cz0;
        if (lx >= 0 && lx < Chunk.SIZE && lz >= 0 && lz < Chunk.SIZE) {
            return chunk.getSkyLight(lx, by, lz);
        }
        return world.getSkyLightAt(bx, by, bz);
    }

    private void upload(FloatBuffer vb, IntBuffer ib, boolean water) {
        int myVao, myVbo, myEbo;
        boolean isNewAllocation = false;

        if (water) {
            if (wVao != 0) {
                myVao = wVao; myVbo = wVbo; myEbo = wEbo;
            } else {
                wVao = myVao = glGenVertexArrays();
                wVbo = myVbo = glGenBuffers();
                wEbo = myEbo = glGenBuffers();
                isNewAllocation = true;
            }
        } else {
            if (vao != 0) {
                myVao = vao; myVbo = vbo; myEbo = ebo;
            } else {
                vao = myVao = glGenVertexArrays();
                vbo = myVbo = glGenBuffers();
                ebo = myEbo = glGenBuffers();
                isNewAllocation = true;
            }
        }

        framesSinceModify++;
        if (framesSinceModify > STABLE_AFTER_FRAMES) {
            wasRecentlyModified = false;
        }
        int hint = wasRecentlyModified ? GL_DYNAMIC_DRAW : GL_STATIC_DRAW;

        if (isNewAllocation) {
            setupAttributes(myVao, myVbo, myEbo, vb, ib, hint);
        } else {
            glBindBuffer(GL_ARRAY_BUFFER, myVbo);
            glBufferData(GL_ARRAY_BUFFER, vb, hint);
            
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, myEbo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, hint);
        }
    }

    public void delete() {
        if (vao  != 0) { glDeleteVertexArrays(vao);  glDeleteBuffers(vbo);  glDeleteBuffers(ebo);  }
        if (wVao != 0) { glDeleteVertexArrays(wVao); glDeleteBuffers(wVbo); glDeleteBuffers(wEbo); }
        vao = vbo = ebo = wVao = wVbo = wEbo = 0;
        indexCount = wIndexCount = 0;
    }

    private void setupAttributes(int vaoId, int vboId, int eboId, FloatBuffer vb, IntBuffer ib, int hint) {
        glBindVertexArray(vaoId);
        
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vb, hint);
        
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, hint);

        int stride = VERT_FLOATS * Float.BYTES;
        
        // aPos
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        
        // aUV
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 12L);
        glEnableVertexAttribArray(1);
        
        // aLightDir
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 20L);
        glEnableVertexAttribArray(2);
        
        // aSkyLight
        glVertexAttribPointer(3, 1, GL_FLOAT, false, stride, 24L);
        glEnableVertexAttribArray(3);
        
        // aBlockLight
        glVertexAttribPointer(4, 1, GL_FLOAT, false, stride, 28L);
        glEnableVertexAttribArray(4);
        
        glBindVertexArray(0);
    }

    private Block getNeighborFast(Chunk self, Chunk nN, Chunk nS, Chunk nW, Chunk nE, int lx, int ly, int lz) {
        if (ly < 0 || ly >= Chunk.HEIGHT) return Block.AIR;
        if (lx >= 0 && lx < Chunk.SIZE && lz >= 0 && lz < Chunk.SIZE)
            return self.getBlock(lx, ly, lz);
        if (lz < 0)            return (nN != null) ? nN.getBlock(lx, ly, Chunk.SIZE-1) : Block.AIR;
        if (lz >= Chunk.SIZE)  return (nS != null) ? nS.getBlock(lx, ly, 0)           : Block.AIR;
        if (lx < 0)            return (nW != null) ? nW.getBlock(Chunk.SIZE-1, ly, lz) : Block.AIR;
        return                         (nE != null) ? nE.getBlock(0, ly, lz)            : Block.AIR;
    }

    public void notifyModified() {
        wasRecentlyModified = true;
        framesSinceModify   = 0;
    }

}