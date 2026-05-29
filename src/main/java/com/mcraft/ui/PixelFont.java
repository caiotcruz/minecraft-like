package com.mcraft.ui;

import java.util.Map;

public final class PixelFont {

    private PixelFont() {}

    private static final Map<Character, int[]> GLYPHS = Map.ofEntries(

        Map.entry('0', new int[]{7,5,5,5,7}),
        Map.entry('1', new int[]{2,6,2,2,7}),
        Map.entry('2', new int[]{7,1,3,6,7}),
        Map.entry('3', new int[]{7,1,3,1,7}),
        Map.entry('4', new int[]{5,5,7,1,1}),
        Map.entry('5', new int[]{7,4,7,1,7}),
        Map.entry('6', new int[]{7,4,7,5,7}),
        Map.entry('7', new int[]{7,1,1,2,2}),
        Map.entry('8', new int[]{7,5,7,5,7}),
        Map.entry('9', new int[]{7,5,7,1,7}),

        Map.entry('A', new int[]{2,5,7,5,5}),
        Map.entry('B', new int[]{6,5,6,5,6}),
        Map.entry('C', new int[]{3,4,4,4,3}),
        Map.entry('D', new int[]{6,5,5,5,6}),
        Map.entry('E', new int[]{7,4,6,4,7}),
        Map.entry('F', new int[]{7,4,6,4,4}),
        Map.entry('G', new int[]{3,4,5,5,3}),
        Map.entry('H', new int[]{5,5,7,5,5}),
        Map.entry('I', new int[]{7,2,2,2,7}),
        Map.entry('J', new int[]{1,1,1,5,2}),
        Map.entry('K', new int[]{5,5,6,5,5}),
        Map.entry('L', new int[]{4,4,4,4,7}),
        Map.entry('M', new int[]{5,7,7,5,5}),
        Map.entry('N', new int[]{5,7,7,7,5}),
        Map.entry('O', new int[]{2,5,5,5,2}),
        Map.entry('P', new int[]{6,5,6,4,4}),
        Map.entry('Q', new int[]{2,5,5,7,3}),
        Map.entry('R', new int[]{6,5,6,5,5}),
        Map.entry('S', new int[]{3,4,2,1,6}),
        Map.entry('T', new int[]{7,2,2,2,2}),
        Map.entry('U', new int[]{5,5,5,5,7}),
        Map.entry('V', new int[]{5,5,5,5,2}),
        Map.entry('W', new int[]{5,5,7,7,5}),
        Map.entry('X', new int[]{5,5,2,5,5}),
        Map.entry('Y', new int[]{5,5,2,2,2}),
        Map.entry('Z', new int[]{7,1,2,4,7}),

        Map.entry(' ', new int[]{0,0,0,0,0}),
        Map.entry('!', new int[]{2,2,2,0,2}),
        Map.entry('?', new int[]{6,1,2,0,2}),
        Map.entry('.', new int[]{0,0,0,0,2}),
        Map.entry(':', new int[]{0,2,0,2,0}),
        Map.entry('-', new int[]{0,0,7,0,0}),
        Map.entry('/', new int[]{1,1,2,4,4})
    );

    private static final int W   = 3;
    private static final int H   = 5;
    private static final int GAP = 1; 

    @FunctionalInterface
    public interface RectBatch {
        void rect(int x, int y, int w, int h, float r, float g, float b, float a);
    }

    public static int measureWidth(int number) {
        int digits = (number == 0) ? 1 : (int) Math.log10(number) + 1;
        return digits * (W + GAP) - GAP;
    }

    public static void drawIntShadow(RectBatch batch, int x, int y,
                                      int ps, int number,
                                      float r, float g, float b) {
        drawInt(batch, x + ps, y + ps, ps, number, 0f, 0f, 0f, 0.85f);
        drawInt(batch, x,      y,      ps, number, r,   g,   b,   1.0f); 
    }

    public static void drawInt(RectBatch batch, int x, int y, int ps, int number, float r, float g, float b, float a) {
        drawString(batch, x, y, ps, Integer.toString(number), r, g, b, a);
    }

    public static int measureWidth(String text) {
        return text.length() * (W + GAP) - GAP;
    }

    public static void drawStringShadow( RectBatch batch, int x, int y, int ps, String text, float r, float g, float b ) {
        drawString(batch, x + ps, y + ps, ps, text, 0f, 0f, 0f, 0.85f);
        drawString(batch, x, y, ps, text, r, g, b, 1f);
    }

    public static void drawString(RectBatch batch, int x, int y, int ps, String text, float r, float g, float b, float a) {
        int cx = x;

        for (char c : text.toUpperCase().toCharArray()) {

            int[] glyph = GLYPHS.get(c);

            if (glyph == null) {
                glyph = GLYPHS.get('?');
            }

            drawGlyph(batch, cx, y, ps, glyph, r, g, b, a);

            cx += (W + GAP) * ps;
        }
    }

    private static void drawGlyph( RectBatch batch, int x, int y, int ps, int[] rows, float r, float g, float b, float a ) {
        for (int row = 0; row < H; row++) {
            for (int col = 0; col < W; col++) {

                if (((rows[row] >> (W - 1 - col)) & 1) == 1) {

                    batch.rect(
                        x + col * ps,
                        y + row * ps,
                        ps,
                        ps,
                        r, g, b, a
                    );
                }
            }
        }
    }
}