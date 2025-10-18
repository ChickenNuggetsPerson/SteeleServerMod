package hsteele.steeleservermod.WalkerSystem;

import hsteele.steeleservermod.Steeleservermod;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Walker {
    Vec3d pos;
    Quaternionf rotation = new Quaternionf(0, 0, 0, 1);
    ServerWorld world;

    List<Leg> legs;
    List<Vec3d> legTargets;
    List<Vec3d> prevLegTargets;
    List<Vec3d> legPoses;

    double time = 0.0;
    ServerPlayerEntity player;
    boolean alive = true;

    public Walker(Vec3d position, ServerPlayerEntity player, ServerWorld world) {

        this.pos = position;
        this.legs = new ArrayList<>();
        this.legTargets = new ArrayList<>();
        this.prevLegTargets = new ArrayList<>();
        this.legPoses = new ArrayList<>();
        this.player = player;
        this.world = world;

        this.createBody();

        Steeleservermod.LOGGER.info("Created Walker: " + this.player.getName().getString());
    }

    public void addLeg(Leg leg) {
        legs.add(leg);
        legTargets.add(leg.basePosition);
        legPoses.add(leg.basePosition);
        prevLegTargets.add(leg.basePosition);
    }

    public void tick() {
        if (!alive) { return; }

        this.time += 1;

        double cycleTime = 2.5;
        PlayerInput input = null;
        boolean walking = false;
        boolean isSeated = false;

        this.updateBody();

        if (this.seat.hasPassengers() && this.seat.getFirstPassenger() instanceof ServerPlayerEntity p) {
            this.player = p;

            p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 1, 1000, false, false));
            seat.fallDistance = 0;

            input = this.player.getPlayerInput();
            walking = input.backward() || input.forward() || input.left() || input.right();
            isSeated = true;
        }

        this.updateLegBasePos();
        this.updatePrevLegTargets();
        if (isSeated) {
            if (input.jump()) {
                this.updateJumpLegs();
            } else if (walking) {
                this.updateWalkInDir(cycleTime, input);
            } else {
                this.updateIdleLegs();
            }

        } else {
            this.updateIdleLegs();
        }

        for (int i = 0; i < legs.size(); i++) { // Move Legs

            this.moveLegs(i);

            Leg leg = legs.get(i);
            Solver.solveLegIK(leg, this.legPoses.get(i));
            leg.updateLegDisplayEntities();
        }

        this.updatePhysics();
        if (seat.isRemoved()) {
            this.kill();
        }

    }
    public void kill() {
        for (int i = 0; i < legs.size(); i++) {
            Leg leg = legs.get(i);
            for (int z = 0; z < leg.entities.size(); z++) {

                leg.entities.get(z).discard();

            }
            leg.segmentLengths = new ArrayList<>();
        }

        body.discard();
        frontBody.discard();
        backBody.discard();
        seat.discard();
        player.setInvisible(false);

        alive = false;
        Steeleservermod.LOGGER.info("Killed Walker");

        WalkerStorage.SHARED.removeWalker(this);
    }

    private void moveLegs(int i) {
        Leg leg = legs.get(i);

        this.legTargets.set(i, limitVec(leg.basePosition, this.legTargets.get(i), leg.length));

        if (this.pos.distanceTo(this.legPoses.get(i)) > leg.length) {
            this.legPoses.set(i, this.pos);
        }

        Vec3d target = this.legTargets.get(i);
        Vec3d pos = this.legPoses.get(i);

        Vec3d newPos = collide(pos, target);

        if (isInside(newPos)) {
            newPos = newPos.add(0, 0.02, 0);
        }

        this.legPoses.set(i, limitVec(leg.basePosition, newPos, leg.length));
    }

    private Vec3d limitVec(Vec3d base, Vec3d desired, double length) {
        Vec3d delta = desired.subtract(base);
        if (delta.length() > length) {
            return delta.normalize().multiply(length);
        }
        return desired;
    }
    private Vec3d getCircleVec(int index, int amt, double radius) {
        return getCircleVec(index, amt, radius, 0);
    }
    private Vec3d getCircleVec(int index, int amt, double radius, double shift) {
        // Compute the circle vector in local space
        double theta = Math.TAU * ((double) index / (double) amt);
        Vec3d localVec = new Vec3d(
                Math.cos(theta + shift) * radius,
                0,
                Math.sin(theta + shift) * radius
        );

        // Convert Vec3d to Vector3f for quaternion operations
        Vector3f vector = new Vector3f((float) localVec.x, (float) localVec.y, (float) localVec.z);

        // Apply the quaternion rotation
        rotation.transform(vector);

        // Convert back to Vec3d and return
        return new Vec3d(vector.x(), vector.y(), vector.z());
    }

    private void updateWalkInDir(double cycleTime, PlayerInput input) {
        double maxWidth = 1.5;
        double maxHeight = 2.0;
        double maxPush = -0.2;

        double groundPercent = 0.5;
        double pushPercent = 0.3;

        float x = 0;
        float z = 0;

        if (input.forward()) z += 1;
        if (input.backward()) z -= 1;
        if (input.left()) x += 1;
        if (input.right()) x -= 1;

        boolean isFB = input.forward() || input.backward();
        boolean isLR = input.left() || input.right();

        if (input.sprint()) {
            cycleTime = cycleTime * 0.3;
        }

        Vec3d moveVec = new Vec3d(x, 0, z).normalize();
        Vector3f rotated = this.rotation.transform(moveVec.toVector3f());

        moveVec = new Vec3d(rotated.x(), rotated.y(), rotated.z());

        for (int i = 0; i < legs.size(); i++) {

            double offset = i % 2 == 0 ? 0 : (cycleTime / 2);
            double seconds = (offset + (time / 20)) % cycleTime;

            double hrzFactor = Walker.walkingHorzAnim(seconds, cycleTime, groundPercent, maxWidth);
            double vrtFactor = Walker.walkingHeightAnim(seconds, cycleTime, groundPercent, pushPercent, maxHeight, maxPush);

            Vec3d target = pos.add(getCircleVec(i, legs.size(), 2, hrzFactor * -x * 0.2))
                    .add(0, -2, 0);

            if (isFB) {
                target = target.add(moveVec.multiply(hrzFactor));
            }
            target = target.add(0, vrtFactor, 0);

            legTargets.set(i, target);
        }
    }

    // https://www.desmos.com/calculator/xwgpxt5i8k
    private static double walkingHeightAnim(double x, double animTime, double groundPercent, double pushPercent, double maxHeight, double pushDown) {

        // Blue Line - Push line
        if ((groundPercent - pushPercent) * animTime < x && x < groundPercent * animTime) {
            return (pushDown / (pushPercent * animTime))
                    * (x - ((groundPercent - pushPercent) * animTime));
        }

        if (x > groundPercent * animTime && x < animTime) {
            double c = (2*animTime - (1 - groundPercent) * animTime) / 2;
            return ((2 * maxHeight) / ((1-groundPercent)*animTime))
                    * Math.sqrt(
                    Math.pow(((1-groundPercent)*animTime)/2, 2)
                    - Math.pow(x - c, 2)
            );
        }

        return 0;
    }
    private static double BezierBlend(double t) { return t * t * (3.0f - 2.0f * t); }
    private static double walkingHorzAnim(double x, double animTime, double groundPercent, double maxOffset) {
        if (x < 0 || x > animTime) { return maxOffset; }

        // Left side
        if (x < groundPercent * animTime) {
            return (-2*maxOffset) / (groundPercent*animTime) * x + maxOffset;
        }

        return (2 * maxOffset * Walker.BezierBlend((x - groundPercent*animTime) / ((1 - groundPercent) * animTime))) - maxOffset;
    }

    private void updateIdleLegs() {

        double seconds = (time / 20);

        for (int i = 0; i < legs.size(); i++) {

            Vec3d target = pos.add(getCircleVec(i, legs.size(), 2));
            target = target.add(0, -1.1 + Math.sin(seconds / 5) * 0.3, 0);

            legTargets.set(i, target);
        }
    }
    private void updateJumpLegs() {
        for (int i = 0; i < legs.size(); i++) {

            Vec3d target = pos.add(getCircleVec(i, legs.size(), 0.7));
            target = target.add(0, -3.5, 0);

            legTargets.set(i, target);
        }
    }
    private void updateLegBasePos() {
        for (int i = 0; i < legs.size(); i++) {
            legs.get(i).basePosition = this.pos
                    .add(getCircleVec(i, legs.size(), 0.2));
        }
    }
    private void updatePrevLegTargets() {
        for (int i = 0; i < this.legTargets.size(); i++) {
            Vec3d target = this.legTargets.get(i);
            if (Double.isNaN(target.x) || Double.isNaN(target.y) || Double.isNaN(target.z)) {
                target = this.pos;
                this.legTargets.set(i, target);
            }

            this.prevLegTargets.set(i, target);
        }
    }


    private DisplayEntity.BlockDisplayEntity body;
    private DisplayEntity.BlockDisplayEntity frontBody;
    private DisplayEntity.BlockDisplayEntity backBody;
    private WalkerMinecart seat;
    // Body System
    private void createBody() {
        body = createDisplayEntity();
        frontBody = createDisplayEntity();
        backBody = createDisplayEntity();

        frontBody.setBlockState(Blocks.LIME_CONCRETE.getDefaultState());
        backBody.setBlockState(Blocks.RED_CONCRETE.getDefaultState());

        WalkerMinecart ride = new WalkerMinecart(EntityType.MINECART, world);
        seat = ride;
        world.spawnEntity(ride);

    }
    private DisplayEntity.BlockDisplayEntity createDisplayEntity() {
        DisplayEntity.BlockDisplayEntity entity = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world);
        entity.setPosition(pos);
        entity.setBlockState(Blocks.GLASS.getDefaultState());
        world.spawnEntity(entity);
        return entity;
    }
    private void updateDisplayEntity(DisplayEntity.BlockDisplayEntity e, float scaleX, float scaleY, float scaleZ, float transformX, float transformY, float transformZ) {

        // Set scale for the block
        Vector3f scale = new Vector3f(scaleX, scaleY, scaleZ);

        // Set translation relative to the base position
        Vector3f translation = rotation.transform(new Vector3f(transformX, transformY, transformZ));

        // Create the affine transformation
        AffineTransformation transformation = new AffineTransformation(
                translation,   // Translation
                rotation.normalize(),  // Rotation quaternion
                scale,         // Scale
                new Quaternionf() // No additional rotation applied
        );

        // Apply transformation and position to the display block entity
        e.setTransformation(transformation);
        e.setPosition(pos);
        e.setInterpolationDuration(1);
        e.setTeleportDuration(1);
    }


    private void updateBody() {

        updateDisplayEntity(body, 2, 2, 2, -1f, 0f, -1f);
        updateDisplayEntity(frontBody, 2.2f, 0.2f, 0.2f, -1.1f, -0.05f, 0.9f);
        updateDisplayEntity(backBody, 2.2f, 0.2f, 0.2f, -1.1f, -0.05f, -1.1f);

        seat.setPosition(pos.add(velocity.multiply(7, 0, 7)));
        seat.rotate(0, false, 0, false);
    }



    // Walker Physics
    private Vec3d velocity = new Vec3d(0.0, 0.0, 0.0);
    private Vec3d r_velocity = new Vec3d(0.0, 0.0, 0.0);

    private Vec3d collide(Vec3d currentPos, Vec3d target) {
        BlockHitResult result = world.raycast(new RaycastContext(currentPos, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, body));
        return result.getPos();
    }
    private void updatePhysics() {
        double deltaTime = 1.0 / 20.0;


        this.pos = pos.add(velocity);

        Pair<Vec3d, Vec3d> forceResult = calcLegForces();

        // Add forces to velocity
        Vec3d forces = forceResult.getLeft();
        this.velocity = this.velocity.add(forces);
        if (this.velocity.length() > 10) {
            this.velocity = this.velocity.normalize().multiply(10);
        }


        Vec3d torque = forceResult.getRight(); // Total torque from forces
        Vec3d inertia = new Vec3d(1, 1, 1); // Moment of inertia for each axis

        // Update angular velocity )
        this.r_velocity = this.r_velocity.add(
                (torque.x / inertia.x) * deltaTime,
                (torque.y / inertia.y) * deltaTime,
                (torque.z / inertia.z) * deltaTime
        );

        // Update rotation
        Quaternionf dq = new Quaternionf(
                (float)(r_velocity.x * deltaTime * 20),
                (float)(r_velocity.y * deltaTime * 20),
                (float)(r_velocity.z * deltaTime * 20),
                0
        );
        dq.mul(rotation);
        rotation.add(dq).normalize();

        // Gravity
        this.velocity = this.velocity.add(0, -0.01, 0);

        // Avoid inside clipping
        while (isInside(this.pos)) {
            this.pos = this.pos.add(0, 0.01, 0);
        }

        // Drag
        this.velocity = this.velocity.multiply(
                0.97,
                this.velocity.y > 0 ? 0.6 : 0.97,
                0.97
        );
        this.r_velocity = this.r_velocity.multiply(0.95);
    }

    private Pair<Vec3d, Vec3d> calcLegForces() {
        Vec3d forces = new Vec3d(0.0, 0.0, 0.0);
        Vec3d r_forces = new Vec3d(0, 0.0, 0);

        // Calc Center of Mass
        Vec3d COM = pos.add(0, -0.4, 0);

        // Debug
//        world.spawnParticles(ParticleTypes.FLAME, COM.getX(), COM.getY(), COM.getZ(), 1, 0, 0, 0, 0);

        for (int i = 0; i < legs.size(); i++) { // Calculate forces for the legs

            if (!(
                    !isInside(this.legPoses.get(i)) && isInside(this.legPoses.get(i).add(0, -0.1, 0))
            )) { // Leg doesn't apply force when in air
                continue;
            }

            Vec3d r = this.legPoses.get(i).subtract(COM); // Vector from COM to contact point
            Vec3d reactionForce = calculateReactionForce(this.legPoses.get(i), this.legTargets.get(i), this.prevLegTargets.get(i));

            forces = forces.add(reactionForce);
            r_forces = r_forces.add(r.crossProduct(reactionForce));
        }

        return new Pair<>(forces, r_forces);
    }

    private Boolean isInside(Vec3d footPosition) {
        Vec3d end = footPosition.add(0, -0.01, 0); // Trace downward
        return world.raycast(new RaycastContext(footPosition, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, body)).isInsideBlock();
    }
    public Vec3d calculateReactionForce(Vec3d pos, Vec3d target, Vec3d prevTarget) {
        Vec3d deltaPos = target.subtract(prevTarget);
        if (deltaPos.length() > 1) {
            deltaPos = new Vec3d(0, 0, 0);
        }

        Vec3d normalForce = pos.subtract(target) // Calc Normal Force
                .multiply((double) 1 / (double) legs.size()); // Scale by the amt of legs

        Vec3d frictionForce = deltaPos.multiply(-0.05 * normalForce.length());
        return normalForce.multiply(0.04).add(frictionForce);
    }

}
