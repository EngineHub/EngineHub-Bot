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

import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.enginehub.discord.EngineHubBot;
import org.enginehub.discord.util.PermissionRole;
import org.enginehub.discord.util.PunishmentUtil;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

public class ChatFilter extends ListenerAdapter implements Module {

    private static final Pattern INVITE_PATTERN = Pattern.compile("discord.gg/([a-zA-Z0-9\\-_]*)");

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        // Don't check for people who are allowed to send invites
        if (EngineHubBot.isAuthorised(event.getMember(), PermissionRole.TRUSTED)) {
            return;
        }

        // Don't check private channels
        if (event.getChannel() instanceof PrivateChannel) {
            return;
        }

        Matcher matcher = INVITE_PATTERN.matcher(event.getMessage().getContentRaw());
        if (matcher.find()) {
            event.getMessage().delete().queue();
        }
    }

    private final List<Pattern> badNamePatterns = List.of(
        Pattern.compile("discord.me/"),
        Pattern.compile("discord.gg/"),
        Pattern.compile("twitter.com/"),
        Pattern.compile("twitter/"),
        Pattern.compile("twitch.tv/"),
        Pattern.compile("bit.ly/")
    );

    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {
        for (Pattern pattern : badNamePatterns) {
            if (pattern.matcher(event.getUser().getName()).find()) {
                PunishmentUtil.banUser(event.getGuild(), event.getUser(), "Banned URL in username", true);
                return;
            }
        }
    }
}
