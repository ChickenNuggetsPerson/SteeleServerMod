package hsteele.steeleservermod.HarvestSystem;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import hsteele.steeleservermod.config.ConfigSystem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.argument;

public class HarvestSystemCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {

        LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> harvestCommand = Commands.literal("harvester")
                .requires(Commands.hasPermission(Commands.LEVEL_OWNERS));

        harvestCommand.then(Commands.literal("radius")
                .executes(HarvestSystemCommand::getRadius)
                .then(argument("radius", IntegerArgumentType.integer(1, 50)) // 0 to 50
                        .executes(HarvestSystemCommand::setRadius)
                ));

        return harvestCommand;
    }

    private static int getRadius(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int radius = ConfigSystem.get().harvestSize;
        context.getSource().sendSuccess(
                () -> Component.literal("Harvest Radius: " + radius + " blocks"),
                true
        );
        return 1;
    }
    private static int setRadius(CommandContext<CommandSourceStack> context) throws  CommandSyntaxException {
        int radius = IntegerArgumentType.getInteger(context, "radius");
        HarvestSystem.setRadius(radius);

        context.getSource().sendSuccess(
                () -> Component.literal("Harvest radius set to " + radius + " blocks."),
                true
        );

        return 1;
    };

}
