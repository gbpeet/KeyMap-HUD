package com.itinerant.keymaphud.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {

    private static KeyBinding SHOW_OVERLAY;

    private KeyBindings() {
    }

    public static void register() {
        SHOW_OVERLAY = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.keymaphud.show_overlay",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_RIGHT_ALT,
                        "KeyKap HUD"
                )
        );
    }

    public static boolean isOverlayHeld() {
        return SHOW_OVERLAY != null && SHOW_OVERLAY.isPressed();
    }

    public static boolean matchesOverlayKey(int keyCode, int scanCode) {
        return SHOW_OVERLAY != null
                && SHOW_OVERLAY.matchesKey(keyCode, scanCode);
    }

    public static KeyBinding getShowOverlay() {
        return SHOW_OVERLAY;
    }
}