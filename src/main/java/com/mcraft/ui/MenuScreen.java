package com.mcraft.ui;

import com.mcraft.core.GameSettings;
import com.mcraft.core.Input;
import com.mcraft.render.Shader;
import com.mcraft.render.TextureAtlas;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;

public class MenuScreen extends Screen2D {

    public enum SubState { MAIN, SEED_INPUT, LOAD_LIST, SETTINGS }

    public record PlayRequest(String worldName, long seed, boolean isNewWorld) {}

    private SubState    subState    = SubState.MAIN;
    private PlayRequest pendingPlay = null;

    private final GameSettings settings;
    private final StringBuilder seedDigits = new StringBuilder();

    private record MenuButton(String label, int x, int y, int w, int h, Runnable action) {}
    private final List<MenuButton> buttons = new ArrayList<>();

    private static final int BTN_W = 260, BTN_H = 30, BTN_GAP = 10;

    public MenuScreen(int sw, int sh, Shader shader, TextureAtlas atlas,
                       float[] ortho, GameSettings settings) {
        super(sw, sh, shader, atlas, ortho);
        this.settings = settings;
        rebuildButtons();
    }

    public PlayRequest consumePlayRequest() {
        PlayRequest r = pendingPlay;
        pendingPlay = null;
        return r;
    }

    public void update(Input input) {
        if (subState != SubState.SEED_INPUT) return;

        for (int k = GLFW_KEY_0; k <= GLFW_KEY_9; k++) {
            if (input.isKeyJustPressed(k) && seedDigits.length() < 18) {
                seedDigits.append((char) ('0' + (k - GLFW_KEY_0)));
            }
        }
        if (input.isKeyJustPressed(GLFW_KEY_BACKSPACE) && seedDigits.length() > 0) {
            seedDigits.deleteCharAt(seedDigits.length() - 1);
        }
        if (input.isKeyJustPressed(GLFW_KEY_ENTER)) {
            confirmSeed();
        }
    }

    private void rebuildButtons() {
        buttons.clear();
        switch (subState) {
            case MAIN       -> buildMainButtons();
            case SEED_INPUT -> buildSeedButtons();
            case LOAD_LIST  -> buildLoadButtons();
            case SETTINGS   -> buildSettingsButtons();
        }
    }

    private void addCenteredButton(int index, int total, String label, Runnable action) {
        int totalH = total * BTN_H + (total - 1) * BTN_GAP;
        int startY = (sh - totalH) / 2 + 30;
        int x = (sw - BTN_W) / 2;
        int y = startY + index * (BTN_H + BTN_GAP);
        buttons.add(new MenuButton(label, x, y, BTN_W, BTN_H, action));
    }

    private void buildMainButtons() {
        addCenteredButton(0, 5, "NOVO MUNDO", this::startRandomWorld);
        addCenteredButton(1, 5, "NOVO MUNDO COM SEED", () -> {
            subState = SubState.SEED_INPUT;
            seedDigits.setLength(0);
            rebuildButtons();
        });
        addCenteredButton(2, 5, "CARREGAR JOGO", () -> {
            subState = SubState.LOAD_LIST;
            rebuildButtons();
        });
        addCenteredButton(3, 5, "CONFIGURACOES", () -> {
            subState = SubState.SETTINGS;
            rebuildButtons();
        });
        addCenteredButton(4, 5, "SAIR", () -> System.exit(0));
    }

    private void buildSeedButtons() {
        int y = sh/2 + 50;
        buttons.add(new MenuButton("CONFIRMAR", sw/2 - BTN_W - 10, y, BTN_W, BTN_H, this::confirmSeed));
        buttons.add(new MenuButton("VOLTAR",    sw/2 + 10,         y, BTN_W, BTN_H, this::backToMain));
    }

    private void buildLoadButtons() {
        File savesDir = new File("saves");
        File[] worlds = savesDir.listFiles(File::isDirectory);
        int count = (worlds != null) ? worlds.length : 0;

        if (count == 0) {
            buttons.add(new MenuButton("VOLTAR", (sw-BTN_W)/2, sh/2 + 60, BTN_W, BTN_H, this::backToMain));
            return;
        }

        for (int i = 0; i < count; i++) {
            String display  = sanitizeForDisplay(worlds[i].getName());
            String realName = worlds[i].getName();
            addCenteredButton(i, count + 1, display, () -> {
                pendingPlay = new PlayRequest(realName, 0L, false);
            });
        }
        addCenteredButton(count, count + 1, "VOLTAR", this::backToMain);
    }

    private void buildSettingsButtons() {
        int y0 = sh/2 - 140, rowH = 50;

        buttons.add(new MenuButton("DISTANCIA -", sw/2-280, y0, 240, BTN_H,
            () -> settings.renderDistance = Math.max(4, settings.renderDistance - 1)));
        buttons.add(new MenuButton("DISTANCIA +", sw/2+20, y0, 240, BTN_H,
            () -> settings.renderDistance = Math.min(16, settings.renderDistance + 1)));

        buttons.add(new MenuButton("VOLUME -", sw/2-280, y0+rowH, 240, BTN_H,
            () -> settings.masterVolumePct = Math.max(0, settings.masterVolumePct - 10)));
        buttons.add(new MenuButton("VOLUME +", sw/2+20, y0+rowH, 240, BTN_H,
            () -> settings.masterVolumePct = Math.min(100, settings.masterVolumePct + 10)));

        buttons.add(new MenuButton("MOUSE -", sw/2-280, y0+rowH*2, 240, BTN_H,
            () -> settings.mouseSensitivity = Math.max(0.2f, settings.mouseSensitivity - 0.1f)));
        buttons.add(new MenuButton("MOUSE +", sw/2+20, y0+rowH*2, 240, BTN_H,
            () -> settings.mouseSensitivity = Math.min(3.0f, settings.mouseSensitivity + 0.1f)));

        buttons.add(new MenuButton("TELA CHEIA", sw/2-280, y0+rowH*3, 240, BTN_H,
            () -> settings.fullscreen = !settings.fullscreen));

        buttons.add(new MenuButton("VOLTAR", (sw-BTN_W)/2, y0+rowH*5, BTN_W, BTN_H, () -> {
            settings.save();
            backToMain();
        }));
    }

    private void startRandomWorld() {
        long   seed = new Random().nextLong();
        String name = "mundo_" + System.currentTimeMillis();
        pendingPlay = new PlayRequest(name, seed, true);
    }

    private void confirmSeed() {
        long seed = seedDigits.length() > 0
            ? Long.parseLong(seedDigits.toString())
            : new Random().nextLong();
        String name = "mundo_" + System.currentTimeMillis();
        pendingPlay = new PlayRequest(name, seed, true);
    }

    private void backToMain() {
        subState = SubState.MAIN;
        rebuildButtons();
    }

    private static String sanitizeForDisplay(String raw) {
        StringBuilder sb = new StringBuilder();
        for (char c : raw.toCharArray()) {
            char up = Character.toUpperCase(c);
            sb.append(((up >= 'A' && up <= 'Z') || (up >= '0' && up <= '9')) ? up : ' ');
        }
        return sb.toString();
    }

    @Override
    public void render() {
        beginRender();
        shader.use();
        shader.setMatrix4("uProjection", ortho);
        beginBatch();

        addRect(0, 0, sw, sh, 0.10f, 0.10f, 0.10f, 1.0f); 
        addRect(0, 0, sw, sh, 0.0f, 0.0f, 0.0f, 0.35f);

        String title = "MCRAFT";
        int titlePs = 7;
        int titleW  = PixelFont.measureStringWidth(title, titlePs);
        PixelFont.drawStringShadow(this::addRect, (sw-titleW)/2, 50, titlePs, title, 0.98f, 0.80f, 0.20f);

        for (MenuButton b : buttons) {
            boolean hovered = hit(mouseX, mouseY, b.x(), b.y(), b.w(), b.h());
            renderButton(b, hovered);
        }

        if (subState == SubState.SEED_INPUT) renderSeedInputBox();
        if (subState == SubState.SETTINGS)   renderSettingsValues();

        flushBatch(false);
        endRender();
    }

    private void renderButton(MenuButton b, boolean hovered) {
        float bgR = hovered ? 0.45f : 0.35f;
        float bgG = hovered ? 0.45f : 0.35f;
        float bgB = hovered ? 0.65f : 0.35f;
        addRect(b.x(), b.y(), b.w(), b.h(), bgR, bgG, bgB, 1.0f);
        
        addRect(b.x(), b.y(), b.w(), 2, 0.7f, 0.7f, 0.7f, 1f);
        addRect(b.x(), b.y(), 2, b.h(), 0.7f, 0.7f, 0.7f, 1f);
        addRect(b.x(), b.y() + b.h() - 2, b.w(), 2, 0.15f, 0.15f, 0.15f, 1f);
        addRect(b.x() + b.w() - 2, b.y(), 2, b.h(), 0.15f, 0.15f, 0.15f, 1f);

        int ps = 2;
        int textW = PixelFont.measureStringWidth(b.label(), ps);
        int tx = b.x() + (b.w() - textW) / 2;
        int ty = b.y() + (b.h() - 5*ps) / 2;
        
        if (hovered) {
            PixelFont.drawStringShadow(this::addRect, tx, ty, ps, b.label(), 1.0f, 1.0f, 0.4f);
        } else {
            PixelFont.drawStringShadow(this::addRect, tx, ty, ps, b.label(), 0.88f, 0.88f, 0.88f);
        }
    }

    private void renderSeedInputBox() {
        int bw = 380, bh = 44;
        int bx = sw/2 - bw/2, by = sh/2 - 30;
        addRect(bx, by, bw, bh, 0.0f, 0.0f, 0.0f, 1f);
        addRect(bx, by, bw, 2, 0.5f, 0.5f, 0.5f, 1f);
        addRect(bx, by + bh - 2, bw, 2, 0.5f, 0.5f, 0.5f, 1f);
        addRect(bx, by, 2, bh, 0.5f, 0.5f, 0.5f, 1f);
        addRect(bx + bw - 2, by, 2, bh, 0.5f, 0.5f, 0.5f, 1f);

        String shown = seedDigits.length() > 0 ? seedDigits.toString() : "ALEATORIA";
        int ps = 2;
        int tw = PixelFont.measureStringWidth(shown, ps);
        PixelFont.drawStringShadow(this::addRect, bx + (bw-tw)/2, by + (bh - 5*ps)/2, ps, shown, 0.6f, 0.6f, 0.6f);
    }

    private void renderSettingsValues() {
        int y0 = sh/2 - 140, rowH = 50, ps = 2;

        PixelFont.drawStringShadow(this::addRect, sw/2 - 20, y0 + 10,
            ps, String.valueOf(settings.renderDistance), 1f, 1f, 1f);
        PixelFont.drawStringShadow(this::addRect, sw/2 - 20, y0 + rowH + 10,
            ps, String.valueOf(settings.masterVolumePct), 1f, 1f, 1f);
        PixelFont.drawStringShadow(this::addRect, sw/2 - 20, y0 + rowH*2 + 10,
            ps, String.valueOf((int)(settings.mouseSensitivity * 10)), 1f, 1f, 1f);
        PixelFont.drawStringShadow(this::addRect, sw/2 - 20, y0 + rowH*3 + 10,
            ps, settings.fullscreen ? "ON" : "OFF", 1f, 1f, 1f);
    }

    @Override
    public boolean onClick(int mx, int my) {
        for (MenuButton b : buttons) {
            if (hit(mx, my, b.x(), b.y(), b.w(), b.h())) {
                b.action().run();
                rebuildButtons();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClose() {}
}