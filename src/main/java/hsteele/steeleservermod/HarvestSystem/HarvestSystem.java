package hsteele.steeleservermod.HarvestSystem;

import hsteele.steeleservermod.config.ConfigSystem;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;

public class HarvestSystem {

    public static void register() {

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

            if (
                    player.isShiftKeyDown()
                    && itemIsHarvester(player.getItemInHand(hand).getItem())
                    && isFullyGrownCrop(world.getBlockState(hitResult.getBlockPos()))
            ) {
                useHoe(player, world, hand, hitResult);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });

    }

    private static boolean itemIsHarvester(Item item) {
        return item == Items.DIAMOND_HOE || item == Items.NETHERITE_HOE;
    }

    private static boolean isFullyGrownCrop(BlockState block) {
        return block.hasProperty(BlockStateProperties.AGE_7) && block.getValue(BlockStateProperties.AGE_7) >= 7;
    }

    private static void useHoe(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {

        BlockPos pos = hitResult.getBlockPos();
        ServerLevel serverWorld = Objects.requireNonNull(world.getServer()).getLevel(world.dimension());

        int radius = ConfigSystem.get().harvestSize;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {

                BlockPos checkPos = new BlockPos(pos.getX() + x, pos.getY(), pos.getZ() + z);
                if (!pos.closerThan(checkPos, radius)) { continue; }

                BlockState block = world.getBlockState(checkPos);
                if (!isFullyGrownCrop((block))) { continue; }

                world.destroyBlock(checkPos, false, player);
                world.setBlockAndUpdate(checkPos, block.getBlock().defaultBlockState());
                player.getItemInHand(hand).hurtAndBreak(2, player, hand);

                List<ItemStack> items = Block.getDrops(block, serverWorld, checkPos, null, player, player.getItemInHand(hand));

                for (ItemStack stack : items) {
                    Vec3 center = checkPos.getCenter();
                    Vec3 dir = pos.getCenter().subtract(checkPos.getCenter())
                            .scale(0.1)
                            .add(0, 0.1, 0);

                    ItemEntity itemEntity = new ItemEntity(world, center.x(), center.y(), center.z(), stack.copy());
                    itemEntity.setDeltaMovement(dir);

                    world.addFreshEntity(itemEntity);
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
