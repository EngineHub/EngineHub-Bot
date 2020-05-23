/*
 * Copyright (c) Me4502 (Matthew Miller)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.me4502.me4bot.discord;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;
import com.me4502.me4bot.discord.module.Alerts;
import com.me4502.me4bot.discord.module.AutoErase;
import com.me4502.me4bot.discord.module.ChatFilter;
import com.me4502.me4bot.discord.module.EmojiRole;
import com.me4502.me4bot.discord.module.JoinMessage;
import com.me4502.me4bot.discord.module.LinkGrabber;
import com.me4502.me4bot.discord.module.Module;
import com.me4502.me4bot.discord.module.NoSpam;
import com.me4502.me4bot.discord.module.PingWarning;
import com.me4502.me4bot.discord.module.SetProfilePicture;
import com.me4502.me4bot.discord.module.errorHelper.ErrorHelper;
import com.me4502.me4bot.discord.util.PermissionRoles;
import com.me4502.me4bot.discord.util.binding.MemberBinding;
import com.me4502.me4bot.discord.util.binding.MessageBinding;
import com.sk89q.intake.CommandException;
import com.sk89q.intake.InvalidUsageException;
import com.sk89q.intake.context.CommandLocals;
import com.sk89q.intake.dispatcher.Dispatcher;
import com.sk89q.intake.fluent.CommandGraph;
import com.sk89q.intake.fluent.DispatcherNode;
import com.sk89q.intake.parametric.ParametricBuilder;
import com.sk89q.intake.util.auth.AuthorizationException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;

public class Me4Bot implements Runnable, EventListener {

    public static final String COMMAND_PREFIX = "~";

    public static Me4Bot bot;
    private static boolean running = true;

    public static boolean isAuthorised(Member member, String permission) {
        if (permission.equalsIgnoreCase(PermissionRoles.ANY)) {
            return true;
        }

        if (member == null) {
            return false;
        }

        if (permission.equalsIgnoreCase(PermissionRoles.BOT_OWNER)) {
            return member.getUser().getName().equals(Settings.hostUsername)
                    && member.getUser().getDiscriminator().equals(Settings.hostIdentifier);
        }

        boolean hasRank;

        while (true) {
            String finalPermission = permission;
            hasRank = member.getRoles().stream().anyMatch(role -> role.getName().equalsIgnoreCase(finalPermission));
            if (!hasRank) {
                if (PermissionRoles.TRUSTED.equals(permission)) {
                    permission = PermissionRoles.MODERATOR;
                } else if (PermissionRoles.MODERATOR.equals(permission)) {
                    permission = PermissionRoles.ADMIN;
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        return hasRank;
    }

    public static void main(String[] args) {
        Settings.load();

        try {
            new Me4Bot();

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

            Settings.saveModules();
            Settings.save();

            // Force kill.
            System.exit(0);
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public JDA api;
    private final Dispatcher commandDispatcher;

    private final static List<GatewayIntent> intents = Lists.newArrayList(
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.GUILD_MEMBERS,
        GatewayIntent.GUILD_BANS,
        GatewayIntent.GUILD_EMOJIS,
        GatewayIntent.GUILD_INVITES,
        GatewayIntent.GUILD_MESSAGE_REACTIONS
    );

    private Me4Bot() throws LoginException, InterruptedException {
        bot = this;
        System.out.println("Connecting...");
        api = JDABuilder.create(Settings.token, intents)
                .setAutoReconnect(true)
                .addEventListeners(this)
                .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.CLIENT_STATUS)
                .build();
        api.awaitReady();
        System.out.println("Connected");

        Settings.loadModules();

        ParametricBuilder builder = new ParametricBuilder();
        builder.addBinding(new MemberBinding());
        builder.addBinding(new MessageBinding());
        builder.setAuthorizer((namespace, permission) -> {
            Member member = checkNotNull(namespace.get(Member.class), "Unknown member.");

            return isAuthorised(member, permission);
        });

        DispatcherNode node = new CommandGraph()
                .builder(builder)
                .commands();

        for (Module module : modules) {
            node = module.setupCommands(node);
            if (module instanceof EventListener) {
                api.addEventListener((EventListener) module);
            }
        }

        commandDispatcher = node.graph().getDispatcher();

        modules.forEach(Module::onInitialise);
    }

    private void disconnect() {
        modules.forEach(Module::onShutdown);

        api.shutdown();
    }

    private final Collection<Module> modules = Lists.newArrayList(
            new AutoErase(),
            new Alerts(),
            new ChatFilter(),
            new SetProfilePicture(),
            new NoSpam(),
            new ErrorHelper(),
            new LinkGrabber(),
            new JoinMessage(),
            new PingWarning(),
            new EmojiRole()
    );

    public Collection<Module> getModules() {
        return this.modules;
    }

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

    private static final String[] PARENT_COMANDS = new String[0];

    @Override
    public void onEvent(@Nonnull GenericEvent event) {
        if (event instanceof MessageReceivedEvent && ((MessageReceivedEvent) event).getMessage().getContentRaw().startsWith(COMMAND_PREFIX)) {
            String commandArgs = ((MessageReceivedEvent) event).getMessage().getContentRaw().substring(COMMAND_PREFIX.length());

            if (commandArgs.equals("stop") && isAuthorised(((MessageReceivedEvent) event).getMember(), PermissionRoles.BOT_OWNER)) {
                running = false;
                return;
            }

            CommandLocals locals = new CommandLocals();
            locals.put(Member.class, ((MessageReceivedEvent) event).getMember());
            locals.put(Message.class, ((MessageReceivedEvent) event).getMessage());

            try {
                commandDispatcher.call(commandArgs, locals, PARENT_COMANDS);
            } catch (InvalidUsageException e) {
                String usage = e.getMessage();
                if ("Please choose a sub-command.".equals(usage)) {
                    usage = "Unknown command!";
                }
                ((MessageReceivedEvent) event).getChannel().sendMessage(usage == null ? "No help text available." : usage).queue();
            } catch (CommandException e) {
                ((MessageReceivedEvent) event).getChannel().sendMessage("Failed to send command! " + e.getMessage()).queue();
                e.printStackTrace();
            } catch (AuthorizationException e) {
                ((MessageReceivedEvent) event).getChannel().sendMessage("You don't have permissions!").queue();
            }
        }
    }
}
