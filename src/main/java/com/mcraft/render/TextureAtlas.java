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

    private static int[] tilePixel(int col, int row, int px, int py) {
        int h = (col * 31 + row * 17) ^ (px * 7 + py * 13);
        int n = ((h * 1664525 + 1013904223) >> 16) & 0xF;

        // WATER
        if (col == 13 && row == 12) { 
            boolean wave = ((px + py * 3) % 5 == 0);
            int b = wave ? 210 : 158 + n * 4;
            int g = wave ? 130 : 95  + n * 2;
            return new int[]{ 18, clamp(g), clamp(b), 165 }; 
        }

        //  ROW 0 - BLOCOS BÁSICOS
        
        // Grass
        if (col == 0 && row == 0) {

            int g = 110 + n * 3;
            int r = g - 30 - (n & 3);

            return new int[]{ clamp(r), clamp(g), clamp(r - 20), 255};
        }

        // GRASS_SIDE
        if (col == 0 && row == 15) {

            if (py < 4) {
                int g = 110 + n * 3;
                int r = g - 30 - (n & 3);

                return new int[]{
                    clamp(r),
                    clamp(g),
                    clamp(r - 20),
                    255
                };
            }

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

        // STONE
        if (col == 1 && row == 0) {
            int v = 118 + n * 4;
            return new int[]{ v, v, v + (n & 3), 255 };
        }

        // DIRT
        if (col == 2 && row == 0) {
            int r = 134 + (n > 8 ? 1 : -1) * (n & 7);
            int g = 90  + (n & 5);
            int b = 50  + (n & 3);
            return new int[]{ clamp(r), clamp(g), clamp(b), 255 };
        }

        // PLANKS
        if (col == 3 && row == 0) {
            boolean grain = ((py % 4) == 0) || (px == 0 || px == 15);
            int r = grain ? 145 : 170 + (n & 7);
            int g = grain ? 95  : 115 + (n & 5);
            int b = grain ? 45  : 60  + (n & 3);
            return new int[]{ r, g, b, 255 };
        }

        // BEDROCK (4, 0)
        if (col == 4 && row == 0) {
            int v = 32 + (n < 4 ? 0 : n * 2);
            return new int[]{ v, v, v, 255 };
        }


        // ROW 1 — NATUREZA 
        
        // SAND
        if (col == 0 && row == 1) {
            int r = 218 + (n & 7) - 4;
            int g = 204 + (n & 5) - 2;
            int b = 112 + (n & 3);
            return new int[]{ clamp(r), clamp(g), clamp(b), 255 };
        }

        // WOOD_LOG
        if (col == 1 && row == 1) {
            boolean ring = (px % 5 < 2) || (py % 8 == 0);
            int r = ring ? 88 : 108 + (n & 7);
            int g = ring ? 60 : 76  + (n & 5);
            int b = ring ? 32 : 46  + (n & 3);
            return new int[]{ r, g, b, 255 };
        }

        // LEAVES
        if (col == 2 && row == 1) {
            boolean hole = (n == 0 || n == 15) && (px % 3 == 0) && (py % 3 == 0);
            if (hole) return new int[]{ 0, 0, 0, 0 };
            int g = 78 + n * 3;
            int r = clamp(g - 50);
            return new int[]{ r, g, 20, 255 };
        }

        // CACTUS
        if (col == 3 && row == 1) {
            boolean border = (px == 0 || px == 15 || py == 0 || py == 15);
            boolean spine  = (px % 5 == 0 && py % 3 == 2) || (px % 7 == 3 && py % 5 == 1);
            if (border) return new int[]{ 25, 100, 25, 255 };
            if (spine)  return new int[]{ 200, 210, 180, 255 }; 
            int g = 68 + n * 5;
            return new int[]{ 20, clamp(g), 20, 255 };
        }

        // DENSE_GRASS
        if (col == 4 && row == 1) {
            int g = 62 + n * 7;
            int r = clamp(g - 48 - (n & 5));
            int b = clamp(g - 55);
            return new int[]{ r, g, b, 255 };
        }

        // DENSE_GRASS_SIDE
        if (col == 1 && row == 15) {
            if (py < 5) {
                int hash = ((px * 13 ^ py * 17) * 1664525 + 1013904223) & 0xFF;
                float hf = (hash / 255.0f - 0.5f) * 0.1f;
                int g = clamp((int)((62 + n * 7) * (1 + hf)));
                int r = clamp(g - 48);
                return new int[]{ r, g, clamp(g - 55), 255 };
            }
            
            float t = (py - 5) / 10.0f;
            int hash = ((px * 11 ^ py * 19) * 1664525 + 1013904223) & 0xFF;
            float hf = (hash / 255.0f - 0.5f) * 0.12f;
            
            int g = clamp((int)((55 - t * 15 + hf * 15) + n * 3));
            
            int r = clamp((int)((35 + t * t * 40 + hf * 15) + n * 3));
            
            int b = clamp((int)((20 + t * 15)));
            
            return new int[]{ r, g, b, 255 };
        }


        // ROW 2 - MINÉRIOS
        
        // COAL_ORE 
        if (col == 0 && row == 2) { 
            boolean speck = (px % 5 == 1 && py % 4 == 1) || (px % 7 == 3 && py % 5 == 3) || (px % 6 == 5 && py % 7 == 5);
            if (speck) return new int[]{ 25, 25, 25, 255 }; 
            int v = 118 + n * 4;
            return new int[]{ v, v, v + (n & 3), 255 };
        }

        // IRON_ORE 
        if (col == 1 && row == 2) { 
            boolean speck = (px % 4 == 2 && py % 3 == 1) || (px % 6 == 4 && py % 6 == 4);
            if (speck) return new int[]{ 192, 118, 65, 255 };
            int v = 118 + n * 4;
            return new int[]{ v, v, v + (n & 3), 255 };
        }

        // GOLD_ORE 
        if (col == 2 && row == 2) { 
            boolean speck = (px % 3 == 1 && py % 5 == 2) || (px % 7 == 5 && py % 4 == 1) || (px % 5 == 3 && py % 6 == 5);
            if (speck) return new int[]{ 235, 188, 30, 255 };
            int v = 118 + n * 4;
            return new int[]{ v, v, v + (n & 3), 255 };
        }

        // DIAMOND_ORE
        if (col == 3 && row == 2) {
            boolean speck = (px % 5 == 2 && py % 3 == 1) || (px % 6 == 1 && py % 7 == 4) || (px % 8 == 7 && py % 5 == 2);
            if (speck) return new int[]{ 40, 220, 220, 255 };
            int v = 118 + n * 4;
            return new int[]{ v, v, v + (n & 3), 255 };
        }


        // ROW 3 — BLOCOS UTILITÁRIOS 
        
        // CRAFTING_TABLE 
        if (col == 0 && row == 3) {

            int r = 125 + (n & 7);
            int g = 85  + (n & 5);
            int b = 50  + (n & 3);

            if (px <= 1 || px >= 14 || py <= 1 || py >= 14) {
                return new int[]{70, 45, 20, 255};
            }

            boolean gridArea =
                    px >= 3 && px <= 12 &&
                    py >= 3 && py <= 12;

            if (gridArea) {

                r = 105 + (n & 5);
                g = 70  + (n & 4);
                b = 38  + (n & 3);

                if (px == 6 || px == 9) {
                    return new int[]{55, 35, 18, 255};
                }

                if (py == 6 || py == 9) {
                    return new int[]{55, 35, 18, 255};
                }

                return new int[]{r, g, b, 255};
            }

            return new int[]{r, g, b, 255};
        }

        //CRAFTING_TABLE LATERAL
        if (col == 2 && row == 15) {

            boolean plank =
                py == 5 ||
                py == 10;

            if (plank) {
                return new int[]{90,60,30,255};
            }

            return new int[]{
                125 + (n & 7),
                85  + (n & 5),
                50  + (n & 3),
                255
            };
        }

        // CHEST
        if (col == 1 && row == 3) {
            boolean border = (px == 0 || px == 15 || py == 0 || py == 15 || px == 1 || px == 14);
            boolean latch = (px >= 7 && px <= 8 && py >= 6 && py <= 9);
            if (latch) return new int[]{ 210, 210, 210, 255 };
            int r = border ? 60 : 100 + (n & 7);
            int g = border ? 35 : 60 + (n & 5);
            int b = border ? 15 : 30 + (n & 3);
            return new int[]{ r, g, b, 255 };
        }

        //CHEST SIDES
        if (col == 4 && row == 15){
            boolean border = px == 0 || px == 15 || py == 0 || py == 15;

            int r = border ? 60 : 100 + (n & 7);
            int g = border ? 35 : 60  + (n & 5);
            int b = border ? 15 : 30  + (n & 3);

            return new int[]{r,g,b,255};
        }

        // BED
        if (col == 2 && row == 3) {
            if (py < 8) {
                boolean pillow = (px >= 2 && px <= 13) && (py >= 1 && py <= 4);
                if (pillow) {
                    int v = 225 + (n & 7);
                    return new int[]{ v, v, v - 5, 255 }; 
                }
                
                int r = 175 + (n & 7);
                return new int[]{ r, 35, 35, 255 };
            } 
            
            else {
                int shading = (px % 4 == 0) ? -15 : 0;
                return new int[]{ clamp(160 + shading), clamp(30 + shading), clamp(30 + shading), 255 };
            }
        }

        // BED_SIDE
        if (col == 5 && row == 15) {

            if (py < 10) {
                int shading = (px % 4 == 0) ? -15 : 0;

                return new int[]{
                    clamp(160 + shading),
                    clamp(30 + shading),
                    clamp(30 + shading),
                    255
                };
            }

            int shading = (px % 4 == 0 || py == 10) ? -20 : 0;

            return new int[]{
                clamp(130 + shading),
                clamp(82 + shading),
                clamp(40 + shading),
                255
            };
        }

        //FURNACE
        if (col == 3 && row == 3) { 
            boolean rim    = (px <= 1 || px >= 14 || py <= 1 || py >= 14);
            boolean mouth  = (px >= 3 && px <= 12 && py >= 4 && py <= 12);
            boolean inside = (px >= 5 && px <= 10 && py >= 6 && py <= 10);
            
            if (rim)    return new int[]{ 80, 80, 80, 255 }; 
            if (inside) return new int[]{ 240, 110, 15, 255 }; 
            if (mouth)  return new int[]{ 25, 25, 25, 255 };
            
            int stoneV = 105 + (n & 3) * 3;
            return new int[]{ stoneV, stoneV, stoneV, 255 };
        }

        // FURNACE_SIDE
        if (col == 3 && row == 15) {
            boolean edge = (px <= 1 || px >= 14 || py <= 1 || py >= 14);
            
            if (edge) {
                return new int[]{ 80, 80, 80, 255 };
            }
            
            int stoneV = 100 + (n & 3) * 4;
            return new int[]{ stoneV, stoneV, stoneV + (n & 1) * 2, 255 };
        }

        // ROW 4 — FERRAMENTAS E ARMAS

        // WOODEN_PICKAXE
        if (col == 0 && row == 4) {
            boolean shaft = (px == (15 - py)) && (px >= 2 && px <= 11);
            boolean head = (px + py >= 21 && px + py <= 23) && (px >= 8 && py <= 7);
            if ((px == 14 && py == 8) || (px == 7 && py == 1)) head = true;
            
            boolean outline = (px + py == 20 || px + py == 24 || (px == 6 && py == 1) || (px == 14 && py == 9));
            boolean shaftOutline = (px == (14 - py)) || (px == (16 - py));

            if ((outline && head) || (shaftOutline && px >= 1 && px <= 12)) {
                if (!shaft && !head) return new int[]{ 45, 25, 10, 255 };
            }
            if (head) {
                boolean edgeLight = (px + py == 21); 
                int r = edgeLight ? 195 + (n & 3) * 5 : 150 + (n & 3) * 4;
                return new int[]{ r, clamp(r - 50), clamp(r - 95), 255 };
            }
            if (shaft) return new int[]{ 110, 70, 30, 255 };
            return new int[]{ 0, 0, 0, 0 };
        }

        // WOODEN_AXE
        if (col == 1 && row == 4) {
            boolean shaft = (px == (15 - py)) && (px >= 2 && px <= 12);
            boolean blade = (px >= 9 && px <= 14 && py >= 1 && py <= 6) && (px >= (15 - py));
            if (py == 1 && px == 9) blade = false;
            if (px == 14 && py == 6) blade = false;

            if (blade) {
                boolean cuttingEdge = (px == 14 || py == 1);
                int r = cuttingEdge ? 195 + (n & 3) * 5 : 145 + (n & 3) * 4;
                return new int[]{ r, clamp(r - 45), clamp(r - 90), 255 };
            }
            if (shaft) return new int[]{ 100, 62, 25, 255 };
            
            boolean outline = (px == 8 && py >= 2 && py <= 5) || (py == 7 && px >= 10 && px <= 13);
            if (outline) return new int[]{ 50, 30, 10, 255 };
            return new int[]{ 0, 0, 0, 0 };
        }

        // WOODEN_SHOVEL
        if (col == 2 && row == 4) {
            boolean shaft = (px == (15 - py)) && (px >= 2 && px <= 10);
            boolean head = (px >= 10 && px <= 15 && py >= 0 && py <= 5) && (px + py >= 13);
            if (py == 0 && px < 13) head = false;
            if (px == 15 && py > 2) head = false;
            if (py == 5 || px == 10) head = false;

            if (head) {
                boolean centerLine = (px == (15 - py)) || (px == (16 - py));
                int r = centerLine ? 190 + (n & 3) * 5 : 140 + (n & 3) * 4;
                return new int[]{ r, clamp(r - 45), clamp(r - 90), 255 };
            }
            if (shaft) return new int[]{ 110, 70, 30, 255 };
            if ((px == 9 && py == 5) || (px == 10 && py == 6)) return new int[]{ 45, 25, 10, 255 };
            return new int[]{ 0, 0, 0, 0 };
        }

        // WOODEN_SWORD
        if (col == 3 && row == 4) {
            boolean blade = (px == py || px == py + 1 || px == py - 1) && (px >= 5 && px <= 14 && py >= 5 && py <= 14);
            if (px == 14 && py == 14) blade = true;
            if ((px == 5 && py == 5) || (px == 14 && py == 13) || (px == 13 && py == 14)) blade = true;

            boolean guard = (px + py == 9 || px + py == 10) && (px >= 2 && px <= 7 && py >= 2 && py <= 7) && !blade;
            boolean hilt = (px == py) && (px >= 1 && px <= 3);

            if (blade) {
                boolean edge = (px == py);
                int r = edge ? 190 + (n & 3) * 5 : 145 + (n & 3) * 4;
                return new int[]{ r, clamp(r - 45), clamp(r - 90), 255 };
            }
            if (guard) {
                int r = 100 + (n & 3) * 3;
                return new int[]{ r, clamp(r - 55), clamp(r - 80), 255 };
            }
            if (hilt) return new int[]{ 70, 42, 15, 255 };
            return new int[]{ 0, 0, 0, 0 };
        }

        // STONE_PICKAXE
        if (col == 4 && row == 4) {
            boolean shaft = (px == (15 - py)) && (px >= 2 && px <= 11);
            boolean head = (px + py >= 21 && px + py <= 23) && (px >= 8 && py <= 7);
            if ((px == 14 && py == 8) || (px == 7 && py == 1)) head = true;
            
            boolean outline = (px + py == 20 || px + py == 24 || (px == 6 && py == 1) || (px == 14 && py == 9));
            boolean shaftOutline = (px == (14 - py)) || (px == (16 - py));

            if (outline && head) return new int[]{ 40, 40, 40, 255 };
            if (shaftOutline && px >= 1 && px <= 12 && !shaft && !head) return new int[]{ 45, 25, 10, 255 };

            if (head) {
                boolean edgeLight = (px + py == 21);
                int gray = edgeLight ? 160 + (n & 3) * 6 : 100 + (n & 3) * 5;
                return new int[]{ gray, gray, gray, 255 };
            }
            if (shaft) return new int[]{ 110, 70, 30, 255 };
            return new int[]{ 0, 0, 0, 0 };
        }

        // STONE_AXE
        if (col == 5 && row == 4) {
            boolean shaft = (px == (15 - py)) && (px >= 2 && px <= 12);
            boolean blade = (px >= 9 && px <= 14 && py >= 1 && py <= 6) && (px >= (15 - py));
            if (py == 1 && px == 9) blade = false;
            if (px == 14 && py == 6) blade = false;

            if (blade) {
                boolean cuttingEdge = (px == 14 || py == 1);
                int gray = cuttingEdge ? 160 + (n & 3) * 6 : 100 + (n & 3) * 5;
                return new int[]{ gray, gray, gray, 255 };
            }
            if (shaft) return new int[]{ 100, 62, 25, 255 };
            
            boolean outline = (px == 8 && py >= 2 && py <= 5) || (py == 7 && px >= 10 && px <= 13);
            if (outline) return new int[]{ 40, 40, 40, 255 };
            return new int[]{ 0, 0, 0, 0 };
        }

        // STONE_SHOVEL
        if (col == 6 && row == 4) {
            boolean shaft = (px == (15 - py)) && (px >= 2 && px <= 10);
            boolean head = (px >= 10 && px <= 15 && py >= 0 && py <= 5) && (px + py >= 13);
            if (py == 0 && px < 13) head = false;
            if (px == 15 && py > 2) head = false;
            if (py == 5 || px == 10) head = false;

            if (head) {
                boolean centerLine = (px == (15 - py)) || (px == (16 - py));
                int gray = centerLine ? 155 + (n & 3) * 6 : 100 + (n & 3) * 5;
                return new int[]{ gray, gray, gray, 255 };
            }
            if (shaft) return new int[]{ 110, 70, 30, 255 };
            if ((px == 9 && py == 5) || (px == 10 && py == 6)) return new int[]{ 40, 40, 40, 255 };
            return new int[]{ 0, 0, 0, 0 };
        }

        // STONE_SWORD
        if (col == 7 && row == 4) {
            boolean blade = (px == py || px == py + 1 || px == py - 1) && (px >= 5 && px <= 14 && py >= 5 && py <= 14);
            if (px == 14 && py == 14) blade = true;
            if ((px == 5 && py == 5) || (px == 14 && py == 13) || (px == 13 && py == 14)) blade = true;

            boolean guard = (px + py == 9 || px + py == 10) && (px >= 2 && px <= 7 && py >= 2 && py <= 7) && !blade;
            boolean hilt = (px == py) && (px >= 1 && px <= 3);

            if (blade) {
                boolean edge = (px == py);
                int gray = edge ? 160 + (n & 3) * 6 : 100 + (n & 3) * 5;
                return new int[]{ gray, gray, gray, 255 };
            }
            if (guard) {
                int gray = 75 + (n & 3) * 3;
                return new int[]{ gray, gray, gray, 255 };
            }
            if (hilt) return new int[]{ 70, 42, 15, 255 };
            return new int[]{ 0, 0, 0, 0 };
        }

        // IRON_PICKAXE
        if (col == 8 && row == 4) {
            boolean shaft = (px == (15 - py)) && (px >= 2 && px <= 11);
            boolean head = (px + py >= 21 && px + py <= 23) && (px >= 8 && py <= 7);
            if ((px == 14 && py == 8) || (px == 7 && py == 1)) head = true;
            
            boolean outline = (px + py == 20 || px + py == 24 || (px == 6 && py == 1) || (px == 14 && py == 9));
            boolean shaftOutline = (px == (14 - py)) || (px == (16 - py));

            if (outline && head) return new int[]{ 70, 70, 75, 255 }; 
            if (shaftOutline && px >= 1 && px <= 12 && !shaft && !head) return new int[]{ 45, 25, 10, 255 };

            if (head) {
                boolean edgeLight = (px + py == 21);
                int iron = edgeLight ? 240 + (n & 1) * 15 : 195 + (n & 3) * 8;
                return new int[]{ iron, iron, clamp(iron + 10), 255 };
            }
            if (shaft) return new int[]{ 110, 70, 30, 255 };
            return new int[]{ 0, 0, 0, 0 };
        }

        // IRON_AXE
        if (col == 9 && row == 4) {
            boolean shaft = (px == (15 - py)) && (px >= 2 && px <= 12);
            boolean blade = (px >= 9 && px <= 14 && py >= 1 && py <= 6) && (px >= (15 - py));
            if (py == 1 && px == 9) blade = false;
            if (px == 14 && py == 6) blade = false;

            if (blade) {
                boolean cuttingEdge = (px == 14 || py == 1);
                int iron = cuttingEdge ? 240 + (n & 1) * 15 : 195 + (n & 3) * 8;
                return new int[]{ iron, iron, clamp(iron + 10), 255 };
            }
            if (shaft) return new int[]{ 100, 62, 25, 255 };
            
            boolean outline = (px == 8 && py >= 2 && py <= 5) || (py == 7 && px >= 10 && px <= 13);
            if (outline) return new int[]{ 70, 70, 75, 255 };
            return new int[]{ 0, 0, 0, 0 };
        }

        // IRON_SHOVEL
        if (col == 10 && row == 4) {
            boolean shaft = (px == (15 - py)) && (px >= 2 && px <= 10);
            boolean head = (px >= 10 && px <= 15 && py >= 0 && py <= 5) && (px + py >= 13);
            if (py == 0 && px < 13) head = false;
            if (px == 15 && py > 2) head = false;
            if (py == 5 || px == 10) head = false;

            if (head) {
                boolean centerLine = (px == (15 - py)) || (px == (16 - py));
                int iron = centerLine ? 235 + (n & 1) * 15 : 190 + (n & 3) * 8;
                return new int[]{ iron, iron, clamp(iron + 10), 255 };
            }
            if (shaft) return new int[]{ 110, 70, 30, 255 };
            if ((px == 9 && py == 5) || (px == 10 && py == 6)) return new int[]{ 70, 70, 75, 255 };
            return new int[]{ 0, 0, 0, 0 };
        }

        // IRON_SWORD
        if (col == 11 && row == 4) {
            boolean blade = (px == py || px == py + 1 || px == py - 1) && (px >= 5 && px <= 14 && py >= 5 && py <= 14);
            if (px == 14 && py == 14) blade = true;
            if ((px == 5 && py == 5) || (px == 14 && py == 13) || (px == 13 && py == 14)) blade = true;

            boolean guard = (px + py == 9 || px + py == 10) && (px >= 2 && px <= 7 && py >= 2 && py <= 7) && !blade;
            boolean hilt = (px == py) && (px >= 1 && px <= 3);

            if (blade) {
                boolean edge = (px == py);
                int iron = edge ? 240 + (n & 1) * 15 : 195 + (n & 3) * 8;
                return new int[]{ iron, iron, clamp(iron + 10), 255 };
            }
            if (guard) {
                int ironGuard = 140 + (n & 3) * 6;
                return new int[]{ ironGuard, ironGuard, ironGuard + 5, 255 };
            }
            if (hilt) return new int[]{ 70, 42, 15, 255 };
            return new int[]{ 0, 0, 0, 0 };
        }

        // DIAMOND_PICKAXE
        if (col == 12 && row == 4) {
            boolean shaft = (px == (15 - py)) && (px >= 2 && px <= 11);
            boolean head = (px + py >= 21 && px + py <= 23) && (px >= 8 && py <= 7);
            if ((px == 14 && py == 8) || (px == 7 && py == 1)) head = true;
            
            boolean outline = (px + py == 20 || px + py == 24 || (px == 6 && py == 1) || (px == 14 && py == 9));
            boolean shaftOutline = (px == (14 - py)) || (px == (16 - py));

            if (outline && head) return new int[]{ 10, 75, 85, 255 };
            if (shaftOutline && px >= 1 && px <= 12 && !shaft && !head) return new int[]{ 45, 25, 10, 255 };

            if (head) {
                boolean edgeLight = (px + py == 21);
                int cyan = edgeLight ? 180 + (n & 3) * 10 : 100 + (n & 3) * 12;
                return new int[]{ clamp(cyan - 40), cyan, clamp(cyan + 45), 255 };
            }
            if (shaft) return new int[]{ 110, 70, 30, 255 };
            return new int[]{ 0, 0, 0, 0 };
        }

        // DIAMOND_AXE
        if (col == 13 && row == 4) {
            boolean shaft = (px == (15 - py)) && (px >= 2 && px <= 12);
            boolean blade = (px >= 9 && px <= 14 && py >= 1 && py <= 6) && (px >= (15 - py));
            if (py == 1 && px == 9) blade = false;
            if (px == 14 && py == 6) blade = false;

            if (blade) {
                boolean cuttingEdge = (px == 14 || py == 1);
                int cyan = cuttingEdge ? 180 + (n & 3) * 10 : 100 + (n & 3) * 12;
                return new int[]{ clamp(cyan - 40), cyan, clamp(cyan + 45), 255 };
            }
            if (shaft) return new int[]{ 100, 62, 25, 255 };
            
            boolean outline = (px == 8 && py >= 2 && py <= 5) || (py == 7 && px >= 10 && px <= 13);
            if (outline) return new int[]{ 10, 75, 85, 255 };
            return new int[]{ 0, 0, 0, 0 };
        }

        // DIAMOND_SHOVEL
        if (col == 14 && row == 4) {
            boolean shaft = (px == (15 - py)) && (px >= 2 && px <= 10);
            boolean head = (px >= 10 && px <= 15 && py >= 0 && py <= 5) && (px + py >= 13);
            if (py == 0 && px < 13) head = false;
            if (px == 15 && py > 2) head = false;
            if (py == 5 || px == 10) head = false;

            if (head) {
                boolean centerLine = (px == (15 - py)) || (px == (16 - py));
                int cyan = centerLine ? 175 + (n & 3) * 10 : 95 + (n & 3) * 12;
                return new int[]{ clamp(cyan - 40), cyan, clamp(cyan + 45), 255 };
            }
            if (shaft) return new int[]{ 110, 70, 30, 255 };
            if ((px == 9 && py == 5) || (px == 10 && py == 6)) return new int[]{ 10, 75, 85, 255 };
            return new int[]{ 0, 0, 0, 0 };
        }

        // DIAMOND_SWORD
        if (col == 15 && row == 4) {
            boolean blade = (px == py || px == py + 1 || px == py - 1) && (px >= 5 && px <= 14 && py >= 5 && py <= 14);
            if (px == 14 && py == 14) blade = true;
            if ((px == 5 && py == 5) || (px == 14 && py == 13) || (px == 13 && py == 14)) blade = true;

            boolean guard = (px + py == 9 || px + py == 10) && (px >= 2 && px <= 7 && py >= 2 && py <= 7) && !blade;
            boolean hilt = (px == py) && (px >= 1 && px <= 3);

            if (blade) {
                boolean edge = (px == py);
                int cyan = edge ? 180 + (n & 3) * 10 : 100 + (n & 3) * 12;
                return new int[]{ clamp(cyan - 40), cyan, clamp(cyan + 45), 255 };
            }
            if (guard) {
                int gCyan = 80 + (n & 3) * 8;
                return new int[]{ clamp(gCyan - 30), gCyan, clamp(gCyan + 30), 255 };
            }
            if (hilt) return new int[]{ 70, 42, 15, 255 };
            return new int[]{ 0, 0, 0, 0 };
        }

        // ROW 5 — DROPS E RECURSOS 
        
        // FEATHER
        if (col == 0 && row == 5) { 
            int curve = (py < 6) ? 8 : (py < 11) ? 7 : 6;
            boolean shaft = (px == curve) && py >= 2 && py <= 13;
            
            int spread = (py >= 3 && py <= 11) ? (py < 7 ? 3 : 4) : (py == 2 || py == 12) ? 1 : 0;
            boolean barb = (px >= curve - spread && px <= curve + spread) && (py >= 2 && py <= 13);
            
            if (barb && !shaft) {
                if ((px + py * 2) % 5 == 0 && py > 4) return new int[]{ 0, 0, 0, 0 };
            }

            if (shaft) return new int[]{ 215, 215, 220, 255 };
            if (barb) {
                int v = 240 - (13 - py) * 2 + (n & 3) * 3;
                return new int[]{ clamp(v), clamp(v), clamp(v + 5), 255 };
            }
            
            return new int[]{ 0, 0, 0, 0 };
        }

        // LEATHER
        if (col == 1 && row == 5) { 
            boolean inLeather = (px >= 2 && px <= 13 && py >= 2 && py <= 13);
            if (inLeather) {
                boolean cornerCut = (py == 2 || py == 13) && (px <= 3 || px >= 12);
                boolean sideCut   = (px == 2 || px == 13) && (py <= 3 || py >= 12);
                boolean centerIn  = (py >= 7 && py <= 8) && (px == 2 || px == 13); 
                if (cornerCut || sideCut || centerIn) inLeather = false;
            }

            if (!inLeather) return new int[]{ 0, 0, 0, 0 };

            boolean edge = (px == 3 || px == 12 || py == 3 || py == 13 || (px <= 4 && py <= 4));
            
            int v = 125 + (n & 7) * 4;
            int r = v;
            int g = v - 45;
            int b = v - 85;

            if (edge) {
                r -= 35; g -= 30; b -= 20;
            }

            return new int[]{ clamp(r), clamp(g), clamp(b), 255 };
        }

        // RAW_BEEF
        if (col == 2 && row == 5) {
            boolean inBeef = (py >= 3 && py <= 12 && px >= 2 && px <= 13);
            if (inBeef) {
                if ((py == 3 || py == 12) && (px <= 4 || px >= 11)) inBeef = false;
            }
            if (!inBeef) return new int[]{ 0, 0, 0, 0 };

            boolean fatBorder = (py == 3 && px >= 5) || (px == 13 && py <= 8) || (py == 4 && px >= 10);
            if (fatBorder) {
                int w = 225 + (n & 3) * 5;
                return new int[]{ w, w - 5, w - 20, 255 };
            }

            boolean bone = (px >= 3 && px <= 4 && py >= 10 && py <= 11);
            if (bone) return new int[]{ 235, 235, 220, 255 };

            boolean fiber = (px % 3 == 0);
            int r = fiber ? 150 + (n & 3) * 5 : 185 + (n & 3) * 6;
            int g = fiber ? 25 : 45;
            int b = fiber ? 25 : 35;

            return new int[]{ clamp(r), clamp(g), clamp(b), 255 };
        }

        // COOKED_BEEF
        if (col == 6 && row == 5) {
            boolean inBeef = (py >= 3 && py <= 12 && px >= 2 && px <= 13);
            if (inBeef) {
                if ((py == 3 || py == 12) && (px <= 4 || px >= 11)) inBeef = false;
            }
            if (!inBeef) return new int[]{ 0, 0, 0, 0 };

            boolean fatBorder = (py == 3 && px >= 5) || (px == 13 && py <= 8) || (py == 4 && px >= 10);
            if (fatBorder) {
                int r = 185 + (n & 3) * 4;
                int g = 135 + (n & 3) * 3;
                return new int[]{ clamp(r), clamp(g), 30, 255 };
            }

            boolean bone = (px >= 3 && px <= 4 && py >= 10 && py <= 11);
            if (bone) return new int[]{ 215, 215, 200, 255 };

            boolean fiber = (px % 3 == 0);
            
            int r = fiber ? 90 + (n & 3) * 4  : 135 + (n & 3) * 5;
            int g = fiber ? 40 + (n & 1) * 2  : 65 + (n & 3) * 3;
            int b = fiber ? 15                : 25;

            return new int[]{ clamp(r), clamp(g), clamp(b), 255 };
        }

        // ROTTEN_FLESH
        if (col == 3 && row == 5) {
            boolean inFlesh = (py >= 3 && py <= 12 && px >= 3 && px <= 13);
            if (inFlesh) {
                if ((px * py + n) % 7 == 0) inFlesh = false;
            }
            if (!inFlesh) return new int[]{ 0, 0, 0, 0 };

            boolean necroticVein = ((px + py) % 5 == 0 && px > 4) || (px == 7) || (py == 8 && px <= 10);
            if (necroticVein) {
                return new int[]{ 55 + (n & 3) * 4, 75 + (n & 3) * 3, 35, 255 };
            }

            int r = 95 + (n & 3) * 5;
            int g = 70 + (n & 3) * 3;
            int b = 40;
            
            return new int[]{ clamp(r), clamp(g), clamp(b), 255 };
        }

        // GUNPOWDER
        if (col == 4 && row == 5) {
            int heightOffset = (py - 4);
            boolean inPile = (py >= 4 && py <= 13) && (px >= 8 - heightOffset / 2 && px <= 8 + heightOffset / 2);
            
            if (!inPile) return new int[]{ 0, 0, 0, 0 };

            boolean darkGrain = ((px * 7 + py * 13) % 4 == 0);
            boolean sulfurGrain = ((px * 11 + py * 3) % 7 == 0);
            
            if (darkGrain)   return new int[]{ 40, 40, 40, 255 };
            if (sulfurGrain) return new int[]{ 110, 110, 110, 255 };

            int v = 70 + (n & 3) * 5;
            if (px > 8) v -= 15;

            return new int[]{ clamp(v), clamp(v), clamp(v), 255 };
        }

        // WOOL
        if (col == 5 && row == 5) {
            boolean fiberLight = ((px / 3 + py / 3) % 2 == 0) && ((px + py + (n & 1)) % 3 == 0);
            boolean fiberShadow = ((px / 3 - py / 3) % 2 == 0) && ((px - py) % 4 == 0);

            int base = 230 + (n & 3) * 4; 

            if (fiberLight) {
                base = Math.min(255, base + 15);
            } else if (fiberShadow) {
                base = Math.max(160, base - 35);
            }

            return new int[]{ base, clamp(base - 2), clamp(base - 6), 255 };
        }


        // ROW 6 — BIOMAS FRIOS
        
        // SNOW 
        if (col == 0 && row == 6) {
            int v = 240 + n;
            return new int[]{ clamp(v), clamp(v), 255, 255 };
        }

        // SNOWY_GRASS 
        if (col == 1 && row == 6) { 
            int v = 225 + n * 2;
            return new int[]{ clamp(v), clamp(v), clamp(v + 5), 255 };
        }

        // SNOWY_GRASS_SIDE
        if (col == 6 && row == 15) {

            if (py < 3) {
                int v = 230 + (n & 3);

                return new int[] {
                    clamp(v),
                    clamp(v),
                    clamp(v + 5),
                    255
                };
            }

            if (py < 5) {
                int v = 205 + (n & 3);

                return new int[] {
                    clamp(v),
                    clamp(v),
                    clamp(v + 3),
                    255
                };
            }

            if (py < 6){
                int g = 105 + (n & 3) * 4; 
                int r = g - 35;
                
                if ((px + n) % 4 == 0) {
                    return new int[]{ 190, 190, 195, 255 };
                }

                return new int[] {
                    clamp(r),
                    clamp(g),
                    clamp(r - 15),
                    255
                };
            }

            int d = 85 + n * 2;
            return new int[]{ d, d - 10, d - 20, 255 };
        }

        // ICE
        if (col == 2 && row == 6) { 
            boolean fissure = ((px * 5 + py * 7) % 13 == 0);
            if (fissure) return new int[]{ 160, 190, 230, 220 };
            int v = 195 + n * 3;
            return new int[]{ clamp(v - 10), clamp(v), clamp(v + 15), 210 };
        }

        // MATERIAIS

        // STICK
        if (col == 0 && row == 7) { 
            int diff = px - py;
            boolean shaft = (diff >= -1 && diff <= 1) && (px >= 2 && px <= 13);
            
            if (!shaft) return new int[]{ 0, 0, 0, 0 };

            boolean lightWood = (diff == -1 || (px == py && n > 8));
            if (lightWood) {
                int r = 165 + (n & 3) * 4;
                return new int[]{ r, clamp(r - 45), clamp(r - 90), 255 };
            }

            int r = 110 + (n & 3) * 3;
            if (diff == 1) r -= 30;

            return new int[]{ clamp(r), clamp(r - 50), clamp(r - 80), 255 };
        }

        // COAL
        if (col == 1 && row == 7) {
            boolean inCoal = (py >= 3 && py <= 12 && px >= 3 && px <= 12)
                        && !(py == 3 && (px == 3 || px == 12)) 
                        && !(py == 12 && (px == 3 || px == 12))
                        && !(py == 4 && px == 3);
                        
            if (!inCoal) return new int[]{ 0, 0, 0, 0 };

            boolean ridge = (px == py + 1) || (px == 5 && py >= 4 && py <= 9) || (py == 9 && px >= 4 && px <= 10);
            if (ridge) {
                int v = 45 + (n & 3) * 4;
                return new int[]{ v, v, v + 2, 255 };
            }

            int c = 16 + (n & 3) * 3;
            return new int[]{ c, c, c + 2, 255 };
        }

         // IRON_INGOT
        if (col == 2 && row == 7) {
            int inset = (py < 5) ? 4 : (py > 10) ? 4 : (py == 5 || py == 10) ? 3 : 2;
            boolean inIngot = (py >= 4 && py <= 11) && (px >= inset && px <= (15 - inset));
            
            if (!inIngot) return new int[]{ 0, 0, 0, 0 };

            boolean borderLight = (py == 4) || (px == inset);
            boolean flash       = (px == py + 2) || (px == py + 3);
            
            if (borderLight || flash) {
                int light = 230 + (n & 3) * 6;
                return new int[]{ clamp(light), clamp(light), clamp(light), 255 };
            }

            boolean shadow = (py == 11) || (px == 15 - inset);
            int baseV = 150 + (n & 3) * 5;
            if (shadow) baseV -= 45;

            return new int[]{ clamp(baseV), clamp(baseV - 5), clamp(baseV - 5), 255 };
        }

        // GOLD_INGOT
        if (col == 3 && row == 7) {
            int inset = (py < 5) ? 4 : (py > 10) ? 4 : (py == 5 || py == 10) ? 3 : 2;
            boolean inIngot = (py >= 4 && py <= 11) && (px >= inset && px <= (15 - inset));
            
            if (!inIngot) return new int[]{ 0, 0, 0, 0 };

            boolean borderLight = (py == 4) || (px == inset);
            boolean flash       = (px == py + 2) || (px == py + 3);
            
            if (borderLight || flash) {
                int r = 255;
                int g = 240 + (n & 3) * 4;
                return new int[]{ r, clamp(g), 160, 255 };
            }

            boolean shadow = (py == 11) || (px == 15 - inset);
            
            int r = 220 + (n & 3) * 5;
            int g = 165 + (n & 3) * 4;
            
            if (shadow) {
                r -= 60; 
                g -= 50;
            }

            return new int[]{ clamp(r), clamp(g), 15, 255 };
        }

        // DIAMOND
        if (col == 4 && row == 7) {
            if (py < 2 || py > 13) return new int[]{ 0, 0, 0, 0 };
            
            int widthOffset = (py < 6) ? (6 - py) : (py - 6) / 2;
            int minX = 2 + widthOffset;
            int maxX = 13 - widthOffset;
            
            boolean inGem = (px >= minX && px <= maxX);
            if (!inGem) return new int[]{ 0, 0, 0, 0 };

            boolean shine = (px == py) || (py == 3 && px >= 4 && px <= 11) || (px == 7 || px == 8);
            if (shine) {
                int v = 230 + (n & 3) * 5;
                return new int[]{ clamp(v - 50), clamp(v), clamp(v), 255 };
            }

            int cianoG = 170 + n * 4;
            int cianoB = 210 + n * 3;
            return new int[]{ clamp(cianoG - 110), clamp(cianoG), clamp(cianoB), 255 };
        }

        //ILUMINAÇÃO

        //TORCH
        if (col == 0 && row == 8) {
            int stickWidth = (py >= 12) ? 1 : 2;
            boolean stick = (py >= 5 && py <= 14) && (px >= 8 - stickWidth && px <= 7 + stickWidth);
            
            int flameWidth = (py == 0 || py == 4) ? 1 : (py == 1) ? 2 : 3;
            boolean flame = (py >= 0 && py <= 4) && (px >= 8 - flameWidth && px <= 7 + flameWidth);
            
            if (!stick && !flame) return new int[]{ 0, 0, 0, 0 }; 

            if (flame) {
                boolean core = (py >= 1 && py <= 3) && (px >= 7 && px <= 8);
                if (core) {
                    int yVal = 240 + (n & 3) * 5;
                    return new int[]{ 255, clamp(yVal), 160, 255 };
                }
                
                int r = 245 + (n & 3) * 3;
                int g = 100 + n * 4;
                return new int[]{ clamp(r), clamp(g), 20, 230 }; 
            }

            if (py == 5 || py == 6) {
                int coalV = 45 + (n & 3) * 4;
                return new int[]{ coalV, coalV - 5, coalV - 5, 255 };
            }

            int v = 115 + (n & 3) * 4;
            int r = v;
            int g = (int)(v * 0.68f);
            int b = (int)(v * 0.35f);

            if (px == 8 - stickWidth) {
                r += 25; g += 20;
            } else if (px == 7 + stickWidth) { 
                r -= 30; g -= 20; b -= 10;
            }

            return new int[]{ clamp(r), clamp(g), clamp(b), 255 };
        }


        // FALLBACK
        float hue = (col * 16 + row) / 256.0f;
        int[] rgb = hsvToRgb(hue, 0.6f, 0.7f + n * 0.01f);
        return new int[]{ rgb[0], rgb[1], rgb[2], 255 };
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