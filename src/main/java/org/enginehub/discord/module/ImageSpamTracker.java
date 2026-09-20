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

import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.math.PairedStatsAccumulator;
import com.google.common.math.StatsAccumulator;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.DoubleBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/// Tracks images sent by users and detects if a user is sending the same image repeatedly within a short time frame.
final class ImageSpamTracker {

    /// Tracking window for similar images.
    private static final Duration WINDOW = Duration.ofMinutes(1);
    /// How many messages with similar images must be sent within the window to trigger a spam detection.
    private static final int TRIGGER_MESSAGES = 3;
    /// The similarity threshold for two images to be considered similar.
    private static final double SIMILARITY_THRESHOLD = 0.999;
    /// The size of the fingerprint used to compare images. A larger size will be more accurate but slower.
    private static final int FINGERPRINT_SIZE = 32;
    /// The maximum number of entries to keep in the tracker.
    private static final int MAX_ENTRIES = 16 * 1024;

    private record Entry(long userId, long messageId, DoubleBuffer fingerprint) {
    }

    private final Set<Entry> entries;

    ImageSpamTracker(InstantSource clock) {
        this.entries = Collections.newSetFromMap(
            CacheBuilder.newBuilder()
                .expireAfterWrite(WINDOW.toNanos(), TimeUnit.NANOSECONDS)
                .maximumSize(MAX_ENTRIES)
                .ticker(new Ticker() {
                    @Override
                    public long read() {
                        Instant now = clock.instant();
                        return TimeUnit.SECONDS.toNanos(now.getEpochSecond()) + now.getNano();
                    }
                })
                .<Entry, Boolean>build()
                .asMap()
        );
    }

    synchronized boolean isSpammedImage(long userId, long messageId, BufferedImage image) {
        DoubleBuffer fingerprint = fingerprint(image);
        if (fingerprint == null) {
            return false;
        }

        int repeatedImageMessageCount = getRepeatedImageMessageCount(userId, messageId, fingerprint);
        // Add the new entry after counting repeated images to avoid counting the new message as a repeat
        entries.add(new Entry(userId, messageId, fingerprint));

        return repeatedImageMessageCount >= TRIGGER_MESSAGES;
    }

    private int getRepeatedImageMessageCount(long userId, long messageId, DoubleBuffer fingerprint) {
        // Although we're doing set-like things, the number of messages is small enough that a linear search is faster.
        var messageIds = new ArrayList<Long>();
        messageIds.add(messageId);
        for (Entry entry : entries) {
            if (entry.userId() != userId) {
                continue;
            }
            // Avoid spending effort flagging messages that have already been flagged
            if (messageIds.contains(entry.messageId())) {
                continue;
            }
            if (similarity(fingerprint, entry.fingerprint()) >= SIMILARITY_THRESHOLD) {
                messageIds.add(entry.messageId());
            }
        }
        return messageIds.size();
    }

    private static double similarity(DoubleBuffer a, DoubleBuffer b) {
        var stats = new PairedStatsAccumulator();
        for (int i = 0; i < a.limit(); i++) {
            stats.add(a.get(i), b.get(i));
        }
        return stats.pearsonsCorrelationCoefficient();
    }

    /// Generate a fingerprint for the given image, which can be used with [#similarity(DoubleBuffer, DoubleBuffer)] to
    /// determine if two images are similar. Returns `null` if the image is a solid color.
    ///
    /// @param source the image to fingerprint
    /// @return a fingerprint of the image, or `null` if not fingerprintable
    private static @Nullable DoubleBuffer fingerprint(BufferedImage source) {
        BufferedImage scaled = scaleAndCompositeOverWhite(source);

        double[] cells = new double[FINGERPRINT_SIZE * FINGERPRINT_SIZE];
        var stats = new StatsAccumulator();
        int count = 0;
        for (int i : scaled.getRGB(0, 0, FINGERPRINT_SIZE, FINGERPRINT_SIZE, null, 0, FINGERPRINT_SIZE)) {
            double luma = getLuma(i);
            cells[count++] = luma;
            stats.add(luma);
        }
        return stats.populationVariance() == 0 ? null : DoubleBuffer.wrap(cells).asReadOnlyBuffer();
    }

    private static BufferedImage scaleAndCompositeOverWhite(BufferedImage source) {
        var scaled = new BufferedImage(FINGERPRINT_SIZE, FINGERPRINT_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // Set white and fill rather than use bgColor overloads of `drawImage` because it does the bgColor prior to
        // scaling, which is very slow.
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, FINGERPRINT_SIZE, FINGERPRINT_SIZE);
        graphics.drawImage(source, 0, 0, FINGERPRINT_SIZE, FINGERPRINT_SIZE, null);
        graphics.dispose();
        return scaled;
    }

    /// {@return the luma of the color}
    ///
    /// @param rgb the color in RGB format
    private static double getLuma(int rgb) {
        double red = (rgb >> 16) & 0xFF;
        double green = (rgb >> 8) & 0xFF;
        double blue = rgb & 0xFF;
        return 0.299 * red + 0.587 * green + 0.114 * blue;
    }
}
