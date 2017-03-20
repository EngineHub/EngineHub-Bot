package com.me4502.me4bot.discord.module;

import com.me4502.me4bot.discord.Me4Bot;
import com.me4502.me4bot.discord.util.PermissionRoles;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.context.CommandLocals;
import com.sk89q.intake.fluent.DispatcherNode;
import net.dv8tion.jda.core.entities.Icon;
import net.dv8tion.jda.core.entities.Message;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class SetProfilePicture implements Module {

    @Override
    public DispatcherNode setupCommands(DispatcherNode dispatcherNode) {
        return dispatcherNode
                .registerMethods(this);
    }

    @Command(aliases = "setprofilepicture", desc = "Set's the profile picture of this bot.")
    @Require(PermissionRoles.BOT_OWNER)
    public void setProfilePicture(Message message) {
        Optional<Message.Attachment> attachmentOptional = message.getAttachments().stream().filter(Message.Attachment::isImage).findFirst();

        if (attachmentOptional.isPresent()) {
            File file = new File("avatar_cache.png");
            attachmentOptional.get().download(file);
            try {
                Me4Bot.bot.api.getSelfUser().getManager().setAvatar(Icon.from(file)).queue(aVoid -> file.delete());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            message.getTextChannel().sendMessage("You need to attach an image!").queue();
        }
    }
}
