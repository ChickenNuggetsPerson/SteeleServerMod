package hsteele.steeleservermod.mixins.ChristmasSystem;


import com.google.common.annotations.VisibleForTesting;
import hsteele.steeleservermod.ChristmasSystem.ChristmasSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.spawner.SpecialSpawner;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;


@Mixin(ServerWorld.class)
public class ServerWorldMixin {

//    @Inject(at = @At("HEAD"), method = "getPrecipitation*", cancellable = true)
//    public void getPrecipitation(BlockPos pos, int seaLevel, CallbackInfoReturnable<Biome.Precipitation> info) {
//        if (ChristmasSystem.shared.overrideWeather()) {
//            info.setReturnValue(Biome.Precipitation.SNOW);
//        }
//    }

    @Inject(method = "tickIceAndSnow", at = @At("HEAD"), cancellable = true)
    public void tickIceAndSnow(BlockPos pos, CallbackInfo ci) {
        ServerWorld world = (ServerWorld) (Object) this;

        BlockPos blockPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, pos);
        BlockPos blockPos2 = blockPos.down();

        Biome biome = world.getBiome(blockPos).value();
        Biome snowBiome = ChristmasSystem.shared.getSnowBiome().value();
        BlockState blockState = world.getBlockState(blockPos);
        BlockState blockStateBottom = world.getBlockState(blockPos2);

        // Check if ice can form
        if (biome.canSetIce(world, blockPos2)) {
            world.setBlockState(blockPos2, Blocks.ICE.getDefaultState());
        }

        // Check if it's raining (snowing in cold biomes)
        if (world.isRaining()) {
            int maxSnowHeight = world.getGameRules().getInt(GameRules.SNOW_ACCUMULATION_HEIGHT);

            if (maxSnowHeight > 0 && (
                    ChristmasSystem.shared.overrideWeather() ?
                            snowBiome.canSetSnow(world, blockPos)
                            : biome.canSetSnow(world, blockPos)
            )) {

                if (blockState.isOf(Blocks.SNOW)) {
                    // Increase snow layer height
                    int currentLayers = blockState.get(SnowBlock.LAYERS);
                    if (currentLayers < Math.min(maxSnowHeight, 8)) {
                        BlockState newState = blockState.with(SnowBlock.LAYERS, currentLayers + 1);
                        Block.pushEntitiesUpBeforeBlockChange(blockState, newState, world, blockPos);
                        world.setBlockState(blockPos, newState);
                    }
                } else {
                    // Place new snow layer
                    world.setBlockState(blockPos, Blocks.SNOW.getDefaultState());
                }
            }

            // Handle precipitation effects
            Biome.Precipitation precipitation = biome.getPrecipitation(blockPos2, world.getSeaLevel());
            if (precipitation != Biome.Precipitation.NONE) {
                BlockState blockState3 = world.getBlockState(blockPos2);
                blockState3.getBlock().precipitationTick(blockState3, world, blockPos2, precipitation);
            }
        }

        // Break Tall Grass
        if (world.isRaining() && ChristmasSystem.shared.overrideWeather() && blockState.isOf(Blocks.SHORT_GRASS)) {
            Random random = new Random();
            if (random.nextInt() % 100 == 0) {
                world.breakBlock(blockPos, false);
            }
        }

//        world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, blockPos.toCenterPos().getX(), blockPos.toCenterPos().getY(), blockPos.toCenterPos().getZ(), 1, 0, 0, 0, 0);

        ci.cancel(); // Cancel the original method
    }

}
