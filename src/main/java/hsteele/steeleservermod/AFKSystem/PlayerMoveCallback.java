package hsteele.steeleservermod.AFKSystem;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public interface PlayerMoveCallback {
    Event<PlayerMoveCallback> EVENT = EventFactory.createArrayBacked(PlayerMoveCallback.class,
            (listeners) -> (player, from, to) -> {
                ActionResult result = ActionResult.PASS;
                for (PlayerMoveCallback listener : listeners) {
                    result = listener.onMove(player, from, to);
                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return result;
            });

    ActionResult onMove(ServerPlayerEntity player, BlockPos from, BlockPos to);
}
