package com.me4502.me4bot.discord.util.binding;

import com.sk89q.intake.parametric.ParameterException;
import com.sk89q.intake.parametric.argument.ArgumentStack;
import com.sk89q.intake.parametric.binding.BindingBehavior;
import com.sk89q.intake.parametric.binding.BindingHelper;
import com.sk89q.intake.parametric.binding.BindingMatch;
import net.dv8tion.jda.core.entities.Message;

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
