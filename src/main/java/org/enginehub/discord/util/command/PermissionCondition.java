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
package org.enginehub.discord.util.command;

import net.dv8tion.jda.api.entities.Member;
import org.enginehub.discord.EngineHubBot;
import org.enginehub.discord.util.PermissionRole;
import org.enginehub.piston.Command;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

public class PermissionCondition implements Command.Condition {
    private static final Key<Member> MEMBER_KEY = Key.of(Member.class);

    private final PermissionRole permission;

    public PermissionCondition(PermissionRole permission) {
        this.permission = permission;
    }

    @Override
    public boolean satisfied(InjectedValueAccess context) {
        return permission == null || context.injectedValue(MEMBER_KEY)
            .map(member -> EngineHubBot.isAuthorised(member, permission))
            .orElse(false);
    }
}
