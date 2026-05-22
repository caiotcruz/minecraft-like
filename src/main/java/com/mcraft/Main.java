package com.mcraft;

import com.mcraft.core.GameLoop;
import com.mcraft.core.Window;

public class Main {
    public static void main(String[] args) {
        Window window = new Window(1920, 1080, "Minecraft Clássico");
        window.init();

        GameLoop loop = new GameLoop(window);
        loop.run();
    }
}