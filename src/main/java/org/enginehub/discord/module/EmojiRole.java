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

import com.google.common.reflect.TypeToken;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.enginehub.discord.EngineHubBot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class EmojiRole extends ListenerAdapter implements Module {

    private Map<String, String> emojiToRole = new HashMap<>();
    private String messageId;
    private String channelId;

    private Optional<Role> getRoleByEmoji(Guild guild, String emoji) {
        if (!emojiToRole.containsKey(emoji)) {
            return Optional.empty();
        }
        return Optional.ofNullable(guild.getRoleById(emojiToRole.get(emoji)));
    }

    private static void toggleRole(Guild guild, Role role, Member member, MessageReaction reaction) {
        boolean hasRole = member.getRoles().stream().anyMatch(testRole -> role.getIdLong() == testRole.getIdLong());
        if (hasRole) {
            guild.removeRoleFromMember(member, role).queue();
        } else {
            guild.addRoleToMember(member, role).queue();
        }
        reaction.removeReaction(member.getUser()).queue();
    }

    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        if (!(event.getChannel() instanceof GuildMessageChannel)) {
            // Only supported within a guild.
            return;
        }

        if (event.getUser().isBot()) {
            // Ignore bots.
            return;
        }

        if (event.getMessageId().equals(messageId)) {
            getRoleByEmoji(event.getGuild(), event.getEmoji().asCustom().getId()).ifPresentOrElse(
                    role -> toggleRole(event.getGuild(), role, event.getMember(), event.getReaction()),
                    () -> event.getReaction().removeReaction(event.getUser()).queue());
        }
    }

    @Override
    public void load(ConfigurationNode loadedNode) {
        emojiToRole = loadedNode.getNode("roleMap").getChildrenMap().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> e.getValue().getString("FAILED TO LOAD")
                ));
        messageId = loadedNode.getNode("messageId").getString();
        channelId = loadedNode.getNode("channelId").getString();

        try {
            TextChannel channel = EngineHubBot.bot.api.getTextChannelById(channelId);
            if (channel == null) {
                throw new IllegalArgumentException("Invalid channel ID provided");
            }
            Message message = channel.retrieveMessageById(messageId).complete();
            Guild guild = message.getGuild();
            message.getReactions()
                    .forEach(reaction -> getRoleByEmoji(guild, reaction.getEmoji().asCustom().getId())
                            .ifPresent(role -> reaction.retrieveUsers()
                                    .queue(users -> {
                                        for (User user : users) {
                                            if (user.isBot()) {
                                                // Skip bots.
                                                continue;
                                            }
                                            guild.retrieveMember(user).queue(mem -> {
                                                if (mem != null) {
                                                    toggleRole(guild, role, mem, reaction);
                                                }
                                            });
                                        }
                                    })));

            message.clearReactions().complete();
            for (String emoteKey : emojiToRole.keySet()) {
                RichCustomEmoji emote = guild.getEmojiById(emoteKey);
                if (emote != null) {
                    message.addReaction(emote).complete();
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void save(ConfigurationNode loadedNode) {
        try {
            loadedNode.getNode("roleMap").setValue(new TypeToken<Map<String, String>>(){}, emojiToRole);
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }
        loadedNode.getNode("messageId").setValue(messageId);
        loadedNode.getNode("channelId").setValue(channelId);
    }
}
