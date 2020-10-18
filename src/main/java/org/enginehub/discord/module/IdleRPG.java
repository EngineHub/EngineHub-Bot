/*
 * Copyright (c) Me4502 (Matthew Miller)
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

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.enginehub.discord.util.BigMath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
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

    private static long getXpForLevelUpUncached(int level) {
        BigDecimal exponent = XP_POWER.multiply(BigDecimal.valueOf(level));
        return XP_FACTOR.multiply(BigMath.exp(exponent)).longValue();
    }

    private final static Gson IDLE_RPG_SERIALISER = new GsonBuilder().create();

    private final Map<Long, PlayerData> players = Maps.newConcurrentMap();

    private final Map<Integer, Long> xpCacheMap = Maps.newHashMap();

    private long getXpForLevelUp(int level) {
        if (level <= 1) {
            return 0;
        }
        return xpCacheMap.computeIfAbsent(level, IdleRPG::getXpForLevelUpUncached);
    }

    public String formatTimeTo(long timestamp) {
        // Down to seconds
        timestamp /= 1000;

        // Extract seconds
        long seconds = timestamp % 60;
        timestamp -= seconds;
        timestamp /= 60;

        // Extract minutes
        long minutes = timestamp % 60;
        timestamp -= minutes;
        timestamp /= 60;

        // Extract hours
        long hours = timestamp % 24;
        timestamp -= hours;
        timestamp /= 24;

        // Extract days
        long days = timestamp;

        StringBuilder timeBuilder = new StringBuilder();
        if (days > 0) {
            timeBuilder.append(days).append(" days ");
        }
        if (hours > 0) {
            timeBuilder.append(hours).append(" hours ");
        }
        if (minutes > 0 && days == 0) {
            timeBuilder.append(minutes).append(" minutes ");
        }
        if (seconds > 0 && hours == 0 && days == 0) {
            timeBuilder.append(seconds).append(" seconds ");
        }

        return timeBuilder.append("remaining").toString();
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (Objects.equals(event.getMessage().getContentRaw(), IDLE_RPG_TOKEN)) {
            PlayerData data = players.computeIfAbsent(event.getAuthor().getIdLong(), _l -> new PlayerData());
            if (System.currentTimeMillis() >= data.levelTime + TimeUnit.SECONDS.toMillis(getXpForLevelUp(data.level + 1))) {
                EmbedBuilder builder = createEmbed();
                builder.setAuthor("IdleRPG");
                builder.appendDescription(event.getAuthor().getAsMention() + " LEVEL UP! You are now level " + (data.level + 1) + '!');

                event.getChannel().sendMessage(builder.build()).queue();
                data.lastName = event.getAuthor().getName();
                data.levelTime = System.currentTimeMillis();
                data.level++;
                isDirty = true;
            } else {
                long diff = System.currentTimeMillis() - data.levelTime;
                long required = TimeUnit.SECONDS.toMillis(getXpForLevelUp(data.level + 1));
                String remaining = PERCENTAGE_FORMAT.format(BigDecimal
                    .valueOf(diff)
                    .setScale(5, RoundingMode.DOWN)
                    .divide(BigDecimal.valueOf(required), RoundingMode.HALF_EVEN)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.DOWN));
                EmbedBuilder builder = createEmbed();
                builder.setAuthor("IdleRPG");
                builder.appendDescription(event.getAuthor().getAsMention() + " you're " + remaining + "% of the way there! " + formatTimeTo(required - diff));
                event.getChannel().sendMessage(builder.build()).queue();
            }
        } else if (Objects.equals(event.getMessage().getContentRaw(), IDLE_RPG_LEADERBOARD_TOKEN)) {
            List<PlayerData> topPlayers = players.values()
                .stream()
                .sorted(Comparator
                    .comparingInt((PlayerData p) -> p.level)
                    .thenComparing(Comparator.comparingLong((PlayerData p) -> p.levelTime).reversed())
                    .reversed())
                .limit(10)
                .collect(Collectors.toList());
            StringBuilder leaderboardMessage = new StringBuilder("IdleRPG Leaderboard\n\n");
            for (int i = 0; i < topPlayers.size(); i++) {
                PlayerData data = topPlayers.get(i);
                boolean canLevelUp = System.currentTimeMillis() >= data.levelTime + TimeUnit.SECONDS.toMillis(getXpForLevelUp(data.level + 1));
                leaderboardMessage
                    .append('#')
                    .append(i + 1)
                    .append(' ')
                    .append(data.lastName)
                    .append(": Level ")
                    .append(data.level)
                    .append(canLevelUp ? '*' : ' ')
                    .append('\n');
            }
            leaderboardMessage.append("\n(Showing ").append(topPlayers.size()).append(" out of ").append(players.size()).append(')');

            EmbedBuilder builder = createEmbed();
            builder.setAuthor("IdleRPG");
            builder.appendDescription(leaderboardMessage.toString());

            event.getChannel().sendMessage(builder.build()).queue();
        }
    }

    private long lastSave;
    private boolean isDirty;

    @Override
    public void onTick() {
        if (lastSave + 1000 * 60 < System.currentTimeMillis()) {
            lastSave = System.currentTimeMillis();

            save();
        }
    }

    @Override
    public void onInitialise() {
        PERCENTAGE_FORMAT.setMaximumFractionDigits(2);
        PERCENTAGE_FORMAT.setMinimumFractionDigits(2);
        PERCENTAGE_FORMAT.setGroupingUsed(false);

        isDirty = false;
        lastSave = 0;
        players.clear();

        try (FileReader reader = new FileReader(new File(IDLE_RPG_FILE))) {
            Map<Long, PlayerData> map = IDLE_RPG_SERIALISER.fromJson(reader, new TypeToken<Map<Long, PlayerData>>() {
            }.getType());
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
            try (FileWriter writer = new FileWriter(new File(IDLE_RPG_FILE))) {
                IDLE_RPG_SERIALISER.toJson(players, writer);
                isDirty = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class PlayerData {
        long levelTime;
        int level;
        String lastName;
    }
}
