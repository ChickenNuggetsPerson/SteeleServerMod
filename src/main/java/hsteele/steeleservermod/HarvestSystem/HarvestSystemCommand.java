package hsteele.steeleservermod.HarvestSystem;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import hsteele.steeleservermod.config.ConfigSystem;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;

public class HarvestSystemCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> register() {

        LiteralArgumentBuilder<net.minecraft.server.command.ServerCommandSource> harvestCommand = CommandManager.literal("harvester")
                .requires(source -> source.hasPermissionLevel(4));

        harvestCommand.then(CommandManager.literal("radius")
                .executes(HarvestSystemCommand::getRadius)
                .then(argument("radius", IntegerArgumentType.integer(1, 50)) // 0 to 50
                        .executes(HarvestSystemCommand::setRadius)
                ));

        return harvestCommand;
    }

    private static int getRadius(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int radius = ConfigSystem.get().harvestSize;
        context.getSource().sendFeedback(
                () -> Text.literal("Harvest Radius: " + radius + " blocks"),
                true
        );
        return 1;
    }
    private static int setRadius(CommandContext<ServerCommandSource> context) throws  CommandSyntaxException {
        int radius = IntegerArgumentType.getInteger(context, "radius");
        HarvestSystem.setRadius(radius);

        context.getSource().sendFeedback(
                () -> Text.literal("Harvest radius set to " + radius + " blocks."),
                true
        );

        return 1;
    };

}
