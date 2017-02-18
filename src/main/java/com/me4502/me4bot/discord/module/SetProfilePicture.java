package com.me4502.me4bot.discord.module;

import com.me4502.me4bot.discord.Me4Bot;
import net.dv8tion.jda.core.entities.Icon;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.io.File;
import java.io.IOException;

public class SetProfilePicture implements Module, EventListener {

    @Override
    public void onEvent(Event event) {
        if (event instanceof MessageReceivedEvent) {
            if (((MessageReceivedEvent) event).getMessage().getContent().equals("~setprofilepicture")) {
                if (Me4Bot.isAuthorised(((MessageReceivedEvent) event).getAuthor())) {
                    ((MessageReceivedEvent) event).getMessage().getAttachments().stream().filter(Message.Attachment::isImage).forEach(attachment -> {
                        File file = new File("avatar_cache.png");
                        attachment.download(file);
                        try {
                            Me4Bot.bot.api.getSelfUser().getManager().setAvatar(Icon.from(file)).queue(aVoid -> file.delete());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    ((MessageReceivedEvent) event).getChannel().sendMessage("Oi mate, you can't do that!").queue();
                }
            }
        }
    }
}
