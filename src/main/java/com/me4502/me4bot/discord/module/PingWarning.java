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
import com.me4502.me4bot.discord.util.PermissionRoles;
import com.me4502.me4bot.discord.util.StringUtil;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class PingWarning implements Module, EventListener {

    @Override
    public void onEvent(Event event) {
        if (event instanceof MessageReceivedEvent) {
            boolean mentionsDev =
                    ((MessageReceivedEvent) event).getMessage().getMentionedMembers().stream().anyMatch(user -> Me4Bot.isAuthorised(user,
                    PermissionRoles.MODERATOR));
            if (mentionsDev && !Me4Bot.isAuthorised(((MessageReceivedEvent) event).getMember(), PermissionRoles.TRUSTED)) {
                ((MessageReceivedEvent) event).getMessage().delete().queue();
                ((MessageReceivedEvent) event).getChannel().sendMessage("Hey " + StringUtil.annotateUser(
                        ((MessageReceivedEvent) event).getMember().getUser()
                ) + "! It's against the rule to ping the developers!").queue();
            }
        }
    }
}
