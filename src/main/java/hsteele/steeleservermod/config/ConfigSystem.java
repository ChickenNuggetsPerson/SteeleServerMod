package hsteele.steeleservermod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hsteele.steeleservermod.Steeleservermod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigSystem {

    private static final Path CONFIG_PATH = Path.of("config/steeleserverconfig.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ConfigData configData;
    private static boolean isLoaded = false;

    public static void load() {
        try {
            Steeleservermod.LOGGER.info("Loading Config");
            if (!Files.exists(CONFIG_PATH)) {
                configData = new ConfigData(); // defaults
                save(); // write it
                return;
            }

            configData = GSON.fromJson(Files.newBufferedReader(CONFIG_PATH), ConfigData.class);
            isLoaded = true;
        } catch (IOException e) {
            Steeleservermod.LOGGER.error(e.toString());
            configData = new ConfigData(); // fallback
        }
    }

    public static void save() {
        Steeleservermod.LOGGER.info("Saving Config");
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(configData));
        } catch (IOException e) {
            Steeleservermod.LOGGER.error(e.toString());
        }
    }

    public static ConfigData get() {
        if (!isLoaded) {
            load();
        }
        return configData;
    }
}