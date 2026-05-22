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

    // Formato de vértice: posição(3) + UV(2) + luminosidade(1) = 6 floats = 24 bytes
    private static final int VERTEX_FLOATS = 6;

    private int vao, vbo, ebo;
    private int indexCount; // Número de índices a desenhar

    // ── Definição geométrica das 6 faces ─────────────────────────────────────

    // Normais de cada face para determinar qual bloco vizinho verificar
    // Ordem: Topo(Y+), Base(Y-), Frente(Z+), Trás(Z-), Esquerda(X-), Direita(X+)
    private static final int[][] FACE_DIR = {
        { 0,  1,  0}, { 0, -1,  0},
        { 0,  0,  1}, { 0,  0, -1},
        {-1,  0,  0}, { 1,  0,  0}
    };

    // 4 vértices (posição local) para cada uma das 6 faces de um cubo unitário
    // Cada sub-array: {x, y, z} de cada vértice no sentido anti-horário visto de fora
    private static final float[][][] FACE_VERTS = {
        // Topo   (Y+): normal (0,+1,0)
        {{0,1,0}, {1,1,0}, {1,1,1}, {0,1,1}},
        // Base   (Y-): normal (0,-1,0)
        {{0,0,1}, {1,0,1}, {1,0,0}, {0,0,0}},
        // Frente (Z+): normal (0,0,+1)
        {{0,0,1}, {1,0,1}, {1,1,1}, {0,1,1}},
        // Trás   (Z-): normal (0,0,-1)
        {{1,0,0}, {0,0,0}, {0,1,0}, {1,1,0}},
        // Esq    (X-): normal (-1,0,0)
        {{0,0,0}, {0,0,1}, {0,1,1}, {0,1,0}},
        // Dir    (X+): normal (+1,0,0)
        {{1,0,1}, {1,0,0}, {1,1,0}, {1,1,1}}
    };

    // Fator de luz estático por face (simula iluminação direcional sem calcular normais)
    private static final float[] FACE_LIGHT = {
        1.0f,   // Topo: plena luz
        0.5f,   // Base: sombra forte
        0.8f,   // Frente
        0.8f,   // Trás
        0.65f,  // Esquerda
        0.65f   // Direita
    };

    // ── Construção da Mesh ────────────────────────────────────────────────────

    /**
     * Itera todos os blocos do chunk e adiciona apenas as faces expostas à mesh.
     * Uma face é "exposta" quando o bloco vizinho nessa direção não é sólido.
     *
     * @param chunk O chunk a ser meshed
     * @param world Necessário para consultar vizinhos em outros chunks
     */
    public void buildMesh(Chunk chunk, World world) {
        int cs = Chunk.SIZE;
        int ch = Chunk.HEIGHT;

        // Pré-aloca buffers para o pior caso (cada bloco com 6 faces visíveis)
        int maxQuads = cs * ch * cs * 6;
        FloatBuffer vBuf = BufferUtils.createFloatBuffer(maxQuads * 4 * VERTEX_FLOATS);
        IntBuffer   iBuf = BufferUtils.createIntBuffer (maxQuads * 6);

        int quadCount = 0;

        for (int y = 0; y < ch; y++) {
            for (int z = 0; z < cs; z++) {
                for (int x = 0; x < cs; x++) {

                    Block block = chunk.getBlock(x, y, z);
                    if (!block.solid) continue;

                    // Coordenadas globais deste bloco (para consultar vizinhos)
                    int wx = chunk.getChunkX() * cs + x;
                    int wz = chunk.getChunkZ() * cs + z;

                    for (int face = 0; face < 6; face++) {
                        int[] dir = FACE_DIR[face];

                        // Vizinho na direção da face (pode estar em outro chunk)
                        Block neighbor = world.getBlock(wx + dir[0], y + dir[1], wz + dir[2]);
                        if (neighbor.solid) continue; // Face oculta — descarta

                        float[] uvs   = block.getUVs(face);
                        float   light = FACE_LIGHT[face];

                        // Adiciona os 4 vértices do quad
                        float[][] verts = FACE_VERTS[face];
                        for (int v = 0; v < 4; v++) {
                            vBuf.put(x + verts[v][0]); // Posição local X
                            vBuf.put(y + verts[v][1]); // Posição local Y
                            vBuf.put(z + verts[v][2]); // Posição local Z
                            vBuf.put(uvs[v * 2    ]); // U
                            vBuf.put(uvs[v * 2 + 1]); // V
                            vBuf.put(light);           // Luminosidade
                        }

                        // Dois triângulos por quad: [0,1,2] e [2,3,0]
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

    // ── Upload para a GPU ─────────────────────────────────────────────────────

    private void uploadToGPU(FloatBuffer vBuf, IntBuffer iBuf) {
        // Limpa buffers anteriores se existirem
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
            glDeleteBuffers(ebo);
        }

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        // VBO — dados de vértice
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vBuf, GL_DYNAMIC_DRAW);

        // EBO — índices dos triângulos
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, iBuf, GL_DYNAMIC_DRAW);

        int stride = VERTEX_FLOATS * Float.BYTES; // 24 bytes por vértice

        // Atributo 0: posição (3 floats, offset 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);

        // Atributo 1: UV (2 floats, offset 12 bytes)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 12L);
        glEnableVertexAttribArray(1);

        // Atributo 2: luminosidade (1 float, offset 20 bytes)
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 20L);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
    }

    // ── Renderização ─────────────────────────────────────────────────────────

    public void render() {
        if (indexCount == 0) return;
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    public void delete() {
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
            glDeleteBuffers(ebo);
            vao = vbo = ebo = 0;
        }
    }
}