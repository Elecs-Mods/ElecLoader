package nl.elec332.minecraft.loader.impl;

import nl.elec332.minecraft.loader.api.modloader.ModLoadingStage;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

public class DeferredWorkQueue<T> {

    public DeferredWorkQueue() {
        this.workQueue = new ConcurrentHashMap<>();
        Arrays.stream(ModLoadingStage.values()).forEach(stage -> {
            if (stage == ModLoadingStage.PRE_CONSTRUCT) {
                return;
            }
            this.workQueue.put(stage, new ConcurrentLinkedDeque<>());
        });
    }

    private final Map<ModLoadingStage, ConcurrentLinkedDeque<T>> workQueue;

    public void enqueueDeferredWork(ModLoadingStage stage, T t) {
        if (stage == ModLoadingStage.PRE_CONSTRUCT) {
            throw new UnsupportedOperationException();
        }
        if (!this.workQueue.containsKey(stage)) {
            throw new IllegalArgumentException("Invalid stage: " + stage.getName());
        }
        this.workQueue.get(stage).add(t);
    }

    public void processQueue(ModLoadingStage stage, Consumer<T> processor) {
        synchronized (this.workQueue) {
            var runnables = this.workQueue.get(stage);
            if (runnables == null) {
                throw new IllegalArgumentException("Stage already processed: " + stage.getName());
            }
            T entry;
            while ((entry = runnables.poll()) != null) {
                processor.accept(entry);
            }
            if (!runnables.isEmpty()) {
                throw new RuntimeException();
            }
            this.workQueue.remove(stage);
        }
    }

}
