package com.mcraft.world;

import com.mcraft.render.ChunkRenderer;
import com.mcraft.render.Shader;

public class Chunk {

    public static final int SIZE   = 16;
    public static final int HEIGHT = 128;

    // Array compacto: 16 × 128 × 16 = 32.768 bytes ≈ 32 KB por chunk
    private final byte[] blocks = new byte[SIZE * HEIGHT * SIZE];

    private final int chunkX, chunkZ;   // Posição em coordenadas de chunk
    private boolean dirty = true;        // Mesh precisa ser reconstruída?

    private final ChunkRenderer renderer = new ChunkRenderer();

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    // ── Índice ───────────────────────────────────────────────────────────────

    /** Converte coordenadas locais (x,y,z) para índice no array. */
    private static int idx(int x, int y, int z) {
        return y * SIZE * SIZE + z * SIZE + x;
    }

    // ── Acesso a blocos ──────────────────────────────────────────────────────

    /**
     * Retorna o bloco em coordenadas locais (0…SIZE-1, 0…HEIGHT-1, 0…SIZE-1).
     * Retorna AIR para coordenadas fora dos limites (sem exceção).
     */
    public Block getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) {
            return Block.AIR;
        }
        return Block.fromId(blocks[idx(x, y, z)] & 0xFF); // & 0xFF: byte → int sem sinal
    }

    /**
     * Define um bloco em coordenadas locais e marca a mesh como suja.
     */
    public void setBlock(int x, int y, int z, int blockId) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) return;
        blocks[idx(x, y, z)] = (byte) blockId;
        dirty = true;
    }

    /**
     * Copia um array de dados gerados pelo WorldGen para este chunk.
     */
    public void setBlocks(byte[] data) {
        int len = Math.min(data.length, blocks.length);
        System.arraycopy(data, 0, blocks, 0, len);
        dirty = true;
    }

    // ── Renderização ─────────────────────────────────────────────────────────

    /**
     * Renderiza o chunk. Se a mesh estiver suja, reconstrói antes de desenhar.
     *
     * @param shader  Shader com uniform uModel já ativo
     * @param world   Necessário para consultar blocos vizinhos de outros chunks
     */
    public void render(Shader shader, World world) {
        if (dirty) {
            renderer.buildMesh(this, world);
            dirty = false;
        }

        // Model matrix: identidade com translação para a posição do chunk no mundo
        float[] model = {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            chunkX * (float) SIZE, 0, chunkZ * (float) SIZE, 1  // translação
        };
        shader.setMatrix4("uModel", model);

        renderer.render();
    }

    public void markDirty() { dirty = true; }
    public int  getChunkX() { return chunkX; }
    public int  getChunkZ() { return chunkZ; }
}