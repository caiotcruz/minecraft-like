package com.mcraft.render;

import com.mcraft.world.Chunk;
import com.mcraft.world.LightEngine;
import com.mcraft.world.World;

import java.util.concurrent.*;
import java.util.Queue;

public class LightScheduler {

    private static final int MAX_CONCURRENT = 2;

    private final ExecutorService pool;
    private final World            world;
    private final Queue<Chunk>     resultQueue;
    public LightScheduler(World world) {
        this.world       = world;
        this.resultQueue = new ConcurrentLinkedQueue<>();
        this.pool        = Executors.newFixedThreadPool(
            MAX_CONCURRENT,
            r -> { Thread t=new Thread(r,"light-worker"); t.setDaemon(true); return t; }
        );
    }

    public void submit(Chunk chunk) {
        if (chunk.isLightPending()) return;
        chunk.setLightPending(true);

        pool.submit(() -> {
            try {
                LightEngine.calculateChunkLightAsync(chunk, world);
                resultQueue.offer(chunk);
            } catch (Exception e) {
                chunk.setLightPending(false);
                System.err.println("[Light] Erro: " + e.getMessage());
            }
        });
    }

    public void flushResults(int maxPerFrame) {
        Chunk c;
        int processed = 0;
        while (processed < maxPerFrame && (c = resultQueue.poll()) != null) {
            if (c.isLightStagingReady()) {
                c.commitLightStaging();
            }
            processed++;
        }
    }

    public void delete() { pool.shutdownNow(); }
}