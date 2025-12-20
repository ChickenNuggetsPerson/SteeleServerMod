package hsteele.steeleservermod.ChristmasSystem;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import hsteele.steeleservermod.config.ConfigSystem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.commands.Commands.argument;

public class ChristmasSystem {

    public static ChristmasSystem shared = new ChristmasSystem();

    private Holder<Biome> snowBiome;


    public boolean overrideWeather() {

        return ConfigSystem.get().christmasWeather;
    }
    private void setOverrideWeather(boolean value) {
        ConfigSystem.get().christmasWeather = value;
        ConfigSystem.save();
    }

    public Holder<Biome> getSnowBiome() {
        return snowBiome;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> registerCommands() {

        LiteralArgumentBuilder<CommandSourceStack> christmasCommand = Commands.literal("christmas")
                .requires(Commands.hasPermission(Commands.LEVEL_OWNERS));


        christmasCommand.then(Commands.literal("weatherOverride")
                .then(argument("value", BoolArgumentType.bool())
                        .executes(ChristmasSystem::setOverrideCommand)
                ));

        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            ChristmasSystem.shared.snowBiome = server
                    .overworld()
                    .registryAccess()
                    .lookupOrThrow(Registries.BIOME)
                    .get(Biomes.SNOWY_PLAINS.identifier())
                    .orElseThrow(() -> new IllegalStateException("Snowy Plains biome not found"));
        });

        ServerTickEvents.END_SERVER_TICK.register((server) -> {
            if (!shared.overrideWeather()) { return; }
            for (ServerPlayer player: server.overworld().players()) {
                snowPlayer(player, server);
            }
        });

        return christmasCommand;
    }
    private static int setOverrideCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        boolean val = BoolArgumentType.getBool(context, "value");
        ChristmasSystem.shared.setOverrideWeather(val);

        context.getSource().sendSuccess(
                () -> Component.literal("Christmas weather override set to " + val),
                true
        );

        return 1;
    };


    public static void snowPlayer(ServerPlayer player, MinecraftServer server) {
        if (!player.level().isRaining()) { return; }

        Vec3 pos = player.position().add(0, 13, 0);
        player.level().sendParticles(ParticleTypes.SNOWFLAKE, true, false, pos.x(), pos.y(), pos.z(), 60, 10, 5, 10, 0.06);
    }
}
