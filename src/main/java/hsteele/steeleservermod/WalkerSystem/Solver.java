package hsteele.steeleservermod.WalkerSystem;

import net.minecraft.util.math.Vec3d;
import java.util.List;

public class Solver {

    public static void solveLegIK(Leg leg, Vec3d target) {
        List<Vec3d> jointPositions = leg.jointPositions;
        List<Double> segmentLengths = leg.segmentLengths;
        double totalLength = segmentLengths.stream().mapToDouble(Double::doubleValue).sum();

        // Step 1: Clamp target position within the reachable range
        double distanceToTarget = leg.basePosition.distanceTo(target);
        if (distanceToTarget > totalLength) {
            Vec3d direction = target.subtract(leg.basePosition).normalize();
            target = leg.basePosition.add(direction.multiply(totalLength));
        }

        // Forward Reaching Phase
        jointPositions.set(jointPositions.size() - 1, target); // Set the end effector to the target
        for (int i = jointPositions.size() - 2; i >= 0; i--) {
            Vec3d next = jointPositions.get(i + 1);
            Vec3d current = jointPositions.get(i);
            double length = segmentLengths.get(i);

            // Calculate the direction and new position for the joint
            Vec3d direction = current.subtract(next).normalize();

            // Apply upward bias for the last joint
            if (i == jointPositions.size() - 2) {
                Vec3d upwardBias = new Vec3d(0, 1, 0); // Bias direction (upward)
                double biasWeight = 0.4; // Adjust the weight of the bias (0 = no bias, 1 = full upward)
                direction = direction.multiply(1 - biasWeight).add(upwardBias.multiply(biasWeight)).normalize();
            }

            if (i == 1) {
                Vec3d upwardBias = new Vec3d(0, 1, 0); // Bias direction (upward)
                double biasWeight = 0.5; // Adjust the weight of the bias (0 = no bias, 1 = full upward)
                direction = direction.multiply(1 - biasWeight).add(upwardBias.multiply(biasWeight)).normalize();
            }

            jointPositions.set(i, next.add(direction.multiply(length)));
        }

        // Backward Reaching Phase
        jointPositions.set(0, leg.basePosition); // Anchor the base to its original position
        for (int i = 1; i < jointPositions.size(); i++) {
            Vec3d previous = jointPositions.get(i - 1);
            Vec3d current = jointPositions.get(i);
            double length = segmentLengths.get(i - 1);

            // Calculate the direction and new position for the joint
            Vec3d direction = current.subtract(previous).normalize();
            jointPositions.set(i, previous.add(direction.multiply(length)));
        }


        // Step 4: Update leg target position
        leg.targetPosition = target;
    }

}
