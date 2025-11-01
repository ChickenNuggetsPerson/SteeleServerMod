package hsteele.steeleservermod.mixins.ChristmasSystem;


import hsteele.steeleservermod.ChristmasSystem.ChristmasSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(SnowBlock.class)
public class SnowblockMixin {

    @Inject(at = @At("TAIL"), method = "randomTick*")
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (ChristmasSystem.shared.overrideWeather()) { return; }
        if (world.isRaining()) { return; }

        float temperature = world.getBiome(pos).value().getTemperature();
        if (temperature >= 0.15F) {
            world.breakBlock(pos, false); // melt + drop
        }
    }
}
