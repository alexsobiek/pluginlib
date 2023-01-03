package com.alexsobiek.pluginlib.adapter;

import com.alexsobiek.nexus.inject.dependency.DependencyProvider;
import com.alexsobiek.pluginlib.AbstractPlugin;

public class BaseDependencyProvider extends DependencyProvider {
    private final AbstractPlugin plugin;
    public BaseDependencyProvider(AbstractPlugin plugin) {
        this.plugin = plugin;
        supply(AbstractPlugin.class, this::getPlugin);
    }

    public AbstractPlugin getPlugin() {
        return plugin;
    }
}