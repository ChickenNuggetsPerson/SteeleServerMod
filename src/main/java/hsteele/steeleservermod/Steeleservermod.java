package hsteele.steeleservermod;

import hsteele.steeleservermod.AFKSystem.AFKManager;
import hsteele.steeleservermod.AFKSystem.PlayerMoveCallback;
import hsteele.steeleservermod.AFKSystem.PlayerMovementTracker;
import hsteele.steeleservermod.Commands.RegisterCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Steeleservermod implements ModInitializer {

    public static final String MOD_ID = "steelemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Steele Mod");

        // Register Commands
        RegisterCommands.register();

        // Register AFK Manager at end of server tick
        ServerTickEvents.END_SERVER_TICK.register(AFKManager::tick);

        // Register player movement event with afk tick
        PlayerMovementTracker.register();

        // Register the movement listener
        PlayerMoveCallback.EVENT.register((player, from, to) -> {
            AFKManager.onPlayerActivity(player);
            return ActionResult.PASS;
        });

    }
}
