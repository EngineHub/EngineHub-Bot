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
package org.enginehub.discord.util;

import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;

public class StringUtilTest {
    @Test
    public void testDurationFormattingNegative() {
        assertEquals(
            "No time left",
            StringUtil.formatDurationHumanReadable(Duration.ofSeconds(-1))
        );
    }

    @Test
    public void testDurationFormattingZero() {
        assertEquals(
            "No time left",
            StringUtil.formatDurationHumanReadable(Duration.ZERO)
        );
    }

    @Test
    public void testDurationFormattingDays() {
        assertEquals(
            "5 days remaining",
            StringUtil.formatDurationHumanReadable(Duration.ofDays(5))
        );
    }

    @Test
    public void testDurationFormattingHours() {
        assertEquals(
            "5 hours remaining",
            StringUtil.formatDurationHumanReadable(Duration.ofHours(5))
        );
    }

    @Test
    public void testDurationFormattingMinutes() {
        assertEquals(
            "5 minutes remaining",
            StringUtil.formatDurationHumanReadable(Duration.ofMinutes(5))
        );
    }

    @Test
    public void testDurationFormattingSeconds() {
        assertEquals(
            "5 seconds remaining",
            StringUtil.formatDurationHumanReadable(Duration.ofSeconds(5))
        );
    }

    @Test
    public void testDurationFormattingMinutesAndSeconds() {
        assertEquals(
            "7 minutes 5 seconds remaining",
            StringUtil.formatDurationHumanReadable(Duration.parse("PT7M5S"))
        );
    }

    @Test
    public void testDurationFormattingHoursAndMinutes() {
        assertEquals(
            "7 hours 5 minutes remaining",
            StringUtil.formatDurationHumanReadable(Duration.parse("PT7H5M"))
        );
        // seconds are dropped:
        assertEquals(
            "7 hours 5 minutes remaining",
            StringUtil.formatDurationHumanReadable(Duration.parse("PT7H5M3S"))
        );
    }

    @Test
    public void testDurationFormattingDaysAndHours() {
        assertEquals(
            "7 days 5 hours remaining",
            StringUtil.formatDurationHumanReadable(Duration.parse("P7DT5H"))
        );
        // minutes are dropped:
        assertEquals(
            "7 days 5 hours remaining",
            StringUtil.formatDurationHumanReadable(Duration.parse("P7DT5H3M"))
        );
        // minutes, seconds are dropped:
        assertEquals(
            "7 days 5 hours remaining",
            StringUtil.formatDurationHumanReadable(Duration.parse("P7DT5H3M1S"))
        );
    }

    @Test
    public void testSingular() {
        assertEquals(
            "1 day remaining",
            StringUtil.formatDurationHumanReadable(Duration.ofDays(1))
        );
        assertEquals(
            "1 hour remaining",
            StringUtil.formatDurationHumanReadable(Duration.ofHours(1))
        );
        assertEquals(
            "1 minute remaining",
            StringUtil.formatDurationHumanReadable(Duration.ofMinutes(1))
        );
        assertEquals(
            "1 second remaining",
            StringUtil.formatDurationHumanReadable(Duration.ofSeconds(1))
        );
    }
}
