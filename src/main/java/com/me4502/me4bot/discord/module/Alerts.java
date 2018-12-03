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
package com.me4502.me4bot.discord.module;

import com.google.common.collect.Maps;
import com.me4502.me4bot.discord.Me4Bot;
import com.me4502.me4bot.discord.Settings;
import com.me4502.me4bot.discord.util.PermissionRoles;
import com.me4502.me4bot.discord.util.StringUtil;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.fluent.DispatcherNode;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import ninja.leaping.configurate.ConfigurationNode;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Alerts implements Module, EventListener {

    public static Map<String, String> alertChannels = Maps.newHashMap();

    private List<Pattern> badNamePatterns = List.of(
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
    public void onEvent(Event event) {
        if (event instanceof GuildMemberJoinEvent) {
            for (Pattern pattern : badNamePatterns) {
                if (pattern.matcher(((GuildMemberJoinEvent) event).getUser().getName()).find()) {
                    ((GuildMemberJoinEvent) event).getUser().openPrivateChannel().queue(privateChannel
                            -> NoSpam.banForSpam(((GuildMemberJoinEvent) event).getGuild(), ((GuildMemberJoinEvent) event).getUser(), privateChannel));
                    return;
                }
            }
            String channelId = alertChannels.get(((GuildMemberJoinEvent) event).getGuild().getId());
            if (channelId != null) {
                MessageChannel channel = Me4Bot.bot.api.getTextChannelById(channelId);
                if (channel != null) {
                    sendMessage(((GuildMemberJoinEvent) event).getGuild(), channel, ((GuildMemberJoinEvent) event).getUser(), true);
                }
            }
        } else if (event instanceof GuildMemberLeaveEvent) {
            String channelId = alertChannels.get(((GuildMemberLeaveEvent) event).getGuild().getId());
            if (channelId != null) {
                MessageChannel channel = Me4Bot.bot.api.getTextChannelById(channelId);
                if (channel != null) {
                    sendMessage(((GuildMemberLeaveEvent) event).getGuild(), channel, ((GuildMemberLeaveEvent) event).getUser(), false);
                }
            }
        }
    }

    /**
     * Sends the alert message.
     *
     * @param user The user
     * @param join True for join false for leave
     */
    private void sendMessage(Guild guild, MessageChannel channel, User user, boolean join) {

        if (!join && guild.getBanList().complete().stream().anyMatch(ban -> ban.getUser().getIdLong() == user.getIdLong())) {
            channel.sendMessage("**" + StringUtil.annotateUser(user) + "** has been banned!").queue();
            return;
        }

        if (user.isBot()) {
            channel.sendMessage("**" + StringUtil.annotateUser(user) + "** (Bot) has been " + (join ? "added to" : "removed from") + " the server!").queue();
        } else {
            channel.sendMessage("**" + StringUtil.annotateUser(user) + "** has " + (join ? "joined" : "left") + " the server!").queue();
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
