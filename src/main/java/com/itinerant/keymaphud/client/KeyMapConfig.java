package com.itinerant.keymaphud.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

public final class KeyMapConfig {
    public String _comment = "KeyMap HUD configuration. Custom key labels may contain up to 6 characters.";

    public String keyboardLayout = "ANSI_US";
    public float hudScale = 1.0f;
    public String hudPosition = "CENTER";
    public String mousePosition = "RIGHT";

    public String lastSearch = "";
    public String activeProfileName = "";

    public Map<String, String> labels = new LinkedHashMap<>();
    public Set<String> safeConflicts = new LinkedHashSet<>();

    public String getLabel(int keyCode) {
        return labels.getOrDefault(Integer.toString(keyCode), "");
    }

    public void setLabel(int keyCode, String label) {
        String cleaned = sanitizeLabel(label);

        if (cleaned.isEmpty()) {
            labels.remove(Integer.toString(keyCode));
        } else {
            labels.put(Integer.toString(keyCode), cleaned);
        }
    }

    public void clearLabel(int keyCode) {
        labels.remove(Integer.toString(keyCode));
    }

    public static String sanitizeLabel(String label) {
        if (label == null) {
            return "";
        }

        String cleaned = label.strip();

        if (cleaned.length() > 6) {
            cleaned = cleaned.substring(0, 6);
        }

        return cleaned;
    }
}