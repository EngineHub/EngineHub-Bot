package com.me4502.me4bot.discord.module;

import com.me4502.me4bot.discord.Me4Bot;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFilter implements Module, EventListener {

    private static final Pattern INVITE_PATTERN = Pattern.compile("discord.gg/([a-zA-Z0-9\\-_]*)");

    @Override
    public void onEvent(Event event) {
        if (event instanceof MessageReceivedEvent) {
            Matcher matcher = INVITE_PATTERN.matcher(((MessageReceivedEvent) event).getMessage().getContent());
            if (matcher.find() && !Me4Bot.isAuthorised(((MessageReceivedEvent) event).getAuthor())) {
                ((MessageReceivedEvent) event).getMessage().delete().queue();
            }
        }
    }
}
