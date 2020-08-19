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
package org.enginehub.discord.module;

import com.google.common.collect.Maps;
import org.enginehub.discord.EngineHubBot;
import org.enginehub.discord.Settings;
import org.enginehub.discord.util.PermissionRoles;
import org.enginehub.discord.util.PunishmentUtil;
import org.enginehub.discord.util.StringUtil;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.fluent.DispatcherNode;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ninja.leaping.configurate.ConfigurationNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

public class Alerts extends ListenerAdapter implements Module {

    public static Map<String, String> alertChannels = Maps.newHashMap();

    private final List<Pattern> badNamePatterns = List.of(
            Pattern.compile("discord.me/"),
            Pattern.compile("twitter.com/"),
            Pattern.compile("twitter/"),
            Pattern.compile("twitch.tv/"),
            Pattern.compile("bit.ly/")
    );

    @Override
    public DispatcherNode setupCommands(DispatcherNode dispatcherNode) {
        return dispatcherNode
                .registerMethods(this);
    }

    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {
        for (Pattern pattern : badNamePatterns) {
            if (pattern.matcher(event.getUser().getName()).find()) {
                PunishmentUtil.banUser(event.getGuild(), event.getUser(), "Banned URL in username", true);
                return;
            }
        }
    }

    @Override
    public void onGuildMemberRemove(@Nonnull GuildMemberRemoveEvent event) {
        String channelId = alertChannels.get(event.getGuild().getId());
        if (channelId != null) {
            MessageChannel channel = EngineHubBot.bot.api.getTextChannelById(channelId);
            if (channel != null) {
                sendMessage(event.getGuild(), channel, event.getUser());
            }
        }
    }

    /**
     * Sends the alert message.
     *
     * @param user The user
     */
    private static void sendMessage(Guild guild, MessageChannel channel, User user) {
        String annotatedName = "**" + StringUtil.annotateUser(user) + "** (" + user.getName() + ") ";
        if (user.isBot()) {
            annotatedName += "[Bot] ";
        }
        Optional<Guild.Ban> banEntry = guild.retrieveBanList().complete()
                        .stream()
                        .filter(ban -> ban.getUser().getIdLong() == user.getIdLong())
                        .findAny();
        if (banEntry.isPresent()) {
            channel.sendMessage(annotatedName + "has been banned! `" + banEntry.get().getReason() + '`').queue();
            return;
        }

        if (user.isBot()) {
            channel.sendMessage(annotatedName + "has been removed from the server!").queue();
        } else {
            channel.sendMessage(annotatedName + "has left the server!").queue();
        }
    }

    @Command(aliases = "alert", desc = "Sets the channel to alert.")
    @Require(PermissionRoles.ADMIN)
    public void alert(Message message) {
        alertChannels.put(message.getGuild().getId(), message.getChannel().getId());
        message.getChannel().sendMessage("Set alert channel!").queue();

        Settings.saveModule(this);
    }

    @Override
    public void load(ConfigurationNode loadedNode) {
        alertChannels = loadedNode.getNode("alert-channels").getChildrenMap().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> e.getValue().getString("alert")
                ));
    }

    @Override
    public void save(ConfigurationNode loadedNode) {
        loadedNode.getNode("alert-channels").setValue(alertChannels);
    }
}
