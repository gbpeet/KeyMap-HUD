package com.itinerant.keymaphud.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class KeyMapProfileManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final Path PROFILES_DIRECTORY =
            FabricLoader.getInstance().getConfigDir()
                    .resolve("keymaphud")
                    .resolve("profiles");

    private KeyMapProfileManager() {
    }

    public static Path getProfilesDirectory() {
        ensureProfilesDirectory();
        return PROFILES_DIRECTORY;
    }

    public static ExportResult exportProfile(String requestedName) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.options == null) {
            return new ExportResult(false, null, "Minecraft options are not available.");
        }

        String profileName = requestedName == null ? "" : requestedName.strip();

        if (profileName.isBlank()) {
            profileName = "KeyMap HUD Profile";
        }

        KeyMapProfileData data = new KeyMapProfileData();
        data.profileName = profileName;
        data.createdAt = Instant.now().toString();
        data.minecraftVersion = SharedConstants.getGameVersion().getName();
        data.keyMapHudVersion = getKeyMapHudVersion();

        FabricLoader.getInstance().getAllMods().stream()
                .sorted(Comparator.comparing(mod -> mod.getMetadata().getId()))
                .forEach(mod -> data.installedMods.add(
                        new KeyMapProfileData.ModEntry(
                                mod.getMetadata().getId(),
                                mod.getMetadata().getName(),
                                mod.getMetadata().getVersion().getFriendlyString()
                        )
                ));

        for (KeyBinding binding : client.options.allKeys) {
            InputUtil.Key key = KeyBindingResolver.getEffectiveKey(binding);

            data.keybindings.add(
                    new KeyMapProfileData.BindingEntry(
                            binding.getCategory(),
                            binding.getTranslationKey(),
                            key.getCategory().name(),
                            key.getCode()
                    )
            );
        }

        KeyMapConfig config = KeyMapConfigManager.get();
        data.keyLabels = new LinkedHashMap<>(config.labels);
        data.displaySettings.keyboardLayout = config.keyboardLayout;
        data.displaySettings.hudScale = config.hudScale;
        data.displaySettings.hudPosition = config.hudPosition;
        data.displaySettings.mousePosition = config.mousePosition;

        ensureProfilesDirectory();

        Path target = PROFILES_DIRECTORY.resolve(sanitizeFilename(profileName) + ".json");

        try (Writer writer = Files.newBufferedWriter(target)) {
            GSON.toJson(data, writer);
            config.activeProfileName = profileName;
            KeyMapConfigManager.save();
            return new ExportResult(true, target, "Exported " + target.getFileName());
        } catch (IOException exception) {
            exception.printStackTrace();
            return new ExportResult(false, target, "Could not export profile.");
        }
    }

    public static List<Path> listProfiles() {
        ensureProfilesDirectory();

        try (var stream = Files.list(PROFILES_DIRECTORY)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
        } catch (IOException exception) {
            exception.printStackTrace();
            return List.of();
        }
    }

    public static DeleteResult deleteProfile(Path path) {
        try {
            boolean deleted = Files.deleteIfExists(path);
            return deleted
                    ? new DeleteResult(true, "Deleted " + path.getFileName())
                    : new DeleteResult(false, "Profile file was not found.");
        } catch (IOException exception) {
            exception.printStackTrace();
            return new DeleteResult(false, "Could not delete " + path.getFileName());
        }
    }

    public static KeyMapProfileData readProfile(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            KeyMapProfileData data = GSON.fromJson(reader, KeyMapProfileData.class);

            if (data == null) {
                throw new IOException("Profile file is empty.");
            }

            normalize(data);
            return data;
        }
    }

    public static PreviewResult preview(KeyMapProfileData data) {
        MinecraftClient client = MinecraftClient.getInstance();

        Map<String, KeyBinding> currentBindings = buildCurrentBindingMap(client.options.allKeys);

        int matched = 0;
        int unavailable = 0;

        for (KeyMapProfileData.BindingEntry entry : data.keybindings) {
            if (currentBindings.containsKey(bindingIdentity(entry.categoryKey, entry.translationKey))) {
                matched++;
            } else {
                unavailable++;
            }
        }

        Set<String> currentModIds = new HashSet<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            currentModIds.add(mod.getMetadata().getId());
        }

        List<String> missingMods = new ArrayList<>();

        for (KeyMapProfileData.ModEntry mod : data.installedMods) {
            if (mod.id != null && !mod.id.isBlank() && !currentModIds.contains(mod.id)) {
                missingMods.add(mod.name == null || mod.name.isBlank() ? mod.id : mod.name);
            }
        }

        int settingChanges = countSettingChanges(data);

        return new PreviewResult(
                matched,
                unavailable,
                missingMods,
                settingChanges,
                data.keyLabels == null ? 0 : data.keyLabels.size()
        );
    }

    public static ApplyResult applyProfile(KeyMapProfileData data) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.options == null) {
            return new ApplyResult(false, 0, 0, "Minecraft options are not available.");
        }

        Map<String, KeyBinding> currentBindings = buildCurrentBindingMap(client.options.allKeys);

        int applied = 0;
        int unavailable = 0;

        for (KeyMapProfileData.BindingEntry entry : data.keybindings) {
            KeyBinding binding = currentBindings.get(
                    bindingIdentity(entry.categoryKey, entry.translationKey)
            );

            if (binding == null) {
                unavailable++;
                continue;
            }

            try {
                InputUtil.Key key;

                if (entry.keyCode == GLFW.GLFW_KEY_UNKNOWN) {
                    key = InputUtil.UNKNOWN_KEY;
                } else {
                    InputUtil.Type type = InputUtil.Type.valueOf(entry.keyType);
                    key = type.createFromCode(entry.keyCode);
                }

                binding.setBoundKey(key);
                applied++;
            } catch (Exception exception) {
                unavailable++;
            }
        }

        KeyBinding.updateKeysByCode();
        client.options.write();

        KeyMapConfig config = KeyMapConfigManager.get();

        config.activeProfileName =
                data.profileName == null ? "" : data.profileName.strip();

        if (data.keyLabels != null) {
            config.labels = new LinkedHashMap<>();

            data.keyLabels.forEach((key, value) -> {
                String cleaned = KeyMapConfig.sanitizeLabel(value);

                if (!cleaned.isEmpty()) {
                    config.labels.put(key, cleaned);
                }
            });
        }

        if (data.displaySettings != null) {
            config.keyboardLayout = nonBlank(data.displaySettings.keyboardLayout, "ANSI_US");
            config.hudScale = sanitizeHudScale(data.displaySettings.hudScale);
            config.hudPosition = nonBlank(data.displaySettings.hudPosition, "CENTER");

            String mouse = nonBlank(data.displaySettings.mousePosition, "RIGHT");
            config.mousePosition = "LEFT".equals(mouse) ? "LEFT" : "RIGHT";
        }

        KeyMapConfigManager.save();

        return new ApplyResult(
                true,
                applied,
                unavailable,
                "Applied " + applied + " bindings"
                        + (unavailable > 0 ? "; skipped " + unavailable + " unavailable bindings." : ".")
        );
    }

    public static ResetResult resetToDefaults() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.options == null) {
            return new ResetResult(false, "Minecraft options are not available.");
        }

        int resetBindings = 0;

        for (KeyBinding binding : client.options.allKeys) {
            binding.setBoundKey(binding.getDefaultKey());
            resetBindings++;
        }

        KeyBinding.updateKeysByCode();
        client.options.write();

        KeyMapConfig config = KeyMapConfigManager.get();
        config.keyboardLayout = "ANSI_US";
        config.hudScale = 1.0f;
        config.hudPosition = "CENTER";
        config.mousePosition = "RIGHT";
        config.lastSearch = "";
        config.activeProfileName = "";
        config.labels.clear();

        KeyMapConfigManager.save();

        return new ResetResult(
                true,
                "Reset " + resetBindings + " keybindings and cleared KeyMap HUD profile settings."
        );
    }

    private static Map<String, KeyBinding> buildCurrentBindingMap(KeyBinding[] bindings) {
        Map<String, KeyBinding> result = new HashMap<>();

        for (KeyBinding binding : bindings) {
            result.putIfAbsent(
                    bindingIdentity(binding.getCategory(), binding.getTranslationKey()),
                    binding
            );
        }

        return result;
    }

    private static String bindingIdentity(String categoryKey, String translationKey) {
        return nonBlank(categoryKey, "") + "\u0000" + nonBlank(translationKey, "");
    }

    private static int countSettingChanges(KeyMapProfileData data) {
        if (data.displaySettings == null) {
            return 0;
        }

        KeyMapConfig config = KeyMapConfigManager.get();
        int changes = 0;

        if (!nonBlank(data.displaySettings.keyboardLayout, "ANSI_US").equals(config.keyboardLayout)) {
            changes++;
        }

        if (Math.abs(sanitizeHudScale(data.displaySettings.hudScale) - config.hudScale) > 0.001f) {
            changes++;
        }

        if (!nonBlank(data.displaySettings.hudPosition, "CENTER").equals(config.hudPosition)) {
            changes++;
        }

        if (!nonBlank(data.displaySettings.mousePosition, "RIGHT").equals(config.mousePosition)) {
            changes++;
        }

        return changes;
    }

    private static void normalize(KeyMapProfileData data) {
        if (data.installedMods == null) {
            data.installedMods = new ArrayList<>();
        }

        if (data.keybindings == null) {
            data.keybindings = new ArrayList<>();
        }

        if (data.keyLabels == null) {
            data.keyLabels = new LinkedHashMap<>();
        }

        if (data.displaySettings == null) {
            data.displaySettings = new KeyMapProfileData.DisplaySettings();
        }

        if (data.profileName == null || data.profileName.isBlank()) {
            data.profileName = "Unnamed Profile";
        }
    }

    private static String sanitizeFilename(String name) {
        String cleaned = name.strip()
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", " ");

        if (cleaned.isBlank()) {
            cleaned = "KeyMap HUD Profile";
        }

        return cleaned;
    }

    private static String getKeyMapHudVersion() {
        return FabricLoader.getInstance()
                .getModContainer("keymaphud")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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

    private static void ensureProfilesDirectory() {
        try {
            Files.createDirectories(PROFILES_DIRECTORY);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public record DeleteResult(boolean success, String message) {
    }

    public record ResetResult(boolean success, String message) {
    }

    public record ExportResult(boolean success, Path path, String message) {
    }

    public record PreviewResult(
            int matchedBindings,
            int unavailableBindings,
            List<String> missingMods,
            int settingChanges,
            int keyLabelCount
    ) {
    }

    public record ApplyResult(
            boolean success,
            int appliedBindings,
            int unavailableBindings,
            String message
    ) {
    }
}
