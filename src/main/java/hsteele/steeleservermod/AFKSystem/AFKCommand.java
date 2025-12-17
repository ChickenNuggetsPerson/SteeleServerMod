package hsteele.steeleservermod.AFKSystem;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import hsteele.steeleservermod.config.ConfigSystem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.argument;

public class AFKCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {

        LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> afkCommand = Commands.literal("afk")
                .requires(Commands.hasPermission(Commands.LEVEL_OWNERS));

        afkCommand.then(Commands.literal("timeout")
                .executes(AFKCommand::getTimeout)
                .then(argument("minutes", IntegerArgumentType.integer(1, 1440)) // 1 min to 24 hours
                        .executes(AFKCommand::setTimeout)
                ));

        return afkCommand;
    }


    private static int getTimeout(CommandContext<CommandSourceStack> context) throws  CommandSyntaxException {
        int minutes = ConfigSystem.get().afkTimeoutMinutes;
        context.getSource().sendSuccess(
                () -> Component.literal("AFK timeout: " + minutes + " minutes"),
                true
        );
        return 1;
    }
    private static int setTimeout(CommandContext<CommandSourceStack> context) throws  CommandSyntaxException {
        int minutes = IntegerArgumentType.getInteger(context, "minutes");
        AFKManager.setTimeoutMinutes(minutes);

        context.getSource().sendSuccess(
                () -> Component.literal("AFK timeout set to " + minutes + " minutes."),
                true
        );

        return 1;
    };

    private static int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return 0;
    }

}
