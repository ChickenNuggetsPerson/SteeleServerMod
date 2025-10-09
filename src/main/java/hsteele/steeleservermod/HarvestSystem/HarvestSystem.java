package hsteele.steeleservermod.HarvestSystem;

import hsteele.steeleservermod.config.ConfigSystem;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.state.property.Properties;

import java.util.List;
import java.util.Objects;

public class HarvestSystem {

    public static void register() {

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

            if (
                    player.isSneaking()
                    && itemIsHarvester(player.getStackInHand(hand).getItem())
                    && isFullyGrownCrop(world.getBlockState(hitResult.getBlockPos()))
            ) {
                useHoe(player, world, hand, hitResult);
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });

    }

    private static boolean itemIsHarvester(Item item) {
        return item == Items.DIAMOND_HOE || item == Items.NETHERITE_HOE;
    }

    private static boolean isFullyGrownCrop(BlockState block) {
        return block.contains(Properties.AGE_7) && block.get(Properties.AGE_7) >= 7;
    }

    private static void useHoe(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {

        BlockPos pos = hitResult.getBlockPos();
        ServerWorld serverWorld = Objects.requireNonNull(world.getServer()).getWorld(world.getRegistryKey());

        int radius = ConfigSystem.get().harvestSize;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {

                BlockPos checkPos = new BlockPos(pos.getX() + x, pos.getY(), pos.getZ() + z);
                if (!pos.isWithinDistance(checkPos, radius)) { continue; }

                BlockState block = world.getBlockState(checkPos);
                if (!isFullyGrownCrop((block))) { continue; }

                world.breakBlock(checkPos, false, player);
                world.setBlockState(checkPos, block.getBlock().getDefaultState());
                player.getStackInHand(hand).damage(2, player, hand);

                List<ItemStack> items = Block.getDroppedStacks(block, serverWorld, checkPos, null, player, player.getStackInHand(hand));

                for (ItemStack stack : items) {
                    Vec3d center = checkPos.toCenterPos();
                    Vec3d dir = pos.toCenterPos().subtract(checkPos.toCenterPos())
                            .multiply(0.1)
                            .add(0, 0.1, 0);

                    ItemEntity itemEntity = new ItemEntity(world, center.getX(), center.getY(), center.getZ(), stack.copy());
                    itemEntity.setVelocity(dir);

                    world.spawnEntity(itemEntity);
                }
            }
        }

    }


    public static void setRadius(int radius) {
        if (radius < 0) { return ; }
        ConfigSystem.get().harvestSize = radius;
        ConfigSystem.save();
    }

}
