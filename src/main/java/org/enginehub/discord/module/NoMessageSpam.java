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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ninja.leaping.configurate.ConfigurationNode;
import org.enginehub.discord.EngineHubBot;
import org.enginehub.discord.util.PermissionRoles;
import org.enginehub.discord.util.PunishmentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * A simple anti-same-message spam filter.
 */
public class NoMessageSpam extends ListenerAdapter implements Module {

    private record CacheKey(long userId, int messageHash) { }

    // Track messages from a user for the last 1 minute.
    private final LoadingCache<CacheKey, AtomicInteger> messageCounts = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.MINUTES)
        .build(CacheLoader.from(() -> new AtomicInteger(0)));
    private final HashSet<Long> hasPingedBefore = new HashSet<>();
    private volatile Set<Long> guildsForPunish;

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        // Don't check for people who are trusted not to spam
        if (EngineHubBot.isAuthorised(event.getMember(), PermissionRoles.TRUSTED)) {
            return;
        }

        var contentRaw = event.getMessage().getContentRaw();
        var cacheKey = new CacheKey(
            event.getAuthor().getIdLong(),
            contentRaw.hashCode()
        );
        var hashCount = messageCounts.getUnchecked(cacheKey).incrementAndGet();

        if (contentRaw.length() < 10) {
            // This is unlikely to be a "true" spam message. Only kick people if they repeat it
            // an unrealistic amount (given our 1 minute counter, more than 10/12 is likely spam)
            hashCount /= 2;
        }

        // We only want one thread to run this, so use an exact equality to ensure this
        if (hashCount == 5) {
            PunishmentUtil.kickUser(event.getGuild(), event.getMember(), "Message spam");
        } else if (hashCount == 6) {
            PunishmentUtil.banUser(event.getGuild(), event.getAuthor(), "Message spam", true);
        }
    }

    @Override
    public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {
        if (!(event.getMessage().mentionsEveryone() || event.getMessage().getContentRaw().contains("@everyone"))) {
            return;
        }

        if (hasPingedBefore.add(event.getAuthor().getIdLong())) {
            // They haven't pinged before, kick.
            punishForAtEveryone(event, PunishmentUtil::kickUser);
        } else {
            // It's ban time.
            punishForAtEveryone(
                event,
                (guild, member, reason) -> PunishmentUtil.banUser(guild, member.getUser(), reason, false)
            );
        }
    }

    // TODO: Consider extracting out?
    private interface Punishment {
        void enact(Guild guild, Member member, String reason);
    }

    private void punishForAtEveryone(@NotNull PrivateMessageReceivedEvent event, Punishment punishment) {
        for (long guildId : guildsForPunish) {
            Guild guild = event.getJDA().getGuildById(guildId);
            if (guild == null) {
                System.err.println("Warning, guild " + guildId + " does not appear to be known to this bot.");
                continue;
            }
            Member member = guild.getMember(event.getAuthor());
            if (member != null) {
                punishment.enact(guild, member, "Attempting to ping everyone in DMs");
            }
        }
    }

    @Override
    public void load(ConfigurationNode loadedNode) {
        guildsForPunish = loadedNode.getNode("default-punishment-guilds").getChildrenList().stream()
            .map(ConfigurationNode::getLong)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void save(ConfigurationNode loadedNode) {
        loadedNode.getNode("default-punishment-guilds").setValue(guildsForPunish);
    }
}
