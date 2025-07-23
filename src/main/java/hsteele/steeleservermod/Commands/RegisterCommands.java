package hsteele.steeleservermod.Commands;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import hsteele.steeleservermod.AFKSystem.AFKCommand;
import hsteele.steeleservermod.StatisticsBook.StatisticsBookCommand;
import hsteele.steeleservermod.Steeleservermod;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class RegisterCommands {
    public static void register() {

        Steeleservermod.LOGGER.info("Registering Commands");

        // Register Main Command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal("steeleserver")
                    .requires(source -> source.hasPermissionLevel(4));

            command.then(RunCommand.register());
            command.then(AFKCommand.register());
            command.then(StatisticsBookCommand.register());

            dispatcher.register(command);
        });

    }

}
