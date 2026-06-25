package com.mcraft.render;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.stb.STBImage.*;

public class Texture2D {

    private final int textureId;
    private final int width, height;

    public Texture2D(String classPath) {
        ByteBuffer raw = loadToBuffer(classPath);

        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        IntBuffer comp = BufferUtils.createIntBuffer(1);

        stbi_set_flip_vertically_on_load(true);
        ByteBuffer pixels = stbi_load_from_memory(raw, w, h, comp, 4);

        if (pixels == null) {
            throw new RuntimeException(
                "Falha ao decodificar " + classPath + ": " + stbi_failure_reason());
        }

        width  = w.get(0);
        height = h.get(0);

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                     GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        stbi_image_free(pixels);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getWidth()  { return width; }
    public int getHeight() { return height; }

    public void bind(int slot) {
        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void delete() { glDeleteTextures(textureId); }

    private ByteBuffer loadToBuffer(String classPath) {
        try (InputStream is = getClass().getResourceAsStream(classPath)) {
            if (is == null) throw new RuntimeException("Imagem nao encontrada: " + classPath);
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler imagem: " + classPath, e);
        }
    }
}