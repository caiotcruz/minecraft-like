package com.mcraft;

import java.io.IOException;

import com.mcraft.core.GameLoop;
import com.mcraft.core.Window;
import com.mcraft.world.SaveData;
import com.mcraft.world.WorldIO;

public class Main {

    public static void main(String[] args) {

        String worldName = args.length > 0 ? args[0] : "mundo1";

        WorldIO io = new WorldIO(worldName);

        long seed;

        float spawnX = 8;
        float spawnY = 90;
        float spawnZ = 8;

        float yaw   = 0f;
        float pitch = 0f;

        float timeOfDay = 0.333f;
        int day = 1;
        SaveData saveData = null;

        if (io.hasSave()) {

            try {

                saveData = io.loadWorldMeta();

                seed = saveData.seed;

                spawnX = saveData.playerX;
                spawnY = saveData.playerY;
                spawnZ = saveData.playerZ;

                yaw   = saveData.yaw;
                pitch = saveData.pitch;

                timeOfDay = saveData.timeOfDay;
                day = saveData.day;

                System.out.println("[Load] Mundo carregado: seed=" + seed);

            } catch (IOException e) {

                seed = System.currentTimeMillis();

                System.err.println(
                    "[Load] Falha ao carregar mundo: " + e.getMessage()
                );
            }

        } else {

            seed = System.currentTimeMillis();

            System.out.println("[New] Novo mundo: seed=" + seed);
        }

        Window window = new Window(
            1920,
            1000,
            "Minecraft Clássico — " + worldName
        );

        window.init();

        GameLoop loop = new GameLoop(
            window,
            io,
            seed,
            spawnX,
            spawnY,
            spawnZ
        );

        if (saveData != null) {

            loop.getPlayer().getInventory().load(
                saveData.inventoryItems,
                saveData.inventoryCounts,
                saveData.selectedSlot
            );

            loop.getDayNight().setTime(timeOfDay);
            loop.getDayNight().setDay(day);

            loop.getPlayer()
                .getCamera()
                .setRotation(yaw, pitch);
            
            loop.getWorld().setChestInventories(io.loadChests());
        }

        loop.run();

        try {

            io.save(
                loop.getWorld(),
                loop.getPlayer(),
                loop.getDayNight()
            );

            System.out.println("[Save] Mundo salvo com sucesso.");

        } catch (IOException e) {

            System.err.println(
                "[Save] Falha ao salvar: " + e.getMessage()
            );
        }
    }
}