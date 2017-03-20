package com.me4502.me4bot.discord.module;

import com.me4502.me4bot.discord.Me4Bot;
import com.me4502.me4bot.discord.Settings;
import com.me4502.me4bot.discord.util.PermissionRoles;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.context.CommandLocals;
import com.sk89q.intake.fluent.DispatcherNode;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.TextChannel;

public class AutoErase implements Module {

    private long lastTime = 0;

    @Override
    public DispatcherNode setupCommands(DispatcherNode dispatcherNode) {
        return dispatcherNode
                .registerMethods(this);
    }

    @Override
    public void onTick() {
        if (System.currentTimeMillis() - lastTime > 1000 * 60 * 2) {
            lastTime = System.currentTimeMillis();
            for (Guild server : Me4Bot.bot.api.getGuilds()) {
                for (TextChannel channel : server.getTextChannels()) {
                    if (Settings.autoEraseChannels.contains(channel.getId())) {
                        MessageHistory history = channel.getHistory();
                        try {
                            for (Message message : history.retrievePast(100).complete(true)) {
                                if (message.isPinned()) {
                                    continue;
                                }
                                if (System.currentTimeMillis() - message.getCreationTime().toInstant().toEpochMilli() > 1000 * 60 * 10) {
                                    message.delete().queue();
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

    @Command(aliases = "autoerase", desc = "Sets the channel to auto-erase.")
    @Require(PermissionRoles.ADMIN)
    public void autoErase(Message message) {
        if (Settings.autoEraseChannels.contains(message.getChannel().getId())) {
            Settings.autoEraseChannels.remove(message.getChannel().getId());
            message.getChannel().sendMessage("Channel will no longer auto-erase!").queue();
        } else {
            Settings.autoEraseChannels.add(message.getChannel().getId());
            message.getChannel().sendMessage("Channel will now auto-erase!").queue();
        }

        Settings.save();
    }
}
