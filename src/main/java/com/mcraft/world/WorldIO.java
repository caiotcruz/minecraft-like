package com.mcraft.world;

import com.mcraft.player.Player;
import com.mcraft.ui.Inventory;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WorldIO {

    private final Path saveDir;
    private final Path chunksDir;

    public WorldIO(String worldName) {

        this.saveDir   = Paths.get("saves", worldName);
        this.chunksDir = saveDir.resolve("chunks");

        try {
            Files.createDirectories(chunksDir);
        } catch (IOException e) {
            throw new RuntimeException(
                "[WorldIO] Não foi possível criar diretórios de save: " + saveDir,
                e
            );
        }
    }

    public void save(World world, Player player, DayNightCycle dayNight, WeatherSystem weatherSystem) throws IOException {

        saveWorldMeta(world.getSeed(), player, dayNight, weatherSystem);
        boolean chestsSaved = false;
        boolean furnacesSaved = false;

        int saved = 0;
        int failed = 0;

        for (Chunk chunk : world.getLoadedChunks().values()) {

            try {
                saveChunk(chunk);
                saved++;

            } catch (IOException e) {

                failed++;

                System.err.printf(
                    "[Save] Falha ao salvar chunk %d,%d -> %s%n",
                    chunk.getChunkX(),
                    chunk.getChunkZ(),
                    e.getMessage()
                );
            }
        }

        try {
            saveChests(world.getChestInventories());
            chestsSaved = true;
        } catch (IOException e) {

            System.err.printf(
                "[Save] Falha ao salvar baús -> %s%n",
                e.getMessage()
            );
        }

        try {
            saveFurnaces(world.getFurnaceStates());
            furnacesSaved = true;
        } catch (IOException e) {

            System.err.printf(
                "[Save] Falha ao salvar fornalhas -> %s%n",
                e.getMessage()
            );
        }

        System.out.printf(
            "[Save] %d chunks salvos, %d falhas, baús=%s em '%s'%n",
            saved,
            failed,
            chestsSaved ? "OK" : "ERRO",
            furnacesSaved ? "OK" : "ERRO",
            saveDir
        );
    }

    private void saveWorldMeta(long seed, Player player, DayNightCycle dayNight, WeatherSystem weatherSystem) throws IOException {

        Files.createDirectories(saveDir);

        Path metaPath = saveDir.resolve("world.dat");

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(metaPath)))) {

                out.writeInt(2);

                out.writeLong(seed);

                out.writeFloat(player.getX());
                out.writeFloat(player.getY());
                out.writeFloat(player.getZ());

                out.writeInt(player.getHealth());
                out.writeFloat(player.getHunger());
                out.writeFloat(player.getAir());

                out.writeFloat(player.getCamera().getYaw());
                out.writeFloat(player.getCamera().getPitch());

                out.writeFloat(dayNight.getTime());
                out.writeInt(dayNight.getDay());

                int[] items  = player.getInventory().getItems();
                int[] counts = player.getInventory().getCounts();
                int[] durabilities = player.getInventory().getDurabilities();

                out.writeInt(items.length);

                for (int i = 0; i < items.length; i++) {
                    out.writeInt(items[i]);
                    out.writeInt(counts[i]);
                    out.writeInt(durabilities[i]);
                }

                out.writeInt(player.getInventory().getSelectedSlot());

                int[] armors = player.getInventory().getArmors();
                int[] armorsDur = player.geInventory().getArmorDurabilities();

                out.writeInt(armors.length);

                for (int i = 0; i < armors.length; i++){
                    out.writeInt(armors[i]);
                    out.writeInt(armorsDur[i]);
                }

                out.writeInt(weatherSystem.getCurrentOrdinal());
                out.writeFloat(weatherSystem.getIntensity());
                out.writeFloat(weatherSystem.getChangeTimer());
            }
    }

    public void saveChunk(Chunk chunk) throws IOException {

        Files.createDirectories(chunksDir);

        String name = String.format(
            "c_%d_%d.dat",
            chunk.getChunkX(),
            chunk.getChunkZ()
        );

        Path path = chunksDir.resolve(name);

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(
                    Files.newOutputStream(
                        path,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                    )
                ))) {

            out.writeInt(chunk.getChunkX());
            out.writeInt(chunk.getChunkZ());

            byte[] blocks = chunk.getRawBlocks();

            out.write(blocks);
        }
    }

    public void saveChests(Map<Long, Inventory> chestInventories) throws IOException {

        Files.createDirectories(saveDir);

        Path path = saveDir.resolve("chests.dat");

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(
                    Files.newOutputStream(
                        path,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                    )
                ))) {

            out.writeInt(chestInventories.size());

            for (Map.Entry<Long, Inventory> entry : chestInventories.entrySet()) {

                out.writeLong(entry.getKey());

                Inventory inv = entry.getValue();

                out.writeInt(Inventory.TOTAL_SLOTS);

                for (int s = 0; s < Inventory.TOTAL_SLOTS; s++) {
                    out.writeInt(inv.getItemId(s));
                    out.writeInt(inv.getItemQty(s));
                }
            }
        }
    }

    public void saveFurnaces(Map<Long, FurnaceState> states) throws IOException {
        if (states.isEmpty()) return;
        Path path = saveDir.resolve("furnaces.dat");
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(path)))) {
            out.writeInt(states.size());
            for (Map.Entry<Long, FurnaceState> e : states.entrySet()) {
                long key = e.getKey();
                out.writeInt((int)( key        & 0x3FFFFFF));
                out.writeInt((int)((key >> 26) & 0x1FF));
                out.writeInt((int)((key >> 35) & 0x3FFFFFF));
                FurnaceState s = e.getValue();
                out.writeInt  (s.inputId);   
                out.writeInt  (s.inputQty);
                out.writeInt  (s.fuelId);    
                out.writeInt  (s.fuelQty);
                out.writeInt  (s.outputId);  
                out.writeInt  (s.outputQty);
                out.writeFloat(s.smeltProgress);
                out.writeFloat(s.fuelRemaining);
            }
        }
    }

    public Map<Long, Inventory> loadChests() {
        Path path = saveDir.resolve("chests.dat");
        Map<Long, Inventory> map = new HashMap<>();

        if (!Files.exists(path)) return map;

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {

            int numChests = in.readInt();

            for (int c = 0; c < numChests; c++) {

                long key = in.readLong();

                int numSlots = in.readInt();

                if (numSlots < 0 || numSlots > Inventory.TOTAL_SLOTS) {
                    throw new IOException(
                        "Quantidade de slots inválida: " + numSlots
                    );
                }

                Inventory inv = new Inventory();

                for (int s = 0; s < numSlots && s < Inventory.TOTAL_SLOTS; s++) {
                    int id  = in.readInt();
                    int qty = in.readInt();

                    if (id != 0) {
                        inv.setSlot(s, id, qty);
                    }
                }

                map.put(key, inv);
            }

        } catch (IOException e) {
            System.err.println("[WorldIO] Erro ao carregar baús: " + e.getMessage());
        }

        return map;
    }

    public Map<Long, FurnaceState> loadFurnaces() {
        Path path = saveDir.resolve("furnaces.dat");
        Map<Long, FurnaceState> map = new HashMap<>();
        if (!Files.exists(path)) return map;
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int wx = in.readInt(), wy = in.readInt(), wz = in.readInt();
                FurnaceState s = new FurnaceState();
                s.inputId      = in.readInt(); s.inputQty      = in.readInt();
                s.fuelId       = in.readInt(); s.fuelQty       = in.readInt();
                s.outputId     = in.readInt(); s.outputQty     = in.readInt();
                s.smeltProgress = in.readFloat();
                s.fuelRemaining = in.readFloat();
                map.put(World.blockKey(wx, wy, wz), s);
            }
        } catch (IOException e) {
            System.err.println("[WorldIO] Erro ao carregar fornalhas: " + e.getMessage());
        }
        return map;
    }

    public boolean hasSave() {
        return Files.exists(saveDir.resolve("world.dat"));
    }

    public SaveData loadWorldMeta() throws IOException {

        Path path = saveDir.resolve("world.dat");

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {

            SaveData data = new SaveData();

            int version = in.readInt();

            data.seed = in.readLong();

            data.playerX = in.readFloat();
            data.playerY = in.readFloat();
            data.playerZ = in.readFloat();

            data.playerHealth = in.readInt();
            data.playerHunger = in.readFloat();
            data.playerAir = in.readFloat();

            data.yaw   = in.readFloat();
            data.pitch = in.readFloat();

            if (version >= 2) {

                data.timeOfDay = in.readFloat();
                data.day = in.readInt();

                int size = in.readInt();

                data.inventoryItems  = new int[size];
                data.inventoryCounts = new int[size];
                data.inventoryDurabilities = new int[size];

                for (int i = 0; i < size; i++) {
                    data.inventoryItems[i]  = in.readInt();
                    data.inventoryCounts[i] = in.readInt();
                    data.inventoryDurabilities[i] = in.readInt();
                }

                data.selectedSlot = in.readInt();

                int armorSize = in.readInt();

                data.armorItems = new int[armorSize];
                data.armorDurabilities = new int[armorSize];

                for (int i = 0; i < armorSize; i++) {
                    data.armorItems[i] = in.readInt();
                    data.armorDurabilities[i] = in.readInt();
                }

                data.weatherType = in.readInt();
                data.weatherIntensity = in.readFloat();
                data.weatherChangeTimer = in.readFloat();
            }
            return data;
        }
    }

    public Optional<byte[]> loadChunkBlocks(int cx, int cz) {

        String name = String.format("c_%d_%d.dat", cx, cz);
        Path path = chunksDir.resolve(name);
        if (!Files.exists(path)) {
            return Optional.empty();
        }

        try (DataInputStream in = new DataInputStream( new BufferedInputStream(Files.newInputStream(path)))) {

            in.readInt();
            in.readInt();

            byte[] blocks = new byte[
                Chunk.SIZE *
                Chunk.HEIGHT *
                Chunk.SIZE
            ];

            int read = in.read(blocks);
            if (read != blocks.length) {
                throw new IOException("Chunk truncado");
            }
            return Optional.of(blocks);

        } catch (IOException e) {
            System.err.printf(
                "[Load] Chunk corrompido %s -> %s%n",
                name,
                e.getMessage()
            );
            return Optional.empty();
        }
    }
}