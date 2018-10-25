package com.me4502.me4bot.discord.module.error_helper.resolver;

import java.util.List;

@FunctionalInterface
public interface ErrorResolver {

    /**
     * Get a list of messages that can be parsed for errors from this message.
     *
     * <p>
     *     This method should exit early if it's not worth checking.
     * </p>
     *
     * @param message The message
     * @return The parseable messages
     */
    List<String> foundText(String message);
}
