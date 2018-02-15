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
package com.me4502.me4bot.discord.module.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class WrappedTrack {

    private AudioTrack track;

    private boolean showMessage = true;

    public WrappedTrack(AudioTrack track) {
        this.track = track;
    }

    public AudioTrack getTrack() {
        return this.track;
    }

    public void setShowMessage(boolean showMessage) {
        this.showMessage = showMessage;
    }

    public boolean shouldShowMessage() {
        return this.showMessage;
    }

    public String getPretty() {
        String pretty = track.getInfo().title + " by " + track.getInfo().author;
        if (track.getDuration() < Integer.MAX_VALUE) {
            pretty += " (" + prettifyTime(track.getDuration()) + ')';
        }
        return pretty;
    }

    private static String prettifyTime(long time) {
        time /= 1000;

        long seconds = time % 60;
        long minutes = time / 60;
        long hours = minutes / 60;
        minutes %= 60;

        String format = seconds + "s";
        if (minutes > 0 || hours > 0) {
            format = minutes + "m" + format;
            if (hours > 0) {
                format = hours + "h" + format;
            }
        }

        return format;
    }
}
