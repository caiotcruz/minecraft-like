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

    public void save(World world, Player player, DayNightCycle dayNight) throws IOException {

        saveWorldMeta(world.getSeed(), player, dayNight);
        boolean chestsSaved = false;

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

        System.out.printf(
            "[Save] %d chunks salvos, %d falhas, baús=%s em '%s'%n",
            saved,
            failed,
            chestsSaved ? "OK" : "ERRO",
            saveDir
        );
    }

    private void saveWorldMeta(long seed, Player player, DayNightCycle dayNight) throws IOException {

        Files.createDirectories(saveDir);

        Path metaPath = saveDir.resolve("world.dat");

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(metaPath)))) {

            out.writeInt(2);

            out.writeLong(seed);

            out.writeFloat(player.getX());
            out.writeFloat(player.getY());
            out.writeFloat(player.getZ());

            out.writeFloat(player.getCamera().getYaw());
            out.writeFloat(player.getCamera().getPitch());

            out.writeFloat(dayNight.getTime());
            out.writeInt(dayNight.getDay());

            int[] items  = player.getInventory().getItems();
            int[] counts = player.getInventory().getCounts();

            out.writeInt(items.length);

            for (int i = 0; i < items.length; i++) {
                out.writeInt(items[i]);
                out.writeInt(counts[i]);
            }

            out.writeInt(player.getInventory().getSelectedSlot());
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

            data.yaw   = in.readFloat();
            data.pitch = in.readFloat();

            if (version >= 2) {

                data.timeOfDay = in.readFloat();
                data.day = in.readInt();

                int size = in.readInt();

                data.inventoryItems  = new int[size];
                data.inventoryCounts = new int[size];

                for (int i = 0; i < size; i++) {
                    data.inventoryItems[i]  = in.readInt();
                    data.inventoryCounts[i] = in.readInt();
                }

                data.selectedSlot = in.readInt();
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