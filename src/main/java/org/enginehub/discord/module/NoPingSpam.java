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

import org.enginehub.discord.EngineHubBot;
import org.enginehub.discord.util.PermissionRoles;
import org.enginehub.discord.util.PunishmentUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashMap;

import javax.annotation.Nonnull;

public class NoPingSpam extends ListenerAdapter implements Module {

    private final HashMap<String, Integer> spamTimes = new HashMap<>();

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        // Don't check for people who are allowed to ping
        if (EngineHubBot.isAuthorised(event.getMember(), PermissionRoles.TRUSTED)) {
            return;
        }

        int mentionCount = event.getMessage().getMentions(Message.MentionType.ROLE, Message.MentionType.USER).size();

        if (mentionCount >= 6) {
            event.getMessage().delete().queue();
            int spamTime = spamTimes.getOrDefault(event.getAuthor().getId(), 0) + 1;
            spamTimes.put(event.getAuthor().getId(), spamTime);
            if (spamTime >= 3) {
                // Do the ban.
                PunishmentUtil.banUser(event.getGuild(), event.getAuthor(), "Ping spam", true);
            } else {
                event.getAuthor().openPrivateChannel().queue(privateChannel ->
                        privateChannel.sendMessage("Hey! Spamming messages is not allowed here. If you continue, you will be banned.").queue());
            }
        }
    }
}
