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
