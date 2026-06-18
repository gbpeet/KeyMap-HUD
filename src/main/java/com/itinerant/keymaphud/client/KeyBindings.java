package com.itinerant.keymaphud.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    public static KeyBinding showOverlay;

    private KeyBindings() {
    }

    public static void register() {
        showOverlay = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.keymaphud.show_overlay",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT,
                "key.category.keymaphud"
        ));
    }

    public static boolean isOverlayHeld() {
        return showOverlay != null && showOverlay.isPressed();
    }
}
