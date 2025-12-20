package hsteele.steeleservermod.WalkerSystem;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Leg {

    public Vec3 basePosition; // The root (e.g., hip) position of the leg
    public Vec3 targetPosition; // The target position for the end effector (e.g., foot)
    public List<Double> segmentLengths; // Lengths of each limb segment
    public List<Vec3> jointPositions; // Calculated positions of the joints
    public List<Display.BlockDisplay> entities;
    public double length = 0.0;

    public Leg(Vec3 basePosition, List<Double> segmentLengths, Level world) {
        this.basePosition = basePosition;
        this.segmentLengths = segmentLengths;
        this.jointPositions = new ArrayList<>(segmentLengths.size() + 1);

        // Initialize joint positions (base + segments)
        this.jointPositions.add(basePosition);
        for (int i = 0; i < segmentLengths.size(); i++) {
            this.jointPositions.add(basePosition); // Placeholder positions

            this.length += segmentLengths.get(i);
        }

        // Create Display Entities
        this.entities = new ArrayList<>();
        for (int i = 0; i < segmentLengths.size(); i++) {
            Display.BlockDisplay entity = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, world);
            entity.setPos(basePosition);
            this.entities.add(entity);
            world.addFreshEntity(entity);
        }

        this.setState(Blocks.GRAY_CONCRETE.defaultBlockState());
    }

    private BlockState state;
    public void setState(BlockState state) {
        this.state = state;
    }


    public void updateLegDisplayEntities() {

        List<Vec3> jointPositions = this.jointPositions;

        for (int i = 0; i < entities.size(); i++) {
            Vec3 start = jointPositions.get(i);
            Vec3 end = jointPositions.get(i + 1);

            // Calculate direction vector and rotation
            Vec3 direction = end.subtract(start);
            Vector3f normalizedDirection = new Vector3f((float) direction.x, (float) direction.y, (float) direction.z);
            normalizedDirection.normalize();

            Quaternionf rotation = calculateRotation(normalizedDirection);

            // Calculate translation (position of the segment's base)
            Vector3f translation = rotation.transform(new Vector3f(-0.1f, 0f, -0.1f));

            // Calculate scale (length of the segment along its axis)
            Vector3f scale = new Vector3f(0.25f, (float) direction.length(), (float)0.25f);

            // Create the affine transformation
            Transformation transformation = new Transformation(
                    translation,
                    rotation, // Left rotation
                    scale,
                    new Quaternionf() // No additional rotation applied
            );

            // Apply transformation to the BlockDisplayEntity
            entities.get(i).setTransformation(transformation);
            entities.get(i).setPos(start);
            entities.get(i).setBlockState(state);
            entities.get(i).setPosRotInterpolationDuration(1);
            entities.get(i).setTransformationInterpolationDuration(1);
        }

    }

    static Quaternionf calculateRotation(Vector3f direction) {
        Vector3f defaultUp = new Vector3f(0, 1, 0); // Default axis (Y-axis)

        // Normalize both vectors
        direction.normalize();
        defaultUp.normalize();

        // Check if direction is already aligned with defaultUp
        if (direction.equals(defaultUp)) {
            return new Quaternionf(); // Identity quaternion (no rotation needed)
        }

        // Calculate the cross product and dot product
        Vector3f cross = defaultUp.cross(direction, new Vector3f()); // Cross product
        float dot = defaultUp.dot(direction); // Dot product

        // Handle edge cases (e.g., opposite direction)
        if (dot < -0.9999f) {
            // Rotate 180 degrees about an orthogonal axis (e.g., X-axis)
            return new Quaternionf().rotateX((float) Math.PI);
        }

        // Calculate the angle between the vectors
        float angle = (float) Math.acos(dot); // Angle in radians

        // Create the rotation quaternion
        return new Quaternionf().rotationAxis(angle, cross.normalize());
    }




}