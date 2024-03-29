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
package org.enginehub.discord.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

import java.awt.Color;
import java.time.Duration;
import java.time.Instant;

public class StringUtil {

    public static EmbedBuilder createEmbed() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor("EngineHub Bot", "https://github.com/EngineHub/EngineHub-Bot", "https://enginehub.org/icons/icon-256x256.png");
        builder.setColor(new Color(87, 61, 129));
        builder.setThumbnail("https://enginehub.org/icons/icon-256x256.png");
        builder.setTimestamp(Instant.now());

        return builder;
    }

    public static String formatDurationHumanReadable(Duration duration) {
        if (duration.isZero() || duration.isNegative()) {
            return "No time left";
        }

        StringBuilder timeBuilder = new StringBuilder();
        var days = duration.toDaysPart();
        if (days > 0) {
            appendDurationPart(timeBuilder, days, "day");
        }
        var hours = duration.toHoursPart();
        if (hours > 0) {
            appendDurationPart(timeBuilder, hours, "hour");
        }
        if (days == 0) {
            var minutes = duration.toMinutesPart();
            if (minutes > 0) {
                appendDurationPart(timeBuilder, minutes, "minute");
            }
            var seconds = duration.toSecondsPart();
            if (hours == 0 && seconds > 0) {
                appendDurationPart(timeBuilder, seconds, "second");
            }
        }

        return timeBuilder.append("remaining").toString();
    }


    private static void appendDurationPart(StringBuilder builder, long part, String base) {
        builder.append(part).append(" ").append(base);
        if (part != 1) {
            builder.append("s");
        }
        builder.append(" ");
    }

    public static MessageCreateAction attachMessageReference(MessageCreateAction action, Message reference) {
        if (reference != null) {
            return action.setMessageReference(reference);
        }
        return action;
    }
}
