package com.me4502.me4bot.discord.module;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;

public interface Module {

    default void onInitialise() {}

    default void onTick() {}

    default void load(ConfigurationNode loadedNode) {}

    default void save(ConfigurationNode loadedNode) {}

    default void onShutdown() {}
}
