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

        int currentOrdinal = 0;
        float intensity = 0;
        float changeTimer = 0;

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

                currentOrdinal = saveData.weatherType;
                intensity = saveData.weatherIntensity;
                changeTimer = saveData.weatherChangeTimer;

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
                saveData.inventoryDurabilities,
                saveData.selectedSlot,
                saveData.armorItems,
                saveData.armorDurabilities
            );

            loop.getPlayer().setHealth(saveData.playerHealth);
            loop.getPlayer().setHunger(saveData.playerHunger);
            loop.getPlayer().setAir(saveData.playerAir);

            loop.getDayNight().setTime(timeOfDay);
            loop.getDayNight().setDay(day);

            loop.getPlayer()
                .getCamera()
                .setRotation(yaw, pitch);
            
            loop.getWorld().setChestInventories(io.loadChests());
            loop.getWorld().setFurnaceStates(io.loadFurnaces());
            loop.setMobs(
                io.loadMobs(loop.getWorld())
            );  

            loop.getWeatherSystem().restoreState(currentOrdinal, intensity, changeTimer);
        }

        loop.run();

        try {

            io.save(
                loop.getWorld(),
                loop.getPlayer(),
                loop.getDayNight(),
                loop.getWeatherSystem()
            );

            System.out.println("[Save] Mundo salvo com sucesso.");

        } catch (IOException e) {

            System.err.println(
                "[Save] Falha ao salvar: " + e.getMessage()
            );
        }
    }
}