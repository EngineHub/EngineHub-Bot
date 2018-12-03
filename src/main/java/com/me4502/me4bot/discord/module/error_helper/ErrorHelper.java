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

import com.google.common.reflect.TypeToken;
import com.me4502.me4bot.discord.module.Module;
import com.me4502.me4bot.discord.module.error_helper.resolver.ErrorResolver;
import com.me4502.me4bot.discord.module.error_helper.resolver.GhostbinResolver;
import com.me4502.me4bot.discord.module.error_helper.resolver.GistResolver;
import com.me4502.me4bot.discord.module.error_helper.resolver.HastebinResolver;
import com.me4502.me4bot.discord.module.error_helper.resolver.MessageResolver;
import com.me4502.me4bot.discord.module.error_helper.resolver.PastebinResolver;
import com.me4502.me4bot.discord.util.StringUtil;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ErrorHelper implements Module, EventListener {

    private List<ErrorResolver> resolvers = List.of(
            new MessageResolver(),
            new PastebinResolver(),
            new HastebinResolver(),
            new GhostbinResolver(),
            new GistResolver()
    );

    private List<ErrorEntry> errorMessages = new ArrayList<>();

    @Override
    public void onEvent(Event event) {
        if (event instanceof MessageReceivedEvent) {
            String messageText = ((MessageReceivedEvent) event).getMessage().getContentRaw();
            MessageChannel channel = ((MessageReceivedEvent) event).getChannel();
            resolvers.parallelStream()
                    .flatMap(resolver -> resolver.foundText(messageText).stream())
                    .map(ErrorHelper::cleanString)
                    .map(this::messageForError)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .distinct()
                    .forEach(message ->
                            channel.sendMessage("[AutoReply] " + StringUtil.annotateUser(((MessageReceivedEvent) event).getAuthor()) + ' ' + message).queue());
        }
    }

    private static String cleanString(String string) {
        return string.toLowerCase().replace("\n", "").replace("\r", "").replace(" ", "").replace("\t", "");
    }

    private Optional<String> messageForError(String error) {
        for (ErrorEntry entry : errorMessages) {
            if (entry.doesTrigger(error)) {
                return Optional.of(entry.getResponse());
            }
        }

        return Optional.empty();
    }

    public static String getStringFromUrl(String url) {
        StringBuilder main = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                main.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return main.toString();
    }

    @Override
    public void load(ConfigurationNode loadedNode) {
        errorMessages = loadedNode.getNode("error-messages").getChildrenMap().entrySet().stream()
                .map(entry -> {
                    try {
                        return new ErrorEntry(entry.getKey().toString(),
                                entry.getValue().getNode("match-text").getList(TypeToken.of(String.class)),
                                entry.getValue().getNode("error-message").getString()
                        );
                    } catch (ObjectMappingException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void save(ConfigurationNode loadedNode) {
        loadedNode.getNode("error-messages").setValue(errorMessages.stream().collect(Collectors.toMap(
                ErrorEntry::getName,
                e -> Map.of("match-text", e.getTriggers(), "error-message", e.getResponse())
        )));
    }

    private static class ErrorEntry {
        private String name;
        private List<String> triggers;
        private List<String> cleanedTriggers;
        private String response;

        ErrorEntry(String name, List<String> triggers, String response) {
            this.name = name;
            this.triggers = triggers;
            this.cleanedTriggers = triggers.stream().map(ErrorHelper::cleanString).collect(Collectors.toList());
            this.response = response;
        }

        String getName() {
            return this.name;
        }

        List<String> getTriggers() {
            return this.triggers;
        }

        boolean doesTrigger(String error) {
            return cleanedTriggers.stream().allMatch(error::contains);
        }

        String getResponse() {
            return this.response;
        }
    }
}