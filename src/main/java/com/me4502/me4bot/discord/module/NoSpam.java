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
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.util.HashMap;

public class NoSpam implements Module, EventListener {

    private final HashMap<String, Integer> spamTimes = new HashMap<>();

    @Override
    public void onEvent(Event event) {
        if (event instanceof MessageReceivedEvent) {
            int mentionCount = ((MessageReceivedEvent) event).getMessage().getMentionedUsers().size()
                    + ((MessageReceivedEvent) event).getMessage().getMentionedRoles().size();
            if (mentionCount >= 6 && !Me4Bot.isAuthorised(((MessageReceivedEvent) event).getMember(), PermissionRoles.ADMIN)) {
                ((MessageReceivedEvent) event).getMessage().delete().queue();
                ((MessageReceivedEvent) event).getAuthor().openPrivateChannel().queue(privateChannel -> {
                    privateChannel.sendMessage("Oi mate, you seem to be spamming mentions! If you keep doing this you will be banned.").queue();
                    int spamTime = spamTimes.getOrDefault(((MessageReceivedEvent) event).getAuthor().getId(), 0);
                    spamTime ++;
                    spamTimes.put(((MessageReceivedEvent) event).getAuthor().getId(), spamTime);
                    if (spamTime >= 3) {
                        // Do the ban.
                        banForSpam(((MessageReceivedEvent) event).getGuild(), ((MessageReceivedEvent) event).getAuthor(), privateChannel);
                    }
                });
            } else {
                spamTimes.remove(((MessageReceivedEvent) event).getAuthor().getId());
            }
        }
    }

    public static void banForSpam(Guild guild, User user, PrivateChannel privateChannel) {
        privateChannel.sendMessage("You have been banned for spamming. Contact " + Settings.hostUsername + "#" + Settings.hostIdentifier + " if you believe this is a mistake.")
                .queue(message -> guild.getController().ban(user, 0).queue());
    }
}
