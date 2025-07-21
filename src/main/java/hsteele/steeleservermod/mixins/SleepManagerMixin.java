package hsteele.steeleservermod.mixins;


import hsteele.steeleservermod.AFKSystem.AFKManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.SleepManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(SleepManager.class)
public abstract class SleepManagerMixin {

    @Shadow private int total;
    @Shadow private int sleeping;

    /**
     * @author Hayden
     * @reason Exclude AFK Players from sleep count
     */
    @Overwrite
    public boolean update(List<ServerPlayerEntity> players) {
        int i = this.total;
        int j = this.sleeping;
        this.total = 0;
        this.sleeping = 0;

        for (ServerPlayerEntity serverPlayerEntity : players) {
            if (!serverPlayerEntity.isSpectator() && !AFKManager.isAFK(serverPlayerEntity)) {
                this.total++;
                if (serverPlayerEntity.isSleeping()) {
                    this.sleeping++;
                }
            }
        }

        return (j > 0 || this.sleeping > 0) && (i != this.total || j != this.sleeping);
    }

}
