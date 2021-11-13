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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.sk89q.intake.Command;
import com.sk89q.intake.fluent.DispatcherNode;
import com.sk89q.intake.parametric.annotation.Optional;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class RoryFetch implements Module {

    private static final Map<String, String> roryOverrides = new HashMap<>();

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModules(new Jdk8Module(), new ParameterNamesModule());
    private static final TypeReference<Map<String, String>> RORY_RESPONSE =
        new TypeReference<>() {
        };

    private final HttpClient client = HttpClient.newHttpClient();

    static {
        roryOverrides.put("gilmore", "https://i.pinimg.com/736x/25/ee/b7/25eeb71bb71aeee5574c50f96205d871.jpg");
    }

    @Override
    public DispatcherNode setupCommands(DispatcherNode dispatcherNode) {
        return dispatcherNode
            .registerMethods(this);
    }

    private static MessageEmbed createRoryEmbed(String roryId, String imageUrl) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setImage(imageUrl);
        builder.setColor(new Color(87, 61, 129));
        builder.setTimestamp(Instant.now());
        builder.setFooter("ID: " + roryId);

        return builder.build();
    }

    @Command(aliases = {"rory"}, desc = "Grabs a rory pic.")
    public void rory(Message message, @Optional String roryId) {
        if (roryId != null && roryOverrides.containsKey(roryId)) {
            message.getChannel().sendMessageEmbeds(createRoryEmbed(roryId, roryOverrides.get(roryId))).queue();
        } else {
            String url = "https://rory.cat/purr";
            if (roryId != null) {
                url = url + '/' + roryId;
            }
            try {
                HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(new URL(url).toURI()).build(),
                    HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() == 404) {
                    message.getChannel().sendMessage(message.getAuthor().getAsTag() + ", that's not a valid rory pic").queue();
                    return;
                }

                Map<String, String> parsedResponse = OBJECT_MAPPER.readValue(response.body(), RORY_RESPONSE);
                message.getChannel().sendMessageEmbeds(createRoryEmbed(parsedResponse.get("id"), parsedResponse.get("url"))).queue();
            } catch (MalformedURLException | URISyntaxException e) {
                message.getChannel().sendMessage(message.getAuthor().getAsTag() + ", that's an invalid URL!").queue();
            } catch (InterruptedException | IOException e) {
                message.getChannel().sendMessage(message.getAuthor().getAsTag() + ", failed to lookup rory pic!").queue();
            }
        }
    }

}
