package com.alexsobiek.pluginlib.adapter;

import com.alexsobiek.nexus.inject.annotation.Inject;
import com.alexsobiek.pluginlib.AbstractPlugin;
import lombok.Getter;
import org.bukkit.event.*;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class EventAdapter<P extends AbstractPlugin> implements Adapter<P>, Listener {
    private final List<Closeable> closeables = new ArrayList<>();

    @Getter
    private boolean enabled;

    @Inject
    @Getter
    private P plugin;

    public static <P extends AbstractPlugin, T extends EventAdapter<P>> T register(Class<T> adapter, P plugin) {
        return Adapter.create(adapter, plugin, new BaseDependencyProvider(plugin), EventAdapter::afterConstruction);
    }

    private static <P extends AbstractPlugin> void afterConstruction(EventAdapter<P> adapter) {
        adapter.getPlugin().getServer().getPluginManager().registerEvents(adapter, adapter.getPlugin());
        try {
            adapter.enable();
            adapter.enabled = true;
        } catch (Throwable t) {
            adapter.getPlugin().logger().error("Failed to enable adapter {}", adapter.getClass().getSimpleName(), t);
        }
    }

    public static <E extends Event> Closeable listen(AbstractPlugin plugin, Class<E> event, EventPriority priority, boolean ignoreCancelled, Consumer<E> consumer) {
        Listener listener = new Listener() {
        };

        plugin.getServer().getPluginManager().registerEvent(event, listener, priority, (l, e) -> {
            if (e.getClass().equals(event)) consumer.accept(event.cast(e));
        }, plugin, ignoreCancelled);

        return () -> {
            try {
                Method getHandlerListMethod = event.getDeclaredMethod("getHandlerList");
                getHandlerListMethod.setAccessible(true);
                HandlerList handlers = (HandlerList) getHandlerListMethod.invoke(null);
                handlers.unregister(listener);
            } catch (Throwable t) {
                plugin.logger().error("Failed to unregister listener for event {}", event.getSimpleName(), t);
            }
        };
    }

    public static <E extends Event> Closeable listen(AbstractPlugin plugin, Class<E> event, EventPriority priority, Consumer<E> consumer) {
        return listen(plugin, event, priority, false, consumer);
    }

    public static <E extends Event> Closeable listen(AbstractPlugin plugin, Class<E> event, Consumer<E> consumer) {
        return listen(plugin, event, EventPriority.NORMAL, false, consumer);
    }

    protected <T extends Closeable> T closeable(T closeable) {
        closeables.add(closeable);
        return closeable;
    }

    public <E extends Event> void listen(Class<E> event, EventPriority priority, boolean ignoreCancelled, Consumer<E> consumer) {
        closeable(listen(getPlugin(), event, priority, ignoreCancelled, consumer));
    }

    public <E extends Event> void listen(Class<E> event, EventPriority priority, Consumer<E> consumer) {
        closeable(listen(getPlugin(), event, priority, consumer));
    }

    public <E extends Event> void listen(Class<E> event, Consumer<E> consumer) {
        closeable(listen(getPlugin(), event, consumer));
    }

    private void unregisterListeners() {
        for (Method method : this.getClass().getDeclaredMethods()) {
            method.setAccessible(true);
            if (method.isAnnotationPresent(EventHandler.class)) {
                if (method.getParameterCount() >= 1) {
                    Class<?> param = method.getParameters()[0].getType();
                    if (Event.class.isAssignableFrom(param)) {
                        try {
                            Method getHandlerListMethod = param.getDeclaredMethod("getHandlerList");
                            getHandlerListMethod.setAccessible(true);
                            HandlerList handlers = (HandlerList) getHandlerListMethod.invoke(null);
                            handlers.unregister(this);
                        } catch (Throwable e) {
                            getPlugin().logger().error("Failed to unregister listener for {} in adapter {}", param.getSimpleName(), this.getClass().getSimpleName(), e);
                        }
                    }
                }
            }
        }
    }

    public void disable() {
        if (enabled) {
            closeables.forEach(c -> {
                try {
                    c.close();
                } catch (Exception e) {
                    plugin.logger().warn("Failed closing {}", c, e);
                }
            });
            unregisterListeners();
            enabled = false;
        } else
            getPlugin().logger().warn("Tried to disable adapter {} but it was not enabled", this.getClass().getSimpleName());
    }

    public abstract void enable();

    public abstract void reload();
}
