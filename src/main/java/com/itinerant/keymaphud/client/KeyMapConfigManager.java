package com.itinerant.keymaphud.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class KeyMapConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("keymaphud.json");

    private static KeyMapConfig config;

    private KeyMapConfigManager() {
    }

    public static synchronized KeyMapConfig get() {
        if (config == null) {
            load();
        }

        return config;
    }

    public static synchronized void load() {
        if (!Files.exists(CONFIG_PATH)) {
            config = new KeyMapConfig();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            KeyMapConfig loaded = GSON.fromJson(reader, KeyMapConfig.class);
            config = loaded != null ? loaded : new KeyMapConfig();

            if (config.labels == null) {
                config.labels = new java.util.LinkedHashMap<>();
            }

            config.labels.replaceAll((key, value) -> KeyMapConfig.sanitizeLabel(value));
            config.labels.entrySet().removeIf(entry -> entry.getValue().isEmpty());

            if (config.keyboardLayout == null || config.keyboardLayout.isBlank()) {
                config.keyboardLayout = "ANSI_US";
            }

            config.hudScale = sanitizeHudScale(config.hudScale);

            if (config.hudPosition == null || config.hudPosition.isBlank()) {
                config.hudPosition = "CENTER";
            }

            if (config.mousePosition == null
                    || (!config.mousePosition.equals("LEFT") && !config.mousePosition.equals("RIGHT"))) {
                config.mousePosition = "RIGHT";
            }

            if (config.lastSearch == null) {
                config.lastSearch = "";
            }
        } catch (Exception exception) {
            System.err.println("[KeyMap HUD] Could not read config/keymaphud.json. Using defaults.");
            exception.printStackTrace();
            config = new KeyMapConfig();
        }
    }

    private static float sanitizeHudScale(float value) {
        float[] allowed = {0.50f, 0.75f, 1.00f, 1.25f, 1.50f};
        float closest = 1.00f;
        float closestDistance = Float.MAX_VALUE;

        for (float candidate : allowed) {
            float distance = Math.abs(candidate - value);

            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }

        return closest;
    }

    public static synchronized void save() {
        if (config == null) {
            config = new KeyMapConfig();
        }

        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            System.err.println("[KeyMap HUD] Could not save config/keymaphud.json.");
            exception.printStackTrace();
        }
    }
}