package com.alexsobiek.pluginlib;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class TimeBasedViolationManager {
    private final HashMap<UUID, List<Long>> violations;
    private final AbstractPlugin plugin;
    private final long period;
    private final int kickThreshold;
    private final TextComponent kickReason;
    private final Consumer<Player> violationConsumer;

    public TimeBasedViolationManager(AbstractPlugin plugin, long period, int kickThreshold, TextComponent kickReason, Consumer<Player> onViolation) {
        this.violations = plugin.getPlayerCache().create(HashMap.class);
        this.plugin = plugin;
        this.period = period;
        this.kickThreshold = kickThreshold;
        this.kickReason = kickReason;
        this.violationConsumer = onViolation;
    }

    public void logAction(Player player) {
        final long time = System.currentTimeMillis();
        final UUID id = player.getUniqueId();
        final List<Long> vls = violations.getOrDefault(id, new ArrayList<>());

        vls.removeIf(l -> l < time - period * (kickThreshold - 1));
        vls.add(time);

        int size = vls.size();
        if (size > 1) { // at least 2 entries
            if (Math.abs(vls.get(size - 1) - vls.get(size - 2)) < period) { // if the last two violations were within the period
                plugin.getViolationManager().increment(player, kickReason);
                if (player.isOnline()) violationConsumer.accept(player);
            }

            if (player.isOnline() && vls.size() >= kickThreshold) {
                plugin.executeMainThread(() -> {
                    player.kick(kickReason);
                    return null;
                });
            }
        }
        if (player.isOnline()) violations.put(id, vls);
    }
}