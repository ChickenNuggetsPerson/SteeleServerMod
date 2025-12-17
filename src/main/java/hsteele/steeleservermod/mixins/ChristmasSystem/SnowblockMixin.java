package hsteele.steeleservermod.mixins.ChristmasSystem;


import hsteele.steeleservermod.ChristmasSystem.ChristmasSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(SnowLayerBlock.class)
public class SnowblockMixin {

    @Inject(at = @At("TAIL"), method = "randomTick*")
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (ChristmasSystem.shared.overrideWeather()) { return; }
        if (world.isRaining()) { return; }

        float temperature = world.getBiome(pos).value().getBaseTemperature();
        if (temperature >= 0.15F) {
            world.destroyBlock(pos, false); // melt + drop
        }
    }
}
