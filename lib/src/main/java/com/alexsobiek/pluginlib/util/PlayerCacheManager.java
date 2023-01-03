package com.alexsobiek.pluginlib.util;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerCacheManager {
    private final List<Map<UUID, ?>> cacheList = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public <M extends Map<UUID, T>, T> M create(Class<? extends Map> mapClass, Object... args) {
        try {
            Constructor<? extends Map<UUID, T>> constructor = (Constructor<? extends Map<UUID, T>>) mapClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            M map = (M) constructor.newInstance(args);
            cacheList.add(map);
            return map;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create cache", t);
        }
    }

    public void clear() {
        cacheList.clear();
    }

    public void handleLeave(UUID uuid) {
        cacheList.forEach(cache -> {
            cache.remove(uuid);
        });

    }
}
