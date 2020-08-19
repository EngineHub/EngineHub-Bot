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
package org.enginehub.discord;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import org.enginehub.discord.module.Module;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Settings {

    private static CommentedConfigurationNode loadedNode;

    public static String token;
    public static String hostUsername;
    public static String hostIdentifier;
    public static List<String> autoEraseChannels = Lists.newArrayList();

    public static void load() {
        File file = new File("settings.conf").getAbsoluteFile();
        ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setPath(file.toPath()).build(); // Create the loader
        try {
            if (file.exists()) {
                loadedNode = loader.load();
            } else {
                loadedNode = loader.createEmptyNode();
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            token = loadedNode.getNode("token").getString("token");
            hostUsername = loadedNode.getNode("host-username").getString("Me4502");
            hostIdentifier = loadedNode.getNode("host-identifier").getString("4502");
            autoEraseChannels = loadedNode.getNode("auto-erase-channels").getList(TypeToken.of(String.class), autoEraseChannels);
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
        }

        save();
    }

    public static void loadModules() {
        for (Module module : EngineHubBot.bot.getModules()) {
            File moduleSettings = new File(module.getClass().getSimpleName() + ".conf").getAbsoluteFile();
            ConfigurationLoader<CommentedConfigurationNode> moduleLoader = HoconConfigurationLoader.builder().setPath(moduleSettings.toPath()).build();
            try {
                ConfigurationNode node;

                if (moduleSettings.exists()) {
                    node = moduleLoader.load();
                } else {
                    node = moduleLoader.createEmptyNode();
                    moduleSettings.getParentFile().mkdirs();
                    moduleSettings.createNewFile();
                }

                module.load(node);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setPath(new File("settings.conf").getAbsoluteFile().toPath()).build(); // Create the loader
        try {
            loadedNode.getNode("token").setValue(token);
            loadedNode.getNode("auto-erase-channels").setValue(new TypeToken<List<String>>(){}, autoEraseChannels);

            loader.save(loadedNode);
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
        }
    }

    public static void saveModules() {
        EngineHubBot.bot.getModules().forEach(Settings::saveModule);
    }

    public static void saveModule(Module module) {
        File moduleSettings = new File(module.getClass().getSimpleName() + ".conf").getAbsoluteFile();
        ConfigurationLoader<CommentedConfigurationNode> moduleLoader = HoconConfigurationLoader.builder().setPath(moduleSettings.toPath()).build();
        try {
            ConfigurationNode node = moduleLoader.createEmptyNode();
            module.save(node);
            moduleLoader.save(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
