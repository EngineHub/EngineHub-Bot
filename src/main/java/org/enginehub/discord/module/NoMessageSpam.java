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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.enginehub.discord.EngineHubBot;
import org.enginehub.discord.util.PermissionRoles;
import org.enginehub.discord.util.PunishmentUtil;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple anti-same-message spam filter.
 */
public class NoMessageSpam extends ListenerAdapter implements Module {

    private static final class CacheKey {
        final long userId;
        final int messageHash;

        private CacheKey(long userId, int messageHash) {
            this.userId = userId;
            this.messageHash = messageHash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return userId == cacheKey.userId && messageHash == cacheKey.messageHash;
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, messageHash);
        }
    }

    // Track messages from a user for the last 1 minute.
    private final LoadingCache<CacheKey, AtomicInteger> messageCounts = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.MINUTES)
        .build(CacheLoader.from(() -> new AtomicInteger(0)));

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        // Don't check for people who are trusted not to spam
        if (EngineHubBot.isAuthorised(event.getMember(), PermissionRoles.TRUSTED)) {
            return;
        }

        var cacheKey = new CacheKey(
            event.getAuthor().getIdLong(),
            event.getMessage().getContentRaw().hashCode()
        );
        var hashCount = messageCounts.getUnchecked(cacheKey).incrementAndGet();

        // We only want one thread to run this, so use an exact equality to ensure this
        if (hashCount == 5) {
            PunishmentUtil.kickUser(event.getGuild(), event.getMember(), "Message spam");
        } else if (hashCount == 6) {
            PunishmentUtil.banUser(event.getGuild(), event.getAuthor(), "Message spam", true);
        }
    }
}
