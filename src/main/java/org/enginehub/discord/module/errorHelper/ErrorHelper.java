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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.sourceforge.tess4j.Tesseract;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.enginehub.discord.EngineHubBot;
import org.enginehub.discord.module.LinkGrabber;
import org.enginehub.discord.module.Module;
import org.enginehub.discord.module.errorHelper.resolver.ErrorResolver;
import org.enginehub.discord.module.errorHelper.resolver.GhostbinResolver;
import org.enginehub.discord.module.errorHelper.resolver.GistResolver;
import org.enginehub.discord.module.errorHelper.resolver.IncompatibleResolver;
import org.enginehub.discord.module.errorHelper.resolver.MCLogsResolver;
import org.enginehub.discord.module.errorHelper.resolver.RawSubdirectoryUrlResolver;
import org.enginehub.discord.util.PasteUtil;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

public class ErrorHelper extends ListenerAdapter implements Module {

    private static final Logger LOGGER = LogManager.getLogger();

    private final List<ErrorResolver> resolvers = List.of(
            List::of,
            new RawSubdirectoryUrlResolver("pastebin.com", "raw"), // PastebinResolver
            new RawSubdirectoryUrlResolver("hastebin.com", "raw"), // HastebinResolver
            new RawSubdirectoryUrlResolver("paste.helpch.at", "raw"), // PasteHelpchatResolver
            new RawSubdirectoryUrlResolver("paste.md-5.net", "raw"), // md-5.net
            new GhostbinResolver(),
            new GistResolver(),
            new MCLogsResolver(),
            new RawSubdirectoryUrlResolver("paste.enginehub.org", "documents", true), // EngineHubResolver
            new IncompatibleResolver("mcpaste.io")
    );

    private List<ErrorEntry> errorMessages = new ArrayList<>();
    private Tesseract tesseract;

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.getAuthor().isSystem() || event.getAuthor().isBot()) {
            // Don't bother scanning bot or system messages.
            return;
        }
        scanMessage(event.getMessage(), event.getAuthor(), event.getChannel());
    }

    public void scanMessage(Message message, User author, MessageChannel channel) {
        StringBuilder messageText = new StringBuilder(message.getContentRaw());
        for (Message.Attachment attachment : message.getAttachments()) {
            if (attachment.isImage()) {
                if (tesseract == null) {
                    continue;
                }

                try (InputStream is = attachment.getProxy().download().get()) {
                    BufferedImage image = ImageIO.read(is);
                    messageText.append(tesseract.doOCR(image));
                    if (EngineHubBot.isBotOwner(author) && message.getChannel() instanceof PrivateChannel) {
                        // If it's a bot developer, send OCR debug text.
                        channel.sendMessage("[OCR Debug] " + messageText).queue();
                    }
                } catch (Throwable t) {
                    LOGGER.warn("Tesseract failed to OCR image", t);
                }
            }
            if (attachment.getFileName().endsWith(".txt") || attachment.getFileName().endsWith(".log")) {
                if (attachment.getSize() > 1024*1024*10) {
                    channel.sendMessage("[AutoReply] " + author.getAsMention() + " Log too large "
                        + "to scan.").queue();
                    continue; //Ignore >10MB for now.
                }
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(attachment.getProxy().download().get()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        messageText.append(line).append('\n');
                    }
                } catch (IOException | InterruptedException | ExecutionException e) {
                    LOGGER.error("Failed to read attachment", e);
                }

                try {
                    PasteUtil.sendToPastebin(messageText.toString()).thenAccept(url -> {
                        String responseUrl = url.toString();
                        if (attachment.getFileName().equals("report.txt")) {
                            responseUrl = responseUrl + ".report";
                        }
                        channel.sendMessage("[AutoReply] Here's a pasted version, " + responseUrl).queue();
                    });
                } catch (IOException | URISyntaxException | InterruptedException e) {
                    LOGGER.error("Failed to send to EH Paste Service", e);
                }
            }
        }
        resolvers.parallelStream()
            .flatMap(resolver -> resolver.foundText(messageText.toString()).stream())
            .map(ErrorHelper::cleanString)
            .flatMap(this::messagesForError)
            .distinct()
            .map(mes -> {
                if (mes.startsWith("~~ ")) {
                    return EngineHubBot.bot.getModuleByType(LinkGrabber.class).map(linkGrabber -> linkGrabber.mapAlias(mes.substring("~~ ".length()))).orElse(mes);
                } else {
                    return mes;
                }
            })
            .forEach(mes -> {
                channel.sendMessage("[AutoReply] " + author.getAsMention() + ' ' + mes).queue();
            });
    }

    private static final Pattern REMOVE_CHARS = Pattern.compile("[\n\r \t’‘“”`\"']");

    private static String cleanString(String string) {
        return REMOVE_CHARS.matcher(string.toLowerCase()).replaceAll("");
    }

    private Stream<String> messagesForError(String error) {
        List<ErrorEntry> applicableErrors = errorMessages.stream().filter(entry -> entry.doesTrigger(error)).toList();
        Set<String> applicableEntryNames = applicableErrors.stream().map(ErrorEntry::getName).collect(Collectors.toSet());
        return applicableErrors.stream().filter(entry -> {
            if (entry.getSilencedBy().isEmpty()) {
                return true;
            }
            return entry.getSilencedBy().stream().noneMatch(applicableEntryNames::contains);
        }).map(ErrorEntry::getResponse);
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
            LOGGER.warn("Failed to load URL " + url + " (Tries " + tries + ')', e);
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
                    ConfigurationNode node = entry.getValue();
                    try {
                        return new ErrorEntry(entry.getKey().toString(),
                            node.getNode("match-text").getList(TypeToken.of(String.class)),
                            node.getNode("silenced-by").getList(TypeToken.of(String.class), List.of()),
                            node.getNode("error-message").getString()
                        );
                    } catch (ObjectMappingException e) {
                        LOGGER.error("Failed to read ErrorHelper config mapping for " + entry.getKey().toString(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        Path tessDataPath = Paths.get("tessdata");

        if (Files.exists(tessDataPath) && Files.isDirectory(tessDataPath)) {
            tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath.toAbsolutePath().toString());
            LOGGER.info("Tesseract loaded!");
        } else {
            LOGGER.warn("Tesseract not found!");
        }
    }

    @Override
    public void save(ConfigurationNode loadedNode) {
        loadedNode.getNode("error-messages").setValue(errorMessages.stream().collect(Collectors.toMap(
                ErrorEntry::getName,
                e -> Map.of("match-text", e.getTriggers(), "error-message", e.getResponse(), "silenced-by", e.getSilencedBy())
        )));
    }

    public static class ErrorEntry {
        private final String name;
        private final List<String> triggers;
        private final List<String> cleanedTriggers;
        private final List<String> silencedBy;
        private final String response;

        ErrorEntry(String name, List<String> triggers, List<String> silencedBy, String response) {
            this.name = name;
            this.triggers = triggers;
            this.cleanedTriggers = triggers.stream().map(ErrorHelper::cleanString).toList();
            this.silencedBy = silencedBy;
            this.response = response;
        }

        String getName() {
            return this.name;
        }

        List<String> getTriggers() {
            return this.triggers;
        }

        boolean doesTrigger(String error) {
            return this.cleanedTriggers.stream().allMatch(error::contains);
        }

        public List<String> getSilencedBy() {
            return this.silencedBy;
        }

        String getResponse() {
            return this.response;
        }
    }
}
