package com.alexsobiek.pluginlib;

import co.aikar.commands.BukkitCommandManager;
import com.alexsobiek.nexus.Nexus;
import com.alexsobiek.nexus.inject.NexusInject;
import com.alexsobiek.nexus.lazy.Lazy;
import com.alexsobiek.pluginlib.adapter.Adapter;
import com.alexsobiek.pluginlib.adapter.EventAdapter;
import com.alexsobiek.pluginlib.component.Color;
import com.alexsobiek.pluginlib.concurrent.CompletableBukkitFuture;
import com.alexsobiek.pluginlib.concurrent.LifetimeTickScheduler;
import com.alexsobiek.pluginlib.util.PlayerCacheManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractPlugin extends JavaPlugin implements Listener {
    private final List<Adapter<? extends AbstractPlugin>> adapters = new ArrayList<>();
    private final Lazy<PlayerCacheManager> playerCacheManager = new Lazy<>(PlayerCacheManager::new);
    private final Lazy<Nexus> nexus = new Lazy<>(() -> Nexus.builder().build());
    private final Lazy<NexusInject> nexusInject = new Lazy<>(() -> getNexus().library(NexusInject.buildable()));
    private final Lazy<LifetimeTickScheduler> lifetimeTickScheduler = new Lazy<>(() -> new LifetimeTickScheduler(this));
    private final Lazy<ViolationManager> violationManager = new Lazy<>(() -> new ViolationManager(this, 5));
    private boolean reloadLock;
    private BukkitCommandManager commandManager;
    private TeleportManager teleportManager;


    public boolean isReloading() {
        return reloadLock;
    }


    public Logger logger() {
        return this.getSLF4JLogger();
    }

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

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public <T> Optional<T> getService(Class<T> serviceClass) {
        RegisteredServiceProvider<T> rsp = getServer().getServicesManager().getRegistration(serviceClass);
        if (rsp == null) return Optional.empty();
        else return Optional.of(rsp.getProvider());
    }

    /**
     * Registers a command to the server
     * @Author 254n_m
     * @param name Name of the command
     * @param command CommandExecutor
     */
    public void registerCommand(String name, CommandExecutor command) {
        try {
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            PluginCommand pluginCommand = constructor.newInstance(name, this);
            pluginCommand.setExecutor(command);


            Server server = Bukkit.getServer();

            Method commandMapMethod = server.getClass().getDeclaredMethod("getCommandMap");
            commandMapMethod.setAccessible(true);

            Method registerMethod = commandMapMethod.getReturnType().getDeclaredMethod("register", String.class, Command.class);
            registerMethod.setAccessible(true);

            registerMethod.invoke(commandMapMethod.invoke(server), name, pluginCommand);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected Stream<Adapter<? extends AbstractPlugin>> enabledAdapters() {
        return getAdapters().stream().filter(Adapter::isEnabled);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        getPlayerCache().handleLeave(event.getPlayer().getUniqueId());
    }

    @Override
    public void onEnable() {
        commandManager = new BukkitCommandManager(this);
        teleportManager = EventAdapter.register(TeleportManager.class, this);
        getServer().getPluginManager().registerEvents(this, this);

        registerCommand(String.format("%sreload", getName()), (sender, command, label, args) -> {
            if (sender.hasPermission(String.format("%s.reload", getName()))) {
                try {
                    reload();
                    sender.sendMessage(Component.text(String.format("%s reloaded.", getName()), Color.GREEN));
                } catch (Throwable t) {
                    sender.sendMessage(Component.text(String.format("Error reloading %s. Check console for more details.", getName()), Color.RED));
                    t.printStackTrace();
                }
            } else {
                sender.sendMessage(Component.text("You do not have permission to do that.", Color.RED));
            }
            return true;
        });

        enable();
    }

    @Override
    public void onDisable() {
        enabledAdapters().forEach(Adapter::disable);
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

    public Optional<Integer> getMaxPerm(Set<PermissionAttachmentInfo> perms, String perm) {
        return perms.stream()
                .map(PermissionAttachmentInfo::getPermission)
                .map(s -> Integer.parseInt(s.substring(s.lastIndexOf('.') + 1)))
                .max(Integer::compareTo);
    }

    public Optional<Integer> getMaxPerm(Player player, String perm) {
        return getMaxPerm(player.getEffectivePermissions(), perm);
    }

    public Collection<String> getPermissions(Set<PermissionAttachmentInfo> perms, String prefix) {
        final String p = prefix.endsWith(".") ? prefix : prefix + ".";
        return perms.stream()
                .map(PermissionAttachmentInfo::getPermission)
                .filter(s -> s.startsWith(p))
                .collect(Collectors.toList());
    }

    public Collection<String> getPermissions(Player player, String prefix) {
        return getPermissions(player.getEffectivePermissions(), prefix);
    }

    public void reload() {
        reloadLock = true;
        try {
            reloadConfig();
            enabledAdapters().forEach(Adapter::reload);
        } catch (Throwable t) {
            reloadLock = false; // unlock reload in case there is an exception
            throw t;
        }
        reloadLock = false;
    }

    public abstract void disable();

    public abstract void enable();
}
