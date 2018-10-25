package com.me4502.me4bot.discord.module.error_helper;

import com.me4502.me4bot.discord.module.Module;
import com.me4502.me4bot.discord.module.error_helper.resolver.ErrorResolver;
import com.me4502.me4bot.discord.module.error_helper.resolver.MessageResolver;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.util.List;
import java.util.Optional;

public class ErrorHelper implements Module, EventListener {

    private List<ErrorResolver> resolvers = List.of(new MessageResolver());

    @Override
    public void onEvent(Event event) {
        if (event instanceof MessageReceivedEvent) {
            String messageText = ((MessageReceivedEvent) event).getMessage().getContentRaw();
            MessageChannel channel = ((MessageReceivedEvent) event).getChannel();
            resolvers.parallelStream()
                    .flatMap(resolver -> resolver.foundText(messageText).stream())
                    .map(this::messageForError)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .distinct()
                    .forEach(message ->
                            channel.sendMessage("@" + ((MessageReceivedEvent) event).getAuthor().getIdLong() + " " + message).queue());
        }
    }

    public Optional<String> messageForError(String error) {
        return Optional.empty();
    }
}