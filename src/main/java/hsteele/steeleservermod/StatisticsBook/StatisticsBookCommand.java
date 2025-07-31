package hsteele.steeleservermod.StatisticsBook;


import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.state.property.Properties;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static hsteele.steeleservermod.Steeleservermod.LOGGER;


public class StatisticsBookCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> register() {

        supportedStats = getStaticIdentifiersFromStats().toArray(new Identifier[0]);

        LiteralArgumentBuilder<ServerCommandSource> statsCommand = CommandManager.literal("stats")
                .requires(source -> source.hasPermissionLevel(2));

        for (Identifier stat : supportedStats) {

            LiteralArgumentBuilder<ServerCommandSource> cmd = CommandManager.literal(stat.getPath())
                    .then(CommandManager.literal("north").executes(ctx -> {
                        return getBook(ctx, stat.getPath(), Direction.NORTH);
                    }))
                    .then(CommandManager.literal("south").executes(ctx -> {
                        return getBook(ctx, stat.getPath(), Direction.SOUTH);
                    }))
                    .then(CommandManager.literal("east").executes(ctx -> {
                        return getBook(ctx, stat.getPath(), Direction.EAST);
                    }))
                    .then(CommandManager.literal("west").executes(ctx -> {
                        return getBook(ctx, stat.getPath(), Direction.WEST);
                    }));

            statsCommand.then(cmd);
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

    private static int getBook(CommandContext<ServerCommandSource> context, String statName, Direction dir) {

        MinecraftServer server = context.getSource().getServer();
        World world = context.getSource().getWorld();

        Identifier stat = null; // Get the stat from the arg
        for (Identifier s : supportedStats) {
            if (s.getPath().equals(statName)) {
                stat = s;
            }
        }
        if (stat == null) { return -1; }

        String str = getStatsString(stat, server);
        ItemStack book = makeBookItem(new String[]{str});

        Vec3d vecPos = context.getSource().getPosition().add(-1, 2, 0);
        BlockPos pos = new BlockPos((int) vecPos.x, (int) vecPos.y, (int) vecPos.z);

        BlockState withBook = Blocks.LECTERN
                .getDefaultState()
                .with(LecternBlock.HAS_BOOK, true)
                .with(Properties.HORIZONTAL_FACING, dir);
        world.setBlockState(pos, withBook);

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null) {
            LOGGER.error("No BlockEntity at {} after placement!", pos);
            return -1;
        }

        if (blockEntity instanceof LecternBlockEntity lectern) {
            lectern.setBook(book);
            world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            lectern.markDirty();

            context.getSource().sendFeedback(
                    () -> Text.literal("Made Lectern at: " + pos),
                    true
            );
        }
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player != null) {
            player.giveOrDropStack(book);
        }

        return 1;
    }

    private static String getStatsString(Identifier stat, MinecraftServer server) {
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
            if (val == 0) { continue; }

            String formatted = Stats.CUSTOM.getOrCreateStat(stat).format(val);
            String name = CachedNames.getCachedName(entry.getKey());

            dispStats.add(new StatEntry(name, val, formatted));
        }

        StringBuilder builder = new StringBuilder();

        builder.append(stat.getPath().toUpperCase()).append(":\n");

        dispStats.sort(Comparator.comparingInt(StatEntry::getRawValue).reversed());
        for (StatEntry s : dispStats) {
            builder.append(s.getName()).append(" ").append(s.getFormattedValue()).append("\n");
        }

        return builder.toString();
    }
    private static ItemStack makeBookItem(String[] pages) {
        // Title and Author
        RawFilteredPair<String> title = RawFilteredPair.of("Server Stats");
        String author = "Hayden";

//        LocalDate currentDate = LocalDate.now();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
//        String formattedDate = currentDate.format(formatter);

        // Pages
        List<RawFilteredPair<Text>> bookPages = new ArrayList<>();
//        bookPages.add(RawFilteredPair.of(Text.literal("Steele Statistics:\n\n" + formattedDate)));
        for (String page : pages) {
            bookPages.add(RawFilteredPair.of(Text.literal(page)));
        }

        int generation = 0;
        WrittenBookContentComponent content = new WrittenBookContentComponent(title, author, generation, bookPages, false);

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, content);

        return book;
    }
}
