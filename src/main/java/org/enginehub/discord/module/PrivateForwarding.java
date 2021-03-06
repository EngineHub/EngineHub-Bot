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
package org.enginehub.discord.module;

import com.sk89q.intake.Command;
import com.sk89q.intake.fluent.DispatcherNode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ninja.leaping.configurate.ConfigurationNode;
import org.enginehub.discord.EngineHubBot;
import org.jetbrains.annotations.NotNull;

import static org.enginehub.discord.util.StringUtil.createEmbed;

public class PrivateForwarding extends ListenerAdapter implements Module {

    private String forwardChannel;

    private static boolean shouldIgnore(String text) {
        return text.startsWith("Hey! Welcome to the EngineHub Discord")
            || text.contains("Hey! Spamming messages is not allowed here")
            || text.contains("It's against the rules")
            || text.contains("You have been kicked")
            || text.contains("You have been banned");
    }

    @Override
    public DispatcherNode setupCommands(DispatcherNode dispatcherNode) {
        return dispatcherNode
            .registerMethods(this);
    }

    @Command(aliases = {"replydm"}, desc = "Manually reply to a DM to the bot.")
    public void replyDM(Message message, String userId, String response) {
        User user = EngineHubBot.bot.api.getUserByTag(userId);
        if (user == null) {
            message.getChannel().sendMessage("Unknown user!").queue();
            return;
        }

        user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(response).queue());
    }

    @Override
    public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {
        if (forwardChannel != null) {
            var channel = EngineHubBot.bot.api.getGuildChannelById(forwardChannel);
            if (channel instanceof TextChannel) {
                if (event.getMessage().getType().isSystem()) {
                    // Ignore any system messages
                    return;
                }

                User user = event.getAuthor();

                if (event.getMessage().getContentRaw().isBlank()) {
                    // Ignore this blank message
                    // TODO figure out what causes these
                    System.err.println("Ignoring message from " + user.getIdLong() + " (" + user.getAsTag() + ")"
                        + " because it is empty, the type of the message was " + event.getMessage().getType());
                    return;
                }

                if (user.getIdLong() == event.getJDA().getSelfUser().getIdLong() && shouldIgnore(event.getMessage().getContentRaw())) {
                    // Ignore some of our own messages.
                    return;
                }

                EmbedBuilder builder = createEmbed()
                    .setAuthor(
                        user.getAsTag(),
                        "https://discord.com/channels/@me/" + user.getId(),
                        user.getEffectiveAvatarUrl()
                    )
                    .setDescription(event.getMessage().getContentRaw())
                    .setTimestamp(event.getMessage().getTimeCreated())
                    .setFooter("In DM " + event.getChannel().getUser().getAsTag()
                        + " (" + event.getChannel().getUser().getId() + ")");

                event.getMessage().getAttachments().forEach(att -> builder.addField(att.getFileName(), att.getUrl(), false));

                ((TextChannel) channel).sendMessage(builder.build()).queue();
            }
        }
    }

    @Override
    public void load(ConfigurationNode loadedNode) {
        forwardChannel = loadedNode.getNode("forward-channel").getString(null);
    }

    @Override
    public void save(ConfigurationNode loadedNode) {
        loadedNode.getNode("forward-channel").setValue(forwardChannel);
    }
}
