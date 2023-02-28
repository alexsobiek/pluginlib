package com.alexsobiek.pluginlib.test;

import com.alexsobiek.pluginlib.AbstractPlugin;
import com.alexsobiek.pluginlib.adapter.EventAdapter;

public class TestPlugin extends AbstractPlugin {
    @Override
    public void enable() {
        TestEventAdapter adapter = EventAdapter.register(TestEventAdapter.class, this);
        logger().info("Registered adapter: {}", adapter);
        adapter.disable();
    }

    @Override
    public void disable() {

    }
}
