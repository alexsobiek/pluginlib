package com.alexsobiek.pluginlib.concurrent;

import com.alexsobiek.nexus.Scheduler;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class DynamicTaskService {
    private final Map<UUID, Runnable> tasks = new ConcurrentHashMap<>();
    private final Scheduler scheduler;
    private final long period;
    private boolean active = false;
    private UUID taskId;

    public UUID addConsumer(Runnable runnable) {
        UUID id = UUID.randomUUID();
        tasks.put(id, runnable);
        if (!active) start();
        return id;
    }

    public void removeConsumer(UUID id) {
        tasks.remove(id);
        if (tasks.isEmpty()) shutdown();
    }

    private void shutdown() {
        if (active) {
            scheduler.cancel(taskId);
            active = false;
        }
    }

    private void start() {
        taskId = scheduler.scheduleAtFixedRate(() -> {
            tasks.values().forEach(Runnable::run);
        }, 0, period);
        active = true;
    }
}
