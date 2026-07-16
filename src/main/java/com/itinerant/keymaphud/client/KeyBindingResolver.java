package com.itinerant.keymaphud.client;

import com.itinerant.keymaphud.mixin.KeyBindingAccessor;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class KeyBindingResolver {
    private KeyBindingResolver() {}

    public static InputUtil.Key getEffectiveKey(KeyBinding binding) {
        InputUtil.Key raw = ((KeyBindingAccessor) binding).getBoundKey();
        if (raw.getCode() != GLFW.GLFW_KEY_UNKNOWN) return raw;

        String name = binding.getBoundKeyLocalizedText().getString();
        if ("Not Bound".equals(name)) return InputUtil.UNKNOWN_KEY;

        for (int code = GLFW.GLFW_KEY_SPACE; code <= GLFW.GLFW_KEY_LAST; code++) {
            InputUtil.Key candidate = InputUtil.fromKeyCode(code, 0);
            if (candidate.getLocalizedText().getString().equals(name)) return candidate;
        }

        for (int button = GLFW.GLFW_MOUSE_BUTTON_1; button <= GLFW.GLFW_MOUSE_BUTTON_LAST; button++) {
            InputUtil.Key candidate = InputUtil.Type.MOUSE.createFromCode(button);
            if (candidate.getLocalizedText().getString().equals(name)) return candidate;
        }

        return InputUtil.UNKNOWN_KEY;
    }

    public static int getEffectiveKeyCode(KeyBinding binding) {
        InputUtil.Key key = getEffectiveKey(binding);
        if (key.getCode() == GLFW.GLFW_KEY_UNKNOWN) return GLFW.GLFW_KEY_UNKNOWN;
        return key.getCategory() == InputUtil.Type.MOUSE ? -100 + key.getCode() : key.getCode();
    }
}
