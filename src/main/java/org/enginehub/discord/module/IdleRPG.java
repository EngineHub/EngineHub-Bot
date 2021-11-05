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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Maps;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.enginehub.discord.util.BigMath;
import org.enginehub.discord.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import static org.enginehub.discord.util.StringUtil.createEmbed;

public class IdleRPG extends ListenerAdapter implements Module {

    private static final String IDLE_RPG_TOKEN = ">";
    private static final String IDLE_RPG_LEADERBOARD_TOKEN = ">l";
    private static final String IDLE_RPG_FILE = "idlerpg_data.json";
    private static final BigDecimal XP_FACTOR = new BigDecimal("681.19");
    private static final BigDecimal XP_POWER = new BigDecimal("0.0991");
    private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat();

    @VisibleForTesting
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModules(new Jdk8Module(), new JavaTimeModule(), new ParameterNamesModule())
        .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
        .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
    private static final TypeReference<Map<Long, PlayerData>> PLAYER_DATA_MAP_TYPE =
        new TypeReference<>() {
        };

    private static Duration getXpForLevelUpUncached(int level) {
        BigDecimal exponent = XP_POWER.multiply(BigDecimal.valueOf(level));
        return Duration.ofSeconds(XP_FACTOR.multiply(BigMath.exp(exponent)).longValue());
    }

    private final Map<Long, PlayerData> players = Maps.newConcurrentMap();
    private final Map<Integer, Duration> xpCacheMap = Maps.newHashMap();
    private Instant nextSave;
    private volatile boolean isDirty;

    private TemporalAmount getXpForLevelUp(int level) {
        if (level <= 1) {
            return Duration.ZERO;
        }
        return xpCacheMap.computeIfAbsent(level, IdleRPG::getXpForLevelUpUncached);
    }

    private Instant getLevelUpInstant(PlayerData data) {
        return data.getLevelTime().plus(getXpForLevelUp(data.getLevel() + 1));
    }

    private PlayerData getPlayerData(@Nonnull MessageReceivedEvent event) {
        return players.computeIfAbsent(
            event.getAuthor().getIdLong(),
            _l -> new PlayerData(Instant.EPOCH, 0, event.getAuthor().getName())
        );
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        String[] commandArguments = event.getMessage().getContentRaw().split(" ");
        if (Objects.equals(event.getMessage().getContentRaw(), IDLE_RPG_TOKEN)) {
            tryLevelUpgrade(event);
        } else if ((commandArguments.length == 1 || commandArguments.length == 2)
                && Objects.equals(commandArguments[0], IDLE_RPG_LEADERBOARD_TOKEN)) {
            listLeaderboard(event, commandArguments);
        }
    }

    private void tryLevelUpgrade(@Nonnull MessageReceivedEvent event) {
        PlayerData data = getPlayerData(event);
        var now = Instant.now();
        var levelUpTime = getLevelUpInstant(data);
        if (now.isAfter(levelUpTime)) {
            var postLevelUp = data.applyLevelUp(now, event.getAuthor().getName());
            EmbedBuilder builder = createEmbed();
            builder.setAuthor("IdleRPG");
            builder.appendDescription(event.getAuthor().getAsMention()
                + " LEVEL UP! You are now level "
                + postLevelUp.getLevel()
                + '!'
            );

            event.getChannel().sendMessage(builder.build()).queue();
            players.put(event.getAuthor().getIdLong(), postLevelUp);
            isDirty = true;
        } else {
            var durationUntil = Duration.between(now, levelUpTime);
            var fullDuration = Duration.between(data.getLevelTime(), levelUpTime);
            var percentRemaining = BigDecimal.valueOf(fullDuration.minus(durationUntil).toMillis())
                .divide(BigDecimal.valueOf(fullDuration.toMillis()), MathContext.DECIMAL128)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.DOWN);
            EmbedBuilder builder = createEmbed();
            builder.setAuthor("IdleRPG");
            builder.appendDescription(
                event.getAuthor().getAsMention()
                    + " you're "
                    + PERCENTAGE_FORMAT.format(percentRemaining)
                    + "% of the way there! "
                    + StringUtil.formatDurationHumanReadable(durationUntil));
            event.getChannel().sendMessage(builder.build()).queue();
        }
    }

    private void listLeaderboard(@Nonnull MessageReceivedEvent event, String[] commandArguments) {
        int page = 1;
        if (commandArguments.length == 2) {
            try {
                page = Integer.parseUnsignedInt(commandArguments[1]);
                if (page == 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                EmbedBuilder builder = createEmbed();
                builder.setAuthor("IdleRPG");
                builder.appendDescription(
                    event.getAuthor().getAsMention()
                        + " that's not a valid page number!");
                event.getChannel().sendMessage(builder.build()).queue();
                return;
            }
        }
        List<PlayerData> topPlayers = players.values()
            .stream()
            .sorted(Comparator.<PlayerData>naturalOrder().reversed())
            .limit(10)
            .skip((page - 1) * 10L)
            .collect(Collectors.toList());
        var now = Instant.now();
        StringBuilder leaderboardMessage = new StringBuilder("IdleRPG Leaderboard (Page " + page + ")\n\n");
        for (int i = 0; i < topPlayers.size(); i++) {
            PlayerData data = topPlayers.get(i);
            boolean canLevelUp = now.isAfter(getLevelUpInstant(data));
            leaderboardMessage
                .append('#')
                .append(i + 1)
                .append(' ')
                .append(data.getLastName())
                .append(": Level ")
                .append(data.getLevel())
                .append(canLevelUp ? '*' : ' ')
                .append('\n');
        }
        leaderboardMessage.append("\n(Showing ").append(topPlayers.size()).append(" out of ").append(players.size()).append(')');

        EmbedBuilder builder = createEmbed();
        builder.setAuthor("IdleRPG");
        builder.appendDescription(leaderboardMessage.toString());

        event.getChannel().sendMessage(builder.build()).queue();
    }

    @Override
    public void onTick() {
        if (Instant.now().isAfter(nextSave)) {
            nextSave = Instant.now().plus(1, ChronoUnit.MINUTES);

            save();
        }
    }

    @Override
    public void onInitialise() {
        PERCENTAGE_FORMAT.setMaximumFractionDigits(2);
        PERCENTAGE_FORMAT.setMinimumFractionDigits(2);
        PERCENTAGE_FORMAT.setGroupingUsed(false);

        isDirty = false;
        nextSave = Instant.EPOCH;
        players.clear();

        try{
            Map<Long, PlayerData> map = OBJECT_MAPPER.readValue(
                new File(IDLE_RPG_FILE), PLAYER_DATA_MAP_TYPE
            );
            if (map != null) {
                players.putAll(map);
            }
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onShutdown() {
        save();
    }

    public void save() {
        if (isDirty) {
            try {
                OBJECT_MAPPER.writeValue(new File(IDLE_RPG_FILE), players);
                isDirty = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static final class PlayerData implements Comparable<PlayerData> {
        private final Instant levelTime;
        private final int level;
        private final String lastName;

        @JsonCreator
        public PlayerData(Instant levelTime, int level, String lastName) {
            this.levelTime = levelTime;
            this.level = level;
            this.lastName = lastName;
        }

        public Instant getLevelTime() {
            return levelTime;
        }

        public int getLevel() {
            return level;
        }

        public String getLastName() {
            return lastName;
        }

        public PlayerData applyLevelUp(Instant now, String name) {
            return new PlayerData(now, level + 1, name);
        }

        @Override
        public int compareTo(@NotNull IdleRPG.PlayerData o) {
            return ComparisonChain.start()
                .compare(level, o.level)
                // purposefully backwards
                .compare(o.levelTime, levelTime)
                .result();
        }
    }
}
