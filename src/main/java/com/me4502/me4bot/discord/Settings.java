package com.me4502.me4bot.discord;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.me4502.me4bot.discord.module.Module;
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
            autoEraseChannels = loadedNode.getNode("auto-erase-channels").getList(TypeToken.of(String.class), autoEraseChannels);
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
        }

        save();
    }

    public static void loadModules() {
        for (Module module : Me4Bot.bot.getModules()) {
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
        for (Module module : Me4Bot.bot.getModules()) {
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
}
