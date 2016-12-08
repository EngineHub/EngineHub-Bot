package com.me4502.me4bot.discord.module;

import com.me4502.me4bot.discord.Me4Bot;
import com.me4502.me4bot.discord.Settings;
import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.listener.message.MessageCreateListener;
import de.btobastian.javacord.listener.server.ServerMemberAddListener;
import de.btobastian.javacord.listener.server.ServerMemberRemoveListener;

public class Alerts implements Module, ServerMemberAddListener, ServerMemberRemoveListener, MessageCreateListener {

    @Override
    public void onMessageCreate(DiscordAPI discordAPI, Message message) {
        if (message.getContent().equals("~alert")) {
            if (Me4Bot.isAuthorised(message.getAuthor())) {
                Settings.alertChannel = message.getChannelReceiver().getId();
                message.getChannelReceiver().sendMessage("Set alert channel!");

                Settings.save();
            }
        }
    }

    @Override
    public void onServerMemberAdd(DiscordAPI discordAPI, User user, Server server) {
        Channel channel = discordAPI.getChannelById(Settings.alertChannel);
        if (channel != null) {
            channel.sendMessage("**" + user.getName() + "** has joined the server!");
        }
    }

    @Override
    public void onServerMemberRemove(DiscordAPI discordAPI, User user, Server server) {
        Channel channel = discordAPI.getChannelById(Settings.alertChannel);
        if (channel != null) {
            channel.sendMessage("**" + user.getName() + "** has left the server!");
        }
    }
}
