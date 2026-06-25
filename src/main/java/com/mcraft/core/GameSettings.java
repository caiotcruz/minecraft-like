package com.mcraft.core;

import java.io.*;
import java.util.Properties;

public class GameSettings {

    public int     renderDistance   = 8;
    public int     masterVolumePct  = 100;
    public float   mouseSensitivity = 1.0f;
    public boolean fullscreen       = false;

    private static final String FILE = "settings.properties";

    public static GameSettings loadOrDefault() {
        GameSettings s = new GameSettings();
        File f = new File(FILE);
        if (!f.exists()) return s;

        try (FileInputStream in = new FileInputStream(f)) {
            Properties p = new Properties();
            p.load(in);
            s.renderDistance   = Integer.parseInt(p.getProperty("renderDistance", "8"));
            s.masterVolumePct  = Integer.parseInt(p.getProperty("masterVolumePct", "100"));
            s.mouseSensitivity = Float.parseFloat (p.getProperty("mouseSensitivity", "1.0"));
            s.fullscreen       = Boolean.parseBoolean(p.getProperty("fullscreen", "false"));
        } catch (Exception e) {
            System.err.println("[Settings] Falha ao carregar, usando padrao: " + e.getMessage());
        }
        return s;
    }

    public void save() {
        try (FileOutputStream out = new FileOutputStream(FILE)) {
            Properties p = new Properties();
            p.setProperty("renderDistance",   String.valueOf(renderDistance));
            p.setProperty("masterVolumePct",  String.valueOf(masterVolumePct));
            p.setProperty("mouseSensitivity", String.valueOf(mouseSensitivity));
            p.setProperty("fullscreen",       String.valueOf(fullscreen));
            p.store(out, "MCraft settings");
        } catch (IOException e) {
            System.err.println("[Settings] Falha ao salvar: " + e.getMessage());
        }
    }
}