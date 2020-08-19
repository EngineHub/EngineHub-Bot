/*
 * Copyright (c) Me4502 (Matthew Miller)
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

import org.enginehub.discord.EngineHubBot;
import org.enginehub.discord.util.PermissionRoles;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.fluent.DispatcherNode;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Message;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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
            try {
                EngineHubBot.bot.api.getSelfUser().getManager().setAvatar(Icon.from(attachmentOptional.get().retrieveInputStream().get())).queue();
            } catch (IOException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            message.getTextChannel().sendMessage("You need to attach an image!").queue();
        }
    }
}
