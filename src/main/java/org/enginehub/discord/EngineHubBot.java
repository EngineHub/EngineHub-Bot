/*
 * Copyright (c) EngineHub and Contributors
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
package org.enginehub.discord;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.enginehub.discord.module.Alerts;
import org.enginehub.discord.module.ChatFilter;
import org.enginehub.discord.module.EmojiRole;
import org.enginehub.discord.module.IdleRPG;
import org.enginehub.discord.module.JoinMessage;
import org.enginehub.discord.module.LinkGrabber;
import org.enginehub.discord.module.Module;
import org.enginehub.discord.module.NoMessageSpam;
import org.enginehub.discord.module.NoPingSpam;
import org.enginehub.discord.module.PingWarning;
import org.enginehub.discord.module.PrivateForwarding;
import org.enginehub.discord.module.RoryFetch;
import org.enginehub.discord.module.SetProfilePicture;
import org.enginehub.discord.module.errorHelper.ErrorHelper;
import org.enginehub.discord.util.PermissionRole;
import org.enginehub.discord.util.command.CommandArgParser;
import org.enginehub.discord.util.command.CommandRegistrationHandler;
import org.enginehub.discord.util.command.Substring;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.CommandManagerService;
import org.enginehub.piston.exception.CommandException;
import org.enginehub.piston.exception.ConditionFailedException;
import org.enginehub.piston.exception.UsageException;
import org.enginehub.piston.impl.CommandManagerServiceImpl;
import org.enginehub.piston.inject.InjectedValueStore;
import org.enginehub.piston.inject.Key;
import org.enginehub.piston.inject.MapBackedValueStore;
import org.enginehub.piston.util.ValueProvider;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;

public class EngineHubBot extends ListenerAdapter implements Runnable {

    public static final String COMMAND_PREFIX = "~";

    private static final Logger LOGGER = LogManager.getLogger();

    public static EngineHubBot bot;
    private static volatile boolean running = true;

    public static boolean isAuthorised(Member member, PermissionRole permission) {
        if (permission == PermissionRole.ANY) {
            return true;
        }

        if (member == null) {
            return false;
        }

        // Collect all roles into a set
        var roles = member.getRoles().stream()
            .map(Role::getName)
            .map(PermissionRole::fromDiscordRoleName)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(TreeSet::new));

        // Add BOT_OWNER to the set if applicable
        if (member.getUser().getName().equals(Settings.hostUsername)
            && member.getUser().getDiscriminator().equals(Settings.hostIdentifier)) {
            roles.add(PermissionRole.BOT_OWNER);
        }

        // Take any permission role that is equal to or higher than the requested permission
        return roles.ceiling(permission) != null;
    }

    public static void main(String[] args) {
        Settings.load();

        try {
            new EngineHubBot();

            Thread thread = new Thread(bot);
            thread.setDaemon(false);
            thread.setName("Main Bot Thread");
            thread.start();
            thread.join();

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
    private final CommandManager commandManager;
    private final CommandManagerService commandManagerService;
    private final CommandRegistrationHandler registrationHandler;

    private final static List<GatewayIntent> intents = Lists.newArrayList(
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.GUILD_MEMBERS,
        GatewayIntent.GUILD_BANS,
        GatewayIntent.GUILD_EMOJIS,
        GatewayIntent.GUILD_INVITES,
        GatewayIntent.GUILD_MESSAGE_REACTIONS
    );

    private EngineHubBot() throws LoginException, InterruptedException {
        bot = this;
        LOGGER.info("Connecting...");
        api = JDABuilder.create(Settings.token, intents)
                .setAutoReconnect(true)
                .addEventListeners(this)
                .disableCache(
                    CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.CLIENT_STATUS,
                    CacheFlag.ONLINE_STATUS
                )
                .build();

        api.awaitReady();

        Settings.loadModules();

        this.commandManagerService = new CommandManagerServiceImpl();
        this.commandManager = this.commandManagerService.newCommandManager();
        this.registrationHandler = new CommandRegistrationHandler();

        for (Module module : modules) {
            module.setupCommands(registrationHandler, commandManager);
            if (module instanceof EventListener) {
                api.addEventListener(module);
            }
        }

        modules.forEach(Module::onInitialise);
        LOGGER.info("Connected!");
    }

    private void disconnect() {
        modules.forEach(Module::onShutdown);

        api.shutdown();
    }

    private final Collection<Module> modules = Lists.newArrayList(
            new Alerts(),
            new ChatFilter(),
            new SetProfilePicture(),
            new NoPingSpam(),
            new NoMessageSpam(),
            new ErrorHelper(),
            new LinkGrabber(),
            new JoinMessage(),
            new PingWarning(),
            new EmojiRole(),
            new PrivateForwarding(),
            new IdleRPG(),
            new RoryFetch()
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
                LOGGER.info("Shutting down!");
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
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (commandManager != null && event.getMessage().getContentRaw().startsWith(COMMAND_PREFIX)) {
            String commandArgs = event.getMessage().getContentRaw().substring(COMMAND_PREFIX.length());

            if (commandArgs.equals("stop") && isAuthorised(event.getMember(), PermissionRole.BOT_OWNER)) {
                running = false;
                return;
            }

            String[] split = CommandArgParser.forArgString(commandArgs).parseArgs()
                .map(Substring::getSubstring)
                .toArray(String[]::new);

            // No command found!
            if (!commandManager.containsCommand(split[0])) {
                return;
            }

            InjectedValueStore store = MapBackedValueStore.create();
            store.injectValue(Key.of(Member.class), ValueProvider.constant(event.getMember()));
            store.injectValue(Key.of(Message.class), ValueProvider.constant(event.getMessage()));

            try {
                commandManager.execute(store, ImmutableList.copyOf(split));
            } catch (UsageException e) {
                String usage = e.getMessage();
                if ("Please choose a sub-command.".equals(usage)) {
                    // Don't send a message.
                    return;
                }
                event.getChannel().sendMessage(usage == null ? "No help text available." : usage).queue();
            } catch (ConditionFailedException e) {
                event.getChannel().sendMessage("You don't have permissions!").queue();
            } catch (CommandException e) {
                event.getChannel().sendMessage("Failed to send command! " + e.getMessage()).queue();
                e.printStackTrace();
            }
        }
    }
}
