package hsteele.steeleservermod.ShutdownStickSystem;

import hsteele.steeleservermod.Commands.RunCommand;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Objects;


public class ShutdownStickSystem {

    private static final String stickName = "SHUTDOWN";

    private static final DustParticleOptions c = new DustParticleOptions(15221045, 1.0f);
    private static final DustParticleOptions p = new DustParticleOptions(6043112, 0.6f);
    private static final DustParticleOptions w = new DustParticleOptions(16777215, 0.6f);

    private static boolean running = false;
    private static long startTime = 0;

    private static boolean explosionRunning = false;
    private static long explosionStartTime = 0;
    private static Vec3 explosionStart = new Vec3(0, 200, 0);
    private static Vec3 explosionCenter = new Vec3(0, 200, 0);
    private static Level explosionWorld = null;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ShutdownStickSystem::tick);
        UseItemCallback.EVENT.register((player, world, hand) -> {

            if (isHoldingStick(player)) {
                stickClicked(player, world, hand);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.getStringUUID().equals("b655d895-9d38-44c6-b7c8-1b2df1db69b0")) { continue; }
            if (!isHoldingStick((player))) {
                running = false;
                continue;
            }

            if (!running || explosionRunning) {
                running = true;
                startTime = System.currentTimeMillis();
            }

            playerHoldingTick(player);
        }

        if (explosionRunning) {
            renderExplosion(server);
        }
    }

    private static double BezierBlend(double t)
    {
        return t * t * (3.0f - 2.0f * t);
    }
    private static double anim(long time, int start, int end) {
        if (time < start) {
            return 0;
        }
        if (time > end) {
            return 1;
        }
        int range = end - start;
        return BezierBlend((double) (time - start) / range);
    }
    private static double randomInRange(double min, double max) {
        double range = max - min;
        return min + (range * Math.random());
    }

    private static boolean isHoldingStick(ServerPlayer player) {
        ItemStack stack = player.getItemInHand(player.getUsedItemHand());
        if (stack.getItem() != Items.STICK) { return false; }
        Component name = stack.getCustomName();
        if (name != null) {
            return name.getString().equals(stickName);
        }
        return false;
    }
    private static boolean isHoldingStick(Player player) {
        ItemStack stack = player.getItemInHand(player.getUsedItemHand());
        if (stack.getItem() != Items.STICK) { return false; }
        Component name = stack.getCustomName();
        if (name != null) {
            return name.getString().equals(stickName);
        }
        return false;
    }


    private static <T extends ParticleOptions> void raycastParticle(T parameters, Vec3 startPos, Vec3 endPos, ServerLevel world) {
        double dist = startPos.distanceTo(endPos);
        Vec3 dir = endPos.subtract(startPos).normalize();
        int steps = 100;

        for (double i = 0; i < steps; i++) {
            Vec3 pos = startPos.add(dir.scale(dist * (i / steps)));
            world.sendParticles(parameters, true, true, pos.x(), pos.y(), pos.z(), 10, 0, 0, 0, 0.02);
        }
    }
    private static <T extends ParticleOptions> void renderCircle(T parameters, Vec3 center, double radius, ServerLevel world) {

        for (double h = -Math.PI / 2; h < Math.PI/2; h += 0.1) {
            for (double angle = 0; angle < Math.PI * 2; angle += 0.1) {

                double r = radius * Math.cos(h);
                Vec3 position = center
                        .add(0, Math.sin(h) * radius, 0)
                        .add(Math.sin(angle) * r, 0, Math.cos(angle) * r);

                world.sendParticles(parameters, true, true, position.x(), position.y(), position.z(), 1, 0, 0, 0, 0);

            }
        }

    }


    private static void playerHoldingTick(ServerPlayer player) {
        if (explosionRunning) { return; }

        long time = System.currentTimeMillis() - startTime;

        ServerLevel world = player.level();
        Vec3 playerPos = player.position();

        double angleOffset = (double) System.currentTimeMillis() / 400;

        for (double angle = 0; angle < Math.PI * 2; angle += 0.1) {

            if (Math.sin(angle + angleOffset) > 0.95 ) {
                double baseSize = anim(time, 100, 2000);
                Vec3 basePos = playerPos.add(
                        Math.cos(angle) * baseSize,
                        0.6,
                        Math.sin(angle) * baseSize
                );
                world.sendParticles(c, true, true, basePos.x(), basePos.y(), basePos.z(), 1, 0, 0, 0, 0);
            }

        }
    }


    private static void stickClicked(Player player, Level world, InteractionHand hand) {
        Vec3 startPos = player.pick(1, 1, false).getLocation();
        HitResult result = player.pick(30, 1, false);

        explosionRunning = true;
        explosionStartTime = System.currentTimeMillis();
        explosionStart = startPos;
        explosionCenter = result.getLocation();
        explosionWorld = world;
    }
    private static void renderExplosion(MinecraftServer server) {
        if (!explosionRunning) { return; }

        long time = System.currentTimeMillis() - explosionStartTime;
        ServerLevel world = Objects.requireNonNull(server.getLevel(explosionWorld.dimension()));

        if (time < 1000) {
            Vec3 part = explosionStart
                    .add(explosionCenter.subtract(explosionStart).scale(
                            anim(time, 0, 700)
                    ));

                    world.sendParticles(c, true, true, part.x(), part.y(), part.z(), 10, 0, 0, 0, 0.02);
        }

        if (time > 1100 && time < 2400) {
            renderCircle(
                    w,
                    explosionCenter,
                    anim(time, 1100, 2000) * 8,
                    server.getLevel(explosionWorld.dimension())
            );
        }

        if (time > 2000 && time < 2900) {
            renderCircle(
                    ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                    explosionCenter,
                    anim(time, 1200, 2500) * 8.5,
                    server.getLevel(explosionWorld.dimension())
            );
        }

        if (time > 2600 && time < 3900) {
            renderCircle(
                    ParticleTypes.FLAME,
                    explosionCenter,
                    8.5,
                    server.getLevel(explosionWorld.dimension())
            );
        }


        if (time > 3700) {
            double spread = 4;
            LightningBolt le = new LightningBolt(EntityType.LIGHTNING_BOLT, world);
            le.setPos(explosionCenter.add(
                    randomInRange(-spread, spread),
                    randomInRange(-spread, spread),
                    randomInRange(-spread, spread)
            ));
            world.addFreshEntity(le);
        }

        if (time > 4000) {
            explosionRunning = false;
            RunCommand.runRestartScript();
        }

    }
}
