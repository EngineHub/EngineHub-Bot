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
package org.enginehub.discord.util.command;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * An explicit substring. Provides the range from which it was taken.
 */
public final class Substring {

    /**
     * Take a substring from {@code original}, and {@link #wrap(String, int, int)} it into
     * a Substring.
     */
    public static Substring from(String original, int start) {
        return new Substring(original.substring(start), start, original.length());
    }

    /**
     * Take a substring from {@code original}, and {@link #wrap(String, int, int)} it into
     * a Substring.
     */
    public static Substring from(String original, int start, int end) {
        return new Substring(original.substring(start, end), start, end);
    }

    /**
     * Wrap the given parameters into a Substring instance.
     */
    public static Substring wrap(String substring, int start, int end) {
        checkArgument(0 <= start, "Start must be greater than or equal to zero");
        checkArgument(start <= end, "End must be greater than or equal to start");
        return new Substring(substring, start, end);
    }

    private final String substring;
    private final int start;
    private final int end;

    private Substring(String substring, int start, int end) {
        this.substring = substring;
        this.start = start;
        this.end = end;
    }

    public String getSubstring() {
        return substring;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Substring substring1 = (Substring) o;
        return start == substring1.start
            && end == substring1.end
            && substring.equals(substring1.substring);
    }

    @Override
    public int hashCode() {
        return Objects.hash(substring, start, end);
    }

    @Override
    public String toString() {
        return "Substring{"
            + "substring='" + substring + "'"
            + ",start=" + start
            + ",end=" + end
            + "}";
    }
}
