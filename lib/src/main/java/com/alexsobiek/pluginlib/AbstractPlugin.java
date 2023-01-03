package com.alexsobiek.pluginlib;

import co.aikar.commands.BukkitCommandManager;
import com.alexsobiek.nexus.Nexus;
import com.alexsobiek.nexus.inject.NexusInject;
import com.alexsobiek.nexus.lazy.Lazy;
import com.alexsobiek.pluginlib.adapter.Adapter;
import com.alexsobiek.pluginlib.concurrent.CompletableBukkitFuture;
import com.alexsobiek.pluginlib.concurrent.LifetimeTickScheduler;
import com.alexsobiek.pluginlib.util.PlayerCacheManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class AbstractPlugin extends JavaPlugin implements Listener {
    private final List<Adapter<? extends AbstractPlugin>> adapters = new ArrayList<>();
    private final Lazy<PlayerCacheManager> playerCacheManager = new Lazy<>(PlayerCacheManager::new);
    private final Lazy<Nexus> nexus = new Lazy<>(() -> Nexus.builder().build());
    private final Lazy<NexusInject> nexusInject = new Lazy<>(() -> getNexus().library(NexusInject.buildable()));
    private final Lazy<LifetimeTickScheduler> lifetimeTickScheduler = new Lazy<>(() -> new LifetimeTickScheduler(this));
    private Lazy<ViolationManager> violationManager = new Lazy<>(() -> new ViolationManager(this, 5));
    private BukkitCommandManager commandManager;


    public PlayerCacheManager getPlayerCache() {
        return playerCacheManager.get();
    }

    public Nexus getNexus() {
        return nexus.get();
    }

    public NexusInject getNexusInject() {
        return nexusInject.get();
    }

    public LifetimeTickScheduler getLifetimeTickScheduler() {
        return lifetimeTickScheduler.get();
    }

    public List<Adapter<? extends AbstractPlugin>> getAdapters() {
        return adapters;
    }

    public ViolationManager getViolationManager() {
        return violationManager.get();
    }

    public BukkitCommandManager getCommandManager() {
        return commandManager;
    }

    public <T> Optional<T> getService(Class<T> serviceClass) {
        RegisteredServiceProvider<T> rsp = getServer().getServicesManager().getRegistration(serviceClass);
        if (rsp == null) return Optional.empty();
        else return Optional.of(rsp.getProvider());
    }

    public Logger logger() {
        return this.getSLF4JLogger();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        getPlayerCache().handleLeave(event.getPlayer().getUniqueId());
    }

    @Override
    public void onEnable() {
        commandManager = new BukkitCommandManager(this);
        getAdapters().forEach(Adapter::enable);
        getServer().getPluginManager().registerEvents(this, this);
        enable();
    }

    @Override
    public void onDisable() {
        getAdapters().forEach(Adapter::disable);
        getPlayerCache().clear();
        disable();
    }

    public <T> CompletableFuture<T> executeMainThread(Supplier<T> callable) {
        return CompletableBukkitFuture.supplyAsync(callable, this);
    }

    public CompletableFuture<Void> executeMainThread(Runnable runnable) {
        return CompletableBukkitFuture.runAsync(runnable, this);
    }

    public CompletableFuture<Iterator<? extends Player>> getPlayers() {
        return executeMainThread(() -> getServer().getOnlinePlayers().iterator());
    }

    public void forEachPlayer(Consumer<Player> players) {
        getPlayers().thenAcceptAsync(iter -> {
            iter.forEachRemaining(players);
        });
    }

    public Optional<Player> getPlayer(UUID uuid) {
        return Optional.ofNullable(getServer().getPlayer(uuid));
    }

    public Optional<Player> getPlayer(String name) {
        return Optional.ofNullable(getServer().getPlayer(name));
    }

    public void reload() {
        reloadConfig();
        getAdapters().forEach(Adapter::reload);
    }

    public abstract void enable();



    public abstract void disable();
}
