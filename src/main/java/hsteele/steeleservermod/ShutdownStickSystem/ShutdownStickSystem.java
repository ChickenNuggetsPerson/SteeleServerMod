package hsteele.steeleservermod.ShutdownStickSystem;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Objects;


public class ShutdownStickSystem {

    private static final String stickName = "SHUTDOWN";

    private static final DustParticleEffect c = new DustParticleEffect(15221045, 1.0f);
    private static final DustParticleEffect p = new DustParticleEffect(6043112, 0.6f);
    private static final DustParticleEffect w = new DustParticleEffect(16777215, 0.6f);

    private static boolean running = false;
    private static long startTime = 0;

    private static boolean explosionRunning = false;
    private static long explosionStartTime = 0;
    private static Vec3d explosionStart = new Vec3d(0, 200, 0);
    private static Vec3d explosionCenter = new Vec3d(0, 200, 0);
    private static World explosionWorld = null;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ShutdownStickSystem::tick);
        UseItemCallback.EVENT.register((player, world, hand) -> {

            if (isHoldingStick(player)) {
                stickClicked(player, world, hand);
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getPermissionLevel() != 4) { continue; }
            if (!player.getUuidAsString().equals("b655d895-9d38-44c6-b7c8-1b2df1db69b0")) { continue; }
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

    private static boolean isHoldingStick(ServerPlayerEntity player) {
        ItemStack stack = player.getStackInHand(player.getActiveHand());
        if (stack.getItem() != Items.STICK) { return false; }
        Text name = stack.getCustomName();
        if (name != null) {
            return name.getString().equals(stickName);
        }
        return false;
    }
    private static boolean isHoldingStick(PlayerEntity player) {
        ItemStack stack = player.getStackInHand(player.getActiveHand());
        if (stack.getItem() != Items.STICK) { return false; }
        Text name = stack.getCustomName();
        if (name != null) {
            return name.getString().equals(stickName);
        }
        return false;
    }


    private static <T extends ParticleEffect> void raycastParticle(T parameters, Vec3d startPos, Vec3d endPos, ServerWorld world) {
        double dist = startPos.distanceTo(endPos);
        Vec3d dir = endPos.subtract(startPos).normalize();
        int steps = 100;

        for (double i = 0; i < steps; i++) {
            Vec3d pos = startPos.add(dir.multiply(dist * (i / steps)));
            world.spawnParticles(parameters, true, true, pos.getX(), pos.getY(), pos.getZ(), 10, 0, 0, 0, 0.02);
        }
    }
    private static <T extends ParticleEffect> void renderCircle(T parameters, Vec3d center, double radius, ServerWorld world) {

        for (double h = -Math.PI / 2; h < Math.PI/2; h += 0.1) {
            for (double angle = 0; angle < Math.PI * 2; angle += 0.1) {

                double r = radius * Math.cos(h);
                Vec3d position = center
                        .add(0, Math.sin(h) * radius, 0)
                        .add(Math.sin(angle) * r, 0, Math.cos(angle) * r);

                world.spawnParticles(parameters, true, true, position.getX(), position.getY(), position.getZ(), 1, 0, 0, 0, 0);

            }
        }

    }


    private static void playerHoldingTick(ServerPlayerEntity player) {
        if (explosionRunning) { return; }

        long time = System.currentTimeMillis() - startTime;

        ServerWorld world = player.getEntityWorld();
        Vec3d playerPos = player.getEntityPos();

        double particleDist = 0.75;
        double angleOffset = (double) System.currentTimeMillis() / 600;

        for (double angle = 0; angle < Math.PI * 2; angle += 0.1) {

            double baseSize = anim(time, 100, 2000) * 2;
            Vec3d basePos = playerPos
                    .add(
                            Math.cos(angle) * particleDist * baseSize,
                            0.8,
                            Math.sin(angle) * particleDist * baseSize
                    );

            if (Math.sin(angle + angleOffset) > 0.95 ) {
                world.spawnParticles(c, true, true, basePos.getX(), basePos.getY(), basePos.getZ(), 1, 0, 0, 0, 0);
            }

            double c3Size = anim(time, 100, 2000) * 1.2 + Math.cos(-angleOffset) * 0.8;
            Vec3d c3 = playerPos
                    .add(
                            Math.cos(angle) * c3Size,
                            anim(time, 500, 3000) * (Math.sin(-angleOffset)*0.5 + 0.8),
                            Math.sin(angle) * c3Size
                    );
            world.spawnParticles(w, true, true, c3.getX(), c3.getY(), c3.getZ(), 1, 0, 0, 0, 0);

        }
    }


    private static void stickClicked(PlayerEntity player, World world, Hand hand) {
        Vec3d startPos = player.raycast(1, 1, false).getPos();
        HitResult result = player.raycast(30, 1, false);

        explosionRunning = true;
        explosionStartTime = System.currentTimeMillis();
        explosionStart = startPos;
        explosionCenter = result.getPos();
        explosionWorld = world;
    }
    private static void renderExplosion(MinecraftServer server) {
        long time = System.currentTimeMillis() - explosionStartTime;
        ServerWorld world = Objects.requireNonNull(server.getWorld(explosionWorld.getRegistryKey()));

        if (time < 1000) {
            Vec3d part = explosionStart
                    .add(explosionCenter.subtract(explosionStart).multiply(
                            anim(time, 0, 700)
                    ));

                    world.spawnParticles(c, true, true, part.getX(), part.getY(), part.getZ(), 10, 0, 0, 0, 0.02);
        }

        if (time > 1100 && time < 2400) {
            renderCircle(
                    w,
                    explosionCenter,
                    anim(time, 1100, 2000) * 8,
                    server.getWorld(explosionWorld.getRegistryKey())
            );
        }

        if (time > 2000 && time < 2900) {
            renderCircle(
                    ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                    explosionCenter,
                    anim(time, 1200, 2500) * 8.5,
                    server.getWorld(explosionWorld.getRegistryKey())
            );
        }

        if (time > 2600 && time < 3900) {
            renderCircle(
                    ParticleTypes.FLAME,
                    explosionCenter,
                    8.5,
                    server.getWorld(explosionWorld.getRegistryKey())
            );
        }


        if (time > 3700) {
            double spread = 4;
            LightningEntity le = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
            le.setPosition(explosionCenter.add(
                    randomInRange(-spread, spread),
                    randomInRange(-spread, spread),
                    randomInRange(-spread, spread)
            ));
            world.spawnEntity(le);
        }

        if (time > 4000) {
            server.stop(false);
            explosionRunning = false;
        }

    }
}
