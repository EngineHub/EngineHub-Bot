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

import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Message;
import org.enginehub.discord.EngineHubBot;
import org.enginehub.discord.util.PermissionRole;
import org.enginehub.discord.util.command.CommandPermission;
import org.enginehub.discord.util.command.CommandPermissionConditionGenerator;
import org.enginehub.discord.util.command.CommandRegistrationHandler;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@CommandContainer(superTypes = CommandPermissionConditionGenerator.Registration.class)
public class SetProfilePicture implements Module {

    @Override
    public void setupCommands(CommandRegistrationHandler handler, CommandManager commandManager) {
        handler.register(commandManager, SetProfilePictureRegistration.builder(), this);
    }


    @Command(name = "setprofilepicture", desc = "Sets the profile picture of this bot.")
    @CommandPermission(PermissionRole.BOT_OWNER)
    public void setProfilePicture(Message message) {
        Optional<Message.Attachment> attachmentOptional = message.getAttachments().stream().filter(Message.Attachment::isImage).findFirst();

        if (attachmentOptional.isPresent()) {
            try {
                EngineHubBot.bot.api.getSelfUser().getManager().setAvatar(Icon.from(attachmentOptional.get().retrieveInputStream().get())).queue();
            } catch (IOException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            message.getChannel().sendMessage("You need to attach an image!").queue();
        }
    }
}
