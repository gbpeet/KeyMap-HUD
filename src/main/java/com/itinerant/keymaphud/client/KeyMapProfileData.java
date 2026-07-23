package com.itinerant.keymaphud.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

public final class KeyMapProfileData {
    public int profileFormat = 1;
    public String profileName = "KeyMap HUD Profile";
    public String createdAt = "";
    public String minecraftVersion = "";
    public String keyMapHudVersion = "";

    public List<ModEntry> installedMods = new ArrayList<>();
    public List<BindingEntry> keybindings = new ArrayList<>();
    public Map<String, String> keyLabels = new LinkedHashMap<>();
    public Set<String> safeConflicts = new LinkedHashSet<>();
    public DisplaySettings displaySettings = new DisplaySettings();

    public static final class ModEntry {
        public String id = "";
        public String name = "";
        public String version = "";

        public ModEntry() {
        }

        public ModEntry(String id, String name, String version) {
            this.id = id;
            this.name = name;
            this.version = version;
        }
    }

    public static final class BindingEntry {
        public String categoryKey = "";
        public String translationKey = "";
        public String keyType = "KEYSYM";
        public int keyCode = -1;

        public BindingEntry() {
        }

        public BindingEntry(String categoryKey, String translationKey, String keyType, int keyCode) {
            this.categoryKey = categoryKey;
            this.translationKey = translationKey;
            this.keyType = keyType;
            this.keyCode = keyCode;
        }
    }

    public static final class DisplaySettings {
        public String keyboardLayout = "ANSI_US";
        public float hudScale = 1.0f;
        public String hudPosition = "CENTER";
        public String mousePosition = "RIGHT";
    }
}
