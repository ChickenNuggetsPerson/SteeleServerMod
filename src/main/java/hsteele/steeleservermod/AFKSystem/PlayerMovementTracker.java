package hsteele.steeleservermod.AFKSystem;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.UUID;

public class PlayerMovementTracker {
    private static final HashMap<UUID, BlockPos> lastPositions = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(PlayerMovementTracker::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BlockPos currentPos = player.blockPosition();
            BlockPos lastPos = lastPositions.get(player.getUUID());

            if (lastPos == null || !lastPos.equals(currentPos)) {
                // Fire the event
                PlayerMoveCallback.EVENT.invoker().onMove(player, lastPos, currentPos);

                // Update stored position
                lastPositions.put(player.getUUID(), currentPos);
            }
        }
    }
}
