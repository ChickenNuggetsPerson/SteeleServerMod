package hsteele.steeleservermod;

import hsteele.steeleservermod.Commands.RegisterCommands;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Steeleservermod implements ModInitializer {

    public static final String MOD_ID = "steelemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initalizing Steele Mod");

        // Register Commands
        RegisterCommands.register();

    }
}
