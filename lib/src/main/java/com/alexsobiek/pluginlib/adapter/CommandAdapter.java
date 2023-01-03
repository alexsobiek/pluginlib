package com.alexsobiek.pluginlib.adapter;

import co.aikar.commands.BaseCommand;
import com.alexsobiek.nexus.inject.annotation.Inject;
import com.alexsobiek.pluginlib.AbstractPlugin;
import lombok.Getter;

public abstract class CommandAdapter<P extends AbstractPlugin> extends BaseCommand implements Adapter<P> {
    public static <P extends AbstractPlugin, T extends CommandAdapter<P>> T register(Class<T> adapter, P plugin) {
        return Adapter.create(adapter, plugin, new BaseDependencyProvider(plugin), CommandAdapter::afterConstruction);
    }

    private static <P extends AbstractPlugin> void afterConstruction(CommandAdapter<P> adapter) {
        adapter.getPlugin().getCommandManager().registerCommand(adapter);
        adapter.enable();
    }

    @Inject
    @Getter
    private P plugin;

    public P getPlugin() {
        return plugin;
    }

    @Override
    public void disable() {
        plugin.getCommandManager().unregisterCommand(this);
    }

    public abstract void enable();

    public abstract void reload();
}
