package com.alexsobiek.pluginlib.adapter;

import co.aikar.commands.BaseCommand;
import com.alexsobiek.nexus.inject.annotation.Inject;
import com.alexsobiek.pluginlib.AbstractPlugin;
import lombok.Getter;

@Getter
public abstract class CommandAdapter<P extends AbstractPlugin> extends BaseCommand implements Adapter<P> {
    private boolean enabled = false;
    @Inject
    private P plugin;

    public static <P extends AbstractPlugin, T extends CommandAdapter<P>> T register(Class<T> adapter, P plugin) {
        return Adapter.create(adapter, plugin, new BaseDependencyProvider(plugin), CommandAdapter::afterConstruction);
    }

    private static <P extends AbstractPlugin> void afterConstruction(CommandAdapter<P> adapter) {
        adapter.getPlugin().getCommandManager().registerCommand(adapter);
        try {
            adapter.enable();
            adapter.enabled = true;
        } catch (Throwable t) {
            adapter.getPlugin().logger().error("Failed to enable adapter {}", adapter.getClass().getSimpleName(), t);
        }
    }

    @Override
    public void disable() {
        if (enabled) {
            plugin.getCommandManager().unregisterCommand(this);
            enabled = false;
        } else
            getPlugin().logger().warn("Tried to disable adapter {} but it was not enabled", this.getClass().getSimpleName());
    }

    public abstract void enable();

    public abstract void reload();
}
