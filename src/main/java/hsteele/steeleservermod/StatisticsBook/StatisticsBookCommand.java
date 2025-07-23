package hsteele.steeleservermod.StatisticsBook;


import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;


public class StatisticsBookCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> register() {

        supportedStats = getStaticIdentifiersFromStats().toArray(new Identifier[0]);

        LiteralArgumentBuilder<net.minecraft.server.command.ServerCommandSource> statsCommand = CommandManager.literal("stats")
                .requires(source -> source.hasPermissionLevel(4));

        for (Identifier stat : supportedStats) {
            statsCommand.then(CommandManager.literal(stat.getPath())
                    .executes(ctx -> {
                        return getBook(ctx, stat.getPath());
                    }));
        }

        return statsCommand;
    }

    public static Identifier[] supportedStats = {};
    public static List<Identifier> getStaticIdentifiersFromStats() {
        List<Identifier> identifiers = new ArrayList<>();
        for (Field field : Stats.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(Identifier.class)) {
                try {
                    Identifier id = (Identifier) field.get(null); // null for static field
                    identifiers.add(id);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return identifiers;
    }

    private static int getBook(CommandContext<ServerCommandSource> context, String statName) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        MinecraftServer server = context.getSource().getServer();

        if (player == null) { return -1; }

        Identifier stat = null; // Get the stat from the arg
        for (Identifier s : supportedStats) {
            if (s.getPath().equals(statName)) {
                stat = s;
            }
        }
        if (stat == null) { return -1; }

        // Force server to save stats
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            p.getStatHandler().save();
        }
        CachedNames.updateCache(); // Update the name cache

        ArrayList<StatEntry> dispStats = new ArrayList<>();


        // Read stats from json files
        Map<UUID, JsonObject> stats = StatReader.readAllPlayerStats(server);
        for (Map.Entry<UUID, JsonObject> entry : stats.entrySet()) {

            int val = StatReader.getCustomCount(entry.getValue(), stat);
            String formatted = Stats.CUSTOM.getOrCreateStat(stat).format(val);
            String name = CachedNames.getCachedName(entry.getKey());

            dispStats.add(new StatEntry(name, val, formatted));
        }

        context.getSource().sendFeedback(
                () -> Text.literal(statName + ":"),
                true
        );

        dispStats.sort(Comparator.comparingInt(StatEntry::getRawValue).reversed());
        for (StatEntry s : dispStats) {
            context.getSource().sendFeedback(
                    () -> Text.literal(s.getName() + " " + s.getFormattedValue()),
                    true
            );
        }

        return 1;
    }
}
