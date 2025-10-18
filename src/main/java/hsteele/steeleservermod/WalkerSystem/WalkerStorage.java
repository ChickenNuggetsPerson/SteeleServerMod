package hsteele.steeleservermod.WalkerSystem;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;


public class WalkerStorage {

    public static void register() {

        UseItemCallback.EVENT.register((player, world, hand) -> {

            if (isHoldingSpawner(player)) {

                ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(player.getUuid());
                if (p == null) {
                    return ActionResult.PASS;
                }

                HitResult result = player.raycast(10, 1.0f, false);
                Vec3d pos = result.getPos();

                List<Double> segmentLengths = new ArrayList<>();

                Walker walker = new Walker(pos, p, world.getServer().getWorld(world.getRegistryKey()));

                segmentLengths.add(0.5);
                segmentLengths.add(1.6);
                segmentLengths.add(1.4);
                segmentLengths.add(1.0);

                for (int i = 0; i < 6; i++) {

                    walker.addLeg(new Leg(pos, segmentLengths, world));
                }

                WalkerStorage.SHARED.addWalker(walker);

                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });

        ServerTickEvents.START_WORLD_TICK.register(world -> {
            WalkerStorage.SHARED.tick();

            List<ServerPlayerEntity> players = world.getPlayers();
            for (ServerPlayerEntity player: players) {
                if (isHoldingSpawner(player)) {
                    Vec3d pos = player.raycast(10, 1.0f, false).getPos();
                    world.spawnParticles(ParticleTypes.END_ROD, pos.getX(), pos.getY() + 0.1, pos.getZ(), 1, 0, 0, 0, 0);
                }
            }
        });
    }

    private static boolean isHoldingSpawner(PlayerEntity player) {
        if (player.getStackInHand(player.getActiveHand()).getItem() != Items.AMETHYST_SHARD) { return false; }
        Text name = player.getStackInHand(player.getActiveHand()).getCustomName();
        if (name == null) { return false; }
        return name.getString().equals("Walker");
    }

    public static WalkerStorage SHARED = new WalkerStorage();

    private final List<Walker> walkers = new ArrayList<>();

    public void addWalker(Walker w) {
        walkers.add(w);
    }
    public void removeWalker(Walker w) {
        walkers.remove(w);
    }

    public void tick() {
        for (int i = 0; i < walkers.size(); i++) {
            walkers.get(i).tick();
        }
    }


}
