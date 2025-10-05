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

import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.enginehub.discord.EngineHubBot;
import org.enginehub.discord.util.PermissionRole;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A simple anti-same-message spam filter.
 */
public class NoHello extends ListenerAdapter implements Module {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Za-z0-9]");

    private static final Set<String> bannedPhrases = Set.of(
            "hello",
            "hi",
            "hey"
    );

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        // Don't check for people who are trusted not to spam
        if (EngineHubBot.isAuthorised(event.getMember(), PermissionRole.TRUSTED)) {
            return;
        }

        if (event.getChannel() instanceof GuildChannel) {
            // replace non-alphanumeric characters with nothing, and make it lowercase
            var message = event.getMessage().getContentRaw();
            var cleaned = NON_ALPHANUMERIC.matcher(message).replaceAll("").toLowerCase();

            if (bannedPhrases.contains(cleaned) && !event.getMessage().getAttachments().isEmpty()) {
                var reply = EngineHubBot.bot.getModuleByType(LinkGrabber.class).map(linkGrabber -> linkGrabber.mapAlias("hello")).get();
                event.getMessage().reply(reply).queue();
            }
        }
    }

}
