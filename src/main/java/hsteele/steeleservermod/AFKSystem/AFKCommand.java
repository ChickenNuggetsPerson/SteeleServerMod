package hsteele.steeleservermod.AFKSystem;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import hsteele.steeleservermod.config.ConfigSystem;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;

public class AFKCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> register() {

        LiteralArgumentBuilder<net.minecraft.server.command.ServerCommandSource> afkCommand = CommandManager.literal("afk")
                .requires(source -> source.hasPermissionLevel(4));

        afkCommand.then(CommandManager.literal("timeout")
                .executes(AFKCommand::getTimeout)
                .then(argument("minutes", IntegerArgumentType.integer(1, 1440)) // 1 min to 24 hours
                        .executes(AFKCommand::setTimeout)
                ));

        return afkCommand;
    }


    private static int getTimeout(CommandContext<ServerCommandSource> context) throws  CommandSyntaxException {
        int minutes = ConfigSystem.get().afkTimeoutMinutes;
        context.getSource().sendFeedback(
                () -> Text.literal("AFK timeout: " + minutes + " minutes"),
                true
        );
        return 1;
    }
    private static int setTimeout(CommandContext<ServerCommandSource> context) throws  CommandSyntaxException {
        int minutes = IntegerArgumentType.getInteger(context, "minutes");
        AFKManager.setTimeoutMinutes(minutes);

        context.getSource().sendFeedback(
                () -> Text.literal("AFK timeout set to " + minutes + " minutes."),
                true
        );

        return 1;
    };

    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return 0;
    }

}
