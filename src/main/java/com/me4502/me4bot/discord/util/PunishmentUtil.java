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
package com.me4502.me4bot.discord.util;

import com.me4502.me4bot.discord.Settings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class PunishmentUtil {

    private static String getContactString() {
        return "Contact " + Settings.hostUsername + '#' + Settings.hostIdentifier + " if you believe this is a mistake.";
    }

    public static void kickUser(Guild guild, Member member, String reason) {
        member.getUser().openPrivateChannel().queue((privateChannel ->
                privateChannel.sendMessage("You have been kicked for `" + reason + "`. " + getContactString())
                .queue(message -> {
                    guild.kick(member, reason).queue();
                })
        ));
    }

    public static void banUser(Guild guild, User user, String reason, boolean eraseHistory) {
        user.openPrivateChannel().queue((privateChannel ->
                privateChannel.sendMessage("You have been banned for `" + reason + "`. " + getContactString())
                .queue(message -> {
                    guild.ban(user, eraseHistory ? 7 : 0, "[Bot Ban] " + reason).queue();
                })
        ));
    }
}
