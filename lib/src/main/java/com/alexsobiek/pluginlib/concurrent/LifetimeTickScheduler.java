package com.alexsobiek.pluginlib.concurrent;

import com.alexsobiek.pluginlib.AbstractPlugin;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Utilizes a DynamicTaskService (which only runs when it has tasks) to perform operations
 * on a player every second until the specified amount of seconds has elapsed
 * <p>
 * This works by using the amount of ticks a player has lived divided by 20
 * which is roughly how many seconds they have lived for.
 */
public class LifetimeTickScheduler {
    private final DynamicTaskService service;
    private final ConcurrentHashMap<UUID, Queue<Task>> taskLists;
    private UUID taskId;
    private boolean active;

    public LifetimeTickScheduler(AbstractPlugin plugin) {
        service = new DynamicTaskService(plugin.getNexus().scheduler(), 1000L);
        taskLists = plugin.getPlayerCache().create(ConcurrentHashMap.class);
    }

    public Task schedule(Player player, int seconds, @Nullable Consumer<TaskStage> intermediaryConsumer, Consumer<TaskStage> completionConsumer) {
        Task entry = Task.from(player, new ConsumerPair(intermediaryConsumer, completionConsumer), seconds);
        UUID uuid = player.getUniqueId();
        Queue<Task> list = taskLists.getOrDefault(uuid, new ConcurrentLinkedQueue<>());
        list.add(entry);
        taskLists.putIfAbsent(uuid, list);
        entry.active = true;

        if (!active) start();

        return entry;
    }

    private void start() {
        taskId = service.addConsumer(this::tick);
        active = true;
    }

    private void stop() {
        service.removeConsumer(taskId);
        active = false;
    }

    private void tick() {
        taskLists.forEach((id, tasks) -> {
            tasks.forEach(task -> {
                if (task.isActive()) {
                    Player player = task.player;
                    int start = task.startTicks;
                    int current = player.getTicksLived();
                    int delta = (current - start) / 20;
                    TaskStage stage = new TaskStage(task, delta);
                    if (delta >= task.targetSeconds) { // should never be greater than, just a safety measure
                        task.consumers.completion.accept(stage);
                        tasks.remove(task);
                    } else {
                        Consumer<TaskStage> intermediary = task.consumers.intermediary;
                        if (intermediary != null) intermediary.accept(stage);
                    }
                } else tasks.remove(task);
                if (tasks.isEmpty()) taskLists.remove(id);
            });
        });

        if (taskLists.isEmpty()) stop();
    }

    public record TaskStage(Task entry, int secondsElapsed) {
    }

    private record ConsumerPair(Consumer<TaskStage> intermediary, Consumer<TaskStage> completion) {
    }

    @Data
    @AllArgsConstructor
    public static class Task {
        private final Player player;
        private final ConsumerPair consumers;
        private int startTicks;
        private int targetSeconds;
        private boolean active;

        public static Task from(Player player, ConsumerPair consumers, int totalTicks) {
            return new Task(player, consumers, player.getTicksLived(), totalTicks, false);
        }

        public void reset() {
            startTicks = player.getTicksLived();
        }

        public void cancel() {
            active = false;
        }
    }
}
