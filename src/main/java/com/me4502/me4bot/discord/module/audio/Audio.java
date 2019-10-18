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

import com.me4502.me4bot.discord.module.Module;
import com.me4502.me4bot.discord.util.PermissionRoles;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.fluent.DispatcherNode;
import com.sk89q.intake.parametric.annotation.Text;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class Audio implements Module {

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
    public DispatcherNode setupCommands(DispatcherNode dispatcherNode) {
        return dispatcherNode
                .registerMethods(this);
    }

    @Command(aliases = "joinvoice", desc = "Asks the bot to join the voice channel.")
    @Require(PermissionRoles.ANY)
    public void joinVoice(Message message, Member member) {
        Optional<VoiceChannel> voiceChannelOptional = message.getGuild().getVoiceChannels().stream().filter(voiceChannel -> voiceChannel.getMembers().contains(member)).findFirst();
        if (voiceChannelOptional.isPresent()) {
            if (audioManager != null) {
                audioManager.closeAudioConnection();
            }

            audioManager = message.getGuild().getAudioManager();
            audioManager.setSendingHandler(new DiscordAudioSender(player));
            audioManager.openAudioConnection(voiceChannelOptional.get());
            audioQueue.setTextChannel(message.getTextChannel());
        } else {
            message.getChannel().sendMessage("You aren't in a voice channel!").queue();
        }
    }

    @Command(aliases = "leavevoice", desc = "Asks the bot to leave the voice channel.")
    @Require(PermissionRoles.ANY)
    public void leaveVoice() {
        if (audioManager != null) {
            audioManager.closeAudioConnection();
            audioManager = null;
        }
    }

    @Command(aliases = {"volume", "vol"}, desc = "Sets the volume of the bot.")
    @Require(PermissionRoles.MODERATOR)
    public void volume(Message message, int volume) {
        player.setVolume(volume);
        message.getChannel().sendMessage("Set volume to " + volume).queue();
    }

    @Command(aliases = {"pause"}, desc = "Pauses bot output.")
    @Require(PermissionRoles.ANY)
    public void pause(Message message) {
        player.setPaused(true);
        message.getChannel().sendMessage("Paused player").queue();
    }

    @Command(aliases = {"resume"}, desc = "Resumes bot output.")
    @Require(PermissionRoles.ANY)
    public void resume(Message message) {
        player.setPaused(false);
        message.getChannel().sendMessage("Resume player").queue();
    }

    @Command(aliases = {"skip"}, desc = "Skips the current song.")
    @Require(PermissionRoles.TRUSTED)
    public void skip() {
        audioQueue.playNext();
    }

    @Command(aliases = {"shuffle"}, desc = "Shuffles the current queue.")
    @Require(PermissionRoles.ANY)
    public void shuffle(Message message) {
        audioQueue.playNext();
        message.getChannel().sendMessage("Shuffled the queue.").queue();
    }

    @Command(aliases = {"removequeue"}, desc = "Removes an index from the queue.")
    @Require(PermissionRoles.MODERATOR)
    public void removeQueue(Message message, int index) {
        index --;
        if (index >= 0 && index < audioQueue.size()) {
            WrappedTrack track = audioQueue.remove(index);
            if (track != null) {
                message.getChannel().sendMessage("Removed " + track.getPretty() + " from the queue").queue();
            }
        } else {
            message.getChannel().sendMessage("Unknown queue index").queue();
        }
    }

    @Command(aliases = {"play"}, desc = "Plays a media source.")
    @Require(PermissionRoles.ANY)
    public void play(Message message, @Text String song) {
        playSong(message.getChannel(), song, false, true);
    }

    @Command(aliases = {"rickroll"}, desc = ";)")
    @Require(PermissionRoles.ANY)
    public void rickroll(Message message) {
        playSong(message.getChannel(), "https://www.youtube.com/watch?v=dQw4w9WgXcQ", true, false);
        message.delete().queue();
    }

    @Command(aliases = {"clear"}, desc = "Clear's the queue.")
    @Require(PermissionRoles.MODERATOR)
    public void clear(Message message) {
        audioQueue.clearQueue();
        message.getChannel().sendMessage("Cleared the queue.").queue();
    }

    @Command(aliases = {"queue"}, desc = "Outputs the queue.")
    @Require(PermissionRoles.ANY)
    public void queue(Message message) {
        StringBuilder queueMessage = new StringBuilder();
        List<String> tracks = audioQueue.getPrettyQueue();
        queueMessage.append("Current Queue: (Length of ").append(tracks.size()).append(")\n");
        int num = 1;
        for (String track : tracks) {
            if (queueMessage.length() + track.length() > 1980) {
                message.getChannel().sendMessage(queueMessage.toString()).queue();
                queueMessage = new StringBuilder();
            }
            queueMessage.append("**").append(num).append("**: ").append(track).append('\n');
            num++;
        }
        message.getChannel().sendMessage(queueMessage.toString()).queue();
    }

    @Command(aliases = {"nowplaying"}, desc = "Outputs the current song.")
    @Require(PermissionRoles.ANY)
    public void nowPlaying(Message message) {
        if (player.getPlayingTrack() != null) {
            WrappedTrack wrappedTrack = new WrappedTrack(player.getPlayingTrack());
            message.getChannel().sendMessage("Now playing: " + wrappedTrack.getPretty()).queue();
        } else {
            message.getChannel().sendMessage("Nothing is currently playing!").queue();
        }
    }

    public void playSong(MessageChannel channel, String songId, boolean upNext, boolean showMessage) {
        try {
            playerManager.loadItem(songId, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    WrappedTrack wrappedTrack = new WrappedTrack(track);
                    wrappedTrack.setShowMessage(showMessage);
                    if (showMessage) {
                        channel.sendMessage("Queued track: " + wrappedTrack.getPretty()).queue();
                    }
                    if (upNext) {
                        audioQueue.queueNext(wrappedTrack);
                    } else {
                        audioQueue.queue(wrappedTrack);
                    }
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    if (playlist.isSearchResult()) {
                        trackLoaded(playlist.getTracks().get(0));
                    } else {
                        if (showMessage) {
                            channel.sendMessage("Queued playlist: " + playlist.getName()).queue();
                        }
                        for (AudioTrack track : playlist.getTracks()) {
                            WrappedTrack wrappedTrack = new WrappedTrack(track);
                            wrappedTrack.setShowMessage(showMessage);
                            audioQueue.queue(wrappedTrack);
                        }
                    }
                }

                @Override
                public void noMatches() {
                    if (showMessage) {
                        channel.sendMessage("No song found for text: " + songId).queue();
                    }
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    if (showMessage) {
                        channel.sendMessage("Failed to load song: " + songId).queue();
                    }
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
