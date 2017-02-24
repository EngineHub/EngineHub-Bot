package com.me4502.me4bot.discord.module;

import com.me4502.me4bot.discord.Me4Bot;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoSpam implements Module, EventListener {

    private static final Pattern AT_PATTERN = Pattern.compile("@", Pattern.LITERAL);

    private final HashMap<String, Integer> spamTimes = new HashMap<>();

    @Override
    public void onEvent(Event event) {
        if (event instanceof MessageReceivedEvent) {
            String content = ((MessageReceivedEvent) event).getMessage().getContent();
            int mentionCount = content.length() - AT_PATTERN.matcher(content).replaceAll(Matcher.quoteReplacement("")).length();
            if (mentionCount >= 6 && !Me4Bot.isAuthorised(((MessageReceivedEvent) event).getAuthor())) {
                ((MessageReceivedEvent) event).getMessage().delete().queue();
                ((MessageReceivedEvent) event).getAuthor().getPrivateChannel().sendMessage("Oi mate, you seem to be spamming mentions! If you keep doing this you will be banned.").queue();
                int spamTime = spamTimes.getOrDefault(((MessageReceivedEvent) event).getAuthor().getId(), 0);
                spamTime ++;
                spamTimes.put(((MessageReceivedEvent) event).getAuthor().getId(), spamTime);
                if (spamTime >= 3) {
                    // Do the ban.
                    ((MessageReceivedEvent) event).getAuthor().getPrivateChannel().sendMessage("You have been banned for spamming. Contact Me4502#3758 if you believe this is a mistake.")
                            .queue(message -> ((MessageReceivedEvent) event).getGuild().getController().ban(((MessageReceivedEvent) event).getAuthor(), 0).queue());
                }
            } else {
                spamTimes.remove(((MessageReceivedEvent) event).getAuthor().getId());
            }
        }
    }
}
