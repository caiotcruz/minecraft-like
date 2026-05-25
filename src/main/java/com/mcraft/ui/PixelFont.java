package com.mcraft.ui;

public final class PixelFont {

    private PixelFont() {}

    private static final int[][] DIGITS = {
        {7,5,5,5,7}, 
        {2,6,2,2,7}, 
        {7,1,3,6,7}, 
        {7,1,3,1,7}, 
        {5,5,7,1,1},
        {7,4,7,1,7}, 
        {7,4,7,5,7},
        {7,1,1,2,2}, 
        {7,5,7,5,7},
        {7,5,7,1,7}, 
    };

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

    public static void drawInt(RectBatch batch, int x, int y,
                                int ps, int number,
                                float r, float g, float b, float a) {
        String s = Integer.toString(number);
        int cx = x;
        for (char c : s.toCharArray()) {
            int d = c - '0';
            if (d >= 0 && d <= 9) drawDigit(batch, cx, y, ps, d, r, g, b, a);
            cx += (W + GAP) * ps;
        }
    }

    private static void drawDigit(RectBatch batch, int x, int y,
                                   int ps, int digit,
                                   float r, float g, float b, float a) {
        int[] rows = DIGITS[digit];
        for (int row = 0; row < H; row++) {
            for (int col = 0; col < W; col++) {
                if (((rows[row] >> (W - 1 - col)) & 1) == 1) {
                    batch.rect(x + col * ps, y + row * ps, ps, ps, r, g, b, a);
                }
            }
        }
    }
}