/*
 * Copyright (c) Me4502 (Matthew Miller)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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