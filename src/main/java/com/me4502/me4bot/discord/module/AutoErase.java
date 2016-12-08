package com.me4502.me4bot.discord.module;

import com.me4502.me4bot.discord.Me4Bot;
import com.me4502.me4bot.discord.Settings;
import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.message.MessageHistory;
import de.btobastian.javacord.listener.message.MessageCreateListener;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class AutoErase implements Module, MessageCreateListener {

    private long lastTime = 0;

    @Override
    public void onTick() {
        if (System.currentTimeMillis() - lastTime > 1000 * 30) {
            lastTime = System.currentTimeMillis();
            for (Server server : Me4Bot.bot.api.getServers()) {
                for (Channel channel : server.getChannels()) {
                    if (channel != null && Settings.autoEraseChannels.contains(channel.getId())) {
                        Future<MessageHistory> messageHistoryFuture = channel.getMessageHistory(100000);
                        try {
                            MessageHistory history = messageHistoryFuture.get();
                            for (Message message : history.getMessagesSorted()) {
                                if (System.currentTimeMillis() - message.getCreationDate().getTimeInMillis() > 1000 * 60 * 10) {
                                    Exception e = message.delete().get();
                                    if (e != null)
                                        e.printStackTrace();
                                } else {
                                    break;
                                }
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onMessageCreate(DiscordAPI discordAPI, Message message) {
        if (message.getContent().equals("~autoerase")) {
            if (Me4Bot.isAuthorised(message.getAuthor())) {
                if (Settings.autoEraseChannels.contains(message.getChannelReceiver().getId())) {
                    Settings.autoEraseChannels.remove(message.getChannelReceiver().getId());
                    message.getChannelReceiver().sendMessage("Channel will no longer auto-erase!");
                } else {
                    Settings.autoEraseChannels.add(message.getChannelReceiver().getId());
                    message.getChannelReceiver().sendMessage("Channel will now auto-erase!");
                }

                Settings.save();
            }
        }
    }
}
