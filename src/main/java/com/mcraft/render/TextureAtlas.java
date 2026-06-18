package com.mcraft.render;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

import com.mcraft.world.Block;

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

    public TextureAtlas(String classPath) {
        ByteBuffer raw = loadToBuffer(classPath);

        IntBuffer width    = BufferUtils.createIntBuffer(1);
        IntBuffer height   = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);

        stbi_set_flip_vertically_on_load(true);

        ByteBuffer pixels = stbi_load_from_memory(raw, width, height, channels, 4);

        if (pixels == null) {
            throw new RuntimeException(
                "Falha ao decodificar textura: "
                + stbi_failure_reason()
                + " ← " + classPath
            );
        }

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA,
            width.get(0),
            height.get(0),
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            pixels
        );

        stbi_image_free(pixels);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public TextureAtlas(ByteBuffer pixels, int width, int height) {
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA,
            width,
            height,
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            pixels
        );

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public static TextureAtlas generateProcedural() {
        final int ATLAS = 256;
        final int TILE  = 16;

        ByteBuffer atlasBuffer = BufferUtils.createByteBuffer(ATLAS * ATLAS * 4);
        for (int i = 0; i < ATLAS * ATLAS * 4; i++) atlasBuffer.put((byte) 0);
        atlasBuffer.flip();

        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        IntBuffer comp = BufferUtils.createIntBuffer(1);

        stbi_set_flip_vertically_on_load(false);

        for (Block b : Block.values()) {
            if (b == Block.AIR) continue;

            String textureName = b.name.toLowerCase() + ".png";
            float[] tint = getTintForBlock(
                b.name.toLowerCase().substring(b.name.toLowerCase().lastIndexOf('/') + 1)
            );

            copiarParaAtlas( atlasBuffer, "/textures/" + textureName, b.texCol, b.texRow, TILE, ATLAS, w, h, comp, tint );
        }

        String[][] texturasAuxiliares = {
            { "block/grass_side.png"       , "0", "15" },
            { "block/furnace_front.png"    , "1", "15" },
            { "block/crafting_table_side.png", "2", "15" },
            { "block/furnace_side.png"     , "3", "15" },
            { "block/chest_side.png"       , "4", "15" },
            { "block/chest_bottom.png"     , "5", "15" },
            { "block/snowy_grass_side.png" , "6", "15" },
            { "block/wood_log_side.png"    , "7", "15" },
            { "block/cactus_side.png"      , "8", "15" },
        };

        for (String[] aux : texturasAuxiliares) {
            String filename = aux[0];
            String path = "/textures/" + filename;
            int targetCol = Integer.parseInt(aux[1]);
            int targetRow = Integer.parseInt(aux[2]);
            
            float[] tint = getTintForBlock(filename.replace(".png", ""));
            
            copiarParaAtlas(atlasBuffer, path, targetCol, targetRow, TILE, ATLAS, w, h, comp, tint);
        }

        return new TextureAtlas(atlasBuffer, ATLAS, ATLAS);
    }

    private static float[] getTintForBlock(String name) {
        if (name.equals("grass")) {
            return new float[]{ 0.44f, 0.69f, 0.22f }; 
        }
        if (name.equals("leather_chestplate")) {
            return new float[]{ 0.70f, 0.15f, 0.15f }; 
        }
        if (name.equals("leather_helmet")) {
            return new float[]{ 0.40f, 0.25f, 0.15f };
        }
        if (name.equals("leather_leggins")) {
            return new float[]{ 0.40f, 0.25f, 0.15f };
        }
        if (name.equals("leather_boots")) {
            return new float[]{ 0.40f, 0.25f, 0.15f };
        }
        if (name.equals("leaves")) {
            return new float[]{ 0.48f, 0.74f, 0.29f };
        }
        if (name.equals("water")) {
            return new float[]{ 0.22f, 0.43f, 0.84f };
        }

        return new float[]{ 1.0f, 1.0f, 1.0f }; 
    }

    private static void copiarParaAtlas(ByteBuffer atlasBuffer, String path, int col, int row, int tile, int atlasSize, IntBuffer w, IntBuffer h, IntBuffer comp, float[] tint) {
        try {
            ByteBuffer rawFile = loadToBufferFromInstance(path);
            ByteBuffer itemPixels = stbi_load_from_memory(rawFile, w, h, comp, 4);

            if (itemPixels != null) {
                for (int py = 0; py < tile; py++) {
                    for (int px = 0; px < tile; px++) {
                        int atlasX = col * tile + px;
                        int atlasY = row * tile + py;

                        int atlasIdx = (atlasY * atlasSize + atlasX) * 4;
                        int itemIdx  = (py * tile + px) * 4;

                        int r = itemPixels.get(itemIdx)     & 0xFF;
                        int g = itemPixels.get(itemIdx + 1) & 0xFF;
                        int b = itemPixels.get(itemIdx + 2) & 0xFF;
                        byte a = itemPixels.get(itemIdx + 3);

                        byte finalR = (byte) Math.max(0, Math.min(255, (int)(r * tint[0])));
                        byte finalG = (byte) Math.max(0, Math.min(255, (int)(g * tint[1])));
                        byte finalB = (byte) Math.max(0, Math.min(255, (int)(b * tint[2])));

                        atlasBuffer.put(atlasIdx,     finalR);
                        atlasBuffer.put(atlasIdx + 1, finalG);
                        atlasBuffer.put(atlasIdx + 2, finalB);
                        atlasBuffer.put(atlasIdx + 3, a);
                    }
                }
                stbi_image_free(itemPixels);
            }
        } catch (Exception e) {
            System.out.println("[TextureAtlas] Alerta: arquivo opcional não encontrado: " + path);
        }
    }

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

    private ByteBuffer loadToBuffer(String classPath) {
        try (InputStream is = getClass().getResourceAsStream(classPath)) {
            if (is == null) {
                throw new RuntimeException("Textura não encontrada no classpath: " + classPath);
            }
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler textura: " + classPath, e);
        }
    }

    private static ByteBuffer loadToBufferFromInstance(String classPath) {
        try (InputStream is = TextureAtlas.class.getResourceAsStream(classPath)) {
            if (is == null) {
                throw new RuntimeException("Arquivo não encontrado: " + classPath);
            }
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Erro de I/O em: " + classPath, e);
        }
    }
}