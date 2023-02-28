package com.alexsobiek.pluginlib.test;

import com.alexsobiek.pluginlib.adapter.EventAdapter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

public class TestEventAdapter extends EventAdapter<TestPlugin> {
    @Override
    public void enable() {
        getPlugin().logger().info("TestEventAdapter enabled");
    }

    @Override
    public void reload() {
        getPlugin().logger().info("TestEventAdapter reloaded");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        getPlugin().logger().info("Player joined");
        event.getPlayer().sendMessage("Hello from the test plugin!");
    }
}
