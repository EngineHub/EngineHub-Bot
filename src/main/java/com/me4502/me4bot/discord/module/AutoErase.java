package com.me4502.me4bot.discord.module;

import com.me4502.me4bot.discord.Me4Bot;
import com.me4502.me4bot.discord.Settings;
import net.dv8tion.jda.core.MessageHistory;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class AutoErase implements Module, EventListener {

    private long lastTime = 0;

    @Override
    public void onTick() {
        if (System.currentTimeMillis() - lastTime > 1000 * 60 * 2) {
            lastTime = System.currentTimeMillis();
            for (Guild server : Me4Bot.bot.api.getGuilds()) {
                for (TextChannel channel : server.getTextChannels()) {
                    if (Settings.autoEraseChannels.contains(channel.getId())) {
                        MessageHistory history = channel.getHistory();
                        try {
                            for (Message message : history.retrievePast(100).block()) {
                                if (message.getContent().startsWith("~nodelete") && Me4Bot.isAuthorised(message.getAuthor())) {
                                    continue;
                                }
                                if (System.currentTimeMillis() - message.getCreationTime().toInstant().toEpochMilli() > 1000 * 60 * 10) {
                                    message.deleteMessage().queue();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof MessageReceivedEvent) {
            if (((MessageReceivedEvent) event).getMessage().getContent().equals("~autoerase")) {
                if (Me4Bot.isAuthorised(((MessageReceivedEvent) event).getAuthor())) {
                    if (Settings.autoEraseChannels.contains(((MessageReceivedEvent) event).getChannel().getId())) {
                        Settings.autoEraseChannels.remove(((MessageReceivedEvent) event).getChannel().getId());
                        ((MessageReceivedEvent) event).getChannel().sendMessage("Channel will no longer auto-erase!").queue();
                    } else {
                        Settings.autoEraseChannels.add(((MessageReceivedEvent) event).getChannel().getId());
                        ((MessageReceivedEvent) event).getChannel().sendMessage("Channel will now auto-erase!").queue();
                    }

                    Settings.save();
                }
            }
        }
    }
}
