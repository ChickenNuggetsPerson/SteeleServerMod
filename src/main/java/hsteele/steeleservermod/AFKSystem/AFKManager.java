package hsteele.steeleservermod.AFKSystem;


import hsteele.steeleservermod.config.ConfigSystem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

import java.util.*;

public class AFKManager {

    private static final Map<UUID, Long> lastActivity = new HashMap<>();
    private static final Set<UUID> afkPlayers = new HashSet<>();

    public static void register() {

        // Register AFK Manager at end of server tick
        ServerTickEvents.END_SERVER_TICK.register(AFKManager::tick);

        // Register player movement event with afk tick
        PlayerMovementTracker.register();

        // Register the movement listener
        PlayerMoveCallback.EVENT.register((player, from, to) -> {
            AFKManager.onPlayerActivity(player);
            return ActionResult.PASS;
        });


        // Clear player from storage on join and disconnect
        // -> This stops the afk messages from appearing when the join / leave
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            AFKManager.forceRemoveAFK(player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            AFKManager.forceRemoveAFK(player);
        });
    }

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
        if (server.getTicks() % 20 != 0) { return; } // Only check once a second

        long now = System.currentTimeMillis();
        int AFK_TIMEOUT_MILLIS = ConfigSystem.get().afkTimeoutMinutes * 60 * 1000;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

            UUID uuid = player.getUuid();
            long lastSeen = lastActivity.getOrDefault(uuid, now);
            long dt = now - lastSeen;

            if (!isAFK(player) && dt > AFK_TIMEOUT_MILLIS) {
                addAFK(player);
            }

            if (isAFK(player)) {
                player.sendMessage(Text.literal(getMessageString(server.getTicks())), true);
            }
        }
    }

    private static String getMessageString(int tick) {

        int size = 3;
        int t = tick / 20 % (size * 2);
        if (t > size) {
            t = size - (t - size);
        }

        return "-".repeat(t + 1) +
                " You are AFK " +
                "-".repeat(size - t + 1);
    }

    public static void addAFK(ServerPlayerEntity player) {
        afkPlayers.add(player.getUuid());
        MinecraftServer server = player.getEntityWorld().getServer();
        server.getPlayerManager().broadcast(
                Text.literal(player.getName().getString() + " is now AFK").formatted(Formatting.YELLOW),
                false
        );
        addAFKPrefix(player);
    }
    public static void removeAFK(ServerPlayerEntity player) {
        afkPlayers.remove(player.getUuid());
        MinecraftServer server = player.getEntityWorld().getServer();
        server.getPlayerManager().broadcast(
                Text.literal(player.getName().getString() + " is no longer AFK").formatted(Formatting.YELLOW),
                false
        );
        removeAFKPrefix(player);
    }

    public static boolean isAFK(ServerPlayerEntity player) {
        return afkPlayers.contains(player.getUuid());
    }

    private static void forceRemoveAFK(ServerPlayerEntity player) {
        afkPlayers.remove(player.getUuid());
        lastActivity.remove(player.getUuid());
        removeAFKPrefix(player);
    }


    private static void addAFKPrefix(ServerPlayerEntity player) {
        Scoreboard scoreboard = player.getEntityWorld().getServer().getScoreboard();

        Team team = scoreboard.getTeam("afk");
        // Create team if it does not exist
        if (team == null) {
            team = scoreboard.addTeam("afk");
            team.setPrefix(Text.literal("[AFK] ").formatted(Formatting.GRAY));
        }
        scoreboard.addScoreHolderToTeam(player.getNameForScoreboard(), team);
    }
    private static void removeAFKPrefix(ServerPlayerEntity player) {

        Team playerteam = player.getScoreboardTeam();
        if (playerteam == null) {
            return;
        }

        Scoreboard scoreboard = player.getEntityWorld().getServer().getScoreboard();
        Team team = scoreboard.getTeam("afk");
        if (team != null) {
            scoreboard.removeScoreHolderFromTeam(player.getNameForScoreboard(), team);
        }
    }

}
