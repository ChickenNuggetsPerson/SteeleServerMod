package hsteele.steeleservermod.WalkerSystem;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class WalkerMinecart extends Minecart {
    public WalkerMinecart(EntityType<?> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public ItemStack getPickResult() {
        return Items.AIR.getDefaultInstance();
    }

    @Override
    protected Item getDropItem() {
        return Items.AIR;
    }
}
