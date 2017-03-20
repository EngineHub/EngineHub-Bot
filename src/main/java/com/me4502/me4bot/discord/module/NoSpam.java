package com.me4502.me4bot.discord.module;

import com.me4502.me4bot.discord.Me4Bot;
import com.me4502.me4bot.discord.util.PermissionRoles;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.util.HashMap;

public class NoSpam implements Module, EventListener {

    private final HashMap<String, Integer> spamTimes = new HashMap<>();

    @Override
    public void onEvent(Event event) {
        if (event instanceof MessageReceivedEvent) {
            int mentionCount = ((MessageReceivedEvent) event).getMessage().getMentionedUsers().size()
                    + ((MessageReceivedEvent) event).getMessage().getMentionedRoles().size();
            if (mentionCount >= 6 && !Me4Bot.isAuthorised(((MessageReceivedEvent) event).getMember(), PermissionRoles.ADMIN)) {
                ((MessageReceivedEvent) event).getMessage().delete().queue();
                ((MessageReceivedEvent) event).getAuthor().openPrivateChannel().queue(privateChannel -> {
                    privateChannel.sendMessage("Oi mate, you seem to be spamming mentions! If you keep doing this you will be banned.").queue();
                    int spamTime = spamTimes.getOrDefault(((MessageReceivedEvent) event).getAuthor().getId(), 0);
                    spamTime ++;
                    spamTimes.put(((MessageReceivedEvent) event).getAuthor().getId(), spamTime);
                    if (spamTime >= 3) {
                        // Do the ban.
                        privateChannel.sendMessage("You have been banned for spamming. Contact Me4502#3758 if you believe this is a mistake.")
                                .queue(message -> ((MessageReceivedEvent) event).getGuild().getController().ban(((MessageReceivedEvent) event).getAuthor(), 0).queue());
                    }
                });
            } else {
                spamTimes.remove(((MessageReceivedEvent) event).getAuthor().getId());
            }
        }
    }
}
