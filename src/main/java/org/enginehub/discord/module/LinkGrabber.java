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
package org.enginehub.discord.module;

import com.typesafe.config.Optional;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import ninja.leaping.configurate.ConfigurationNode;
import org.apache.commons.lang3.StringUtils;
import org.enginehub.discord.Settings;
import org.enginehub.discord.util.PermissionRoles;
import org.enginehub.discord.util.command.CommandPermission;
import org.enginehub.discord.util.command.CommandPermissionConditionGenerator;
import org.enginehub.discord.util.command.CommandRegistrationHandler;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.enginehub.discord.util.StringUtil.createEmbed;

@CommandContainer(superTypes = CommandPermissionConditionGenerator.Registration.class)
public class LinkGrabber implements Module {

    private Map<String, String> aliasMap = new HashMap<>();

    @Override
    public void setupCommands(CommandRegistrationHandler handler, CommandManager commandManager) {
        handler.register(commandManager, LinkGrabberRegistration.builder(), this);
    }

    @Command(name = "get", aliases = {"g", "~"}, desc = "Grabs an alias.")
    public void aliasGrabber(Message message, String key, @Optional String userName) {
        User user = null;
        if (userName != null) {
            List<User> users = message.getMentionedUsers();
            if (users.size() != 1) {
                message.getChannel().sendMessage("I don't know who you want me to send that to, sorry!").queue();
                return;
            }
            user = users.get(0);
        }

        String alias = aliasMap.get(key);
        if (alias == null) {
            message.getChannel().sendMessage("I don't know what that alias is, sorry!").queue();
            aliasMap.keySet().stream().min(Comparator.comparingInt(o -> StringUtils.getLevenshteinDistance(key, o)))
            .ifPresent(possibleKey -> message.getChannel().sendMessage("Did you mean `" + possibleKey + "`?").queue());
            return;
        }

        if (alias.contains("\n") || alias.contains(" ")) {
            EmbedBuilder builder = createEmbed();
            if (user != null) {
                builder.appendDescription("Hey, " + user.getAsMention() + "! \n\n");
            }
            builder.appendDescription(alias);
            builder.setFooter("Requested by " + message.getAuthor().getName());
            message.getChannel().sendMessageEmbeds(builder.build()).queue();
        } else {
            String prefix = "";
            if (user != null) {
                prefix = "Hey, " + user.getAsMention() + "! ";
            }
            message.getChannel().sendMessage(prefix + alias).queue();
        }
    }

    @Command(name = "addalias", aliases = {"addlink"}, desc = "Adds an alias.")
    @CommandPermission(PermissionRoles.MODERATOR)
    public void addLink(Message message, String key, String link) {
        aliasMap.put(key, link.replace("\\n", "\n"));

        message.getChannel().sendMessage("Added an alias to the list!").queue();

        Settings.saveModule(this);
    }

    @Command(name = "listaliases", aliases = {"aliases", "listlinks"}, desc = "Lists all available aliases.")
    public void aliasLister(Message message) {
        message.getChannel().sendMessage("Here you go, " + message.getAuthor().getAsMention() + "! " + String.join(", ", aliasMap.keySet())).queue();
    }

    @Override
    public void load(ConfigurationNode loadedNode) {
        aliasMap = loadedNode.getNode("links").getChildrenMap().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> e.getValue().getString("FAILED TO LOAD")
                ));
    }

    @Override
    public void save(ConfigurationNode loadedNode) {
        loadedNode.getNode("links").setValue(aliasMap);
    }
}
