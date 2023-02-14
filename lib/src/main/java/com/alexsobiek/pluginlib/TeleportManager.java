package com.alexsobiek.pluginlib;

import com.alexsobiek.pluginlib.adapter.EventAdapter;
import com.alexsobiek.pluginlib.component.Color;
import com.alexsobiek.pluginlib.concurrent.CompletableBukkitFuture;
import com.alexsobiek.pluginlib.concurrent.LifetimeTickScheduler;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TeleportManager extends EventAdapter<AbstractPlugin> {
    private final Component teleportFail = Component.text("Could not teleport you to that location", Color.RED);
    private final Component teleportCancel = Component.text("Teleport canceled.", Color.RED);
    private final Component teleportSuccess = Component.text("Teleported!", Color.GREEN);
    private final Component teleportFailCombat = Component.text("You cannot teleport while in combat!", Color.RED);
    private final Component teleportFailLastLocation = Component.text("You do not have a last location to teleport to", Color.RED);
    private final List<Predicate<Player>> teleportConditions = new ArrayList<>();
    private final List<Predicate<Player>> instantTeleportConditions = new ArrayList<>();
    private final HashMap<UUID, LifetimeTickScheduler.Task> teleportTasks = new HashMap<>();
    private HashMap<UUID, Location> lastLocations;
    private LifetimeTickScheduler scheduler;

    @Override
    public void enable() {
        lastLocations = getPlugin().getPlayerCache().create(HashMap.class);
        scheduler = getPlugin().getLifetimeTickScheduler();
    }

    @Override
    public void reload() {
    }

    public void addTeleportCondition(Predicate<Player> condition) {
        teleportConditions.add(condition);
    }

    public void addInstantTeleportCondition(Predicate<Player> condition) {
        instantTeleportConditions.add(condition);
    }

    public void validateLocation(Location location, String msg) {
        if (location.getWorld() == null)
            getPlugin().logger().warn(msg, new NullPointerException("Location#getWorld() is null"));
    }

    Location getLocationFromConfig(ConfigurationSection cs) {
        float x = (float) cs.getDouble("x");
        float y = (float) cs.getDouble("y");
        float z = (float) cs.getDouble("z");
        float yaw = (float) cs.getDouble("yaw");
        float pitch = (float) cs.getDouble("pitch");
        Location loc = new Location(getPlugin().getServer().getWorld(cs.getString("world")), x, y, z, yaw, pitch);
        validateLocation(loc, "Failed to properly get location from config");
        return loc;
    }

    void writeLocationToConfig(ConfigurationSection cs, Location location) {
        cs.set("x", location.getX());
        cs.set("y", location.getY());
        cs.set("z", location.getZ());
        cs.set("yaw", location.getYaw());
        cs.set("pitch", location.getPitch());
        cs.set("world", location.getWorld().getName());
    }

    public void cancelTeleport(Player player) {
        UUID playerId = player.getUniqueId();
        if (teleportTasks.containsKey(playerId)) {
            teleportTasks.remove(playerId).cancel();
            player.sendActionBar(teleportCancel);
        }
    }

    private boolean shouldInstantTP(Player player) {
        if (instantTeleportConditions.isEmpty()) return false;
        for (Predicate<Player> condition : instantTeleportConditions)
            if (!condition.test(player)) return false;
        return true;
    }

    public CompletableFuture<Boolean> instantTeleport(Player player, Location location) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        CompletableBukkitFuture.supplyAsync(() -> {
            try {
                return player.teleportAsync(location);
            } catch (Throwable t) {
                getPlugin().logger().warn("Teleport task failed", t);
                player.sendMessage(teleportFail);
                return null;
            }
        }, getPlugin()).thenAcceptAsync(teleportFuture -> {
            if (teleportFuture != null) teleportFuture.thenAcceptAsync(bl -> {
                if (bl) {
                    player.playSound(Sound.sound(Key.key("minecraft:entity.enderman.teleport"),
                            Sound.Source.AMBIENT, 1.0F, 1.0F));
                    player.sendActionBar(teleportSuccess);
                } else player.sendActionBar(teleportFail);
                future.complete(bl);
            });
        });
        return future;
    }

    public CompletableFuture<Boolean> queueTeleportAction(Player player, int seconds, Consumer<Player> readyConsumer) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        for (Predicate<Player> condition : teleportConditions) {
            if (!condition.test(player)) {
                player.sendMessage(teleportFailCombat);
                future.complete(false);
                return future;
            }
        }

        cancelTeleport(player);
        teleportTasks.put(player.getUniqueId(),
                scheduler.schedule(player, seconds, intermediaryStage -> player.sendActionBar(Component.text()
                        .append(Component.text("You will be teleported in ", Color.BLUE))
                        .append(Component.text(String.format("%d seconds",
                                seconds - intermediaryStage.secondsElapsed()), Color.GREEN))
                        .append(Component.text(". Don't move!", Color.BLUE))
                        .build()), completionStage -> {
                    teleportTasks.remove(player.getUniqueId());
                    readyConsumer.accept(player);
                    future.complete(true);
                }));
        return future;
    }

    public CompletableFuture<Boolean> queueTeleport(Player player, int seconds, Location location) {
        return queueTeleportAction(player, seconds, p -> instantTeleport(p, location));
    }

    public void teleport(Player player, Location location, int seconds) {
        if (location != null)
            if (seconds < 1 || shouldInstantTP(player)) instantTeleport(player, location);
            else queueTeleport(player, seconds, location);
        else player.sendMessage(teleportFail);
    }

    public void teleport(Player player, ConfigurationSection config, int seconds) {
        Location location = getLocationFromConfig(config);
        if (location != null) teleport(player, location, seconds);
        else player.sendMessage(teleportFail);
    }


    public void teleport(Player p1, Player p2, int seconds) {
        if (seconds < 1 || shouldInstantTP(p1)) instantTeleport(p1, p2.getLocation());
        else queueTeleportAction(p1, seconds, p -> {
            if (p2.isOnline()) instantTeleport(p1, p2.getLocation());
            else p1.sendMessage(teleportFail);
        });
    }

    public void teleportToLastLocation(Player player, int seconds) {
        UUID id = player.getUniqueId();
        if (lastLocations.containsKey(id)) teleport(player, lastLocations.get(id), seconds);
        else player.sendMessage(teleportFailLastLocation);
    }

    public void storeLastLocation(Player player, Location location) {
        lastLocations.put(player.getUniqueId(), location);
    }

    public void storeLastLocation(Player player) {
        storeLastLocation(player, player.getLocation());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().distance(event.getTo()) > 0.1) // Cancel teleport on move (with some tolerance)
            cancelTeleport(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (teleportTasks.containsKey(id)) teleportTasks.get(id).cancel();
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.COMMAND || cause == PlayerTeleportEvent.TeleportCause.PLUGIN)
            storeLastLocation(event.getPlayer(), event.getFrom());
    }
}
