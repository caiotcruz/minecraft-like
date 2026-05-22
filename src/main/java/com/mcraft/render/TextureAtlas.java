package com.mcraft.render;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.stb.STBImage.stbi_set_flip_vertically_on_load;

public class TextureAtlas {

    private final int textureId;

    /**
     * @param classPath Caminho relativo ao classpath, ex: "/textures/terrain.png"
     */
    public TextureAtlas(String classPath) {
        // 1. Lê o arquivo para um ByteBuffer direto (STBImage precisa de buffer nativo)
        ByteBuffer raw = loadToBuffer(classPath);

        // 2. Decodifica com STBImage
        IntBuffer width    = BufferUtils.createIntBuffer(1);
        IntBuffer height   = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);

        // Flip vertical: PNG tem Y=0 no topo, OpenGL quer Y=0 na base
        stbi_set_flip_vertically_on_load(true);

        ByteBuffer pixels = stbi_load_from_memory(raw, width, height, channels, 4);
        if (pixels == null) {
            throw new RuntimeException("Falha ao decodificar textura: "
                + stbi_failure_reason() + " ← " + classPath);
        }

        // 3. Cria textura OpenGL e faz upload
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        // Nearest-neighbor: sem suavização (visual pixelado original)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA,
            width.get(0), height.get(0),
            0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        stbi_image_free(pixels); // Libera memória da CPU após upload para a GPU

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Ativa a textura na unidade de textura `slot` (0, 1, 2…).
     * Depois use shader.setInt("uTexture", slot).
     */
    public void bind(int slot) {
        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void delete() {
        glDeleteTextures(textureId);
    }

    // ── Utilitário ───────────────────────────────────────────────────────────

    private ByteBuffer loadToBuffer(String classPath) {
        try (InputStream is = getClass().getResourceAsStream(classPath)) {
            if (is == null) {
                throw new RuntimeException("Textura não encontrada no classpath: " + classPath);
            }
            byte[] bytes = is.readAllBytes();
            // STBImage exige ByteBuffer direto (fora do heap gerenciado da JVM)
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler textura: " + classPath, e);
        }
    }
}