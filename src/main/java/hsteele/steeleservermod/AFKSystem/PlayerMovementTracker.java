package hsteele.steeleservermod.AFKSystem;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.UUID;

public class PlayerMovementTracker {
    private static final HashMap<UUID, BlockPos> lastPositions = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(PlayerMovementTracker::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            BlockPos currentPos = player.getBlockPos();
            BlockPos lastPos = lastPositions.get(player.getUuid());

            if (lastPos == null || !lastPos.equals(currentPos)) {
                // Fire the event
                PlayerMoveCallback.EVENT.invoker().onMove(player, lastPos, currentPos);

                // Update stored position
                lastPositions.put(player.getUuid(), currentPos);
            }
        }
    }
}
