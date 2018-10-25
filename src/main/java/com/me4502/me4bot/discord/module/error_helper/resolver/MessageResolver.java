package com.me4502.me4bot.discord.module.error_helper.resolver;

import java.util.List;

public class MessageResolver implements ErrorResolver {

    @Override
    public List<String> foundText(String message) {
        return List.of(message);
    }
}
