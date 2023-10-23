package com.alexsobiek.pluginlib.test;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import com.alexsobiek.pluginlib.adapter.CommandAdapter;
import org.bukkit.command.CommandSender;


@CommandAlias("test")
public class TestCommand extends CommandAdapter<TestPlugin> {
    @Override
    public void enable() {

    }

    @Override
    public void reload() {

    }

    @Default
    public void onTest(CommandSender sender) {
        sender.sendMessage("Hello from the test plugin!");
    }
}
