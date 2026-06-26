package com.mcraft.ui;

import com.mcraft.render.Shader;
import com.mcraft.render.TextureAtlas;

public class LoadingScreen extends Screen2D {

    private float progress = 0f;

    public LoadingScreen(int sw, int sh, Shader shader, TextureAtlas atlas, float[] ortho) {
        super(sw, sh, shader, atlas, ortho);
    }

    public void setProgress(float p) { this.progress = Math.max(0f, Math.min(1f, p)); }

    @Override
    public void render() {
        beginRender();
        beginBatch();

        addRect(0, 0, sw, sh, 0.07f, 0.07f, 0.10f, 1f);

        String title = "CARREGANDO MUNDO";
        int ps = 4;
        int tw = PixelFont.measureStringWidth(title, ps);
        PixelFont.drawStringShadow(this::addRect, (sw-tw)/2, sh/2 - 60, ps, title, 1f, 1f, 1f);

        int barW = 420, barH = 22;
        int bx = (sw-barW)/2, by = sh/2;

        addRect(bx-2, by-2, barW+4, barH+4, 0.30f, 0.30f, 0.35f, 1f);
        addRect(bx,   by,   barW,   barH,   0.15f, 0.15f, 0.18f, 1f);

        int fillW = (int)(barW * progress);
        addRect(bx, by, Math.max(0, fillW), barH, 0.30f, 0.75f, 0.35f, 1f);

        String pct = ((int)(progress * 100)) + "";
        int pps = 3;
        int pw = PixelFont.measureStringWidth(pct, pps);
        PixelFont.drawStringShadow(this::addRect, (sw-pw)/2, by+barH+14, pps, pct, 0.9f, 0.9f, 0.9f);

        flushBatch(false);
        endRender();
    }

    @Override
    public boolean onClick(int mx, int my) { return false; }

    @Override
    public void onClose() {}
}