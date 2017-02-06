package com.me4502.me4bot.discord.module.audio;

import com.google.common.collect.Queues;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class AudioQueue extends AudioEventAdapter {

    private Queue<WrappedTrack> tracks = Queues.newArrayDeque();
    private AudioPlayer player;
    private TextChannel textChannel;

    public AudioQueue(AudioPlayer player) {
        this.player = player;
        this.player.addListener(this);
    }

    public void queue(WrappedTrack track) {
        tracks.offer(track);

        if (this.player.getPlayingTrack() == null) {
            playNext();
        }
    }

    public void queueNext(WrappedTrack track) {
        List<WrappedTrack> trackTemp = new ArrayList<>(tracks);
        trackTemp.add(0, track);
        tracks.clear();
        for (WrappedTrack oldTrack : trackTemp) {
            tracks.offer(oldTrack);
        }

        if (this.player.getPlayingTrack() == null) {
            playNext();
        }
    }

    public WrappedTrack remove(int index) {
        List<WrappedTrack> trackTemp = new ArrayList<>(tracks);
        WrappedTrack removed = trackTemp.remove(index);
        tracks.clear();
        for (WrappedTrack track : trackTemp) {
            tracks.offer(track);
        }
        return removed;
    }

    public void setTextChannel(TextChannel textChannel) {
        this.textChannel = textChannel;
    }

    public TextChannel getTextChannel() {
        return textChannel;
    }

    public void playNext() {
        if (!tracks.isEmpty()) {
            WrappedTrack track = tracks.poll();
            this.player.playTrack(track.getTrack());
            if (track.shouldShowMessage()) {
                textChannel.sendMessage("Playing track: " + track.getPretty()).queue();
            }
        } else if (this.player.getPlayingTrack() != null) {
            this.player.stopTrack();
        }
    }

    public void clearQueue() {
        tracks.clear();
        playNext();
        textChannel.sendMessage("Queue cleared!").queue();
    }

    public List<String> getPrettyQueue() {
        return tracks.stream().map(WrappedTrack::getPretty).collect(Collectors.toList());
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        super.onTrackEnd(player, track, endReason);
        if (endReason == AudioTrackEndReason.FINISHED) {
            playNext();
        } else if (endReason == AudioTrackEndReason.LOAD_FAILED) {
            textChannel.sendMessage("Encountered an error playing " + new WrappedTrack(track).getPretty() + '.');
            playNext();
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        super.onTrackException(player, track, exception);

        textChannel.sendMessage("Encountered an error playing " + new WrappedTrack(track).getPretty() + ". " + exception.getMessage());
        playNext();
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        super.onTrackStuck(player, track, thresholdMs);

        textChannel.sendMessage("Got stuck whilst playing " + new WrappedTrack(track).getPretty() + ". Skipping.");
        playNext();
    }

    public void shuffle() {
        List<WrappedTrack> trackTemp = new ArrayList<>(tracks);
        Collections.shuffle(trackTemp, ThreadLocalRandom.current());
        tracks.clear();
        for (WrappedTrack track : trackTemp) {
            tracks.offer(track);
        }
    }

    public int size() {
        return tracks.size();
    }
}
