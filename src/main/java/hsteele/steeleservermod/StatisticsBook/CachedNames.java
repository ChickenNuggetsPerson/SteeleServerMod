package hsteele.steeleservermod.StatisticsBook;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CachedNames {

    private static Map<UUID, String> cache = new HashMap<>();

    public static void updateCache() {
        File cacheFile = new File("usercache.json");
        if (!cacheFile.exists() || cacheFile.isDirectory()) {
            return;
        }

        CachedNames.cache = new HashMap<>();

        try {
            JsonArray cacheArray = JsonParser.parseReader(new FileReader(cacheFile)).getAsJsonArray();
            for (JsonElement element : cacheArray) {
                JsonObject obj = element.getAsJsonObject();

                String name = obj.get("name").getAsString();
                String uuid = obj.get("uuid").getAsString();

                CachedNames.cache.put(UUID.fromString(uuid), name);
            }

        } catch (Exception e) {
            System.err.println("Failed to parse stats file: " + cacheFile.getName());
            e.printStackTrace();
        }
    }

    public static String getCachedName(UUID uuid) {
        return CachedNames.cache.getOrDefault(uuid, "Error Reading Name");
    }

}
