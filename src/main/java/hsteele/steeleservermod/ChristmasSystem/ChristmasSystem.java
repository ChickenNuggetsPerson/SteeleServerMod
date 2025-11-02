package hsteele.steeleservermod.ChristmasSystem;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import hsteele.steeleservermod.config.ConfigData;
import hsteele.steeleservermod.config.ConfigSystem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import static net.minecraft.server.command.CommandManager.argument;

public class ChristmasSystem {

    public static ChristmasSystem shared = new ChristmasSystem();

    private RegistryEntry<Biome> snowBiome;


    public boolean overrideWeather() {

        return ConfigSystem.get().christmasWeather;
    }
    private void setOverrideWeather(boolean value) {
        ConfigSystem.get().christmasWeather = value;
        ConfigSystem.save();
    }

    public RegistryEntry<Biome> getSnowBiome() {
        return snowBiome;
    }

    public static LiteralArgumentBuilder<ServerCommandSource> registerCommands() {

        LiteralArgumentBuilder<ServerCommandSource> christmasCommand = CommandManager.literal("christmas")
                .requires(source -> source.hasPermissionLevel(4));

        christmasCommand.then(CommandManager.literal("weatherOverride")
                .then(argument("value", BoolArgumentType.bool())
                        .executes(ChristmasSystem::setOverrideCommand)
                ));

        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            ChristmasSystem.shared.snowBiome = server
                    .getOverworld()
                    .getRegistryManager()
                    .getOrThrow(RegistryKeys.BIOME)
                    .getEntry(BiomeKeys.SNOWY_PLAINS.getValue())
                    .orElseThrow(() -> new IllegalStateException("Snowy Plains biome not found"));
        });

        ServerTickEvents.END_SERVER_TICK.register((server) -> {
            if (!shared.overrideWeather()) { return; }
            for (ServerPlayerEntity player: server.getOverworld().getPlayers()) {
                snowPlayer(player, server);
            }
        });

        return christmasCommand;
    }
    private static int setOverrideCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean val = BoolArgumentType.getBool(context, "value");
        ChristmasSystem.shared.setOverrideWeather(val);

        context.getSource().sendFeedback(
                () -> Text.literal("Christmas weather override set to " + val),
                true
        );

        return 1;
    };


    public static void snowPlayer(ServerPlayerEntity player, MinecraftServer server) {
        if (!player.getEntityWorld().isRaining()) { return; }

        Vec3d pos = player.getEntityPos().add(0, 13, 0);
        player.getEntityWorld().spawnParticles(ParticleTypes.SNOWFLAKE, true, false, pos.getX(), pos.getY(), pos.getZ(), 60, 10, 5, 10, 0.06);
    }
}
