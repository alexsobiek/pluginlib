package com.alexsobiek.pluginlib;

import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

@Setter
public class ViolationManager {
    private final AbstractPlugin plugin;
    private final HashMap<UUID, Integer> violations;
    private int threshold;

    public ViolationManager(AbstractPlugin plugin, int threshold) {
        this.plugin = plugin;
        violations = plugin.getPlayerCache().create(HashMap.class);
        this.threshold = threshold;
    }

    public int incrementAndGet(Player player, TextComponent reason) {
        UUID id = player.getUniqueId();
        int vls = violations.getOrDefault(id, 0) + 1;
        if (vls >= threshold) { // TODO: don't hard code this in
            plugin.getSLF4JLogger().warn("Kicking {}/{} for: {}", player.getPlayerProfile().getName(),
                    id, reason.content());
            plugin.executeMainThread(() -> {
                player.kick(Component.text()
                        .append(Component.text("Exceeded violation limit", NamedTextColor.RED))
                        .append(Component.newline())
                        .append(Component.text("Last violation: ", NamedTextColor.GRAY))
                        .append(reason)
                        .build());
                return null;
            });
        }
        violations.put(id, vls);
        return vls;
    }

    public void increment(Player player, TextComponent reason) {
        incrementAndGet(player, reason);
    }
}