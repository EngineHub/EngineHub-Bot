package com.me4502.me4bot.discord;

import com.google.common.collect.Sets;
import com.me4502.me4bot.discord.module.Alerts;
import com.me4502.me4bot.discord.module.AutoErase;
import com.me4502.me4bot.discord.module.Module;
import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.message.MessageHistory;
import de.btobastian.javacord.listener.Listener;
import de.btobastian.javacord.listener.message.MessageCreateListener;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Me4Bot implements Runnable, MessageCreateListener {

    public static Me4Bot bot;
    private static boolean running = true;

    public static boolean isAuthorised(User user) {
        return user.getName().equals("Me4502") && user.getDiscriminator().contains("3758");
    }

    public static void main(String[] args) {
        Settings.load();

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
    }

    public DiscordAPI api;

    private Me4Bot() {
        api = Javacord.getApi("MjE5MDkyOTI4NTg3NDk3NDcy.CqMttw.zIEaDF0dc7N_-o_CkGR_XIEky_g", true);

        System.out.println("Connecting...");
        api.connectBlocking();
        api.setAutoReconnect(true);
        System.out.println("Connected");
        api.registerListener(this);
        for (Module module : modules) {
            if (module instanceof Listener) {
                api.registerListener((Listener) module);
            }
        }
    }

    public void disconnect() {
        api.disconnect();
    }

    private Set<Module> modules = Sets.newHashSet(new AutoErase(), new Alerts());

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
    public void onMessageCreate(DiscordAPI discordAPI, Message message) {
        if (message.getContent().equals("~stop") && isAuthorised(message.getAuthor())) {
            running = false;
        }
    }
}
