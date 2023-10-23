package com.alexsobiek.pluginlib.test;

import com.alexsobiek.pluginlib.AbstractPlugin;
import com.alexsobiek.pluginlib.adapter.CommandAdapter;
import com.alexsobiek.pluginlib.adapter.EventAdapter;
import org.bukkit.event.player.PlayerJoinEvent;

public class TestPlugin extends AbstractPlugin {
    @Override
    public void enable() {
        TestEventAdapter adapter = EventAdapter.register(TestEventAdapter.class, this);
        logger().info("Registered adapter: {}", adapter);

        CommandAdapter.register(TestCommand.class, this);

        EventAdapter.listen(this, PlayerJoinEvent.class, event -> {
            System.out.println(this.getMaxPerm(event.getPlayer(), "test").orElse(0));
        });
    }

    @Override
    public void disable() {

    }
}
