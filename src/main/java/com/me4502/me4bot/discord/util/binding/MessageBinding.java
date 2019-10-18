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
package com.me4502.me4bot.discord.util.binding;

import com.sk89q.intake.parametric.ParameterException;
import com.sk89q.intake.parametric.argument.ArgumentStack;
import com.sk89q.intake.parametric.binding.BindingBehavior;
import com.sk89q.intake.parametric.binding.BindingHelper;
import com.sk89q.intake.parametric.binding.BindingMatch;
import net.dv8tion.jda.api.entities.Message;

public class MessageBinding extends BindingHelper {

    @BindingMatch(type = Message.class,
            behavior = BindingBehavior.PROVIDES
    ) // No arguments consumed
    public Message getMessage(ArgumentStack context) throws ParameterException {
        Message message = context.getContext().getLocals().get(Message.class);
        if (message == null) {
            throw new ParameterException("Failed to get message.");
        } else {
            return message;
        }
    }

}
