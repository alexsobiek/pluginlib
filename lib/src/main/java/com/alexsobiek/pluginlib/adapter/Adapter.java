package com.alexsobiek.pluginlib.adapter;

import com.alexsobiek.nexus.inject.dependency.DependencyProvider;
import com.alexsobiek.pluginlib.AbstractPlugin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface Adapter<P extends AbstractPlugin> {
    static <P extends AbstractPlugin, T extends Adapter<P>>
    T create(Class<T> adapter, P plugin, DependencyProvider dependencyProvider, Consumer<T> afterConstruct) {
        try {
            Optional<T> opt = plugin.getNexusInject().construct(adapter, dependencyProvider).get();
            if (opt.isPresent()) {
                T instance = opt.get();

                if (!plugin.isReloading()) plugin.getAdapters().add(instance);
                else CompletableFuture.runAsync(() -> {
                    while (plugin.isReloading()) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    plugin.getAdapters().add(instance);
                    plugin.logger().warn("Attempted to register adapter {} while a reload was in progress.", adapter.getSimpleName());
                });

                afterConstruct.accept(instance);
                return instance;
            }
        } catch (Throwable t) {
            plugin.logger().warn("Failed to create adapter instance: {}", adapter, t);
            t.getCause().printStackTrace();
        }
        return null;
    }

    P getPlugin();

    void enable();

    void reload();

    void disable();

}
