package hsteele.steeleservermod.mixins.AFKMixins;

import hsteele.steeleservermod.AFKSystem.AFKManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.PhantomSpawner;

@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;players()Ljava/util/List;"
            )
    )
    private List<ServerPlayer> redirectPlayerList(ServerLevel world) {
        List<ServerPlayer> original = world.players();

        return original.stream()
                .filter(player -> !AFKManager.isAFK(player))
                .toList();
    }

}
