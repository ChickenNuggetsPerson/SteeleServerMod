package hsteele.steeleservermod.AFKSystem;


import hsteele.steeleservermod.config.ConfigSystem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class AFKManager {

    private static final Map<UUID, Long> lastActivity = new HashMap<>();
    private static final Set<UUID> afkPlayers = new HashSet<>();

    public static void setTimeoutMinutes(int minutes) {
        ConfigSystem.get().afkTimeoutMinutes = minutes;
        ConfigSystem.save();
    }

    public static void onPlayerActivity(ServerPlayerEntity player) {
        lastActivity.put(player.getUuid(), System.currentTimeMillis());

        // Remove AFK status if they were AFK
        if (isAFK(player)) {
            removeAFK(player);
        }
    }

    public static void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        int AFK_TIMEOUT_MILLIS = ConfigSystem.get().afkTimeoutMinutes * 60 * 1000;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

            UUID uuid = player.getUuid();
            long lastSeen = lastActivity.getOrDefault(uuid, now);
            long dt = now - lastSeen;

            if (!isAFK(player) && dt > AFK_TIMEOUT_MILLIS) {
                addAFK(player);
            }
        }
    }


    public static void addAFK(ServerPlayerEntity player) {
        afkPlayers.add(player.getUuid());
        MinecraftServer server = player.getServer();
        assert server != null;
        server.getPlayerManager().broadcast(
                Text.literal(player.getName().getString() + " is now AFK").formatted(Formatting.YELLOW),
                false
        );
    }
    public static void removeAFK(ServerPlayerEntity player) {
        afkPlayers.remove(player.getUuid());
        MinecraftServer server = player.getServer();
        assert server != null;
        server.getPlayerManager().broadcast(
                Text.literal(player.getName().getString() + " is no longer AFK.").formatted(Formatting.YELLOW),
                false
        );
    }

    public static boolean isAFK(ServerPlayerEntity player) {
        return afkPlayers.contains(player.getUuid());
    }

}
