package com.mcraft.world;

import com.mcraft.player.Player;

import java.io.*;
import java.nio.file.*;
import java.util.Optional;

public class WorldIO {

    private final Path saveDir;

    public WorldIO(String worldName) {
        this.saveDir = Paths.get("saves", worldName);
    }

    public void save(World world, Player player, DayNightCycle dayNight) throws IOException {

        Files.createDirectories(saveDir.resolve("chunks"));

        saveWorldMeta(world.getSeed(), player, dayNight);

        world.getLoadedChunks().forEach((key, chunk) -> {
            try {
                saveChunk(chunk);
            } catch (IOException e) {
                System.err.println("[Save] Falha ao salvar chunk " +
                        chunk.getChunkX() + "," + chunk.getChunkZ() + ": " + e.getMessage());
            }
        });

        System.out.printf("[Save] %d chunks salvos em '%s'%n",
                world.getLoadedChunks().size(), saveDir);
    }

    private void saveWorldMeta(long seed, Player player, DayNightCycle dayNight) throws IOException {

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

            int[] items = player.getInventory().getItems();
            int[] counts = player.getInventory().getCounts();

            out.writeInt(items.length);

            for (int i = 0; i < items.length; i++) {
                out.writeInt(items[i]);
                out.writeInt(counts[i]);
            }

            out.writeInt(player.getInventory().getSelectedSlot());
        }
    }

    private void saveChunk(Chunk chunk) throws IOException {
        String name = String.format("c_%d_%d.dat", chunk.getChunkX(), chunk.getChunkZ());
        Path path = saveDir.resolve("chunks").resolve(name);

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(path)))) {
            out.writeInt(chunk.getChunkX());
            out.writeInt(chunk.getChunkZ());
            byte[] blocks = chunk.getRawBlocks(); 
            out.write(blocks);
        }
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
        Path path = saveDir.resolve("chunks").resolve(name);
        if (!Files.exists(path)) return Optional.empty();

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {
            in.readInt(); in.readInt();
            byte[] blocks = new byte[Chunk.SIZE * Chunk.HEIGHT * Chunk.SIZE];
            int read = in.read(blocks);
            if (read != blocks.length) throw new IOException("Chunk truncado");
            return Optional.of(blocks);
        } catch (IOException e) {
            System.err.println("[Load] Chunk corrompido: " + name);
            return Optional.empty();
        }
    }
}