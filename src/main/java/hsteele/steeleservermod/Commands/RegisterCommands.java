package hsteele.steeleservermod.Commands;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import hsteele.steeleservermod.AFKSystem.AFKCommand;
import hsteele.steeleservermod.HarvestSystem.HarvestSystemCommand;
import hsteele.steeleservermod.StatisticsBook.StatisticsBookCommand;
import hsteele.steeleservermod.Steeleservermod;
import hsteele.steeleservermod.WalkerSystem.WalkerStorage;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class RegisterCommands {
    public static void register() {

        Steeleservermod.LOGGER.info("Registering Commands");

        // Register Main Command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("steeleserver")
                    .requires(Commands.hasPermission(Commands.LEVEL_OWNERS));

            command.then(RunCommand.register());
            command.then(AFKCommand.register());
            command.then(StatisticsBookCommand.register());
            command.then(HarvestSystemCommand.register());
            command.then(WalkerStorage.registerCommand());

            dispatcher.register(command);
        });

    }

}
