package com.me4502.me4bot.discord.module;

import com.me4502.me4bot.discord.Me4Bot;
import com.me4502.me4bot.discord.Settings;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class Alerts implements Module, EventListener {

    @Override
    public void onEvent(Event event) {
        if (event instanceof MessageReceivedEvent) {
            System.out.println("Received message: " + ((MessageReceivedEvent) event).getMessage().getContent());

            if (((MessageReceivedEvent) event).getMessage().getContent().equals("~alert")) {
                if (Me4Bot.isAuthorised(((MessageReceivedEvent) event).getMessage().getAuthor())) {
                    Settings.alertChannel = ((MessageReceivedEvent) event).getChannel().getId();
                    ((MessageReceivedEvent) event).getChannel().sendMessage("Set alert channel!").queue();

                    Settings.save();
                }
            }
        } else if (event instanceof GuildMemberJoinEvent) {
            MessageChannel channel = Me4Bot.bot.api.getTextChannelById(Settings.alertChannel);
            if (channel != null) {
                channel.sendMessage("**" + ((GuildMemberJoinEvent) event).getMember().getUser().getName() + "** has joined the server!").queue();
            }
        } else if (event instanceof GuildMemberLeaveEvent) {
            MessageChannel channel = Me4Bot.bot.api.getTextChannelById(Settings.alertChannel);
            if (channel != null) {

                channel.sendMessage("**" + ((GuildMemberLeaveEvent) event).getMember().getUser().getName() + "** has left the server!").queue();
            }
        }
    }
}
