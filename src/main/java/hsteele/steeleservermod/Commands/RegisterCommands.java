package hsteele.steeleservermod.Commands;


import hsteele.steeleservermod.Steeleservermod;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class RegisterCommands {
    public static void register() {

        Steeleservermod.LOGGER.info("Registering Commands");

        // Register Test Command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            RunCommand.register(dispatcher, environment.dedicated);
        });

    }

}
