package hsteele.steeleservermod.StatisticsBook;


import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static hsteele.steeleservermod.Steeleservermod.LOGGER;


public class StatisticsBookCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {

        supportedStats = getStaticIdentifiersFromStats().toArray(new Identifier[0]);

        LiteralArgumentBuilder<CommandSourceStack> statsCommand = Commands.literal("stats")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS));

        for (Identifier stat : supportedStats) {

            LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal(stat.getPath())
                    .then(Commands.literal("north").executes(ctx -> {
                        try {
                            return getBook(ctx, stat.getPath(), Direction.NORTH);
                        } catch (Exception e) {
                            LOGGER.error(e.toString());
                            e.printStackTrace();
                            return -1;
                        }
                    }))
                    .then(Commands.literal("south").executes(ctx -> {
                        try {
                            return getBook(ctx, stat.getPath(), Direction.SOUTH);
                        } catch (Exception e) {
                            LOGGER.error(e.toString());
                            e.printStackTrace();
                            return -1;
                        }
                    }))
                    .then(Commands.literal("east").executes(ctx -> {
                        try {
                            return getBook(ctx, stat.getPath(), Direction.EAST);
                        } catch (Exception e) {
                            LOGGER.error(e.toString());
                            e.printStackTrace();
                            return -1;
                        }
                    }))
                    .then(Commands.literal("west").executes(ctx -> {
                        try {
                            return getBook(ctx, stat.getPath(), Direction.WEST);
                        } catch (Exception e) {
                            LOGGER.error(e.toString());
                            e.printStackTrace();
                            return -1;
                        }
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

    private static int getBook(CommandContext<CommandSourceStack> context, String statName, Direction dir) {

        MinecraftServer server = context.getSource().getServer();
        Level world = context.getSource().getLevel();

        Identifier stat = null; // Get the stat from the arg
        for (Identifier s : supportedStats) {
            if (s.getPath().equals(statName)) {
                stat = s;
            }
        }
        if (stat == null) { return -1; }

        String str = getStatsString(stat, server);
        ItemStack book = makeBookItem(new String[]{str});

        Vec3 vecPos = context.getSource().getPosition();
        BlockPos pos = BlockPos.containing(vecPos).offset(0, 2, 0);

        BlockState withBook = Blocks.LECTERN
                .defaultBlockState()
                .setValue(LecternBlock.HAS_BOOK, true)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, dir);
        world.setBlockAndUpdate(pos, withBook);

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null) {
            LOGGER.error("No BlockEntity at {} after placement!", pos);
            return -1;
        }

        if (blockEntity instanceof LecternBlockEntity lectern) {
            lectern.setBook(book);
            world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            lectern.setChanged();

            context.getSource().sendSuccess(
                    () -> Component.literal("Made Lectern at: " + pos),
                    true
            );
        }
//        ServerPlayerEntity player = context.getSource().getPlayer();
//        if (player != null) {
//            player.giveOrDropStack(book);
//        }

        return 1;
    }

    private static String getStatsString(Identifier stat, MinecraftServer server) {
        // Force server to save stats
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.getStats().save();
        }
        CachedNames.updateCache(); // Update the name cache

        ArrayList<StatEntry> dispStats = new ArrayList<>();

        // Read stats from json files
        Map<UUID, JsonObject> stats = StatReader.readAllPlayerStats(server);
        for (Map.Entry<UUID, JsonObject> entry : stats.entrySet()) {

            int val = StatReader.getCustomCount(entry.getValue(), stat);
            if (val == 0) { continue; }

            String formatted = Stats.CUSTOM.get(stat).format(val);
            String name = CachedNames.getCachedName(entry.getKey());
            if (name.equals("Error Reading Name")) { continue; }

            dispStats.add(new StatEntry(limitString(name, 10), val, formatted));
        }

        StringBuilder builder = new StringBuilder();

        builder.append(stat.getPath().toUpperCase()).append(":\n");

        dispStats.sort(Comparator.comparingInt(StatEntry::getRawValue).reversed());
        for (StatEntry s : dispStats) {
            builder.append(s.getName()).append(": ").append(s.getFormattedValue()).append("\n");
        }
        
        return builder.toString();
    }
    private static ItemStack makeBookItem(String[] pages) {
        // Title and Author
        Filterable<String> title = Filterable.passThrough("Server Stats");
        String author = "Hayden";

//        LocalDate currentDate = LocalDate.now();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
//        String formattedDate = currentDate.format(formatter);

        // Pages
        List<Filterable<Component>> bookPages = new ArrayList<>();
//        bookPages.add(RawFilteredPair.of(Text.literal("Steele Statistics:\n\n" + formattedDate)));
        for (String page : pages) {
            bookPages.add(Filterable.passThrough(Component.literal(page)));
        }

        int generation = 0;
        WrittenBookContent content = new WrittenBookContent(title, author, generation, bookPages, false);

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);

        return book;
    }

    private static String limitString(String str, int limit) {
        if (str.length() < limit) {
            return str;
        }
        return str.substring(0, limit - 1);
    }
}
