package hsteele.steeleservermod.WalkerSystem;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;

public class WalkerMinecart extends MinecartEntity {
    public WalkerMinecart(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public ItemStack getPickBlockStack() {
        return Items.AIR.getDefaultStack();
    }

    @Override
    protected Item asItem() {
        return Items.AIR;
    }
}
