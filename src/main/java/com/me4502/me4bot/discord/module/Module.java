package com.me4502.me4bot.discord.module;

public interface Module {

    default void onInitialise() {}

    default void onTick() {}

    default void onShutdown() {}
}
