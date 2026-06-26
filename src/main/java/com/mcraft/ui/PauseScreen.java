package com.mcraft.ui;

import com.mcraft.render.Shader;
import com.mcraft.render.TextureAtlas;

import java.util.ArrayList;
import java.util.List;

public class PauseScreen extends Screen2D {

    public enum Choice { NONE, RESUME, QUIT }

    private Choice choice = Choice.NONE;

    private record Btn(String label, int x, int y, int w, int h, Choice c) {}
    private final List<Btn> buttons = new ArrayList<>();

    private static final int BTN_W = 280, BTN_H = 40, GAP = 14;

    public PauseScreen(int sw, int sh, Shader shader, TextureAtlas atlas, float[] ortho) {
        super(sw, sh, shader, atlas, ortho);
        rebuild();
    }

    @Override
    protected void onResize() { rebuild(); }

    private void rebuild() {
        buttons.clear();
        String[] labels  = { "RETOMAR", "SAIR" };
        Choice[]  choices = { Choice.RESUME, Choice.QUIT };

        int totalH = labels.length * BTN_H + (labels.length - 1) * GAP;
        int startY = (sh - totalH) / 2;
        int x = (sw - BTN_W) / 2;

        for (int i = 0; i < labels.length; i++) {
            buttons.add(new Btn(labels[i], x, startY + i*(BTN_H+GAP), BTN_W, BTN_H, choices[i]));
        }
    }

    public Choice consumeChoice() {
        Choice c = choice;
        choice = Choice.NONE;
        return c;
    }

    @Override
    public void render() {
        beginRender();
        beginBatch();

        addRect(0, 0, sw, sh, 0f, 0f, 0f, 0.55f);

        String title = "PAUSADO";
        int ps = 5;
        int tw = PixelFont.measureStringWidth(title, ps);
        PixelFont.drawStringShadow(this::addRect, (sw-tw)/2, sh/2 - 120, ps, title, 1f, 1f, 1f);

        for (Btn b : buttons) {
            boolean hovered = hit(mouseX, mouseY, b.x(), b.y(), b.w(), b.h());
            float bg = hovered ? 0.32f : 0.18f;
            addRect(b.x(), b.y(), b.w(), b.h(), bg, bg, bg+0.05f, 0.95f);
            addRect(b.x(), b.y(),             b.w(), 2, 0.7f, 0.7f, 0.8f, 1f);
            addRect(b.x(), b.y()+b.h()-2,     b.w(), 2, 0.7f, 0.7f, 0.8f, 1f);

            int bps = 2;
            int btw = PixelFont.measureStringWidth(b.label(), bps);
            PixelFont.drawStringShadow(this::addRect,
                b.x() + (b.w()-btw)/2, b.y() + (b.h()-5*bps)/2, bps, b.label(), 1f, 1f, 1f);
        }

        flushBatch(false);
        endRender();
    }

    @Override
    public boolean onClick(int mx, int my) {
        for (Btn b : buttons) {
            if (hit(mx, my, b.x(), b.y(), b.w(), b.h())) { choice = b.c(); return true; }
        }
        return false;
    }

    @Override
    public void onClose() {}
}