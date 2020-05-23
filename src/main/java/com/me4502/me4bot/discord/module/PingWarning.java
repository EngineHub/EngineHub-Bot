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

import com.me4502.me4bot.discord.Me4Bot;
import com.me4502.me4bot.discord.Settings;
import com.me4502.me4bot.discord.util.PermissionRoles;
import com.me4502.me4bot.discord.util.StringUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashMap;

import javax.annotation.Nonnull;

public class PingWarning extends ListenerAdapter implements Module {

    private final HashMap<String, Integer> spamTimes = new HashMap<>();

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        boolean mentionsDev = event.getMessage().getMentionedMembers().stream().anyMatch(user -> Me4Bot.isAuthorised(user,
                PermissionRoles.MODERATOR));
        if (mentionsDev && !Me4Bot.isAuthorised(event.getMember(), PermissionRoles.TRUSTED)) {
            event.getChannel().sendMessage("Hey " + StringUtil.annotateUser(
                    event.getAuthor()
            ) + "! It's against the rule to ping the developers, make sure to read the rules!").queue();
            int spamTime = spamTimes.getOrDefault(event.getAuthor().getId(), 0);
            spamTime ++;
            spamTimes.put(event.getAuthor().getId(), spamTime);
            if (spamTime >= 3) {
                // Do the ban.
                kickForSpam(event.getGuild(), event.getAuthor(), spamTime >= 4);
            }
        }
    }

    public static void kickForSpam(Guild guild, User user, boolean ban) {
        user.openPrivateChannel().queue((privateChannel -> {
            privateChannel.sendMessage("You have been " + (ban ? "banned" : "kicked") +
                    " for repeatedly pinging devs. Contact " + Settings.hostUsername + "#" + Settings.hostIdentifier + " if you believe this is a mistake.")
                    .queue(message -> {
                        if (ban) {
                            guild.ban(user, 0, "Repeatedly pinging devs").queue();
                        } else {
                            guild.kick(guild.getMember(user), "Pinging devs").queue();
                        }
                    });
        }));
    }
}
