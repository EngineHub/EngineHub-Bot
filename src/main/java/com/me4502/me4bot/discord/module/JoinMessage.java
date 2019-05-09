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

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class JoinMessage implements Module, EventListener {

    @Override
    public void onEvent(Event event) {
        if (event instanceof GuildMemberJoinEvent) {
            ((GuildMemberJoinEvent) event).getUser().openPrivateChannel().queue(
                    privateChannel -> {
                        MessageBuilder builder = new MessageBuilder();
                        builder.append("Hey! Welcome to the EngineHub Discord!\n\n");
                        builder.append("Before you get started, make sure you read the rules at <#139272202431234048>.\nwizjany, one of the "
                                + "developers, can be very helpful but also is angered easily. Make sure not to ping him, and don't mind him too "
                                + "much.\n\n");
                        builder.append("If you need help, put your server log `logs/latest.log` onto http://paste.enginehub.org and provide us with"
                                + " the link in the appropriate channel.\n\n~ The EngineHub team");
                        privateChannel.sendMessage(
                                builder.build()
                        ).queue(message -> privateChannel.close().queue());
                    });
        }
    }

}
