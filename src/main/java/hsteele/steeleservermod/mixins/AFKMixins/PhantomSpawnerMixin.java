package hsteele.steeleservermod.mixins.AFKMixins;

import hsteele.steeleservermod.AFKSystem.AFKManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.spawner.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {

    @Redirect(
            method = "spawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;getPlayers()Ljava/util/List;"
            )
    )
    private List<ServerPlayerEntity> redirectPlayerList(ServerWorld world) {
        List<ServerPlayerEntity> original = world.getPlayers();

        return original.stream()
                .filter(player -> !AFKManager.isAFK(player))
                .toList();
    }

}
