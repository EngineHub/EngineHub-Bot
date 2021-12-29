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

import org.enginehub.piston.Command;
import org.enginehub.piston.gen.CommandConditionGenerator;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;

public final class CommandPermissionConditionGenerator implements CommandConditionGenerator {

    public interface Registration {
        Registration commandPermissionConditionGenerator(CommandPermissionConditionGenerator generator);
    }

    @Override
    public Command.Condition generateCondition(Method commandMethod) {
        CommandPermission annotation = commandMethod.getAnnotation(CommandPermission.class);
        checkNotNull(annotation, "Annotation is missing from commandMethod");
        return new PermissionCondition(annotation.value());
    }
}
