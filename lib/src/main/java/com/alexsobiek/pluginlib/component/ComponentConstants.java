package com.alexsobiek.pluginlib.component;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ComponentConstants {
    public static final TextComponent PLUGIN_RELOAD_SUCCESS = Component.text("Plugin reloaded", Color.BLUE);
    public static final TextComponent PLUGIN_RELOAD_FAIL = Component.text("Failed to reload plugin. Check the console for more details.", Color.RED);
    public static final TextComponent INTERNAL_SERVER_ERROR = Component.text("Internal server error.", Color.RED);
    public static final TextComponent NO_PLAYER = Component.text("That player does not exist", Color.RED);
}