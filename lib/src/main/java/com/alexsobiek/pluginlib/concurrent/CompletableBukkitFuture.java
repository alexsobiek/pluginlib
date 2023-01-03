package com.alexsobiek.pluginlib.concurrent;

import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class CompletableBukkitFuture<T> {

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> callable, Plugin plugin) {
        return new CompletableBukkitFuture<T>(plugin).supplyAsync(callable);
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable, Plugin plugin) {
        return new CompletableBukkitFuture<>(plugin).runAsync(runnable);
    }

    private final Plugin plugin;

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                plugin.getLogger().severe("An error occurred while running a task asynchronously");
                t.printStackTrace();
            }
        }, plugin.getServer().getScheduler().getMainThreadExecutor(plugin));
    }

    public CompletableFuture<T> supplyAsync(Supplier<T> callable) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callable.get();
            } catch (Exception e) {
                throw new RuntimeException("Failed future bukkit callable", e);
            }
        }, plugin.getServer().getScheduler().getMainThreadExecutor(plugin));
    }
}