package com.me4502.me4bot.discord.util.binding;

import com.sk89q.intake.parametric.ParameterException;
import com.sk89q.intake.parametric.argument.ArgumentStack;
import com.sk89q.intake.parametric.binding.BindingBehavior;
import com.sk89q.intake.parametric.binding.BindingHelper;
import com.sk89q.intake.parametric.binding.BindingMatch;
import net.dv8tion.jda.core.entities.Member;

public class MemberBinding extends BindingHelper {

    @BindingMatch(type = Member.class,
            behavior = BindingBehavior.PROVIDES
    ) // No arguments consumed
    public Member getMember(ArgumentStack context) throws ParameterException {
        Member member = context.getContext().getLocals().get(Member.class);
        if (member == null) {
            throw new ParameterException("Failed to get member.");
        } else {
            return member;
        }
    }

}
