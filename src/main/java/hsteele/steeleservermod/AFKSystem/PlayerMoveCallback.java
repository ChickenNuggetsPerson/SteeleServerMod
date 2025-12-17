package hsteele.steeleservermod.AFKSystem;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public interface PlayerMoveCallback {
    Event<PlayerMoveCallback> EVENT = EventFactory.createArrayBacked(PlayerMoveCallback.class,
            (listeners) -> (player, from, to) -> {
                InteractionResult result = InteractionResult.PASS;
                for (PlayerMoveCallback listener : listeners) {
                    result = listener.onMove(player, from, to);
                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }
                return result;
            });

    InteractionResult onMove(ServerPlayer player, BlockPos from, BlockPos to);
}
