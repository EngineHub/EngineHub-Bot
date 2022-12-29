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
package org.enginehub.discord.util;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.util.concurrent.TimeUnit;

public class PunishmentUtil {

    private static String getContactString() {
        return "Submit an appeal via https://ehub.to/ban-appeal if you wish to appeal.";
    }

    public static void kickUser(Guild guild, Member member, String reason) {
        member.getUser().openPrivateChannel().submit()
            .thenCompose(privateChannel ->
                privateChannel.sendMessage("You have been kicked for `" + reason + "`. Make sure to read the rules if you join again!")
                    .submit()
            )
            .whenComplete((v, ex) -> guild.kick(member).reason(reason).queue());
    }

    public static void banUser(Guild guild, User user, String reason, boolean eraseHistory) {
        user.openPrivateChannel().submit()
            .thenCompose(privateChannel ->
                privateChannel.sendMessage("You have been banned for `" + reason + "`. " + getContactString())
                    .submit()
            )
            .whenComplete((v, ex) -> guild.ban(user, eraseHistory ? 1 : 0, TimeUnit.HOURS).reason("[Bot Ban] " + reason).queue());
    }

    public static void timeoutUser(Guild guild, Member member, long seconds, String reason) {
         guild.timeoutFor(member, seconds, TimeUnit.SECONDS).queue();
    }
}
