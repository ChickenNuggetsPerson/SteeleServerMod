package hsteele.steeleservermod.mixins.AFKMixins;


import hsteele.steeleservermod.AFKSystem.AFKManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(SleepStatus.class)
public abstract class SleepManagerMixin {

    @Shadow private int activePlayers;
    @Shadow private int sleepingPlayers;

    /**
     * @author Hayden
     * @reason Exclude AFK Players from sleep count
     */
    @Overwrite
    public boolean update(List<ServerPlayer> players) {
        int i = this.activePlayers;
        int j = this.sleepingPlayers;
        this.activePlayers = 0;
        this.sleepingPlayers = 0;

        for (ServerPlayer serverPlayerEntity : players) {
            if (!serverPlayerEntity.isSpectator() && !AFKManager.isAFK(serverPlayerEntity)) {
                this.activePlayers++;
                if (serverPlayerEntity.isSleeping()) {
                    this.sleepingPlayers++;
                }
            }
        }

        return (j > 0 || this.sleepingPlayers > 0) && (i != this.activePlayers || j != this.sleepingPlayers);
    }

}
