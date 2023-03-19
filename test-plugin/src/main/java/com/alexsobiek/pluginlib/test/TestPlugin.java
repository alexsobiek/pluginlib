package com.alexsobiek.pluginlib.test;

import com.alexsobiek.pluginlib.AbstractPlugin;
import com.alexsobiek.pluginlib.adapter.EventAdapter;
import org.bukkit.event.player.PlayerJoinEvent;

public class TestPlugin extends AbstractPlugin {
    @Override
    public void enable() {
        TestEventAdapter adapter = EventAdapter.register(TestEventAdapter.class, this);
        logger().info("Registered adapter: {}", adapter);

        AutoCloseable listener = EventAdapter.listen(this, PlayerJoinEvent.class, event -> {
            event.getPlayer().sendMessage("Hello from the test plugin!");
        });

        getServer().getScheduler().runTaskLater(this, () -> {
            try {
                listener.close();
                System.out.println("Closed listener");
            } catch (Throwable t) {
                logger().error("Failed to close listener", t);
            }
        }, 400L);
    }

    @Override
    public void disable() {

    }
}
