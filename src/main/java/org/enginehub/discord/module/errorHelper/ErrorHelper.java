/*
 * Copyright (c) EngineHub and Contributors
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
package org.enginehub.discord.module.errorHelper;

import com.google.common.reflect.TypeToken;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.enginehub.discord.module.Module;
import org.enginehub.discord.module.errorHelper.resolver.ErrorResolver;
import org.enginehub.discord.module.errorHelper.resolver.GhostbinResolver;
import org.enginehub.discord.module.errorHelper.resolver.GistResolver;
import org.enginehub.discord.module.errorHelper.resolver.MCLogsResolver;
import org.enginehub.discord.module.errorHelper.resolver.RawSubdirectoryUrlResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class ErrorHelper extends ListenerAdapter implements Module {

    private final List<ErrorResolver> resolvers = List.of(
            List::of,
            new RawSubdirectoryUrlResolver("pastebin.com", "raw"), // PastebinResolver
            new RawSubdirectoryUrlResolver("hastebin.com", "raw"), // HastebinResolver
            new RawSubdirectoryUrlResolver("paste.helpch.at", "raw"), // PasteHelpchatResolver
            new GhostbinResolver(),
            new GistResolver(),
            new MCLogsResolver(),
            new RawSubdirectoryUrlResolver("paste.enginehub.org", "documents", true) // EngineHubResolver
    );

    private List<ErrorEntry> errorMessages = new ArrayList<>();

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        scanMessage(event.getMessage(), event.getAuthor(), event.getChannel());
    }

    public void onPrivateMessageReceived(@Nonnull PrivateMessageReceivedEvent event) {
        scanMessage(event.getMessage(), event.getAuthor(), event.getChannel());
    }

    public void scanMessage(Message message, User author, MessageChannel channel) {
        StringBuilder messageText = new StringBuilder(message.getContentRaw());
        for (Message.Attachment attachment : message.getAttachments()) {
            if (attachment.isImage()) continue; // TODO Maybe text processing in images?
            if (attachment.getFileName().endsWith(".txt") || attachment.getFileName().endsWith(".log")) {
                if (attachment.getSize() > 1024*1024*10) {
                    channel.sendMessage("[AutoReply] " + author.getAsMention() + " Log too large "
                        + "to scan.").queue();
                    continue; //Ignore >10MB for now.
                }
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(attachment.retrieveInputStream().get()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        messageText.append(line);
                    }
                } catch (IOException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
        resolvers.parallelStream()
            .flatMap(resolver -> resolver.foundText(messageText.toString()).stream())
            .map(ErrorHelper::cleanString)
            .flatMap(error -> messagesForError(error).stream())
            .distinct()
            .forEach(mes ->
                channel.sendMessage("[AutoReply] " + author.getAsMention() + ' ' + mes).queue());
    }

    private static String cleanString(String string) {
        return string.toLowerCase().replace("\n", "").replace("\r", "").replace(" ", "").replace("\t", "");
    }

    private List<String> messagesForError(String error) {
        return errorMessages.stream()
                .filter(entry -> entry.doesTrigger(error))
                .map(ErrorEntry::getResponse)
                .collect(Collectors.toList());
    }

    public static String getStringFromUrl(String url) {
        return getStringFromUrl0(url, 0);
    }

    private static String getStringFromUrl0(String url, int tries) {
        StringBuilder main = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                main.append(line);
            }
        } catch (Throwable e) {
            System.out.println("Failed to load URL " + url + " (Tries " + tries + ')');
            e.printStackTrace();
            if (tries < 5) {
                return getStringFromUrl0(url, tries + 1);
            }
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

    public static class ErrorEntry {
        private final String name;
        private final List<String> triggers;
        private final List<String> cleanedTriggers;
        private final String response;

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