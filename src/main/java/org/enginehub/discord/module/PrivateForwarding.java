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
package org.enginehub.discord.module;

import org.enginehub.discord.EngineHubBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ninja.leaping.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;

public class PrivateForwarding extends ListenerAdapter implements Module {

    private String forwardChannel;

    @Override
    public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {
        if (event.getAuthor().getIdLong() == EngineHubBot.bot.api.getSelfUser().getIdLong()) {
            // Ignore messages sent by us.
            return;
        }
        if (forwardChannel != null) {
            var channel = EngineHubBot.bot.api.getGuildChannelById(forwardChannel);
            if (channel instanceof TextChannel) {
                User user = event.getAuthor();

                EmbedBuilder builder = new EmbedBuilder()
                    .setAuthor(user.getAsTag(),
                        "https://discord.com/channels/@me/" + user.getId(),
                        user.getEffectiveAvatarUrl()
                    )
                    .setDescription(event.getMessage().getContentRaw());

                event.getMessage().getAttachments().forEach(att -> builder.addField(att.getFileName(), att.getUrl(), false));

                ((TextChannel) channel).sendMessage(builder.build()).queue();
            }
        }
    }

    @Override
    public void load(ConfigurationNode loadedNode) {
        forwardChannel = loadedNode.getNode("forward-channel").getString(null);
    }

    @Override
    public void save(ConfigurationNode loadedNode) {
        loadedNode.getNode("forward-channel").setValue(forwardChannel);
    }
}
