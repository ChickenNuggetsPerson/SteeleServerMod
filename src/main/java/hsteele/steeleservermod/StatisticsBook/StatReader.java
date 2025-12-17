package hsteele.steeleservermod.StatisticsBook;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class StatReader {
    private static final Gson GSON = new Gson();

    public static Map<UUID, JsonObject> readAllPlayerStats(MinecraftServer server) {
        Map<UUID, JsonObject> result = new HashMap<>();

        File statsDir = new File("world/stats");
        if (!statsDir.exists() || !statsDir.isDirectory()) {
            return result;
        }

        File[] statFiles = statsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (statFiles == null) return result;

        for (File file : statFiles) {
            String fileName = file.getName().replace(".json", "");
            try {
                UUID uuid = UUID.fromString(fileName);
                JsonObject statsJson = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
                result.put(uuid, statsJson);
            } catch (Exception e) {
                System.err.println("Failed to parse stats file: " + file.getName());
                e.printStackTrace();
            }
        }

        return result;
    }

    public static int getMinedBlockCount(JsonObject statsJson, Identifier blockId) {
        JsonObject stats = statsJson.getAsJsonObject("stats");
        if (stats == null) return 0;

        JsonObject mined = stats.getAsJsonObject("minecraft:mined");
        if (mined == null) return 0;

        if (!mined.has(blockId.toString())) return 0;

        return mined.get(blockId.toString()).getAsInt();
    }

    public static int getCustomCount(JsonObject statsJson, Identifier s) {
        JsonObject stats = statsJson.getAsJsonObject("stats");
        if (stats == null) return 0;

        JsonObject custom = stats.getAsJsonObject("minecraft:custom");
        if (custom == null) return 0;

        if (!custom.has(s.toString())) return 0;

        return custom.get(s.toString()).getAsInt();
    }
}
