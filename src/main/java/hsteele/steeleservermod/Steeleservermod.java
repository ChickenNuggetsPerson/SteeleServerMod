package hsteele.steeleservermod;

import hsteele.steeleservermod.AFKSystem.AFKManager;
import hsteele.steeleservermod.Commands.RegisterCommands;
import hsteele.steeleservermod.HarvestSystem.HarvestSystem;
import hsteele.steeleservermod.ShutdownStickSystem.ShutdownStickSystem;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Steeleservermod implements ModInitializer {

    public static final String MOD_ID = "steelemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Steele Mod");

        // Register Systems
        RegisterCommands.register();
        AFKManager.register();
        HarvestSystem.register();
        ShutdownStickSystem.register();

    }
}
