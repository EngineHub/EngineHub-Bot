package com.me4502.me4bot.discord;

import com.google.common.collect.Sets;
import com.me4502.me4bot.discord.module.Alerts;
import com.me4502.me4bot.discord.module.AutoErase;
import com.me4502.me4bot.discord.module.Module;
import com.me4502.me4bot.discord.module.audio.Audio;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.EventListener;

import javax.security.auth.login.LoginException;
import java.util.Set;

public class Me4Bot implements Runnable, EventListener {

    public static Me4Bot bot;
    private static boolean running = true;

    public static boolean isAuthorised(User user) {
        return user.getName().equals("Me4502") && user.getDiscriminator().equals("3758");
    }

    public static void main(String[] args) {
        Settings.load();

        try {
            bot = new Me4Bot();

            Thread thread = new Thread(bot);
            thread.setDaemon(false);
            thread.setName("Main Bot Thread");
            thread.start();

            while (running) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            bot.disconnect();

            Settings.save();

            // Force kill.
            System.exit(0);
        } catch (LoginException | InterruptedException | RateLimitedException e) {
            e.printStackTrace();
        }
    }

    public JDA api;

    private Me4Bot() throws LoginException, InterruptedException, RateLimitedException {
        System.out.println("Connecting...");
        api = new JDABuilder(AccountType.BOT).setToken(Settings.token).addListener(this).buildBlocking();
        api.setAutoReconnect(true);
        System.out.println("Connected");
        for (Module module : modules) {
            if (module instanceof EventListener) {
                api.addEventListener((EventListener) module);
            }
        }

        modules.forEach(Module::onInitialise);
    }

    public void disconnect() {
        modules.forEach(Module::onShutdown);

        api.shutdown(true);
    }

    private Set<Module> modules = Sets.newHashSet(new AutoErase(), new Alerts(), new Audio());

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        while (running) {
            modules.forEach(Module::onTick);

            if (System.currentTimeMillis() - startTime > 1000 * 60 * 60 * 12) {
                running = false;
                System.out.println("Shutting down!");
                break;
            }

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof MessageReceivedEvent) {
            if (((MessageReceivedEvent) event).getMessage().getContent().equals("~stop") && isAuthorised(((MessageReceivedEvent) event).getAuthor())) {
                running = false;
            }
        }
    }
}
