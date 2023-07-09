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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ninja.leaping.configurate.ConfigurationNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.enginehub.discord.EngineHubBot;
import org.enginehub.discord.util.PermissionRole;
import org.enginehub.discord.util.command.CommandPermission;
import org.enginehub.discord.util.command.CommandPermissionConditionGenerator;
import org.enginehub.discord.util.command.CommandRegistrationHandler;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.enginehub.discord.util.StringUtil.createEmbed;

@CommandContainer(superTypes = CommandPermissionConditionGenerator.Registration.class)
public class PrivateForwarding extends ListenerAdapter implements Module {

    private static final Logger LOGGER = LogManager.getLogger();

    private String forwardChannel;

    private static boolean shouldIgnore(String text) {
        return text.startsWith("Hey! Welcome to the EngineHub Discord")
            || text.contains("Hey! Spamming messages is not allowed here")
            || text.contains("It's against the rules")
            || text.contains("You have been kicked")
            || text.contains("You have been banned");
    }

    @Override
    public void setupCommands(CommandRegistrationHandler handler, CommandManager commandManager) {
        handler.register(commandManager, PrivateForwardingRegistration.builder(), this);
    }

    @Command(name = "replydm", desc = "Manually reply to a DM to the bot.")
    @CommandPermission(PermissionRole.MODERATOR)
    public void replyDM(Message message, @Arg(desc = "The user to reply to") String userId, @Arg(desc = "The response", variable = true) List<String> response) {
        User user = EngineHubBot.bot.api.getUserByTag(userId);
        if (user == null) {
            message.getChannel().sendMessage("Unknown user!").queue();
            return;
        }

        user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(String.join(" ", response)).queue());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (forwardChannel != null && event.getChannel() instanceof PrivateChannel privateChannel) {
            var channel = EngineHubBot.bot.api.getGuildChannelById(forwardChannel);
            if (channel instanceof TextChannel textChannel) {
                if (event.getMessage().getType().isSystem()) {
                    // Ignore any system messages
                    return;
                }

                var user = event.getAuthor();
                if (!textChannel.getGuild().isMember(user)) {
                    // Skip if they're not in the guild we're forwarding the message to.
                    return;
                }

                if (event.getMessage().getContentRaw().isBlank() && event.getMessage().getAttachments().isEmpty()) {
                    // Ignore this blank message
                    LOGGER.warn("Ignoring message from " + user.getIdLong() + " (" + user.getName() + ')'
                        + " because it is empty, the type of the message was " + event.getMessage().getType());
                    return;
                }

                if (user.getIdLong() == event.getJDA().getSelfUser().getIdLong() && shouldIgnore(event.getMessage().getContentRaw())) {
                    // Ignore some of our own messages.
                    return;
                }

                EmbedBuilder builder = createEmbed()
                    .setAuthor(
                        user.getName(),
                        "https://discord.com/channels/@me/" + user.getId(),
                        user.getEffectiveAvatarUrl()
                    )
                    .setDescription(event.getMessage().getContentRaw())
                    .setTimestamp(event.getMessage().getTimeCreated())
                    .setFooter("In DM " + privateChannel.getUser().getName()
                        + " (" + privateChannel.getUser().getId() + ')');

                event.getMessage().getAttachments().forEach(att -> builder.addField(att.getFileName(), att.getUrl(), false));

                textChannel.sendMessageEmbeds(builder.build()).queue();
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
