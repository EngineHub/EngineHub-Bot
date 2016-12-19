package com.me4502.me4bot.discord.module.audio;

import com.me4502.me4bot.discord.module.Module;
import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class Audio implements Module, EventListener {

    private AudioPlayerManager playerManager;
    private AudioPlayer player;
    private AudioManager audioManager;

    private AudioQueue audioQueue;

    @Override
    public void onInitialise() {
        playerManager = new DefaultAudioPlayerManager();

        playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        playerManager.registerSourceManager(new Me45028BitSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new SoundCloudAudioSourceManager());
        playerManager.registerSourceManager(new HttpAudioSourceManager());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());

        player = playerManager.createPlayer();
        audioQueue = new AudioQueue(player);
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
                    audioQueue.setTextChannel(((MessageReceivedEvent) event).getTextChannel());
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
            } else if (message.equals("~skip")) {
                audioQueue.playNext();
            } else if (message.equals("~shuffle")) {
                audioQueue.shuffle();
            } else if (message.equals("~queue")) {
                StringBuilder queueMessage = new StringBuilder();
                List<String> tracks = audioQueue.getPrettyQueue();
                queueMessage.append("Current Queue: (Length of ").append(tracks.size()).append(")\n");
                int num = 1;
                for (String track : tracks) {
                    if (queueMessage.length() + track.length() > 1980) {
                        ((MessageReceivedEvent) event).getChannel().sendMessage(queueMessage.toString()).queue();
                        queueMessage = new StringBuilder();
                    }
                    queueMessage.append("**").append(num).append("**: ").append(track).append('\n');
                    num++;
                }
                ((MessageReceivedEvent) event).getChannel().sendMessage(queueMessage.toString()).queue();
            } else if (message.equals("~clear")) {
                audioQueue.clearQueue();
            } else if (message.startsWith("~play ")) {
                String songId = message.substring(6);
                try {
                    playerManager.loadItem(songId, new AudioLoadResultHandler() {
                        @Override
                        public void trackLoaded(AudioTrack track) {
                            ((MessageReceivedEvent) event).getChannel().sendMessage("Queued track: " + AudioQueue.prettify(track)).queue();
                            audioQueue.queue(track);
                        }

                        @Override
                        public void playlistLoaded(AudioPlaylist playlist) {
                            if (playlist.isSearchResult()) {
                                trackLoaded(playlist.getTracks().get(0));
                            } else {
                                ((MessageReceivedEvent) event).getChannel().sendMessage("Queued playlist: " + playlist.getName()).queue();
                                for (AudioTrack track : playlist.getTracks()) {
                                    audioQueue.queue(track);
                                }
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
