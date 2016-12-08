package com.me4502.me4bot.discord.module.audio;

import com.me4502.me4bot.discord.module.Module;
import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class Audio implements Module, EventListener {

    private AudioPlayerManager playerManager;
    private AudioPlayer player;
    private AudioManager audioManager;

    @Override
    public void onInitialise() {
        playerManager = new DefaultAudioPlayerManager();

        playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());

        player = playerManager.createPlayer();
    }

    @Override
    public void onShutdown() {
        if (audioManager != null) {
            audioManager.closeAudioConnection();
            audioManager = null;
        }
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof MessageReceivedEvent) {
            User author = ((MessageReceivedEvent) event).getAuthor();
            Member authorMember = ((MessageReceivedEvent) event).getGuild().getMember(author);
            String message = ((MessageReceivedEvent) event).getMessage().getContent();
            if (message.equals("~joinvoice")) {
                Optional<VoiceChannel> voiceChannelOptional = ((MessageReceivedEvent) event).getGuild().getVoiceChannels().stream().filter(voiceChannel -> voiceChannel.getMembers().contains(authorMember)).findFirst();
                if (voiceChannelOptional.isPresent()) {
                    if (audioManager != null) {
                        audioManager.closeAudioConnection();
                    }

                    audioManager = ((MessageReceivedEvent) event).getGuild().getAudioManager();
                    audioManager.setSendingHandler(new DiscordAudioSender(player));
                    audioManager.openAudioConnection(voiceChannelOptional.get());
                } else {
                    ((MessageReceivedEvent) event).getChannel().sendMessage("You aren't in a voice channel!").queue();
                }
            } else if (message.equals("~leavevoice")) {
                if (audioManager != null) {
                    audioManager.closeAudioConnection();
                    audioManager = null;
                }
            } else if (message.startsWith("~volume")) {
                String volumeString = message.substring(8);
                int volume = 100;
                try {
                    volume = Integer.parseInt(volumeString);
                } catch (Exception e) {
                }
                player.setVolume(volume);
                ((MessageReceivedEvent) event).getChannel().sendMessage("Set volume to " + volume).queue();
            } else if (message.equals("~pause")) {
                player.setPaused(true);
                ((MessageReceivedEvent) event).getChannel().sendMessage("Paused player").queue();
            } else if (message.equals("~resume")) {
                player.setPaused(false);
                ((MessageReceivedEvent) event).getChannel().sendMessage("Resume player").queue();
            } else if (message.startsWith("~play ")) {
                String songId = message.substring(6);
                try {
                    playerManager.loadItem(songId, new AudioLoadResultHandler() {
                        @Override
                        public void trackLoaded(AudioTrack track) {
                            ((MessageReceivedEvent) event).getChannel().sendMessage("Playing track: " + songId).queue();
                            player.playTrack(track);
                        }

                        @Override
                        public void playlistLoaded(AudioPlaylist playlist) {
                            ((MessageReceivedEvent) event).getChannel().sendMessage("Playing playlist: " + songId).queue();
                            for (AudioTrack track : playlist.getTracks()) {
                                player.playTrack(track);
                            }
                        }

                        @Override
                        public void noMatches() {
                            ((MessageReceivedEvent) event).getChannel().sendMessage("No song found for text: " + songId).queue();
                        }

                        @Override
                        public void loadFailed(FriendlyException exception) {
                            ((MessageReceivedEvent) event).getChannel().sendMessage("Failed to load song: " + songId).queue();
                        }
                    }).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
