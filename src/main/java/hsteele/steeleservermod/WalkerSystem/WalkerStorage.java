package hsteele.steeleservermod.WalkerSystem;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;


public class WalkerStorage {

    public static void register() {

        UseItemCallback.EVENT.register((player, world, hand) -> {

            if (
                    player.getStackInHand(hand).getItem() == Items.AMETHYST_SHARD
            ) {

                Text name = player.getStackInHand(hand).getCustomName();
                if (name == null) { return ActionResult.PASS; }
                if (!name.getString().equals("Walker")) { return ActionResult.PASS; }


                HitResult result = player.raycast(10, 1.0f, false);
                Vec3d pos = result.getPos();

                List<Double> segmentLengths = new ArrayList<>();

                Walker walker = new Walker(pos, player, world.getServer().getWorld(world.getRegistryKey()));

                segmentLengths.add(1.6);
                segmentLengths.add(1.4);
                segmentLengths.add(0.9);

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
        });
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
