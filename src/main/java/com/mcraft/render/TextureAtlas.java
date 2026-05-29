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


    public TextureAtlas(String classPath) {

        ByteBuffer raw = loadToBuffer(classPath);

        IntBuffer width    = BufferUtils.createIntBuffer(1);
        IntBuffer height   = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);

        stbi_set_flip_vertically_on_load(true);

        ByteBuffer pixels =
            stbi_load_from_memory(raw, width, height, channels, 4);

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
        final int GRID  = 16;

        ByteBuffer buf =
            BufferUtils.createByteBuffer(ATLAS * ATLAS * 4);

        for (int row = 0; row < GRID; row++) {
            for (int col = 0; col < GRID; col++) {

                for (int py = 0; py < TILE; py++) {
                    for (int px = 0; px < TILE; px++) {

                        int imgX = col * TILE + px;
                        int imgY = row * TILE + py;

                        int idx = (imgY * ATLAS + imgX) * 4;

                        int[] rgba = tilePixel(col, row, px, py);

                        buf.put(idx,     (byte) rgba[0]);
                        buf.put(idx + 1, (byte) rgba[1]);
                        buf.put(idx + 2, (byte) rgba[2]);
                        buf.put(idx + 3, (byte) rgba[3]);
                    }
                }
            }
        }

        return new TextureAtlas(buf, ATLAS, ATLAS);
    }

    private static int[] tilePixel(
        int col,
        int row,
        int px,
        int py
    ) {

        int h =
            (col * 31 + row * 17)
            ^ (px * 7 + py * 13);

        int n =
            ((h * 1664525 + 1013904223) >> 16) & 0xF;


        if (col == 0 && row == 0) {

            int g = 110 + n * 3;
            int r = g - 30 - (n & 3);

            return new int[]{
                clamp(r),
                clamp(g),
                clamp(r - 20),
                255
            };
        }

        if (col == 0 && row == 2) { 
            boolean speck = (px % 5 == 1 && py % 4 == 1)
                        || (px % 7 == 3 && py % 5 == 3)
                        || (px % 6 == 5 && py % 7 == 5);
            if (speck) return new int[]{ 12, 12, 14, 255 };
            int v = 118 + n * 4;
            return new int[]{ v, v, v + (n & 3), 255 };
        }

        if (col == 1 && row == 0) {

            int v = 118 + n * 4;

            return new int[]{
                v,
                v,
                v + (n & 3),
                255
            };
        }

        if (col == 1 && row == 2) { 
            boolean speck = (px % 4 == 2 && py % 3 == 1)
                        || (px % 6 == 4 && py % 6 == 4);
            if (speck) return new int[]{ 192, 118, 65, 255 };  
            int v = 118 + n * 4;
            return new int[]{ v, v, v + (n & 3), 255 };
        }

        if (col == 2 && row == 0) {

            int r = 134 + (n > 8 ? 1 : -1) * (n & 7);
            int g = 90  + (n & 5);
            int b = 50  + (n & 3);

            return new int[]{
                clamp(r),
                clamp(g),
                clamp(b),
                255
            };
        }

        if (col == 2 && row == 2) { 
            boolean speck = (px % 3 == 1 && py % 5 == 2)
                        || (px % 7 == 5 && py % 4 == 1)
                        || (px % 5 == 3 && py % 6 == 5);
            if (speck) return new int[]{ 235, 188, 30, 255 };   
            int v = 118 + n * 4;
            return new int[]{ v, v, v + (n & 3), 255 };
        }

        //Neve
        if (col == 2 && row == 4) {
            int v = 235 + n * 2;
            int b = 240 + n;
            return new int[]{ clamp(v), clamp(v), clamp(b), 255 };
        }

        if (col == 3 && row == 0) {

            if (py < 4) {

                int g = 120 + n * 3;

                return new int[]{
                    clamp(g - 40),
                    clamp(g),
                    clamp(g - 60),
                    255
                };

            } else {
                return tilePixel(2, 0, px, py);
            }
        }

        if (col == 3 && row == 2) {
            boolean speck = (px % 5 == 2 && py % 3 == 1)
                        || (px % 6 == 1 && py % 7 == 4)
                        || (px % 8 == 7 && py % 5 == 2);
            if (speck) return new int[]{ 40, 220, 220, 255 };  
            int v = 118 + n * 4;
            return new int[]{ v, v, v + (n & 3), 255 };
        }

        if (col == 4 && row == 0) {

            boolean grain =
                ((py % 4) == 0)
                || (px == 0 || px == 15);

            int r = grain ? 178 : 197 + (n & 7);
            int g = grain ? 120 : 140 + (n & 5);
            int b = grain ? 60  : 80  + (n & 3);

            return new int[]{ r, g, b, 255 };
        }

        // Pena
        if (col == 5 && row == 2) { 
            boolean shaft = (px == 7 || px == 8) && py >= 2 && py <= 13;
            boolean barb  = Math.abs(px - 7) <= (14 - py) / 2 && py >= 2 && py <= 14;
            if (shaft) return new int[]{ 200, 200, 210, 255 };
            if (barb)  return new int[]{ 235, 235, 245, 200 };
            return new int[]{ 0, 0, 0, 0 };
        }

        //Couro
        if (col == 6 && row == 2) { 
            if (px > 1 && px < 14 && py > 1 && py < 14) {
                int v = 130 + n * 5;
                return new int[]{ v, clamp(v - 40), clamp(v - 80), 255 };
            }
            return new int[]{ 0, 0, 0, 0 };
        }

        //Cacto
        if (col == 6 && row == 4) { 
            boolean border = (px == 0 || px == 15 || py == 0 || py == 15);
            boolean spine  = (px % 5 == 0 && py % 3 == 2) || (px % 7 == 3 && py % 5 == 1);
            if (border) return new int[]{ 25, 100, 25, 255 };
            if (spine)  return new int[]{ 200, 210, 180, 255 }; 
            int g = 68 + n * 5;
            return new int[]{ 20, clamp(g), 20, 255 };
        }

         // Bife Cru
        if (col == 7 && row == 2) {
            boolean dark = ((px + py) % 4 == 0);
            if (dark) return new int[]{ 140, 30, 30, 255 };
            return new int[]{ 195, 60, 50, 255 };
        }

        // Grama com Neve
        if (col == 7 && row == 4) { 
            if (py < 4) {
                int v = 225 + n * 2;
                return new int[]{ clamp(v), clamp(v), clamp(v + 5), 255 };
            }

            if (py > 11) {
                int d = 85 + n * 2;
                return new int[]{ d, d - 10, d - 20, 255 };
            }

            int g = 95 + n * 3;
            return new int[]{ clamp(g - 35), clamp(g), clamp(g - 25), 255 };
        }

        //Carne Podre
        if (col == 8 && row == 2) {
            boolean vein = (px % 5 == 2) || (py % 5 == 3);
            if (vein) return new int[]{ 40, 60, 30, 255 };
            return new int[]{ 80, 100, 50, 255 };
        }

        // Gelo
        if (col == 8 && row == 4) { 
            boolean fissure = ((px * 5 + py * 7) % 13 == 0);
            if (fissure) return new int[]{ 160, 190, 230, 220 };
            int v = 195 + n * 3;
            return new int[]{ clamp(v - 10), clamp(v), clamp(v + 15), 210 };
        }

        //Polvora
        if (col == 9 && row == 2) {
            boolean grain = ((px * 13 + py * 7) % 6 == 0);
            return grain ? new int[]{ 55, 55, 55, 255 } : new int[]{ 35, 35, 35, 255 };
        }

        //Cama
        if (col == 9 && row == 4) { 
            boolean pillow = (px >= 2 && px <= 5 && py >= 2 && py <= 6);
            if (pillow) return new int[]{ 220, 215, 200, 255 }; 
            return new int[]{ 180, 40, 40, 255 };               
        }
                
        if (col == 1 && row == 1) {

            int v = 32 + (n < 4 ? 0 : n * 2);

            return new int[]{ v, v, v, 255 };
        }

        if (col == 2 && row == 1) {

            int r = 218 + (n & 7) - 4;
            int g = 204 + (n & 5) - 2;
            int b = 112 + (n & 3);

            return new int[]{
                clamp(r),
                clamp(g),
                clamp(b),
                255
            };
        }

        if (col == 4 && row == 1) {

            boolean ring =
                (px % 5 < 2)
                || (py % 8 == 0);

            int r = ring ? 88 : 108 + (n & 7);
            int g = ring ? 60 : 76  + (n & 5);
            int b = ring ? 32 : 46  + (n & 3);

            return new int[]{ r, g, b, 255 };
        }

        if (col == 4 && row == 3) {

            boolean hole =
                (n == 0 || n == 15)
                && (px % 3 == 0)
                && (py % 3 == 0);

            if (hole) {
                return new int[]{ 0, 0, 0, 0 };
            }

            int g = 78 + n * 3;
            int r = clamp(g - 50);

            return new int[]{ r, g, 20, 220 };
        }

        // Água
        if (col == 13 && row == 12) { 
            boolean wave = ((px + py * 3) % 5 == 0);
            int b = wave ? 210 : 158 + n * 4;
            int g = wave ? 130 : 95  + n * 2;
            return new int[]{ 18, clamp(g), clamp(b), 165 }; 
        }

        if (col == 11 && row == 2) {

            boolean grid =
                (px % 7 == 0)
                || (py % 7 == 0);

            int r = grid ? 80 : 120 + (n & 7);
            int g = grid ? 50 : 80  + (n & 5);
            int b = grid ? 25 : 42  + (n & 3);

            return new int[]{ r, g, b, 255 };
        }

        float hue =
            (col * 16 + row) / 256.0f;

        int[] rgb =
            hsvToRgb(hue, 0.6f, 0.7f + n * 0.01f);

        return new int[]{
            rgb[0],
            rgb[1],
            rgb[2],
            255
        };
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static int[] hsvToRgb(float h, float s, float v) {

        float r = 0;
        float g = 0;
        float b = 0;

        int i = (int)(h * 6);

        float f = h * 6 - i;

        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);

        switch (i % 6) {

            case 0 -> {
                r = v;
                g = t;
                b = p;
            }

            case 1 -> {
                r = q;
                g = v;
                b = p;
            }

            case 2 -> {
                r = p;
                g = v;
                b = t;
            }

            case 3 -> {
                r = p;
                g = q;
                b = v;
            }

            case 4 -> {
                r = t;
                g = p;
                b = v;
            }

            case 5 -> {
                r = v;
                g = p;
                b = q;
            }
        }

        return new int[]{
            (int)(r * 255),
            (int)(g * 255),
            (int)(b * 255)
        };
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

        try (InputStream is =
                 getClass().getResourceAsStream(classPath)) {

            if (is == null) {
                throw new RuntimeException(
                    "Textura não encontrada no classpath: "
                    + classPath
                );
            }

            byte[] bytes = is.readAllBytes();

            ByteBuffer buffer =
                BufferUtils.createByteBuffer(bytes.length);

            buffer.put(bytes).flip();

            return buffer;

        } catch (IOException e) {

            throw new RuntimeException(
                "Falha ao ler textura: "
                + classPath,
                e
            );
        }
    }
}