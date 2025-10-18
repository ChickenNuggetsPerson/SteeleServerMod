package hsteele.steeleservermod.WalkerSystem;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Leg {

    public Vec3d basePosition; // The root (e.g., hip) position of the leg
    public Vec3d targetPosition; // The target position for the end effector (e.g., foot)
    public List<Double> segmentLengths; // Lengths of each limb segment
    public List<Vec3d> jointPositions; // Calculated positions of the joints
    public List<DisplayEntity.BlockDisplayEntity> entities;
    public double length = 0.0;

    public Leg(Vec3d basePosition, List<Double> segmentLengths, World world) {
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
            DisplayEntity.BlockDisplayEntity entity = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world);
            entity.setPosition(basePosition);
            this.entities.add(entity);
            world.spawnEntity(entity);
        }

        this.setState(Blocks.GRAY_CONCRETE.getDefaultState());
    }

    private BlockState state;
    public void setState(BlockState state) {
        this.state = state;
    }


    public void updateLegDisplayEntities() {

        List<Vec3d> jointPositions = this.jointPositions;

        for (int i = 0; i < entities.size(); i++) {
            Vec3d start = jointPositions.get(i);
            Vec3d end = jointPositions.get(i + 1);

            // Calculate direction vector and rotation
            Vec3d direction = end.subtract(start);
            Vector3f normalizedDirection = new Vector3f((float) direction.x, (float) direction.y, (float) direction.z);
            normalizedDirection.normalize();

            Quaternionf rotation = calculateRotation(normalizedDirection);

            // Calculate translation (position of the segment's base)
            Vector3f translation = rotation.transform(new Vector3f(-0.1f, 0f, -0.1f));

            // Calculate scale (length of the segment along its axis)
            Vector3f scale = new Vector3f(0.25f, (float) direction.length(), (float)0.25f);

            // Create the affine transformation
            AffineTransformation transformation = new AffineTransformation(
                    translation,
                    rotation, // Left rotation
                    scale,
                    new Quaternionf() // No additional rotation applied
            );

            // Apply transformation to the BlockDisplayEntity
            entities.get(i).setTransformation(transformation);
            entities.get(i).setPosition(start);
            entities.get(i).setBlockState(state);
            entities.get(i).setTeleportDuration(1);
            entities.get(i).setInterpolationDuration(1);
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