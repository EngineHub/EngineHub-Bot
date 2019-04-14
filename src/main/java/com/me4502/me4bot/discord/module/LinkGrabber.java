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
package com.me4502.me4bot.discord.module;

import com.me4502.me4bot.discord.Settings;
import com.me4502.me4bot.discord.util.PermissionRoles;
import com.me4502.me4bot.discord.util.StringUtil;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.fluent.DispatcherNode;
import com.sk89q.intake.parametric.annotation.Optional;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import ninja.leaping.configurate.ConfigurationNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LinkGrabber implements Module {

    private Map<String, String> aliasMap = new HashMap<>();

    @Override
    public DispatcherNode setupCommands(DispatcherNode dispatcherNode) {
        return dispatcherNode
                .registerMethods(this);
    }

    @Command(aliases = {"get", "g", "~"}, desc = "Grabs an alias.")
    public void aliasGrabber(Message message, String key, @Optional String userName) {
        User user = message.getAuthor();
        if (userName != null) {
            List<User> users = message.getMentionedUsers();
            if (users.size() != 1) {
                message.getChannel().sendMessage("I don't know who you want me to send that to, sorry " + StringUtil.annotateUser(user) + '!').queue();
                return;
            }
            user = users.get(0);
        }

        String alias = aliasMap.get(key);
        if (alias == null) {
            message.getChannel().sendMessage("I don't know what that alias is, sorry " + StringUtil.annotateUser(user) + '!').queue();
            return;
        }

        if (alias.contains("\n")) {
            MessageBuilder builder = new MessageBuilder();
            builder.append("Here you go, ").append(StringUtil.annotateUser(user)).append("!\n\n");
            builder.append(alias);
            builder.buildAll(MessageBuilder.SplitPolicy.NEWLINE).forEach(mess -> message.getChannel().sendMessage(mess).queue());
        } else {
            message.getChannel().sendMessage("Here you go, " + StringUtil.annotateUser(user) + "! " + alias).queue();
        }
    }

    @Command(aliases = {"addalias", "addlink"}, desc = "Adds an alias.")
    @Require(PermissionRoles.ADMIN)
    public void addLink(Message message, String key, String link) {
        aliasMap.put(key, link);

        message.getChannel().sendMessage("Added an alias to the list!").queue();

        Settings.saveModule(this);
    }

    @Command(aliases = {"listaliases", "aliases", "listlinks"}, desc = "Lists all available aliases.")
    public void aliasLister(Message message) {
        message.getChannel().sendMessage("Here you go, " + StringUtil.annotateUser(message.getAuthor()) + "! " + String.join(", ", aliasMap.keySet())).queue();
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
