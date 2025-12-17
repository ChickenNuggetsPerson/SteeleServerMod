package hsteele.steeleservermod.mixins.ChristmasSystem;


import hsteele.steeleservermod.ChristmasSystem.ChristmasSystem;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;


@Mixin(ServerLevel.class)
public class ServerWorldMixin {

//    @Inject(at = @At("HEAD"), method = "getPrecipitation*", cancellable = true)
//    public void getPrecipitation(BlockPos pos, int seaLevel, CallbackInfoReturnable<Biome.Precipitation> info) {
//        if (ChristmasSystem.shared.overrideWeather()) {
//            info.setReturnValue(Biome.Precipitation.SNOW);
//        }
//    }

    @Inject(method = "tickPrecipitation", at = @At("HEAD"), cancellable = true)
    public void tickIceAndSnow(BlockPos pos, CallbackInfo ci) {
        ServerLevel world = (ServerLevel) (Object) this;

        BlockPos blockPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos);
        BlockPos blockPos2 = blockPos.below();

        Biome biome = world.getBiome(blockPos).value();
        Biome snowBiome = ChristmasSystem.shared.getSnowBiome().value();
        BlockState blockState = world.getBlockState(blockPos);
        BlockState blockStateBottom = world.getBlockState(blockPos2);

        // Check if ice can form
        if (biome.shouldFreeze(world, blockPos2)) {
            world.setBlockAndUpdate(blockPos2, Blocks.ICE.defaultBlockState());
        }

        // Check if it's raining (snowing in cold biomes)
        if (world.isRaining()) {
            int maxSnowHeight = world.getGameRules().get(GameRules.MAX_SNOW_ACCUMULATION_HEIGHT);

            if (maxSnowHeight > 0 && (
                    ChristmasSystem.shared.overrideWeather() ?
                            snowBiome.shouldSnow(world, blockPos)
                            : biome.shouldSnow(world, blockPos)
            )) {

                if (blockState.is(Blocks.SNOW)) {
                    // Increase snow layer height
                    int currentLayers = blockState.getValue(SnowLayerBlock.LAYERS);
                    if (currentLayers < Math.min(maxSnowHeight, 8)) {
                        BlockState newState = blockState.setValue(SnowLayerBlock.LAYERS, currentLayers + 1);
                        Block.pushEntitiesUp(blockState, newState, world, blockPos);
                        world.setBlockAndUpdate(blockPos, newState);
                    }
                } else {
                    // Place new snow layer
                    world.setBlockAndUpdate(blockPos, Blocks.SNOW.defaultBlockState());
                }
            }

            // Handle precipitation effects
            Biome.Precipitation precipitation = biome.getPrecipitationAt(blockPos2, world.getSeaLevel());
            if (precipitation != Biome.Precipitation.NONE) {
                BlockState blockState3 = world.getBlockState(blockPos2);
                blockState3.getBlock().handlePrecipitation(blockState3, world, blockPos2, precipitation);
            }
        }

        // Break Tall Grass
        if (world.isRaining() && ChristmasSystem.shared.overrideWeather() && blockState.is(Blocks.SHORT_GRASS)) {
            Random random = new Random();
            if (random.nextInt() % 100 == 0) {
                world.destroyBlock(blockPos, false);
            }
        }

//        world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, blockPos.toCenterPos().getX(), blockPos.toCenterPos().getY(), blockPos.toCenterPos().getZ(), 1, 0, 0, 0, 0);

        ci.cancel(); // Cancel the original method
    }

}
