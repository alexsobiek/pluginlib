package com.alexsobiek.pluginlib.adapter;

import com.alexsobiek.nexus.inject.annotation.Inject;
import com.alexsobiek.pluginlib.AbstractPlugin;
import lombok.Getter;
import org.bukkit.event.Listener;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

public abstract class EventAdapter<P extends AbstractPlugin> implements Adapter<P>, Listener {
    public static <P extends AbstractPlugin, T extends EventAdapter<P>> T register(Class<T> adapter, P plugin) {
        return Adapter.create(adapter, plugin, new BaseDependencyProvider(plugin), EventAdapter::afterConstruction);
    }

    private static <P extends AbstractPlugin> void afterConstruction(EventAdapter<P> adapter) {
        adapter.getPlugin().getServer().getPluginManager().registerEvents(adapter, adapter.getPlugin());
        adapter.enable();
    }


    private final List<Closeable> closeables = new ArrayList<>();

    @Inject
    @Getter
    private P plugin;

    public <T extends Closeable> T register(T closeable) {
        closeables.add(closeable);
        return closeable;
    }

    public void disable() {
        closeables.forEach(c -> {
            try {
                c.close();
            } catch(Exception e) {
                plugin.logger().warn("Failed closing {}", c, e);
            }
        });
    }

    public abstract void enable();

    public abstract void reload();
}
