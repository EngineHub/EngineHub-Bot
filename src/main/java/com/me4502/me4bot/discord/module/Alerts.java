package com.me4502.me4bot.discord.module;

import com.me4502.me4bot.discord.Me4Bot;
import com.me4502.me4bot.discord.Settings;
import com.me4502.me4bot.discord.util.PermissionRoles;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.context.CommandLocals;
import com.sk89q.intake.fluent.DispatcherNode;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import ninja.leaping.configurate.ConfigurationNode;

public class Alerts implements Module, EventListener {

    public static String alertChannel = "alerts";

    @Override
    public DispatcherNode setupCommands(DispatcherNode dispatcherNode) {
        return dispatcherNode
                .registerMethods(this);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof GuildMemberJoinEvent) {
            MessageChannel channel = Me4Bot.bot.api.getTextChannelById(alertChannel);
            if (channel != null) {
                if (((GuildMemberJoinEvent) event).getMember().getUser().isBot()) {
                    channel.sendMessage("**" + ((GuildMemberJoinEvent) event).getMember().getUser().getName() + "** (Bot) has been added to the server!").queue();
                } else {
                    channel.sendMessage("**" + ((GuildMemberJoinEvent) event).getMember().getUser().getName() + "** has joined the server!").queue();
                }
            }
        } else if (event instanceof GuildMemberLeaveEvent) {
            MessageChannel channel = Me4Bot.bot.api.getTextChannelById(alertChannel);
            if (channel != null) {
                if (((GuildMemberLeaveEvent) event).getMember().getUser().isBot()) {
                    channel.sendMessage("**" + ((GuildMemberLeaveEvent) event).getMember().getUser().getName() + "** (Bot) has been removed from the server!").queue();
                } else {
                    channel.sendMessage("**" + ((GuildMemberLeaveEvent) event).getMember().getUser().getName() + "** has left the server!").queue();
                }
            }
        }
    }

    @Command(aliases = "alert", desc = "Sets the channel to alert.")
    @Require(PermissionRoles.ADMIN)
    public void alert(Message message) {
        alertChannel = message.getChannel().getId();
        message.getChannel().sendMessage("Set alert channel!").queue();

        Settings.save();
    }

    @Override
    public void load(ConfigurationNode loadedNode) {
        alertChannel = loadedNode.getNode("alert-channel").getString(alertChannel);
    }

    @Override
    public void save(ConfigurationNode loadedNode) {
        loadedNode.getNode("alert-channel").setValue(alertChannel);
    }
}
